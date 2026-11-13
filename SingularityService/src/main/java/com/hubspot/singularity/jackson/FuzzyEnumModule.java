package com.hubspot.singularity.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads the enumerated values of an entity the permissive way the service has always read them, and refuses
 * one it cannot read with the message the service has always answered. A type that brings its own creator,
 * or whose constants carry an annotation of the serialisation library, is left to the library to read.
 */
public class FuzzyEnumModule extends Module {

  private static class PermissiveEnumDeserializer extends StdScalarDeserializer<Enum<?>> {
    private static final long serialVersionUID = 1L;

    private final Enum<?>[] constants;
    private final List<String> acceptedValues;

    @SuppressWarnings("unchecked")
    protected PermissiveEnumDeserializer(Class<Enum<?>> clazz) {
      super(clazz);
      this.constants = ((Class<Enum<?>>) handledType()).getEnumConstants();
      this.acceptedValues = new ArrayList<>();
      for (Enum<?> constant : constants) {
        acceptedValues.add(constant.name());
      }
    }

    @Override
    public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
      Enum<?> constant = FuzzyEnums.fromStringFuzzy(jp.getText(), constants);
      if (constant != null) {
        return constant;
      }
      throw ctxt.weirdStringException(
        jp.getText(),
        handledType(),
        jp.getText() + " was not one of " + acceptedValues
      );
    }
  }

  private static class PermissiveEnumDeserializers extends Deserializers.Base {

    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<?> findEnumDeserializer(
      Class<?> type,
      DeserializationConfig config,
      BeanDescription desc
    )
      throws JsonMappingException {
      if (
        config.hasDeserializationFeatures(
          DeserializationFeature.READ_ENUMS_USING_TO_STRING.getMask()
        )
      ) {
        return null;
      }

      final Collection<AnnotatedMethod> factoryMethods = desc.getFactoryMethods();
      if (factoryMethods != null) {
        for (AnnotatedMethod am : factoryMethods) {
          if (am.hasAnnotation(JsonCreator.class)) {
            return null;
          }
        }
      }

      for (Field field : type.getFields()) {
        for (Annotation annotation : field.getAnnotations()) {
          final String packageName = annotation.annotationType().getPackage().getName();
          if (packageName.equals("com.fasterxml.jackson.annotation")) {
            return null;
          }
        }
      }

      return new PermissiveEnumDeserializer((Class<Enum<?>>) type);
    }
  }

  @Override
  public String getModuleName() {
    return "permissive-enums";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(final SetupContext context) {
    context.addDeserializers(new PermissiveEnumDeserializers());
  }
}
