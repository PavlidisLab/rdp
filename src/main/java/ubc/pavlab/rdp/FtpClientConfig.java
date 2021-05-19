package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.text.MessageFormat;

@CommonsLog
@Configuration
public class FtpClientConfig {

    @Autowired
    private SiteSettings siteSettings;

    @Bean
    @Scope("prototype")
    public FTPClient ftpClient() {
        String proxyHost = siteSettings.getProxyHost();
        if ( proxyHost != null ) {
            int proxyPort = Integer.parseInt( siteSettings.getProxyPort() );
            log.info( MessageFormat.format( "Using HTTP proxy server: {0}:{1}", proxyHost, Integer.toString( proxyPort ) ) );
            return new FTPHTTPClient( proxyHost, proxyPort );
        } else {
            return new FTPClient();
        }
    }

}
