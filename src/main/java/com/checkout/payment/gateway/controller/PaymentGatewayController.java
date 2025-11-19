package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PaymentResult;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.GetPaymentResponse;
import com.checkout.payment.gateway.model.response.RejectedPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  @PostMapping("/payments")
  public ResponseEntity<?> processPayment(@Valid @RequestBody PostPaymentRequest request) {
    log.info("Received payment request");

    PaymentResult result = paymentGatewayService.processPayment(request);

    if (result.isRejected()) { // REJECTED: HTTP 400 bad request as details failed validation
      return ResponseEntity.badRequest()
          .body(RejectedPaymentResponse.of(result.getErrors()));
    }

    HttpStatus status = result.isAuthorized()
        ? HttpStatus.CREATED // AUTHORIZED
        : HttpStatus.OK;     // DECLINED

    return ResponseEntity.status(status).body(result.getResponse());
  }

  @GetMapping("/payments/{id}")
  public ResponseEntity<GetPaymentResponse> getPayment(@PathVariable UUID id) {
    log.info("Retrieving payment: {}", id);
    return ResponseEntity.ok(paymentGatewayService.getPaymentById(id));
  }
}