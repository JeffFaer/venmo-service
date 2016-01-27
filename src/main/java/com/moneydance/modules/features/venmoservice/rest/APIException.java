package com.moneydance.modules.features.venmoservice.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIException extends Exception {
  private static final long serialVersionUID = -8246679200980649874L;
  private final int code;

  APIException(@JsonProperty("message") String message, @JsonProperty("code") int code) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
