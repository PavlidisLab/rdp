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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.JSONObject;

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

    @JsonIgnore
    @Column(name = "PASSWORD")
    private String password;

    @JsonIgnore
    @Column(name = "PASSWORD_HINT")
    private String passwordHint;

    @JsonIgnore
    @Column(name = "ENABLED")
    private Boolean enabled;

    @JsonIgnore
    @Column(name = "SIGNUP_TOKEN")
    private String signupToken;

    @JsonIgnore
    @Column(name = "SIGNUP_TOKEN_DATESTAMP")
    private java.util.Date signupTokenDatestamp;

    @JsonIgnore
    @Column(name = "NAME")
    private String name;

    @JsonIgnore
    @Column(name = "DESCRIPTION")
    private String description;

    // This really should be orphanRemoval = true but a bug related to this HHH-5267
    // was only fixed in Hibernate 4.1.7
    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "user")
    private PasswordResetToken passwordResetToken;

    public User() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !(object instanceof User) ) {
            return false;
        }
        final User that = (User) object;
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
        return this.getFirstName() + " " + this.getLastName();
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

    public PasswordResetToken getPasswordResetToken() {
        return this.passwordResetToken;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + (id == null ? 0 : id.hashCode());

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

    public void setPasswordResetToken( PasswordResetToken passwordResetToken ) {
        this.passwordResetToken = passwordResetToken;
    }

    public JSONObject toJSON() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put( "firstName", this.firstName );
        jsonObj.put( "lastName", this.lastName );
        jsonObj.put( "email", this.email );
        jsonObj.put( "userName", this.userName );

        return jsonObj;
    }

}
