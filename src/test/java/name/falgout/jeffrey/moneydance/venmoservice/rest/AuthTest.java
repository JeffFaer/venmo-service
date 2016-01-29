package name.falgout.jeffrey.moneydance.venmoservice.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

public class AuthTest {
  Auth auth;

  @Before
  public void before() {
    auth = new Auth("http://localhost:12345");
  }

  @After
  public void after() {
    auth.close();
  }

  @Test
  public void closeStopsServer() throws IOException {
    auth.captureAuthorization();

    try {
      HttpServer.create(Auth.REDIRECT_ADDRESS, -1);
      fail("Expected exception");
    } catch (IOException e) {
    }

    auth.close();

    HttpServer s = HttpServer.create(Auth.REDIRECT_ADDRESS, -1);
    s.stop(0);
  }

  @Test
  public void completingFutureStopsServer() throws IOException {
    CompletableFuture<String> token = auth.captureAuthorization();

    try {
      HttpServer.create(Auth.REDIRECT_ADDRESS, -1);
      fail("Expected exception");
    } catch (IOException e) {
    }

    token.complete("foo");

    HttpServer s = HttpServer.create(Auth.REDIRECT_ADDRESS, -1);
    s.stop(0);
  }

  @Test
  public void capturingAccessToken()
    throws MalformedURLException, IOException, InterruptedException, ExecutionException {
    CompletableFuture<String> token = auth.captureAuthorization();
    URI auth = UriBuilder.fromUri("http://localhost")
        .host(Auth.REDIRECT_ADDRESS.getHostName())
        .port(Auth.REDIRECT_ADDRESS.getPort())
        .queryParam(VenmoClient.ACCESS_TOKEN, "foo")
        .build();

    checkAuthResponse(auth.toURL().openStream());
    assertEquals("foo", token.get());
  }

  private void checkAuthResponse(InputStream in) throws IOException {
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int numRead;
    while ((numRead = in.read(buf)) > 0) {
      sink.write(buf, 0, numRead);
    }

    byte[] allRead = sink.toByteArray();
    assertArrayEquals(Auth.getAuthResponse(), allRead);
  }

  @Test
  public void capturingAccessTokenError()
    throws MalformedURLException, IOException, InterruptedException, ExecutionException {
    CompletableFuture<String> token = auth.captureAuthorization();
    URI auth = UriBuilder.fromUri("http://localhost")
        .host(Auth.REDIRECT_ADDRESS.getHostName())
        .port(Auth.REDIRECT_ADDRESS.getPort())
        .queryParam(VenmoClient.ERROR, "ruh roh")
        .build();

    checkAuthResponse(auth.toURL().openStream());
    try {
      token.get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      assertEquals("ruh roh", cause.getMessage());
    }
  }
}
