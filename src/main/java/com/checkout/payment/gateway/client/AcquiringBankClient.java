package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class AcquiringBankClient implements BankClient {

  private final RestTemplate restTemplate;
  private final String bankUrl;
  private final int maxRetries;
  private final long retryDelayMs;

  public AcquiringBankClient(
      RestTemplate restTemplate,
      @Value("${bank.simulator.url}") String bankUrl,
      @Value("${bank.simulator.max-retries:1}") int maxRetries,
      @Value("${bank.simulator.retry-delay-ms:1000}") long retryDelayMs) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
    this.maxRetries = maxRetries;
    this.retryDelayMs = retryDelayMs;
  }

  @Override
  public BankResponse processPayment(BankRequest request) {
    String endpoint = bankUrl + "/payments";
    int attempt = 0;
    Exception lastException = null;

    // Personal opinion: Could've used a library for retry but this was faster this way
    while (attempt <= maxRetries) {
      try {
        if (attempt > 0) {
          log.info("Retrying bank call, attempt {}/{}", attempt + 1, maxRetries + 1);
          Thread.sleep(retryDelayMs * attempt);
        }

        log.info("Calling bank at {}", endpoint);
        ResponseEntity<BankResponse> response = restTemplate.postForEntity(
            endpoint, request, BankResponse.class);

        BankResponse body = response.getBody();
        log.info("Bank responded: authorized={}", body != null && body.isAuthorized());
        return body;

      } catch (ResourceAccessException e) {
        log.warn("Bank connection failed (attempt {}): {}", attempt + 1, e.getMessage());
        lastException = e;
        attempt++;

      } catch (HttpServerErrorException e) {
        log.warn("Bank returned error {} (attempt {})", e.getStatusCode(), attempt + 1);
        lastException = e;
        attempt++;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BankUnavailableException("Interrupted while waiting to retry", e);
      }
    }

    log.error("Bank unavailable after {} attempts", maxRetries + 1);
    throw new BankUnavailableException(
        "Bank did not respond after " + (maxRetries + 1) + " attempts", lastException);
  }
}