package com.outbrain.ob1k.cache.memcache.folsom;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.template.Templates;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eran Harel
 */
public class MessagePackTranscoderTest {
  private final MessagePack messagePack = new MessagePack();

  @Before
  public void setup() {
    messagePack.register(Person.class);
  }

  @Test
  public void testRoundTripTranscoding() {
    final MessagePackTranscoder<Person> transcoder = new MessagePackTranscoder<>(messagePack, Person.class);

    final Person person = new Person(55, "Saba", 99);
    final Person decodedPerson = transcoder.decode(transcoder.encode(person));
    Assert.assertEquals("decoded person age", person.age, decodedPerson.age);
    Assert.assertEquals("decoded person name", person.name, decodedPerson.name);
    Assert.assertEquals("decoded person weight", person.weight, decodedPerson.weight, 0.01f);
    Assert.assertNotSame(person, decodedPerson);
  }

  @Test
  public void testRoundTripTranscoding_map() throws IOException {
    final Template<Map<String, Integer>> mapTemplate = Templates.tMap(Templates.TString, Templates.TInteger);
    final MessagePackTranscoder<Map<String, Integer>> t = new MessagePackTranscoder<Map<String, Integer>>(messagePack, mapTemplate);

    final Map<String, Integer> map = new HashMap<>();
    map.put("1", 1);
    map.put("2", 2);

    final Map<String, Integer> decodedMap = t.decode(t.encode(map));
    Assert.assertEquals(map, decodedMap);
  }

}
