package ola.hd.longtermstorage.repository.ldap;

import ola.hd.longtermstorage.domain.User;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends LdapRepository<User> {

    User findByUsername(String username);
    User findByUsernameAndPassword(String username, String password);
}
