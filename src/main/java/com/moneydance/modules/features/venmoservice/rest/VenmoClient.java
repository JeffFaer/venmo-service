package com.moneydance.modules.features.venmoservice.rest;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.moneydance.modules.features.venmoservice.jersey.VenmoObjectMapperProvider;

public class VenmoClient {
  private final Client client;
  private final WebTarget api;

  public VenmoClient() {
    client = ClientBuilder.newBuilder().register(VenmoObjectMapperProvider.class).build();
    api = client.target("https://api.venmo.com/v1");
  }

  private Response get(WebTarget target) {
    return target.request().get();
  }

  public Future<VenmoResponse<Me>> me(CompletionStage<String> authToken) {
    return authToken.thenApply(token -> api.path("me").queryParam("access_token", token))
        .thenApply(this::get)
        .thenApply(r -> r.readEntity(new GenericType<VenmoResponse<Me>>() {}))
        .toCompletableFuture();
  }
}
