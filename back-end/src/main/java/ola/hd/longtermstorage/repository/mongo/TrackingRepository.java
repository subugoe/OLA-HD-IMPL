package ola.hd.longtermstorage.repository.mongo;

import ola.hd.longtermstorage.domain.TrackingInfo;
import ola.hd.longtermstorage.domain.TrackingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingRepository extends MongoRepository<TrackingInfo, String> {

    TrackingInfo findByPid(String pid);
    TrackingInfo findByOnlineIdOrOfflineId(String onlineId, String offlineId);
    List<TrackingInfo> findByUsername(String username, Pageable pageable);
}
