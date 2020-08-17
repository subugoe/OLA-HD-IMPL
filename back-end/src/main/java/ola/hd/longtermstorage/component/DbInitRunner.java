package ola.hd.longtermstorage.component;

import ola.hd.longtermstorage.domain.MongoUser;
import ola.hd.longtermstorage.repository.mongo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DbInitRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    @Autowired
    public DbInitRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        MongoUser admin = userRepository.findByUsername("admin");
        if (admin == null) {
            admin = new MongoUser("admin", "JW24G.xR");
            userRepository.save(admin);
        }

        MongoUser olahd = userRepository.findByUsername("olahd");
        if (olahd == null) {
            olahd = new MongoUser("olahd", "ja+bw>3L");
            userRepository.save(olahd);
        }
    }
}
