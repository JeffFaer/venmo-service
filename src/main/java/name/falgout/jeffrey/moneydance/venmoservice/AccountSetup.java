package name.falgout.jeffrey.moneydance.venmoservice;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

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
import javax.swing.SwingWorker;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountUtil;
import com.infinitekind.moneydance.model.AcctFilter;
import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.apps.md.view.gui.MoneydanceGUI;
import com.moneydance.apps.md.view.gui.OnlineManager;
import com.moneydance.modules.features.venmoservice.Main;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Auth;
import name.falgout.jeffrey.moneydance.venmoservice.rest.URIBrowser;
import name.falgout.jeffrey.moneydance.venmoservice.rest.VenmoClient;

public class AccountSetup extends JFrame {
  private static final long serialVersionUID = -3239889646842222229L;

  private final VenmoAccountState state;
  private final FeatureModuleContext context;

  private final URIBrowser browser;
  private final Auth auth;
  private final VenmoClient client;

  private final DefaultComboBoxModel<Account> targetAccountModel;
  private final JComboBox<Account> targetAccount;

  private final JTextField token;
  private final JButton tokenLaunch;

  private final JButton ok;
  private final JButton cancel;

  public AccountSetup(VenmoAccountState state, FeatureModule feature,
      FeatureModuleContext context) {
    this.state = state;
    this.context = context;

    browser = new MoneydanceBrowser(context);
    auth = new Auth(browser);
    client = new VenmoClient();

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
        updateAccounts();
      }
    });
    targetAccount.addItemListener(ie -> {
      if (ie.getStateChange() == ItemEvent.SELECTED) {
        Account a = (Account) ie.getItem();
        if (token.getText().isEmpty()) {
          state.getToken(a).ifPresent(this::setToken);
        }
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

  @Override
  public void dispose() {
    super.dispose();
    auth.close();
  }

  private void updateAccounts() {
    Account selected = (Account) targetAccount.getSelectedItem();

    List<Account> accounts = AccountUtil.allMatchesForSearch(context.getRootAccount(),
        AcctFilters.and(AcctFilter.ACTIVE_CATEGORY_CHOICE_FILTER, AcctFilter.NON_CATEGORY_FILTER));

    targetAccountModel.removeAllElements();
    for (Account a : accounts) {
      targetAccountModel.addElement(a);
    }

    if (selected == null || !accounts.contains(selected)) {
      findVenmoAccount(state, accounts).ifPresent(targetAccount::setSelectedItem);
    } else {
      targetAccount.setSelectedItem(selected);
    }

    pack();
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

  private CompletionStage<String> fetchToken() {
    CompletionStage<String> token = auth.authorize();
    token.thenAcceptAsync(this::setToken, SwingUtilities::invokeLater);

    return token;
  }

  private void setToken(String token) {
    this.token.setText(token);
    this.token.setCaretPosition(0);
  }

  private Account getTargetAccount() {
    return targetAccount.getItemAt(targetAccount.getSelectedIndex());
  }

  private Optional<String> getToken() {
    return token.getText().isEmpty() ? Optional.empty() : Optional.of(token.getText());
  }

  private void downloadTransactions(Account account, String token) {
    MoneydanceGUI gui = Main.getUI(context);

    SwingWorker<?, ?> worker =
        new TransactionImporter(client, token, getCreationDate(account), account);
    worker.addPropertyChangeListener(pce -> {
      if (pce.getPropertyName().equals("state")) {
        if (pce.getNewValue().equals(SwingWorker.StateValue.STARTED)) {
          gui.setStatus("Downloading Venmo transactions to account " + account, -1);
        } else if (pce.getNewValue().equals(SwingWorker.StateValue.DONE)) {
          gui.setStatus("", 0);

          try {
            worker.get(); // Check for an exception.

            new OnlineManager(gui).processDownloadedTxns(account);

            state.setToken(account, token);
            state.setLastFetched(account, ZonedDateTime.now());
            state.save(context.getCurrentAccountBook().getLocalStorage());
          } catch (Exception e) {
            Throwable report = e instanceof ExecutionException ? e.getCause() : e;
            gui.showErrorMessage(report);
            e.printStackTrace();
          }
        }
      }
    });
    worker.execute();

    dispose();
  }

  private ZonedDateTime getCreationDate(Account account) {
    long creationDate = account.getCreationDate();
    ZonedDateTime dateTime = Instant.ofEpochMilli(creationDate).atZone(ZoneId.systemDefault());

    LocalDate date = dateTime.toLocalDate();
    return date.atStartOfDay(ZoneId.systemDefault());
  }
}
