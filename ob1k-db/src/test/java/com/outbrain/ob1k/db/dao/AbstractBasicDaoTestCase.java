package com.outbrain.ob1k.db.dao;

import org.junit.After;
import org.junit.Before;

import com.outbrain.ob1k.db.BasicDao;
import com.outbrain.ob1k.db.BasicTestingDao;

/**
 * Base class for async dao testing
 * @author tpraizler
 *         Date: 8/25/14
 *         Time: 3:36 PM
 */
public abstract class AbstractBasicDaoTestCase {

  private BasicTestingDao dao;
  private final String hostname;
  private final int port;
  private final String scheme;
  private final String user;
  private final String password;
  private final long connectTimeoutMilliSeconds;
  private final long queryTimeoutMilliSeconds;

  protected AbstractBasicDaoTestCase(final String hostname, final int port, final String scheme, final String user, final String password,
                                     final long connectTimeoutMilliSeconds, final long queryTimeoutMilliSeconds) {
    this.hostname = hostname;
    this.port = port;
    this.scheme = scheme;
    this.user = user;
    this.password = password;
    this.connectTimeoutMilliSeconds = connectTimeoutMilliSeconds;
    this.queryTimeoutMilliSeconds = queryTimeoutMilliSeconds;
  }

  @After
  public final void closeDao() throws Exception {
    dao.close();
  }

  @Before
  public final void initDao() throws Exception {
    this.dao = new BasicTestingDao(hostname, port, scheme, user, password, connectTimeoutMilliSeconds, queryTimeoutMilliSeconds);
  }

  public BasicDao getDao() {
    return this.dao;
  }
}
