package pl.dybcio.ordered.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.dybcio.ordered.common.exception.GlobalExceptionHandler;
import pl.dybcio.ordered.commons.exception.CommonExceptionHandler;
import pl.dybcio.ordered.commons.security.AuthenticatedUser;
import pl.dybcio.ordered.user.dto.LoginResponse;
import pl.dybcio.ordered.user.dto.UserResponse;
import pl.dybcio.ordered.user.service.AlreadySellerException;
import pl.dybcio.ordered.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private UserService userService;
  private MockMvc mockMvc;

  private final AuthenticatedUser buyer =
      new AuthenticatedUser(42L, "adam@example.com", List.of("ROLE_USER"));

  @BeforeEach
  void setUp() {
    UserController controller = new UserController(userService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(), new CommonExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    var authorities = buyer.roles().stream().map(SimpleGrantedAuthority::new).toList();
    var token = new UsernamePasswordAuthenticationToken(buyer, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUser_returnsProfile_forAuthenticatedPrincipal() throws Exception {
    when(userService.getById(42L))
        .thenReturn(new UserResponse(42L, "adam@example.com", "Adam", "D"));

    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("adam@example.com"))
        .andExpect(jsonPath("$.firstName").value("Adam"));
  }

  @Test
  void becomeSeller_returns200WithFreshToken_onSuccess() throws Exception {
    when(userService.promoteToSeller(42L)).thenReturn(new LoginResponse("fresh.jwt.with.seller"));

    mockMvc
        .perform(post("/api/v1/users/me/become-seller"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("fresh.jwt.with.seller"));
  }

  @Test
  void becomeSeller_returns409_whenAlreadyASeller() throws Exception {
    when(userService.promoteToSeller(42L)).thenThrow(new AlreadySellerException(42L));

    mockMvc.perform(post("/api/v1/users/me/become-seller")).andExpect(status().isConflict());
  }
}
