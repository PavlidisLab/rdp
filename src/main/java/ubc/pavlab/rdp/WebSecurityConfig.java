package ubc.pavlab.rdp;

import org.springframework.beans.factory.annotation.Autowired;
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
    private SiteSettings siteSettings;

    @Override
    protected void configure( AuthenticationManagerBuilder auth )
            throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder( bCryptPasswordEncoder );
    }


    @Override
    protected void configure( HttpSecurity http ) throws Exception {

        http.
                authorizeRequests()
                .antMatchers(
                        "/login",
                        "/registration",
                        "/registrationConfirm",
                        "/stats",
                        "/stats.html",
                        "/forgotPassword",
                        "/resetPassword",
                        "/updatePassword",
                        "/resendConfirmation",
                        "/api/**",
                        "/access-denied").permitAll()
                .antMatchers( "/manager/**" ).hasAnyRole( "USER", "ADMIN" )
                .antMatchers( "/admin/**" ).hasRole( "ADMIN" )
                .antMatchers( "/", "/user/**" ).authenticated().anyRequest()
                .authenticated().and().csrf().disable().formLogin()
                .loginPage( "/login" ).failureUrl( "/login?error=true" )
                .defaultSuccessUrl( "/" )
                .usernameParameter( "email" )
                .passwordParameter( "password" )
                .and().rememberMe()
                .rememberMeCookieName(siteSettings.getShortname() + "-remember-me")
                .tokenValiditySeconds(7 * 24 * 60 * 60)
                .and().logout()
                .logoutRequestMatcher( new AntPathRequestMatcher( "/logout" ) )
                .logoutSuccessUrl( "/login" ).and().exceptionHandling()
                .accessDeniedPage( "/access-denied" );
    }

    @Override
    public void configure( WebSecurity web ) {
        web
                .ignoring()
                .antMatchers( "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**" );
    }
}
