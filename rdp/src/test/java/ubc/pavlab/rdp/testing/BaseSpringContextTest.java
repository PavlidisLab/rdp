/*
 * The aspiredb project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ubc.pavlab.rdp.testing;

import gemma.gsec.AuthorityConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ubc.pavlab.rdp.server.security.authentication.UserManager;

/**
 * subclass for tests that need the container and use the database
 * 
 * @author pavlidis
 * @version $Id: BaseSpringContextTest.java,v 1.4 2013/06/12 20:18:48 cmcdonald Exp $
 */
@ContextConfiguration(locations = { "classpath*:application-context.xml", "classpath*:test-data-source.xml",
        "classpath*:gemma/gsec/acl/security-bean-baseconfig.xml", "classpath*:applicationContext-security.xml",
        "classpath*:applicationContext-serviceBeans.xml" })
public abstract class BaseSpringContextTest extends AbstractJUnit4SpringContextTests implements InitializingBean {

    protected abstract class InlineTransaction {
        private TransactionStatus txStatus;

        public void execute() {
            beginTransaction();
            instructions();
            commitTransaction();
        }

        public abstract void instructions();

        protected void beginTransaction() {
            txStatus = transactionManager.getTransaction( new DefaultTransactionDefinition() );
        }

        protected void commitTransaction() {
            transactionManager.commit( txStatus );
        }

    }

    @Autowired
    protected HibernateTransactionManager transactionManager;

    protected HibernateDaoSupport hibernateSupport = new HibernateDaoSupport() {
    };

    protected Log log = LogFactory.getLog( getClass() );

    /**
     * The SimpleJdbcTemplate that this base class manages, available to subclasses. (Datasource; autowired at setteer)
     */
    protected SimpleJdbcTemplate simpleJdbcTemplate;

    private AuthenticationTestingUtil authenticationTestingUtil;

    /**
     * @throws Exception
     */
    @Override
    final public void afterPropertiesSet() throws Exception {
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        hibernateSupport.setSessionFactory( this.getBean( SessionFactory.class ) );

        this.authenticationTestingUtil = new AuthenticationTestingUtil();
        this.authenticationTestingUtil.setUserManager( this.getBean( UserManager.class ) );

        runAsAdmin();
    }

    /**
     * Convenience shortcut for RandomStringUtils.randomAlphabetic( 10 ) (or something similar to that)
     * 
     * @return
     */
    public String randomName() {
        return RandomStringUtils.randomAlphabetic( 10 );
    }

    /**
     * Set the DataSource, typically provided via Dependency Injection.
     */
    @Autowired
    public void setDataSource( DataSource dataSource ) {
        this.simpleJdbcTemplate = new SimpleJdbcTemplate( dataSource );
    }

    /**
     * Count the rows in the given table.
     * 
     * @param tableName table name to count rows in
     * @return the number of rows in the table
     */
    protected int countRowsInTable( String tableName ) {
        return SimpleJdbcTestUtils.countRowsInTable( this.simpleJdbcTemplate, tableName );
    }

    /**
     * Convenience method for deleting all rows from the specified tables. Use with caution outside of a transaction!
     * 
     * @param names the names of the tables from which to delete
     * @return the total number of rows deleted from all specified tables
     */
    protected int deleteFromTables( String... names ) {
        return SimpleJdbcTestUtils.deleteFromTables( this.simpleJdbcTemplate, names );
    }

    /**
     * @param t
     * @return
     */
    protected <T> T getBean( Class<T> t ) {
        return this.applicationContext.getBean( t );
    }

    /**
     * Convenience method to obtain instance of any bean by name. Use this only when necessary, you should wire your
     * tests by injection instead.
     * 
     * @param name
     * @return
     */
    protected <T> T getBean( String name, Class<T> t ) {
        try {
            return this.applicationContext.getBean( name, t );
        } catch ( BeansException e ) {
            throw new RuntimeException( e );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Elevate to administrative privileges (tests normally run this way, this can be used to set it back if you called
     * runAsUser). This gets called before each test, no need to run it yourself otherwise.
     */
    protected final void runAsAdmin() {
        authenticationTestingUtil.grantAdminAuthority( this.applicationContext );
    }

    protected final void runAsAnon() {
        authenticationTestingUtil.grantAnonAuthority( this.applicationContext );
    }

    /**
     * Run as a regular user.
     * 
     * @param userName
     */
    protected final void runAsUser( String userName ) {
        authenticationTestingUtil.switchToUser( this.applicationContext, userName );
    }

}

final class AuthenticationTestingUtil {

    /**
     * @param token
     */
    private static void putTokenInContext( AbstractAuthenticationToken token ) {
        SecurityContextHolder.getContext().setAuthentication( token );
    }

    private UserManager userManager;

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
    }

    /**
     * Grant authority to a test user, with admin privileges, and put the token in the context. This means your tests
     * will be authorized to do anything an administrator would be able to do.
     */
    protected void grantAdminAuthority( ApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                Arrays.asList( new GrantedAuthority[] { new SimpleGrantedAuthority(
                        AuthorityConstants.ADMIN_GROUP_AUTHORITY ) } ) );

        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    protected void grantAnonAuthority( ApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "anon", "anon",
                Arrays.asList( new GrantedAuthority[] { new SimpleGrantedAuthority(
                        AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) } ) );

        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * Grant authority to a test user, with regular user privileges, and put the token in the context. This means your
     * tests will be authorized to do anything that user could do
     */
    protected void switchToUser( ApplicationContext ctx, String username ) {

        UserDetails user = userManager.loadUserByUsername( username );

        List<GrantedAuthority> authrs = new ArrayList<GrantedAuthority>( user.getAuthorities() );

        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        TestingAuthenticationToken token = new TestingAuthenticationToken( username, "testing", authrs );
        token.setAuthenticated( true );

        putTokenInContext( token );
    }
}
