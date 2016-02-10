package com.moneydance.modules.features.venmoservice;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFrame;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.apps.md.view.gui.MoneydanceGUI;
import com.moneydance.awt.AwtUtil;

import name.falgout.jeffrey.moneydance.venmoservice.AccountSetup;
import name.falgout.jeffrey.moneydance.venmoservice.VenmoAccountState;

public class Main extends FeatureModule {
  public static byte[] getResource(InputStream in) throws IOException {
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int numRead;
    while ((numRead = in.read(buf)) > 0) {
      sink.write(buf, 0, numRead);
    }
    return sink.toByteArray();
  }

  private VenmoAccountState state;
  private AccountSetup setup;

  public Main() {}

  @Override
  public String getName() {
    return "Venmo Service";
  }

  @Override
  public Image getIconImage() {
    try {
      return Toolkit.getDefaultToolkit()
          .createImage(getResource(getClass().getResourceAsStream("venmo-30.png")));
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void init() {
    getContext().registerFeature(this, "setup", null, "Setup Venmo Account");
  }

  public static MoneydanceGUI getUI(FeatureModuleContext context) {
    com.moneydance.apps.md.controller.Main main = (com.moneydance.apps.md.controller.Main) context;
    return (MoneydanceGUI) main.getUI();
  }

  @Override
  public void invoke(String uri) {
    if (uri.equals("setup")) {
      if (state == null) {
        state = new VenmoAccountState(this);
        try {
          state.load(getContext());
        } catch (Exception e) {
          MoneydanceGUI gui = getUI(getContext());
          gui.showErrorMessage(e);
          e.printStackTrace();
        }
      }

      if (setup == null) {
        setup = new AccountSetup(state, this, getContext());
        setup.pack();
        setup.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      }
      setup.setVisible(true);
      setup.toFront();
      setup.requestFocus();
      AwtUtil.centerWindow(setup);
    }
  }

  @Override
  public void cleanup() {
    if (setup != null) {
      setup.dispose();
    }

    setup = null;
  }

  @Override
  public void unload() {
    cleanup();

    try {
      state.removeFrom(getContext().getCurrentAccountBook().getLocalStorage());
      state = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
