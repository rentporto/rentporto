package pt.rentporto.web.testing;

import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Finds the roles a controller method lets through, by calling it as each role. The {@code @PreAuthorize} check runs
 * before the method, so the arguments do not matter: an exception other than {@link AccessDeniedException} means the
 * role got through.
 */
public final class AllowedRoles {

    private AllowedRoles() {
    }

    /**
     * Calls a controller method once per role.
     *
     * @param call the call to the controller bean
     * @return the roles that were not denied
     */
    public static Set<String> of(Call call) {
        Set<String> allowed = new TreeSet<>();
        for (String role : TestJwt.ROLES) {
            SecurityContextHolder.getContext()
                    .setAuthentication(new TestingAuthenticationToken("user", null, "ROLE_" + role));
            try {
                call.run();
                allowed.add(role);
            } catch (AccessDeniedException e) {
                continue;
            } catch (Exception e) {
                allowed.add(role);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
        return allowed;
    }

    /**
     * Call to a controller method.
     */
    @FunctionalInterface
    public interface Call {

        void run() throws Exception;
    }
}
