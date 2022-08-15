package ubc.pavlab.rdp.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Repository of gene ontology terms and gene-to-term relationships.
 *
 * @author poirigui
 */
@Repository
public class GeneOntologyTermInfoRepository implements CrudRepository<GeneOntologyTermInfo, String> {

    private final Map<String, GeneOntologyTermInfo> termsByIdOrAlias = new HashMap<>();

    private final MultiValueMap<GeneOntologyTermInfo, String> termsToAliases = new LinkedMultiValueMap<>();
    private final MultiValueMap<Integer, GeneOntologyTermInfo> geneIdsToTerms = new LinkedMultiValueMap<>();
    private final MultiValueMap<GeneOntologyTermInfo, Integer> termsToGeneIds = new LinkedMultiValueMap<>();

    /**
     * Guarantees that only one thread can modify termsByIdOrAlias, termsToAliases, geneIdsToTerms or termsToGeneIds at
     * a given time and that no reading occurs while a modification is undergoing.
     */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public <S extends GeneOntologyTermInfo> S save( S term ) {
        Assert.notNull( term, "Term cannot be null." );
        return saveByAlias( term.getGoId(), term );
    }

    @Override
    public <S extends GeneOntologyTermInfo> Iterable<S> saveAll( Iterable<S> iterable ) {
        Assert.notNull( iterable, "Iterable cannot be null." );
        Collection<S> savedTerms = new LinkedHashSet<>();
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( S term : iterable ) {
                savedTerms.add( save( term ) );
            }
        } finally {
            lock.unlock();
        }
        return Collections.unmodifiableCollection( savedTerms );
    }

    @Override
    public Optional<GeneOntologyTermInfo> findById( String id ) {
        Assert.notNull( id, "ID cannot be null." );
        return Optional.ofNullable( termsByIdOrAlias.get( id ) );
    }

    @Override
    public boolean existsById( String id ) {
        Assert.notNull( id, "ID cannot be null." );
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            return termsByIdOrAlias.containsKey( id );
        } finally {
            lock.unlock();
        }
    }

    /**
     * Save a term using an alias instead of its ID.
     */
    public <S extends GeneOntologyTermInfo> S saveByAlias( String alias, S term ) {
        Assert.notNull( alias, "Alias cannot be null." );
        Assert.notNull( term, "Term cannot be null." );
        Assert.notNull( term.getGoId(), "Term ID cannot be null." );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            termsByIdOrAlias.put( alias, term );
            termsToAliases.add( term, alias );
            // also save by ID if missing
            if ( !termsByIdOrAlias.containsKey( term.getGoId() ) ) {
                termsByIdOrAlias.put( term.getGoId(), term );
                termsToAliases.add( term, term.getGoId() );
            }
            // clear any existing associations
            deleteTermToGeneAssociations( term );
            for ( Integer geneId : term.getDirectGeneIds() ) {
                geneIdsToTerms.add( geneId, term );
                termsToGeneIds.add( term, geneId );
            }
        } finally {
            lock.unlock();
        }
        return term;
    }

    /**
     * Save terms using aliases instead of their IDs.
     */
    public <S extends GeneOntologyTermInfo> Iterable<S> saveAllByAlias( Map<String, S> terms ) {
        Assert.notNull( terms, "Terms mapping cannot be null." );
        Collection<S> savedTerms = new LinkedHashSet<>( terms.size() );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( Map.Entry<String, S> entry : terms.entrySet() ) {
                savedTerms.add( saveByAlias( entry.getKey(), entry.getValue() ) );
            }
        } finally {
            lock.unlock();
        }
        return Collections.unmodifiableCollection( savedTerms );
    }

    @Override
    public Collection<GeneOntologyTermInfo> findAll() {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            return Collections.unmodifiableCollection( new HashSet<>( termsByIdOrAlias.values() ) );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<GeneOntologyTermInfo> findAllById( Iterable<String> iterable ) {
        Assert.notNull( iterable, "Iterable cannot be null." );
        Collection<GeneOntologyTermInfo> results = new LinkedHashSet<>();
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            for ( String id : iterable ) {
                Assert.notNull( id, "Iterable elements cannot be null." );
                GeneOntologyTermInfo term = termsByIdOrAlias.get( id );
                if ( term != null ) {
                    results.add( term );
                }
            }
        } finally {
            lock.unlock();
        }
        return Collections.unmodifiableCollection( results );
    }

    /**
     * Find all terms in this repository as a {@link Stream}.
     * <p>
     * This is more efficient than {@link #findAll()} as no read-only copy is created.
     * <p>
     * Note: be extremely careful while using this as the returned stream holds a read-only lock on the internal data
     * structure. You should call {@link Stream#close()} as early as possible.
     */
    public Stream<GeneOntologyTermInfo> findAllAsStream() {
        Lock lock = rwLock.readLock();
        lock.lock();
        return termsByIdOrAlias.values().stream()
                .onClose( lock::unlock )
                .distinct();
    }

    /**
     * Find terms containing the provided gene ID in their direct gene IDs.
     */
    public Collection<GeneOntologyTermInfo> findByDirectGeneIdsContaining( Integer geneId ) {
        Assert.notNull( geneId, "Gene ID cannot be null." );
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            List<GeneOntologyTermInfo> terms = geneIdsToTerms.get( geneId );
            return terms != null ? Collections.unmodifiableCollection( new LinkedHashSet<>( terms ) ) : Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long count() {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            return termsToAliases.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteById( String id ) {
        // FIXME: we should acquire a read lock here and promote it to a write lock if the element exists, but I don't
        //        think the Java implementation supports that pattern
        Assert.notNull( id, "ID cannot be null." );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            GeneOntologyTermInfo term = termsByIdOrAlias.get( id );
            if ( term != null ) {
                delete( term );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete( GeneOntologyTermInfo term ) {
        Assert.notNull( term, "Term cannot be null." );
        Assert.notNull( term.getGoId(), "Term ID cannot be null." );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            List<String> aliases = termsToAliases.remove( term );
            if ( aliases != null ) {
                for ( String alias : aliases ) {
                    termsByIdOrAlias.remove( alias );
                }
            }
            deleteTermToGeneAssociations( term );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteAllById( Iterable<? extends String> iterable ) {
        Assert.notNull( iterable, "Iterable cannot be null." );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( String id : iterable ) {
                Assert.notNull( id, "Iterable elements cannot be null." );
                deleteById( id );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteAll( Iterable<? extends GeneOntologyTermInfo> iterable ) {
        Assert.notNull( iterable, "Iterable cannot be null." );
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            for ( GeneOntologyTermInfo term : iterable ) {
                Assert.notNull( term, "Iterable elements cannot be null." );
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
            termsByIdOrAlias.clear();
            termsToAliases.clear();
            geneIdsToTerms.clear();
            termsToGeneIds.clear();
        } finally {
            lock.unlock();
        }
    }

    private void deleteTermToGeneAssociations( GeneOntologyTermInfo term ) {
        List<Integer> geneIds = termsToGeneIds.remove( term );
        if ( geneIds != null ) {
            for ( Integer geneId : geneIds ) {
                geneIdsToTerms.get( geneId ).remove( term );
            }
        }
    }
}
