package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.ServiceBindBuilder.ServiceBindBuilderSection;

/**
 * portion of the ServerBuilder methods used for service registration.
 *
 * ports, timeouts, listeners etc.
 *
 *
 * @param <B> ability to extend this builder
 */
public class ServiceRegisterBuilder<B extends ServiceRegisterBuilder<B>> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface ServiceRegisterBuilderSection extends BuilderSection<ServiceRegisterBuilder> {}

  private static final NoOpBindSection NO_OP = new NoOpBindSection();

  private final ServerBuilderState state;
  private final ServiceBindBuilder bindBuilder;

  public ServiceRegisterBuilder(final ServerBuilderState state) {
    this.state = state;
    this.bindBuilder = new ServiceBindBuilder(state);

  }

  public B register(final Service service, final String path, final ServiceFilter... filters) {
    return register(service, path, NO_OP, filters);
  }

  public B register(final Service service, final String path, final ServiceBindBuilderSection bindSection, final ServiceFilter... filters) {
    state.addServiceDescriptor(service, path, filters);
    bindSection.apply(bindBuilder);
    return self();
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  private static class NoOpBindSection implements ServiceBindBuilderSection {

    @Override
    public void apply(final ServiceBindBuilder builder) {
      // do nothing
    }
  }
}
