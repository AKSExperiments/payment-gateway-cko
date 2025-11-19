package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Slf4j
public class PaymentsRepository {

  private final ConcurrentHashMap<UUID, PostPaymentResponse> payments = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UUID> idempotencyIndex = new ConcurrentHashMap<>();
  private final Object lock = new Object();

  // Added this as an Atomic operation to store payment and index by idempotency key together
  public void addWithIdempotencyKey(PostPaymentResponse payment, String idempotencyKey) {
    synchronized (lock) {
      if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        idempotencyIndex.put(idempotencyKey, payment.getId());
        payments.put(payment.getId(), payment);
        log.info("Payment stored: id={}", payment.getId());
        return;
      }
      log.info("Payment was not stored: id={}", payment.getId());
    }

  }

  public Optional<PostPaymentResponse> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

  public Optional<PostPaymentResponse> getByIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return Optional.empty();
    }

    UUID paymentId = idempotencyIndex.get(idempotencyKey);
    if (paymentId == null) {
      return Optional.empty();
    }

    return get(paymentId);
  }

  public void clear() {
    payments.clear();
    idempotencyIndex.clear();
  }
}