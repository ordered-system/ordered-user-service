package pl.dybcio.ordered.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.dybcio.ordered.user.dto.LoginResponse;
import pl.dybcio.ordered.user.dto.UserResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestRestTemplate
class RegisterLoginFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void extraProperties(DynamicPropertyRegistry registry) {
    registry.add("eureka.client.enabled", () -> "false");
    registry.add("app.jwt.secret", () -> "test-only-signing-secret-not-used-for-any-real-auth");
  }

  @Autowired private TestRestTemplate restTemplate;

  private String uniqueEmail() {
    return "adam+" + System.nanoTime() + "@example.com";
  }

  private HttpEntity<String> jsonBody(String json) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(json, headers);
  }

  @Test
  void registerThenLogin_returnsTokenThatUnlocksProtectedEndpoint() {
    String email = uniqueEmail();

    ResponseEntity<UserResponse> registerResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            jsonBody(
                """
                                {"email":"%s","password":"correct-password","firstName":"Adam","lastName":"D"}
                                """
                    .formatted(email)),
            UserResponse.class);
    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(registerResponse.getBody().email()).isEqualTo(email);

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            jsonBody(
                """
                {"email":"%s","password":"correct-password"}
                """
                    .formatted(email)),
            LoginResponse.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = loginResponse.getBody().token();
    assertThat(token).isNotBlank();

    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.setBearerAuth(token);
    ResponseEntity<UserResponse> meResponse =
        restTemplate.exchange(
            "/api/v1/users/me",
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            UserResponse.class);

    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meResponse.getBody().email()).isEqualTo(email);
  }

  @Test
  void register_returns409_onDuplicateEmail() {
    String email = uniqueEmail();
    String body =
        """
                {"email":"%s","password":"correct-password","firstName":"Adam","lastName":"D"}
                """
            .formatted(email);

    restTemplate.postForEntity("/api/v1/auth/register", jsonBody(body), UserResponse.class);
    ResponseEntity<String> secondAttempt =
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(body), String.class);

    assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void login_returns401_onWrongPassword() {
    String email = uniqueEmail();
    restTemplate.postForEntity(
        "/api/v1/auth/register",
        jsonBody(
            """
                        {"email":"%s","password":"correct-password","firstName":"Adam","lastName":"D"}
                        """
                .formatted(email)),
        UserResponse.class);

    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            jsonBody(
                """
                {"email":"%s","password":"totally-wrong"}
                """
                    .formatted(email)),
            String.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void protectedEndpoint_returns401_withoutToken() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void protectedEndpoint_returns401_withGarbageToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("this.is.not-a-valid-jwt");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/users/me",
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
