package ubc.pavlab.rdp.server.model;

import java.io.Serializable;

import org.directwebremoting.annotations.DataTransferObject;
import org.eclipse.jdt.internal.compiler.util.Util.Displayable;

/**
 * TODO Document Me
 * 
 * @author Paul/jleong
 * @version $Id: GeneValueObject.java,v 1.10 2013/06/11 22:30:57 anton Exp $
 */
@DataTransferObject(javascript = "GeneValueObject")
public class GeneValueObject implements Displayable, Serializable {
    private static final long serialVersionUID = -7411514301896256147L;

    private String key;
    private String symbol;
    private String name;
    private String taxon;
    private String ensemblId;
    private String linkToGemma;
    private String geneBioType;

    private GenomicRange genomicRange;

    public GeneValueObject() {
    }

    public GeneValueObject( String ensemblId, String symbol, String geneName, String gene_biotype, String taxon ) {
        this.ensemblId = ensemblId;
        this.symbol = symbol;
        this.name = geneName;
        this.geneBioType = gene_biotype;
        this.taxon = taxon;
        this.key = symbol + ":" + taxon;
    }

    public String getEnsemblId() {
        return ensemblId;
    }

    public String getGeneBioType() {
        return geneBioType;
    }

    public GenomicRange getGenomicRange() {
        return this.genomicRange;
    }

    /*
     * @Override public String getHtmlLabel() { return "<b>" + getLabel() + "</b>: " + name; }
     */

    public String getKey() {
        return key;
    }

    // @Override
    public String getLabel() {
        return symbol.equals( "" ) ? ensemblId : symbol;
    }

    public String getLinkToGemma() {
        return linkToGemma;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTaxon() {
        return taxon;
    }

    /*
     * @Override public String getTooltip() { String ret = getLabel() + ": " + name; if ( this.genomicRange != null ) {
     * ret += " - " + this.genomicRange.toString(); } return ret; }
     */

    public void setEnsemblId( String ensemblId ) {
        this.ensemblId = ensemblId;
    }

    public void setGeneBioType( String geneBioType ) {
        this.geneBioType = geneBioType;
    }

    public void setGenomicRange( GenomicRange genomicRange ) {
        this.genomicRange = genomicRange;
    }

    public void setKey( String key ) {
        this.key = key;
    }

    public void setLinkToGemma( String linkToGemma ) {
        this.linkToGemma = linkToGemma;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setSymbol( String symbol ) {
        this.symbol = symbol;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    @Override
    public String displayString( Object arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }
}
