package com.checkout.payment.gateway;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

  private static final String PAYMENTS_ENDPOINT = "/api/payments";
  private static final String VALID_CARD = "2222405343248877";
  private static final String LAST_FOUR = "8877";
  private static final int VALID_AMOUNT = 100;
  private static final int NEXT_YEAR = LocalDate.now().getYear() + 1;
  private static final String IDEMPOTENCY_KEY = "order-123";
  private static final String AUTH_CODE = "auth-123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @MockBean
  private BankClient bankClient;

  @BeforeEach
  void setUp() {
    paymentsRepository.clear();
  }

  @Test
  void shouldAuthorizePaymentAndReturn201() throws Exception { // 201 is Accepted
    when(bankClient.processPayment(any())).thenReturn(authorizedResponse());

    mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPaymentJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.card_number_last_four").value(LAST_FOUR));
  }

  @Test
  void shouldDeclinePaymentAndReturn200() throws Exception { // 200 is OK
    when(bankClient.processPayment(any())).thenReturn(declinedResponse());

    mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPaymentJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
  }

  @Test
  void shouldRejectInvalidPaymentWithBadRequest() throws Exception {
    int currentYear = LocalDate.now().getYear();
    String expiredCardJson = """
            {
                "idempotency_key": "%s",
                "card_number": "%s",
                "expiry_month": 1,
                "expiry_year": %d,
                "currency": "GBP",
                "amount": %d,
                "cvv": "123"
            }
            """.formatted(IDEMPOTENCY_KEY, VALID_CARD, currentYear, VALID_AMOUNT);

    mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(expiredCardJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  void shouldRetrieveStoredPayment() throws Exception {
    when(bankClient.processPayment(any())).thenReturn(authorizedResponse());

    String requestJson = """
            {
                "idempotency_key": "%s",
                "card_number": "%s",
                "expiry_month": 12,
                "expiry_year": %d,
                "currency": "EUR",
                "amount": %d,
                "cvv": "123"
            }
            """.formatted(IDEMPOTENCY_KEY, VALID_CARD, NEXT_YEAR, VALID_AMOUNT);

    String responseJson = mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String paymentId = extractPaymentId(responseJson);

    mockMvc.perform(get(PAYMENTS_ENDPOINT + "/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId));
  }

  @Test
  void shouldRejectPaymentWithoutIdempotencyKey() throws Exception {
    String noIdempotencyKeyJson = """
            {
                "card_number": "%s",
                "expiry_month": 12,
                "expiry_year": %d,
                "currency": "GBP",
                "amount": %d,
                "cvv": "123"
            }
            """.formatted(VALID_CARD, NEXT_YEAR, VALID_AMOUNT);

    mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(noIdempotencyKeyJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnCachedResponseForIdempotencyKey() throws Exception {
    when(bankClient.processPayment(any())).thenReturn(authorizedResponse());

    String requestJson = """
            {
                "idempotency_key": "%s",
                "card_number": "%s",
                "expiry_month": 12,
                "expiry_year": %d,
                "currency": "GBP",
                "amount": %d,
                "cvv": "123"
            }
            """.formatted(IDEMPOTENCY_KEY, VALID_CARD, NEXT_YEAR, VALID_AMOUNT);

    String firstId = extractPaymentId(mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andReturn().getResponse().getContentAsString());

    String secondId = extractPaymentId(mockMvc.perform(post(PAYMENTS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andReturn().getResponse().getContentAsString());

    assertEquals(firstId, secondId);
  }

  private String validPaymentJson() {
    return """
            {
                "idempotency_key": "%s",
                "card_number": "%s",
                "expiry_month": 12,
                "expiry_year": %d,
                "currency": "GBP",
                "amount": %d,
                "cvv": "123"
            }
            """.formatted(IDEMPOTENCY_KEY, VALID_CARD, NEXT_YEAR, VALID_AMOUNT);
  }

  private String extractPaymentId(String json) throws Exception {
    return objectMapper.readTree(json).get("id").asText();
  }

  private BankResponse authorizedResponse() {
    return BankResponse.builder().authorized(true).authorizationCode(AUTH_CODE).build();
  }

  private BankResponse declinedResponse() {
    return BankResponse.builder().authorized(false).build();
  }
}