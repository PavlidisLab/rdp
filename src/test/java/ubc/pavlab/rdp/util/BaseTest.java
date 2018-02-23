package ubc.pavlab.rdp.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mjacobson on 14/02/18.
 */
public class BaseTest {


    protected void becomeUser( User user ) {
        UserPrinciple up = new UserPrinciple( user );
        Authentication authentication = mock( Authentication.class );
        SecurityContext securityContext = mock( SecurityContext.class );
        when( securityContext.getAuthentication() ).thenReturn( authentication );
        SecurityContextHolder.setContext( securityContext );
        when( SecurityContextHolder.getContext().getAuthentication().getPrincipal() ).thenReturn( up );
        when( SecurityContextHolder.getContext().getAuthentication().getName() ).thenReturn( user.getEmail() );
    }

    protected User createUnpersistedUser() {
        User user = new User();
        user.setEmail( "bruce@wayne.com" );
        user.setPassword( "$2a$10$ny8JDrJGVcf27xs7RqsHh.ytcFQYhXqr4vI9Kq57HE1tQgePfQXyC" ); // imbatman

        Profile profile = new Profile();
        profile.setName( "Bruce" );
        profile.setLastName( "Wayne" );
        user.setProfile( profile );

        return user;
    }

    protected User createUser( int id ) {
        User user = createUnpersistedUser();
        user.setId( id );

        return user;
    }

    protected Taxon createUnpersistedTaxon() {
        Taxon taxon = new Taxon();
        taxon.setActive( true );
        taxon.setCommonName( "Honey Bee" );
        taxon.setScientificName( "Apis mellifera" );
        taxon.setGeneUrl( "" );

        return taxon;
    }

    protected Taxon createTaxon( int id ) {
        Taxon taxon = createUnpersistedTaxon();
        taxon.setId( id );

        return taxon;
    }

    protected GeneOntologyTerm createTerm( String id ) {
        GeneOntologyTerm term = new GeneOntologyTerm();
        term.setGoId( id );
        term.setObsolete( false );

        return term;
    }

    protected GeneOntologyTerm createTermWithGene( String id, Gene gene ) {
        GeneOntologyTerm term = new GeneOntologyTerm();
        term.setGoId( id );
        term.setObsolete( false );

        term.getDirectGenes().add( gene );
        gene.getTerms().add( term );

        return term;
    }

    protected Gene createGene( int id, Taxon taxon ) {
        Gene gene = new Gene();
        gene.setGeneId( id );
        gene.setTaxon( taxon );

        return gene;
    }

    protected UserTerm createUserTerm( int id, GeneOntologyTerm term, Taxon taxon ) {
        UserTerm ut = new UserTerm(term, taxon, null );
        ut.setId( id );
        return ut;
    }

    protected UserGene createUserGene( int id, Gene gene, User user, TierType tier ) {
        UserGene ug = new UserGene( gene, user, tier );
        ug.setId( id );
        return ug;
    }

    protected String toGOId( int id ) {
        return String.format( "GO:%07d", id );
    }

}
