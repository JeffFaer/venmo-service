package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Me {
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class User {
    private final String name;
    private final LocalDateTime dateJoined;

    User(@JsonProperty("display_name") String name,
        @JsonProperty("date_joined") LocalDateTime dateJoined) {
      this.name = name;
      this.dateJoined = dateJoined;
    }

    public String getName() {
      return name;
    }

    public LocalDateTime getDateJoined() {
      return dateJoined;
    }
  }

  private final BigDecimal balance;
  private final User user;

  Me(@JsonProperty("balance") BigDecimal balance, @JsonProperty("user") User user) {
    this.balance = balance;
    this.user = user;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public String getName() {
    return user.getName();
  }

  public LocalDateTime getDateJoined() {
    return user.getDateJoined();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [balance=");
    builder.append(balance);
    builder.append(", getName()=");
    builder.append(getName());
    builder.append(", getDateJoined()=");
    builder.append(getDateJoined());
    builder.append("]");
    return builder.toString();
  }
}
