package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcquiringBankClientTest {

  private static final String BANK_URL = "http://localhost:8080";
  private static final int MAX_RETRIES = 2;
  private static final long RETRY_DELAY_MS = 10; // Short delay for tests

  @Mock
  private RestTemplate restTemplate;

  private AcquiringBankClient bankClient;

  @BeforeEach
  void setUp() {
    bankClient = new AcquiringBankClient(restTemplate, BANK_URL, MAX_RETRIES, RETRY_DELAY_MS);
  }

  @Test
  void shouldReturnResponseOnFirstAttempt() {
    BankRequest request = createBankRequest();
    BankResponse expectedResponse = BankResponse.builder()
        .authorized(true)
        .authorizationCode("auth-123")
        .build();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(ResponseEntity.ok(expectedResponse));

    BankResponse result = bankClient.processPayment(request);

    assertEquals(expectedResponse, result);
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void shouldRetryOnResourceAccessException() {
    BankRequest request = createBankRequest();
    BankResponse expectedResponse = BankResponse.builder()
        .authorized(true)
        .authorizationCode("auth-123")
        .build();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"))
        .thenReturn(ResponseEntity.ok(expectedResponse));

    BankResponse result = bankClient.processPayment(request);

    assertEquals(expectedResponse, result);
    verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void shouldRetryOnHttpServerErrorException() {
    BankRequest request = createBankRequest();
    BankResponse expectedResponse = BankResponse.builder()
        .authorized(true)
        .authorizationCode("auth-123")
        .build();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
        .thenReturn(ResponseEntity.ok(expectedResponse));

    BankResponse result = bankClient.processPayment(request);

    assertEquals(expectedResponse, result);
    verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void shouldThrowBankUnavailableExceptionAfterMaxRetries() {
    BankRequest request = createBankRequest();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    BankUnavailableException exception = assertThrows(
        BankUnavailableException.class,
        () -> bankClient.processPayment(request)
    );

    assertTrue(exception.getMessage().contains("3 attempts"));
    // Initial attempt + 2 retries = 3 total attempts
    verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void shouldRetryMultipleTimesBeforeSuccess() {
    BankRequest request = createBankRequest();
    BankResponse expectedResponse = BankResponse.builder()
        .authorized(false)
        .build();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"))
        .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
        .thenReturn(ResponseEntity.ok(expectedResponse));

    BankResponse result = bankClient.processPayment(request);

    assertEquals(expectedResponse, result);
    verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void shouldCallCorrectEndpoint() {
    BankRequest request = createBankRequest();
    BankResponse expectedResponse = BankResponse.builder()
        .authorized(true)
        .authorizationCode("auth-123")
        .build();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(ResponseEntity.ok(expectedResponse));

    bankClient.processPayment(request);

    verify(restTemplate).postForEntity(
        eq(BANK_URL + "/payments"),
        eq(request),
        eq(BankResponse.class)
    );
  }

  @Test
  void shouldHandleDeclinedPayment() {
    BankRequest request = createBankRequest();
    BankResponse declinedResponse = BankResponse.builder()
        .authorized(false)
        .build();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(ResponseEntity.ok(declinedResponse));

    BankResponse result = bankClient.processPayment(request);

    assertFalse(result.isAuthorized());
    assertNull(result.getAuthorizationCode());
  }

  @Test
  void shouldNotRetryWithZeroMaxRetries() {
    AcquiringBankClient clientWithNoRetries = new AcquiringBankClient(
        restTemplate, BANK_URL, 0, RETRY_DELAY_MS);
    BankRequest request = createBankRequest();

    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    BankUnavailableException exception = assertThrows(
        BankUnavailableException.class,
        () -> clientWithNoRetries.processPayment(request)
    );

    assertTrue(exception.getMessage().contains("1 attempts"));
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  private BankRequest createBankRequest() {
    return BankRequest.builder()
        .cardNumber("2222405343248877")
        .expiryDate("12/2025")
        .currency("GBP")
        .amount(100)
        .cvv("123")
        .build();
  }
}
