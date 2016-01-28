package name.falgout.jeffrey.moneydance.venmoservice.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VenmoException extends Exception {
  private static final long serialVersionUID = -8246679200980649874L;
  private final int code;

  VenmoException(@JsonProperty("message") String message, @JsonProperty("code") int code) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + code;
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
    if (!(obj instanceof VenmoException)) {
      return false;
    }
    VenmoException other = (VenmoException) obj;
    if (code != other.code) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [code=");
    builder.append(code);
    builder.append(", getMessage()=");
    builder.append(getMessage());
    builder.append("]");
    return builder.toString();
  }
}
