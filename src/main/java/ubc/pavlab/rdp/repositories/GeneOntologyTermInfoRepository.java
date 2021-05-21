package ubc.pavlab.rdp.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;

import java.util.*;

@Repository
public class GeneOntologyTermInfoRepository implements CrudRepository<GeneOntologyTermInfo, String> {

    private Map<String, GeneOntologyTermInfo> terms = new HashMap<>();

    private MultiValueMap<Integer, GeneOntologyTermInfo> geneIdsToTerms = new LinkedMultiValueMap<>();

    @Override
    public <S extends GeneOntologyTermInfo> S save( S term ) {
        return saveAlias( term.getGoId(), term );
    }

    @Override
    public <S extends GeneOntologyTermInfo> Iterable<S> save( Iterable<S> iterable ) {
        for ( GeneOntologyTermInfo term : iterable ) {
            save( term );
        }
        // TODO: implement return value
        return null;
    }

    public <S extends GeneOntologyTermInfo> S saveAlias( String alias, S term ) {
        terms.put( alias, term );
        for ( Gene gene : term.getDirectGenes() ) {
            geneIdsToTerms.add( gene.getGeneId(), term );
        }
        return term;
    }

    public <S extends GeneOntologyTermInfo> Iterable<S> saveAlias( Map<String, GeneOntologyTermInfo> terms ) {
        for ( Map.Entry<String, GeneOntologyTermInfo> entry : terms.entrySet() ) {
            saveAlias( entry.getKey(), entry.getValue() );
        }
        // TODO: implement return value
        return null;
    }

    @Override
    public GeneOntologyTermInfo findOne( String s ) {
        return terms.get( s );
    }

    @Override
    public boolean exists( String s ) {
        return terms.containsKey( s );
    }

    @Override
    public Collection<GeneOntologyTermInfo> findAll() {
        return terms.values();
    }

    @Override
    public Collection<GeneOntologyTermInfo> findAll( Iterable<String> iterable ) {
        Collection<GeneOntologyTermInfo> results = new HashSet<>();
        for ( String goTerm : iterable ) {
            results.add( findOne( goTerm ) );
        }
        return results;
    }

    public Collection<GeneOntologyTermInfo> findAllByGene( Gene gene ) {
        return geneIdsToTerms.getOrDefault( gene.getGeneId(), Collections.emptyList() );
    }

    @Override
    public long count() {
        return terms.values().stream().distinct().count();
    }

    @Override
    public void delete( String s ) {
        terms.remove( s );
    }

    @Override
    public void delete( GeneOntologyTermInfo term ) {
        terms.remove( term.getGoId() );
    }

    @Override
    public void delete( Iterable<? extends GeneOntologyTermInfo> iterable ) {
        for ( GeneOntologyTermInfo term : iterable ) {
            delete( term );
        }
    }

    @Override
    public void deleteAll() {
        terms.clear();
        geneIdsToTerms.clear();
    }
}
