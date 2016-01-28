package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.net.URI;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pagination {
  private final Optional<URI> next;
  private final Optional<URI> previous;

  Pagination(@JsonProperty("next") URI next, @JsonProperty("previous") URI previous) {
    this.next = Optional.ofNullable(next);
    this.previous = Optional.ofNullable(previous);
  }

  public Optional<URI> getNext() {
    return next;
  }

  public Optional<URI> getPrevious() {
    return previous;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [");
    if (next.isPresent()) {
      builder.append("next=");
      builder.append(next.get());
    }
    if (next.isPresent() && previous.isPresent()) {
      builder.append(", ");
    }
    if (previous.isPresent()) {
      builder.append("previous=");
      builder.append(previous.get());
    }
    builder.append("]");
    return builder.toString();
  }
}
