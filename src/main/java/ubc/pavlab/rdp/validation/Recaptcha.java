package ubc.pavlab.rdp.validation;

import lombok.Data;
import lombok.Value;

@Value
public class Recaptcha {
    String response;
    String remoteIp;
}
