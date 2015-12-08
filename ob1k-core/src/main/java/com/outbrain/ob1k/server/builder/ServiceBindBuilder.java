package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;
/**
 * portion of the ServerBuilder methods used for binding endpoints of a specific registered service.
 *
 * ports, timeouts, listeners etc.
 *
 *
 * @param <B> ability to extend this builder
 */
public class ServiceBindBuilder<B extends ServiceBindBuilder<B>> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface ServiceBindBuilderSection extends BuilderSection<ServiceBindBuilder> {}

  private final ServerBuilderState state;

  public ServiceBindBuilder(final ServerBuilderState state) {
    this.state = state;
  }

  public B bindPrefix(final boolean bindPrefix) {
    state.setBindPrefixToLastDescriptor(bindPrefix);
    return self();
  }

  public B endpoint(final String methodName, final String path, final ServiceFilter... filters) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, filters);
  }

  public B endpoint(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter... filters) {
    state.setEndpointBinding(methodType, methodName, path, filters);
    return self();
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }
}
