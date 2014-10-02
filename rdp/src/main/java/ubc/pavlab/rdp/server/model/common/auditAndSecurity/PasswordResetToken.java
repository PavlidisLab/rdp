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

package ubc.pavlab.rdp.server.model.common.auditAndSecurity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * A persisted one-time use secure password reset token.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Entity
@Table(name = "PASSWORD_RESET_TOKEN")
public class PasswordResetToken {

    @Id
    @Column(name = "USER_ID")
    private Long userId;

    @OneToOne
    @PrimaryKeyJoinColumn(name = "USER_ID", referencedColumnName = "ID")
    private User user;

    @Column(name = "TOKEN_KEY")
    private String tokenKey;

    @Column(name = "CREATION_DATESTAMP")
    private java.util.Date creationDateStamp;

    public PasswordResetToken() {
    }

    public PasswordResetToken( User user, String tokenKey, java.util.Date creationDateStamp ) {
        this.user = user;
        this.userId = user.getId();
        this.creationDateStamp = creationDateStamp;
        this.tokenKey = tokenKey;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    public void setKey( String tokenKey ) {
        this.tokenKey = tokenKey;
    }

    public java.util.Date getCreationDateStamp() {
        return creationDateStamp;
    }

    public void setCreationDateStamp( java.util.Date creationDateStamp ) {
        this.creationDateStamp = creationDateStamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser( User user ) {
        this.user = user;
        this.userId = user.getId();
    }
}
