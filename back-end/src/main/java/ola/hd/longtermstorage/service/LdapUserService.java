package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.domain.LdapUser;
import ola.hd.longtermstorage.repository.ldap.LdapUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LdapUserService implements UserDetailsService {

    private LdapUserRepository ldapUserRepository;

    @Autowired
    public LdapUserService(LdapUserRepository ldapUserRepository) {
        this.ldapUserRepository = ldapUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LdapUser ldapUser = ldapUserRepository.findByUsername(username);

        if (ldapUser == null) {
            throw new UsernameNotFoundException(username);
        }

        // TODO: Get user roles
        return new User(ldapUser.getUsername(), ldapUser.getPassword(), AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
