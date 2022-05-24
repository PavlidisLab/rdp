package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ubc.pavlab.rdp.util.MatchType;

@Getter
@AllArgsConstructor
public enum OntologyTermMatchType implements MatchType {
    TERM_ID_EXACT( "Term ID", 0 ),
    ALT_ID_EXACT( "Term ID", 0 ),

    TERM_NAME_EXACT( "Term name", 1 ),
    TERM_NAME_STARTS_WITH( "Term name", 1 ),
    TERM_NAME_CONTAINS( "Term name", 1 ),
    TERM_NAME_MATCH( "Term name", 1 ),

    SYNONYM_EXACT( "Synonym", 2 ),
    SYNONYM_STARTS_WITH( "Synonym", 2 ),
    SYNONYM_CONTAINS( "Synonym", 2 ),
    SYNONYM_MATCH("Synonym", 2),

    DEFINITION_CONTAINS( "Definition", 3 ),
    DEFINITION_MATCH( "Definition", 3 );

    private final String label;
    private final int order;

    @JsonValue
    public String getLabel() {
        return label;
    }
    }
