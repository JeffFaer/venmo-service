package name.falgout.jeffrey.moneydance.venmoservice;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
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

  private final URIBrowser browser;
  private final Auth auth;

  private final JComboBox<Account> targetAccount;

  private final JTextField token;
  private final JButton tokenLaunch;

  public AccountSetup(FeatureModule feature, FeatureModuleContext context) {
    browser = new MoneydanceBrowser(context);
    auth = new Auth(browser);

    targetAccount = new JComboBox<>(AccountUtil
        .allMatchesForSearch(context.getRootAccount(), AcctFilters
            .and(AcctFilter.ACTIVE_CATEGORY_CHOICE_FILTER, AcctFilter.NON_CATEGORY_FILTER))
        .toArray(new Account[0]));

    token = new JTextField();
    tokenLaunch = new JButton(new ImageIcon(feature.getIconImage()));

    Box tokenBox = new Box(BoxLayout.X_AXIS);
    tokenBox.add(token);
    tokenBox.add(tokenLaunch);

    Box content = new Box(BoxLayout.Y_AXIS);
    content.add(targetAccount);
    content.add(tokenBox);

    add(content);
    tokenLaunch.addActionListener(ae -> {
      auth.authorize().thenAcceptAsync(token -> {
        this.token.setText(token);
        this.token.setCaretPosition(0);
      } , SwingUtilities::invokeLater);
    });
  }
}
