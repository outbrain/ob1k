package com.outbrain.gruffalo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Gruffalo Server main class
 *
 * @author Eran Harel
 */
public class StandaloneGruffaloServer {
  private static final Logger logger = LoggerFactory.getLogger(StandaloneGruffaloServer.class);

  public static void main(final String[] args) {
    new StandaloneGruffaloServer().start();
    logger.info("******** Gruffalo started ********");
  }

  public void start() {
    new ClassPathXmlApplicationContext("applicationContext-GruffaloLib-all.xml");
  }

}
