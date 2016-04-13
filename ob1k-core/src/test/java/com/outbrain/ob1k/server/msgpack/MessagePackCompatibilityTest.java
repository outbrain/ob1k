package com.outbrain.ob1k.server.msgpack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.msgpack.MessagePack;
import org.msgpack.core.MessagePack.PackerConfig;
import org.msgpack.jackson.dataformat.JsonArrayFormat;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author marenzo
 * @since 2016-04-05
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagePackCompatibilityTest {

  private MessagePack msgPack;
  private ObjectMapper objectMapper;

  @Before
  public void initialize() {

    // creating old msgpack client
    msgPack = new MessagePack();
    msgPack.register(BasicEntity.class);

    // creating new msgpack client (via ObjectMapper)
    objectMapper = new ObjectMapper(new MessagePackFactory(new PackerConfig().withStr8FormatSupport(false)));
    objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setAnnotationIntrospector(new JsonArrayFormat());
  }

  @Test
  public void serializingCompatibility() throws Exception {

    final BasicEntity entity = new BasicEntity(1, "blabla", asList("v", "b", "b", "f", "r"), singletonMap("v", 5));

    final byte[] oldBytes = msgPack.write(entity);
    final byte[] newBytes = objectMapper.writeValueAsBytes(entity);

    assertArrayEquals("serializations should be the same", oldBytes, newBytes);

    final BasicEntity oldEntity = msgPack.read(newBytes, BasicEntity.class);
    final BasicEntity newEntity = objectMapper.readValue(oldBytes, BasicEntity.class);

    assertEquals("both entities should be the same", oldEntity, newEntity);
  }

  public static class BasicEntity {

    public int number;
    public String value;
    public List<String> values;
    public Map<String, Integer> map;

    public BasicEntity() {

    }

    public BasicEntity(final int number, final String value, final List<String> values, final Map<String, Integer> map) {
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
}
