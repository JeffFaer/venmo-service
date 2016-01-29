package name.falgout.jeffrey.moneydance.venmoservice;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.LocalStorage;
import com.infinitekind.tiksync.SyncRecord;
import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

public class VenmoAccountState {
  private static class StateEntry {
    private ZonedDateTime lastFetched;
    private String token;

    Optional<ZonedDateTime> getLastFetched() {
      return Optional.ofNullable(lastFetched);
    }

    void setLastFetched(ZonedDateTime lastFetched) {
      this.lastFetched = lastFetched;
    }

    Optional<String> getToken() {
      return Optional.ofNullable(token);
    }

    void setToken(String token) {
      this.token = token;
    }
  }

  private static final String KEY_DELIMITER = ".";
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

  private final String featureName;
  private final String accountsKey;

  private final Map<Account, StateEntry> state = new LinkedHashMap<>();

  public VenmoAccountState(FeatureModule feature) {
    featureName = feature.getIDStr();
    accountsKey = featureName + KEY_DELIMITER + "accounts";
  }

  private String getDateKey(Account account) {
    return featureName + KEY_DELIMITER + account.getUUID() + KEY_DELIMITER + "lastFetched";
  }

  public Set<Account> getAccounts() {
    return Collections.unmodifiableSet(state.keySet());
  }

  public Optional<String> getToken(Account acct) {
    return state.containsKey(acct) ? state.get(acct).getToken() : Optional.empty();
  }

  public Optional<ZonedDateTime> getLastFetched(Account acct) {
    return state.containsKey(acct) ? state.get(acct).getLastFetched() : Optional.empty();
  }

  public void setToken(Account acct, String token) {
    state.computeIfAbsent(acct, k -> new StateEntry()).setToken(token);
  }

  public void setLastFetched(Account acct, LocalDateTime time) {
    setLastFetched(acct, ZonedDateTime.of(time, ZoneId.systemDefault()));
  }

  public void setLastFetched(Account acct, ZonedDateTime time) {
    state.computeIfAbsent(acct, k -> new StateEntry()).setLastFetched(time);
  }

  public void load(FeatureModuleContext context) {
    for (Account account : getAccounts(context)) {
      Optional<ZonedDateTime> lastFetched = getLastFetched(context, account);
      StateEntry e = new StateEntry();
      lastFetched.ifPresent(e::setLastFetched);

      state.put(account, e);
    }
  }

  private List<Account> getAccounts(FeatureModuleContext context) {
    SyncRecord s = context.getCurrentAccountBook().getLocalStorage();
    if (s.containsKey(accountsKey)) {
      return s.getStringList(accountsKey)
          .stream()
          .map(context.getCurrentAccountBook()::getAccountByUUID)
          .collect(toList());
    } else {
      return Collections.emptyList();
    }
  }

  private Optional<ZonedDateTime> getLastFetched(FeatureModuleContext context, Account account) {
    String dateKey = getDateKey(account);
    SyncRecord s = context.getCurrentAccountBook().getLocalStorage();
    if (s.containsKey(dateKey)) {
      return Optional.of(ZonedDateTime.parse(s.get(dateKey), FORMATTER));
    } else {
      return Optional.empty();
    }
  }

  public void removeFrom(SyncRecord storage) {
    for (Account a : state.keySet()) {
      storage.remove(getDateKey(a));
    }

    storage.removeSubset(accountsKey);
  }

  public void save(LocalStorage storage) {
    SyncRecord r = storage;
    List<String> uuids = state.keySet().stream().map(Account::getUUID).collect(toList());
    r.put(accountsKey, uuids);

    for (Entry<Account, StateEntry> e : state.entrySet()) {
      e.getValue().getLastFetched().ifPresent(time -> {
        r.put(getDateKey(e.getKey()), FORMATTER.format(time));
      });
    }

    storage.save();
  }
}
