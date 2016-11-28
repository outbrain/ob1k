package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Eran Harel
 */
public class JsonTranscoderTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonTranscoder<Person> transcoder = new JsonTranscoder<>(objectMapper, Person.class);

  @Test
  public void testRoundTripTranscoding() {
    final Person person = new Person(66, "Saba", 90);
    final Person decodedPerson = transcoder.decode(transcoder.encode(person));
    Assert.assertEquals("decoded person age", person.age, decodedPerson.age);
    Assert.assertEquals("decoded person name", person.name, decodedPerson.name);
    Assert.assertEquals("decoded person weight", person.weight, decodedPerson.weight, 0.01f);
    Assert.assertNotSame(person, decodedPerson);
  }

}
