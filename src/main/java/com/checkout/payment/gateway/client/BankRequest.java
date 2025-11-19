package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.domain.Card;
import com.checkout.payment.gateway.model.domain.Money;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankRequest {

  @JsonProperty("card_number")
  private String cardNumber;

  @JsonProperty("expiry_date")
  private String expiryDate;  // Bank expects "MM/YYYY"

  private String currency;

  private int amount;

  private String cvv;

  public static BankRequest from(Card card, Money money) {
    String expiryDate = String.format("%02d/%d", card.getExpiryMonth(), card.getExpiryYear());

    return BankRequest.builder()
        .cardNumber(card.getNumber())
        .expiryDate(expiryDate)
        .currency(money.getCurrency())
        .amount(money.getAmount())
        .cvv(card.getCvv())
        .build();
  }
}