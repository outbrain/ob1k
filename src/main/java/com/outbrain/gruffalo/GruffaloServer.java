package com.outbrain.gruffalo;

import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.jetty.JettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gruffalo Server main class
 *
 * @author Eran Harel
 */
public class GruffaloServer {
  private static final Logger logger = LoggerFactory.getLogger(GruffaloServer.class);

  public static void main(final String[] args) {
    new GruffaloServer().build(true).start();
    logger.info("******** Gruffalo started ********");
  }

  public Server build(final boolean useConfigurationPort) {
    final JettyServerBuilder serverBuilder = new JettyServerBuilder().setContextPath("/Gruffalo");

    if (useConfigurationPort) {
      serverBuilder.useConfigurationPorts();
    }

    return serverBuilder.build();
  }

}
