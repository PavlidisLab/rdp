package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.repositories.UserRepository;

/**
 * Created by mjacobson on 06/02/18.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPrinciple loadUserByUsername( String email ) {
        return userRepository.findByEmailIgnoreCase( email )
                .map( UserPrinciple::new )
                .orElseThrow( () -> new UsernameNotFoundException( email ) );
    }

}
