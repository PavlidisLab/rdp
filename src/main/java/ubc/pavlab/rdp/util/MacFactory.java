package ubc.pavlab.rdp.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.crypto.Mac;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Factory for {@link Mac} instances.
 *
 * @author poirigui
 */
@Getter
@Setter
public class MacFactory implements InitializingBean {

    private String algorithm;

    private Key key;

    private AlgorithmParameterSpec algorithmParameterSpec;

    public Mac createMac() {
        try {
            Mac instance = Mac.getInstance( algorithm );
            if ( algorithmParameterSpec != null ) {
                instance.init( key, algorithmParameterSpec );
            } else {
                instance.init( key );
            }
            return instance;
        } catch ( NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull( algorithm, "The algorithm must be set." );
        Assert.notNull( key, "The key must be set." );
        Mac.getInstance( algorithm );
    }
}
