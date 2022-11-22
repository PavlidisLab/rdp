package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.AsyncRestTemplate;
import ubc.pavlab.rdp.settings.ApplicationSettings;

@CommonsLog
@Configuration
public class RemoteResourceConfig {

    @Bean
    public AsyncRestTemplate remoteResourceRestTemplate( ApplicationSettings applicationSettings ) {
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        httpRequestFactory.setTaskExecutor( new SimpleAsyncTaskExecutor() );
        if ( applicationSettings.getIsearch().getRequestTimeout() != null ) {
            log.warn( "The 'rdp.settings.isearch.request-timeout' configuration is deprecated, use 'rdp.settings.isearch.connect-timeout' and 'rdp.settings.isearch.read-timeout' instead." );
            httpRequestFactory.setConnectTimeout( 1000 );
            httpRequestFactory.setReadTimeout( (int) applicationSettings.getIsearch().getRequestTimeout().toMillis() );
        } else {
            if ( applicationSettings.getIsearch().getConnectTimeout() != null ) {
                httpRequestFactory.setConnectTimeout( (int) applicationSettings.getIsearch().getConnectTimeout().toMillis() );
            }
            if ( applicationSettings.getIsearch().getReadTimeout() != null ) {
                httpRequestFactory.setReadTimeout( (int) applicationSettings.getIsearch().getReadTimeout().toMillis() );
            }
        }
        return new AsyncRestTemplate( httpRequestFactory );
    }

}
