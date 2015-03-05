package com.outbrain.ob1k.server.services;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import rx.Observable;
import rx.subjects.ReplaySubject;

/**
 * Streams the log4j messages starting from the first message that arrived after the call to this API, AKA log tail ;).
 *
 * @author Eran Harel
 */
public class Log4jTailService implements ILogService {
  public Observable<String> handle() {
    return TailAppender.instance.publishSubject;
  }

  private static class TailAppender extends AppenderSkeleton {
    private static final TailAppender instance = new TailAppender();
    private final ReplaySubject<String> publishSubject = ReplaySubject.createWithSize(20);

    private TailAppender() {
      layout = new PatternLayout("%d %-5p [%t] [%C{1}:%L] - %m%n");
      final Logger rootLogger = Logger.getRootLogger();
      rootLogger.addAppender(this);
    }

    @Override
    protected void append(final LoggingEvent event) {
      if (closed) {
        return;
      }

      publishSubject.onNext(layout.format(event));
    }

    @Override
    public void close() {
      this.closed = true;
      publishSubject.onCompleted();
    }

    @Override
    public boolean requiresLayout() {
      return false;
    }

  }
}