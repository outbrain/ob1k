package com.outbrain.ob1k.server.registry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by aronen on 2/8/14.
 * a trie based data structure that holds the different parts of the (URL)path in an efficient way
 */
public class PathTrie<T> {
  private final TrieNode root;
  private final String separator;
  private final String wildcard;

  public PathTrie() {
    this('/', "*");
  }

  public PathTrie(final char separator, final String wildcard) {
    this.separator = String.valueOf(separator);
    this.wildcard = wildcard;
    this.root = new TrieNode(new String(new char[] { separator }), null, false);
  }

  public class TrieNode {

    private final String token;
    private final String namedWildcard;
    private final Map<String, TrieNode> children;
    private boolean allowPrefix;

    private T value;

    public TrieNode(final String token, final T value, final boolean allowPrefix) {
      this.token = token;
      this.value = value;
      this.allowPrefix = allowPrefix;
      this.children = new HashMap<>();
      if (isNamedWildcard(token)) {
        namedWildcard = token.substring(token.indexOf('{') + 1, token.indexOf('}'));
      } else {
        namedWildcard = null;
      }
    }

    private boolean isNamedWildcard(final String key) {
      return key.indexOf('{') != -1 && key.indexOf('}') != -1;
    }

    public void insert(final String[] path, final int index, final T value, final boolean allowPrefix) {
      if (index >= path.length)
        return;

      final String token = path[index];
      final String key = isNamedWildcard(token) ? wildcard : token;

      TrieNode node = children.get(key);
      if (node == null) {
        if (index == (path.length - 1)) {
          node = new TrieNode(token, value, allowPrefix);
        } else {
          node = new TrieNode(token, null, false);
        }
        children.put(key, node);
      } else {
        if (isNamedWildcard(token) && !node.token.equals(token)) {
          throw new RuntimeException("parametrized path " + toPath(path) + " can't be bound twice");
        }

        // in case the target(last) node already exist but without a value
        // than the value should be updated.
        if (index == (path.length - 1)) {
          assert (node.value == null || node.value == value);
          if (node.value == null) {
            node.value = value;
            node.allowPrefix = allowPrefix;
          } else {
            throw new RuntimeException("path " + toPath(path) + " can't be bound twice");
          }
        }
      }

      node.insert(path, index + 1, value, allowPrefix);
    }

    private String toPath(final String[] path) {
      final StringBuilder builder = new StringBuilder();
      for (final String pathPart : path) {
        builder.append(separator);
        builder.append(pathPart);
      }

      return builder.toString();
    }

    public T retrieve(final String[] path, final int index, final Map<String, String> params) {
      if (index >= path.length)
        return null;

      final String token = path[index];
      if (token.isEmpty()) {
        return retrieve(path, index + 1, params);
      }

      TrieNode node = children.get(token);
      boolean usedWildcard = false;
      if (node == null) {
        node = children.get(wildcard);
        if (node == null) {
          if (allowPrefix) {
            return value;
          } else {
            return null;
          }
        }
        usedWildcard = true;
      }

      updateParams(params, node, token);

      if (index == (path.length - 1)) {
        return node.value;
      }

      // try the explicit path.
      T res = node.retrieve(path, index + 1, params);
      if (res == null && !usedWildcard) {
        // try the wildcard path.
        node = children.get(wildcard);
        if (node != null) {
          updateParams(params, node, token);
          res = node.retrieve(path, index + 1, params);
        }
      }

      return res;
    }

    private void updateParams(final Map<String, String> params, final TrieNode node, final String value) {
      if (params != null && node.namedWildcard != null) {
        params.put(node.namedWildcard, value);
      }
    }

    public void collectPathMappings(final Map<String, T> collectedMappings) {
      // skip the root...
      for (final TrieNode child : children.values()) {
        child.collectPathMappings("", collectedMappings);
      }
    }

    private void collectPathMappings(final String parentPath, final Map<String, T> collectedMappings) {
      final String currentPath = parentPath + '/' + (token == null ? '{' + namedWildcard + '}' : token);
      if (value != null) {
        collectedMappings.put(currentPath, value);
      }

      for (final TrieNode child : children.values()) {
        child.collectPathMappings(currentPath, collectedMappings);
      }
    }
  }

  public void insert(final String path, final T value, final boolean allowPrefix) {
    final String[] tokens = splitPath(path);
    if (tokens.length == 0) {
      root.value = value;
      return;
    }

    root.insert(tokens, 0, value, allowPrefix);
  }

  private String[] splitPath(final String path) {
    String[] tokens = path.split(separator);
    if (tokens.length > 0 && tokens[0].isEmpty()) {
      tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
    }

    if (tokens.length > 0 && tokens[tokens.length - 1].isEmpty()) {
      tokens = Arrays.copyOfRange(tokens, 0, tokens.length - 1);
    }

    return tokens;
  }

  public T retrieve(final String path) {
    return retrieve(path, null);
  }

  public T retrieve(final String path, final Map<String, String> params) {
    final String[] strings = splitPath(path);
    if (strings.length == 0) {
      return root.value;
    }

    return root.retrieve(strings, 0, params);
  }

  public SortedMap<String, T> getPathToValueMapping() {
    final SortedMap<String, T> result = new TreeMap<>();
    root.collectPathMappings(result);
    return result;
  }

}
