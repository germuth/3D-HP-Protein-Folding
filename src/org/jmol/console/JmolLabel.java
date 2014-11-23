package org.jmol.console;

import javax.swing.JLabel;

import org.jmol.api.JmolAbstractButton;

public class JmolLabel extends JLabel implements JmolAbstractButton {

  public JmolLabel(String text, int horizontalAlignment) {
    super(text, horizontalAlignment);
  }

  // unused:
  
  public String getKey() {
    return null;
  }

  public boolean isSelected() {
    return false;
  }

  public void setMnemonic(char mnemonic) {
  }

  public void addConsoleListener(Object console) {
  }

}
