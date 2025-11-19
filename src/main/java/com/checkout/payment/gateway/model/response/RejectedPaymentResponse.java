package com.checkout.payment.gateway.model.response;

import com.checkout.payment.gateway.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedPaymentResponse {

  private PaymentStatus status;
  private List<String> errors;

  public static RejectedPaymentResponse of(List<String> errors) {
    return RejectedPaymentResponse.builder()
        .status(PaymentStatus.REJECTED)
        .errors(errors)
        .build();
  }
}