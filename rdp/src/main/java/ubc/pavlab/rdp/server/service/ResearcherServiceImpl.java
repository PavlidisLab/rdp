/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.service;

import gemma.gsec.model.UserGroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubc.pavlab.rdp.server.dao.ResearcherDao;
import ubc.pavlab.rdp.server.dao.UserDao;
import ubc.pavlab.rdp.server.dao.UserGroupDao;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.security.authentication.UserService;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Service("researcherService")
public class ResearcherServiceImpl implements ResearcherService {

    @Autowired
    ResearcherDao researcherDao;

    @Autowired
    UserDao userDao;
    
    @Autowired
    UserGroupDao userGroupDao;
    
    @Override
    @Transactional
    public Researcher create( final Researcher researcher ) {
        return researcherDao.create( researcher );
    }

    @Override
    @Transactional
    public void update( Researcher researcher ) {
        researcherDao.update( researcher );
    }

    @Override
    public Researcher findByEmail( final String email ) {
        return researcherDao.findByEmail( email );
    }

    @Override
    public Researcher findByUserName( final String username ) {
        return researcherDao.findByUsername( username );
    }

    @Override
    public void delete( Researcher researcher ) {
        
        /**
         * We can only delete Researcher.Contact if
         * it's no longer referenced in UserGroup!
         */
        if ( researcher != null ) {
            User contact = researcher.getContact();
            for ( UserGroup group : this.userDao
                    .loadGroups( contact ) ) {
                group.getGroupMembers().remove( contact );
                this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );
            }
    
            researcherDao.remove( researcher );
        }
    }

    public Researcher thaw( Researcher researcher ) {
        return researcherDao.thaw( researcher );
    }
}
