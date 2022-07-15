package ubc.pavlab.rdp.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;
import ubc.pavlab.rdp.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Repository of gene ontology terms and gene-to-term relationships.
 *
 * @author poirigui
 */
@Repository
public class GeneOntologyTermInfoRepository implements CrudRepository<GeneOntologyTermInfo, String> {

    private final Map<String, GeneOntologyTermInfo> terms = new HashMap<>();

    private final MultiValueMap<Integer, GeneOntologyTermInfo> geneIdsToTerms = new LinkedMultiValueMap<>();

    /**
     * Guarantees that only one thread can modify terms and geneIdsToTerms at a given time and that no reading occurs
     * while a modification is undergoing.
     */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public <S extends GeneOntologyTermInfo> S save( S term ) {
        return saveByAlias( term.getGoId(), term );
    }

    @Override
    public <S extends GeneOntologyTermInfo> Iterable<S> saveAll( Iterable<S> iterable ) {
        List<S> savedTerms = new ArrayList<>();
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( S term : iterable ) {
                savedTerms.add( save( term ) );
            }
        } finally {
            lock.unlock();
        }
        return savedTerms;
    }

    @Override
    public Optional<GeneOntologyTermInfo> findById( String s ) {
        return Optional.ofNullable( terms.get( s ) );
    }

    @Override
    public boolean existsById( String s ) {
        Lock lock = rwLock.readLock();
        try {
            return terms.containsKey( s );
        } finally {
            lock.unlock();
        }
    }

    public <S extends GeneOntologyTermInfo> S saveByAlias( String alias, S term ) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            terms.put( alias, term );
            for ( Integer geneId : term.getDirectGeneIds() ) {
                geneIdsToTerms.add( geneId, term );
            }
        } finally {
            lock.unlock();
        }
        return term;
    }

    public <S extends GeneOntologyTermInfo> Iterable<S> saveAllByAlias( Map<String, S> terms ) {
        List<S> savedTerms = new ArrayList<>( terms.size() );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( Map.Entry<String, S> entry : terms.entrySet() ) {
                savedTerms.add( saveByAlias( entry.getKey(), entry.getValue() ) );
            }
        } finally {
            lock.unlock();
        }
        return savedTerms;
    }

    @Override
    public Collection<GeneOntologyTermInfo> findAll() {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            return CollectionUtils.unmodifiableCopy( terms.values() );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<GeneOntologyTermInfo> findAllById( Iterable<String> iterable ) {
        Collection<GeneOntologyTermInfo> results = new HashSet<>();
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            for ( String id : iterable ) {
                GeneOntologyTermInfo term = terms.get( id );
                if ( term != null ) {
                    results.add( term );
                }
            }
        } finally {
            lock.unlock();
        }
        return results;
    }

    public Collection<GeneOntologyTermInfo> findByDirectGeneIdsContaining( Integer geneId ) {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            List<GeneOntologyTermInfo> terms = geneIdsToTerms.get( geneId );
            return terms != null ? CollectionUtils.unmodifiableCopy( terms ) : Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long count() {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            return terms.values().stream().distinct().count();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteById( String s ) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            GeneOntologyTermInfo term = terms.get( s );
            if ( term != null ) {
                delete( term );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete( GeneOntologyTermInfo term ) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            terms.remove( term.getGoId() );
            for ( Integer geneId : term.getDirectGeneIds() ) {
                geneIdsToTerms.remove( geneId );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteAllById( Iterable<? extends String> iterable ) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( String s : iterable ) {
                deleteById( s );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteAll( Iterable<? extends GeneOntologyTermInfo> iterable ) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( GeneOntologyTermInfo term : iterable ) {
                delete( term );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteAll() {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            terms.clear();
            geneIdsToTerms.clear();
        } finally {
            lock.unlock();
        }
    }
}
