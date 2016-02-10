package name.falgout.jeffrey.moneydance.venmoservice;

import java.net.URI;

import com.moneydance.apps.md.controller.FeatureModuleContext;

import name.falgout.jeffrey.moneydance.venmoservice.rest.URIBrowser;

public class MoneydanceBrowser implements URIBrowser {
  private final FeatureModuleContext context;

  public MoneydanceBrowser(FeatureModuleContext context) {
    this.context = context;
  }

  @Override
  public void browse(URI uri) throws Exception {
    context.showURL(uri.toURL().toString());
  }
}
