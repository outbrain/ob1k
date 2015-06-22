package com.outbrain.ob1k.security.server;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.security.server.PathAssociations.PathAssociationsBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PathAssociationsTest {

  private static final String ASSOCIATED_PATH = "/part";

  private CredentialsAuthenticator<String> rootAssociatedAuthenticator;
  private CredentialsAuthenticator<String> pathAssociatedAuthenticator;

  private PathAssociations<String> pathAssociations;

  @Before
  public void setup() {
    rootAssociatedAuthenticator = new DummyAuthenticator();
    pathAssociatedAuthenticator = new DummyAuthenticator();

    pathAssociations = new PathAssociationsBuilder<String>()
      .associate("/", rootAssociatedAuthenticator)
      .associate(ASSOCIATED_PATH, pathAssociatedAuthenticator)
      .build();
  }

  @After
  public void tearDown() {
    rootAssociatedAuthenticator = null;
    pathAssociatedAuthenticator = null;
    pathAssociations = null;
  }

  @Test
  //Tests an authenticator that's associated with the root url
  public void testRootAssociation() {
    assertTrue(pathAssociations.isAuthorized(rootAssociatedAuthenticator, "/"));
    assertTrue(pathAssociations.isAuthorized(rootAssociatedAuthenticator, ASSOCIATED_PATH));
    assertTrue(pathAssociations.isAuthorized(rootAssociatedAuthenticator, ASSOCIATED_PATH + "/endpoint"));
  }

  @Test
  //Tests an authenticator that's associated with
  public void testSpecificAssociation() {
    assertTrue(pathAssociations.isAuthorized(pathAssociatedAuthenticator, ASSOCIATED_PATH));
    assertTrue(pathAssociations.isAuthorized(pathAssociatedAuthenticator, ASSOCIATED_PATH + "/endpoint"));

    assertFalse(pathAssociations.isAuthorized(pathAssociatedAuthenticator, "/"));
    assertFalse(pathAssociations.isAuthorized(pathAssociatedAuthenticator, "/just/made/up/"));
  }

  private class DummyAuthenticator implements CredentialsAuthenticator<String> {

    @Override
    public ComposableFuture<Boolean> authenticate(final Credentials<String> credentials) {
      return ComposableFutures.fromValue(false);
    }

    @Override
    public String getId() {
      return null;
    }
  }

  @Test
  public void testGetAuthenticatorsRootPath() {
    final Set<CredentialsAuthenticator<String>> authenticators = pathAssociations.getAuthenticators("/");
    assertEquals(1, authenticators.size());
    assertTrue(authenticators.contains(rootAssociatedAuthenticator));
  }

  @Test
  public void testGetAuthenticatorsSpecificPath() {
    final Set<CredentialsAuthenticator<String>> authenticators = pathAssociations.getAuthenticators(ASSOCIATED_PATH);
    assertEquals(2, authenticators.size());
    assertTrue(authenticators.contains(rootAssociatedAuthenticator));
    assertTrue(authenticators.contains(pathAssociatedAuthenticator));
  }

  @Test
  public void testGetAuthenticatorsSomePath() {
    final Set<CredentialsAuthenticator<String>> authenticators = pathAssociations.getAuthenticators("just/made/up");
    assertEquals(1, authenticators.size());
    assertTrue(authenticators.contains(rootAssociatedAuthenticator));
  }

  @Test
  public void testGetExistingAuthenticator() {
    assertEquals(rootAssociatedAuthenticator, pathAssociations.getAuthenticator(rootAssociatedAuthenticator.getId()));
  }

  @Test
  public void testGetNonExistingAuthenticator() {
    assertNull(pathAssociations.getAuthenticator("made-up-id"));
  }
}
