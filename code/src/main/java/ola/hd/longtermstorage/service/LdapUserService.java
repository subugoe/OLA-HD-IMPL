package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.domain.User;
import ola.hd.longtermstorage.domain.UserDetailsImpl;
import ola.hd.longtermstorage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LdapUserService implements UserDetailsService {

    private UserRepository userRepository;

    @Autowired
    public LdapUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        return new UserDetailsImpl(user);
    }
}
