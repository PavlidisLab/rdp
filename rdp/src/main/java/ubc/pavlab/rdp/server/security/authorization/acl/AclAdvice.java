/*
 * The rdp project
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

package ubc.pavlab.rdp.server.security.authorization.acl;

import gemma.gsec.SecurityService;
import gemma.gsec.acl.BaseAclAdvice;
import gemma.gsec.model.Securable;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.GroupAuthority;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup;

/**
 * Adds security controls to newly created objects, and removes them for objects that are deleted. Methods in this
 * interceptor are run for all new objects (to add security if needed) and when objects are deleted. This is not used to
 * modify permissions on existing objects.
 * <p>
 * Implementation Note: For permissions modification to be triggered, the method name must match certain patterns, which
 * include "create", or "remove". These patterns are defined in the {@link AclPointcut}. Other methods that would
 * require changes to permissions will not work without modifying the source code.
 */
@Component
public class AclAdvice extends BaseAclAdvice {

    @Autowired
    SecurityService securityService;

    private static Log log = LogFactory.getLog( AclAdvice.class );

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#getUserGroupGrantedAuthority(gemma.gsec.model.Securable)
     */
    @Override
    protected GrantedAuthority getUserGroupGrantedAuthority( Securable object ) {
        Collection<GroupAuthority> authorities = ( ( UserGroup ) object ).getAuthorities();
        assert authorities.size() == 1;
        GrantedAuthority ga = new SimpleGrantedAuthority( authorities.iterator().next().getAuthority() );
        return ga;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#getUserName(gemma.gsec.model.Securable)
     */
    @Override
    protected String getUserName( Securable user ) {
        return ( ( User ) user ).getUserName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#objectIsUser(gemma.gsec.model.Securable)
     */
    @Override
    protected boolean objectIsUser( Securable object ) {
        return User.class.isAssignableFrom( object.getClass() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#objectIsUserGroup(gemma.gsec.model.Securable)
     */
    @Override
    protected boolean objectIsUserGroup( Securable object ) {
        return UserGroup.class.isAssignableFrom( object.getClass() );
    }

    /**
     * Certain objects are not made public immediately on creation by administrators. The default implementation returns
     * true if clazz is assignable to SecuredChild; otherwise false. Subclasses overriding this method should probably
     * call super.specialCaseToKeepPrivateOnCreation()
     * 
     * @param clazz
     * @return true if it's a special case to be kept private on creation.
     */
    @Override
    protected boolean specialCaseToKeepPrivateOnCreation( Class<? extends Securable> clazz ) {
        return true;
    }
}