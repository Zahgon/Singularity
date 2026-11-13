package com.hubspot.singularity.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Decides how long an answer is and whether it is compressed.
 *
 * The service has always held an answer back until it is finished, so that it can say how long it is, and
 * sent it in pieces once it outgrows the buffer the connector keeps. It compresses an answer only to a
 * request that asks to read a coding it writes, only for the method it compresses at all, only for a kind of
 * entity worth compressing, and only from a size on. Every answer to that method says that what it carries
 * depends on what the request accepts, whether or not this one was compressed.
 */
public class ResponseCompressionFilter extends OncePerRequestFilter {
  /** The size of the buffer the connector keeps for an answer, in bytes. */
  public static final int BUFFER_SIZE = 32768;
  private static final String GZIP = "gzip";
  private static final String VARY = "Vary";
  private static final String ACCEPT_ENCODING = "Accept-Encoding";

  /** The kinds of entity that are not worth compressing. */
  private static final List<String> EXCLUDED_PREFIXES = Arrays.asList(
    "image/",
    "audio/",
    "video/"
  );
  private static final List<String> EXCLUDED_TYPES = Arrays.asList(
    "application/compress",
    "application/zip",
    "application/gzip",
    "application/bzip2",
    "application/brotli",
    "application/x-xz",
    "application/x-rar-compressed"
  );

  private final int minimumEntitySize;
  private final List<String> includedMethods;

  public ResponseCompressionFilter(int minimumEntitySize, List<String> includedMethods) {
    this.minimumEntitySize = minimumEntitySize;
    this.includedMethods = includedMethods;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    request.setAttribute(ContainerErrorValve.SEEN, Boolean.TRUE);
    final boolean method = includedMethods.contains(request.getMethod());
    final Buffered buffered = new Buffered(request, response, method,
                                           method && acceptsGzip(request));
    try {
      chain.doFilter(request, buffered);
    } finally {
      buffered.finish();
    }
  }

  /**
   * Whether the request asks to read the coding the service writes. An entry that names it with a weight of
   * zero refuses it, and speaks only for itself; every value the request carries under the name is read.
   */
  private static boolean acceptsGzip(HttpServletRequest request) {
    boolean accepted = false;
    final Enumeration<String> values = request.getHeaders(ACCEPT_ENCODING);
    while (values != null && values.hasMoreElements()) {
      for (String entry : values.nextElement().split(",")) {
        final String[] parts = entry.trim().split(";");
        final String coding = parts[0].trim().toLowerCase(Locale.ENGLISH);
        if (!GZIP.equals(coding) && !"*".equals(coding)) {
          continue;
        }
        double weight = 1.0;
        for (int i = 1; i < parts.length; i++) {
          final String parameter = parts[i].trim();
          if (parameter.startsWith("q=")) {
            try {
              weight = Double.parseDouble(parameter.substring(2).trim());
            } catch (NumberFormatException e) {
              weight = 1.0;
            }
          }
        }
        if (weight > 0) {
          accepted = true;
        } else if (GZIP.equals(coding)) {
          return false;
        }
      }
    }
    return accepted;
  }

  private boolean compressible(HttpServletResponse response, int length) {
    if (length < minimumEntitySize) {
      return false;
    }
    if (response.getHeader("Content-Encoding") != null) {
      return false;
    }
    final int status = response.getStatus();
    if (status == 204 || status == 304) {
      return false;
    }
    final String type = response.getContentType();
    if (type == null) {
      return true;
    }
    final String media = type.split(";")[0].trim().toLowerCase(Locale.ENGLISH);
    for (String prefix : EXCLUDED_PREFIXES) {
      if (media.startsWith(prefix)) {
        return false;
      }
    }
    return !EXCLUDED_TYPES.contains(media);
  }

  /** An answer that is kept until it is finished, or until it outgrows the buffer. */
  private final class Buffered extends HttpServletResponseWrapper {
    private final HttpServletRequest request;
    private final HttpServletResponse real;
    private final boolean mayVary;
    private final boolean mayCompress;
    private ByteArrayOutputStream held = new ByteArrayOutputStream();
    private ServletOutputStream stream;
    private PrintWriter writer;
    private OutputStream streamingTo;
    private boolean streaming;
    private boolean finished;
    private boolean wrote;

    Buffered(HttpServletRequest request, HttpServletResponse real, boolean mayVary,
             boolean mayCompress) {
      super(real);
      this.request = request;
      this.real = real;
      this.mayVary = mayVary;
      this.mayCompress = mayCompress;
    }

    /**
     * Whether this answer is one whose entity could have been compressed. An answer that did not succeed
     * never is, and neither is one that carries nothing, so neither of them says that what it carries
     * depends on what the request accepts.
     */
    private boolean varies() {
      final int status = real.getStatus();
      return mayVary && status >= 200 && status < 300 && status != 204 && status != 205;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      if (stream == null) {
        stream = new Holding();
      }
      wrote = true;
      return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      if (writer == null) {
        final String encoding = getCharacterEncoding();
        final Charset charset = Charset.forName(
          encoding == null ? StandardCharsets.ISO_8859_1.name() : encoding
        );
        writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset), false);
      }
      wrote = true;
      return writer;
    }

    @Override
    public void setBufferSize(int size) {
      // the buffer is the one the connector has always kept
    }

    @Override
    public int getBufferSize() {
      return BUFFER_SIZE;
    }

    @Override
    public boolean isCommitted() {
      return streaming && real.isCommitted();
    }

    /**
     * An answer that is finished asks to be sent on its way, and the service has always answered one that
     * fits in the buffer with the length it turned out to have, so the buffer is kept until the answer is
     * finished rather than sent here.
     */
    @Override
    public void flushBuffer() throws IOException {
      if (streaming) {
        real.flushBuffer();
      }
    }

    @Override
    public void resetBuffer() {
      if (streaming) {
        real.resetBuffer();
      } else {
        held = new ByteArrayOutputStream();
        writer = null;
        stream = null;
        wrote = false;
      }
    }

    @Override
    public void reset() {
      resetBuffer();
      real.reset();
    }

    /** An answer that outgrows the buffer is sent from here on in pieces, compressed if it may be. */
    private void overflow() throws IOException {
      if (streaming) {
        return;
      }
      streaming = true;
      final byte[] data = held == null ? new byte[0] : held.toByteArray();
      held = null;
      if (varies()) {
        real.addHeader(VARY, ACCEPT_ENCODING);
      }
      dropRequestedFilenameMitigation();
      if (mayCompress && compressible(real, BUFFER_SIZE)) {
        real.setHeader("Content-Encoding", GZIP);
        gzipEtag();
        streamingTo = new GZIPOutputStream(real.getOutputStream(), 8192);
      } else {
        streamingTo = real.getOutputStream();
      }
      if (data.length > 0) {
        streamingTo.write(data);
      }
    }

    /** The answer is finished: it is compressed when it is worth it, and says how long it turned out. */
    void finish() throws IOException {
      if (finished) {
        return;
      }
      finished = true;
      if (writer != null) {
        writer.flush();
      }
      if (streaming) {
        streamingTo.flush();
        if (streamingTo instanceof GZIPOutputStream) {
          ((GZIPOutputStream) streamingTo).finish();
        }
        real.getOutputStream().flush();
        return;
      }
      if (varies()) {
        real.addHeader(VARY, ACCEPT_ENCODING);
      }
      dropRequestedFilenameMitigation();
      byte[] data = held == null ? new byte[0] : held.toByteArray();
      if (mayCompress && compressible(real, data.length)) {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed, 8192)) {
          gzip.write(data);
        }
        data = compressed.toByteArray();
        real.setHeader("Content-Encoding", GZIP);
        gzipEtag();
      }
      // Every answer whose status allows an entity says how long the one it carries is, even when that is
      // nothing at all, which is what the service has always said.
      final int status = real.getStatus();
      if (
        real.getHeader("Content-Length") == null &&
        real.getHeader("Transfer-Encoding") == null &&
        status != 204 &&
        status != 304
      ) {
        real.setContentLength(data.length);
      }
      if (data.length > 0) {
        real.getOutputStream().write(data);
      }
      real.getOutputStream().flush();
    }

    /**
     * The framework names a download of its own accord when the target ends in something it is wary of; the
     * API has never named one, and only says so where the service itself says so.
     */
    private void dropRequestedFilenameMitigation() {
      if ("inline;filename=f.txt".equals(real.getHeader("Content-Disposition"))) {
        real.setHeader("Content-Disposition", null);
      }
    }

    /** A tag that names an answer names the compressed answer differently, as the service has always. */
    private void gzipEtag() {
      final String etag = real.getHeader("ETag");
      if (etag != null && !etag.endsWith("--gzip\"") && !etag.endsWith("--gzip")) {
        real.setHeader(
          "ETag",
          etag.endsWith("\"")
            ? etag.substring(0, etag.length() - 1) + "--gzip\""
            : etag + "--gzip"
        );
      }
    }

    /** The stream the answer is written to while it is still being held. */
    private final class Holding extends ServletOutputStream {

      @Override
      public void write(int b) throws IOException {
        if (streaming) {
          streamingTo.write(b);
          return;
        }
        held.write(b);
        if (held.size() > BUFFER_SIZE) {
          overflow();
        }
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        if (streaming) {
          streamingTo.write(b, off, len);
          return;
        }
        held.write(b, off, len);
        if (held.size() > BUFFER_SIZE) {
          overflow();
        }
      }

      @Override
      public void flush() throws IOException {
        if (streaming) {
          streamingTo.flush();
        }
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setWriteListener(WriteListener listener) {
        throw new UnsupportedOperationException("the answer is written as it is built");
      }
    }
  }
}
