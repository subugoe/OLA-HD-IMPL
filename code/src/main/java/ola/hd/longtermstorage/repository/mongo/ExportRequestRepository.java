package ola.hd.longtermstorage.repository.mongo;

import ola.hd.longtermstorage.domain.ExportRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportRequestRepository extends MongoRepository<ExportRequest, String> {
}
