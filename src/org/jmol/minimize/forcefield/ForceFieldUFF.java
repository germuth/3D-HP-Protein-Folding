/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.minimize.forcefield;

import java.io.BufferedReader;
import org.jmol.util.JmolList;
import java.util.Hashtable;

import java.util.Map;

import org.jmol.minimize.Minimizer;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.Parser;


public class ForceFieldUFF extends ForceField {

  private static JmolList<String[]> atomTypes;
  private static Map<Object, FFParam> ffParams;
  private BS bsAromatic;
  
  public ForceFieldUFF(Minimizer minimizer) {
    this.minimizer = minimizer;
    this.name = "UFF";
  }

  @Override
  public void clear() {
    bsAromatic = null;
  }
 
  @Override
  public boolean setModel(BS bsElements, int elemnoMax) {
    setModelFields();
    Logger.info("minimize: setting atom types...");
    if (atomTypes == null && (atomTypes = getAtomTypes()) == null)
      return false;
    if (ffParams == null  && (ffParams = getFFParameters()) == null)
      return false;
    setAtomTypes(bsElements, elemnoMax);
    calc = new CalculationsUFF(this, ffParams, minAtoms, minBonds, 
        minAngles, minTorsions, minimizer.constraints);
    return calc.setupCalculations();
  }
  
  private void setAtomTypes(BS bsElements, int elemnoMax) {
    int nTypes = atomTypes.size();
    bsElements.clear(0);
    for (int i = 0; i < nTypes; i++) {
      String[] data = atomTypes.get(i);
      String smarts = data[0];
      if (smarts == null)
        continue;
      BS search = getSearch(smarts, elemnoMax, bsElements);
      // if the 0 bit in bsElements gets set, then the element is not present,
      // and there is no need to search for it;
      // if search is null, then we are done -- max elemno exceeded
      if (bsElements.get(0))
        bsElements.clear(0);
      else if (search == null)
        break;
      else
        for (int j = minimizer.bsAtoms.nextSetBit(0), pt = 0; j < minimizer.atoms.length && j >= 0; j = minimizer.bsAtoms.nextSetBit(j + 1), pt++)
            if (search.get(j))
              minAtoms[pt].sType = data[1].intern();
    }
  }

  /*
  Token[keyword(0x108002a) value="connected"]
        Token[keyword(0x880000) value="("]
        Token[integer(0x2) intValue=1(0x1) value="1"]
        Token[keyword(0x880008) value=","]
        Token[string(0x4) value="triple"]
        Token[keyword(0x880001) value=")"]
        Token[keyword(0x880018) value="or"]
        Token[keyword(0x108002a) value="connected"]
        Token[keyword(0x880000) value="("]
        Token[integer(0x2) intValue=2(0x2) value="2"]
        Token[keyword(0x880008) value=","]
        Token[string(0x4) value="double"]
        Token[keyword(0x880001) value=")"]
        Token[keyword(0x80065) value="expressionEnd"]
  */
  private BS getSearch(String smarts, int elemnoMax, BS bsElements) {
    /*
     * 
     * only a few possibilities --
     *
     * [#n] an element --> elemno=n
     * [XDn] element X with n connections
     * [X^n] element X with n+1 connections
     * [X+n] element X with formal charge +n
     * 
     */

    T[] search = null;

    int len = smarts.length();
    search = tokenTypes[TOKEN_ELEMENT_ONLY];
    int n = smarts.charAt(len - 2) - '0';
    int elemNo = 0;
    if (n >= 10)
      n = 0;
    boolean isAromatic = false;
    if (smarts.charAt(1) == '#') {
      elemNo = Parser.parseInt(smarts.substring(2, len - 1));
    } else {
      String s = smarts.substring(1, (n > 0 ? len - 3 : len - 1));
      if (s.equals(s.toLowerCase())) {
        s = s.toUpperCase();
        isAromatic = true;
      }
      elemNo = Elements.elementNumberFromSymbol(s, false);
    }
    if (elemNo > elemnoMax)
      return null;
    if (!bsElements.get(elemNo)) {
      bsElements.set(0);
      return null;
    }
    switch (smarts.charAt(len - 3)) {
    case 'D':
      search = tokenTypes[TOKEN_ELEMENT_CONNECTED];
      search[PT_CONNECT].intValue = n;
      break;
    case '^': //1 or 2
      search = tokenTypes[TOKEN_ELEMENT_SP + (n - 1)];
      break;
    case '+':
      search = tokenTypes[TOKEN_ELEMENT_CHARGED];
      search[PT_CHARGE].intValue = n;
      break;
    case '-':
      search = tokenTypes[TOKEN_ELEMENT_CHARGED];
      search[PT_CHARGE].intValue = -n;
      break;
    case 'A': // amide/allylic (also just plain C=X)
      search = tokenTypes[TOKEN_ELEMENT_ALLYLIC];
      break;
    } 
    search[PT_ELEMENT].intValue = elemNo;
    Object v = minimizer.viewer.evaluateExpression(search);
    if (!(v instanceof BS))
      return null;
    BS bs = (BS) v;
    if (isAromatic && bs.cardinality() > 0) {
      if (bsAromatic == null)
        bsAromatic = (BS) minimizer.viewer.evaluateExpression(tokenTypes[TOKEN_AROMATIC]);
      bs.and(bsAromatic);
    }
    if (Logger.debugging && bs.cardinality() > 0)
      Logger.debug(smarts + " minimize atoms=" + bs);
    return bs;
  }
  
  private Map<Object, FFParam> getFFParameters() {
    FFParam ffParam;

    Map<Object, FFParam> temp = new Hashtable<Object, FFParam>();

    // open UFF.txt
    String fileName = "UFF.txt";
    BufferedReader br = null;
    try {
      br = getBufferedReader(fileName);
      String line;
      while ((line = br.readLine()) != null) {
        String[] vs = Parser.getTokens(line);
        if (vs.length < 13)
          continue;
        if (Logger.debugging)
          Logger.info(line);
        if (line.substring(0, 5).equals("param")) {
          // set up all params from this
          ffParam = new FFParam();
          temp.put(vs[1], ffParam);
          ffParam.dVal = new double[11];
          ffParam.sVal = new String[1];
          ffParam.sVal[0] = vs[1]; // atom type
          
          ffParam.dVal[CalculationsUFF.PAR_R] = Parser.parseFloatStr(vs[2]); // r1
          ffParam.dVal[CalculationsUFF.PAR_THETA] = Parser.parseFloatStr(vs[3]) 
             * Calculations.DEG_TO_RAD; // theta0(radians)
          ffParam.dVal[CalculationsUFF.PAR_X] = Parser.parseFloatStr(vs[4]); // x1
          ffParam.dVal[CalculationsUFF.PAR_D] = Parser.parseFloatStr(vs[5]); // D1
          ffParam.dVal[CalculationsUFF.PAR_ZETA] = Parser.parseFloatStr(vs[6]); // zeta
          ffParam.dVal[CalculationsUFF.PAR_Z] = Parser.parseFloatStr(vs[7]); // Z1
          ffParam.dVal[CalculationsUFF.PAR_V] = Parser.parseFloatStr(vs[8]); // Vi
          ffParam.dVal[CalculationsUFF.PAR_U] = Parser.parseFloatStr(vs[9]); // Uj
          ffParam.dVal[CalculationsUFF.PAR_XI] = Parser.parseFloatStr(vs[10]); // Xi
          ffParam.dVal[CalculationsUFF.PAR_HARD] = Parser.parseFloatStr(vs[11]); // Hard
          ffParam.dVal[CalculationsUFF.PAR_RADIUS] = Parser.parseFloatStr(vs[12]); // Radius
          
          ffParam.iVal = new int[1];

          char coord = (vs[1].length() > 2 ? vs[1].charAt(2) : '1'); // 3rd character of atom type

          switch (coord) {
          case 'R':
            coord = '2';
            break;
          default: // general case (unknown coordination)
            // These atoms appear to generally be linear coordination like Cl
            coord = '1';
            break;
          case '1': // linear
          case '2': // trigonal planar (sp2)
          case '3': // tetrahedral (sp3)
          case '4': // square planar
          case '5': // trigonal bipyramidal -- not actually in parameterization
          case '6': // octahedral
            break;
          }
          ffParam.iVal[0] = coord - '0';
        }
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Exception " + e.toString() + " in getResource "
          + fileName);
      try{
        br.close();
      } catch (Exception ee) {
        
      }
      return null;
    }
    Logger.info(temp.size() + " atom types read from " + fileName);
    return temp;
  }

  private JmolList<String[]> getAtomTypes() {
    JmolList<String[]> types = new  JmolList<String[]>(); //!< external atom type rules
    String fileName = "UFF.txt";
    try {
      BufferedReader br = getBufferedReader(fileName);
      String line;
      while ((line = br.readLine()) != null) {
        if (line.length() > 4 && line.substring(0, 4).equals("atom")) {
          String[] vs = Parser.getTokens(line);
          String[] info = new String[] { vs[1], vs[2] };
          types.addLast(info);
        }
      }

      br.close();
    } catch (Exception e) {
      System.err.println("Exception " + e.toString() + " in getResource "
          + fileName);

    }
    Logger.info(types.size() + " UFF parameters read");
    return (types.size() > 0 ? types : null);
  }
  
  //////////////// atom type support //////////////////
  
  
  private final static int TOKEN_ELEMENT_ONLY = 0;
  private final static int TOKEN_ELEMENT_CHARGED = 1;
  private final static int TOKEN_ELEMENT_CONNECTED = 2;
  private final static int TOKEN_AROMATIC = 3;
  private final static int TOKEN_ELEMENT_SP = 4;
 // private final static int TOKEN_ELEMENT_SP2 = 5;
  private final static int TOKEN_ELEMENT_ALLYLIC = 6;
  
  /*
Token[keyword(0x80064) value="expressionBegin"]
Token[keyword(0x2880034) intValue=2621446(0x280006) value="="]
Token[integer(0x2) intValue=6(0x6) value="6"]
Token[keyword(0x880020) value="and"]
Token[keyword(0x108002a) value="connected"]
Token[keyword(0x880000) value="("]
Token[integer(0x2) intValue=3(0x3) value="3"]
Token[keyword(0x880001) value=")"]

   */
  private final static int PT_ELEMENT = 2;
  private final static int PT_CHARGE = 5;
  private final static int PT_CONNECT = 6;
  
  private final static T[][] tokenTypes = new T[][] {
         /*0*/  new T[]{
       T.tokenExpressionBegin,
       T.n(T.opEQ, T.elemno), 
       T.i(0), //2
       T.tokenExpressionEnd},
         /*1*/  new T[]{
       T.tokenExpressionBegin,
       T.n(T.opEQ, T.elemno), 
       T.i(0), //2
       T.tokenAnd, 
       T.n(T.opEQ, T.formalcharge),
       T.i(0), //5
       T.tokenExpressionEnd},
         /*2*/  new T[]{
       T.tokenExpressionBegin,
       T.n(T.opEQ, T.elemno), 
       T.i(0)  ,  // 2
       T.tokenAnd, 
       T.tokenConnected,
       T.tokenLeftParen,
       T.i(0),   // 6
       T.tokenRightParen,
       T.tokenExpressionEnd},
         /*3*/  new T[]{     // not used this way
       T.tokenExpressionBegin,
       T.o(T.identifier, "flatring"),
       T.tokenExpressionEnd},
         /*4*/  new T[]{ //sp == connected(1,"triple") or connected(2, "double")
       T.tokenExpressionBegin,
       T.n(T.opEQ, T.elemno), 
       T.i(0)  ,  // 2
       T.tokenAnd, 
       T.tokenLeftParen,
       T.tokenConnected,
       T.tokenLeftParen,
       T.i(1),
       T.tokenComma,
       T.o(T.string, "triple"),
       T.tokenRightParen,
       T.tokenOr,
       T.tokenConnected,
       T.tokenLeftParen,
       T.i(2),
       T.tokenComma,
       T.o(T.string, "double"),
       T.tokenRightParen,
       T.tokenRightParen,
       T.tokenExpressionEnd},
         /*5*/  new T[]{  // sp2 == connected(1, double)
       T.tokenExpressionBegin,
       T.n(T.opEQ, T.elemno), 
       T.i(0)  ,  // 2
       T.tokenAnd, 
       T.o(T.connected, "connected"),
       T.tokenLeftParen,
       T.i(1),
       T.tokenComma,
       T.o(T.string, "double"),
       T.tokenRightParen,
       T.tokenExpressionEnd},
       /*6*/  new T[]{ //Nv vinylic == connected(3) && connected(connected("double"))
       T.tokenExpressionBegin,
       T.n(T.opEQ, T.elemno), 
       T.i(0)  ,  // 2
       T.tokenAnd, 
       T.tokenConnected,
       T.tokenLeftParen,
       T.i(3),
       T.tokenRightParen,
       T.tokenAnd, 
       T.tokenConnected,
       T.tokenLeftParen,
       T.tokenConnected,
       T.tokenLeftParen,
       T.o(T.string, "double"),
       T.tokenRightParen,
       T.tokenRightParen,
       T.tokenExpressionEnd},
  };

}
