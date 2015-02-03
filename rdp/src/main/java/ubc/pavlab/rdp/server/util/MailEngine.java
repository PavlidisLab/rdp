/*
 * The Gemma project
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
package ubc.pavlab.rdp.server.util;

import java.util.Map;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * @author paul
 * @version $Id: MailEngine.java,v 1.1 2013/07/24 00:43:21 paul Exp $
 */
public interface MailEngine {

    /**
     * @param bodyText
     * @param subject
     */
    public abstract void sendAdminMessage( String bodyText, String subject );

    /**
     * @param msg
     */
    public abstract void send( SimpleMailMessage msg );

    /**
     * @param msg
     * @param templateName
     * @param model
     */
    public abstract void sendMessage( SimpleMailMessage msg, String templateName, Map<String, Object> model );

    /**
     * @param msg
     * @param templateName
     * @param model
     * @param attachFile
     */
    public abstract void sendMessage( SimpleMailMessage msg, String templateName, Map<String, Object> model,
            CommonsMultipartFile attachFile );

}