package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.uri.UriComponent;

import name.falgout.jeffrey.moneydance.venmoservice.jersey.VenmoObjectMapperProvider;

public class VenmoClient {
  public interface ResponseIterator<T> {
    public boolean hasNext();

    public VenmoResponse<T> getNextResponse(CompletionStage<String> authToken)
      throws ExecutionException, InterruptedException;

    default T next(CompletionStage<String> authToken)
      throws VenmoException, ExecutionException, InterruptedException {
      return getNextResponse(authToken).getData();
    }

    public boolean hasPrevious();

    public VenmoResponse<T> getPreviousResponse(CompletionStage<String> authToken)
      throws ExecutionException, InterruptedException;

    default T previous(CompletionStage<String> authToken)
      throws VenmoException, ExecutionException, InterruptedException {
      return getPreviousResponse(authToken).getData();
    }
  }

  private class ResponseIteratorImpl<T> implements ResponseIterator<T> {
    private Future<VenmoResponse<T>> previous;
    private Future<VenmoResponse<T>> next;

    ResponseIteratorImpl(CompletionStage<String> authToken, VenmoResponse<T> next) {
      setPrevious(authToken, next);
      this.next = CompletableFuture.completedFuture(next);
    }

    private void setPrevious(CompletionStage<String> authToken, VenmoResponse<T> current) {
      if (previous != null) {
        previous.cancel(true);
      }
      previous = current.hasPrevious() ? getPrevious(authToken, current) : null;
    }

    private void setCurrent(CompletionStage<String> authToken, VenmoResponse<T> current) {
      setPrevious(authToken, current);

      if (next != null) {
        next.cancel(true);
      }
      next = current.hasNext() ? getNext(authToken, current) : null;
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public VenmoResponse<T> getNextResponse(CompletionStage<String> authToken)
      throws ExecutionException, InterruptedException {
      if (next == null) {
        throw new NoSuchElementException();
      }

      VenmoResponse<T> actualNext = next.get();
      setCurrent(authToken, actualNext);
      return actualNext;
    }

    @Override
    public boolean hasPrevious() {
      return previous != null;
    }

    @Override
    public VenmoResponse<T> getPreviousResponse(CompletionStage<String> authToken)
      throws ExecutionException, InterruptedException {
      if (previous == null) {
        throw new NoSuchElementException();
      }

      VenmoResponse<T> actualNext = previous.get();
      setCurrent(authToken, actualNext);
      return actualNext;
    }
  }

  static final String ACCESS_TOKEN = "access_token";
  static final String ERROR = "error";

  private final Client client;
  private final WebTarget api;

  public VenmoClient() {
    this("https://api.venmo.com/v1");
  }

  VenmoClient(String baseUri) {
    client = ClientBuilder.newBuilder().register(VenmoObjectMapperProvider.class).build();
    api = client.target(baseUri);
  }

  public Future<VenmoResponse<Me>> getMe(CompletionStage<String> authToken) {
    return get(authToken, "me")
        .thenApply(t -> t.request().get().readEntity(new GenericType<VenmoResponse<Me>>() {}));
  }

  public Future<VenmoResponse<List<Payment>>> getPayments(CompletionStage<String> authToken) {
    return get(authToken, "payments").thenApply(
        t -> t.request().get().readEntity(new GenericType<VenmoResponse<List<Payment>>>() {}));
  }

  CompletableFuture<WebTarget> get(CompletionStage<String> authToken, String path) {
    return authToken.thenApply(token -> api.path(path).queryParam(ACCESS_TOKEN, token))
        .toCompletableFuture();
  }

  public <T> ResponseIterator<T> iterator(CompletionStage<String> authToken,
      VenmoResponse<T> next) {
    return new ResponseIteratorImpl<>(authToken, next);
  }

  public <T> Future<VenmoResponse<T>> getNext(CompletionStage<String> authToken,
      VenmoResponse<T> response) {
    if (response.getException().isPresent()) {
      CompletableFuture<VenmoResponse<T>> fail = new CompletableFuture<>();
      fail.completeExceptionally(response.getException().get());
      return fail;
    } else if (!response.hasNext()) {
      CompletableFuture<VenmoResponse<T>> fail = new CompletableFuture<>();
      fail.completeExceptionally(new NoSuchElementException("next"));
      return fail;
    }

    try {
      GenericType<VenmoResponse<T>> type = response.getGenericType();
      return get(authToken, response.getPagination().flatMap(Pagination::getNext).get())
          .thenApply(r -> r.readEntity(type));
    } catch (VenmoException e) {
      throw new Error("We already checked for the VenmoException", e);
    }
  }

  public <T> Future<VenmoResponse<T>> getPrevious(CompletionStage<String> authToken,
      VenmoResponse<T> response) {
    if (response.getException().isPresent()) {
      CompletableFuture<VenmoResponse<T>> fail = new CompletableFuture<>();
      fail.completeExceptionally(response.getException().get());
      return fail;
    } else if (!response.hasPrevious()) {
      CompletableFuture<VenmoResponse<T>> fail = new CompletableFuture<>();
      fail.completeExceptionally(new NoSuchElementException("previous"));
      return fail;
    }

    try {
      GenericType<VenmoResponse<T>> type = response.getGenericType();
      return get(authToken, response.getPagination().flatMap(Pagination::getPrevious).get())
          .thenApply(r -> r.readEntity(type));
    } catch (VenmoException e) {
      throw new Error("We already checked for the VenmoException", e);
    }
  }

  private CompletableFuture<Response> get(CompletionStage<String> authToken, URI uri) {
    URI relative = api.getUri().relativize(uri);
    if (relative == uri) {
      // We couldn't relativize the URIs. We won't be able to access it with our (WebTarget api).
      throw new IllegalArgumentException("Could not relativize " + uri);
    }

    return get(authToken, relative.getPath()).thenApply(t -> {
      MultivaluedMap<String, String> query = UriComponent.decodeQuery(uri, true);
      for (String key : query.keySet()) {
        t = t.queryParam(key, query.get(key).toArray());
      }

      return t;
    }).thenApply(t -> t.request().get());
  }
}
