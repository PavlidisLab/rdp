package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TierType;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("goService")
public class GOServiceImpl implements GOService {

    private static Log log = LogFactory.getLog( GOServiceImpl.class );

    private Map<String, GeneOntologyTerm> termMap = new HashMap<>();

    private static Map<GeneOntologyTerm, Collection<GeneOntologyTerm>> descendantsCache = new HashMap<>();

    private static int GO_SIZE_LIMIT = 100;

    @Override
    public void setTerms( Map<String, GeneOntologyTerm> termMap ) {
        this.termMap = termMap;
    }

    @Override
    public Collection<GeneOntologyTerm> getAllTerms() {
        return termMap.values();
    }

    @Override
    public int size() {
        return termMap.size();
    }

    @Override
    public Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon, Set<Gene> genes ) {
        List<UserTerm> newTerms = new ArrayList<>();
        for ( GeneOntologyTerm goTerm : goTerms ) {
            UserTerm term = new UserTerm();
            term.setTerm( goTerm );
            term.setTaxon( taxon );
            if ( taxon != null ) {
                term.setTaxon( taxon );
                term.setSize( goTerm.getSize( taxon ).intValue() );
            } else {
                term.setSize( goTerm.getSize().intValue() );
            }

            if ( term.getSize() < GO_SIZE_LIMIT ) {
                if ( genes != null ) {
                    term.setFrequency( computeOverlapFrequency( goTerm, genes ) );
                }
                newTerms.add( term );
            }

        }
        return newTerms;
    }

    @Override
    public List<GeneOntologyTerm> recommendTerms( Collection<Gene> genes ) {
        return new ArrayList<>( calculateGoTermFrequency( genes, 2, 10, 100 ).keySet() );
    }

    private LinkedHashMap<GeneOntologyTerm, Integer> calculateGoTermFrequency( Collection<Gene> genes, int minimumFrequency,
                                                                     int minimumTermSize, int maximumTermSize ) {
        Map<GeneOntologyTerm, Integer> frequencyMap = new HashMap<>();
        for ( Gene g : genes ) {
            for ( GeneOntologyTerm term : getGOTerms( g, true, true ) ) {
                // Count
                frequencyMap.merge( term, 1, ( oldValue, one ) -> oldValue + one );
            }
        }

        log.debug( "Calculate overlaps for each term with propagation done." );
        // Limit by minimum frequency
        if ( minimumFrequency > 0 ) {
            frequencyMap.entrySet().removeIf( termEntry -> termEntry.getValue() < minimumFrequency );
        }
        log.debug( "Limit by min freq done." );

        // Limit by maximum gene pool size of go terms
        if ( maximumTermSize > 0 && minimumTermSize > 0 ) {
            // TODO: Restrict by size in taxon only?
            frequencyMap.entrySet().removeIf( termEntry -> termEntry.getKey().getSize() < minimumTermSize || termEntry.getKey().getSize() > maximumTermSize );
        }
        log.debug( "Limit by size done." );

        // Reduce GO TERM redundancies
        frequencyMap = reduceRedundancies( frequencyMap );

        log.debug( "Reduce redundancy done." );

        return frequencyMap.entrySet().stream()
                .sorted( Map.Entry.<GeneOntologyTerm, Integer>comparingByValue().reversed() ).
                        collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue,
                                ( e1, e2 ) -> e1, LinkedHashMap::new ) );
    }

    private Map<GeneOntologyTerm, Integer> reduceRedundancies( Map<GeneOntologyTerm, Integer> frequencyMap ) {
        // Reduce GO TERM redundancies
        Collection<GeneOntologyTerm> markedForDeletion = new HashSet<>();
        Map<GeneOntologyTerm, Integer> fmap = new HashMap<>( frequencyMap );
        for ( Map.Entry<GeneOntologyTerm, Integer> entry : fmap.entrySet() ) {
            GeneOntologyTerm term = entry.getKey();
            Integer frequency = entry.getValue();

            if ( markedForDeletion.contains( term ) ) {
                // Skip because a previous step would have taken care of this terms parents anyways
                continue;
            }

            Collection<GeneOntologyTerm> parents = getAncestors( term, true );

            for ( GeneOntologyTerm parent : parents ) {
                if ( fmap.containsKey( parent ) ) {
                    if ( fmap.get( parent ) > frequency ) {
                        // keep parent
                        markedForDeletion.add( term );
                        break;
                    } else {
                        // keep child
                        markedForDeletion.add( parent );
                    }
                }
            }

        }

        // Delete those marked for deletion
        for ( GeneOntologyTerm redundantTerm : markedForDeletion ) {
            fmap.remove( redundantTerm );
        }

        return fmap;
    }

    @Override
    public List<GeneOntologyTerm> search( String queryString ) {
        //TODO: Rewrite this whole thing
        if ( queryString == null ) return new ArrayList<>();

        // ArrayList<GeneOntologyTerm> results = new ArrayList<GeneOntologyTerm>();
        Map<GeneOntologyTerm, Integer> results = new HashMap<>();
        // log.info( "search: " + queryString );
        for ( GeneOntologyTerm term : termMap.values() ) {
            if ( queryString.equals( term.getId() ) || ("GO:" + queryString).equals( term.getId() ) ) {
                results.put( term, 1 );
                continue;
            }

            String pattern = "(?i:.*" + Pattern.quote( queryString ) + ".*)";
            // Pattern r = Pattern.compile(pattern);
            String m = term.getName();
            // Matcher m = r.matcher( term.getTerm() );
            if ( m.matches( pattern ) ) {
                results.put( term, 2 );
                continue;
            }

            m = term.getDefinition();
            // m = r.matcher( term.getDefinition() );
            if ( m.matches( pattern ) ) {
                results.put( term, 3 );
                continue;
            }

            String[] queryList = queryString.split( " " );
            for ( String q : queryList ) {
                pattern = "(?i:.*" + Pattern.quote( q ) + ".*)";
                // r = Pattern.compile(pattern);
                m = term.getName();
                // m = r.matcher( term.getTerm() );

                if ( m.matches( pattern ) ) {
                    results.put( term, 4 );
                    break;
                }

            }

            if ( results.containsKey( term ) ) continue;

            for ( String q : queryList ) {
                pattern = "(?i:.*" + Pattern.quote( q ) + ".*)";
                // r = Pattern.compile(pattern);
                // m = r.matcher( term.getDefinition() );
                m = term.getDefinition();
                if ( m.matches( pattern ) ) {
                    results.put( term, 5 );
                    break;
                }

            }

        }

        // log.info( "search result size " + results.size() );

        // Now we have a set of terms with how well they match
        return results.entrySet().stream()
                .sorted( Map.Entry.comparingByValue() )
                .map( Map.Entry::getKey ).collect( Collectors.toList() );
    }

    @Override
    public Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry ) {
        return getChildren( entry, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry, boolean includePartOf ) {
        return entry.getChildren().stream()
                .filter( r -> includePartOf || r.getType().equals( RelationshipType.IS_A ) )
                .map( Relationship::getTerm )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneOntologyTerm> getParents( GeneOntologyTerm entry ) {
        return getParents( entry, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getParents( GeneOntologyTerm entry, boolean includePartOf ) {
        return entry.getParents().stream()
                .filter( r -> includePartOf || r.getType().equals( RelationshipType.IS_A ) )
                .map( Relationship::getTerm )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry ) {
        return getDescendants( entry, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry, boolean includePartOf ) {

        Collection<GeneOntologyTerm> descendants = descendantsCache.get( entry );

        if ( descendants == null ) {
            descendants = new HashSet<>();

            for ( GeneOntologyTerm child : getChildren( entry, includePartOf ) ) {
                descendants.add( child );
                descendants.addAll( getDescendants( child, includePartOf ) );
            }

            descendants = Collections.unmodifiableCollection( descendants );

            descendantsCache.put( entry, descendants );

        }

        return new HashSet<>( descendants );
    }

    @Override
    public Collection<GeneOntologyTerm> getAncestors( GeneOntologyTerm entry ) {
        return getAncestors( entry, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getAncestors( GeneOntologyTerm entry, boolean includePartOf ) {
        Collection<GeneOntologyTerm> ancestors = new HashSet<>();

        for ( GeneOntologyTerm parent : getParents( entry, includePartOf ) ) {
            ancestors.add( parent );
            ancestors.addAll( getAncestors( parent, includePartOf ) );
        }

        ancestors = Collections.unmodifiableCollection( ancestors );

        return new HashSet<>( ancestors );
    }

    @Override
    public Collection<Gene> getGenes( String id, Taxon taxon ) {
        return getGenes( termMap.get( id ), taxon );
    }

    @Override
    public Collection<Gene> getGenes( GeneOntologyTerm t, Taxon taxon ) {
        if ( t == null ) return null;
        Collection<GeneOntologyTerm> descendants = getDescendants( t );

        descendants.add( t );

        return descendants.stream().flatMap(term -> term.getGenes().stream()).filter(g -> g.getTaxon().equals( taxon )).collect(Collectors.toSet());
    }

    @Override
    public Collection<GeneOntologyTerm> getGOTerms( Gene gene ) {
        return getGOTerms( gene, true, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getGOTerms( Gene gene, boolean includePartOf ) {
        return getGOTerms( gene, includePartOf, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getGOTerms( Gene gene, boolean includePartOf, boolean propagateUpwards ) {

        Collection<GeneOntologyTerm> allGOTermSet = new HashSet<>();

        for (GeneOntologyTerm term : gene.getTerms()) {
            allGOTermSet.add( term );

            if ( propagateUpwards ) {
                allGOTermSet.addAll( getAncestors( term, includePartOf ) );
            }
        }

        return Collections.unmodifiableCollection( allGOTermSet );
    }

    @Override
    public Collection<UserGene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Taxon taxon ) {
        Collection<UserGene> results = new HashSet<>();

        for ( GeneOntologyTerm term : goTerms ) {
            results.addAll( getGenes( termMap.get( term.getId() ), taxon ).stream().map(g -> new UserGene( g, TierType.TIER3 )).collect( Collectors.toSet()) );
        }

        return results;
    }

    @Override
    public Integer computeOverlapFrequency( GeneOntologyTerm t, Set<Gene> genes ) {
        Integer frequency = 0;
        for ( Gene g : genes ) {
            Collection<GeneOntologyTerm> directTerms = getGOTerms( g, true, true );

            for ( GeneOntologyTerm term : directTerms ) {
                if ( term.equals( t ) ) {
                    frequency++;
                    // Can break because a gene cannot (and shouldn't) have duplicate terms
                    break;
                }
            }

        }
        return frequency;
    }

    @Override
    public GeneOntologyTerm getTerm( String goId ) {
        return termMap.get( goId );
    }

}
