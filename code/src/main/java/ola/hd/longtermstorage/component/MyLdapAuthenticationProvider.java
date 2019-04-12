package ola.hd.longtermstorage.component;

import ola.hd.longtermstorage.domain.LdapUser;
import ola.hd.longtermstorage.repository.ldap.LdapUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class MyLdapAuthenticationProvider implements AuthenticationProvider {

    private final LdapUserRepository ldapUserRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public MyLdapAuthenticationProvider(LdapUserRepository ldapUserRepository) {
        this.ldapUserRepository = ldapUserRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        // Already authenticated? Pass
        if (authentication.isAuthenticated()) {
            return authentication;
        }

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Encode the password
        password = passwordEncoder.encode(password);

        // TODO: find by username and password
        //LdapUser user = ldapUserRepository.findByUsernameAndPassword(username, password);

        LdapUser user = ldapUserRepository.findByUsername(username);

        // User found
        if (user != null) {

            // TODO: Check if user is allowed to use the LZA service
//            if (user.getUserServices().contains("lzaAccess")) {
//                return new UsernamePasswordAuthenticationToken(username, password);
//            }
            // TODO: Get user roles
            return new UsernamePasswordAuthenticationToken(username, password, AuthorityUtils.createAuthorityList("ROLE_USER"));
        }

        // User not found or is not allowed to use the service
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
}
