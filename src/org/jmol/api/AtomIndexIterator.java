package org.jmol.api;



import org.jmol.atomdata.RadiusData;
import org.jmol.modelset.ModelCollection;
import org.jmol.util.BS;
import org.jmol.util.P3;


/**
 * note: YOU MUST RELEASE THE ITERATOR
 */
public interface AtomIndexIterator {
  /**
   * @param modelSet 
   * @param modelIndex
   * @param zeroBase    an offset used in the AtomIteratorWithinSet only
   * @param atomIndex
   * @param center
   * @param distance
   * @param rd 
   */
  public void setModel(ModelCollection modelSet, int modelIndex, int zeroBase, int atomIndex, P3 center, float distance, RadiusData rd);
  public void setCenter(P3 center, float distance);
  public void addAtoms(BS bsResult);
  public boolean hasNext();
  public int next();
  public float foundDistance2();
  public void release();
}
