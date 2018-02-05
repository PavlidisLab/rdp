package ubc.pavlab.rdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

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
    @Qualifier("dataSource")
    private DataSource dataSource;

    @Value("${rdp.queries.users-query}")
    private String usersQuery;

    @Value("${rdp.queries.roles-query}")
    private String rolesQuery;

    @Override
    protected void configure( AuthenticationManagerBuilder auth )
            throws Exception {
        auth.
                jdbcAuthentication()
                .usersByUsernameQuery( usersQuery )
                .authoritiesByUsernameQuery( rolesQuery )
                .dataSource( dataSource )
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
                        "/access-denied").permitAll()
                .antMatchers( "/manager/**" ).hasAnyRole( "MANAGER", "ADMIN" )
                .antMatchers( "/admin/**" ).hasRole( "ADMIN" )
                .antMatchers( "/", "/user/**" ).authenticated().anyRequest()
                .authenticated().and().csrf().disable().formLogin()
                .loginPage( "/login" ).failureUrl( "/login?error=true" )
                .defaultSuccessUrl( "/" )
                .usernameParameter( "email" )
                .passwordParameter( "password" )
                .and().logout()
                .logoutRequestMatcher( new AntPathRequestMatcher( "/logout" ) )
                .logoutSuccessUrl( "/login" ).and().exceptionHandling()
                .accessDeniedPage( "/access-denied" );
    }

    @Override
    public void configure( WebSecurity web ) throws Exception {
        web
                .ignoring()
                .antMatchers( "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**" );
    }
}
