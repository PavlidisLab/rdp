package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.mockito.ArgumentMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mjacobson on 14/02/18.
 */
@SuppressWarnings("WeakerAccess")
public final class TestUtils {

    public static final String EMAIL = "bruce@wayne.com";
    public static final String ENCODED_PASSWORD = "$2a$10$ny8JDrJGVcf27xs7RqsHh.ytcFQYhXqr4vI9Kq57HE1tQgePfQXyC";
    public static final String PASSWORD = "imbatman";
    public static final String NAME = "Bruce";
    public static final String LAST_NAME = "Wayne";


    public static final String TAXON_COMMON_NAME = "Honey Bee";
    public static final String TAXON_SCIENTIFIC_NAME = "Apis mellifera";

    public static void becomeUser( User user ) {
        UserPrinciple up = new UserPrinciple( user );
        Authentication authentication = mock( Authentication.class );
        SecurityContext securityContext = mock( SecurityContext.class );
        when( securityContext.getAuthentication() ).thenReturn( authentication );
        SecurityContextHolder.setContext( securityContext );
        when( SecurityContextHolder.getContext().getAuthentication().getPrincipal() ).thenReturn( up );
        when( SecurityContextHolder.getContext().getAuthentication().getName() ).thenReturn( user.getEmail() );
    }

    public static Role createRole( int id, String role ) {
        Role r = new Role();
        r.setId( id );
        r.setRole( role );
        return r;
    }

    public static User createUnpersistedUser() {
        User user = new User();
        user.setEmail( EMAIL );
        user.setPassword( ENCODED_PASSWORD ); // imbatman

        Profile profile = new Profile();
        profile.setName( NAME );
        profile.setLastName( LAST_NAME );
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setShared( false );
        user.setProfile( profile );

        return user;
    }

    public static User createUser( int id ) {
        User user = createUnpersistedUser();
        user.setId( id );

        return user;
    }

    public static User createUserWithRole( int id, String... role ) {
        User user = createUnpersistedUser();
        user.setId( id );

        user.setRoles( Arrays.stream( role ).map( r -> createRole( r.length(), r ) ).collect( Collectors.toSet() ) );

        return user;
    }

    @SneakyThrows
    public static Taxon createTaxon( int id ) {
        Taxon taxon = new Taxon();
        taxon.setActive( true );
        taxon.setCommonName( TAXON_COMMON_NAME );
        taxon.setScientificName( TAXON_SCIENTIFIC_NAME );
        taxon.setGeneUrl( new URL( "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Caenorhabditis_elegans.gene_info.gz" ) );
        taxon.setId( id );

        return taxon;
    }

    public static GeneOntologyTerm createTerm( String id ) {
        GeneOntologyTerm term = new GeneOntologyTerm();
        term.setGoId( id );
        term.setObsolete( false );

        return term;
    }

    public static GeneOntologyTerm createTermWithGene( String id, GeneInfo... genes ) {
        GeneOntologyTerm term = new GeneOntologyTerm();
        term.setGoId( id );
        term.setObsolete( false );

        Arrays.stream( genes ).forEach( g -> {
            term.getDirectGenes().add( g );
            g.getTerms().add( term );
        } );

        return term;
    }

    public static GeneInfo createGene( int id, Taxon taxon ) {
        GeneInfo gene = new GeneInfo();
        gene.setGeneId( id );
        gene.setTaxon( taxon );

        return gene;
    }

    public static UserTerm createUserTerm( int id, User user, GeneOntologyTerm term, Taxon taxon ) {
        UserTerm ut = UserTerm.createUserTerm( user, term, taxon, null );
        ut.setId( id );
        return ut;
    }

    public static UserTerm createUserTerm( int id, User user, GeneOntologyTerm term, Taxon taxon, Set<Gene> genes ) {
        UserTerm ut = UserTerm.createUserTerm( user, term, taxon, genes );
        ut.setId( id );
        return ut;
    }

    public static UserGene createUserGene( int id, Gene gene, User user, TierType tier ) {
        UserGene ug = UserGene.createUserGeneFromGene( gene, user, tier, PrivacyLevelType.PRIVATE );
        ug.setId( id );
        return ug;
    }

    public static String toGOId( int id ) {
        return String.format( "GO:%07d", id );
    }

    @AllArgsConstructor
    private static class IsPrefixOfIgnoreCase extends ArgumentMatcher<String> {

        private String s;

        @Override
        public boolean matches( Object argument ) {
            return argument != null && s.toLowerCase().startsWith( ( (String) argument ).toLowerCase() );
        }
    }

    public static String isPrefixOfIgnoreCase( String prefix ) {
        return argThat( new IsPrefixOfIgnoreCase( prefix ) );
    }

    @AllArgsConstructor
    private static class IsSubstringOfIgnoreCase extends ArgumentMatcher<String> {

        private String s;

        @Override
        public boolean matches( Object argument ) {
            return argument != null && s.contains( (String) argument );
        }
    }

    public static String isSubstringOfIgnoreCase( String s ) {
        return argThat( new IsSubstringOfIgnoreCase( s ) );
    }

    public static OrganInfo createOrgan( String uberonId, String name, String description ) {
        OrganInfo oi = new OrganInfo();
        oi.setUberonId( uberonId );
        oi.setName( name );
        oi.setDescription( description );
        return oi;
    }

    public static UserOrgan createUserOrgan( User user, Organ organ ) {
        UserOrgan uo = new UserOrgan();
        uo.setUser( user );
        uo.setUberonId( ( organ.getUberonId() ) );
        uo.setName( ( organ.getName() ) );
        uo.setDescription( organ.getDescription() );
        return uo;
    }
}
