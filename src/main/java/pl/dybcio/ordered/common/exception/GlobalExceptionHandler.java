package pl.dybcio.ordered.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.dybcio.ordered.address.service.AddressNotFoundException;
import pl.dybcio.ordered.user.service.AlreadySellerException;
import pl.dybcio.ordered.user.service.EmailAlreadyTakenException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailAlreadyTakenException.class)
  public ProblemDetail handleEmailAlreadyTaken(EmailAlreadyTakenException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Email already taken");
    return pd;
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
  }

  @ExceptionHandler(AddressNotFoundException.class)
  public ProblemDetail handleAddressNotFound(AddressNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Address not found");
    return pd;
  }

  @ExceptionHandler(AlreadySellerException.class)
  public ProblemDetail handleAlreadySeller(AlreadySellerException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Already a seller");
    return pd;
  }
}
