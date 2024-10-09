package ubc.pavlab.rdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import ubc.pavlab.rdp.security.PermissionEvaluatorImpl;
import ubc.pavlab.rdp.security.authentication.TokenBasedAuthenticationFilter;
import ubc.pavlab.rdp.security.authentication.TokenBasedAuthenticationManager;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.BooleanSupplier;

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
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private HandlerMappingIntrospector mvcIntrospector;

    @Autowired
    private UserService userService;

    @Override
    protected void configure( AuthenticationManagerBuilder auth ) throws Exception {
        auth.userDetailsService( userDetailsService ).passwordEncoder( bCryptPasswordEncoder );
    }

    @Override
    public void configure( WebSecurity web ) {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator( permissionEvaluator );
        web.expressionHandler( expressionHandler );
    }

    @Override
    protected void configure( HttpSecurity http ) throws Exception {
        http
                .addFilterBefore( new ConditionSatisfiedFilter( new MvcRequestMatcher( mvcIntrospector, "/api/**" ), () -> applicationSettings.getIsearch().isEnabled(), HttpStatus.SERVICE_UNAVAILABLE, "Public API is not available for this registry." ), AbstractPreAuthenticatedProcessingFilter.class )
                .addFilterBefore( new ConditionSatisfiedFilter( new MvcRequestMatcher( mvcIntrospector, "/api/users/search" ), () -> applicationSettings.getSearch().getEnabledSearchModes().contains( ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER ), HttpStatus.SERVICE_UNAVAILABLE, "Search by researcher is not available for this registry." ), AbstractPreAuthenticatedProcessingFilter.class )
                .addFilterBefore( new ConditionSatisfiedFilter( new MvcRequestMatcher( mvcIntrospector, "/api/genes/search" ), () -> applicationSettings.getSearch().getEnabledSearchModes().contains( ApplicationSettings.SearchSettings.SearchMode.BY_GENE ), HttpStatus.SERVICE_UNAVAILABLE, "Search by gene is not available for this registry." ), AbstractPreAuthenticatedProcessingFilter.class )
                .addFilterBefore( new ConditionSatisfiedFilter( new MvcRequestMatcher( mvcIntrospector, "/api/users/by-anonymous-id/*" ), () -> applicationSettings.getPrivacy().isEnableAnonymizedSearchResults(), HttpStatus.SERVICE_UNAVAILABLE, "Anonymized search results are not available for this registry." ), AbstractPreAuthenticatedProcessingFilter.class )
                .addFilterBefore( new ConditionSatisfiedFilter( new MvcRequestMatcher( mvcIntrospector, "/api/genes/by-anonymous-id/*" ), () -> applicationSettings.getPrivacy().isEnableAnonymizedSearchResults(), HttpStatus.SERVICE_UNAVAILABLE, "Anonymized search results are not available for this registry." ), AbstractPreAuthenticatedProcessingFilter.class )
                // allow _method in HTML form
                .addFilterBefore( new HiddenHttpMethodFilter(), AbstractPreAuthenticatedProcessingFilter.class )
                .addFilterAfter( new TokenBasedAuthenticationFilter( new AntPathRequestMatcher( "/api/**" ), new TokenBasedAuthenticationManager( userService, applicationSettings ) ), AbstractPreAuthenticatedProcessingFilter.class )
                .authorizeRequests()
                    // public endpoints
                    .mvcMatchers( "/", "/login", "/registration", "/registrationConfirm", "/stats", "/stats.html",
                            "/forgotPassword", "/resetPassword", "/updatePassword", "/resendConfirmation", "/search/**",
                            "/userView/**", "/taxon/**", "/access-denied" )
                        .permitAll()
                    // static assets
                    .mvcMatchers( "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**" )
                        .permitAll()
                    // API for international search
                    .mvcMatchers("/api/**")
                        .permitAll()
                    // administrative endpoints
                    .mvcMatchers( "/admin/**" )
                        .hasRole( "ADMIN" )
                    // user endpoints
                    .mvcMatchers( "/user/**" )
                        .hasAnyRole("USER", "ADMIN")
                    // ensure that actuator endpoints are secured regardless of the basepath they are mounted on
                    .requestMatchers( EndpointRequest.toAnyEndpoint() )
                        .hasRole( "ADMIN" )
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

    /**
     * Simple filter that check if a condition is satisfied, otherwise raise a {@link HttpStatus#SERVICE_UNAVAILABLE} error.
     */
    public static class ConditionSatisfiedFilter implements Filter {

        private final RequestMatcher requestMatcher;

        private final BooleanSupplier condition;

        private final HttpStatus status;
        private final String message;

        public ConditionSatisfiedFilter( RequestMatcher requestMatcher, BooleanSupplier condition, HttpStatus status, String message ) {
            this.requestMatcher = requestMatcher;
            this.condition = condition;
            this.status = status;
            this.message = message;
        }

        @Override
        public void doFilter( ServletRequest request, ServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
            if ( !( request instanceof HttpServletRequest ) || !( response instanceof HttpServletResponse ) ) {
                throw new ServletException( "Only HTTP requests are supported by the ConditionSatisfiedFilter." );
            }
            if ( requestMatcher.matches( (HttpServletRequest) request ) && !condition.getAsBoolean() ) {
                ( (HttpServletResponse) response ).sendError( status.value(), message );
            } else {
                filterChain.doFilter( request, response );
            }
        }
    }
}
