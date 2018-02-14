package ubc.pavlab.rdp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Application {

    private static Log log = LogFactory.getLog( Application.class );

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }

}
