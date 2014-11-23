/* $RCSfile$
 * $Author$
 * $Date$
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
package org.jmol.bspt;


import org.jmol.modelset.Atom;
import org.jmol.util.Escape;
import org.jmol.util.P3;
import org.jmol.util.SB;

/**
 * A leaf of Point3f objects in the bsp tree
 *
 * @author Miguel, miguel@jmol.org
 */
class Leaf extends Element {
  P3[] tuples;
    
  Leaf(Bspt bspt, Leaf leaf, int countToKeep) {
    this.bspt = bspt;
    count = 0;
    tuples = new P3[Bspt.leafCountMax];
    if (leaf == null)
      return;
    for (int i = countToKeep; i < Bspt.leafCountMax; ++i) {
      tuples[count++] = leaf.tuples[i];
      leaf.tuples[i] = null;
    }
    leaf.count = countToKeep;
  }

  void sort(int dim) {
    for (int i = count; --i > 0; ) { // this is > not >=
      P3 champion = tuples[i];
      float championValue = Node.getDimensionValue(champion, dim);
      for (int j = i; --j >= 0; ) {
        P3 challenger = tuples[j];
        float challengerValue = Node.getDimensionValue(challenger, dim);
        if (challengerValue > championValue) {
          tuples[i] = challenger;
          tuples[j] = champion;
          champion = challenger;
          championValue = challengerValue;
        }
      }
    }
  }

  @Override
  Element addTuple(int level, P3 tuple) {
    if (count < Bspt.leafCountMax) {
      tuples[count++] = tuple;
      return this;
    }
    Node node = new Node(bspt, level, this);
    return node.addTuple(level, tuple);
  }
    
 
  @Override
  void dump(int level, SB sb) {
    for (int i = 0; i < count; ++i) {
      P3 t = tuples[i];
      for (int j = 0; j < level; ++j)
        sb.append(".");
      sb.append(Escape.e(t)).append("Leaf ").appendI(i).append(": ").append(((Atom) t).getInfo());
    }
  }

  @Override
  public String toString() {
    return "leaf:" + count + "\n";
  }
 

}
