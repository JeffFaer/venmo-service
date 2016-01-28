package name.falgout.jeffrey.moneydance.venmoservice;

import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountUtil;
import com.infinitekind.moneydance.model.AcctFilter;
import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

import name.falgout.jeffrey.moneydance.venmoservice.rest.Auth;
import name.falgout.jeffrey.moneydance.venmoservice.rest.URIBrowser;

public class AccountSetup extends JPanel {
  private static final long serialVersionUID = -3239889646842222229L;
  private static final URI VENMO_TOKEN_URI;

  static {
    try {
      VENMO_TOKEN_URI = new URI("https://venmo.com/account/settings/developer");
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  private final URIBrowser browser;
  private final Auth auth;

  private final JComboBox<Account> targetAccount;

  private final JTextField token;
  private final JButton tokenHelp;
  private final JButton tokenLaunch;

  public AccountSetup(FeatureModule feature, FeatureModuleContext context) {
    browser = new MoneydanceBrowser(context);
    auth = new Auth(browser);

    targetAccount = new JComboBox<>(AccountUtil
        .allMatchesForSearch(context.getRootAccount(), AcctFilters
            .and(AcctFilter.ACTIVE_CATEGORY_CHOICE_FILTER, AcctFilter.NON_CATEGORY_FILTER))
        .toArray(new Account[0]));

    token = new JTextField();
    tokenHelp = new JButton("?");
    tokenLaunch = new JButton("V");

    Box tokenBox = new Box(BoxLayout.X_AXIS);
    tokenBox.add(token);
    tokenBox.add(tokenHelp);
    tokenBox.add(tokenLaunch);

    Box content = new Box(BoxLayout.Y_AXIS);
    content.add(targetAccount);
    content.add(tokenBox);

    add(content);

    tokenHelp.addActionListener(ae -> {
      try {
        browser.browse(VENMO_TOKEN_URI);
      } catch (Throwable e) {
        if (e instanceof Error) {
          throw (Error) e;
        } else if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        } else {
          throw new RuntimeException(e);
        }
      }
    });
    tokenLaunch.addActionListener(ae -> {
      auth.authorize().thenAcceptAsync(token::setText, SwingUtilities::invokeLater);
    });
  }
}
