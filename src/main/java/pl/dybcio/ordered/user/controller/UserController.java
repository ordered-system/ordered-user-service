package pl.dybcio.ordered.user.controller;

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
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
    return userService.getById(user.userId());
  }

  @PostMapping("/me/become-seller")
  public LoginResponse becomeSeller(@AuthenticationPrincipal AuthenticatedUser user) {
    return userService.promoteToSeller(user.userId());
  }
}
