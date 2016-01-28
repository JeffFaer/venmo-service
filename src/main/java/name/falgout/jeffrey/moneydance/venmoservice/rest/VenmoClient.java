package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

import org.glassfish.jersey.uri.UriComponent;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

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

  CompletableFuture<WebTarget> target(CompletionStage<String> authToken, String path) {
    return authToken.thenApply(token -> api.path(path).queryParam(ACCESS_TOKEN, token))
        .toCompletableFuture();
  }

  <T> VenmoResponse<T> getData(WebTarget target, Class<T> dataType) {
    return getData(target, TypeToken.of(dataType));
  }

  <T> VenmoResponse<T> getData(WebTarget target, TypeToken<T> dataType) {
    TypeToken<VenmoResponse<T>> type = new TypeToken<VenmoResponse<T>>() {
      private static final long serialVersionUID = -4655053155717345610L;
    }.where(new TypeParameter<T>() {}, dataType);

    GenericType<VenmoResponse<T>> responseType = new GenericType<>(type.getType());
    VenmoResponse<T> response = target.request().get(responseType);
    response.setDataType(dataType);
    return response;
  }

  public Future<VenmoResponse<Me>> getMe(CompletionStage<String> authToken) {
    return target(authToken, "me").thenApply(t -> getData(t, Me.class));
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsAfter(CompletionStage<String> authToken,
      LocalDateTime after) {
    return getPaymentsAfter(authToken, ZonedDateTime.of(after, ZoneId.systemDefault()));
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsBefore(CompletionStage<String> authToken,
      LocalDateTime before) {
    return getPaymentsBefore(authToken, ZonedDateTime.of(before, ZoneId.systemDefault()));
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsBetween(CompletionStage<String> authToken,
      LocalDateTime after, LocalDateTime before) {
    return getPaymentsBetween(authToken, ZonedDateTime.of(after, ZoneId.systemDefault()),
        ZonedDateTime.of(before, ZoneId.systemDefault()));
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsAfter(CompletionStage<String> authToken,
      ZonedDateTime after) {
    return payments(authToken).thenApply(t -> paymentsAfter(t, after))
        .thenApply(this::finishPayments);
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsBefore(CompletionStage<String> authToken,
      ZonedDateTime before) {
    return payments(authToken).thenApply(t -> paymentsBefore(t, before))
        .thenApply(this::finishPayments);
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsBetween(CompletionStage<String> authToken,
      ZonedDateTime after, ZonedDateTime before) {
    return payments(authToken).thenApply(t -> paymentsAfter(t, after))
        .thenApply(t -> paymentsBefore(t, before))
        .thenApply(this::finishPayments);
  }

  public Future<VenmoResponse<List<Payment>>> getPayments(CompletionStage<String> authToken) {
    return payments(authToken).thenApply(this::finishPayments);
  }

  private CompletableFuture<WebTarget> payments(CompletionStage<String> authToken) {
    return target(authToken, "payments");
  }

  private LocalDateTime toVenmoUTC(ZonedDateTime dateTime) {
    return dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().withNano(0);
  }

  private WebTarget paymentsBefore(WebTarget target, ZonedDateTime before) {
    return target.queryParam("before",
        toVenmoUTC(before).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  private WebTarget paymentsAfter(WebTarget target, ZonedDateTime after) {
    return target.queryParam("after",
        toVenmoUTC(after).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

  }

  private VenmoResponse<List<Payment>> finishPayments(WebTarget target) {
    return getData(target, new TypeToken<List<Payment>>() {
      private static final long serialVersionUID = -265257930840525648L;
    });
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

    return targetRelative(authToken, response.getPagination().flatMap(Pagination::getNext).get())
        .thenApply(t -> getData(t, response.getDataType()));
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

    return targetRelative(authToken,
        response.getPagination().flatMap(Pagination::getPrevious).get())
            .thenApply(t -> getData(t, response.getDataType()));
  }

  private CompletableFuture<WebTarget> targetRelative(CompletionStage<String> authToken, URI uri) {
    URI relative = api.getUri().relativize(uri);
    if (relative == uri) {
      // We couldn't relativize the URIs. We won't be able to access it with our (WebTarget api).
      throw new IllegalArgumentException("Could not relativize " + uri);
    }

    return target(authToken, relative.getPath()).thenApply(t -> {
      MultivaluedMap<String, String> query = UriComponent.decodeQuery(uri, true);
      for (String key : query.keySet()) {
        t = t.queryParam(key, query.get(key).toArray());
      }

      return t;
    });
  }
}
