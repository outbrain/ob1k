package com.outbrain.ob1k.consul;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A single value in the Consul <code>/v1/health/service/{service}</code> response
 *
 * @author Eran Harel
 */
public class HealthInfoInstance {

  public Node Node;
  public Service Service;
  public List<Check> Checks;

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof HealthInfoInstance)) {
      return false;
    }

    final HealthInfoInstance other = (HealthInfoInstance)o;

    return Objects.equals(Node, other.Node) && Objects.equals(Service, other.Service) && Objects.equals(Checks, other.Checks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Node, Service, Checks);
  }

  public static class Service {
    public String ID;
    public String Service;
    public Set<String> Tags;
    public String Address;
    public long Port;
    public long ModifyIndex;

    public Integer port(final String portType) {
      return TagsUtil.extractPort(Tags, portType);
    }

    public String context() {
      return TagsUtil.extractContextPath(Tags);
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof Service)) {
        return false;
      }

      final Service other = (Service)o;

      return Objects.equals(ID, other.ID) && Objects.equals(ModifyIndex, other.ModifyIndex) && Objects.equals(Address, other.Address) && Objects.equals(Port, other.Port);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ID, Address, Port);
    }
  }

  public static class Node {
    public String Node;
    public String Address;
    public long ModifyIndex;

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof Node)) {
        return false;
      }

      final Node other = (Node)o;

      return Objects.equals(Node, other.Node);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(Node);
    }
  }

  public static class Check {
    public String Node;
    public String CheckID;
    public String Name;
    public String Status;
    public String Notes;
    public String Output;
    public String ServiceID;
    public String ServiceName;
    public long ModifyIndex;

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof Check)) {
        return false;
      }

      final Check other = (Check)o;

      return Objects.equals(CheckID, other.CheckID) && Objects.equals(Status, other.Status);
    }

    @Override
    public int hashCode() {
      return Objects.hash(CheckID, Status);
    }
  }
}
