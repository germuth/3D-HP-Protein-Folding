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
package org.jmol.awtjs2d;

//import java.net.URL;

import org.jmol.i18n.GT;
import org.jmol.modelkit.ModelKitPopupResourceBundle;
import org.jmol.popup.PopupResource;
//import org.jmol.util.Elements;
import org.jmol.util.Elements;
import org.jmol.viewer.Viewer;

public class JSModelKitPopup extends JSPopup {

  public JSModelKitPopup() {
    // required by reflection
  }
  
  public void jpiInitialize(Viewer viewer, String menu) {
    updateMode = UPDATE_NEVER;
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = new ModelKitPopupResourceBundle();
    initialize(viewer, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }

  @Override
  public void checkMenuClick(Object source, String script) {
    if (script.equals("clearQ")) {
      for (Object o : htCheckbox.values()) {
        /**
         * @j2sNative
         * 
         * script = o.getActionCommand();
         * if (script.indexOf(":??") < 0)
         *  continue;   
         * this.updateButton(o, "??", "_??P!:");
         * o.setSelected(false);
         * this.thisPopup.tainted = true;
         */
        {
          System.out.println(o);
        }
      }
      viewer.evalStringQuiet("set picking assignAtom_C");
      return;
    }
    super.checkMenuClick(source, script);  
  }

  @Override
  public String menuSetCheckBoxOption(Object item, String name, String what) {
    String element = GT._("Element?");
    /**
     * @j2sNative
     * 
     * element = prompt(element, "");
     * 
     */
    if (element == null || Elements.elementNumberFromSymbol(element, true) == 0)
      return null;
    updateButton(item, element, "assignAtom_" + element + "P!:??");
    return "set picking assignAtom_" + element;
  }

  @Override
  protected Object getEntryIcon(String[] ret) {
    String entry = ret[0];
    if (!entry.startsWith("<"))
      return null;
    int pt = entry.indexOf(">");
    ret[0] = entry.substring(pt + 1);
    return "org/jmol/modelkit/images/" + entry.substring(1, pt);
  }

}
