package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.server.builder.BuilderSection;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

public class SpringServiceRegisterBuilder extends ExtendableSpringServiceRegisterBuilder<SpringServiceRegisterBuilder> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface SpringServiceRegisterBuilderSection extends BuilderSection<SpringServiceRegisterBuilder> {}

  public SpringServiceRegisterBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    super(state, ctx);
  }
}
