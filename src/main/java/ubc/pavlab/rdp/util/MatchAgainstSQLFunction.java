package ubc.pavlab.rdp.util;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.util.List;

/**
 * Custom match(...) against(...) SQL function used for full-text searches.
 * <p>
 * The HQL syntax only supports a single column being matched in boolean mode. Two arguments are expected: the column
 * name and the query and returns a double that indicates the similarity between the query sequence and the matched
 * documents.
 * <p>
 * Reference: <a href="https://dev.mysql.com/doc/refman/5.6/en/fulltext-search.html">12.10 Full-Text Search Functions</a>
 *
 * @author poirigui
 */
public class MatchAgainstSQLFunction implements SQLFunction {

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasParenthesesIfNoArguments() {
        return false;
    }

    @Override
    public Type getReturnType( Type type, Mapping mapping ) {
        return StandardBasicTypes.DOUBLE;
    }

    @Override
    public String render( Type type, List list, SessionFactoryImplementor sessionFactoryImplementor ) throws QueryException {
        if ( list.size() < 2 || list.size() > 3 ) {
            throw new QueryException( "The match() function expects 2 parameters." );
        }
        return "match" +
                '(' + list.get( 0 ) + ')' +
                ' ' +
                "against" +
                '(' + list.get( 1 ) + ' ' + "in boolean mode" + ')';
    }
}
