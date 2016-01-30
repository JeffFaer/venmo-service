package name.falgout.jeffrey.moneydance.venmoservice;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.OnlineTxn;
import com.infinitekind.moneydance.model.OnlineTxnList;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Me;
import name.falgout.jeffrey.moneydance.venmoservice.rest.PageIterator;
import name.falgout.jeffrey.moneydance.venmoservice.rest.Payment;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoResponse;

public class TransactionImporter extends SwingWorker<Void, Payment> {
  private final VenmoClient client;
  private final CompletionStage<String> token;
  private final ZonedDateTime after;

  private final Account account;

  private volatile String myName;

  public TransactionImporter(VenmoClient client, String token, ZonedDateTime after,
      Account account) {
    this.client = client;
    this.token = CompletableFuture.completedFuture(token);
    this.after = after;
    this.account = account;
  }

  @Override
  protected Void doInBackground() throws Exception {
    Future<VenmoResponse<Me>> whoAmI = client.getMe(token);
    Future<VenmoResponse<List<Payment>>> firstPage = client.getPayments(token);
    PageIterator<List<Payment>> itr = client.iterator(token, firstPage.get());

    Me me = whoAmI.get().getData();
    myName = me.getName();

    while (itr.hasNext()) {
      List<Payment> payments = itr.next(token);
      Payment[] publishable = payments.stream()
          .filter(p -> p.getStatus().equals(Payment.Status.SETTLED))
          .filter(p -> p.getDateCompleted().filter(z -> after.compareTo(z) < 0).isPresent())
          .toArray(Payment[]::new);
      publish(publishable);
    }

    return null;
  }

  @Override
  protected void process(List<Payment> chunks) {
    OnlineTxnList txns = account.getDownloadedTxns();

    for (Payment p : chunks) {
      if (p.getStatus() == Payment.Status.SETTLED) {
        OnlineTxn otxn = txns.newTxn();

        BigDecimal amount = p.getAmount();
        boolean areWeSource = p.getSourceName().equals(myName);
        if (areWeSource && p.getAction() == Payment.Action.PAY) {
          amount = amount.negate();
        }
        if (!areWeSource && p.getAction() == Payment.Action.CHARGE) {
          amount = amount.negate();
        }

        long mdAmount = amount.multiply(new BigDecimal(100)).toBigInteger().longValue();

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
  }
}
