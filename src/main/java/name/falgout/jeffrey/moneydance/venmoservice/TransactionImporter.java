package name.falgout.jeffrey.moneydance.venmoservice;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingWorker;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.OnlineTxn;
import com.infinitekind.moneydance.model.OnlineTxnList;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Me;
import name.falgout.jeffrey.moneydance.venmoservice.rest.PageIterator;
import name.falgout.jeffrey.moneydance.venmoservice.rest.Payment;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoResponse;

public class TransactionImporter extends SwingWorker<ZonedDateTime, Payment> {
  private final VenmoClient client;
  private final CompletionStage<String> token;
  private final ZonedDateTime after;

  private final Account account;

  private final AtomicReference<Me> me = new AtomicReference<>();
  private final CompletableFuture<Me> meFuture = new CompletableFuture<>();

  public TransactionImporter(VenmoClient client, String token, ZonedDateTime after,
      Account account) {
    this.client = client;
    this.token = CompletableFuture.completedFuture(token);
    this.after = after;
    this.account = account;
  }

  public CompletionStage<Me> getMe() {
    return meFuture;
  }

  @Override
  protected ZonedDateTime doInBackground() throws Exception {
    Future<VenmoResponse<Me>> whoAmI = client.getMe(token);
    // XXX Venmo's payments?after=* doesn't work.
    Future<VenmoResponse<List<Payment>>> firstPage = client.getPayments(token);
    PageIterator<List<Payment>> itr = client.iterator(token, firstPage.get());

    me.set(whoAmI.get().getData());
    meFuture.complete(me.get());

    ZonedDateTime lastFetched = after;
    while (itr.hasNext()) {
      List<Payment> payments = itr.next(token);
      Payment[] publishable = payments.stream()
          .filter(p -> p.getStatus().equals(Payment.Status.SETTLED))
          .filter(p -> p.getDateCompleted().filter(after::isBefore).isPresent())
          .toArray(Payment[]::new);

      Optional<ZonedDateTime> latestPayment = Arrays.stream(publishable)
          .map(Payment::getDateCompleted)
          .map(Optional::get)
          .max(Comparator.naturalOrder())
          .filter(lastFetched::isBefore);
      if (latestPayment.isPresent()) {
        lastFetched = latestPayment.get();
      }

      publish(publishable);
    }

    // TODO return the date of the newest transaction OR the oldest pending transaction.
    // If Venmo's payments?after=* filters by dateCompleted then just return the date of the newest
    // transaction.
    // If Venmo's payments?after=* filters by dateCreated then return oldest pending transaction.
    return lastFetched;
  }

  @Override
  protected void process(List<Payment> chunks) {
    OnlineTxnList txns = account.getDownloadedTxns();

    for (Payment p : chunks) {
      OnlineTxn otxn = txns.newTxn();

      BigDecimal amount = p.getAmount();
      boolean areWeSource = p.getSourceName().equals(me.get().getName());
      if (areWeSource && p.getAction() == Payment.Action.PAY) {
        amount = amount.negate();
      } else if (!areWeSource && p.getAction() == Payment.Action.CHARGE) {
        amount = amount.negate();
      }

      long mdAmount = getMoneydanceAmount(amount);

      otxn.setDateInitiated(p.getDateCreated().toInstant().toEpochMilli());
      p.getDateCompleted().map(z -> z.toInstant().toEpochMilli()).ifPresent(otxn::setDatePosted);
      otxn.setAmount(mdAmount);
      otxn.setTotalAmount(mdAmount);
      otxn.setMemo(p.getNote());
      otxn.setFITxnId(p.getId());

      if (areWeSource) {
        p.getDestinationName().ifPresent(otxn::setPayeeName);
      } else {
        otxn.setPayeeName(p.getSourceName());
      }

      txns.addNewTxn(otxn);
    }
  }

  private long getMoneydanceAmount(BigDecimal amount) {
    return amount.multiply(new BigDecimal(100)).longValue();
  }

  @Override
  protected void done() {
    try {
      ZonedDateTime fetched = get();

      long instant = fetched.toInstant().toEpochMilli();
      long amount = getMoneydanceAmount(me.get().getBalance());

      OnlineTxnList txns = account.getDownloadedTxns();
      txns.setOFXLastTxnUpdate(instant);
      txns.setOnlineAvailBalance(amount, instant);
    } catch (Exception e) {
      meFuture.completeExceptionally(e);
    }
  }
}
