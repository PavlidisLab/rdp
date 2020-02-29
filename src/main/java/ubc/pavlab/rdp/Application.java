package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@CommonsLog
public class Application {

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }

}
