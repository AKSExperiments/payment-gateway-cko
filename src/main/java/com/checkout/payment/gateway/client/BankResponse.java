package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankResponse {

  private boolean authorized;

  @JsonProperty("authorization_code")
  private String authorizationCode;
}