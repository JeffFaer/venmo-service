package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VenmoResponse<T> {
  private final Optional<T> data;
  private final Optional<Pagination> pagination;
  private final Optional<VenmoException> exception;

  VenmoResponse(@JsonProperty("data") T data, @JsonProperty("pagination") Pagination pagination,
      @JsonProperty("error") VenmoException error) {
    this.data = Optional.ofNullable(data);
    this.pagination = Optional.ofNullable(pagination);
    exception = Optional.ofNullable(error);
  }

  public T getData() throws VenmoException {
    return data.orElseThrow(exception::get);
  }

  public Optional<Pagination> getPagination() {
    return pagination;
  }

  public Optional<VenmoException> getException() {
    return exception;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [");
    if (data.isPresent()) {
      builder.append(" data=");
      builder.append(data.get());
      if (pagination.isPresent()) {
        builder.append(", pagination=");
        builder.append(pagination.get());
      }
    }
    if (exception.isPresent()) {
      builder.append(" exception=");
      builder.append(exception.get());
    }
    builder.append("]");
    return builder.toString();
  }
}
