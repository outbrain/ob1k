package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Eran Harel
 */
public class JacksonTranscoderTest {

  private final static ObjectMapper msgPack = MemcachedClientBuilder.DefaultMessagePackHolder.INSTANCE;
  private final static ObjectMapper json = MemcachedClientBuilder.DefaultObjectMapperHolder.INSTANCE;

  @Test
  public void testRoundTripTranscoding_Entity_JSON() {
    testRoundTripTranscoding_Entity(json);
  }

  @Test
  public void testRoundTripTranscoding_Map_JSON() {
    testRoundTripTranscoding_Map(json);
  }

  @Test
  public void testRoundTripTranscoding_Primitive_JSON() {
    testRoundTripTranscoding_Primitive(json);
  }

  @Test
  public void testRoundTripTransocoding_Entity_MSGPACK() {
    testRoundTripTranscoding_Entity(msgPack);
  }

  @Test
  public void testRoundTripTranscoding_Map_MSGPACK() {
    testRoundTripTranscoding_Map(msgPack);
  }

  @Test
  public void testRoundTripTranscoding_Primitive_MSGPACK() {
    testRoundTripTranscoding_Primitive(msgPack);
  }

  private void testRoundTripTranscoding_Entity(final ObjectMapper objectMapper) {
    final JacksonTranscoder<Person> transcoder = new JacksonTranscoder<>(objectMapper, Person.class);
    final Person person = new Person(66, "Saba", 90);
    final Person decodedPerson = transcoder.decode(transcoder.encode(person));

    assertEquals("decoded person age", person.age, decodedPerson.age);
    assertEquals("decoded person name", person.name, decodedPerson.name);
    assertEquals("decoded person weight", person.weight, decodedPerson.weight, 0.01f);
    assertNotSame(person, decodedPerson);
  }

  private void testRoundTripTranscoding_Map(final ObjectMapper objectMapper) {
    final Type mapType = new TypeReference<Map<String, Integer>>() {}.getType();
    final JavaType javaType = objectMapper.constructType(mapType);
    final JacksonTranscoder<Map<String, Integer>> t = new JacksonTranscoder<>(objectMapper, javaType);

    final Map<String, Integer> map = singletonMap("hello", 42);
    final Map<String, Integer> decodedMap = t.decode(t.encode(map));

    assertEquals("decoded map keys", map.keySet(), decodedMap.keySet());
    assertEquals("decoded map value", map.get("hello"), decodedMap.get("hello"));
  }

  private void testRoundTripTranscoding_Primitive(final ObjectMapper objectMapper) {
    final JacksonTranscoder<Long> transcoder = new JacksonTranscoder<>(objectMapper, Long.class);

    final long val = 666L;
    final long decodedVal = transcoder.decode(transcoder.encode(val));
    assertEquals(val, decodedVal);
  }
}