package ubc.pavlab.rdp.validation;

import lombok.Value;
import org.springframework.lang.Nullable;

@Value
public class Recaptcha {
    @Nullable
    String response;
    @Nullable
    String remoteIp;
}
