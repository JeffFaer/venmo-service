package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class User {
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
