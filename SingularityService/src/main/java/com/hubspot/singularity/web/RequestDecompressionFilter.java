package com.hubspot.singularity.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads an entity the request says is compressed.
 *
 * The coding is read from the first value the request carries under the name, and only the two codings the
 * service has always read are read: an entity named as the one with a header of its own is read through every
 * member it holds, and an entity named as the bare one is read as a stream with no header at all. What the
 * entity says about its own coding and its own length is then kept from the service, because neither is true
 * of what it was read as. An entity the service cannot read as the coding it is named as is an error of the
 * service's own, which is what the API has always answered for one.
 */
public class RequestDecompressionFilter extends OncePerRequestFilter {
  private static final String GZIP = "gzip";
  private static final String DEFLATE = "deflate";
  private static final int BUFFER_SIZE = 8192;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    final String encoding = request.getHeader("Content-Encoding");
    if (GZIP.equalsIgnoreCase(encoding)) {
      chain.doFilter(decompressed(request, true), response);
    } else if (DEFLATE.equalsIgnoreCase(encoding)) {
      chain.doFilter(decompressed(request, false), response);
    } else {
      chain.doFilter(request, response);
    }
  }

  private HttpServletRequest decompressed(HttpServletRequest request, boolean gzip)
    throws IOException {
    final HttpServletRequest hidden = new WithoutContentHeaders(request);
    final InputStream input = gzip
      ? new GZIPInputStream(hidden.getInputStream(), BUFFER_SIZE)
      : new InflaterInputStream(hidden.getInputStream(), new Inflater(true), BUFFER_SIZE);
    return new Decompressed(hidden, input, gzip ? GZIP : DEFLATE);
  }

  /** The request without what it says about the coding and the length of its entity. */
  private static final class WithoutContentHeaders extends HttpServletRequestWrapper {
    private static final Set<String> HIDDEN = new LinkedHashSet<>(
      java.util.Arrays.asList("content-encoding", "content-length")
    );

    WithoutContentHeaders(HttpServletRequest request) {
      super(request);
    }

    private static boolean hidden(String name) {
      return name != null && HIDDEN.contains(name.toLowerCase(java.util.Locale.ENGLISH));
    }

    @Override
    public String getHeader(String name) {
      return hidden(name) ? null : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return hidden(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      final Set<String> names = new LinkedHashSet<>();
      final Enumeration<String> all = super.getHeaderNames();
      while (all != null && all.hasMoreElements()) {
        final String name = all.nextElement();
        if (!hidden(name)) {
          names.add(name);
        }
      }
      return Collections.enumeration(names);
    }

    @Override
    public int getIntHeader(String name) {
      return hidden(name) ? -1 : super.getIntHeader(name);
    }

    @Override
    public int getContentLength() {
      return -1;
    }

    @Override
    public long getContentLengthLong() {
      return -1;
    }
  }

  /** The request with its entity read as the coding it was named as. */
  private static final class Decompressed extends HttpServletRequestWrapper {
    private final ServletInputStream input;
    private final BufferedReader reader;

    Decompressed(HttpServletRequest request, InputStream stream, String coding)
      throws IOException {
      super(request);
      this.input = new Reading(stream, coding);
      final String encoding = request.getCharacterEncoding();
      this.reader =
        new BufferedReader(
          new InputStreamReader(input, encoding == null ? "ISO-8859-1" : encoding)
        );
    }

    @Override
    public ServletInputStream getInputStream() {
      return input;
    }

    @Override
    public BufferedReader getReader() {
      return reader;
    }
  }

  /**
   * An entity the service cannot read as the coding it is named as: this is an error of the service's own,
   * which is what the API has always answered for one, rather than a refusal of the entity.
   */
  public static class UnreadableEntity extends RuntimeException {
    private static final long serialVersionUID = 1L;

    UnreadableEntity(String coding, Throwable cause) {
      super("could not read the entity as " + coding, cause);
    }
  }

  /** A stream over what the entity was read as. */
  private static final class Reading extends ServletInputStream {
    private final InputStream stream;
    private final String coding;
    private boolean ended;

    Reading(InputStream stream, String coding) {
      this.stream = stream;
      this.coding = coding;
    }

    @Override
    public int read() throws IOException {
      try {
        final int b = stream.read();
        ended = b < 0;
        return b;
      } catch (IOException e) {
        throw new UnreadableEntity(coding, e);
      }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      try {
        final int n = stream.read(b, off, len);
        ended = n < 0;
        return n;
      } catch (IOException e) {
        throw new UnreadableEntity(coding, e);
      }
    }

    @Override
    public int available() throws IOException {
      return stream.available();
    }

    @Override
    public void close() throws IOException {
      stream.close();
    }

    @Override
    public boolean isFinished() {
      return ended;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
      throw new UnsupportedOperationException("the entity is read as it arrives");
    }
  }
}
