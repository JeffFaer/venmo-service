package name.falgout.jeffrey.moneydance.venmoservice;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AcctFilter;

public final class AcctFilters {
  public static AcctFilter and(AcctFilter first, AcctFilter... others) {
    return new AcctFilter() {

      @Override
      public boolean matches(Account acct) {
        for (AcctFilter o : others) {
          if (!o.matches(acct)) {
            return false;
          }
        }

        return first.matches(acct);
      }

      @Override
      public String format(Account acct) {
        return first.format(acct);
      }
    };
  }
}
