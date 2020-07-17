package ola.hd.longtermstorage.repository.mongo;

import ola.hd.longtermstorage.domain.Archive;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchiveRepository extends MongoRepository<Archive, String> {

    Archive findByPid(String pid);
    Archive findByOnlineIdOrOfflineId(String onlineId, String offlineId);
}
