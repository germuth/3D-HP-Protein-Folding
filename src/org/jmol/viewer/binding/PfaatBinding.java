package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class PfaatBinding extends JmolBinding {

  public PfaatBinding() {
    super("extendedSelect");
  }

  @Override
  protected void setSelectBindings() {
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_select);    
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_selectNone);    
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_selectToggle);
    bindAction(SINGLE_CLICK+ALT+SHIFT+LEFT, ActionManager.ACTION_selectAndNot);
    bindAction(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_selectOr);
  }


}

