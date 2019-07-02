package ola.hd.longtermstorage.component;

import ola.hd.longtermstorage.domain.ArchiveStatus;
import ola.hd.longtermstorage.domain.ExportRequest;
import ola.hd.longtermstorage.repository.mongo.ExportRequestRepository;
import ola.hd.longtermstorage.service.ArchiveManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Component
public class ScheduledTasks {

    private final ExportRequestRepository exportRequestRepository;

    private final ArchiveManagerService archiveManagerService;

    @Autowired
    public ScheduledTasks(ExportRequestRepository exportRequestRepository, ArchiveManagerService archiveManagerService) {
        this.exportRequestRepository = exportRequestRepository;
        this.archiveManagerService = archiveManagerService;
    }

    @Scheduled(fixedDelay = 5000)
    public void cleanStorage() throws IOException {

        // Get all online archive requests, order by timestamp descending
        List<ExportRequest> requests = exportRequestRepository.findByStatusOrderByTimestampDesc(ArchiveStatus.ONLINE);

        // PID - availableUntil. If availableUntil is null, change all subsequent requests statuses to deleted,
        // Otherwise, set all availableUntil to the latest one
        HashMap<String, Instant> statuses = new HashMap<>();

        for (ExportRequest request : requests) {
            String pid = request.getPid();

            // First new PID encountered?
            if (!statuses.containsKey(pid)) {
                Instant availableUntil = request.getAvailableUntil();

                // Is it still available?
                if (availableUntil.compareTo(Instant.now()) < 0) {

                    // No, time to delete
                    archiveManagerService.moveFromDiskToTape(pid);

                    // Update the database
                    request.setStatus(ArchiveStatus.DELETED);

                    // Tell the other to change status, too
                    statuses.put(pid, null);
                } else {

                    // Still available, do nothing
                    // Just tell other to update their availableUntil to the latest one
                    statuses.put(pid, availableUntil);
                }
            } else {

                // Subsequent PID encountered
                Instant newAvailableUntil = statuses.get(pid);

                // There is a new available time
                if (newAvailableUntil != null) {

                    // Update this available time to the latest one
                    request.setAvailableUntil(newAvailableUntil);
                } else {

                    // No new available time -> the archive was moved to tape already
                    // Update the request status
                    request.setStatus(ArchiveStatus.DELETED);
                }
            }
        }

        exportRequestRepository.saveAll(requests);
    }
}
