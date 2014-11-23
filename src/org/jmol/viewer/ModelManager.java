/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;


import org.jmol.modelset.ModelLoader;
import org.jmol.modelset.ModelSet;
import org.jmol.util.BS;
import org.jmol.util.SB;

class ModelManager {

  private final Viewer viewer;
  private ModelSet modelSet;

  private String modelSetPathName;
  private String fileName;

  ModelManager(Viewer viewer) {
    this.viewer = viewer;
  }

  ModelSet zap() {
    modelSetPathName = fileName = null;
    return (modelSet = (new ModelLoader(viewer, viewer.getZapName(), null, null, null, null)).getModelSet());
  }
  
  String getModelSetFileName() {
    return (fileName != null ? fileName : viewer.getZapName());
  }

  String getModelSetPathName() {
    return modelSetPathName;
  }

  ModelSet createModelSet(String fullPathName, String fileName,
                          SB loadScript, Object atomSetCollection,
                          BS bsNew, boolean isAppend) {
    String modelSetName = null;
    if (isAppend) {
      modelSetName = modelSet.modelSetName;
      if (modelSetName.equals("zapped"))
        modelSetName = null;
      else if (modelSetName.indexOf(" (modified)") < 0)
        modelSetName += " (modified)";
    } else if (atomSetCollection == null) {
      return zap();
    } else {
      this.modelSetPathName = fullPathName;
      this.fileName = fileName;
    }
    if (atomSetCollection != null) {
      if (modelSetName == null) {
        modelSetName = viewer.getModelAdapter().getAtomSetCollectionName(
            atomSetCollection);
        if (modelSetName != null) {
          modelSetName = modelSetName.trim();
          if (modelSetName.length() == 0)
            modelSetName = null;
        }
        if (modelSetName == null)
          modelSetName = reduceFilename(fileName);
      }
      modelSet = (new ModelLoader(viewer, modelSetName, loadScript,
          atomSetCollection, (isAppend ? modelSet : null), bsNew)).getModelSet();
    }
    if (modelSet.getAtomCount() == 0)
      zap();
    return modelSet;
  }

  private static String reduceFilename(String fileName) {
    if (fileName == null)
      return null;
    int ichDot = fileName.indexOf('.');
    if (ichDot > 0)
      fileName = fileName.substring(0, ichDot);
    if (fileName.length() > 24)
      fileName = fileName.substring(0, 20) + " ...";
    return fileName;
  }

  void createAtomDataSet(Object atomSetCollection, int tokType) {
    ModelLoader.createAtomDataSet(viewer, modelSet, tokType, atomSetCollection,
    viewer.getSelectionSet(false));    
  }

}
