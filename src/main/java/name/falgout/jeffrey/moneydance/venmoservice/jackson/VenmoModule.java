package name.falgout.jeffrey.moneydance.venmoservice.jackson;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
    setDeserializerModifier(new BeanDeserializerModifier() {
      @SuppressWarnings("rawtypes")
      @Override
      public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
          BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return new JsonDeserializer<Enum>() {
          @SuppressWarnings("unchecked")
          @Override
          public Enum deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
            return Enum.valueOf((Class<? extends Enum>) type.getRawClass(),
                p.getValueAsString().toUpperCase());
          }
        };
      }
    });

  }
}
