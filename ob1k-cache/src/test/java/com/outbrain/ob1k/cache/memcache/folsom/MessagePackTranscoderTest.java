package com.outbrain.ob1k.cache.memcache.folsom;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.MessagePack;

/**
 * @author Eran Harel
 */
public class MessagePackTranscoderTest {
  private final MessagePack messagePack = new MessagePack();
  private final MessagePackTranscoder<Person> transcoder = new MessagePackTranscoder<>(messagePack, Person.class);

  @Before
  public void setup() {
    messagePack.register(Person.class);
  }

  @Test
  public void testRoundTripTranscoding() {
    final Person person = new Person(55, "Saba", 99);
    final Person decodedPerson = transcoder.decode(transcoder.encode(person));
    Assert.assertEquals("decoded person age", person.age, decodedPerson.age);
    Assert.assertEquals("decoded person name", person.name, decodedPerson.name);
    Assert.assertEquals("decoded person weight", person.weight, decodedPerson.weight, 0.01f);
    Assert.assertNotSame(person, decodedPerson);
  }

}
