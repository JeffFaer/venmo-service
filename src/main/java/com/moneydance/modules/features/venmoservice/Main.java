package com.moneydance.modules.features.venmoservice;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFrame;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.awt.AwtUtil;

import name.falgout.jeffrey.moneydance.venmoservice.AccountSetup;

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

  @Override
  public void invoke(String uri) {
    if (uri.equals("setup")) {
      if (setup == null) {
        setup = new AccountSetup(this, getContext());
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
  }
}
