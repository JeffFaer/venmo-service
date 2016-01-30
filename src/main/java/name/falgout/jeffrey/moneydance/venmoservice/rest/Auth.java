package name.falgout.jeffrey.moneydance.venmoservice.rest;

import static name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient.ACCESS_TOKEN;
import static name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient.ERROR;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import com.sun.net.httpserver.HttpServer;

public class Auth implements Closeable {
  private static final URI VENMO_AUTH = VenmoClient.VENMO_API.resolve("oauth/authorize");
  private static final String CLIENT_ID = "3472";
  static final InetSocketAddress REDIRECT_ADDRESS = new InetSocketAddress("localhost", 54321);

  static byte[] getAuthSuccess() throws IOException {
    InputStream in = Auth.class.getResourceAsStream("auth_success.html");
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int numRead;
    while ((numRead = in.read(buf)) > 0) {
      sink.write(buf, 0, numRead);
    }

    return sink.toByteArray();
  }

  static byte[] getAuthError() throws IOException {
    InputStream in = Auth.class.getResourceAsStream("auth_failure.html");
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int numRead;
    while ((numRead = in.read(buf)) > 0) {
      sink.write(buf, 0, numRead);
    }

    return sink.toByteArray();
  }

  private final URI authUri;
  private final URIBrowser browser;

  private final Lock lock = new ReentrantLock();
  private HttpServer server;
  private int numAuthorizations = 0;

  public Auth() {
    this(URIBrowser.DESKTOP_BROWSER);
  }

  public Auth(URIBrowser browser) {
    this(VENMO_AUTH, browser);
  }

  Auth(URI baseUri) {
    this(baseUri, URIBrowser.DESKTOP_BROWSER);
  }

  Auth(URI baseUri, URIBrowser browser) {
    try {
      authUri = new URIBuilder(baseUri).setParameter("client_id", CLIENT_ID)
          .setParameter("scope",
              String.join(" ", "access_profile", "access_balance", "access_payment_history"))
          .build();
      this.browser = browser;
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  CompletableFuture<String> captureAuthorization() {
    CompletableFuture<String> token = new CompletableFuture<>();
    lock.lock();
    try {
      numAuthorizations++;
      if (server == null) {
        server = HttpServer.create(REDIRECT_ADDRESS, -1);
        server.createContext("/", ex -> {
          List<NameValuePair> query = new URIBuilder(ex.getRequestURI()).getQueryParams();
          String error = null;
          String accessToken = null;
          for (NameValuePair pair : query) {
            if (pair.getName().equals(ACCESS_TOKEN)) {
              accessToken = pair.getValue();
              break;
            } else if (pair.getName().equals(ERROR)) {
              error = pair.getValue();
              break;
            }
          }

          try {
            if (accessToken != null || error != null) {
              byte[] response = accessToken != null ? getAuthSuccess() : getAuthError();
              ex.sendResponseHeaders(200, response.length);
              ex.getResponseBody().write(response);
            } else {
              ex.sendResponseHeaders(404, 0);
            }

            ex.close();
          } finally {
            if (accessToken != null) {
              token.complete(accessToken);
            } else if (error != null) {
              token.completeExceptionally(new Exception(error));
            }
          }
        });
        server.start();
      }
    } catch (Exception e) {
      token.completeExceptionally(e);
    } finally {
      lock.unlock();
    }

    token.whenComplete((r, t) -> {
      lock.lock();
      try {
        numAuthorizations--;
        if (numAuthorizations == 0) {
          close();
        }
      } finally {
        lock.unlock();
      }
    });

    return token;
  }

  public CompletableFuture<String> authorize() {
    CompletableFuture<String> auth = captureAuthorization();
    try {
      browser.browse(authUri);
    } catch (Throwable t) {
      auth.completeExceptionally(t);
    }

    return auth;
  }

  @Override
  public void close() {
    lock.lock();
    try {
      if (server != null) {
        server.stop(0);
        server = null;
      }
    } finally {
      lock.unlock();
    }
  }
}
