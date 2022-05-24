package ubc.pavlab.rdp.model.ontology;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OntologyTest {

    @Test
    public void builder() {
        Ontology ont = Ontology.builder( "uberon" ).build();
        assertThat( ont.getName() ).isEqualTo( "uberon" );
        assertThat( ont.getTerms() ).isEmpty();
        assertThat( ont.isActive() ).isFalse();
        assertThat( ont.getOrdering() ).isNull();
    }

}