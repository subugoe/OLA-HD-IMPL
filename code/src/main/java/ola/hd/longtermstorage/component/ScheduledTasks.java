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

    /**
     * When users request to export data from tapes, it must be moved to disks first. After some time, this background
     * job will move data back to tapes.
     * Run at 04:00 every Monday.
     * @throws IOException Thrown if something's wrong when connecting to different services
     */
    @Scheduled(cron = "0 0 4 ? * MON")
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

    /**
     * Check the status of the archive of each export request and update the database.
     * Run once a day, at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateRequestStatus() throws IOException {

        // Get all pending archive requests
        List<ExportRequest> requests = exportRequestRepository.findByStatus(ArchiveStatus.PENDING);

        // PID - ArchiveStatus. If the ArchiveStatus is ONLINE, change all subsequent request statuses to ONLINE,
        // Otherwise, do nothing
        HashMap<String, ArchiveStatus> statuses = new HashMap<>();

        for (ExportRequest request : requests) {
            String pid = request.getPid();

            // First new PID encountered?
            if (!statuses.containsKey(pid)) {

                // If the archive is ready on disk
                if (archiveManagerService.isArchiveOnDisk(pid)) {

                    // Update the database
                    request.setStatus(ArchiveStatus.ONLINE);

                    // Tell the other to change status, too
                    statuses.put(pid, ArchiveStatus.ONLINE);

                } else {

                    // The archive is not ready yet
                    // Notify the same archive requests so that they won't have to check again
                    statuses.put(pid, ArchiveStatus.PENDING);
                }
            } else {

                // Subsequent PID encountered
                ArchiveStatus archiveStatus = statuses.get(pid);

                // The archive is ready
                if (archiveStatus == ArchiveStatus.ONLINE) {

                    // Update the database
                    request.setStatus(ArchiveStatus.ONLINE);
                }
            }
        }

        exportRequestRepository.saveAll(requests);
    }
}
