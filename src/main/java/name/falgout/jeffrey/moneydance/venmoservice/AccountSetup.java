package name.falgout.jeffrey.moneydance.venmoservice;

import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

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
import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Auth;
import name.falgout.jeffrey.moneydance.venmoservice.rest.URIBrowser;

public class AccountSetup extends JFrame {
  private static final long serialVersionUID = -3239889646842222229L;

  private final URIBrowser browser;
  private final Auth auth;

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

    state = new VenmoAccountState(feature);
    state.load(context);

    targetAccountModel = new DefaultComboBoxModel<>();
    targetAccount = new JComboBox<>();
    targetAccount.setModel(targetAccountModel);

    JLabel accountLabel = new JLabel("Target Account:");
    accountLabel.setLabelFor(accountLabel);

    token = new JTextField();
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
    // TODO Auto-generated method stub
  }
}
