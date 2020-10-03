package ubc.pavlab.rdp.util;

import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
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

    public static User createUserWithRoles( int id, Role... roles ) {
        User user = createUser( id );
        user.setRoles( Arrays.stream( roles ).collect( Collectors.toSet() ) );
        return user;
    }

    public static User createUserWithGenes( int id, Gene... genes ) {
        User user = createUser( id );
        Map<Integer, UserGene> userGenes = Arrays.stream( genes )
                .map( g -> createUnpersistedUserGene( g, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) )
                .collect( Collectors.toMap( UserGene::getGeneId, Function.identity() ) );
        user.getUserGenes().putAll( userGenes );
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

    public static GeneOntologyTerm createTermWithGenes( String id, GeneInfo... genes ) {
        GeneOntologyTerm term = createTerm( id );
        term.setDirectGeneIds( Arrays.stream( genes ).map( GeneInfo::getGeneId ).collect( Collectors.toSet() ) );
        term.setSizesByTaxonId( Arrays.stream( genes )
                .collect( groupingBy( g -> g.getTaxon().getId(), counting() ) ) );
        return term;
    }

    public static GeneInfo createGene( int id, Taxon taxon ) {
        GeneInfo gene = new GeneInfo();
        gene.setGeneId( id );
        gene.setTaxon( taxon );
        return gene;
    }

    public static UserTerm createUnpersistedUserTerm( User user, GeneOntologyTerm term, Taxon taxon ) {
        UserTerm userTerm = new UserTerm();
        userTerm.setGoId( term.getGoId() );
        userTerm.updateTerm( term );
        userTerm.setUser( user );
        userTerm.setTaxon( taxon );
        return userTerm;
    }

    public static UserTerm createUserTerm( int id, User user, GeneOntologyTerm term, Taxon taxon ) {
        UserTerm ut = createUnpersistedUserTerm( user, term, taxon );
        ut.setId( id );
        return ut;
    }

    public static UserGene createUserGene( int id, Gene gene, User user, TierType tier, PrivacyLevelType privacyLevelType ) {
        UserGene ug = createUnpersistedUserGene( gene, user, tier, privacyLevelType );
        ug.setId( id );
        return ug;
    }

    public static UserGene createUnpersistedUserGene( Gene gene, User user, TierType tier, PrivacyLevelType privacyLevelType ) {
        UserGene ug = new UserGene();
        ug.setUser( user );
        ug.setTier( tier );
        ug.setPrivacyLevel( privacyLevelType );
        ug.updateGene( gene );
        return ug;
    }

    public static String toGOId( int id ) {
        return String.format( "GO:%07d", id );
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
