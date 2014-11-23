/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-06-05 21:50:17 -0500 (Sat, 05 Jun 2010) $
 * $Revision: 13295 $
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

package org.jmol.smiles;

import org.jmol.util.JmolList;

import java.util.Iterator;

import java.util.Hashtable;
import java.util.Map;


import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.JmolNode;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.smiles.SmilesSearch.VTemp;

/**
 * Double bond, allene, square planar and tetrahedral stereochemistry only
 * not octahedral or trigonal bipyramidal.
 * 
 * No attempt at canonicalization -- unnecessary for model searching.
 * 
 * see SmilesMatcher and package.html for details
 *
 * Bob Hanson, Jmol 12.0.RC17 2010.06.5
 *
 */
public class SmilesGenerator {

  // inputs:
  private JmolNode[] atoms;
  private int atomCount;
  private BS bsSelected;
  private BS bsAromatic;
  
  private SB ringSets;

  // data

  private VTemp vTemp = new VTemp();
  private int nPairs;
  private BS bsBondsUp = new BS();
  private BS bsBondsDn = new BS();
  private BS bsToDo;
  private JmolNode prevAtom;
  private JmolNode[] prevSp2Atoms;
  
  // outputs

  private Map<String, Object[]> htRingsSequence = new Hashtable<String, Object[]>();
  private Map<String, Object[]> htRings = new Hashtable<String, Object[]>();
  private BS bsIncludingH;

  // generation of SMILES strings

  String getSmiles(JmolNode[] atoms, int atomCount, BS bsSelected)
      throws InvalidSmilesException {
    int i = bsSelected.nextSetBit(0);
    if (i < 0)
      return "";
    this.atoms = atoms;
    this.atomCount = atomCount;
    this.bsSelected = bsSelected = BSUtil.copy(bsSelected);
    return getSmilesComponent(atoms[i], bsSelected, false);
  }

  String getBioSmiles(JmolNode[] atoms, int atomCount, BS bsSelected,
                      boolean allowUnmatchedRings, boolean addCrossLinks, String comment)
      throws InvalidSmilesException {
    this.atoms = atoms;
    this.atomCount = atomCount;
    SB sb = new SB();
    BS bs = BSUtil.copy(bsSelected);
    if (comment != null)
      sb.append("//* Jmol bioSMILES ").append(comment.replace('*', '_')).append(
        " *//");
    String end = "\n";
    BS bsIgnore = new BS();
    String lastComponent = null;
    String s;
    JmolList<Integer> vLinks = new  JmolList<Integer>();
    try {
      int len = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        JmolNode a = atoms[i];
        String ch = a.getGroup1('?');
        String bioStructureName = a.getBioStructureTypeName();
        boolean unknown = (ch.equals("?"));
        if (end != null) {
          if (sb.length() > 0)
            sb.append(end);
          end = null;
          len = 0;
          if (bioStructureName.length() > 0) {
            char id = a.getChainID();
            if (id != '\0') {
              s = "//* chain " + id + " " + bioStructureName + " " + a.getResno() + " *// ";
              len = s.length();
              sb.append(s);
            }
            sb.append("~").appendC(bioStructureName.charAt(0)).append("~");
            len++;
          } else {
            s = getSmilesComponent(a, bs, true);
            if (s.equals(lastComponent)) {
              end = "";
            } else {
              lastComponent = s;
              String groupName = a.getGroup3(true);
              if (groupName != null)
                sb.append("//* ").append(groupName).append(" *//");
              sb.append(s);
              end = ".\n";
            }
            continue;
          }
        }
        if (len >= 75) {
          sb.append("\n  ");
          len = 2;
        }
        if (unknown) {
          addBracketedBioName(sb, a, bioStructureName.length() > 0 ? ".0" : null);
        } else {
          sb.append(ch);
        }
        len++;
        int i0 = a.getOffsetResidueAtom("0", 0);
        if (addCrossLinks) {
          a.getCrossLinkLeadAtomIndexes(vLinks);
          for (int j = 0; j < vLinks.size(); j++) {
            sb.append(":");
            s = getRingCache(i0, vLinks.get(j).intValue(),
                htRingsSequence);
            sb.append(s);
            len += 1 + s.length();
          }
          vLinks.clear();
        }
        a.getGroupBits(bsIgnore);
        bs.andNot(bsIgnore);
        int i2 = a.getOffsetResidueAtom("0", 1);
        if (i2 < 0 || !bs.get(i2)) {
          sb.append(" //* ").appendI(a.getResno()).append(" *//");       
          if (i2 < 0 && (i2 = bs.nextSetBit(i + 1)) < 0)
            break;
          if (len > 0)
            end = ".\n";
        }
        i = i2 - 1;
      }
    } catch (Exception e) {
      System.out.println(e.toString());
      return "";
    }
    if (!allowUnmatchedRings && !htRingsSequence.isEmpty()) {
      dumpRingKeys(sb, htRingsSequence);
      throw new InvalidSmilesException("//* ?ring error? *//");
    }
    s = sb.toString();
    if (s.endsWith(".\n"))
      s = s.substring(0, s.length() - 2);
    return s;
  }

  private void addBracketedBioName(SB sb, JmolNode a, String atomName) {
    sb.append("[");
    if (atomName != null) {
      char chChain = a.getChainID();
      sb.append(a.getGroup3(false));
      if (!atomName.equals(".0"))
        sb.append(atomName).append("#").appendI(a.getElementNumber());
      sb.append("//* ").appendI(
          a.getResno());
      if (chChain != '\0')
        sb.append(":").appendC(chChain);
      sb.append(" *//");
    } else {
      sb.append(Elements.elementNameFromNumber(a.getElementNumber()));
    }
    sb.append("]");
  }

  /**
   * 
   * creates a valid SMILES string from a model. TODO: stereochemistry other
   * than square planar and tetrahedral
   * 
   * @param atom
   * @param bs
   * @param allowConnectionsToOutsideWorld
   * @return SMILES
   * @throws InvalidSmilesException
   */
  private String getSmilesComponent(JmolNode atom, BS bs,
                                    boolean allowConnectionsToOutsideWorld)
      throws InvalidSmilesException {

    if (atom.getElementNumber() == 1 && atom.getEdges().length > 0)
      atom = atoms[atom.getBondedAtomIndex(0)]; // don't start with H
    bsSelected = JmolMolecule.getBranchBitSet(atoms, atom.getIndex(),
        BSUtil.copy(bs), null, -1, true, false);
    bs.andNot(bsSelected);
    bsIncludingH = BSUtil.copy(bsSelected);
    for (int j = bsSelected.nextSetBit(0); j >= 0; j = bsSelected
        .nextSetBit(j + 1)) {
      JmolNode a = atoms[j];
      if (a.getElementNumber() == 1 && a.getIsotopeNumber() == 0)
        bsSelected.clear(j);
    }
    if (bsSelected.cardinality() > 2) {
      SmilesSearch search = null;
      search = SmilesParser.getMolecule("A[=&@]A", true);
      search.jmolAtoms = atoms;
      search.setSelected(bsSelected);
      search.jmolAtomCount = atomCount;
      search.ringDataMax = 7;
      search.setRingData(null);
      bsAromatic = search.bsAromatic;
      ringSets = search.ringSets;
      setBondDirections();
    } else {
      bsAromatic = new BS();
    }
    bsToDo = BSUtil.copy(bsSelected);
    SB sb = new SB();

    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1))
      if (atoms[i].getCovalentBondCount() > 4) {
        getSmiles(sb, atoms[i], allowConnectionsToOutsideWorld, false);
        atom = null;
      }
    if (atom != null)
      while ((atom = getSmiles(sb, atom, allowConnectionsToOutsideWorld, true)) != null) {
      }
    while (bsToDo.cardinality() > 0 || !htRings.isEmpty()) {
      Iterator<Object[]> e = htRings.values().iterator();
      if (e.hasNext()) {
        atom = atoms[((Integer) e.next()[1]).intValue()];
        if (!bsToDo.get(atom.getIndex()))
          break;
      } else {
        atom = atoms[bsToDo.nextSetBit(0)];
      }
      sb.append(".");
      prevSp2Atoms = null;
      prevAtom = null;
      while ((atom = getSmiles(sb, atom, allowConnectionsToOutsideWorld, true)) != null) {
      }
    }
    if (!htRings.isEmpty()) {
      dumpRingKeys(sb, htRings);
      throw new InvalidSmilesException("//* ?ring error? *//\n" + sb);
    }
    return sb.toString();
  }

  /**
   * Retrieves the saved character based on the index of the bond.
   * bsBondsUp and bsBondsDown are global fields.
   * 
   * @param bond
   * @param atomFrom
   * @return   the correct character '/', '\\', '\0' (meaning "no stereochemistry")
   */
  private char getBondStereochemistry(JmolEdge bond, JmolNode atomFrom) {
    if (bond == null)
      return '\0';
    int i = bond.index;
    boolean isFirst = (atomFrom == null || bond.getAtomIndex1() == atomFrom
        .getIndex());
    return (bsBondsUp.get(i) ? (isFirst ? '/' : '\\')
        : bsBondsDn.get(i) ? (isFirst ? '\\' : '/') : '\0');
  }

  /**
   * Creates global BitSets bsBondsUp and bsBondsDown. Noniterative. 
   *
   */
  private void setBondDirections() {
    BS bsDone = new BS();
    JmolEdge[][] edges = new JmolEdge[2][3];
    
    // We don't assume a bond list, just an atom list, so we
    // loop through all the bonds of all the atoms, flagging them
    // as having been done already so as not to do twice. 
    // The bonds we are marking will be bits in bsBondsUp or bsBondsDn
    
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1)) {
      JmolNode atom1 = atoms[i];
      JmolEdge[] bonds = atom1.getEdges();
      for (int k = 0; k < bonds.length; k++) {
        JmolEdge bond = bonds[k];
        int index = bond.index;
        if (bsDone.get(index))
          continue;
        JmolNode atom2 = bond.getOtherAtomNode(atom1);
        if (bond.getCovalentOrder() != 2
            || SmilesSearch.isRingBond(ringSets, i, atom2.getIndex()))
          continue;
        bsDone.set(index);
        JmolEdge b0 = null;
        JmolNode a0 = null;
        int i0 = 0;
        JmolNode[] atom12 = new JmolNode[] { atom1, atom2 };
        if (Logger.debugging)
          Logger.debug(atom1 + " == " + atom2);
        int edgeCount = 1;
        
        // OK, so we have a double bond. Only looking at single bonds around it.
        
        // First pass: just see if there is an already-assigned bond direction
        // and collect the edges in an array. 
        
        for (int j = 0; j < 2 && edgeCount > 0 && edgeCount < 3; j++) {
          edgeCount = 0;
          JmolNode atomA = atom12[j];
          JmolEdge[] bb = atomA.getEdges();
          for (int b = 0; b < bb.length; b++) {
            if (bb[b].getCovalentOrder() != 1)
              continue;
            edges[j][edgeCount++] = bb[b];
            if (getBondStereochemistry(bb[b], atomA) != '\0') {
              b0 = bb[b];
              i0 = j;
            }
          }
        }
        if (edgeCount == 3 || edgeCount == 0)
          continue;
        
        // If no bond around this double bond is already marked, we assign it UP.
        
        if (b0 == null) {
          i0 = 0;
          b0 = edges[i0][0];
          bsBondsUp.set(b0.index);
        }
        
        // The character '/' or '\\' is assigned based on a
        // geometric reference to the reference bond. Initially
        // this comes in in reference to the double bond, but
        // when we save the bond, we are saving the correct 
        // character for the bond itself -- based on its 
        // "direction" from atom 1 to atom 2. Then, when 
        // creating the SMILES string, we use the atom on the 
        // left as the reference to get the correct character
        // for the string itself. The only tricky part, I think.
        // SmilesSearch.isDiaxial is just a simple method that
        // does the dot products to determine direction. In this
        // case we are looking simply for vA.vB < 0,meaning 
        // "more than 90 degrees apart" (ab, and cd)
        // Parity errors would be caught here, but I doubt you
        // could ever get that with a real molecule. 
        
        char c0 = getBondStereochemistry(b0, atom12[i0]);
        a0 = b0.getOtherAtomNode(atom12[i0]);
        if (a0 == null)
          continue;
        for (int j = 0; j < 2; j++)
          for (int jj = 0; jj < 2; jj++) {
            JmolEdge b1 = edges[j][jj];
            if (b1 == null || b1 == b0)
              continue;
            int bi = b1.index;
            JmolNode a1 = b1.getOtherAtomNode(atom12[j]);
            if (a1 == null)
              continue;
            char c1 = getBondStereochemistry(b1, atom12[j]);

            //   c1 is FROM the double bond:
            //    
            //     a0    a1
            //      \   /
            //    [i0]=[j]       /a /b  \c \d
            //   
            boolean isOpposite = SmilesSearch.isDiaxial(atom12[i0], atom12[j],
                a0, a1, vTemp, 0);
            if (c1 == '\0' || (c1 != c0) == isOpposite) {
              boolean isUp = (c0 == '\\' && isOpposite || c0 == '/'
                  && !isOpposite);
              if (isUp == (b1.getAtomIndex1() != a1.getIndex()))
                bsBondsUp.set(bi);
              else
                bsBondsDn.set(bi);
            } else {
              Logger.error("BOND STEREOCHEMISTRY ERROR");
            }
            if (Logger.debugging)
              Logger.debug(getBondStereochemistry(b0, atom12[0]) + " "
                  + a0.getIndex() + " " + a1.getIndex() + " "
                  + getBondStereochemistry(b1, atom12[j]));
          }
      }
    }
  }

  private JmolNode getSmiles(SB sb, JmolNode atom,
                             boolean allowConnectionsToOutsideWorld, boolean allowBranches) {
    int atomIndex = atom.getIndex();

    if (!bsToDo.get(atomIndex))
      return null;
    bsToDo.clear(atomIndex);
    boolean isExtension = (!bsSelected.get(atomIndex));
    int prevIndex = (prevAtom == null ? -1 : prevAtom.getIndex());
    boolean isAromatic = bsAromatic.get(atomIndex);
    // prevSp2Atoms is for allene ABC=C=CDE
    boolean havePreviousSp2Atoms = (prevSp2Atoms != null);
    JmolNode[] sp2Atoms = prevSp2Atoms;
    int nSp2Atoms = 0;
    int atomicNumber = atom.getElementNumber();
    int nH = 0;
    JmolList<JmolEdge> v = new  JmolList<JmolEdge>();
    JmolEdge bond0 = null;
    JmolEdge bondPrev = null;
    JmolEdge[] bonds = atom.getEdges();
    JmolNode aH = null;
    int stereoFlag = (isAromatic ? 10 : 0);
    JmolNode[] stereo = new JmolNode[7];
    if (Logger.debugging)
      Logger.debug(sb.toString());

    // first look through the bonds for the best 
    // continuation -- bond0 -- and count hydrogens
    // and create a list of bonds to process.

    if (bonds != null)
      for (int i = bonds.length; --i >= 0;) {
        JmolEdge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        JmolNode atom1 = bonds[i].getOtherAtomNode(atom);
        int index1 = atom1.getIndex();
        if (index1 == prevIndex) {
          bondPrev = bonds[i];
          continue;
        }
        boolean isH = (atom1.getElementNumber() == 1 && atom1
            .getIsotopeNumber() == 0);
        if (!bsIncludingH.get(index1)) {
          if (!isH && allowConnectionsToOutsideWorld
              && bsSelected.get(atomIndex))
            bsToDo.set(index1);
          else
            continue;
        }
        if (isH) {
          aH = atom1;
          nH++;
          if (nH > 1)
            stereoFlag = 10;
        } else {
          v.addLast(bonds[i]);
        }
      }

    // order of listing is critical for stereochemistry:
    //
    // 1) previous atom
    // 2) bond to previous atom
    // 3) atom symbol
    // 4) hydrogen atoms
    // 5) branches
    // 6) rings

    // add the bond to the previous atom

    //System.out.println(" " + atom);

    String strBond = null;
    if (sp2Atoms == null)
      sp2Atoms = new JmolNode[5];
    if (bondPrev != null) {
      strBond = SmilesBond.getBondOrderString(bondPrev.getCovalentOrder());
      if (prevSp2Atoms == null)
        sp2Atoms[nSp2Atoms++] = prevAtom;
      else
        nSp2Atoms = 2;
    }
    nSp2Atoms += nH;

    // get bond0 
    int nMax = 0;
    BS bsBranches = new BS();
    if (allowBranches)
      for (int i = 0; i < v.size(); i++) {
        JmolEdge bond = v.get(i);
        JmolNode a = bond.getOtherAtomNode(atom);
        int n = a.getCovalentBondCount() - a.getCovalentHydrogenCount();
        int order = bond.getCovalentOrder();
        if (order == 1 && n == 1 && i < v.size() - (bond0 == null ? 1 : 0)) {
          bsBranches.set(bond.index);
        } else if ((order > 1 || n > nMax)
            && !htRings.containsKey(getRingKey(a.getIndex(), atomIndex))) {
          nMax = (order > 1 ? 1000 + order : n);
          bond0 = bond;
        }
      }
    JmolNode atomNext = (bond0 == null ? null : bond0.getOtherAtomNode(atom));
    int orderNext = (bond0 == null ? 0 : bond0.getCovalentOrder());

    if (stereoFlag < 7 && bondPrev != null) {
      if (bondPrev.getCovalentOrder() == 2 && orderNext == 2
          && prevSp2Atoms != null && prevSp2Atoms[1] != null) {
        // allene continuation
        stereo[stereoFlag++] = prevSp2Atoms[0];
        stereo[stereoFlag++] = prevSp2Atoms[1];
      } else {
        stereo[stereoFlag++] = prevAtom;
      }
    }

    if (stereoFlag < 7 && nH == 1)
      stereo[stereoFlag++] = aH;

    boolean deferStereo = (orderNext == 1 && prevSp2Atoms == null);
    char chBond = getBondStereochemistry(bondPrev, prevAtom);

    // now construct the branches part

    SB sMore = new SB();
    for (int i = 0; i < v.size(); i++) {
      JmolEdge bond = v.get(i);
      if (!bsBranches.get(bond.index))
        continue;
      JmolNode a = bond.getOtherAtomNode(atom);
      SB s2 = new SB();
      s2.append("(");
      prevAtom = atom;
      prevSp2Atoms = null;
      JmolEdge bond0t = bond0;
      getSmiles(s2, a, allowConnectionsToOutsideWorld, allowBranches);
      bond0 = bond0t;
      s2.append(")");
      if (sMore.indexOf(s2.toString()) >= 0)
        stereoFlag = 10;
      sMore.appendSB(s2);
      v.remove(i--);
      if (stereoFlag < 7)
        stereo[stereoFlag++] = a;
      if (nSp2Atoms < 5)
        sp2Atoms[nSp2Atoms++] = a;
    }

    // from here on, prevBondAtoms and prevAtom must not be used.    

    // process the bond to the next atom
    // and cancel any double bond stereochemistry if nec.

    int index2 = (orderNext == 2 ? atomNext.getIndex() : -1);
    if (nH > 1 || isAromatic || index2 < 0
        || SmilesSearch.isRingBond(ringSets, atomIndex, index2)) {
      nSp2Atoms = -1;
    }
    if (nSp2Atoms < 0)
      sp2Atoms = null;

    // output section

    if (strBond != null || chBond != '\0') {
      if (chBond != '\0')
        strBond = "" + chBond;
      sb.append(strBond);
    }

    // now process any rings

    String atat = null;
    if (!allowBranches && (v.size() == 5 || v.size() == 6))
      atat = sortInorganic(atom, v);
    for (int i = 0; i < v.size(); i++) {
      JmolEdge bond = v.get(i);
      if (bond == bond0)
        continue;
      JmolNode a = bond.getOtherAtomNode(atom);
      String s = getRingCache(atomIndex, a.getIndex(), htRings);
      strBond = SmilesBond.getBondOrderString(bond.order);
      if (!deferStereo) {
        chBond = getBondStereochemistry(bond, atom);
        if (chBond != '\0')
          strBond = "" + chBond;
      }

      sMore.append(strBond);
      sMore.append(s);
      if (stereoFlag < 7)
        stereo[stereoFlag++] = a;
      if (sp2Atoms != null && nSp2Atoms < 5)
        sp2Atoms[nSp2Atoms++] = a;
    }

    // now the atom symbol or bracketed expression
    // we allow for charge, hydrogen count, isotope number,
    // and stereochemistry 

    if (havePreviousSp2Atoms && stereoFlag == 2 && orderNext == 2
        && atomNext.getCovalentBondCount() == 3) {
      // this is for allenes only, not cumulenes
      bonds = atomNext.getEdges();
      for (int k = 0; k < bonds.length; k++) {
        if (bonds[k].isCovalent()
            && atomNext.getBondedAtomIndex(k) != atomIndex)
          stereo[stereoFlag++] = atoms[atomNext.getBondedAtomIndex(k)];
      }
      nSp2Atoms = 0;
    } else if (atomNext != null && stereoFlag < 7) {
      stereo[stereoFlag++] = atomNext;
    }
    int valence = atom.getValence();
    int charge = atom.getFormalCharge();
    int isotope = atom.getIsotopeNumber();
    String atomName = atom.getAtomName();
    String groupType = atom.getBioStructureTypeName();
    // for bioSMARTS we provide the connecting atom if 
    // present. For example, in 1BLU we have 
    // .[CYS.SG#16] could match either the atom number or the element number 
    if (Logger.debugging)
      sb.append("\n//* " + atom + " *//\t");
    if (isExtension && groupType.length() != 0 && atomName.length() != 0)
      addBracketedBioName(sb, atom, "." + atomName);
    else
      sb.append(SmilesAtom
          .getAtomLabel(atomicNumber, isotope, valence, charge, nH, isAromatic,
              atat != null ? atat : checkStereoPairs(atom, atomIndex, stereo, stereoFlag)));
    sb.appendSB(sMore);

    // check the next bond

    if (bond0 == null)
      return null;

    if (orderNext == 2 && (nSp2Atoms == 1 || nSp2Atoms == 2)) {
      if (sp2Atoms[0] == null)
        sp2Atoms[0] = atom; // CN=C= , for example. close enough!
      if (sp2Atoms[1] == null)
        sp2Atoms[1] = atom; // .C3=C=
    } else {
      sp2Atoms = null;
      nSp2Atoms = 0;
    }

    // prevSp2Atoms is only so that we can track
    // ABC=C=CDE  systems

    prevSp2Atoms = sp2Atoms;
    prevAtom = atom;
    return atomNext;
  }

  /**
   * We must sort the bond vector such that a diaxial pair is
   * first and last. Then we assign stereochemistry based on what
   * is left. The assignment is not made if there are no diaxial groups
   * or with octahedral if there are fewer than three or trigonal bipyramidal
   * with no axial ligands.
   * 
   * @param atom
   * @param v
   * @return  "@" or "@@" or ""
   */
  private String sortInorganic(JmolNode atom, JmolList<JmolEdge> v) {
    int atomIndex = atom.getIndex();
    int n = v.size();
    JmolList<JmolEdge[]> axialPairs = new  JmolList<JmolEdge[]>();
    JmolList<JmolEdge> bonds = new  JmolList<JmolEdge>();
    JmolNode a1, a2;
    JmolEdge bond1, bond2;
    BS bsDone = new BS();
    JmolEdge[] pair0 = null;
    JmolNode[] stereo = new JmolNode[6];
    boolean isOK = true; // AX6 or AX5
    String s = "";
    for (int i = 0; i < n; i++) {
      bond1 = v.get(i);
      stereo[0] = a1 = bond1.getOtherAtomNode(atom);
      if (i == 0)
        s = addStereoCheck(atomIndex, stereo, 0, "");
      else if (isOK && addStereoCheck(atomIndex, stereo, 0, s) != null)
        isOK = false;
      if (bsDone.get(i))
        continue;
      bsDone.set(i);
      boolean isAxial = false;
      for (int j = i + 1; j < n; j++) {
        if (bsDone.get(j))
          continue;
        bond2 = v.get(j);
        a2 = bond2.getOtherAtomNode(atom);
        if (SmilesSearch.isDiaxial(atom, atom, a1, a2, vTemp, -0.95f)) {
          axialPairs.addLast(new JmolEdge[] { bond1, bond2 });
          isAxial = true;
          bsDone.set(j);
          break;
        }
      }
      if (!isAxial)
        bonds.addLast(bond1);
    }
    int nPairs = axialPairs.size();

    // AX6 or AX5 are fine as is
    // can't proceed if octahedral and not all axial pairs
    // or trigonal bipyramidal and no axial pair.
    
    if (isOK || n == 6 && nPairs != 3 || n == 5 && nPairs == 0)
      return "";
    pair0 = axialPairs.get(0);
    bond1 = pair0[0];
    stereo[0] = bond1.getOtherAtomNode(atom);
    
    // now sort them into the ligand vector in the proper order
    
    v.clear();
    v.addLast(bond1);
    if (nPairs > 1)
      bonds.addLast(axialPairs.get(1)[0]);
    if (nPairs == 3)
      bonds.addLast(axialPairs.get(2)[0]);
    if (nPairs > 1)
      bonds.addLast(axialPairs.get(1)[1]);
    if (nPairs == 3)
      bonds.addLast(axialPairs.get(2)[1]);
    for (int i = 0; i < bonds.size(); i++) {
      bond1 = bonds.get(i);
      v.addLast(bond1);
      stereo[i + 1] = bond1.getOtherAtomNode(atom);
    }
    v.addLast(pair0[1]);
    
    // now deterimine the stereochemistry
    
    return getStereoFlag(atom, stereo, n, vTemp);
  }

  private String checkStereoPairs(JmolNode atom, int atomIndex,
                                  JmolNode[] stereo, int stereoFlag) {
    if (stereoFlag < 4)
      return "";
    if (stereoFlag == 4 && (atom.getElementNumber()) == 6) {
      // do a quick check for two of the same group.
      String s = "";
      for (int i = 0; i < 4; i++)
        if ((s = addStereoCheck(atomIndex, stereo, i, s)) == null) {
          stereoFlag = 10;
          break;
        }
    }
    return (stereoFlag > 6 ? "" : getStereoFlag(atom, stereo,
        stereoFlag, vTemp));
  }

  /**
   * 
   * @param atom0
   * @param atoms
   * @param nAtoms
   * @param v
   * @return        String
   */
  private static String getStereoFlag(JmolNode atom0, JmolNode[] atoms, int nAtoms, VTemp v) {
    JmolNode atom1 = atoms[0];
    JmolNode atom2 = atoms[1];
    JmolNode atom3 = atoms[2];
    JmolNode atom4 = atoms[3];
    JmolNode atom5 = atoms[4];
    JmolNode atom6 = atoms[5];
    int chiralClass = SmilesAtom.STEREOCHEMISTRY_TETRAHEDRAL;
    switch (nAtoms) {
    default:
    case 5:
    case 6:
      // like tetrahedral
      return (SmilesSearch.checkStereochemistry(false, atom0, chiralClass, 1, atom1, atom2, atom3, atom4, atom5, atom6, v)? "@" : "@@");
    case 2: // allene
    case 4: // tetrahedral, square planar
      if (atom3 == null || atom4 == null)
        return "";
      float d = SmilesAromatic.getNormalThroughPoints(atom1, atom2, atom3, v.vTemp, v.vA, v.vB);
      if (Math.abs(SmilesSearch.distanceToPlane(v.vTemp, d, (P3) atom4)) < 0.2f) {
        chiralClass = SmilesAtom.STEREOCHEMISTRY_SQUARE_PLANAR;
        if (SmilesSearch.checkStereochemistry(false, atom0, chiralClass, 1, atom1, atom2, atom3, atom4, atom5, atom6, v))
          return "@SP1";
        if (SmilesSearch.checkStereochemistry(false, atom0, chiralClass, 2, atom1, atom2, atom3, atom4, atom5, atom6, v))
          return "@SP2";
        if (SmilesSearch.checkStereochemistry(false, atom0, chiralClass, 3, atom1, atom2, atom3, atom4, atom5, atom6, v))
          return "@SP3";       
      } else {
        return (SmilesSearch.checkStereochemistry(false, atom0, chiralClass, 1, atom1, atom2, atom3, atom4, atom5, atom6, v)? "@" : "@@");
      }       
    }
    return "";
  }

  /**
   * checks a group and either adds a new group to the growing
   * check string or returns null
   * @param atomIndex
   * @param stereo
   * @param i
   * @param s
   * @return   null if duplicate
   */
  private String addStereoCheck(int atomIndex, JmolNode[] stereo, int i, String s) {
    int n = stereo[i].getAtomicAndIsotopeNumber();
    int nx = stereo[i].getCovalentBondCount();
    int nh = (n == 6 ? stereo[i].getCovalentHydrogenCount() : 0);
    // only carbon or singly-connected atoms are checked
    // for C we use nh -- CH3, for example.
    // for other atoms, we use number of bonds.
    // just checking for tetrahedral CH3)
    if (n == 6 ? nx != 4 || nh != 3 : nx > 1)
      return s;
    String sa = ";" + n + "/" + nh + "/" + nx + ",";
    if (s.indexOf(sa) >= 0) {
      if (nh == 3) {
        // must check isotopes for CH3
        int ndt = 0;
        for (int j = 0; j < nx && ndt < 3; j++) {
          int ia = stereo[i].getBondedAtomIndex(j);
          if (ia == atomIndex)
            continue;
          ndt += atoms[ia].getAtomicAndIsotopeNumber();
        }
        if (ndt > 3)
          return s;
      }
      return null;
    }
    return s + sa;
  }

  private String getRingCache(int i0, int i1, Map<String, Object[]> ht) {
    String key = getRingKey(i0, i1);
    Object[] o = ht.get(key);
    String s = (o == null ? null : (String) o[0]);
    if (s == null) {
      ht.put(key, new Object[] {
          s = SmilesParser.getRingPointer(++nPairs), Integer.valueOf(i1) });
      if (Logger.debugging)
        Logger.info("adding for " + i0 + " ring key " + nPairs + ": " + key);
    } else {
      ht.remove(key);
      if (Logger.debugging)
        Logger.info("using ring key " + key);
    }
    return s;//  + " _" + key + "_ \n";
  }

  private void dumpRingKeys(SB sb, Map<String, Object[]> ht) {
    Logger.info(sb.toString() + "\n\n");
    Iterator<String> e = ht.keySet().iterator();
    while (e.hasNext()) {
      Logger.info("unmatched ring key: " + e.next());
    }
  }

  protected static String getRingKey(int i0, int i1) {
    return Math.min(i0, i1) + "_" + Math.max(i0, i1);
  }

}
