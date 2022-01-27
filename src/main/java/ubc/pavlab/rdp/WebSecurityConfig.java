package ubc.pavlab.rdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import ubc.pavlab.rdp.security.PermissionEvaluatorImpl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Created by mjacobson on 16/01/18.
 *
 * @see PermissionEvaluatorImpl for details on how hasPermission is defined for pre/post filtering.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Override
    protected void configure( AuthenticationManagerBuilder auth ) throws Exception {
        auth.userDetailsService( userDetailsService ).passwordEncoder( bCryptPasswordEncoder );
    }

    @Override
    public void configure( WebSecurity web ) {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator( permissionEvaluator );
        web
                .ignoring()
                    .antMatchers( "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**" )
                    .and()
                .expressionHandler( expressionHandler );
    }

    @Override
    protected void configure( HttpSecurity http ) throws Exception {
        http
                // allow _method in HTML form
                .addFilterAfter( new HiddenHttpMethodFilter(), BasicAuthenticationFilter.class )
                .authorizeRequests()
                    // public endpoints
                    .antMatchers( "/", "/login", "/registration", "/registrationConfirm", "/stats", "/stats.html",
                            "/forgotPassword", "/resetPassword", "/updatePassword", "/resendConfirmation", "/search/**",
                            "/userView/**", "/taxon/**", "/access-denied" )
                        .permitAll()
                    // API for international search
                    .antMatchers("/api/**")
                        .permitAll()
                    // administrative endpoints
                    .antMatchers( "/admin/**" )
                        .hasRole( "ADMIN" )
                    // user endpoints
                    .antMatchers( "/user/**" )
                        .hasAnyRole("USER", "ADMIN")
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
                    .tokenValiditySeconds( (int) Duration.of( 7, ChronoUnit.DAYS ).getSeconds() )
                    .and()
                .logout()
                    .logoutRequestMatcher( new AntPathRequestMatcher( "/logout" ) )
                    .logoutSuccessUrl( "/" )
                    .and()
                .exceptionHandling()
                    .accessDeniedPage( "/access-denied" )
                    .and();

    }

    @Bean
    public SecureRandom secureRandom() throws NoSuchAlgorithmException {
        return SecureRandom.getInstance( "SHA1PRNG" );
    }
}
