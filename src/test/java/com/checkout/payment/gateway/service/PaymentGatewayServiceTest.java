package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentResult;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  private static final String VALID_CARD = "2222405343248877";
  private static final String VALID_CVV = "123";
  private static final int VALID_AMOUNT = 100;
  private static final String AUTH_CODE = "auth-123";
  private static final String IDEMPOTENCY_KEY = "order-123";

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankClient bankClient;

  @Mock
  private PaymentValidator paymentValidator;

  private PaymentGatewayService service;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, bankClient, paymentValidator);
  }

  @Test
  void shouldReturnAuthorizedWhenBankAuthorizes() {
    PostPaymentRequest request = createValidRequest();
    when(paymentValidator.validate(any())).thenReturn(List.of());
    when(bankClient.processPayment(any())).thenReturn(authorizedResponse());

    PaymentResult result = service.processPayment(request);

    assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
    assertNotNull(result.getResponse().getId());
  }

  @Test
  void shouldReturnDeclinedWhenBankDeclines() {
    PostPaymentRequest request = createValidRequest();
    when(paymentValidator.validate(any())).thenReturn(List.of());
    when(bankClient.processPayment(any())).thenReturn(declinedResponse());

    PaymentResult result = service.processPayment(request);

    assertEquals(PaymentStatus.DECLINED, result.getStatus());
  }

  @Test
  void shouldReturnRejectedWhenValidationFails() {
    PostPaymentRequest request = createValidRequest();
    when(paymentValidator.validate(any())).thenReturn(List.of("Card has expired"));

    PaymentResult result = service.processPayment(request);

    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    verify(bankClient, never()).processPayment(any());
  }

  @Test
  void shouldReturnCachedResponseForIdempotencyKey() {
    PostPaymentRequest request = createValidRequest();
    request.setIdempotencyKey(IDEMPOTENCY_KEY);

    PostPaymentResponse cached = PostPaymentResponse.builder()
        .id(UUID.randomUUID())
        .status(PaymentStatus.AUTHORIZED)
        .build();

    when(paymentsRepository.getByIdempotencyKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(cached));

    PaymentResult result = service.processPayment(request);

    assertEquals(cached.getId(), result.getResponse().getId());
    verify(bankClient, never()).processPayment(any());
  }

  @Test
  void shouldStorePaymentOnlyWithIdempotencyKey() {
    PostPaymentRequest request = createValidRequest();
    request.setIdempotencyKey(IDEMPOTENCY_KEY);

    when(paymentValidator.validate(any())).thenReturn(List.of());
    when(bankClient.processPayment(any())).thenReturn(authorizedResponse());

    service.processPayment(request);

    verify(paymentsRepository).addWithIdempotencyKey(any(), eq(IDEMPOTENCY_KEY));
  }

  @Test
  void shouldThrowWhenPaymentNotFound() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThrows(PaymentNotFoundException.class, () -> service.getPaymentById(id));
  }

  private PostPaymentRequest createValidRequest() {
    return PostPaymentRequest.builder()
        .idempotencyKey(IDEMPOTENCY_KEY)
        .cardNumber(VALID_CARD)
        .expiryMonth(12)
        .expiryYear(LocalDate.now().getYear() + 1)
        .currency("GBP")
        .amount(VALID_AMOUNT)
        .cvv(VALID_CVV)
        .build();
  }

  private BankResponse authorizedResponse() {
    return BankResponse.builder().authorized(true).authorizationCode(AUTH_CODE).build();
  }

  private BankResponse declinedResponse() {
    return BankResponse.builder().authorized(false).build();
  }
}