package com.moneydance.modules.features.venmoservice.rest;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VenmoResponse<T> {
  private final Optional<T> data;
  private final Optional<APIException> exception;

  VenmoResponse(@JsonProperty("data") T data, @JsonProperty("error") APIException error) {
    this.data = Optional.ofNullable(data);
    exception = Optional.ofNullable(error);
  }

  public T getData() throws APIException {
    return data.orElseThrow(exception::get);
  }

  public Optional<APIException> getException() {
    return exception;
  }
}
