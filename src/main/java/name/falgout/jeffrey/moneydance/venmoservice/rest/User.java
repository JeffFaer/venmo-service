package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class User {
  private final String name;
  private final ZonedDateTime dateJoined;

  User(@JsonProperty("display_name") String name,
      @JsonProperty("date_joined") ZonedDateTime dateJoined) {
    this.name = name;
    this.dateJoined = dateJoined;
  }

  public String getName() {
    return name;
  }

  public ZonedDateTime getDateJoined() {
    return dateJoined;
  }
}
