package name.falgout.jeffrey.moneydance.venmoservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

  private Answer<Void> resource(String resource) throws URISyntaxException {
    Path path = Paths.get(getClass().getResource(resource).toURI());

    return inv -> {
      HttpExchange ex = inv.getArgumentAt(0, HttpExchange.class);
      byte[] response = Files.readAllBytes(path);
      ex.getResponseHeaders().add("Content-Type", "text/json");
      ex.sendResponseHeaders(200, response.length);
      ex.getResponseBody().write(response);
      ex.close();
      return null;
    };
  }

  @Test
  public void meTest() throws IOException, InterruptedException, ExecutionException, VenmoException,
    URISyntaxException {
    doAnswer(resource("me_response.json")).when(handler).handle(any());
    server.createContext("/me", handler);

    VenmoResponse<Me> req = client.getMe(token).get();
    Me me = req.getData();
    assertEquals(new BigDecimal("102.3"), me.getBalance());
    assertEquals("Cody De La Vara", me.getName());
    assertEquals(LocalDateTime.parse("2013-02-10T21:58:05", DateTimeFormatter.ISO_DATE_TIME),
        me.getDateJoined().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());

    verify(handler).handle(exchangeCaptor.capture());
    HttpExchange ex = exchangeCaptor.getValue();

    checkToken(ex.getRequestURI());
  }

  private void checkToken(URI uri) throws InterruptedException, ExecutionException {
    checkToken(UriComponent.decodeQuery(uri, true));
  }

  private void checkToken(MultivaluedMap<String, String> query)
    throws InterruptedException, ExecutionException {
    assertTrue(query.containsKey(VenmoClient.ACCESS_TOKEN));
    assertEquals(token.get(), query.getFirst(VenmoClient.ACCESS_TOKEN));
  }

  @Test
  public void meErrorTest()
    throws IOException, InterruptedException, ExecutionException, URISyntaxException {
    doAnswer(resource("error_response.json")).when(handler).handle(any());
    server.createContext("/me", handler);

    VenmoResponse<Me> req = client.getMe(token).get();
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

  @Test
  public void paymentsTest() throws IOException, InterruptedException, ExecutionException,
    VenmoException, URISyntaxException {
    doAnswer(resource("payments_response.json")).when(handler).handle(any());
    server.createContext("/payments", handler);

    VenmoResponse<List<Payment>> req = client.getPayments(token).get();
    List<Payment> payments = req.getData();
    assertEquals(2, payments.size());
  }

  private void setupPagination(HttpHandler prevHandler, HttpHandler nextHandler)
    throws IOException, URISyntaxException {
    doAnswer(resource("pagination_response.json")).when(handler).handle(any());
    doAnswer(resource("prev_response.json")).when(prevHandler).handle(any());
    doAnswer(resource("next_response.json")).when(nextHandler).handle(any());
    server.createContext("/", handler);
    server.createContext("/previous", prevHandler);
    server.createContext("/next", nextHandler);
  }

  private Future<VenmoResponse<Integer>> getPaginationInitial() {
    return client.target(token, "").thenApply(t -> client.getData(t, Integer.class));
  }

  @Test
  public void paginationTest() throws IOException, InterruptedException, ExecutionException,
    URISyntaxException, VenmoException {
    HttpHandler prevHandler = mock(HttpHandler.class);
    HttpHandler nextHandler = mock(HttpHandler.class);
    setupPagination(prevHandler, nextHandler);

    Future<VenmoResponse<Integer>> future = getPaginationInitial();
    VenmoResponse<Integer> current = future.get();
    assertEquals(2, (int) current.getData());
    assertTrue(current.hasNext());
    assertTrue(current.hasPrevious());

    VenmoResponse<Integer> next = client.getNext(token, current).get();
    assertEquals(3, (int) next.getData());
    assertFalse(next.hasNext());
    assertTrue(next.hasPrevious());

    VenmoResponse<Integer> current2 = client.getPrevious(token, next).get();
    assertEquals(current, current2);

    VenmoResponse<Integer> previous = client.getPrevious(token, current).get();
    assertEquals(1, (int) previous.getData());
    assertFalse(previous.hasPrevious());
    assertTrue(previous.hasNext());

    // Verify that queries were correctly passed through.
    verify(prevHandler).handle(exchangeCaptor.capture());
    HttpExchange prevExchange = exchangeCaptor.getValue();
    MultivaluedMap<String, String> query =
        UriComponent.decodeQuery(prevExchange.getRequestURI(), true);
    assertTrue(query.containsKey("foo"));
    assertEquals("123", query.getFirst("foo"));
    checkToken(query);

    verify(nextHandler).handle(exchangeCaptor.capture());
    HttpExchange nextExchange = exchangeCaptor.getValue();
    query = UriComponent.decodeQuery(nextExchange.getRequestURI(), true);
    assertTrue(query.containsKey("bar"));
    assertEquals("456", query.getFirst("bar"));
    checkToken(query);

    verify(handler, times(2)).handle(exchangeCaptor.capture());
    HttpExchange initial = exchangeCaptor.getAllValues().get(2);
    HttpExchange fromNext = exchangeCaptor.getAllValues().get(3);

    checkToken(initial.getRequestURI());
    query = UriComponent.decodeQuery(fromNext.getRequestURI(), true);
    assertTrue(query.containsKey("from"));
    assertEquals("next", query.getFirst("from"));
  }

  @Test
  public void responseIteratorTest() throws IOException, URISyntaxException, VenmoException,
    ExecutionException, InterruptedException {
    HttpHandler prevHandler = mock(HttpHandler.class);
    HttpHandler nextHandler = mock(HttpHandler.class);
    setupPagination(prevHandler, nextHandler);

    PageIterator<Integer> itr = client.iterator(token, getPaginationInitial().get());
    assertTrue(itr.hasPrevious());
    assertTrue(itr.hasNext());

    int current = itr.next(token);
    assertEquals(2, current);
    assertTrue(itr.hasPrevious());
    assertTrue(itr.hasNext());

    int next = itr.next(token);
    assertEquals(3, next);
    assertTrue(itr.hasPrevious());
    assertFalse(itr.hasNext());

    int current2 = itr.previous(token);
    assertEquals(current, current2);

    int previous = itr.previous(token);
    assertEquals(1, previous);
    assertFalse(itr.hasPrevious());
    assertTrue(itr.hasNext());
  }
}
