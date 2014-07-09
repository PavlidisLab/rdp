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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * TODO Document Me
 * 
 * @author cmcdonald
 * @version $Id: UserGroup.java,v 1.6 2013/06/11 22:55:59 anton Exp $
 */
@Entity
@Table(name = "USER_GROUP")
public class UserGroup implements gemma.gsec.model.UserGroup {
    /**
     * 
     */
    private static final long serialVersionUID = -1802428808035961711L;

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.EAGER)
    @JoinTable(name = "GROUP_MEMBERS", joinColumns = { @JoinColumn(name = "USER_GROUP_FK") }, inverseJoinColumns = { @JoinColumn(name = "GROUP_MEMBERS_FK") })
    private Set<User> groupMembers;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "GROUP_FK")
    private Collection<GroupAuthority> authorities;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    public UserGroup() {
        groupMembers = new HashSet<User>();
        authorities = new HashSet<GroupAuthority>();
    }

    @Override
    public Collection<GroupAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Collection<User> getGroupMembers() {
        return groupMembers;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T extends gemma.gsec.model.GroupAuthority> void setAuthorities( Collection<T> authorities ) {
        this.authorities = ( Collection<GroupAuthority> ) authorities;
    }

    @Override
    public void setDescription( String description ) {
        this.description = description;
    }

    @Override
    public <T extends gemma.gsec.model.User> void setGroupMembers( Collection<T> groupMembers ) {
        this.groupMembers = ( Set<User> ) groupMembers;
    }

    @Override
    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public void setName( String name ) {
        this.name = name;
    }

}