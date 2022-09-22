package ubc.pavlab.rdp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordReset {

    @NotNull(message = "New password must be provided.")
    @Size(min = 6, message = "New password must have at least 6 characters.")
    private String newPassword;

    @NotNull(message = "Password confirmation must be provided.")
    private String passwordConfirm;

    public boolean isValid() {
        return this.newPassword.equals( this.passwordConfirm );
    }
}
