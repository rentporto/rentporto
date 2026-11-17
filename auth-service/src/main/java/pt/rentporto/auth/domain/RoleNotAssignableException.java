package pt.rentporto.auth.domain;

import java.util.Set;
import pt.rentporto.web.BusinessException;

/**
 * A staff account was requested with a role an administrator cannot give: CUSTOMER or ADMIN (422).
 */
public class RoleNotAssignableException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param roles the roles that cannot be given
     */
    public RoleNotAssignableException(Set<Role> roles) {
        super("ROLE_NOT_ASSIGNABLE", Category.UNPROCESSABLE,
                "A staff account cannot have the roles " + roles + "; use AGENT, FLEET_MANAGER or PRICING_MANAGER");
    }
}
