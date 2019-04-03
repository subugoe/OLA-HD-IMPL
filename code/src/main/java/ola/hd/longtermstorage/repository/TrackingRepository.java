package ola.hd.longtermstorage.repository;

import ola.hd.longtermstorage.domain.TrackingInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingRepository extends MongoRepository<TrackingInfo, String> {

    TrackingInfo findByPid(String pid);
}
