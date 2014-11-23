/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shapespecial;


import org.jmol.shape.AtomShape;
import org.jmol.util.BS;

public class Vectors extends AtomShape {

 @Override
protected void initModelSet() {
    if (!(isActive = modelSet.modelSetHasVibrationVectors()))
      return;
    super.initModelSet();
  }

 @Override
public void setProperty(String propertyName, Object value, BS bsSelected) {
    if (!isActive)
      return;
    setPropAS(propertyName, value, bsSelected);
  }
  
 @Override
public Object getProperty(String propertyName, int param) {
   if (propertyName == "mad")
     return Integer.valueOf(mads == null || param < 0 || mads.length <= param ? 0 : mads[param]);
   return null;
 }

}
