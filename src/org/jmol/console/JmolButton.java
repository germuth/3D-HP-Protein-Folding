package org.jmol.console;

import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.jmol.api.JmolAbstractButton;

public class JmolButton extends JButton implements JmolAbstractButton {

  public JmolButton(String text) {
    super(text);
  }

  public void addConsoleListener(Object console) {
    addActionListener((ActionListener) console);
  }

  public String getKey() {
    return null;
  }
  

}
