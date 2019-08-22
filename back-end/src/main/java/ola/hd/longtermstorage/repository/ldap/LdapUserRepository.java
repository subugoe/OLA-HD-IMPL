package ola.hd.longtermstorage.repository.ldap;

import ola.hd.longtermstorage.domain.LdapUser;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LdapUserRepository extends LdapRepository<LdapUser> {
    LdapUser findByUsername(String username);
}
