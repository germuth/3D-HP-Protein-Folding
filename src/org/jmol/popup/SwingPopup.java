/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.popup;

import org.jmol.util.Logger;
import org.jmol.util.SB;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;


/**
 * all popup-related awt/swing class references are in this file.
 */
abstract public class SwingPopup extends GenericPopup {

  
  @Override
  public void finalize() {
    System.out.println("SwingPopup Finalize " + this);
  }
  
  private MenuItemListener mil;
  private CheckboxMenuItemListener cmil;
  private MenuMouseListener mfl;

  public SwingPopup() {
    // required by reflection
  }

  /**
   * update the button depending upon its type
   * 
   * @param b
   * @param entry
   * @param script
   */
  private void updateButton(AbstractButton b, String entry, String script) {
    String[] ret = new String[] { entry };    
    ImageIcon icon = (ImageIcon) getEntryIcon(ret);
    entry = ret[0];
    if (icon != null)
      b.setIcon(icon);
    if (entry != null)
      b.setText(entry);
    if (script != null)
      b.setActionCommand(script);
  }

  private Object newMenuItem(JMenuItem jmi, Object menu, String entry,
                             String script, String id) {
    updateButton(jmi, entry, script);    
    if (id != null && id.startsWith("Focus")) {
      jmi.addMouseListener(mfl);
      id = ((Component) menu).getName() + "." + id;
    }
    jmi.setName(id == null ? ((Component) menu).getName() + "." : id);
    menuAddItem(menu, jmi);
    return jmi;
  }

  private class MenuItemListener implements ActionListener {
    
    protected MenuItemListener(){}
    
    public void actionPerformed(ActionEvent e) {
      checkMenuClick(e.getSource(), e.getActionCommand());
    }
  }

  private class MenuMouseListener implements MouseListener {

    protected MenuMouseListener(){}

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
      if (e.getSource() instanceof JMenuItem) {
        JMenuItem jmi = (JMenuItem) e.getSource();
        checkMenuFocus(jmi.getName(), jmi.getActionCommand(), true);
      }
    }

    public void mouseExited(MouseEvent e) {
      if (e.getSource() instanceof JMenuItem) {
        JMenuItem jmi = (JMenuItem) e.getSource();
        checkMenuFocus(jmi.getName(), jmi.getActionCommand(), false);
      }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

  }

  private class CheckboxMenuItemListener implements ItemListener {

    protected CheckboxMenuItemListener(){}

    public void itemStateChanged(ItemEvent e) {
      checkBoxStateChanged(e.getSource());
    }
  }

  /// JmolAbstractMenu ///

  public void menuAddButtonGroup(Object newMenu) {
    if (buttonGroup == null)
      buttonGroup = new ButtonGroup();
    ((ButtonGroup) buttonGroup).add((JMenuItem) newMenu);
  }

  public void menuAddItem(Object menu, Object item) {
    if (menu instanceof JPopupMenu) {
      ((JPopupMenu) menu).add((JComponent) item);
    } else if (menu instanceof JMenu) {
      ((JMenu) menu).add((JComponent) item);
    } else {
      Logger.warn("cannot add object to menu: " + menu);
    }
  }

  public void menuAddSeparator(Object menu) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu) menu).addSeparator();
    else
      ((JMenu) menu).addSeparator();
  }

  public void menuAddSubMenu(Object menu, Object subMenu) {
    menuAddItem(menu, subMenu);
  }

  public void menuClearListeners(Object menu) {
    if (menu == null)
      return;
    Component[] subMenus = (menu instanceof JPopupMenu ? ((JPopupMenu) menu)
        .getComponents() : ((JMenu) menu).getPopupMenu().getComponents());
    for (int i = 0; i < subMenus.length; i++) {
      Component m = subMenus[i];
      if (m instanceof JMenu) {
        menuClearListeners(((JMenu) m).getPopupMenu());
      } else {
        try {
          m.removeMouseListener(mfl);
          ((AbstractButton) m).removeActionListener(mil);
          ((AbstractButton) m).removeItemListener(cmil);
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

  public Object menuCreateCheckboxItem(Object menu, String entry,
                                       String basename, String id,
                                       boolean state, boolean isRadio) {
    JMenuItem jmi;
    if (isRadio) {
      JRadioButtonMenuItem jr = new JRadioButtonMenuItem(entry);
      jmi = jr;
      jr.setArmed(state);
    } else {
      JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
      jmi = jcmi;
      jcmi.setState(state);
    }
    jmi.setSelected(state);
    jmi.addItemListener(cmil);
    return newMenuItem(jmi, menu, entry, basename, id);
  }

  public Object menuCreateItem(Object menu, String entry, String script,
                               String id) {
    JMenuItem jmi = new JMenuItem(entry);
    jmi.addActionListener(mil);
    return newMenuItem(jmi, menu, entry, script, id);
  }

  public Object menuCreatePopup(String title) {
    return new JPopupMenu(title);
  }

  public void menuEnable(Object menu, boolean enable) {
    if (menu instanceof JMenuItem) {
      menuEnableItem(menu, enable);
      return;
    }
    try {
      ((JMenu) menu).setEnabled(enable);
    } catch (Exception e) {
      //no menu item;
    }
  }

  public void menuEnableItem(Object item, boolean enable) {
    try {
      ((JMenuItem) item).setEnabled(enable);
    } catch (Exception e) {
      //no menu item;
    }
  }

  public void menuGetAsText(SB sb, int level, Object menu,
                            String menuName) {
    String name = menuName;
    Component[] subMenus = (menu instanceof JPopupMenu ? ((JPopupMenu) menu)
        .getComponents() : ((JMenu) menu).getPopupMenu().getComponents());
    for (int i = 0; i < subMenus.length; i++) {
      Object m = subMenus[i];
      String flags;
      if (m instanceof JMenu) {
        JMenu jm = (JMenu) m;
        name = jm.getName();
        flags = "enabled:" + jm.isEnabled();
        addItemText(sb, 'M', level, name, jm.getText(), null, flags);
        menuGetAsText(sb, level + 1, ((JMenu) m).getPopupMenu(), name);
      } else if (m instanceof JMenuItem) {
        JMenuItem jmi = (JMenuItem) m;
        flags = "enabled:" + jmi.isEnabled();
        if (m instanceof JCheckBoxMenuItem)
          flags += ";checked:" + ((JCheckBoxMenuItem) m).getState();
        String script = fixScript(jmi.getName(), jmi.getActionCommand());
        addItemText(sb, 'I', level, jmi.getName(), jmi.getText(), script, flags);
      } else {
        addItemText(sb, 'S', level, name, null, null, null);
      }
    }
  }

  public String menuGetId(Object menu) {
    return ((Component) menu).getName();
  }

  public int menuGetItemCount(Object menu) {
    return ((JMenu) menu).getItemCount();
  }

  public Object menuGetParent(Object menu) {
    return ((JMenu) menu).getParent();
  }

  public int menuGetPosition(Object menu) {
    Object p = menuGetParent(menu);
    if (p instanceof JPopupMenu) {
      for (int i = ((JPopupMenu) p).getComponentCount(); --i >= 0;)
        if (((JPopupMenu) p).getComponent(i) == menu)
          return i;
    } else {
      for (int i = ((JMenu) p).getItemCount(); --i >= 0;)
        if (((JMenu) p).getItem(i) == menu)
          return i;
    }
    return -1;
  }

  public void menuInsertSubMenu(Object menu, Object subMenu, int index) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu) menu).insert((JMenu) subMenu, index);
    else
      ((JMenu) menu).insert((JMenu) subMenu, index);
  }

  public Object menuNewSubMenu(String entry, String id) {
    JMenu jm = new JMenu(entry);
    updateButton(jm, entry, null);
    jm.setName(id);
    jm.setAutoscrolls(true);
    return jm;
  }

  public void menuRemoveAll(Object menu, int indexFrom) {
    if (indexFrom > 0) {
      for (int i = menuGetItemCount(menu); --i >= indexFrom;)
        ((JMenu) menu).remove(i);
      return;
    }
    if (menu instanceof JMenu)
      ((JMenu) menu).removeAll();
    else
      ((JPopupMenu)menu).removeAll();
  }

  public void menuSetAutoscrolls(Object menu) {
    ((JMenu) menu).setAutoscrolls(true);
  }

  public void menuSetCheckBoxState(Object item, boolean state) {
    if (item instanceof JCheckBoxMenuItem)
      ((JCheckBoxMenuItem) item).setState(state);
    else
      ((JRadioButtonMenuItem) item).setArmed(state);
    ((JMenuItem) item).setSelected(state);
  }

  public String menuSetCheckBoxOption(Object item, String name, String what) {
    return null;
  }

  public void menuSetCheckBoxValue(Object source) {
    JMenuItem jcmi = (JMenuItem) source;
    setCheckBoxValue(jcmi, jcmi.getActionCommand(), jcmi.isSelected());
  }

  public void menuSetLabel(Object menu, String entry) {
    if (menu instanceof JMenuItem)
      ((JMenuItem) menu).setText(entry);
    else
      ((JMenu) menu).setText(entry);
  }

  public void menuSetListeners() {
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
    mfl = new MenuMouseListener();
  }
  
  public void menuShowPopup(Object popup, int x, int y) {
    try {
      ((JPopupMenu)popup).show((Component) viewer.getDisplay(), x, y);
    } catch (Exception e) {
      // ignore
    }
  }

}
