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
package ubc.pavlab.rdp.server.model.common.auditAndSecurity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * A user of the software system, who is authenticated. *
 * 
 * @author ?
 * @version $Id: User.java,v 1.9 2013/06/11 22:56:00 anton Exp $
 */
@Entity
@Table(name = "USER")
public class User implements gemma.gsec.model.User {
    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME")
    private String userName;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "FIRSTNAME")
    private String firstName;

    @Column(name = "LASTNAME")
    private String lastName;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "PASSWORD_HINT")
    private String passwordHint;

    @Column(name = "ENABLED")
    private Boolean enabled;

    @Column(name = "SIGNUP_TOKEN")
    private String signupToken;

    @Column(name = "SIGNUP_TOKEN_DATESTAMP")
    private java.util.Date signupTokenDatestamp;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;
    
    public User() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof User ) ) {
            return false;
        }
        final User that = ( User ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Boolean getEnabled() {
        return this.enabled;
    }

    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getFullName() {
        return this.getName() + " " + this.getLastName();
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getPasswordHint() {
        return this.passwordHint;
    }

    @Override
    public String getSignupToken() {
        return this.signupToken;
    }

    @Override
    public java.util.Date getSignupTokenDatestamp() {
        return this.signupTokenDatestamp;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public void setDescription( String description ) {
        this.description = description;

    }

    @Override
    public void setEmail( String email ) {
        this.email = email;
    }

    @Override
    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    public void setFirstName( String firstName ) {
        this.firstName = firstName;
    }

    
    @Override
    public void setId( Long id ) {
        this.id = id;

    }

    @Override
    public void setLastName( String lastName ) {
        this.lastName = lastName;
    }

    @Override
    public void setName( String name ) {
        this.name = name;

    }

    @Override
    public void setPassword( String password ) {
        this.password = password;
    }

    @Override
    public void setPasswordHint( String passwordHint ) {
        this.passwordHint = passwordHint;
    }

    @Override
    public void setSignupToken( String signupToken ) {
        this.signupToken = signupToken;
    }

    @Override
    public void setSignupTokenDatestamp( java.util.Date signupTokenDatestamp ) {
        this.signupTokenDatestamp = signupTokenDatestamp;
    }

    @Override
    public void setUserName( String userName ) {
        this.userName = userName;
    }

}