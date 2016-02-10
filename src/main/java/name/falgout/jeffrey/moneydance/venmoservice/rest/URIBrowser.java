package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.awt.Desktop;
import java.net.URI;

public interface URIBrowser {
  public void browse(URI uri) throws Exception;

  public static URIBrowser DESKTOP_BROWSER = uri -> {
    Desktop d = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
    if (d == null || !d.isSupported(Desktop.Action.BROWSE)) {
      throw new UnsupportedOperationException("Cannot open " + uri);
    } else {
      d.browse(uri);
    }
  };
}
