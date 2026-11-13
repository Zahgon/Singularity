package com.hubspot.singularity.jackson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.net.HostAndPort;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import java.io.IOException;

/**
 * The object mappers the service reads and writes with. Both the entities of the API and the configuration
 * file are read by a mapper of this shape: the Guava, Joda, Java 8 and Java time data types, enumerated
 * values read permissively, parameter names, the Mesos protocol buffers, absent values left out of what is
 * written, and fields the service does not know left alone rather than refused.
 */
public final class SingularityObjectMappers {

  private SingularityObjectMappers() {}

  /**
   * The mapper the service's own objects read and write with, and the one a configuration file is read by:
   * every data type the service holds, and enumerated values read permissively.
   */
  public static ObjectMapper newObjectMapper() {
    return configure(new ObjectMapper());
  }

  /**
   * The mapper the API reads and writes entities with, which is not the same one: it holds the Guava, Java 8
   * and Mesos data types and leaves out what the service does not read entities with, so an enumerated value
   * in an entity is read exactly as it is written, and only absent values are left out of what is written.
   */
  public static ObjectMapper newApiObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_ABSENT);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new ProtobufModule());
    mapper.registerModule(new Jdk8Module());
    return mapper;
  }

  /** The same mapper over a YAML syntax, which is what a configuration file is written in. */
  public static ObjectMapper newYamlObjectMapper() {
    return configure(new ObjectMapper(new YAMLFactory()));
  }

  public static ObjectMapper newObjectMapper(JsonFactory factory) {
    return configure(new ObjectMapper(factory));
  }

  private static ObjectMapper configure(ObjectMapper mapper) {
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new GuavaExtrasModule());
    mapper.registerModule(new JodaModule());
    mapper.registerModule(new FuzzyEnumModule());
    mapper.registerModule(new ParameterNamesModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new ProtobufModule());
    mapper.setSerializationInclusion(Include.NON_ABSENT);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  /**
   * The host-and-port type the service's own configuration holds, read from the text a configuration file
   * writes it as. The serialisation library has no reader of its own for it.
   */
  static class GuavaExtrasModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    GuavaExtrasModule() {
      super("guava-extras");
      addDeserializer(
        HostAndPort.class,
        new com.fasterxml.jackson.databind.JsonDeserializer<HostAndPort>() {

          @Override
          public HostAndPort deserialize(
            com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context
          )
            throws IOException {
            return HostAndPort.fromString(parser.getValueAsString());
          }
        }
      );
    }
  }
}
