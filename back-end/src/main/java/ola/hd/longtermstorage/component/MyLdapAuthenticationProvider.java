package ola.hd.longtermstorage.component;

import ola.hd.longtermstorage.domain.LdapUser;
import ola.hd.longtermstorage.repository.ldap.LdapUserRepository;
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

    @Autowired
    public MyLdapAuthenticationProvider(LdapUserRepository ldapUserRepository, LdapTemplate ldapTemplate) {
        this.ldapUserRepository = ldapUserRepository;
        this.ldapTemplate = ldapTemplate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        // Already authenticated? Pass
        if (authentication.isAuthenticated()) {
            return authentication;
        }

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // TODO: move the default user setup to the config file
        if (username.equals("admin") && password.equals("admin")) {
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
                throw new BadCredentialsException("This user is not allowed to use the LZA service.");
            }
        }

        // Incorrect username or password
        throw new BadCredentialsException("Incorrect username or password.");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
