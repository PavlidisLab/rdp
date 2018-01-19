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

package ubc.pavlab.rdp.util;

import lombok.*;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "aspect", "term", "definition", "isObsolete"})
public class GOTerm {

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"id"})
    @ToString
    public static class Relationship {
        public enum RelationshipType {
            IS_A, PART_OF
        }

        private String id;
        private RelationshipType type;
    }

    private String id;
    private String aspect;
    private String term;
    private String definition;
    private boolean isObsolete;
    private Collection<Relationship> parents = new HashSet<>();
    private Collection<Relationship> children = new HashSet<>();
    private Map<Taxon, Integer> sizesByTaxon = new HashMap<>();

    public void addChild( Relationship child ) {
        this.children.add( child );
    }

    public void addParent( Relationship parent ) {
        this.parents.add( parent );
    }

    public void setSize( Integer size, Taxon taxon ) {
        this.sizesByTaxon.put( taxon, size );
    }

    public Integer getSize( Taxon taxon ) {
        Integer res = this.sizesByTaxon.get( taxon );
        if ( res == null ) {
            return 0;
        } else {
            return res;
        }
    }

    public Integer getSize() {
        Integer size = 0;
        for ( Integer count : this.sizesByTaxon.values() ) {
            size += count;
        }
        return size;
    }

}
