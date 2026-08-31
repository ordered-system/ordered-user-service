package pl.dybcio.ordered.address.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dybcio.ordered.address.dto.AddressRequest;
import pl.dybcio.ordered.address.dto.AddressResponse;
import pl.dybcio.ordered.address.entity.Address;
import pl.dybcio.ordered.address.service.AddressService;
import pl.dybcio.ordered.commons.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "The authenticated user's own saved delivery addresses")
public class AddressController {

  private final AddressService addressService;

  @GetMapping
  @Operation(summary = "List the authenticated user's saved addresses")
  public List<AddressResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
    return addressService.listForUser(user.userId()).stream().map(AddressResponse::from).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get one of the authenticated user's addresses by id")
  public AddressResponse get(
      @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
    return AddressResponse.from(addressService.getForUser(id, user.userId()));
  }

  @PostMapping
  @Operation(summary = "Save a new address")
  public ResponseEntity<AddressResponse> create(
      @Valid @RequestBody AddressRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    Address address = addressService.create(user.userId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(AddressResponse.from(address));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a saved address")
  public AddressResponse update(
      @PathVariable Long id,
      @Valid @RequestBody AddressRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return AddressResponse.from(addressService.update(id, user.userId(), request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a saved address")
  public ResponseEntity<Void> delete(
      @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
    addressService.delete(id, user.userId());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/default")
  @Operation(summary = "Mark an address as the default delivery address")
  public AddressResponse setDefault(
      @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
    return AddressResponse.from(addressService.setDefault(id, user.userId()));
  }
}
