package org.jmol.popup;

import org.jmol.util.SB;

interface JmolAbstractMenu {

  void   menuAddButtonGroup(Object newMenu);
  void   menuAddItem(Object menu, Object item);
  void   menuAddSeparator(Object menu);
  void   menuAddSubMenu(Object menu, Object subMenu);
  void   checkMenuClick(Object source, String script);
  void   menuClearListeners(Object menu);
  Object menuCreateCheckboxItem(Object menu, String entry, String basename,
                                                String id, boolean state, boolean isRadio);
  Object menuCreateItem(Object menu, String entry, String script, String id);
  Object menuCreatePopup(String name);
  void   menuEnable(Object menu, boolean enable);
  void   menuEnableItem(Object item, boolean enable);
  String menuGetId(Object menu);
  void   menuGetAsText(SB sb, int level,
                                           Object menu, String menuName);
  int    menuGetItemCount(Object menu);
  Object menuGetParent(Object menu);
  int    menuGetPosition(Object menu);

  void   menuInsertSubMenu(Object menu, Object subMenu, int index);
  Object menuNewSubMenu(String entry, String id);
  void   menuRemoveAll(Object menu, int indexFrom);
  void   menuSetAutoscrolls(Object menu);
  String menuSetCheckBoxOption(Object item, String name, String what);
  void   menuSetCheckBoxState(Object item, boolean state);
  void   menuSetCheckBoxValue(Object source);   
  void   menuSetLabel(Object menu, String entry);
  void   menuSetListeners();
  void   menuShowPopup(Object popup, int x, int y);
 
}
