package com.checkout.payment.gateway.exception;

import lombok.Getter;
import java.util.UUID;

@Getter
public class PaymentNotFoundException extends RuntimeException {

  private final UUID paymentId;

  public PaymentNotFoundException(UUID paymentId) {
    super("Payment not found: " + paymentId);
    this.paymentId = paymentId;
  }

}