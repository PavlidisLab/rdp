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

package ubc.pavlab.rdp.server.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.model.Gene;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Service("geneService")
public class GeneServiceImpl implements GeneService {

    @Autowired
    GeneDao geneDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#create(ubc.pavlab.rdp.server.model.Gene)
     */
    @Override
    public Gene create( Gene gene ) {
        return geneDao.create( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#update(ubc.pavlab.rdp.server.model.Gene)
     */
    @Override
    public void update( Gene gene ) {
        geneDao.update( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#delete(ubc.pavlab.rdp.server.model.Gene)
     */
    @Override
    public void delete( Gene gene ) {
        geneDao.remove( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#loadAll()
     */
    @Override
    public Collection<Gene> loadAll() {
        return ( Collection<Gene> ) geneDao.loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#findByOfficalSymbol(java.lang.String)
     */
    @Override
    public Collection<Gene> findByOfficalSymbol( String officialSymbol ) {
        return geneDao.findByOfficalSymbol( officialSymbol );
    }

    @Override
    public Gene load( long geneId ) {
        return geneDao.load( geneId );
    }

}
