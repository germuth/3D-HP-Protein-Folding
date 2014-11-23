/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-02-07 06:40:01 -0600 (Tue, 07 Feb 2012) $
 * $Revision: 16744 $
 *
 * Copyright (C) 2002-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.appletjs;

import java.util.Hashtable;

import java.util.Map;

import org.jmol.api.JmolSyncInterface;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

final class JmolAppletRegistry {

  static Map<String, Object> htRegistry = new Hashtable<String, Object>();

  synchronized static void checkIn(String name, JmolSyncInterface applet) {
    cleanRegistry();
    if (name != null) {
      Logger.info("AppletRegistry.checkIn(" + name + ")");
      htRegistry.put(name, applet);
    }
    if (Logger.debugging) {
      for (Map.Entry<String, Object> entry : htRegistry.entrySet()) {
        String theApplet = entry.getKey();
        Logger.debug(theApplet + " " + entry.getValue());
      }
    }
  }

  synchronized static void checkOut(String name) {
    htRegistry.remove(name);
  }

  synchronized static void findApplets(String appletName, String mySyncId,
                                       String excludeName, JmolList<String> apps) {
    if (appletName != null && appletName.indexOf(",") >= 0) {
      String[] names = TextFormat.split(appletName, ',');
      for (int i = 0; i < names.length; i++)
        findApplets(names[i], mySyncId, excludeName, apps);
      return;
    }
    String ext = "__" + mySyncId + "__";
    if (appletName == null || appletName.equals("*") || appletName.equals(">")) {
      for (String appletName2 : htRegistry.keySet()) {
        if (!appletName2.equals(excludeName) && appletName2.indexOf(ext) > 0) {
          apps.addLast(appletName2);
        }
      }
      return;
    }
    if (appletName.indexOf("__") < 0)
      appletName += ext;
    if (!htRegistry.containsKey(appletName))
      appletName = "jmolApplet" + appletName;
    if (!appletName.equals(excludeName) && htRegistry.containsKey(appletName)) {
      apps.addLast(appletName);
    }
  }

  synchronized private static void cleanRegistry() {
  }

}
