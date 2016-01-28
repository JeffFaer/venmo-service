package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Target {
    private final Optional<User> user;

    Target(@JsonProperty("user") User user) {
      this.user = Optional.ofNullable(user);
    }

    public Optional<User> getUser() {
      return user;
    }
  }

  public enum Status {
    SETTLED, PENDING, FAILED, EXPIRED;
  }

  private final Status status;
  private final Target target;
  private final User actor;
  private final BigDecimal amount;
  private final LocalDateTime dateCreated;
  private final LocalDateTime dateCompleted;
  private final String note;

  Payment(@JsonProperty("status") Status status, @JsonProperty("target") Target target,
      @JsonProperty("actor") User actor, @JsonProperty("amount") BigDecimal amount,
      @JsonProperty("date_created") LocalDateTime dateCreated,
      @JsonProperty("date_completed") LocalDateTime dateCompleted,
      @JsonProperty("note") String note) {
    this.status = status;
    this.target = target;
    this.actor = actor;
    this.amount = amount;
    this.dateCreated = dateCreated;
    this.dateCompleted = dateCompleted;
    this.note = note;
  }

  public Status getStatus() {
    return status;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDateTime getDateCreated() {
    return dateCreated;
  }

  public LocalDateTime getDateCompleted() {
    return dateCompleted;
  }

  public String getNote() {
    return note;
  }

  public String getSourceName() {
    return actor.getName();
  }

  public Optional<String> getDestinationName() {
    return target.getUser().map(User::getName);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [status=");
    builder.append(status);
    builder.append(", amount=");
    builder.append(amount);
    builder.append(", dateCreated=");
    builder.append(dateCreated);
    builder.append(", dateCompleted=");
    builder.append(dateCompleted);
    builder.append(", note=");
    builder.append(note);
    builder.append(", getSourceName()=");
    builder.append(getSourceName());
    if (getDestinationName().isPresent()) {
      builder.append(", getDestinationName()=");
      builder.append(getDestinationName().get());
    }
    builder.append("]");
    return builder.toString();
  }
}
