package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.net.InetSocketAddress;
import java.net.Proxy;

@CommonsLog
@Configuration
public class RemoteResourceConfig {

    @Bean
    public RestTemplate restTemplate( ApplicationSettings applicationSettings ) {
        String proxyHost = applicationSettings.getIsearch().getHost();
        Integer proxyPort = applicationSettings.getIsearch().getPort();
        if ( proxyHost != null && proxyPort != null && !proxyHost.equals( "" ) && !proxyPort.equals( "" ) ) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            Proxy proxy = new Proxy( Proxy.Type.HTTP, new InetSocketAddress( proxyHost, proxyPort ) );
            requestFactory.setProxy( proxy );
            return new RestTemplate( requestFactory );
        } else {
            return new RestTemplate();
        }
    }

}
