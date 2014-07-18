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

package ubc.pavlab.rdp.server.controller;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.Contact;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import scala.annotation.meta.field;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.service.ResearcherService;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Controller
public class ResearcherController extends BaseController {

    @Autowired
    ResearcherService researcherService;
    
    @Autowired
    UserManager userManager;
    
    @RequestMapping("/saveResearcher.html")
    public void saveResearcher( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        
        String firstName = request.getParameter( "firstName" );
        String lastName = request.getParameter( "lastName" );
        String organization = request.getParameter( "organization" );
        String department = request.getParameter( "department" );
        String email = request.getParameter( "email" );
        
        try {
            User contact = ( User ) userManager.findbyEmail( email );
            contact.setEmail( email );
            contact.setFirstName( firstName );
            contact.setLastName( lastName );
            
            Researcher researcher = researcherService.findByEmail( email );
            if ( researcher == null ) {
                researcher = researcherService.create(researcher);
            }
            researcher.setContact( contact );
            researcher.setDepartment( department );
            researcher.setOrganization( organization );
            researcherService.update( researcher );
        } catch ( Exception e ) {
            
        }
        
    }


}
