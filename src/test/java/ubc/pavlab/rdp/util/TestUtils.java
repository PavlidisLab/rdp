package ubc.pavlab.rdp.util;

import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mjacobson on 14/02/18.
 */
@SuppressWarnings("WeakerAccess")
public final class TestUtils {

    public static final String EMAIL_FORMAT = "bruce%d@wayne.com";
    public static final String ENCODED_PASSWORD = "$2a$10$ny8JDrJGVcf27xs7RqsHh.ytcFQYhXqr4vI9Kq57HE1tQgePfQXyC";
    public static final String PASSWORD = "imbatman";
    public static final String NAME = "Bruce";
    public static final String LAST_NAME = "Wayne";

    public static final String TAXON_COMMON_NAME = "Honey Bee";
    public static final String TAXON_SCIENTIFIC_NAME = "Apis mellifera";

    private static int emailCount = 0;

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
        Profile profile = Profile.builder()
                .name( NAME )
                .lastName( LAST_NAME )
                .privacyLevel( PrivacyLevelType.PUBLIC )
                .shared( false )
                .hideGenelist( false )
                .contactEmailVerified( false ).build();
        return User.builder()
                .email( String.format( EMAIL_FORMAT, emailCount++ ) )
                .password( ENCODED_PASSWORD ) // imbatman
                .enabled( false )
                .profile( profile ).build();
    }

    public static User createUser( int id ) {
        User user = createUnpersistedUser();
        user.setId( id );

        return user;
    }

    public static User createUserWithRoles( int id, Role... roles ) {
        User user = createUser( id );
        user.getRoles().addAll( Arrays.stream( roles ).collect( Collectors.toSet() ) );
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

    public static User createRemoteUser( int id, URI originUri ) {
        User user = createUser( id );
        user.setOriginUrl( originUri );
        return user;
    }

    public static User createAnonymousUser() {
        return User.builder()
                .anonymousId( UUID.randomUUID() )
                .profile( new Profile() )
                .build();
    }

    public static User createAnonymousRemoteUser( URI originUrl ) {
        User user = createAnonymousUser();
        user.setOriginUrl( originUrl );
        return user;
    }

    @SneakyThrows
    public static Taxon createTaxon( int taxonId ) {
        return createTaxon( taxonId, TAXON_COMMON_NAME, TAXON_SCIENTIFIC_NAME, new URL( "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Caenorhabditis_elegans.gene_info.gz" ) );
    }

    public static Taxon createTaxon( int id, String commonName, String scientificName, URL geneUrl ) {
        Taxon taxon = new Taxon();
        taxon.setId( id );
        taxon.setActive( true );
        taxon.setCommonName( commonName );
        taxon.setScientificName( scientificName );
        taxon.setGeneUrl( geneUrl );
        return taxon;
    }

    public static GeneOntologyTermInfo createTerm( String id ) {
        GeneOntologyTermInfo term = new GeneOntologyTermInfo();
        term.setGoId( id );
        term.setObsolete( false );
        return term;
    }

    public static GeneOntologyTermInfo createTerm( int id ) {
        return createTerm( String.format( "GO:%05d", id ) );
    }

    public static GeneOntologyTermInfo createTermWithGenes( String goId, GeneInfo... genes ) {
        GeneOntologyTermInfo term = createTerm( goId );
        term.setDirectGeneIds( Arrays.asList( genes ).stream().map( GeneInfo::getGeneId ).collect( Collectors.toSet() ) );
        MultiValueMap<Integer, Integer> m = new LinkedMultiValueMap();
        for ( GeneInfo geneInfo : genes ) {
            m.add( geneInfo.getTaxon().getId(), geneInfo.getGeneId() );
        }
        term.setDirectGeneIdsByTaxonId( m );
        return term;
    }

    public static GeneInfo createGene( int id, Taxon taxon ) {
        GeneInfo gene = new GeneInfo();
        gene.setGeneId( id );
        gene.setTaxon( taxon );
        gene.setSymbol( "BRCA1" );
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
        oi.setActive( false );
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

    public static PasswordResetToken createPasswordResetToken( User user, String token ) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser( user );
        t.updateToken( token );
        return t;
    }

    public static VerificationToken createVerificationToken( User user, String token ) {
        VerificationToken t = new VerificationToken();
        t.setUser( user );
        t.setEmail( user.getEmail() );
        t.updateToken( token );
        return t;
    }

    public static VerificationToken createContactEmailVerificationToken( User user, String token ) {
        VerificationToken t = new VerificationToken();
        t.setUser( user );
        t.setEmail( user.getProfile().getContactEmail() );
        t.setToken( token );
        return t;
    }

    public static AccessToken createAccessToken( Integer id, User user, String token ) {
        AccessToken accessToken = new AccessToken();
        accessToken.setId( id );
        accessToken.setUser( user );
        accessToken.updateToken( token );
        return accessToken;
    }
}
