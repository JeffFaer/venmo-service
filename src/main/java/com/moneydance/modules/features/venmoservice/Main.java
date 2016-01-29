package com.moneydance.modules.features.venmoservice;

import javax.swing.JFrame;
import javax.ws.rs.ext.RuntimeDelegate;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.awt.AwtUtil;

import name.falgout.jeffrey.moneydance.venmoservice.AccountSetup;
import name.falgout.jeffrey.moneydance.venmoservice.jersey.MoneydanceRuntimeDelegate;

public class Main extends FeatureModule {
  private AccountSetup setup;
  private JFrame frame;

  public Main() {}

  @Override
  public String getName() {
    return "Venmo Service";
  }

  @Override
  public void init() {
    System.err.println("Init");
    RuntimeDelegate.setInstance(new MoneydanceRuntimeDelegate());

    getContext().registerFeature(this, "setup", null, "Setup Venmo Account");
  }

  @Override
  public void invoke(String uri) {
    System.err.println("Invoke");
    if (setup == null) {
      setup = new AccountSetup(this, getContext());
    }
    if (frame == null) {
      frame = new JFrame(getName());
      frame.add(setup);
      frame.pack();
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setVisible(true);
    } else {
      frame.setVisible(true);
      frame.toFront();
      frame.requestFocus();
    }

    AwtUtil.centerWindow(frame);
  }

  @Override
  public void cleanup() {
    System.err.println("Cleanup");
    if (frame != null) {
      frame.dispose();
    }

    frame = null;
    setup = null;
  }
}
