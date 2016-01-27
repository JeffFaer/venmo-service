package com.moneydance.modules.features.venmoservice.rest;

import java.awt.Desktop;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.glassfish.jersey.uri.UriComponent;

import com.sun.net.httpserver.HttpServer;

public class Auth implements Closeable {
  private static final String CLIENT_ID = "3472";

  private static final URI AUTH_URI;
  private static final String AUTH_RESPONSE;

  static {
    try {
      AUTH_URI = new URI("https://api.venmo.com/v1/oauth/authorize?" + "client_id=" + CLIENT_ID
          + "&scope=access_profile%20access_balance%20access_payment_history");
    } catch (URISyntaxException e) {
      throw new Error(e);
    }

    AUTH_RESPONSE = new StringBuilder()
        .append("<html><body>Success! Return to Moneydance.</body></html>").toString();
  }

  private final Lock lock = new ReentrantLock();
  private HttpServer server;
  private int numAuthorizations = 0;

  public CompletableFuture<String> authorize() {
    CompletableFuture<String> f = new CompletableFuture<>();
    Desktop d = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
    try {
      if (d == null || !d.isSupported(Desktop.Action.BROWSE)) {
        throw new Exception("Cannot open " + AUTH_URI);
      } else {
        lock.lock();
        try {
          numAuthorizations++;

          if (server == null) {
            server = HttpServer.create(new InetSocketAddress("localhost", 54321), -1);
            server.createContext("/", ex -> {
              Optional<String> token = getAuthToken(ex.getRequestURI());

              if (token.isPresent()) {
                ex.sendResponseHeaders(200, AUTH_RESPONSE.length());
                ex.getResponseBody().write(AUTH_RESPONSE.getBytes());
              } else {
                ex.sendResponseHeaders(404, 0);
              }

              ex.close();

              token.ifPresent(f::complete);
            });
            server.start();
          }
        } finally {
          lock.unlock();
        }

        d.browse(AUTH_URI);
      }
    } catch (Exception e) {
      f.completeExceptionally(e);
    }

    f.whenComplete((r, t) -> {
      lock.lock();
      try {
        numAuthorizations--;
        if (numAuthorizations == 0 && server != null) {
          server.stop(0);
          server = null;
        }
      } finally {
        lock.unlock();
      }
    });

    return f;
  }

  private Optional<String> getAuthToken(URI request) {
    return Optional.ofNullable(UriComponent.decodeQuery(request, true).getFirst("access_token"));
  }

  @Override
  public void close() {
    lock.lock();
    try {
      server.stop(0);
      server = null;
    } finally {
      lock.unlock();
    }
  }
}
