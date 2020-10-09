package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubc.pavlab.rdp.settings.ApplicationSettings;

@CommonsLog
@Configuration
public class RemoteResourceConfig {

    @Bean
    public ResteasyClient resteasyClient( ApplicationSettings applicationSettings ) {
        String proxyHost = applicationSettings.getIsearch().getHost();
        String proxyPort = applicationSettings.getIsearch().getPort();
        if ( proxyHost != null && proxyPort != null &&
                !proxyHost.equals( "" ) && !proxyPort.equals( "" ) ) {
            log.info( "Using " + proxyHost + ":" + proxyPort + " as proxy for rest client." );
            return new ResteasyClientBuilder().defaultProxy( proxyHost, Integer.parseInt( proxyPort ) ).build();
        } else {
            log.info( "Using default proxy for rest client." );
            return new ResteasyClientBuilder().build();
        }
    }

}
