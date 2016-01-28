package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public interface PageIterator<T> {
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
