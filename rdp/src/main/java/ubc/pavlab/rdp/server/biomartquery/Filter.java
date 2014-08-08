package ubc.pavlab.rdp.server.biomartquery;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO Document Me
 * 
 * @author ??
 * @version $Id: Filter.java,v 1.4 2013/01/25 02:59:19 anton Exp $
 */
@XmlRootElement(name = "Filter")
class Filter {

    @XmlAttribute
    public String name;

    @XmlAttribute
    public String value;

    public Filter() {
    }

    public Filter( String name, String value ) {
        this.name = name;
        this.value = value;
    }

}
