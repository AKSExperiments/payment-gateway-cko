package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorTest {

  private static final String VALID_CARD = "2222405343248877";
  private static final String VALID_CVV = "123";
  private static final int VALID_AMOUNT = 100;
  private static final int EXPIRED_YEAR = 2020;

  private PaymentValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentValidator();
  }

  @Test
  void shouldPassValidRequest() {
    PostPaymentRequest request = createValidRequest();

    List<String> errors = validator.validate(request);

    assertTrue(errors.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"USD", "GBP", "EUR"})
  void shouldAcceptSupportedCurrencies(String currency) {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency(currency);

    List<String> errors = validator.validate(request);

    assertTrue(errors.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"JPY", "CNY", "XXX"})
  void shouldRejectUnsupportedCurrencies(String currency) {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency(currency);

    List<String> errors = validator.validate(request);

    assertEquals(1, errors.size());
    assertTrue(errors.get(0).contains("not supported"));
  }

  @Test
  void shouldRejectExpiredCard() {
    PostPaymentRequest request = createValidRequest();
    request.setExpiryYear(EXPIRED_YEAR);
    request.setExpiryMonth(1);

    List<String> errors = validator.validate(request);

    assertEquals(1, errors.size());
    assertTrue(errors.get(0).contains("expired"));
  }

  private PostPaymentRequest createValidRequest() {
    return PostPaymentRequest.builder()
        .cardNumber(VALID_CARD)
        .expiryMonth(12)
        .expiryYear(LocalDate.now().getYear() + 1)
        .currency("GBP")
        .amount(VALID_AMOUNT)
        .cvv(VALID_CVV)
        .build();
  }
}