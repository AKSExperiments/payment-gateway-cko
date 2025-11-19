package com.checkout.payment.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

  private String message;
  private List<String> details;

  public ErrorResponse(String message) {
    this.message = message;
    this.details = List.of();
  }
}