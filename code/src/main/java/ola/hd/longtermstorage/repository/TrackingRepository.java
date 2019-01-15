package ola.hd.longtermstorage.repository;

import ola.hd.longtermstorage.domain.TrackingInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrackingRepository extends MongoRepository<TrackingInfo, String> {
}
