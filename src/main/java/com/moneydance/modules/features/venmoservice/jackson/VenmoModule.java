package com.moneydance.modules.features.venmoservice.jackson;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class VenmoModule extends SimpleModule {
  private static final long serialVersionUID = 2046605587084383357L;

  public VenmoModule() {
    addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
      @Override
      public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        return LocalDateTime.parse(p.getValueAsString(), DateTimeFormatter.ISO_DATE_TIME);
      }
    });
  }
}
