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

package ubc.pavlab.rdp.server.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GOTerm {

    public static class Relationship {
        private String id;

        public enum RelationshipType {
            IS_A, PART_OF
        };

        private RelationshipType type;

        public Relationship( String id, RelationshipType type ) {
            this.id = id;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Relationship [id=" + id + ", type=" + type + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
            return result;
        }

        // Only by ID as there should only be one relationship between two nodes regardless of type, we want to fail
        // loudly otherwise
        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            Relationship other = ( Relationship ) obj;
            if ( id == null ) {
                if ( other.id != null ) return false;
            } else if ( !id.equals( other.id ) ) return false;
            return true;
        }

    }

    private String Id;
    private String aspect;
    private String term;
    private String definition;
    private boolean isObsolete;
    private Collection<Relationship> parents = new HashSet<Relationship>();
    private Collection<Relationship> children = new HashSet<Relationship>();

    public GOTerm() {
    }

    public GOTerm( String id ) {
        Id = id;
    }

    public GOTerm( String id, String aspect, String term, String definition, boolean isObsolete ) {
        Id = id;
        this.aspect = aspect;
        this.term = term;
        this.definition = definition;
        this.isObsolete = isObsolete;
    }

    public String getId() {
        return Id;
    }

    public void setId( String id ) {
        Id = id;
    }

    public String getAspect() {
        return aspect;
    }

    public void setAspect( String aspect ) {
        this.aspect = aspect;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm( String term ) {
        this.term = term;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition( String definition ) {
        this.definition = definition;
    }

    public boolean isObsolete() {
        return isObsolete;
    }

    public void setObsolete( boolean isObsolete ) {
        this.isObsolete = isObsolete;
    }

    public Collection<Relationship> getParents() {
        return parents;
    }

    public void setParents( Collection<Relationship> parents ) {
        this.parents = parents;
    }

    public Collection<Relationship> getChildren() {
        return children;
    }

    public void setChildren( Collection<Relationship> children ) {
        this.children = children;
    }

    public void addChild( Relationship child ) {
        this.children.add( child );
    }

    public void addParent( Relationship parent ) {
        this.parents.add( parent );
    }

    @Override
    public String toString() {
        return "GOTerm [Id=" + Id + ", aspect=" + aspect + ", term=" + term + ", definition=" + definition
                + ", isObsolete=" + isObsolete + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( Id == null ) ? 0 : Id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GOTerm other = ( GOTerm ) obj;
        if ( Id == null ) {
            if ( other.Id != null ) return false;
        } else if ( !Id.equals( other.Id ) ) return false;
        return true;
    }

}
