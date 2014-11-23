/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.util;

import java.util.ArrayList;

/**
 * created to remove ambiguities in add and remove
 * 
 * @param <V>
 */
public class JmolList<V> extends ArrayList<V> {

  //needed?
  public JmolList() {
    super();  
  }
  
  public boolean addLast(V v) {
    /**
     * no overloading of add(Object) in JavaScript
     * 
     * @j2sNative
     * 
     * return this.add1(v);
     *  
     */
    {
      return super.add(v);
    }
  }
  
  public boolean removeObj(Object v) {
    /**
     * no overloading of remove(Object) in JavaScript
     * 
     * @j2sNative
     * 
     * return this.removeObject(v);
     *  
     */
    {
      return super.remove(v);
    }
  }
  
}
