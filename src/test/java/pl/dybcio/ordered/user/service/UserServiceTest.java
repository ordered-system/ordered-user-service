package pl.dybcio.ordered.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.dybcio.ordered.security.JwtService;
import pl.dybcio.ordered.security.UserDetailsImpl;
import pl.dybcio.ordered.security.UserDetailsServiceImpl;
import pl.dybcio.ordered.user.dto.LoginRequest;
import pl.dybcio.ordered.user.dto.LoginResponse;
import pl.dybcio.ordered.user.dto.RegisterRequest;
import pl.dybcio.ordered.user.dto.UserResponse;
import pl.dybcio.ordered.user.entity.Role;
import pl.dybcio.ordered.user.entity.User;
import pl.dybcio.ordered.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private UserDetailsServiceImpl userDetailsService;

  private UserService userService;

  private UserService service() {
    if (userService == null) {
      userService =
          new UserService(
              userRepository,
              passwordEncoder,
              authenticationManager,
              jwtService,
              userDetailsService);
    }
    return userService;
  }

  @Test
  void register_savesUser_withEncodedPasswordAndDefaultUserRole() {
    when(userRepository.existsByEmail("adam@example.com")).thenReturn(false);
    when(passwordEncoder.encode("plaintext-pw")).thenReturn("hashed-pw");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u.setId(1L);
              return u;
            });

    RegisterRequest request = new RegisterRequest("adam@example.com", "plaintext-pw", "Adam", "D");
    UserResponse response = service().register(request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("adam@example.com");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPassword()).isEqualTo("hashed-pw");
    assertThat(captor.getValue().getRoles()).containsExactly(Role.USER);
  }

  @Test
  void register_throwsEmailAlreadyTaken_whenEmailInUse() {
    when(userRepository.existsByEmail("adam@example.com")).thenReturn(true);

    RegisterRequest request = new RegisterRequest("adam@example.com", "plaintext-pw", "Adam", "D");

    assertThatThrownBy(() -> service().register(request))
        .isInstanceOf(EmailAlreadyTakenException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void login_returnsToken_onValidCredentials() {
    User user =
        User.builder()
            .id(1L)
            .email("adam@example.com")
            .password("hashed-pw")
            .roles(Set.of(Role.USER))
            .build();
    UserDetailsImpl userDetails = new UserDetailsImpl(user);
    when(userDetailsService.loadUserByUsername("adam@example.com")).thenReturn(userDetails);
    when(jwtService.generateToken(userDetails)).thenReturn("signed.jwt.token");

    LoginResponse response = service().login(new LoginRequest("adam@example.com", "plaintext-pw"));

    assertThat(response.token()).isEqualTo("signed.jwt.token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    verify(authenticationManager)
        .authenticate(
            argThat(
                auth ->
                    auth instanceof UsernamePasswordAuthenticationToken
                        && "adam@example.com".equals(auth.getPrincipal())));
  }

  @Test
  void login_propagatesBadCredentials_whenAuthenticationManagerRejects() {
    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> service().login(new LoginRequest("adam@example.com", "wrong")))
        .isInstanceOf(BadCredentialsException.class);

    verifyNoInteractions(jwtService);
  }

  @Test
  void getById_returnsProfile_whenUserExists() {
    User user =
        User.builder().id(1L).email("adam@example.com").firstName("Adam").lastName("D").build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    UserResponse response = service().getById(1L);

    assertThat(response.email()).isEqualTo("adam@example.com");
    assertThat(response.firstName()).isEqualTo("Adam");
  }

  @Test
  void getById_throwsIllegalState_whenUserMissing() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getById(1L)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void promoteToSeller_addsSellerRole_andReturnsFreshTokenWithItBakedIn() {
    User user =
        User.builder()
            .id(1L)
            .email("adam@example.com")
            .roles(new java.util.HashSet<>(Set.of(Role.USER)))
            .build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn("fresh.jwt.with.seller");

    LoginResponse response = service().promoteToSeller(1L);

    assertThat(response.token()).isEqualTo("fresh.jwt.with.seller");
    assertThat(user.getRoles()).contains(Role.SELLER, Role.USER);
    verify(userRepository).save(user);

    ArgumentCaptor<UserDetailsImpl> captor = ArgumentCaptor.forClass(UserDetailsImpl.class);
    verify(jwtService).generateToken(captor.capture());
    assertThat(captor.getValue().getAuthorities())
        .extracting(Object::toString)
        .contains("ROLE_SELLER");
  }

  @Test
  void promoteToSeller_throwsAlreadySeller_whenUserAlreadyHasTheRole() {
    User user =
        User.builder()
            .id(1L)
            .email("adam@example.com")
            .roles(new java.util.HashSet<>(Set.of(Role.USER, Role.SELLER)))
            .build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service().promoteToSeller(1L))
        .isInstanceOf(AlreadySellerException.class);

    verify(userRepository, never()).save(any());
    verifyNoInteractions(jwtService);
  }
}
