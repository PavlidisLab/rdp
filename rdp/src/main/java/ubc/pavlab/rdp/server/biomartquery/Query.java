package ubc.pavlab.rdp.server.biomartquery;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO Document Me
 * 
 * @author ??
 * @version $Id: Query.java,v 1.4 2013/02/05 23:39:41 frances Exp $
 */
@XmlRootElement(name = "Query")
public class Query {

    // name of the client making the call
    @XmlAttribute
    public String client = "ASPIREdb";

    // processor name (e.g. TSV, JSON)
    @XmlAttribute
    public String processor = "TSV";

    // the number of rows to return (-1 for no limit)
    @XmlAttribute
    public String limit = "-1";

    // if set to 1 then first row of results will be column headers
    @XmlAttribute
    public String header = "0";

    @XmlAttribute
    public String uniqueRows = "1";

    @XmlElement
    public Dataset Dataset;

}
