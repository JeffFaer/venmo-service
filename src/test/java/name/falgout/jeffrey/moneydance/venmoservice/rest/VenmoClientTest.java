package name.falgout.jeffrey.moneydance.venmoservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.uri.UriComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@RunWith(MockitoJUnitRunner.class)
public class VenmoClientTest {
  static InetSocketAddress localAddress = new InetSocketAddress("localhost", 12345);

  HttpServer server;

  VenmoClient client;
  CompletableFuture<String> token;

  @Mock HttpHandler handler;
  @Captor ArgumentCaptor<HttpExchange> exchangeCaptor;

  @Before
  public void before() throws IOException {
    server = HttpServer.create(localAddress, -1);
    server.start();

    client = new VenmoClient("http://" + localAddress.getHostName() + ":" + localAddress.getPort());
    token = CompletableFuture.completedFuture("foo");
  }

  @After
  public void after() {
    server.stop(0);
  }

  private Answer<Void> resource(String resource) {
    return inv -> {
      HttpExchange ex = inv.getArgumentAt(0, HttpExchange.class);
      byte[] response = Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI()));
      ex.getResponseHeaders().add("Content-Type", "text/json");
      ex.sendResponseHeaders(200, response.length);
      ex.getResponseBody().write(response);
      ex.close();
      return null;
    };
  }

  @Test
  public void meTest()
    throws IOException, InterruptedException, ExecutionException, VenmoException {
    doAnswer(resource("me_response.json")).when(handler).handle(any());
    server.createContext("/me", handler);

    VenmoResponse<Me> req = client.me(token).get();
    Me me = req.getData();
    assertEquals(new BigDecimal("102.3"), me.getBalance());
    assertEquals("Cody De La Vara", me.getName());
    assertEquals(LocalDateTime.parse("2013-02-10T21:58:05", DateTimeFormatter.ISO_DATE_TIME),
        me.getDateJoined());

    verify(handler).handle(exchangeCaptor.capture());
    HttpExchange ex = exchangeCaptor.getValue();

    MultivaluedMap<String, String> query = UriComponent.decodeQuery(ex.getRequestURI(), true);
    assertEquals(1, query.size());
    assertEquals(token.get(), query.getFirst(VenmoClient.ACCESS_TOKEN));
  }

  @Test
  public void meErrorTest() throws IOException, InterruptedException, ExecutionException {
    doAnswer(resource("error_response.json")).when(handler).handle(any());
    server.createContext("/me", handler);

    VenmoResponse<Me> req = client.me(token).get();
    assertTrue(req.getException().isPresent());
    VenmoException ex = req.getException().get();
    assertEquals(261, ex.getCode());

    try {
      req.getData();
      fail("Expected exception");
    } catch (VenmoException e) {
      assertEquals(ex, e);
    }
  }
}
