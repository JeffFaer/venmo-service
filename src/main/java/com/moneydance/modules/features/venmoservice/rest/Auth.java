package com.moneydance.modules.features.venmoservice.rest;

import java.awt.Desktop;
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

import org.glassfish.jersey.uri.UriComponent;

import com.sun.net.httpserver.HttpServer;

public class Auth implements Closeable {
  private static final String ACCESS_TOKEN = "access_token";
  private static final String ERROR = "error";

  private static final String CLIENT_ID = "3472";
  private static final InetSocketAddress REDIRECT_ADDRESS =
      new InetSocketAddress("localhost", 54321);

  private static final URI AUTH_URI;
  private static final Path AUTH_RESPONSE;

  static {
    try {
      AUTH_URI = new URI("https://api.venmo.com/v1/oauth/authorize?" + "client_id=" + CLIENT_ID
          + "&scope=access_profile%20access_balance%20access_payment_history");
      AUTH_RESPONSE = Paths.get(Auth.class.getResource("auth_response.html").toURI());
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  private final Lock lock = new ReentrantLock();
  private HttpServer server;
  private int numAuthorizations = 0;

  public CompletableFuture<String> authorize() {
    CompletableFuture<String> f = new CompletableFuture<>();
    Desktop d = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
    try {
      if (d == null || !d.isSupported(Desktop.Action.BROWSE)) {
        throw new UnsupportedOperationException("Cannot open " + AUTH_URI);
      } else {
        lock.lock();
        try {
          numAuthorizations++;

          if (server == null) {
            server = HttpServer.create(REDIRECT_ADDRESS, -1);
            server.createContext("/", ex -> {
              MultivaluedMap<String, String> query =
                  UriComponent.decodeQuery(ex.getRequestURI(), true);

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
                  f.complete(query.getFirst(ACCESS_TOKEN));
                } else if (query.containsKey(ERROR)) {
                  f.completeExceptionally(new Exception(query.getFirst(ERROR)));
                }
              }
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
        if (numAuthorizations == 0) {
          close();
        }
      } finally {
        lock.unlock();
      }
    });

    return f;
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
