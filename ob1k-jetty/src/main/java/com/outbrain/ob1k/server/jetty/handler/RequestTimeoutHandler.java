package com.outbrain.ob1k.server.jetty.handler;

import com.google.common.base.Preconditions;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Jetty Handler that cancels (interrupts) long running requests.
 *
 * @author Eran Harel
 */
public class RequestTimeoutHandler extends HandlerWrapper {
  private static final Logger log = LoggerFactory.getLogger(RequestTimeoutHandler.class);

  private final ScheduledExecutorService requestTimeoutExecutor = Executors.newScheduledThreadPool(1);
  private final long timeoutMillis;
  private final Counter timeouts;

  public RequestTimeoutHandler(final long timeoutMillis, final MetricFactory metricFactory) {
    Preconditions.checkArgument(0 <= timeoutMillis, "timeoutMillis must be positive");
    this.timeoutMillis = timeoutMillis;
    Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");

    timeouts = metricFactory.createCounter(getClass().getSimpleName(), "requestTimeouts");
  }

  /**
   * the timer thread needs to interrupt the jetty thread only if it is still running the original request
   * when timeout happens. the timer thread and jetty's thread can be in a race condition if the request finishes
   * around the time the timer starts to run. is such a case the timer can check to see if the request is still running
   * and decide to interrupt it, meanwhile the jetty thread finishes the original request and starts to execute
   * another one.
   * to avoid that condition we use a simple three state, state machine. the first thread(usually jetty) move the machine
   * to the END state. if the timer thread starts to run and the request hasn't finished yet it moves the machine to the
   * BEFORE_INTERRUPT state to signal the jetty thread that it is about to interrupt so that the jetty thread should avoid
   * continue to the next request. after interrupting the state moves to END.
   * the jetty thread once encountered the BEFORE_INTERRUPT spin waits until the timer will interrupt him and set the state
   * to END.
   */
  private static enum TimerState {
    BEGIN, BEFORE_INTERRUPT, END
  }

  @Override
  public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    final Thread requestThread = Thread.currentThread();

    final AtomicReference<TimerState> timerState = new AtomicReference<>(TimerState.BEGIN);
    requestTimeoutExecutor.schedule(new Runnable() {
      @Override
      public void run() {
        if (!timerState.compareAndSet(TimerState.BEGIN, TimerState.BEFORE_INTERRUPT)) {
          log.debug("request completed before. serving other request now...");
          return;
        }

        final HttpChannelState.State state = baseRequest.getHttpChannelState().getState();
        response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
        requestThread.interrupt();
        timerState.set(TimerState.END);

        timeouts.inc();
        log.warn("Request timed out!!! state={} url=[{}] queryString=[{}]; Request thread was interrupted...",
            state, request.getRequestURI(), request.getQueryString());
      }
    }, timeoutMillis, TimeUnit.MILLISECONDS);

    try {
      super.handle(target, baseRequest, request, response);
    } finally {
      if (!timerState.compareAndSet(TimerState.BEGIN, TimerState.END)) {
        long counter = 0;
        // spin wait until the timer thread will interrupt us and set the state to END.
        // if we're here the request took the same time as the request timeout so there is a race condition
        // between the executing thread and the timer thread.
        while(!(timerState.get() == TimerState.END)) {
          counter++;
        }
        // reset the interrupt flag on the thread before moving to the next request.
        Thread.interrupted();

        log.info("took {} rounds to finish race between jetty and the timer", counter);
      }
    }
  }
}
