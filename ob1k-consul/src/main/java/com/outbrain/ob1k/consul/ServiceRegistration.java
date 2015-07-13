package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;

import java.util.Set;

/**
 * Service registration data - gets sent to Consul
 *
 * @author Eran Harel
 */
public class ServiceRegistration {
  private final String name;
  private final Integer port;
  private final Set<String> tags;
  private final Check check;
  private final String id;

  // I need to make some marshalers happy ;)
  ServiceRegistration() {
    this(null, 0, null, null, null);
  }

  public ServiceRegistration(final String name, final Integer port, final Set<String> tags, final Check check, final Integer instance) {
    this.name = Preconditions.checkNotNull(name, "name must not be null");
    this.tags = Preconditions.checkNotNull(tags, "tags must not be null");
    this.check = Preconditions.checkNotNull(check, "check must not be null");
    this.port = port;
    this.id = (instance == null ? name : name + instance);
  }

  public String getID() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Integer getPort() {
    return port;
  }

  public Set<String> getTags() {
    return tags;
  }

  public Check getCheck() {
    return check;
  }

  public static class Check {
    private final String script;
    private final String interval;

    // I need to make some marshalers happy ;)
    Check() {
      this(null, 0);
    }

    public Check(final String url, final int intervalSec) {
      this.script = "curl -v --fail --max-time 1 " + Preconditions.checkNotNull(url, "url must not be null");
      Preconditions.checkArgument(0 < intervalSec, "intervalSec must be greater than zero");
      this.interval = intervalSec + "s";
    }

    public String getScript() {
      return script;
    }

    public String getInterval() {
      return interval;
    }
  }
}
