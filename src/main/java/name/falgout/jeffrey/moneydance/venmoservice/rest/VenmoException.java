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
