package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pagination {
  private final URI next;
  private final URI previous;

  Pagination(@JsonProperty("next") URI next, @JsonProperty("previous") URI previous) {
    this.next = next;
    this.previous = previous;
  }

  public URI getNext() {
    return next;
  }

  public URI getPrevious() {
    return previous;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [next=");
    builder.append(next);
    builder.append(", previous=");
    builder.append(previous);
    builder.append("]");
    return builder.toString();
  }
}
