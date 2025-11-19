package com.checkout.payment.gateway.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostPaymentRequest {

  @JsonProperty("idempotency_key")
  @NotBlank(message = "Idempotency key is required")
  @Pattern(regexp = "^[a-zA-Z0-9\\-_]{8,64}$",
      message = "Idempotency key must be 8-64 alphanumeric characters, hyphens, or underscores")
  private String idempotencyKey;

  @JsonProperty("card_number")
  @NotNull(message = "Card number is required")
  @Pattern(regexp = "^\\d{14,19}$",
      message = "Card number must be 14-19 digits")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  @NotNull(message = "Expiry year is required")
  @Min(value = 2000, message = "Expiry year must be valid")
  private Integer expiryYear;

  @NotNull(message = "Currency is required")
  @Pattern(regexp = "^[A-Z]{3}$",
      message = "Currency must be a 3-letter ISO code (e.g., USD, GBP, EUR)")
  private String currency;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be greater than 0")
  private Integer amount;

  @NotNull(message = "CVV is required")
  @Pattern(regexp = "^\\d{3,4}$",
      message = "CVV must be 3 or 4 digits")
  private String cvv;
}