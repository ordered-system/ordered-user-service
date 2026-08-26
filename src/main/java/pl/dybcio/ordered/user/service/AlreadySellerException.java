package pl.dybcio.ordered.user.service;

public class AlreadySellerException extends RuntimeException {
  public AlreadySellerException(Long userId) {
    super("User " + userId + " is already a seller");
  }
}
