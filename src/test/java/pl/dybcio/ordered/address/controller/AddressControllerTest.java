package pl.dybcio.ordered.address.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.dybcio.ordered.address.entity.Address;
import pl.dybcio.ordered.address.service.AddressNotFoundException;
import pl.dybcio.ordered.address.service.AddressService;
import pl.dybcio.ordered.common.exception.GlobalExceptionHandler;
import pl.dybcio.ordered.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

  @Mock private AddressService addressService;
  private MockMvc mockMvc;

  private final AuthenticatedUser buyer =
      new AuthenticatedUser(42L, "adam@example.com", List.of("ROLE_USER"));

  @BeforeEach
  void setUp() {
    AddressController controller = new AddressController(addressService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
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

  private Address sampleAddress(Long id, boolean isDefault) {
    return Address.builder()
        .id(id)
        .label("Home")
        .recipientName("Adam D")
        .street("Testowa")
        .buildingNumber("1")
        .city("Torun")
        .postalCode("87-100")
        .country("PL")
        .isDefault(isDefault)
        .build();
  }

  @Test
  void list_returnsAddressesForRequestingUser() throws Exception {
    when(addressService.listForUser(42L)).thenReturn(List.of(sampleAddress(1L, true)));

    mockMvc
        .perform(get("/api/v1/addresses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].isDefault").value(true));
  }

  @Test
  void get_returns404_whenAddressNotFoundOrNotOwned() throws Exception {
    when(addressService.getForUser(1L, 42L)).thenThrow(new AddressNotFoundException(1L));

    mockMvc.perform(get("/api/v1/addresses/1")).andExpect(status().isNotFound());
  }

  private static final String VALID_ADDRESS_JSON =
      """
      {
        "label": "Home",
        "recipientName": "Adam D",
        "phone": "+48123456789",
        "street": "Testowa",
        "buildingNumber": "1",
        "city": "Torun",
        "postalCode": "87-100",
        "country": "PL"
      }
      """;

  @Test
  void create_returns201WithCreatedAddress() throws Exception {
    when(addressService.create(eq(42L), any())).thenReturn(sampleAddress(1L, true));

    mockMvc
        .perform(
            post("/api/v1/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_ADDRESS_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void create_returns400_whenLabelMissing() throws Exception {
    String incomplete =
        """
        {"recipientName":"Adam D","street":"Testowa","buildingNumber":"1","city":"Torun","postalCode":"87-100"}
        """;

    mockMvc
        .perform(
            post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON).content(incomplete))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_returns200WithUpdatedAddress() throws Exception {
    when(addressService.update(eq(1L), eq(42L), any())).thenReturn(sampleAddress(1L, false));

    mockMvc
        .perform(
            put("/api/v1/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_ADDRESS_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void delete_returns204() throws Exception {
    mockMvc.perform(delete("/api/v1/addresses/1")).andExpect(status().isNoContent());

    verify(addressService).delete(1L, 42L);
  }

  @Test
  void setDefault_returns200WithUpdatedAddress() throws Exception {
    when(addressService.setDefault(1L, 42L)).thenReturn(sampleAddress(1L, true));

    mockMvc
        .perform(patch("/api/v1/addresses/1/default"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isDefault").value(true));
  }
}
