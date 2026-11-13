package com.hubspot.singularity.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * An entity of nothing but blanks is no entity at all.
 *
 * The service has always handed a resource method nothing when the request carried an entity of whitespace
 * only, rather than refusing to read it, so a method that will not work without one fails the way it fails
 * when none was sent. The entity is read far enough to tell blanks from anything else and no further, so an
 * entity of any size still arrives as it was sent.
 */
public class BlankEntityFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    chain.doFilter(new BlanksAreNothing(request), response);
  }

  /** The request whose entity is nothing when it holds nothing but blanks. */
  private static final class BlanksAreNothing extends HttpServletRequestWrapper {
    private ServletInputStream input;
    private BufferedReader reader;

    BlanksAreNothing(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (input == null) {
        input = new Reading(withoutLeadingBlanks(super.getInputStream()));
      }
      return input;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (reader == null) {
        final String encoding = getCharacterEncoding();
        reader =
          new BufferedReader(
            new InputStreamReader(getInputStream(), encoding == null ? "ISO-8859-1" : encoding)
          );
      }
      return reader;
    }

    /** The entity as it was sent, or nothing at all when it holds nothing but blanks. */
    private static InputStream withoutLeadingBlanks(InputStream entity) throws IOException {
      final java.io.ByteArrayOutputStream blanks = new java.io.ByteArrayOutputStream();
      int b = entity.read();
      while (b == ' ' || b == '\t' || b == '\r' || b == '\n' || b == '\f' || b == 0x0b) {
        blanks.write(b);
        b = entity.read();
      }
      if (b < 0) {
        return new ByteArrayInputStream(new byte[0]);
      }
      final byte[] seen = blanks.toByteArray();
      final byte[] first = new byte[seen.length + 1];
      System.arraycopy(seen, 0, first, 0, seen.length);
      first[seen.length] = (byte) b;
      return new SequenceInputStream(new ByteArrayInputStream(first), entity);
    }
  }

  /** A stream over the entity as it is read. */
  private static final class Reading extends ServletInputStream {
    private final InputStream stream;
    private boolean ended;

    Reading(InputStream stream) {
      this.stream = stream;
    }

    @Override
    public int read() throws IOException {
      final int b = stream.read();
      ended = b < 0;
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      final int n = stream.read(b, off, len);
      ended = n < 0;
      return n;
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
