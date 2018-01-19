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

package ubc.pavlab.rdp.services;


import org.json.JSONArray;
import org.springframework.security.access.annotation.Secured;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.UserGene.TierType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GeneService {

    @Secured( "ADMIN" )
    Gene create( final Gene gene );

    @Secured("ADMIN")
    void update( Gene gene );

    Gene load( Integer geneId );

    Collection<Gene> load( Collection<Integer> ids );

    @Secured("ADMIN")
    void delete( Gene gene );

    @Secured("ADMIN")
    Collection<Gene> loadAll();

    Collection<Gene> findByOfficialSymbol( final String officialSymbol );

    Gene findByOfficialSymbolAndTaxon( String officialSymbol, Taxon taxon );

    Collection<Gene> findByTaxonId( final Integer id );

    Collection<Gene> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    List<Gene> autocomplete( String query, Taxon taxon );

    HashMap<Gene, TierType> deserializeGenes( String[] genesJSON );

    HashMap<Gene, TierType> quickDeserializeGenes( String[] genesJSON ) throws IllegalArgumentException;

    @Secured("ADMIN")
    void updateGeneTable( String filePath );

    @Secured("ADMIN")
    void truncateGeneTable();

    @Secured("GROUP_USER")
    JSONArray toJSON( Collection<UserGene> geneAssociations );

}
