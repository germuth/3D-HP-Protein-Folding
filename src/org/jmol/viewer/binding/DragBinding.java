package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class DragBinding extends JmolBinding {

  public DragBinding() {
    super("drag");
  }
    
  @Override
  protected void setSelectBindings() {
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_select);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_selectToggle);
    bindAction(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_selectOr);
    bindAction(SINGLE_CLICK+ALT+SHIFT+LEFT, ActionManager.ACTION_selectAndNot);
    bindAction(DOWN+LEFT, ActionManager.ACTION_selectAndDrag);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_dragSelected);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickAtom);
  }


}

