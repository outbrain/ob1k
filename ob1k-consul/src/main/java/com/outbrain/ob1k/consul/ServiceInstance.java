package com.outbrain.ob1k.consul;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Set;

/**
 * A single value in the Consul <code>/v1/catalog/service/{service}</code> response
 *
 * @author Eran Harel
 */
public class ServiceInstance {
  public String Node;
  public long ServicePort;
  public String Address;
  public String ServiceAddress;
  public String ServiceID;
  public String ServiceName;
  public Set<String> ServiceTags;

  @Override
  public String toString() {
    final ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
    toStringBuilder.append("Node", Node);
    toStringBuilder.append("ServicePort", ServicePort);
    toStringBuilder.append("Address", Address);
    toStringBuilder.append("ServiceAddress", ServiceAddress);
    toStringBuilder.append("ServiceID", ServiceID);
    toStringBuilder.append("ServiceName", ServiceName);
    toStringBuilder.append("ServiceTags", ServiceTags);
    return toStringBuilder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ServiceInstance)) return false;

    ServiceInstance that = (ServiceInstance) o;

    return new EqualsBuilder()
             .append(ServicePort, that.ServicePort)
             .append(Node, that.Node)
             .append(Address, that.Address)
             .append(ServiceAddress, that.ServiceAddress)
             .append(ServiceID, that.ServiceID)
             .append(ServiceName, that.ServiceName)
             .append(ServiceTags, that.ServiceTags)
             .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
             .append(Node)
             .append(ServicePort)
             .append(Address)
             .append(ServiceAddress)
             .append(ServiceID)
             .append(ServiceName)
             .append(ServiceTags)
             .toHashCode();
  }
}
