package ola.hd.longtermstorage.repository.mongo;

import ola.hd.longtermstorage.domain.ArchiveStatus;
import ola.hd.longtermstorage.domain.ExportRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportRequestRepository extends MongoRepository<ExportRequest, String> {

    List<ExportRequest> findByStatus(ArchiveStatus status);

    List<ExportRequest> findByStatusOrderByTimestampDesc(ArchiveStatus status);
}
