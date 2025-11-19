package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankRequest;
import com.checkout.payment.gateway.client.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentResult;
import com.checkout.payment.gateway.model.domain.Card;
import com.checkout.payment.gateway.model.domain.Money;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.GetPaymentResponse;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;
  private final PaymentValidator paymentValidator;

  public PaymentResult processPayment(PostPaymentRequest request) {
    // Check idempotency first
    var existing = paymentsRepository.getByIdempotencyKey(request.getIdempotencyKey());
    if (existing.isPresent()) {
      log.info("Returning cached response for idempotency key: {}", request.getIdempotencyKey());
      return PaymentResult.fromExisting(existing.get());
    }

    // Validation
    List<String> errors = paymentValidator.validate(request);
    if (!errors.isEmpty()) {
      log.warn("Payment rejected: {}", errors);
      return PaymentResult.rejected(errors);
    }

    // Build domain objects, TODO: Eliminate repeat creation of domain objects
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

    // Call bank
    BankRequest bankRequest = BankRequest.from(card, money);
    BankResponse bankResponse = bankClient.processPayment(bankRequest);

    // Response
    UUID paymentId = UUID.randomUUID(); // TODO: Not taking care of UUID collisions
    PaymentStatus status = bankResponse.isAuthorized()
        ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;

    PostPaymentResponse response = PostPaymentResponse.builder()
        .id(paymentId)
        .status(status)
        .cardNumberLastFour(card.getLastFourDigits())
        .expiryMonth(card.getExpiryMonth())
        .expiryYear(card.getExpiryYear())
        .currency(money.getCurrency())
        .amount(money.getAmount())
        .build();

    // Store payment with idempotency key (atomic operation)
    paymentsRepository.addWithIdempotencyKey(response, request.getIdempotencyKey());

    log.info("Payment processed: id={}, status={}", paymentId, status.getName());

    return bankResponse.isAuthorized()
        ? PaymentResult.authorized(response)
        : PaymentResult.declined(response);
  }

  public GetPaymentResponse getPaymentById(UUID id) {
    log.debug("Retrieving payment: {}", id);

    PostPaymentResponse payment = paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));

    return GetPaymentResponse.builder()
        .id(payment.getId())
        .status(payment.getStatus())
        .cardNumberLastFour(payment.getCardNumberLastFour())
        .expiryMonth(payment.getExpiryMonth())
        .expiryYear(payment.getExpiryYear())
        .currency(payment.getCurrency())
        .amount(payment.getAmount())
        .build();
  }
}