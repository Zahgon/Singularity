package com.hubspot.singularity.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Reads a configuration file into the configuration class and refuses one the service cannot run on. An
 * empty file is refused, a file whose text is not an object is refused, and a file that breaks one of the
 * configuration's own constraints is refused with the constraints it breaks listed one to a line.
 */
public class ConfigurationFactory<T> {
  private final Class<T> klass;
  private final ObjectMapper mapper;
  private final Validator validator;

  public ConfigurationFactory(Class<T> klass, ObjectMapper mapper) {
    this(klass, mapper, Validation.buildDefaultValidatorFactory().getValidator());
  }

  public ConfigurationFactory(Class<T> klass, ObjectMapper mapper, Validator validator) {
    this.klass = klass;
    this.mapper = mapper;
    this.validator = validator;
  }

  /** The configuration a file holds, or the default configuration when no file is named. */
  public T build(ConfigurationSourceProvider provider, String path)
    throws IOException, ConfigurationException {
    try (InputStream input = provider.open(path)) {
      final JsonNode node = mapper.readTree(mapper.getFactory().createParser(input));
      if (node == null) {
        throw new ConfigurationException(path + " has no content");
      }
      return read(node, path);
    }
  }

  public T build() throws IOException, ConfigurationException {
    try {
      final T instance = klass.getDeclaredConstructor().newInstance();
      return read(mapper.valueToTree(instance), "default configuration");
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new IllegalArgumentException(
        "Unable create an instance of the configuration class: '" +
        klass.getCanonicalName() +
        "'",
        e
      );
    }
  }

  private T read(JsonNode node, String path) throws IOException, ConfigurationException {
    try {
      final JsonParser parser = new TreeTraversingParser(node, mapper);
      final T config = mapper.readValue(parser, klass);
      validate(path, config);
      return config;
    } catch (InvalidFormatException e) {
      throw new ConfigurationException(
        path +
        " has an error:" +
        System.lineSeparator() +
        "  * Incorrect type of value at: " +
        fieldPath(e) +
        "; is of type: " +
        (e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName()) +
        ", expected: " +
        e.getTargetType().getSimpleName() +
        System.lineSeparator(),
        e
      );
    }
  }

  private void validate(String path, T config) throws ConfigurationException {
    final Set<ConstraintViolation<T>> violations = validator.validate(config);
    if (!violations.isEmpty()) {
      final List<String> messages = new ArrayList<>();
      for (ConstraintViolation<T> violation : violations) {
        messages.add(violation.getPropertyPath() + " " + violation.getMessage());
      }
      Collections.sort(messages);
      final StringBuilder text = new StringBuilder(path)
        .append(" has an error:")
        .append(System.lineSeparator());
      for (String message : messages) {
        text.append("  * ").append(message).append(System.lineSeparator());
      }
      throw new ConfigurationException(text.toString());
    }
  }

  private static String fieldPath(com.fasterxml.jackson.databind.JsonMappingException e) {
    final StringBuilder path = new StringBuilder();
    for (com.fasterxml.jackson.databind.JsonMappingException.Reference reference : e.getPath()) {
      if (path.length() > 0) {
        path.append('.');
      }
      path.append(
        reference.getFieldName() == null
          ? "[" + reference.getIndex() + "]"
          : reference.getFieldName()
      );
    }
    return path.toString();
  }
}
