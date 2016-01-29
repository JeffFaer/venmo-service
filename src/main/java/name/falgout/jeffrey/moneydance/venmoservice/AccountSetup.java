package name.falgout.jeffrey.moneydance.venmoservice;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountUtil;
import com.infinitekind.moneydance.model.AcctFilter;
import com.infinitekind.moneydance.model.OnlineTxn;
import com.infinitekind.moneydance.model.OnlineTxnList;
import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Auth;
import name.falgout.jeffrey.moneydance.venmoservice.rest.Me;
import name.falgout.jeffrey.moneydance.venmoservice.rest.PageIterator;
import name.falgout.jeffrey.moneydance.venmoservice.rest.Payment;
import name.falgout.jeffrey.moneydance.venmoservice.rest.URIBrowser;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoException;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoResponse;

public class AccountSetup extends JFrame {
  private static final long serialVersionUID = -3239889646842222229L;

  private final URIBrowser browser;
  private final Auth auth;
  private final VenmoClient client;

  private final VenmoAccountState state;

  private final DefaultComboBoxModel<Account> targetAccountModel;
  private final JComboBox<Account> targetAccount;

  private final JTextField token;
  private final JButton tokenLaunch;

  private final JButton ok;
  private final JButton cancel;

  public AccountSetup(FeatureModule feature, FeatureModuleContext context) {
    browser = new MoneydanceBrowser(context);
    auth = new Auth(browser);
    client = new VenmoClient();

    state = new VenmoAccountState(feature);
    state.load(context);

    targetAccountModel = new DefaultComboBoxModel<>();
    targetAccount = new JComboBox<>();
    targetAccount.setModel(targetAccountModel);

    JLabel accountLabel = new JLabel("Target Account:");
    accountLabel.setLabelFor(accountLabel);

    token = new JTextField();
    token.setPreferredSize(new Dimension(200, 20));
    tokenLaunch = new JButton(new ImageIcon(feature.getIconImage()));
    tokenLaunch.setToolTipText("Open a token request in your Web browser.");

    JLabel tokenLabel = new JLabel("Access Token:");
    tokenLabel.setLabelFor(token);

    ok = new JButton("Download");
    cancel = new JButton("Cancel");

    Box accountBox = new Box(BoxLayout.X_AXIS);
    accountBox.add(accountLabel);
    accountBox.add(targetAccount);

    Box tokenBox = new Box(BoxLayout.X_AXIS);
    tokenBox.add(tokenLabel);
    tokenBox.add(token);
    tokenBox.add(tokenLaunch);

    Box actions = new Box(BoxLayout.X_AXIS);
    actions.add(ok);
    actions.add(cancel);

    Box content = new Box(BoxLayout.Y_AXIS);
    content.add(accountBox);
    content.add(tokenBox);
    content.add(actions);

    add(content);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowActivated(WindowEvent e) {
        Account selected = (Account) targetAccount.getSelectedItem();

        List<Account> accounts =
            AccountUtil.allMatchesForSearch(context.getRootAccount(), AcctFilters
                .and(AcctFilter.ACTIVE_CATEGORY_CHOICE_FILTER, AcctFilter.NON_CATEGORY_FILTER));

        targetAccountModel.removeAllElements();
        for (Account a : accounts) {
          targetAccountModel.addElement(a);
        }

        if (selected == null) {
          findVenmoAccount(state, accounts).ifPresent(targetAccount::setSelectedItem);
        } else {
          targetAccount.setSelectedItem(selected);
        }

        pack();
      }
    });

    targetAccount.addItemListener(ie -> {
      if (ie.getStateChange() == ItemEvent.SELECTED) {
        Account a = (Account) ie.getItem();
        state.getToken(a).ifPresent(this::setToken);
      }
    });
    tokenLaunch.addActionListener(ae -> fetchToken());
    ok.addActionListener(ae -> {
      Optional<String> token = getToken();
      if (token.isPresent()) {
        downloadTransactions(getTargetAccount(), token.get());
      } else {
        JOptionPane.showMessageDialog(this, "Please enter an access token.", "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    });
    cancel.addActionListener(ae -> {
      dispose();
    });
  }

  private CompletionStage<String> fetchToken() {
    CompletionStage<String> token = auth.authorize();
    token.thenAcceptAsync(this::setToken, SwingUtilities::invokeLater);

    return token;
  }

  private void setToken(String token) {
    this.token.setText(token);
    this.token.setCaretPosition(0);
  }

  private Optional<Account> findVenmoAccount(VenmoAccountState state, Iterable<Account> accounts) {
    if (state.getAccounts().isEmpty()) {
      for (Account a : accounts) {
        if (a.getFullAccountName().toUpperCase().contains("VENMO")) {
          return Optional.of(a);
        }
      }
    } else {
      return Optional.of(state.getAccounts().iterator().next());
    }

    return Optional.empty();
  }

  private Account getTargetAccount() {
    return targetAccount.getItemAt(targetAccount.getSelectedIndex());
  }

  private Optional<String> getToken() {
    return token.getText() == null || token.getText().isEmpty() ? Optional.empty()
        : Optional.of(token.getText());
  }

  private void downloadTransactions(Account account, String token) {
    ZonedDateTime lastFetched =
        state.getLastFetched(account).orElseGet(() -> getCreationDate(account));

    CompletionStage<String> authToken = CompletableFuture.completedFuture(token);

    Future<VenmoResponse<Me>> whoAmI = client.getMe(authToken);
    Future<VenmoResponse<List<Payment>>> firstPage =
        client.getPaymentsAfter(authToken, lastFetched);
    // TODO SwingWorker
    try {
      Me me = whoAmI.get().getData();
      String myName = me.getName();

      OnlineTxnList txns = account.getDownloadedTxns();

      PageIterator<List<Payment>> itr = client.iterator(authToken, firstPage.get());
      while (itr.hasNext()) {
        for (Payment p : itr.next(authToken)) {
          OnlineTxn txn = txns.newTxn();

          BigDecimal amount = p.getAmount();
          if (p.getDestinationName().filter(myName::equals).isPresent()) {
            // We're the target of the payment.
            amount = amount.negate();
          }

          long mdAmount = amount.multiply(new BigDecimal(100)).toBigInteger().longValue();

          txn.setDateInitiated(p.getDateCreated().toInstant().toEpochMilli());
          txn.setDatePosted(p.getDateCompleted().toInstant().toEpochMilli());
          txn.setAmount(mdAmount);
          txn.setMemo(p.getNote());
          txn.setFITxnId(p.getId());
          txn.setName(p.getSourceName());
          p.getDestinationName().ifPresent(txn::setPayeeName);

          System.out.println(txn);
          // txns.addNewTxn(txn);
        }
      }
    } catch (InterruptedException | ExecutionException | VenmoException e) {
      e.printStackTrace();
    }

    state.setLastFetched(account, ZonedDateTime.now());
  }

  private ZonedDateTime getCreationDate(Account account) {
    long creationDate = account.getCreationDate();
    ZonedDateTime dateTime = Instant.ofEpochMilli(creationDate).atZone(ZoneId.systemDefault());

    LocalDate date = dateTime.toLocalDate();
    return date.atStartOfDay(ZoneId.systemDefault());
  }
}
