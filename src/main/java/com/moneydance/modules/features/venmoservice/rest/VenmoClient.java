package com.moneydance.modules.features.venmoservice.rest;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import com.moneydance.modules.features.venmoservice.jersey.VenmoObjectMapperProvider;

public class VenmoClient {
  public static final String ACCESS_TOKEN = "access_token";
  public static final String ERROR = "error";

  private final Client client;
  private final WebTarget api;

  public VenmoClient() {
    client = ClientBuilder.newBuilder().register(VenmoObjectMapperProvider.class).build();
    api = client.target("https://api.venmo.com/v1");
  }

  private CompletionStage<WebTarget> authorize(WebTarget target,
      CompletionStage<String> authToken) {
    return authToken.thenApply(token -> target.queryParam(ACCESS_TOKEN, token));
  }

  public Future<VenmoResponse<Me>> me(CompletionStage<String> authToken) {
    return authorize(api.path("me"), authToken)
        .thenApply(t -> t.request().get().readEntity(new GenericType<VenmoResponse<Me>>() {}))
        .toCompletableFuture();
  }
}
