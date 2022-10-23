package ubc.pavlab.rdp.settings;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import ubc.pavlab.rdp.util.Messages;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 22/01/18.
 */
@Configuration("faqSettings")
@ConfigurationProperties(prefix = "rdp.faq")
@PropertySource("${rdp.settings.faq-file}")
@Data
@CommonsLog
public class FaqSettings implements InitializingBean {

    @Autowired
    private MessageSource messageSource;

    @Value("${rdp.settings.faq-file}")
    private Resource faqFile;

    private List<String> keys;
    private Map<String, String> questions;
    private Map<String, String> answers;

    @Override
    public void afterPropertiesSet() {
        if ( keys == null ) {
            log.warn( "The 'faq.keys' is unset, will default to the question keys in alphabetic order." );
            keys = questions.keySet().stream().sorted().collect( Collectors.toList() );
        }
        List<String> missingEntries = new ArrayList<>();
        for ( String key : keys ) {
            if ( !questions.containsKey( key ) ) {
                missingEntries.add( "faq.questions." + key );
            }
            if ( !answers.containsKey( key ) ) {
                missingEntries.add( "faq.answers." + key );
            }
        }
        Assert.isTrue( missingEntries.isEmpty(), String.format( "The following entries are missing in %s: %s.",
                faqFile,
                String.join( ", ", missingEntries ) ) );
    }

    public MessageSourceResolvable getResolvableQuestion( String key ) {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.faq.questions." + key }, new Object[]{ Messages.SHORTNAME }, questions.get( key ) );
    }

    public MessageSourceResolvable getResolvableAnswer( String key ) {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.faq.answers." + key }, new Object[]{ Messages.SHORTNAME }, answers.get( key ) );
    }
}
