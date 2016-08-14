package com.outbrain.ob1k.server.msgpack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.msgpack.MessagePack;
import org.msgpack.annotation.Message;
import org.msgpack.core.MessagePack.PackerConfig;
import org.msgpack.jackson.dataformat.JsonArrayFormat;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.jackson.dataformat.MessagePackSerializerFactory;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This tests are verifying that msgpack-java 0.8 is backward-compatible with 0.6.
 *
 * @author marenzo
 * @since 2016-04-05
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagePackCompatibilityTest {

  private MessagePack msgPack;
  private ObjectMapper objectMapper;

  // entities to check
  private BasicEntity basicEntity;
  private MinimalEntity minimalEntity;

  @Before
  public void initialize() {

    // pojo to test
    basicEntity = new BasicEntity(1, "word", asList("abc", "def"), singletonMap(true, "v"));
    minimalEntity = new MinimalEntity(2, "string", asList("tar", "xzf"));

    // creating old msgpack client
    msgPack = new MessagePack();

    // creating new msgpack client (via ObjectMapper)
    objectMapper = new ObjectMapper(new MessagePackFactory(new PackerConfig().withStr8FormatSupport(false)));
    objectMapper.setSerializerFactory(new MessagePackSerializerFactory());
    objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setAnnotationIntrospector(new JsonArrayFormat());
  }

  @Test
  public void pojoCompatibility() throws Exception {

    final byte[] oldBytes = msgPack.write(basicEntity);
    final byte[] newBytes = objectMapper.writeValueAsBytes(basicEntity);

    assertArrayEquals("serializations should be the same", oldBytes, newBytes);

    final BasicEntity oldEntity = msgPack.read(newBytes, BasicEntity.class);
    final BasicEntity newEntity = objectMapper.readValue(oldBytes, BasicEntity.class);

    assertEquals("both entities should be the same", oldEntity, newEntity);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void collectionsCompatibility() throws Exception {

    final List<BasicEntity> entities = asList(basicEntity, basicEntity);

    final byte[] oldBytes = msgPack.write(entities);
    final byte[] newBytes = objectMapper.writeValueAsBytes(entities);

    assertArrayEquals("serializations should be the same", oldBytes, newBytes);

    final Type entitiesType = new TypeToken<List<BasicEntity>>() {
    }.getType();

    final Template template = msgPack.lookup(entitiesType);
    final Value value = msgPack.read(newBytes);

    final JavaType jacksonType = objectMapper.constructType(entitiesType);

    final List<BasicEntity> newEntities = (List<BasicEntity>) template.read(new Converter(msgPack, value), null);
    final List<BasicEntity> oldEntities = objectMapper.readValue(oldBytes, jacksonType);

    assertEquals("both entities should be the same", newEntities, oldEntities);
  }

  @Test
  public void basicTypeMapCompatibility() throws Exception {

    // Test a Integer-type key map
    createMapCompatibilityTest(singletonMap(1, basicEntity), new TypeReference<Map<Integer, BasicEntity>>() {
    }.getType());

    // Test a Boolean-type key map
    createMapCompatibilityTest(singletonMap(true, basicEntity), new TypeReference<Map<Boolean, BasicEntity>>() {
    }.getType());

    // Test a String-type key map
    createMapCompatibilityTest(singletonMap("hello", basicEntity), new TypeReference<Map<String, BasicEntity>>() {
    }.getType());
  }

  @Test
  public void unknownProperties() throws Exception {

    final byte[] basicBytes = msgPack.write(basicEntity);
    final byte[] minimalBytes = msgPack.write(minimalEntity);

    final MinimalEntity minimal = objectMapper.readValue(basicBytes, MinimalEntity.class);
    final BasicEntity basic = objectMapper.readValue(minimalBytes, BasicEntity.class);

    assertEquals("minimal value should be of basic", basicEntity.value, minimal.value);
    assertEquals("basic value should be of minimal", minimalEntity.value, basic.value);
    assertNull("basic map should be null", basic.map);
  }

  @SuppressWarnings("unchecked")
  private <K, V> void createMapCompatibilityTest(final Map<K, V> entitiesMap, final Type entitiesType) throws IOException {

    final byte[] oldBytes = msgPack.write(entitiesMap);
    final byte[] newBytes = objectMapper.writeValueAsBytes(entitiesMap);

    assertArrayEquals("serializations should be the same", oldBytes, newBytes);

    final Template template = msgPack.lookup(entitiesType);
    final Value value = msgPack.read(newBytes);

    final JavaType jacksonType = objectMapper.constructType(entitiesType);

    final Map<K, V> newEntities = (Map<K, V>) template.read(new Converter(msgPack, value), null);
    final Map<K, V> oldEntities = objectMapper.readValue(oldBytes, jacksonType);

    assertEquals("both entities should be the same", newEntities, oldEntities);
  }

  @Message
  public static class BasicEntity {

    public int number;
    public String value;
    public List<String> values;
    public Map<Boolean, String> map;

    public BasicEntity() {

    }

    public BasicEntity(final int number, final String value, final List<String> values, final Map<Boolean, String> map) {
      this.number = number;
      this.value = value;
      this.values = values;
      this.map = map;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final BasicEntity entity = (BasicEntity) o;
      return number == entity.number &&
        Objects.equals(value, entity.value) &&
        Objects.equals(values, entity.values) &&
        Objects.equals(map, entity.map);
    }

    @Override
    public int hashCode() {
      return Objects.hash(number, value, values, map);
    }
  }

  @Message
  public static class MinimalEntity {

    public int number;
    public String value;
    public List<String> values;

    public MinimalEntity() {

    }

    public MinimalEntity(final int number, final String value, final List<String> values) {
      this.number = number;
      this.value = value;
      this.values = values;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final BasicEntity entity = (BasicEntity) o;
      return number == entity.number &&
        Objects.equals(value, entity.value) &&
        Objects.equals(values, entity.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(number, value, values);
    }
  }
}