package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author Eran Harel
 */
public class JacksonTranscoderTest {

  @Test
  public void testRoundTripTranscoding_Json() {
    final JacksonTranscoder<Person> transcoder = createTranscoder(new JsonFactory());
    testRoundTripTranscoding(transcoder);
  }

  @Test
  public void testRoundTripTranscoding_MsgPack() {
    final JacksonTranscoder<Person> transcoder = createTranscoder(new MessagePackFactory());
    testRoundTripTranscoding(transcoder);
  }

  private void testRoundTripTranscoding(final JacksonTranscoder<Person> transcoder) {
    final Person person = new Person(66, "Saba", 90);
    final Person decodedPerson = transcoder.decode(transcoder.encode(person));
    Assert.assertEquals("decoded person age", person.age, decodedPerson.age);
    Assert.assertEquals("decoded person name", person.name, decodedPerson.name);
    Assert.assertEquals("decoded person weight", person.weight, decodedPerson.weight, 0.01f);
    Assert.assertNotSame(person, decodedPerson);
  }

  private JacksonTranscoder<Person> createTranscoder(final JsonFactory jsonFactory) {
    final ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
    return new JacksonTranscoder<>(objectMapper, Person.class);
  }
}
