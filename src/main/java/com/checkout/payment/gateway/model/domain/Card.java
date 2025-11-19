package com.checkout.payment.gateway.model.domain;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.time.YearMonth;

@Value
@Builder
public class Card {

  @ToString.Exclude //  This prevents card number to be printed
  String number;

  int expiryMonth;
  int expiryYear;

  @ToString.Exclude  // Don't log CVV
  String cvv;

  public String getLastFourDigits() {
    return number.substring(number.length() - 4);
  }

  public boolean isExpired() {
    YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
    return expiry.isBefore(YearMonth.now()) || expiry.equals(YearMonth.now());
  }

}