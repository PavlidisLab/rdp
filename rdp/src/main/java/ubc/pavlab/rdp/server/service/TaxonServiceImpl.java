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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubc.pavlab.rdp.server.dao.TaxonDao;
import ubc.pavlab.rdp.server.model.Taxon;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service("taxonService")
public class TaxonServiceImpl implements TaxonService {

    private static Log log = LogFactory.getLog( TaxonServiceImpl.class );

    @Autowired
    TaxonDao taxonDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.TaxonService#findById(java.lang.Long)
     */
    @Transactional
    @Override
    public Taxon findById( Long id ) {
        return taxonDao.findById( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.TaxonService#findByCommonName(java.lang.String)
     */
    @Transactional
    @Override
    public Taxon findByCommonName( String commonName ) {
        return taxonDao.findByCommonName( commonName );
    }

    @Transactional
    @Override
    public Collection<Taxon> loadAll() {
        return taxonDao.loadAll();
    }

    @Transactional
    @Override
    public Collection<Long> loadAllIds() {
        return taxonDao.loadAllIds();
    }

}
