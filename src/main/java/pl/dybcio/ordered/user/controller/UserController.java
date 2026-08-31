package pl.dybcio.ordered.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dybcio.ordered.commons.security.AuthenticatedUser;
import pl.dybcio.ordered.user.dto.LoginResponse;
import pl.dybcio.ordered.user.dto.UserResponse;
import pl.dybcio.ordered.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "The authenticated user's own profile")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  @Operation(summary = "Get the authenticated user's own profile")
  public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
    return userService.getById(user.userId());
  }

  @PostMapping("/me/become-seller")
  @Operation(
      summary = "Self-promote to SELLER",
      description =
          "Issues a fresh JWT with ROLE_SELLER baked in - the old token still has ROLE_USER only"
              + " and should be discarded.")
  public LoginResponse becomeSeller(@AuthenticationPrincipal AuthenticatedUser user) {
    return userService.promoteToSeller(user.userId());
  }
}
