package ubc.pavlab.rdp.server.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.directwebremoting.annotations.DataTransferObject;
import org.eclipse.jdt.internal.compiler.util.Util.Displayable;

/**
 * Deprecated: Please use Gene.java instead
 * 
 * @author Paul/jleong
 * @version $Id: GeneValueObject.java,v 1.10 2013/06/11 22:30:57 anton Exp $
 */
@Deprecated
@DataTransferObject(javascript = "GeneValueObject")
public class GeneValueObject implements Displayable, Serializable {
    private static final long serialVersionUID = -7411514301896256147L;

    private String key;
    private String officialSymbol;
    private String officialName;
    private String taxon;
    private String ensemblId;
    private String linkToGemma;
    private String geneBioType;
    private String ncbiGeneId;
    private Set<String> aliases = new HashSet<>();

    private GenomicRange genomicRange;

    public GeneValueObject() {
    }

    public GeneValueObject( String ensemblId, String symbol, String geneName, String gene_biotype, String taxon ) {
        this.ensemblId = ensemblId;
        this.officialSymbol = symbol;
        this.officialName = geneName;
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
        return officialSymbol.equals( "" ) ? ensemblId : officialSymbol;
    }

    public String getLinkToGemma() {
        return linkToGemma;
    }

    public String getOfficialName() {
        return officialName;
    }

    public String getOfficialSymbol() {
        return officialSymbol;
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

    public void setOfficialName( String officialName ) {
        this.officialName = officialName;
    }

    public void setOfficialSymbol( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    @Override
    public String displayString( Object arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNcbiGeneId() {
        return ncbiGeneId;
    }

    public void setNcbiGeneId( String ncbiGeneId ) {
        this.ncbiGeneId = ncbiGeneId;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases( Set<String> aliases ) {
        this.aliases = aliases;
    }
}
