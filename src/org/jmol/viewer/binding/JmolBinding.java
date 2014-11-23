package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class JmolBinding extends Binding {

  public JmolBinding(String name) {
    super(name);
    setGeneralBindings();
    setSelectBindings();
  }
    
  protected void setSelectBindings() {
    // these are only utilized for  set picking select
    bindAction(DOUBLE_CLICK+LEFT, ActionManager.ACTION_select);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_selectToggleExtended);
  }

  private void setGeneralBindings() {
    
    bindAction(DOUBLE_CLICK+LEFT, ActionManager.ACTION_center);
    bindAction(SINGLE_CLICK+CTRL+ALT+LEFT, ActionManager.ACTION_translate);
    bindAction(SINGLE_CLICK+CTRL+RIGHT, ActionManager.ACTION_translate);
    bindAction(DOUBLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_translate); 
    bindAction(DOUBLE_CLICK+MIDDLE, ActionManager.ACTION_translate);

    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_rotate);
    bindAction(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_rotateZ);
    bindAction(SINGLE_CLICK+SHIFT+RIGHT, ActionManager.ACTION_rotateZ);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_rotateZorZoom);
    bindAction(SINGLE_CLICK+MIDDLE, ActionManager.ACTION_rotateZorZoom);
    bindAction(WHEEL, ActionManager.ACTION_wheelZoom);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_slideZoom);

    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_navTranslate);
    
    bindAction(SINGLE_CLICK+CTRL+LEFT, ActionManager.ACTION_popupMenu);
    bindAction(SINGLE_CLICK+RIGHT, ActionManager.ACTION_popupMenu);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_clickFrank);

    bindAction(SINGLE_CLICK+CTRL+SHIFT+LEFT, ActionManager.ACTION_slab);
    bindAction(DOUBLE_CLICK+CTRL+SHIFT+LEFT, ActionManager.ACTION_depth); 
    bindAction(SINGLE_CLICK+CTRL+ALT+SHIFT+LEFT, ActionManager.ACTION_slabAndDepth);
    
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_swipe);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_spinDrawObjectCCW);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_spinDrawObjectCW);

    bindAction(SINGLE_CLICK+ALT+SHIFT+LEFT, ActionManager.ACTION_dragSelected);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_dragZ);
    bindAction(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_rotateSelected);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_rotateBranch);

    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_dragLabel);    
    bindAction(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_dragDrawPoint);
    bindAction(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_dragDrawObject);
    
    bindAction(DOUBLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_reset);
    bindAction(DOUBLE_CLICK+MIDDLE, ActionManager.ACTION_reset); 
    
    bindAction(DOUBLE_CLICK+LEFT, ActionManager.ACTION_stopMotion); 
    
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_dragAtom);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_dragMinimize);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_dragMinimizeMolecule);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickAtom);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickPoint);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickLabel);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickMeasure);
    bindAction(DOUBLE_CLICK+LEFT, ActionManager.ACTION_setMeasure);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickIsosurface); // requires set drawPicking     
    bindAction(SINGLE_CLICK+CTRL+SHIFT+LEFT, ActionManager.ACTION_pickNavigate);      
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_deleteAtom);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_deleteBond);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_connectAtoms);
    bindAction(SINGLE_CLICK+LEFT, ActionManager.ACTION_assignNew);
  }
  
}

