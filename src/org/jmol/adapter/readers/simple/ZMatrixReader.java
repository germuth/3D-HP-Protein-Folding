/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-28 23:13:00 -0500 (Thu, 28 Sep 2006) $
 * $Revision: 5772 $
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

package org.jmol.adapter.readers.simple;

import org.jmol.util.JmolList;
import java.util.Hashtable;

import java.util.Map;


import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.P3;
import org.jmol.util.Point4f;
import org.jmol.util.Quaternion;
import org.jmol.util.V3;

public class ZMatrixReader extends AtomSetCollectionReader {
  /*
   * a simple Z-matrix reader
   * 
   * Can be invoked using ZMATRIX::   or with file starting with #ZMATRIX
   * # are comments; can include jmolscript: xxxx
   * 
   * a positive dihedral is defined as being
   * 
   *              (back)
   *        +120 /
   *  (front)---O
   *  
   *  Any invalid element symbol such as X or XX indicates a dummy
   *  atom that will not be included in the model but is needed
   *  to create the structure
   * 
   * Bob Hanson hansonr@stolaf.edu 11/19/2011
   */

  /*  SYNTAX 

Anything after # on a line is considered a comment.
A first line starting with #ZMATRIX defines the file type:
  Jmol z-format type (just #ZMATRIX) 
  Gaussian (#ZMATRIX GAUSSIAN) 
  Mopac (#ZMATRIX MOPAC) 
Lines starting with # may contain jmolscript
Blank lines are ignored:

#ZMATRIX -- methane
#jmolscript: spin on

C
H   1 1.089000     
H   1 1.089000  2  109.4710      
H   1 1.089000  2  109.4710  3  120.0000   
H   1 1.089000  2  109.4710  3 -120.0000

Bonds will not generally be added, leaving Jmol to do its autoBonding.
To add bond orders, just add them as one more integer on any line
other than the first-atom line:

#ZMATRIX -- CO2 
C
O   1 1.3000                 2     
O   1 1.3000    2  180       2      

Any position number may be replaced by a unique atom name, with number:

#ZMATRIX -- CO2
C1
O1   C1 1.3000                2     
O2   C1 1.3000    O1  180     2      

Ignored dummy atoms are any atoms starting with "X" and a number,
allowing for positioning:

#ZMATRIX -- CO2
X1
X2   X1 1.0
C1   X1 1.0       X2 90
O1   C1 1.3000    X2 90   X1 0  2     
O2   C1 1.3000    O1 180  X2 0  2      

Negative distance indicates that the second angle is a normal angle, not a dihedral:

#ZMATRIX -- NH3 (using simple angles only)
N1 
H1 N1 1.0
H2 N1 1.0 H1 107  
H3 N1 -1.0 H1 107 H2 107

Negative distance and one negative angle reverses the chirality:

#ZMATRIX -- NH3 (using simple angles only; reversed chirality):
N1 
H1 N1 1.0
H2 N1 1.0 H1 107  
H3 N1 -1.0 H1 -107 H2 107

Symbolics may be used -- they may be listed first or last:

#ZMATRIX

dist 1.0
angle 107

N1 
H1 N1 dist
H2 N1 dist H1 angle 
H3 N1 -dist H1 angle H2 angle

All atoms will end up with numbers in their names, 
but you do not need to include those in the z-matrix file
if atoms have unique elements or you are referring
to the last atom of that type. Still, numbering is recommended.

#ZMATRIX

dist 1.0
angle 107

N                  # will be N1
H N dist           # will be H2
H N dist H angle   # will be H3
H N -dist H2 angle H angle # H here refers to H3

MOPAC format will have an initial comment section. Isotopes are in the form of "C13". 
If isotopes are used, the file type MUST be identified as #ZMATRIX MOPAC. 
Lines prior to the first line with less than 2 characters will be considered to be comments and ignored.

 AM1
Ethane

C
C     1     r21
H     2     r32       1     a321
H     2     r32       1     a321      3  d4213
H     2     r32       1     a321      3 -d4213
H     1     r32       2     a321      3   60.
H     1     r32       2     a321      3  180.
H     1     r32       2     a321      3  d300

r21        1.5
r32        1.1
a321     109.5
d4213    120.0
d300     300.0

Gaussian will not have the third line blank and has a slightly 
different format for showing the alternative two-angle format
involving the eighth field having the flag 1
(see http://www.gaussian.com/g_tech/g_ur/c_zmat.htm):

C5 O1 1.0 C2 110.4 C4 105.4 1
C6 O1 R   C2 A1    C3 A2    1

Note that Gaussian cartesian format is allowed -- simply 
set the first atom index to be 0:


No distinction between "Variable:" and "Constant:" is made by Jmol.

   */

  protected int atomCount;
  protected JmolList<Atom> vAtoms = new JmolList<Atom>();
  private Map<String, Integer> atomMap = new Hashtable<String, Integer>();
  private String[] tokens;
  private boolean isJmolZformat;
  private JmolList<String[]> lineBuffer = new JmolList<String[]>();
  private Map<String, Float> symbolicMap = new Hashtable<String, Float>();
  private boolean isMopac;
  private boolean isHeader = true;
  
  @Override
  protected boolean checkLine() throws Exception {
    // easiest just to grab all lines that are comments or symbolic first, then do the processing of atoms.
    cleanLine();
    if (line.length() <= 2) // for Mopac, could be blank or an atom symbol, but not an atom name
      isHeader = false;
    if (line.startsWith("#") || isMopac && isHeader) {
      if (line.startsWith("#ZMATRIX"))
        isJmolZformat = line.toUpperCase().indexOf("GAUSSIAN") < 0
            && !(isMopac = (line.toUpperCase().indexOf("MOPAC") >= 0));
      checkCurrentLineForScript();
      return true;
    }
    if (line.indexOf("#") >= 0)
      line = line.substring(0, line.indexOf("#"));
    if (line.indexOf(":") >= 0)
      return true; // Variables: or Constants:
    tokens = getTokensStr(line);
    if (tokens.length == 2) {
      getSymbolic();
      return true;
    }
    lineBuffer.addLast(tokens);
    return true;
  }

  private void cleanLine() {
    // remove commas for Gaussian and parenthetical expressions for MOPAC 
    line = line.replace(',', ' ');
    int pt1, pt2;
    while ((pt1 = line.indexOf('(')) >= 0 && (pt2 = line.indexOf('(', pt1)) >= 0)
      line = line.substring(0, pt1) + " " + line.substring(pt2 + 1);
    line = line.trim();
  }

  @Override
  protected void finalizeReader() throws Exception {
    int firstLine = 0;
    for (int i = firstLine; i < lineBuffer.size(); i++)
      if ((tokens = lineBuffer.get(i)).length > 0)
        getAtom();
    super.finalizeReader();
  }

  private void getSymbolic() {
    if (symbolicMap.containsKey(tokens[0]))
      return;
    float f = parseFloatStr(tokens[1]);
    symbolicMap.put(tokens[0], Float.valueOf(f));
    Logger.info("symbolic " + tokens[0] + " = " + f);
  }

  private void getAtom() throws Exception {
    float f;
    Atom atom = new Atom();
    String element = tokens[0];
    int i = element.length();
    while (--i >= 0 && Character.isDigit(element.charAt(i))) {
      //continue;
    }
    if (++i == 0)
      throw new Exception("Bad Z-matrix atom name");
    if (i == element.length()) {
      // no number -- append atomCount
      atom.atomName = element + (atomCount + 1);
    } else {
      // has a number -- pull out element
      atom.atomName = element;
      element = element.substring(0, i);
    }
    if (isMopac && i != tokens[0].length())      // C13 == 13C
      element = tokens[0].substring(i) + element;
    setElementAndIsotope(atom, element);
    
    int ia = getAtomIndex(1);
    int bondOrder = 0;
    switch (tokens.length) {
    case 8:
      // angle + dihedral + bond order
      // angle + angle + bond order 
    case 6:
      // angle + bond order 
      bondOrder = (int) getValue(tokens.length - 1);
      //$FALL-THROUGH$
    case 5:
      // angle  
      // Gaussian Sym 0 x y z 
      if (tokens.length == 5 && tokens[1].equals("0")) {
        atom.set(getValue(2), getValue(3), getValue(4));
        bondOrder = 0;
        break;
      }
      //$FALL-THROUGH$
    case 7:
      // angle + dihedral or angle + angle
      int ib, ic;
      if (tokens.length < 7 && atomCount != 2
          || (ib = getAtomIndex(3)) < 0
          || (ic = (tokens.length < 7 ? -2 : getAtomIndex(5))) == -1
        ) {
        atom = null;
      } else {
        float d = getValue(2);
        float theta1 = getValue(4);
        float theta2 = (tokens.length < 7 ? Float.MAX_VALUE : getValue(6));
        if (tokens.length == 8 && !isJmolZformat && !isMopac && bondOrder == 1)
          // Gaussian indicator of alternative angle representation
          d = -Math.abs(d);
        atom = setAtom(atom, ia, ib, ic, d, theta1, theta2); 
      }
      break;
    case 4:
      // angle + bond order
      // Gaussian cartesian
      if (getAtomIndex(1) < 0) {
        atom.set(getValue(1), getValue(2), getValue(3));
        break;
      }
      bondOrder = (int) getValue(3);
      //$FALL-THROUGH$
    case 3:
      // angle
      f = getValue(2);
      if (atomCount != 1 
          || (ia = getAtomIndex(1)) != 0) {
        atom = null;
      } else {
        atom.set(f, 0, 0);
      }
      break;
    case 1:
      if (atomCount != 0)
        atom = null;
      else
        atom.set(0, 0, 0);
      break;
    default:
      atom = null;
    }
    if (atom == null)
      throw new Exception("bad Z-Matrix line");
    vAtoms.addLast(atom);
    atomMap.put(atom.atomName, Integer.valueOf(atomCount));
    atomCount++;
    if (element.startsWith("X") && JmolAdapter.getElementNumber(element) < 1) {
      Logger.info("#dummy atom ignored: atom " + atomCount + " - "
          + atom.atomName);
    } else {
      atomSetCollection.addAtom(atom);
      setAtomCoord(atom);
      Logger.info(atom.atomName + " " + atom.x + " " + atom.y + " " + atom.z);
      if (isJmolZformat && bondOrder > 0)
        atomSetCollection.addBond(new Bond(atom.atomIndex,
            vAtoms.get(ia).atomIndex, bondOrder));
    }
  }

  private float getSymbolic(String key) {
    boolean isNeg = key.startsWith("-");
    Float F = symbolicMap.get(isNeg ? key.substring(1) : key);
    if (F == null)
      return Float.NaN;
    float f = F.floatValue();
    return (isNeg ? -f : f);
  }
  
  private float getValue(int i) throws Exception {
    float f = getSymbolic(tokens[i]);
    if (Float.isNaN(f))
      f = parseFloatStr(tokens[i]);
    if (Float.isNaN(f))
      throw new Exception("Bad Z-matrix value: " + tokens[i]);
    return f;
  }

  private int getAtomIndex(int i) {
    String name;
    if (i >= tokens.length 
        || (name = tokens[i]).indexOf(".") >= 0
        || !Character.isLetterOrDigit(name.charAt(0)))
      return -1;
    int ia = parseIntStr(name);
    if (ia <= 0 || name.length() != ("" + ia).length()) {
      // check for clean integer, not "13C1"
      Integer I = atomMap.get(name);
      if (I == null) {
        // We allow just an element name without a number if it is unique.
        // The most recent atom match will be used 
        for (i = vAtoms.size(); --i >= 0; ) {
          Atom atom = vAtoms.get(i);
          if (atom.atomName.startsWith(name) 
              && atom.atomName.length() > name.length()
              && Character.isDigit(atom.atomName.charAt(name.length()))) {
            I = atomMap.get(atom.atomName);
            break;
          }  
        }
      } 
      if (I == null)
        ia = -1;
      else
        ia = I.intValue();
    } else {
      ia--;
    }
    return ia;
  }

  private final P3 pt0 = new P3();
  private final V3 v1 = new V3();
  private final V3 v2 = new V3();
  private final Point4f plane1 = new Point4f();
  private final Point4f plane2 = new Point4f();
  
  protected Atom setAtom(Atom atom, int ia, int ib, int ic, float d,
                       float theta1, float theta2) {
    if (Float.isNaN(theta1) || Float.isNaN(theta2))
      return null;
    pt0.setT(vAtoms.get(ia));
    v1.sub2(vAtoms.get(ib), pt0);
    v1.normalize();
    if (theta2 == Float.MAX_VALUE) {
      // just the first angle being set
      v2.set(0, 0, 1);
      (Quaternion.newVA(v2, theta1)).transformP2(v1, v2);
    } else if (d >= 0) {
      // theta2 is a dihedral angle
      // just do two quaternion rotations
      v2.sub2(vAtoms.get(ic), pt0);
      v2.cross(v1, v2);
      (Quaternion.newVA(v2, theta1)).transformP2(v1, v2);
      (Quaternion.newVA(v1, -theta2)).transformP2(v2, v2);
    } else {
      // d < 0
      // theta1 and theta2 are simple angles atom-ia-ib and atom-ia-ic 
      // get vector that is intersection of two planes and go from there
      Measure.getPlaneThroughPoint(setAtom(atom, ia, ib, ic, -d, theta1, 0),
          v1, plane1);
      Measure.getPlaneThroughPoint(setAtom(atom, ia, ic, ib, -d, theta2, 0),
          v1, plane2);
      JmolList<Object> list = Measure.getIntersectionPP(plane1, plane2);
      if (list.size() == 0)
        return null;
      pt0.setT((P3) list.get(0));
      d = (float) Math.sqrt(d * d - pt0.distanceSquared(vAtoms.get(ia)))
          * Math.signum(theta1) * Math.signum(theta2);
      v2.setT((V3) list.get(1));
    }
    atom.scaleAdd2(d, v2, pt0);
    return atom;
  }
}
