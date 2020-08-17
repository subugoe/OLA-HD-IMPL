package ola.hd.longtermstorage.repository.mongo;

import ola.hd.longtermstorage.domain.MongoUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<MongoUser, String> {
    MongoUser findByUsername(String username);
    MongoUser findByUsernameAndPassword(String username, String password);
}
