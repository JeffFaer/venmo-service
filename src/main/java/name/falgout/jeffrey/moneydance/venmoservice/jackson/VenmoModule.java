package name.falgout.jeffrey.moneydance.venmoservice.jackson;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Payment;
import name.falgout.jeffrey.moneydance.venmoservice.rest.Payment.Status;

public class VenmoModule extends SimpleModule {
  private static final long serialVersionUID = 2046605587084383357L;

  public VenmoModule() {
    addDeserializer(ZonedDateTime.class, new JsonDeserializer<ZonedDateTime>() {
      @Override
      public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        return ZonedDateTime
            .of(LocalDateTime.parse(p.getValueAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                ZoneOffset.UTC)
            .withZoneSameInstant(ZoneId.systemDefault());
      }
    });

    addDeserializer(Payment.Status.class, new JsonDeserializer<Payment.Status>() {
      @Override
      public Status deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        return Payment.Status.valueOf(p.getValueAsString().toUpperCase());
      }
    });
  }
}
