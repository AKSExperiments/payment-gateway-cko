package com.checkout.payment.gateway.model.response;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPaymentResponse {

  private UUID id;

  private PaymentStatus status;

  @JsonProperty("card_number_last_four")
  private String cardNumberLastFour;

  @JsonProperty("expiry_month")
  private int expiryMonth;

  @JsonProperty("expiry_year")
  private int expiryYear;

  private String currency;

  private int amount;
}