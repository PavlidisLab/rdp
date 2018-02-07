package ubc.pavlab.rdp.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 07/02/18.
 */
public class UserPrinciple implements UserDetails {
    private User user;
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrinciple(User user) {
        this.user = user;
        this.authorities = user.getRoles().stream().map( r -> new SimpleGrantedAuthority( r.getRole() ) ).collect( Collectors.toSet() );
    }

    public int getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
