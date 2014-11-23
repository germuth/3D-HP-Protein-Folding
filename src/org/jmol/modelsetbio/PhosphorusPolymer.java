/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.modelsetbio;


import org.jmol.io.OutputStringBuilder;
import org.jmol.modelset.LabelToken;
import org.jmol.util.BS;
import org.jmol.util.SB;
import org.jmol.viewer.Viewer;



class PhosphorusPolymer extends BioPolymer {

  PhosphorusPolymer(Monomer[] monomers) {
    super(monomers);
  }
  
  @Override
  public void getPdbData(Viewer viewer, char ctype, char qtype, int mStep, int derivType,
                         BS bsAtoms, BS bsSelected, 
                         boolean bothEnds, boolean isDraw, 
                         boolean addHeader, LabelToken[] tokens, OutputStringBuilder pdbATOM, SB pdbCONECT, BS bsWritten) {
    getPdbData(viewer, this, ctype, qtype, mStep, derivType, bsAtoms, bsSelected, bothEnds, 
        isDraw, addHeader, tokens, pdbATOM, pdbCONECT, bsWritten);
  }   

}
