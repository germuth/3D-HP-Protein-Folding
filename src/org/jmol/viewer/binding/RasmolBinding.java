package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class RasmolBinding extends JmolBinding {

  public RasmolBinding() {
    super("selectOrToggle");
  }
    
  @Override
  protected void setSelectBindings() {
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_select);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_selectToggle);
  }

}

