package pl.dybcio.ordered.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dybcio.ordered.address.dto.AddressRequest;
import pl.dybcio.ordered.address.entity.Address;
import pl.dybcio.ordered.address.repository.AddressRepository;
import pl.dybcio.ordered.user.entity.User;
import pl.dybcio.ordered.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

  @Mock private AddressRepository addressRepository;
  @Mock private UserRepository userRepository;

  private AddressService addressService;

  private AddressService service() {
    if (addressService == null) {
      addressService = new AddressService(addressRepository, userRepository);
    }
    return addressService;
  }

  private AddressRequest sampleRequest() {
    return new AddressRequest(
        "Home", "Adam D", "+48123456789", "Testowa", "1", null, "Torun", "87-100", "PL");
  }

  @Test
  void create_marksAddressAsDefault_whenItIsTheFirstOne() {
    when(userRepository.findById(42L)).thenReturn(Optional.of(User.builder().id(42L).build()));
    when(addressRepository.findByUserIdAndIsDefaultTrue(42L)).thenReturn(Optional.empty());
    when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

    Address result = service().create(42L, sampleRequest());

    assertThat(result.isDefault()).isTrue();
  }

  @Test
  void create_doesNotMarkAsDefault_whenUserAlreadyHasADefaultAddress() {
    when(userRepository.findById(42L)).thenReturn(Optional.of(User.builder().id(42L).build()));
    when(addressRepository.findByUserIdAndIsDefaultTrue(42L))
        .thenReturn(Optional.of(Address.builder().id(1L).isDefault(true).build()));
    when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

    Address result = service().create(42L, sampleRequest());

    assertThat(result.isDefault()).isFalse();
  }

  @Test
  void getForUser_throwsAddressNotFound_whenMissingOrNotOwned() {
    when(addressRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getForUser(1L, 42L))
        .isInstanceOf(AddressNotFoundException.class);
  }

  @Test
  void update_overwritesFields() {
    Address existing =
        Address.builder().id(1L).label("Old").recipientName("Old Name").street("Old St").build();
    when(addressRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(existing));
    when(addressRepository.save(existing)).thenReturn(existing);

    Address result = service().update(1L, 42L, sampleRequest());

    assertThat(result.getLabel()).isEqualTo("Home");
    assertThat(result.getRecipientName()).isEqualTo("Adam D");
  }

  @Test
  void delete_promotesNextAddressToDefault_whenDeletingTheDefaultOne() {
    Address deleted = Address.builder().id(1L).isDefault(true).build();
    Address next = Address.builder().id(2L).isDefault(false).build();
    when(addressRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(deleted));
    when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(42L))
        .thenReturn(List.of(next));
    when(addressRepository.save(next)).thenReturn(next);

    service().delete(1L, 42L);

    verify(addressRepository).delete(deleted);
    assertThat(next.isDefault()).isTrue();
    verify(addressRepository).save(next);
  }

  @Test
  void delete_doesNotTouchOtherAddresses_whenDeletingNonDefaultOne() {
    Address deleted = Address.builder().id(1L).isDefault(false).build();
    when(addressRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(deleted));

    service().delete(1L, 42L);

    verify(addressRepository).delete(deleted);
    verify(addressRepository, never()).findByUserIdOrderByIsDefaultDescCreatedAtDesc(any());
  }

  @Test
  void setDefault_unsetsPreviousDefault_andSetsNewOne() {
    Address previous = Address.builder().id(1L).isDefault(true).build();
    Address target = Address.builder().id(2L).isDefault(false).build();
    when(addressRepository.findByIdAndUserId(2L, 42L)).thenReturn(Optional.of(target));
    when(addressRepository.findByUserIdAndIsDefaultTrue(42L)).thenReturn(Optional.of(previous));
    when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

    Address result = service().setDefault(2L, 42L);

    assertThat(previous.isDefault()).isFalse();
    assertThat(result.isDefault()).isTrue();
  }

  @Test
  void setDefault_isNoOpOnPreviousDefault_whenTargetIsAlreadyTheDefault() {
    Address target = Address.builder().id(1L).isDefault(true).build();
    when(addressRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(target));
    when(addressRepository.findByUserIdAndIsDefaultTrue(42L)).thenReturn(Optional.of(target));
    when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

    service().setDefault(1L, 42L);

    verify(addressRepository, times(1)).save(any(Address.class));
  }
}
