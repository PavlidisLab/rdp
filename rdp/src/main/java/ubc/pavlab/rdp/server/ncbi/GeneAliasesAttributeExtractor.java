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

package ubc.pavlab.rdp.server.ncbi;

import java.util.Collection;
import java.util.HashSet;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;

import org.apache.commons.lang.StringUtils;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAlias;

/**
 * Extract gene aliases and combine them in a DELIMITER separated string
 * 
 * @author ptan
 * @version $Id$
 */
public class GeneAliasesAttributeExtractor implements AttributeExtractor {

    /**
     * 
     */
    private static final long serialVersionUID = -342476365828411244L;

    private final String DELIMITER = ",";

    @Override
    public Object attributeFor( Element element, String attributeName ) {
        if ( !attributeName.equals( "aliases" ) ) {
            throw new AssertionError( attributeName );
        }

        Gene gene = ( Gene ) element.getObjectValue();

        Collection<String> aliases = new HashSet<>();
        for ( GeneAlias alias : gene.getAliases() ) {
            aliases.add( alias.getAlias() );
        }
        return StringUtils.join( aliases, DELIMITER );
    }
}
