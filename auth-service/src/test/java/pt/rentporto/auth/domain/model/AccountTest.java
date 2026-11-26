package pt.rentporto.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pt.rentporto.auth.domain.exception.RoleNotAssignableException;

class AccountTest {

    @Test
    void aCustomerHasOnlyTheCustomerRoleAndTheEmailInLowerCase() {
        Account account = Account.customer(new Email(" Ana.Silva@Exemplo.PT "), "hash", "key-1");

        assertThat(account.email().value()).isEqualTo("ana.silva@exemplo.pt");
        assertThat(account.roles()).containsExactly(Role.CUSTOMER);
        assertThat(account.createdWith("key-1")).isTrue();
        assertThat(account.createdWith(null)).isFalse();
    }

    @Test
    void aStaffAccountHasOneOrMoreStaffRoles() {
        Account account = Account.staff(
                new Email("gestor@rentporto.pt"), "hash", Set.of(Role.FLEET_MANAGER, Role.PRICING_MANAGER), null);

        assertThat(account.roles()).containsExactlyInAnyOrder(Role.FLEET_MANAGER, Role.PRICING_MANAGER);
    }

    @Test
    void aStaffAccountCannotBeAnAdministratorOrACustomer() {
        Email email = new Email("staff@rentporto.pt");

        assertThatThrownBy(() -> Account.staff(email, "hash", Set.of(Role.AGENT, Role.ADMIN), null))
                .isInstanceOf(RoleNotAssignableException.class)
                .hasMessageContaining("[ADMIN]");
        assertThatThrownBy(() -> Account.staff(email, "hash", Set.of(Role.CUSTOMER), null))
                .isInstanceOf(RoleNotAssignableException.class)
                .hasMessageContaining("[CUSTOMER]");
    }
}
