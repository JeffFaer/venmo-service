package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import name.falgout.jeffrey.moneydance.venmoservice.jackson.VenmoModule;

public class VenmoClient {
  private class PageIteratorImpl<T> implements PageIterator<T> {
    private Future<VenmoResponse<T>> previous;
    private Future<VenmoResponse<T>> next;

    PageIteratorImpl(CompletionStage<String> authToken, VenmoResponse<T> next) {
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

      VenmoResponse<T> actualPrevious = previous.get();
      setCurrent(authToken, actualPrevious);
      return actualPrevious;
    }
  }

  static final URI VENMO_API = URI.create("https://api.venmo.com/v1/");
  static final String ACCESS_TOKEN = "access_token";
  static final String ERROR = "error";

  private final URI apiTarget;
  private final ObjectMapper mapper;

  public VenmoClient() {
    this(VENMO_API);
  }

  VenmoClient(URI apiTarget) {
    this.apiTarget = apiTarget;
    mapper = new ObjectMapper();
    mapper.registerModule(new VenmoModule());
  }

  CompletableFuture<URIBuilder> target(CompletionStage<String> authToken, String path) {
    return authToken.thenApply(token -> {
      URIBuilder b = new URIBuilder(apiTarget);
      String oldPath = b.getPath();
      String newPath;
      if (oldPath.endsWith("/") || path.startsWith("/")) {
        newPath = oldPath + path;
      } else {
        newPath = oldPath + "/" + path;
      }
      return b.addParameter(ACCESS_TOKEN, token).setPath(newPath);
    }).toCompletableFuture();
  }

  <T> CompletableFuture<VenmoResponse<T>> getData(URIBuilder target, Class<T> dataType) {
    return getData(target, mapper.constructType(dataType));
  }

  <T> CompletableFuture<VenmoResponse<T>> getData(URIBuilder target, TypeReference<T> dataType) {
    return getData(target, mapper.constructType(dataType.getType()));
  }

  private <T> CompletableFuture<VenmoResponse<T>> getData(URIBuilder target, JavaType dataType) {
    JavaType responseType =
        mapper.getTypeFactory().constructParametricType(VenmoResponse.class, dataType);

    CompletableFuture<VenmoResponse<T>> response = new CompletableFuture<>();
    try {
      URI uri = target.build();
      response.complete(Request.Get(uri).execute().handleResponse(resp -> {
        VenmoResponse<T> venmoResponse =
            mapper.readerFor(responseType).readValue(resp.getEntity().getContent());
        venmoResponse.setDataType(dataType);
        venmoResponse.setURI(uri);
        return venmoResponse;
      }));
    } catch (IOException | URISyntaxException e) {
      response.completeExceptionally(e);
    }
    return response;
  }

  public Future<VenmoResponse<Me>> getMe(CompletionStage<String> authToken) {
    return target(authToken, "me").thenCompose(t -> getData(t, Me.class));
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
        .thenCompose(this::finishPayments);
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsBefore(CompletionStage<String> authToken,
      ZonedDateTime before) {
    return payments(authToken).thenApply(t -> paymentsBefore(t, before))
        .thenCompose(this::finishPayments);
  }

  public Future<VenmoResponse<List<Payment>>> getPaymentsBetween(CompletionStage<String> authToken,
      ZonedDateTime after, ZonedDateTime before) {
    return payments(authToken).thenApply(t -> paymentsAfter(t, after))
        .thenApply(t -> paymentsBefore(t, before))
        .thenCompose(this::finishPayments);
  }

  public Future<VenmoResponse<List<Payment>>> getPayments(CompletionStage<String> authToken) {
    return payments(authToken).thenCompose(this::finishPayments);
  }

  private CompletableFuture<URIBuilder> payments(CompletionStage<String> authToken) {
    return target(authToken, "payments");
  }

  private LocalDateTime toVenmoUTC(ZonedDateTime dateTime) {
    return dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().withNano(0);
  }

  private URIBuilder paymentsBefore(URIBuilder target, ZonedDateTime before) {
    return target.addParameter("before",
        toVenmoUTC(before).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  private URIBuilder paymentsAfter(URIBuilder target, ZonedDateTime after) {
    return target.addParameter("after",
        toVenmoUTC(after).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

  }

  private CompletableFuture<VenmoResponse<List<Payment>>> finishPayments(URIBuilder target) {
    return getData(target, new TypeReference<List<Payment>>() {});
  }

  public <T> PageIterator<T> iterator(CompletionStage<String> authToken, VenmoResponse<T> next) {
    return new PageIteratorImpl<>(authToken, next);
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
        .thenCompose(t -> getData(t, response.getDataType()));
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
            .thenCompose(t -> getData(t, response.getDataType()));
  }

  private CompletableFuture<URIBuilder> targetRelative(CompletionStage<String> authToken, URI uri) {
    URI relative = apiTarget.relativize(uri);
    if (relative.isAbsolute()) {
      // We couldn't relativize the URIs. We won't be able to access it with our (WebTarget api).
      throw new IllegalArgumentException("Could not relativize " + uri);
    }

    return target(authToken, relative.getPath())
        .thenApply(t -> t.addParameters(new URIBuilder(uri).getQueryParams()));
  }
}
