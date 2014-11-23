package org.jmol.api;

import org.jmol.viewer.Viewer;

public interface JmolPopupInterface {

  public void jpiDispose();
  public Object jpiGetMenuAsObject();
  public String jpiGetMenuAsString(String string);
  public void jpiInitialize(Viewer viewer, String menu);
  public void jpiShow(int x, int y);
  public void jpiUpdateComputedMenus();

 
}
