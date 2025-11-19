package com.checkout.payment.gateway;

import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentE2ETest {

  private static final String CARD_AUTHORIZED = "2222405343248877";  // Ends in 7 (odd)
  private static final String CARD_DECLINED = "2222405343248872";    // Ends in 2 (even)
  private static final String CARD_BANK_ERROR = "2222405343248870";  // Ends in 0 (503)
  private static final int NEXT_YEAR = LocalDate.now().getYear() + 1;
  private static final String PAYMENTS_ENDPOINT = "/api/payments";

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @BeforeEach
  void setUp() {
    paymentsRepository.clear();
  }

  @Test
  void shouldAuthorizePaymentWhenCardEndsInOddDigit() {
    var request = createRequest(CARD_AUTHORIZED, "order-e2e-1");

    var response = restTemplate.postForEntity(PAYMENTS_ENDPOINT, request, PostPaymentResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Authorized", response.getBody().getStatus().getName());
  }

  @Test
  void shouldDeclinePaymentWhenCardEndsInEvenDigit() {
    var request = createRequest(CARD_DECLINED, "order-e2e-2");

    var response = restTemplate.postForEntity(PAYMENTS_ENDPOINT, request, PostPaymentResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Declined", response.getBody().getStatus().getName());
  }

  @Test
  void shouldReturn502WhenBankReturns503() {
    var request = createRequest(CARD_BANK_ERROR, "order-e2e-3");

    var response = restTemplate.postForEntity(PAYMENTS_ENDPOINT, request, String.class);

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
  }

  private PostPaymentRequest createRequest(String cardNumber, String idempotencyKey) {
    return PostPaymentRequest.builder()
        .idempotencyKey(idempotencyKey)
        .cardNumber(cardNumber)
        .expiryMonth(12)
        .expiryYear(NEXT_YEAR)
        .currency("GBP")
        .amount(100)
        .cvv("123")
        .build();
  }
}