package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Result type pattern: explicit outcomes instead of exceptions for business logic.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentResult {

  private final PaymentStatus status;
  private final PostPaymentResponse response;
  private final List<String> errors;

  public static PaymentResult authorized(PostPaymentResponse response) {
    return new PaymentResult(PaymentStatus.AUTHORIZED, response, List.of());
  }

  public static PaymentResult declined(PostPaymentResponse response) {
    return new PaymentResult(PaymentStatus.DECLINED, response, List.of());
  }

  // Rejected = validation failed, bank never called
  public static PaymentResult rejected(List<String> errors) {
    return new PaymentResult(PaymentStatus.REJECTED, null, errors);
  }

  // For idempotency: return cached result
  public static PaymentResult fromExisting(PostPaymentResponse response) {
    return new PaymentResult(response.getStatus(), response, List.of());
  }

  public boolean isProcessed() {
    return status != PaymentStatus.REJECTED;
  }

  public boolean isAuthorized() {
    return status == PaymentStatus.AUTHORIZED;
  }

  public boolean isRejected() {
    return status == PaymentStatus.REJECTED;
  }
}