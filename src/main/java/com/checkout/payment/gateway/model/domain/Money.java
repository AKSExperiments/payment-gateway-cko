package com.checkout.payment.gateway.model.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class Money {

  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "GBP", "EUR");

  int amount;      // Minor currency units
  String currency;

  public boolean isValid() {
    return amount > 0 && isCurrencySupported();
  }

  public boolean isCurrencySupported() {
    return SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
  }

  public static Set<String> getSupportedCurrencies() {
    return SUPPORTED_CURRENCIES;
  }
}