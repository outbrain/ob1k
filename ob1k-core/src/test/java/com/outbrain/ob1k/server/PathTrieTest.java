package com.outbrain.ob1k.server;

import com.outbrain.ob1k.server.registry.PathTrie;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by aronen on 2/9/14.
 * tests the path trie
 */
public class PathTrieTest {

  @Test
  public void testBasicMappings() {
    final PathTrie<String> trie = new PathTrie<>();

    trie.insert("/", "root", false);
    trie.insert("a", "shorter", false);
    trie.insert("/a/b", "walla", false);
    trie.insert("/a/b/c", "full", true);
    trie.insert("/a/b/e", "full", false);

    final String res0 = trie.retrieve("");
    Assert.assertEquals(res0, "root");

    final String res = trie.retrieve("/a/b");
    final String res3 = trie.retrieve("a/b/");
    Assert.assertEquals(res, "walla");
    Assert.assertEquals(res3, "walla");

    final String res4 = trie.retrieve("/a");
    Assert.assertEquals(res4, "shorter");

    final String res5 = trie.retrieve("a/b/c/d");
    Assert.assertEquals(res5, "full");

    final String res6 = trie.retrieve("a/b/e/d");
    Assert.assertNull(res6);

    trie.insert("/{context}/{service}/{method}/{id}", "result", false);
    final HashMap<String, String> params = new HashMap<>();
    final String res2 = trie.retrieve("/ctx/srv1/action1/3", params);
    Assert.assertEquals(res2, "result");

    Assert.assertEquals(params.size(), 4);
    Assert.assertTrue(params.get("context").equals("ctx"));
    Assert.assertTrue(params.get("service").equals("srv1"));
    Assert.assertTrue(params.get("method").equals("action1"));
    Assert.assertTrue(params.get("id").equals("3"));
  }

}