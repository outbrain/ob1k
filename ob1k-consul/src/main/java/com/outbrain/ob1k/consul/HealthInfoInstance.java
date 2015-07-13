package com.outbrain.ob1k.consul;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Set;

/**
 * A single value in the Consul <code>/v1/health/service/{service}</code> response
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


    public static class Service {
        public String ID;
        public String Service;
        public Set<String> Tags;
        public String Address;
        public long Port;

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
    }

    public static class Node {
        public String Node;
        public String Address;

        @Override
        public String toString() {
            return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
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

        @Override
        public String toString() {
            return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
        }
    }
}
