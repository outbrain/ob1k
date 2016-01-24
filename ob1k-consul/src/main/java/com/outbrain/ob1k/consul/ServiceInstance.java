package com.outbrain.ob1k.consul;

import com.google.common.base.Objects;
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
  public String ServiceID;
  public String ServiceName;
  public Set<String> ServiceTags;

  @Override
  public String toString() {
    final ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
    toStringBuilder.append("Node", Node);
    toStringBuilder.append("ServicePort", ServicePort);
    toStringBuilder.append("Address", Address);
    toStringBuilder.append("ServiceID", ServiceID);
    toStringBuilder.append("ServiceName", ServiceName);
    toStringBuilder.append("ServiceTags", ServiceTags);
    return toStringBuilder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(Node, ServicePort, Address, ServiceID, ServiceName, ServiceTags);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ServiceInstance other = (ServiceInstance) obj;
    return Objects.equal(this.Node, other.Node)
            && Objects.equal(this.ServicePort, other.ServicePort)
            && Objects.equal(this.Address, other.Address)
            && Objects.equal(this.ServiceID, other.ServiceID)
            && Objects.equal(this.ServiceName, other.ServiceName)
            && Objects.equal(this.ServiceTags, other.ServiceTags);
  }
}
