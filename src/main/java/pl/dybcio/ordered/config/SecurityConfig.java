package pl.dybcio.ordered.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.dybcio.ordered.security.JwtClaimsAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtClaimsAuthenticationFilter jwtClaimsAuthenticationFilter;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/v1/auth/**", "/actuator/health", "/actuator/prometheus", "/error"
  };

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll().anyRequest().authenticated())
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtClaimsAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(authenticationEntryPoint()));

    return http.build();
  }

  private AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      ProblemDetail problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType("application/json");
      response.getWriter().write(objectMapper.writeValueAsString(problem));
    };
  }
}
