package com.outbrain.ob1k.security.providers;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.security.server.Credentials;
import com.outbrain.ob1k.security.server.CredentialsAuthenticator;
import com.outbrain.ob1k.security.server.UserPasswordToken;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

/**
 * <p>
 * A credentials authenticator that authenticates against an LDAP server.</p>
 * </p>
 * <p>
 * This authenticator uses a synchronous LDAP client (spring-ldap) and wraps it to conform to OB1K's
 * ComposableFutures interface. Be aware that for a large amount of authentication requests this authenticator
 * might overload the threads queue
 * </p>
 *
 * @author guymarom
 * @see <a href="http://projects.spring.io/spring-ldap/">Spring LDAP Project</a>
 */
public class LdapCredentialsAuthenticator implements CredentialsAuthenticator<UserPasswordToken> {

  private static final String UID_ATTRIBUTE = "uid";

  private final LdapTemplate ldapTemplate;
  private final String id;

  /**
   * This constructor creates a LdapCredentialsAuthenticator that authenticates against an LDAP server
   * that supports anonymous requests
   *
   * @param ldapHost    the LDAP server host
   * @param ldapPort    the LDAP server port
   * @param usersOuPath the path for the organizational unit under which users are found
   */
  public LdapCredentialsAuthenticator(final String ldapHost,
                                      final int ldapPort,
                                      final String usersOuPath) {
    Assert.hasText(ldapHost, "Invalid ldapHost");
    Assert.isTrue(ldapPort > 0);
    Assert.hasText(usersOuPath, "Invalid usersOuPath");

    final LdapContextSource contextSource = new LdapContextSource();
    contextSource.setAnonymousReadOnly(true);
    contextSource.setUrl("ldap://" + ldapHost + ":" + ldapPort);
    contextSource.setBase(usersOuPath);
    contextSource.afterPropertiesSet();

    ldapTemplate = new LdapTemplate(contextSource);
    this.id = calculateId(ldapHost, ldapPort, usersOuPath);
  }

  /**
   * This constructor creates a LdapCredentialsAuthenticator that authenticates against an LDAP server
   * that does not support anonymous requests
   *
   * @param ldapHost    the LDAP server host
   * @param ldapPort    the LDAP server port
   * @param usersOuPath the path for the organizational unit under which users are found
   * @param userDn      the distinguished name for the connection
   * @param password    the password for the connection
   */
  public LdapCredentialsAuthenticator(final String ldapHost,
                                      final int ldapPort,
                                      final String usersOuPath,
                                      final String userDn,
                                      final String password) {
    Assert.hasText(ldapHost, "Invalid ldapHost");
    Assert.isTrue(ldapPort > 0);
    Assert.hasText(usersOuPath, "Invalid usersOuPath");
    Assert.hasText(userDn, "Invalid userDn");
    Assert.hasText(password, "Invalid password");

    final LdapContextSource contextSource = new LdapContextSource();
    contextSource.setUrl("ldap://" + ldapHost + ":" + ldapPort);
    contextSource.setBase(usersOuPath);
    contextSource.setUserDn(userDn);
    contextSource.setPassword(password);
    contextSource.afterPropertiesSet();

    ldapTemplate = new LdapTemplate(contextSource);
    this.id = calculateId(ldapHost, ldapPort, usersOuPath);
  }

  @Override
  public ComposableFuture<Boolean> authenticate(final Credentials<UserPasswordToken> credentials) {
    final String username = credentials.get().getUsername();
    final LdapQuery query = LdapQueryBuilder.query().filter(new EqualsFilter(UID_ATTRIBUTE, username));
    return ComposableFutures.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        try {
          ldapTemplate.authenticate(query, new String(credentials.get().getPassword()));
          return true;
        } catch (final Exception e) {
          return false;
        }
      }
    });
  }

  @Override
  public String getId() {
    return id;
  }

  private String calculateId(final String ldapHost,
                             final int ldapPort,
                             final String usersOuPath) {
    return new HashCodeBuilder()
      .append(ldapHost)
      .append(ldapPort)
      .append(usersOuPath)
      .build().toString();
  }

}
