package pt.rentporto.auth.api.response;

/**
 * Token issued at login.
 *
 * @param accessToken JWT signed with RS256, with {@code sub} = userId and the {@code roles} and {@code email} claims
 * @param tokenType   always {@code Bearer}
 * @param expiresIn   seconds until the token expires
 */
public record TokenResponse(String accessToken, String tokenType, int expiresIn) {
}
