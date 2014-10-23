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

package ubc.pavlab.rdp.server.cache;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.Criteria;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.util.SearchableEhcache;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Component
public class GeneOntologyTermCacheImpl extends SearchableEhcache<GeneOntologyTerm> implements GeneOntologyTermCache {
    // These constants are used in ehcache.xml. If they are changed, ehcache.xml must be modified.
    private static final String CACHE_NAME = "GeneOntologyTermCache";
    private static final String ID_SEARCH_ATTRIBUTE_NAME = "geneOntologyId";
    private static final String TERM_SEARCH_ATTRIBUTE_NAME = "geneOntologyTerm";
    private static final String DEFINITION_SEARCH_ATTRIBUTE_NAME = "definition";

    private Attribute<Object> idAttribute;
    private Attribute<Object> termAttribute;
    private Attribute<Object> definitionAttribute;

    protected final Log log = LogFactory.getLog( GeneOntologyTermCacheImpl.class );

    @Override
    public Collection<GeneOntologyTerm> fetchByIds( Collection<String> ids ) {
        Criteria idCriteria = idAttribute.in( ids );

        return fetchByCriteria( idCriteria );
    }

    @Override
    public GeneOntologyTerm fetchById( String id ) {
        Criteria idCriteria = idAttribute.eq( id );
        Collection<GeneOntologyTerm> results = fetchByCriteria( idCriteria );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple terms match GO ID: (" + id + "), return first hit" );
        }
        return fetchByCriteria( idCriteria ).iterator().next();
    }

    @Override
    public Collection<GeneOntologyTerm> fetchByQuery( String queryString ) {
        ArrayList<GeneOntologyTerm> results = new ArrayList<>();

        // 1. Exact Ids
        String regexQueryString = queryString;
        Criteria idCriteria = idAttribute.ilike( regexQueryString );
        Criteria idCriteria2 = idAttribute.ilike( "GO:" + regexQueryString );
        results.addAll( fetchByCriteria( idCriteria.or( idCriteria2 ) ) );

        // 2. whole query in term
        regexQueryString = "*" + queryString + "*";
        Criteria termCriteria = termAttribute.ilike( regexQueryString );
        Collection<GeneOntologyTerm> tmp = fetchByCriteria( termCriteria );
        tmp.removeAll( results );
        results.addAll( tmp );

        // 3. Whole query in definition
        Criteria definitionCriteria = definitionAttribute.ilike( regexQueryString );
        tmp = fetchByCriteria( definitionCriteria );
        tmp.removeAll( results );
        results.addAll( tmp );

        // 4. Split by spaces and check for each word in term
        String[] queryList = queryString.split( " " );
        termCriteria = null;
        for ( String q : queryList ) {
            Criteria tmpCriteria = termAttribute.ilike( "*" + q + "*" );
            termCriteria = ( termCriteria == null ) ? tmpCriteria : termCriteria.and( tmpCriteria );
        }
        tmp = fetchByCriteria( termCriteria );
        tmp.removeAll( results );
        results.addAll( tmp );

        // 5. Split by spaces and check for each word in definition
        definitionCriteria = null;
        for ( String q : queryList ) {
            Criteria tmpCriteria = definitionAttribute.ilike( "*" + q + "*" );
            definitionCriteria = ( definitionCriteria == null ) ? tmpCriteria : definitionCriteria.and( tmpCriteria );
        }
        tmp = fetchByCriteria( definitionCriteria );
        tmp.removeAll( results );
        results.addAll( tmp );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#clearAll()
     */
    @Override
    public void clearAll() {
        removeAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#size()
     */
    @Override
    public long size() {
        return getSize();
    }

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public Object getKey( GeneOntologyTerm term ) {
        return term.getGeneOntologyId();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
        idAttribute = getSearchAttribute( ID_SEARCH_ATTRIBUTE_NAME );
        termAttribute = getSearchAttribute( TERM_SEARCH_ATTRIBUTE_NAME );
        definitionAttribute = getSearchAttribute( DEFINITION_SEARCH_ATTRIBUTE_NAME );
    }

}
