package com.checkout.payment.gateway.client;

public interface BankClient {
  BankResponse processPayment(BankRequest request);
}