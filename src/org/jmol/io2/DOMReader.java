/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.io2;

import java.util.Map;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public class DOMReader {
  private FileManager fm;
  private Viewer viewer;
  private Object[] aDOMNode = new Object[1];
  private Object atomSetCollection;
  private Map<String, Object> htParams;

  public DOMReader() {}
  
  void set(FileManager fileManager, Viewer viewer, Object DOMNode, Map<String, Object> htParams) {
    fm = fileManager;
    this.viewer = viewer;
    aDOMNode[0] = DOMNode;
    this.htParams = htParams;
  }

  void run() {
    Object info = viewer.apiPlatform.getJsObjectInfo(aDOMNode, null, null);
    // note that this will not work in JSmol because we don't implement the nameSpaceInfo stuff there
    // and we cannot pass [HTMLUnknownObject]
    if (info != null)
      htParams.put("nameSpaceInfo", info);
    atomSetCollection = viewer.getModelAdapter().getAtomSetCollectionFromDOM(
        aDOMNode, htParams);
    if (atomSetCollection instanceof String)
      return;
    viewer.zap(false, true, false);
    fm.fullPathName = fm.fileName = fm.nameAsGiven = "JSNode";
  }
}