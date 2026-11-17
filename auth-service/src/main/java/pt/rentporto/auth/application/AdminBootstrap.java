package pt.rentporto.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pt.rentporto.auth.domain.EmailAlreadyRegisteredException;

/**
 * Creates the first administrator at startup, so that someone can create the staff accounts. An existing account,
 * created by an earlier startup or by another replica, is left as it is: changing the configured password later does
 * not change it.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AccountService accounts;
    private final String email;
    private final String password;

    /**
     * Creates the runner. The values are read with {@code @Value}, so that a placeholder without a value, such as the
     * Secret of the k8s profile, stops the startup instead of becoming the password.
     *
     * @param accounts creates the account
     * @param email    email of the administrator, or empty for none
     * @param password password of the administrator, or empty for none
     */
    AdminBootstrap(
            AccountService accounts,
            @Value("${rentporto.auth.admin.email:}") String email,
            @Value("${rentporto.auth.admin.password:}") String password) {
        this.accounts = accounts;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("No administrator configured in rentporto.auth.admin");
            return;
        }
        try {
            accounts.createAdministrator(email, password);
            log.info("Created the administrator account {}", email);
        } catch (EmailAlreadyRegisteredException e) {
            log.info("The administrator account {} already exists", email);
        }
    }
}
