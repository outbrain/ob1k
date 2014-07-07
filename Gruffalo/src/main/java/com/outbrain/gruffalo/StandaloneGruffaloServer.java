package com.outbrain.gruffalo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gruffalo Server main class
 *
 * @author Eran Harel
 */
public class StandaloneGruffaloServer {
  private static final Logger logger = LoggerFactory.getLogger(StandaloneGruffaloServer.class);

  // TODO restore to start a TCP / UDP server only
  public static void main(final String[] args) {
//    new GruffaloServer().build(true).start();
//    logger.info("******** Gruffalo started ********");
  }

//  public Server build(final boolean useConfigurationPort) {
//    final JettyServerBuilder serverBuilder = new JettyServerBuilder().setContextPath("/Gruffalo");
//
//    if (useConfigurationPort) {
//      serverBuilder.useConfigurationPorts();
//    }
//
//    return serverBuilder.build();
//  }

}
