package name.falgout.jeffrey.moneydance.venmoservice.rest;

import static name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient.ACCESS_TOKEN;
import static name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient.ERROR;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.uri.UriComponent;

import com.sun.net.httpserver.HttpServer;

public class Auth implements Closeable {
  private static final String CLIENT_ID = "3472";
  static final InetSocketAddress REDIRECT_ADDRESS = new InetSocketAddress("localhost", 54321);
  static final Path AUTH_RESPONSE;

  static {
    try {
      AUTH_RESPONSE = Paths.get(Auth.class.getResource("auth_response.html").toURI());
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
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
    this("https://api.venmo.com/v1/oauth/authorize", browser);
  }

  Auth(String baseUri) {
    this(baseUri, URIBrowser.DESKTOP_BROWSER);
  }

  Auth(String baseUri, URIBrowser browser) {
    authUri =
        UriBuilder.fromUri(baseUri)
            .queryParam("client_id", CLIENT_ID)
            .queryParam("scope",
                String.join(" ", "access_profile", "access_balance", "access_payment_history"))
            .build();
    this.browser = browser;
  }

  CompletableFuture<String> captureAuthorization() {
    CompletableFuture<String> token = new CompletableFuture<>();
    lock.lock();
    try {
      numAuthorizations++;
      if (server == null) {
        server = HttpServer.create(REDIRECT_ADDRESS, -1);
        server.createContext("/", ex -> {
          MultivaluedMap<String, String> query = UriComponent.decodeQuery(ex.getRequestURI(), true);

          try {
            if (query.containsKey(ACCESS_TOKEN) || query.containsKey(ERROR)) {
              byte[] response = Files.readAllBytes(AUTH_RESPONSE);
              ex.sendResponseHeaders(200, response.length);
              ex.getResponseBody().write(response);
            } else {
              ex.sendResponseHeaders(404, 0);
            }

            ex.close();
          } finally {
            if (query.containsKey(ACCESS_TOKEN)) {
              token.complete(query.getFirst(ACCESS_TOKEN));
            } else if (query.containsKey(ERROR)) {
              token.completeExceptionally(new Exception(query.getFirst(ERROR)));
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
