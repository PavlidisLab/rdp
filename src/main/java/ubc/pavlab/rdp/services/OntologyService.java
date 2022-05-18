package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /**
 * This service combines both {@link OntologyTermInfoRepository} and {@link OntologyRepository}.
 *
 * @author poirgui
 */
@Service
@CommonsLog
public class OntologyService {

    private final OntologyRepository ontologyRepository;
    private final OntologyTermInfoRepository ontologyTermInfoRepository;

    @Autowired
    public OntologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository ) {
        this.ontologyRepository = ontologyRepository;
        this.ontologyTermInfoRepository = ontologyTermInfoRepository;
    }


    public Ontology findOntologyById( Integer id ) {
        return ontologyRepository.findOne( id );
    }

    public List<Ontology> findAllOntologies() {
        return ontologyRepository.findAllByActiveTrue().stream()
                .sorted( Ontology.getComparator() )
                .collect( Collectors.toList() );
    }

    public List<OntologyTermInfo> findAllByTermId( String termId ) {
        return ontologyTermInfoRepository.findByTermId( termId );
    }

    public List<OntologyTermInfo> findAllTerms() {
        return ontologyTermInfoRepository.findAllByActiveTrueAndOntologyActiveTrue().stream()
                .sorted( OntologyTermInfo.getComparator() )
                .collect( Collectors.toList() );
    }

    public OntologyTermInfo findByTermIdAndOntologyId( String termId, Integer ontologyId ) {
        return ontologyTermInfoRepository.findByTermIdAndOntologyId( termId, ontologyId );
    }

    @Transactional
    public void updateOntologies() {
        log.info( "Updating all active ontologies..." );
        for ( Ontology ontology : findAllOntologies() ) {
            if ( ontology.getOntologyUrl() != null ) {
                Resource resource = new UrlResource( ontology.getOntologyUrl() );
                log.info( "Updating " + ontology + " from " + resource + "..." );
                // TODO:
            }
        }
        log.info( "Ontologies have been successfully updated." );
    }

    @Transactional(readOnly = true)
    public void writeObo( Ontology ontology, Writer writer ) throws IOException {
        try ( OboWriter bw = new OboWriter( writer ) ) {
            bw.writeLine( "format-version: 1.2" );
            bw.writeLine( "ontology: " + ontology.getName() );
            for ( OntologyTermInfo term : ontology.getTerms() ) {
                bw.newLine();
                bw.writeLine( "[Term]" );
                bw.writeLine( "id: " + term.getTermId() );
                bw.writeLine( "name: " + term.getName() );
                if ( term.getDefinition() != null ) {
                    bw.writeLine( "def: " + '"' + term.getDefinition() + '"' + " []" );
                }
                for ( OntologyTermInfo superTerm : term.getSuperTerms() ) {
                    bw.writeLine( "is_a: " + superTerm.getTermId() + " ! " + superTerm.getName() );
                }
                if ( term.getOrdering() != null ) {
                    bw.writeLine( "relationship: has_order {order=" + '"' + term.getOrdering() + '"' + "}" );
                }
            }

            bw.newLine();
            bw.writeLine( "[Typedef]" );
            bw.writeLine( "id: has_order" );
            bw.writeLine( "name: has_order" );
            bw.writeLine( "def: " + '"' + "Term has relative order to other terms defined at the same subclass level." + '"' + " []" );
            bw.writeLine( "is_reflexive: true" );
            bw.writeLine( "is_metadata_tag: true" );
        }
    }

    @Transactional
    public void delete( List<Ontology> ontologies ) {
        ontologyRepository.delete( ontologies );
    }

    public long count() {
        return ontologyRepository.count();
    }

    private static class OboWriter extends BufferedWriter {
        public OboWriter( Writer out ) {
            super( out );
        }

        public void writeLine( String s ) throws IOException {
            write( s );
            newLine();
        }
    }

}
