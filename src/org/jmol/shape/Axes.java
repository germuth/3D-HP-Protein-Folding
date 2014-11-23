/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.shape;



import org.jmol.api.SymmetryInterface;
import org.jmol.constant.EnumAxesMode;
import org.jmol.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.util.V3;
import org.jmol.viewer.JC;

public class Axes extends FontLineShape {

  public P3 axisXY = new P3();
  public float scale;
  
  private P3 fixedOrigin;
  final P3 originPoint = new P3();
  final P3[] axisPoints = new P3[6];
  final static P3 pt0 = new P3();
  public String[] labels;
  
  {
    for (int i = 6; --i >= 0; )
      axisPoints[i] = new P3();
  }

  public P3 getOriginPoint(boolean isDataFrame) {
    return (isDataFrame ? pt0 : originPoint);
  }
  
  final P3 ptTemp = new P3();
  public P3 getAxisPoint(int i, boolean isDataFrame) {
    if (!isDataFrame && axisXY.z == 0)
      return axisPoints[i];
    ptTemp.setT(axisPoints[i]);
    ptTemp.sub(originPoint);
    ptTemp.scale(0.5f);
    return ptTemp; 
  }
  
  private final static float MIN_AXIS_LEN = 1.5f;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if ("position" == propertyName) {
      axisXY = (P3) value;
      // z = 0 for no set xy position (default)
      // z = -Float.MAX_VALUE for percent
      // z = Float.MAX_VALUE for positioned
      return;
    }
    if ("origin" == propertyName) {
      if (value == null) {
        fixedOrigin = null;
      } else {
        if (fixedOrigin == null)
          fixedOrigin = new P3();
        fixedOrigin.setT((P3) value);
      }
      initShape();
      return;
    }
    if ("labels" == propertyName) {
      labels = (String[]) value;
      return;
    }
    if ("labelsOn" == propertyName) {
      labels = null;
      return;
    }
    if ("labelsOff" == propertyName) {
      labels = new String[] {"", "", ""};
      return;
    }
    
    setPropFLS(propertyName, value);
  }

  @Override
  public void initShape() {
    super.initShape();
    myType = "axes";
    font3d = gdata.getFont3D(JC.AXES_DEFAULT_FONTSIZE);
    EnumAxesMode axesMode = viewer.getAxesMode();
    if (fixedOrigin == null)
      originPoint.set(0, 0, 0);
    else
      originPoint.setT(fixedOrigin);
    if (axesMode == EnumAxesMode.UNITCELL
        && modelSet.unitCells != null) {
      SymmetryInterface unitcell = viewer.getCurrentUnitCell();
      if (unitcell != null) {
        P3[] vectors = unitcell.getUnitCellVertices();
        P3 offset = unitcell.getCartesianOffset();
        if (fixedOrigin == null) {
          originPoint.setT(offset);
        } else {
          offset = fixedOrigin;
        }
        scale = viewer.getAxesScale() / 2f;
        // We must divide by 2 because that is the default for ALL axis types.
        // Not great, but it will have to do.
        axisPoints[0].scaleAdd2(scale, vectors[4], offset);
        axisPoints[1].scaleAdd2(scale, vectors[2], offset);
        axisPoints[2].scaleAdd2(scale, vectors[1], offset);
        return;
      }
    } else if (axesMode == EnumAxesMode.BOUNDBOX) {
      if (fixedOrigin == null)
        originPoint.setT(viewer.getBoundBoxCenter());
    }
    setScale(viewer.getAxesScale() / 2f);
  }
  
  @Override
  public Object getProperty(String property, int index) {
    if (property == "axisPoints")
      return axisPoints;
    if (property == "origin")
      return fixedOrigin;
    if (property == "axesTypeXY")
      return (axisXY.z == 0 ? Boolean.FALSE : Boolean.TRUE);
    return null;
  }

  V3 corner = new V3();
  
  void setScale(float scale) {
    this.scale = scale;
    corner.setT(viewer.getBoundBoxCornerVector());
    for (int i = 6; --i >= 0;) {
      P3 axisPoint = axisPoints[i];
      axisPoint.setT(JC.unitAxisVectors[i]);
      // we have just set the axisPoint to be a unit on a single axis
   
      // therefore only one of these values (x, y, or z) will be nonzero
      // it will have value 1 or -1
      if (corner.x < MIN_AXIS_LEN)
        corner.x = MIN_AXIS_LEN;
      if (corner.y < MIN_AXIS_LEN)
        corner.y = MIN_AXIS_LEN;
      if (corner.z < MIN_AXIS_LEN)
        corner.z = MIN_AXIS_LEN;
      if (axisXY.z == 0) {
        axisPoint.x *= corner.x * scale;
        axisPoint.y *= corner.y * scale;
        axisPoint.z *= corner.z * scale;
      }
      axisPoint.add(originPoint);
    }
  }
  
 @Override
public String getShapeState() {
    SB sb = new SB();
    sb.append("  axes scale ").appendF(viewer.getAxesScale()).append(";\n"); 
    if (fixedOrigin != null)
      sb.append("  axes center ")
          .append(Escape.eP(fixedOrigin)).append(";\n");
    if (axisXY.z != 0)
      sb.append("  axes position [")
          .appendI((int) axisXY.x).append(" ")
          .appendI((int) axisXY.y).append(" ")
          .append(axisXY.z < 0 ? " %" : "").append("];\n");
    if (labels != null) {
      sb.append("  axes labels ");
      for (int i = 0; i < labels.length; i++)
        if (labels[i] != null)
          sb.append(Escape.eS(labels[i])).append(" ");
      sb.append(";\n");
    }
    return super.getShapeState() + sb;
  }

}
