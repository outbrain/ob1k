package com.outbrain.ob1k.server.netty;

import com.outbrain.ob1k.server.StaticPathResolver;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static io.netty.channel.ChannelHandler.Sharable;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 *
 * <h3>How Browser Caching Works</h3>
 *
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of {@code /file1.txt}.</li>
 * <li>Contents of {@code /file1.txt} is cached by the browser.</li>
 * <li>Request #2 for {@code /file1.txt} does return the contents of the
 *     file again. Rather, a 304 Not Modified is returned. This tells the
 *     browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 *     {@code If-Modified-Since} date is the same as the file's last
 *     modified date.</li>
 * </ol>
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
@Sharable
public class HttpStaticFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int HTTP_CACHE_SECONDS = 60;

  private final MimetypesFileTypeMap mimeTypesMap;
  private final StaticPathResolver pathResolver;
  private final long startupTime;

  public HttpStaticFileServerHandler(final StaticPathResolver pathResolver) {
    this.pathResolver = pathResolver;
    this.startupTime = System.currentTimeMillis();
    this.mimeTypesMap = new MimetypesFileTypeMap();
  }


  // here to make sure that the message is not released twice by the dispatcherHandler and by this handler
  @Override
  public boolean acceptInboundMessage(final Object msg) throws Exception {
    if (!(msg instanceof FullHttpRequest))
      return false;

    final FullHttpRequest request = (FullHttpRequest) msg;
    final String uri = request.uri();
    return pathResolver.isStaticPath(uri);
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
    if (!request.decoderResult().isSuccess()) {
      sendError(ctx, BAD_REQUEST);
      return;
    }

    if (request.method() != GET) {
      sendError(ctx, METHOD_NOT_ALLOWED);
      return;
    }

    // Cache Validation
    final String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
      final SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
      // Only compare up to the second because the datetime format we send to the client
      // does not have milliseconds
      final long lastDownloadTime = dateFormatter.parse(ifModifiedSince).getTime();
      if (startupTime < lastDownloadTime) {
        sendNotModified(ctx);
        return;
      }

    }

    // clean uri from potential request params.
    final String cleanUri = new QueryStringDecoder(request.uri()).path();
    final URLConnection connection = getURLConnection(cleanUri);
    if (connection == null) {
      sendError(ctx, NOT_FOUND);
      return;
    }

    final InputStream stream;
    try {
      stream = connection.getInputStream();
    } catch (final IOException e1) {
      sendError(ctx, NOT_FOUND);
      return;
    }
    final long fileLength = connection.getContentLength();

    final HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
    HttpUtil.setContentLength(response, fileLength);
    setContentTypeHeader(response, connection);
    setDateAndCacheHeaders(response);
    if (HttpUtil.isKeepAlive(request)) {
      response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    // Write the initial line and the header.
    ctx.write(response);

    // Write the content.
    final FileRegion region = new ResourceRegion(stream, fileLength);
    ctx.write(region, ctx.newProgressivePromise());

    // Write the end marker
    final ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

    // Decide whether to close the connection or not.
    if (!HttpUtil.isKeepAlive(request)) {
      // Close the connection when the whole content is written out.
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private URLConnection getURLConnection(final String uri) {
    final String resourcePath = pathResolver.getRelativePath(uri);
    final URL url = getClass().getResource(resourcePath);
    if (url != null) {
      try {
        return url.openConnection();
      } catch (final IOException e) {
        return null;
      }
    } else {
      return null;
    }
  }

  private static void sendError(final ChannelHandlerContext ctx, final HttpResponseStatus status) {
    final FullHttpResponse response = new DefaultFullHttpResponse(
        HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  /**
   * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
   *
   * @param ctx
   *            Context
   */
  private static void sendNotModified(final ChannelHandlerContext ctx) {
    final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
    setDateHeader(response);

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  /**
   * Sets the Date header for the HTTP response
   *
   * @param response
   *            HTTP response
   */
  private static void setDateHeader(final FullHttpResponse response) {
    final SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    final Calendar time = new GregorianCalendar();
    response.headers().set(DATE, dateFormatter.format(time.getTime()));
  }

  /**
   * Sets the Date and Cache headers for the HTTP Response
   *
   * @param response HTTP response
   */
  private void setDateAndCacheHeaders(final HttpResponse response) {
    final SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    // Date header
    final Calendar time = new GregorianCalendar();
    response.headers().set(DATE, dateFormatter.format(time.getTime()));

    // Add cache headers
    time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
    response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
    response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
    response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(startupTime)));
  }

  /**
   * Sets the content type header for the HTTP Response
   *
   * @param response HTTP response
   * @param connection connection to extract content type
   */
  private void setContentTypeHeader(final HttpResponse response, final URLConnection connection) {
    response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(connection.getURL().getPath()));
  }

}