package ola.hd.longtermstorage.component;

import ola.hd.longtermstorage.domain.LdapUser;
import ola.hd.longtermstorage.domain.MongoUser;
import ola.hd.longtermstorage.repository.ldap.LdapUserRepository;
import ola.hd.longtermstorage.repository.mongo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

@Component
public class MyLdapAuthenticationProvider implements AuthenticationProvider {

    @Value("${spring.ldap.access}")
    private String access;

    private final LdapUserRepository ldapUserRepository;
    private final LdapTemplate ldapTemplate;
    private final UserRepository userRepository;

    @Autowired
    public MyLdapAuthenticationProvider(LdapUserRepository ldapUserRepository, LdapTemplate ldapTemplate, UserRepository userRepository) {
        this.ldapUserRepository = ldapUserRepository;
        this.ldapTemplate = ldapTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        // Already authenticated? Pass
        if (authentication.isAuthenticated()) {
            return authentication;
        }

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Check for test users
        MongoUser testUser = userRepository.findByUsernameAndPassword(username, password);
        if (testUser != null) {
            return new UsernamePasswordAuthenticationToken(username, password, AuthorityUtils.createAuthorityList("ROLE_USER"));
        }

        LdapUser user = ldapUserRepository.findByUsername(username);

        // User found
        if (user != null) {

            // Bind to the LDAP with user DN and password
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("uid", username));
            if (ldapTemplate.authenticate(user.getDn(), filter.toString(), password)) {

                // Check if user is allowed to use the LZA service
                if (user.getUserServices().contains(access)) {

                    // TODO: Get user roles

                    return new UsernamePasswordAuthenticationToken(username, password, AuthorityUtils.createAuthorityList("ROLE_USER"));
                }

                // User is not allowed to use the service
                throw new BadCredentialsException("Full authentication is required to access this resource.");
            }
        }

        // Incorrect username or password
        throw new BadCredentialsException("Full authentication is required to access this resource.");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
