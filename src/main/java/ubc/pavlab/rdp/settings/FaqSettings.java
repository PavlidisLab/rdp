package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import ubc.pavlab.rdp.util.Messages;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * Created by mjacobson on 22/01/18.
 */
@Configuration
@ConfigurationProperties(prefix = "rdp.faq")
@PropertySource("${rdp.settings.faq-file}")
@Data
public class FaqSettings {

    @Autowired
    private MessageSource messageSource;

    private LinkedHashMap<String, String> questions;
    private LinkedHashMap<String, String> answers;

    public MessageSourceResolvable getResolvableQuestion( String key ) {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.faq.questions." + key }, new Object[]{ Messages.SHORTNAME }, questions.get( key ) );
    }

    public MessageSourceResolvable getResolvableAnswer( String key ) {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.faq.answers." + key }, new Object[]{ Messages.SHORTNAME }, answers.get( key ) );
    }
}
