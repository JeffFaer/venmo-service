package name.falgout.jeffrey.moneydance.venmoservice;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.LocalStorage;
import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

public class VenmoAccountState {
  private static class StateEntry implements Serializable {
    private static final long serialVersionUID = 2767501973800986951L;
    private ZonedDateTime lastFetched;
    private transient String token;

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

  private final String featureName;
  private final Map<Account, StateEntry> state = new LinkedHashMap<>();

  public VenmoAccountState(FeatureModule feature) {
    featureName = feature.getIDStr();
  }

  private String getKey(Account account, String keyName) {
    return String.join(KEY_DELIMITER, featureName, account.getUUID(), keyName);
  }

  private String getTokenKey(Account account) {
    return getKey(account, "token");
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

  public void load(FeatureModuleContext context) throws Exception {
    LocalStorage storage = context.getCurrentAccountBook().getLocalStorage();
    if (storage.exists(featureName)) {
      ObjectInputStream in = new ObjectInputStream(storage.openFileForReading(featureName));
      @SuppressWarnings("unchecked") Map<String, StateEntry> accounts =
          (Map<String, StateEntry>) in.readObject();

      for (Entry<String, StateEntry> e : accounts.entrySet()) {
        Account acct = context.getCurrentAccountBook().getAccountByUUID(e.getKey());
        e.getValue().setToken(storage.getCachedAuthentication(getTokenKey(acct)));
        state.put(acct, e.getValue());
      }
    }
  }

  public void removeFrom(LocalStorage storage) throws Exception {
    storage.clearAuthenticationCache(featureName);
    storage.delete(featureName);
  }

  public void save(LocalStorage storage) throws Exception {
    Map<String, StateEntry> uuids = new LinkedHashMap<>(state.size());
    for (Entry<Account, StateEntry> e : state.entrySet()) {
      uuids.put(e.getKey().getUUID(), e.getValue());

      e.getValue().getToken().ifPresent(t -> {
        storage.cacheAuthentication(getTokenKey(e.getKey()), t);
      });
    }

    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(sink);
    oos.writeObject(uuids);
    oos.close();

    storage.writeToFileAtomically(sink.toByteArray(), featureName);
  }
}
