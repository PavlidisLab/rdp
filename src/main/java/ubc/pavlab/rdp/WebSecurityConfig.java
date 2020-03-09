package ubc.pavlab.rdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Locale;

/**
 * Created by mjacobson on 16/01/18.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private MessageSource messageSource;

    @Override
    protected void configure( AuthenticationManagerBuilder auth ) throws Exception {
        auth.userDetailsService( userDetailsService ).passwordEncoder( bCryptPasswordEncoder );
    }

    @Override
    public void configure( WebSecurity web ) {
        web.ignoring().antMatchers( "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**" );
    }

    @Override
    protected void configure( HttpSecurity http ) throws Exception {

        http
                .authorizeRequests()
                    // public endpoints
                    .antMatchers( "/", "/login", "/registration", "/registrationConfirm", "/stats", "/stats.html",
                            "/forgotPassword", "/resetPassword", "/updatePassword", "/resendConfirmation", "/search/**",
                            "/userView/**", "/request-match/**", "/api/**", "/taxon/**", "/access-denied" )
                        .permitAll()
                    // adninistrative endpoints
                    .antMatchers( "/admin/**" )
                        .hasRole( "ADMIN" )
                    // user endpoints
                    .antMatchers( "/user/**" )
                        .authenticated()
                    .and()
                // TODO: we should fully comply with CSRF
                .csrf()
                    .disable()
                .formLogin()
                    .loginPage( "/login" )
                    .usernameParameter( "email" )
                    .passwordParameter( "password" )
                    .defaultSuccessUrl( "/" )
                    .failureUrl( "/login?error=true" )
                    .and()
                .rememberMe()
                    .rememberMeCookieName( messageSource.getMessage("rdp.site.shortname", null, Locale.getDefault()) + "-remember-me" )
                    .tokenValiditySeconds( 7 * 24 * 60 * 60 )
                    .and()
                .logout()
                    .logoutRequestMatcher( new AntPathRequestMatcher( "/logout" ) )
                    .logoutSuccessUrl( "/" )
                    .and()
                .exceptionHandling()
                    .accessDeniedPage( "/access-denied" );
    }
}
