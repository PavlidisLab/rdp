package ubc.pavlab.rdp.server.biomartquery;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id: Attribute.java,v 1.3 2013/01/25 02:59:20 anton Exp $
 */
@XmlRootElement(name = "Attribute")
class Attribute {

    @XmlAttribute
    public String name;

    public Attribute() {
    }

    public Attribute( String name ) {
        this.name = name;
    }

}
