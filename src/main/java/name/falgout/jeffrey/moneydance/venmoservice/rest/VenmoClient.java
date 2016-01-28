package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

  public CompletableFuture<Response> get(CompletionStage<String> authToken, URI uri) {
    URI relative = api.getUri().relativize(uri);
    CompletableFuture<WebTarget> target = get(authToken, relative.getPath());
    return target.thenApply(t -> {
      MultivaluedMap<String, String> query = UriComponent.decodeQuery(uri, true);
      for (String key : query.keySet()) {
        t = t.queryParam(key, query.get(key).toArray());
      }

      return t;
    }).thenApply(t -> t.request().get());
  }

  private CompletableFuture<WebTarget> get(CompletionStage<String> authToken, String path) {
    return authToken.thenApply(token -> api.path(path).queryParam(ACCESS_TOKEN, token))
        .toCompletableFuture();
  }

  public Future<VenmoResponse<Me>> getMe(CompletionStage<String> authToken) {
    return get(authToken, "me")
        .thenApply(t -> t.request().get().readEntity(new GenericType<VenmoResponse<Me>>() {}));
  }

  public Future<VenmoResponse<List<Payment>>> getPayments(CompletionStage<String> authToken) {
    return get(authToken, "payments").thenApply(
        t -> t.request().get().readEntity(new GenericType<VenmoResponse<List<Payment>>>() {}));
  }
}
