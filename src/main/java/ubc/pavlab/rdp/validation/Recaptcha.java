package ubc.pavlab.rdp.validation;

import lombok.Value;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Value
public class Recaptcha {
    String response;
    @Nullable
    String remoteIp;
}
