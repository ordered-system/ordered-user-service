package pl.dybcio.ordered.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.dybcio.ordered.common.exception.GlobalExceptionHandler;
import pl.dybcio.ordered.user.dto.LoginResponse;
import pl.dybcio.ordered.user.dto.UserResponse;
import pl.dybcio.ordered.user.service.EmailAlreadyTakenException;
import pl.dybcio.ordered.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private UserService userService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AuthController controller = new AuthController(userService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void register_returns201_onSuccess() throws Exception {
    when(userService.register(any()))
        .thenReturn(new UserResponse(1L, "adam@example.com", "Adam", "D"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"adam@example.com","password":"plaintext-pw","firstName":"Adam","lastName":"D"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("adam@example.com"));
  }

  @Test
  void register_returns409_whenEmailAlreadyTaken() throws Exception {
    when(userService.register(any())).thenThrow(new EmailAlreadyTakenException("adam@example.com"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"adam@example.com","password":"plaintext-pw","firstName":"Adam","lastName":"D"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void register_returns400_onInvalidEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"not-an-email","password":"plaintext-pw","firstName":"Adam","lastName":"D"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_returns400_whenPasswordTooShort() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"adam@example.com","password":"short","firstName":"Adam","lastName":"D"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_returns200WithToken_onSuccess() throws Exception {
    when(userService.login(any())).thenReturn(new LoginResponse("signed.jwt.token"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"adam@example.com","password":"plaintext-pw"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("signed.jwt.token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void login_returns401_onBadCredentials() throws Exception {
    when(userService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"adam@example.com","password":"wrong"}
                    """))
        .andExpect(status().isUnauthorized());
  }
}
