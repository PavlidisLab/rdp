package ubc.pavlab.rdp.server.model;

import java.io.Serializable;

import org.directwebremoting.annotations.DataTransferObject;
import org.eclipse.jdt.internal.compiler.util.Util.Displayable;

/**
 * @author anton
 */
@DataTransferObject(javascript = "GenomicRange")
public class GenomicRange implements Displayable, Serializable, Comparable<GenomicRange> {
    private static final long serialVersionUID = 6917870790522866428L;

    private String bandEnd;
    private String bandStart;
    private String bandString;
    private int baseEnd;
    private int baseStart;

    private int bin;

    private String chromosome;

    public GenomicRange( String chromosome ) {
        this( chromosome, 0, GenomeBin.BINRANGE_MAXEND_512M );
    }

    public GenomicRange( String chromosome, int start, int end ) {
        assert start <= end;
        this.chromosome = chromosome;
        this.baseStart = Math.min( start, end );
        this.baseEnd = Math.max( start, end );
        this.bin = GenomeBin.binFromRange( chromosome, start, end );
    }

    public GenomicRange( String chromosome, String bandStart, String bandEnd ) {
        this.chromosome = chromosome;
        this.bandStart = bandStart;
        this.bandEnd = bandEnd;
    }

    GenomicRange() {
    }

    @Override
    public int compareTo( GenomicRange genomicRange ) {
        int myChromosomeIndex = getChromosomeIndex( this.chromosome );
        int otherChromosomeIndex = getChromosomeIndex( genomicRange.getChromosome() );
        if ( myChromosomeIndex == otherChromosomeIndex ) {
            return this.baseStart - genomicRange.getBaseStart();
        }
        return myChromosomeIndex - otherChromosomeIndex;

    }

    public String getBandEnd() {
        return bandEnd;
    }

    public String getBandStart() {
        return bandStart;
    }

    public String getBandString() {
        return this.bandString;
    }

    public int getBaseEnd() {
        return baseEnd;
    }

    public int getBaseStart() {
        return baseStart;
    }

    public int getBin() {
        return bin;
    }

    public String getChromosome() {
        return chromosome;
    }

    // @Override
    public String getHtmlLabel() {
        return toString();
    }

    // @Override
    public String getLabel() {
        return toString();
    }

    // @Override
    public String getTooltip() {
        return toString();
    }

    public void initBandCoordinates() {
        this.bandString = this.toCytobandString();
    }

    public boolean isWithin( GenomicRange other ) {
        if ( chromosome.equals( other.getChromosome() ) ) {
            if ( baseStart >= other.getBaseStart() && baseEnd <= other.getBaseEnd() ) {
                return true;
            }
        }
        return false;
    }

    public void setBandEnd( String bandEnd ) {
        this.bandEnd = bandEnd;
    }

    public void setBandStart( String bandStart ) {
        this.bandStart = bandStart;
    }

    public void setBaseEnd( int baseEnd ) {
        this.baseEnd = baseEnd;
    }

    public void setBaseStart( int baseStart ) {
        this.baseStart = baseStart;
    }

    public void setChromosome( String chromosome ) {
        this.chromosome = chromosome;
    }

    public String toBaseString() {
        String string = "";
        if ( chromosome != null ) {
            string += chromosome;
            if ( baseStart != 0 ) {
                string += ":" + baseStart;
                if ( baseEnd != 0 ) {
                    string += "-" + baseEnd;
                }
            }
        }
        return string;
    }

    public String toCytobandString() {
        String string = "";
        if ( chromosome != null ) {
            string += chromosome;
            if ( bandStart.equals( bandEnd ) ) {
                string += bandStart;
            } else {
                string += bandStart;
                if ( bandEnd != null ) {
                    string += "-" + bandEnd;
                }
            }
        }
        return string;
    }

    @Override
    public String toString() {
        if ( this.bandStart != null ) {
            return toCytobandString();
        }
        return toBaseString();

    }

    private int getChromosomeIndex( String chr ) {
        if ( chr.equals( "X" ) ) {
            return 22;
        } else if ( chr.equals( "Y" ) ) {
            return 23;
        } else {
            return Integer.parseInt( chr );
        }
    }

    @Override
    public String displayString( Object arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }
}