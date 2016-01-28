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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((next == null) ? 0 : next.hashCode());
    result = prime * result + ((previous == null) ? 0 : previous.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Pagination)) {
      return false;
    }
    Pagination other = (Pagination) obj;
    if (next == null) {
      if (other.next != null) {
        return false;
      }
    } else if (!next.equals(other.next)) {
      return false;
    }
    if (previous == null) {
      if (other.previous != null) {
        return false;
      }
    } else if (!previous.equals(other.previous)) {
      return false;
    }
    return true;
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
