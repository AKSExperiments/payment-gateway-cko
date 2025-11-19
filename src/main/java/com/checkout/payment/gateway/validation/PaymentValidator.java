package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.domain.Card;
import com.checkout.payment.gateway.model.domain.Money;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation after Bean Regex Validation
 */
@Component
public class PaymentValidator {

  // TODO: Turn the error strings to enums
  public List<String> validate(PostPaymentRequest request) {
    List<String> errors = new ArrayList<>();

    Card card = Card.builder()
        .number(request.getCardNumber())
        .expiryMonth(request.getExpiryMonth())
        .expiryYear(request.getExpiryYear())
        .cvv(request.getCvv())
        .build();

    Money money = Money.builder()
        .amount(request.getAmount())
        .currency(request.getCurrency())
        .build();

    if (card.isExpired()) {
      errors.add("Card has expired");
    }

    if (!money.isCurrencySupported()) {
      errors.add("Currency '" + request.getCurrency() + "' is not supported. " +
          "Allowed: " + Money.getSupportedCurrencies());
    }

    return errors;
  }
}