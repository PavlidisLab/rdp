package ubc.pavlab.rdp.server.biomartquery;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO Document Me
 * 
 * @author jleong
 * @version $Id$
 */
@XmlRootElement(name = "Dataset")
public class Dataset {

    @XmlAttribute
    public String name;

    // (optional): the config of the mart (if applicable)
    @XmlAttribute
    public String config;

    @XmlElement
    public List<Filter> Filter = new ArrayList<Filter>();

    @XmlElement
    public List<Attribute> Attribute = new ArrayList<Attribute>();

    public Dataset() {
    }

    public Dataset( String name ) {
        this.name = name;
    }

}
