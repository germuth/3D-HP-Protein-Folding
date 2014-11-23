package org.jmol.api;



import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.JmolNode;

public interface SmilesMatcherInterface {

  public abstract String getLastException();

  public int areEqual(String smiles1, String smiles2);

  public abstract BS[] find(String pattern,/* ...in... */String smiles,
                                boolean isSmarts, boolean firstMatchOnly);

  public abstract BS getSubstructureSet(String pattern, JmolNode[] atoms,
                                            int atomCount, BS bsSelected,
                                            boolean isSmarts,
                                            boolean firstMatchOnly);

  public abstract BS[] getSubstructureSetArray(String pattern,
                                                   JmolNode[] atoms,
                                                   int atomCount,
                                                   BS bsSelected,
                                                   BS bsAromatic,
                                                   boolean isSmarts,
                                                   boolean firstMatchOnly);

  public abstract int[][] getCorrelationMaps(String pattern, JmolNode[] atoms,
                                             int atomCount, BS bsSelected,
                                             boolean isSmarts,
                                             boolean firstMatchOnly);

  public abstract String getMolecularFormula(String pattern, boolean isSearch);

  public abstract String getSmiles(JmolNode[] atoms, int atomCount,
                                   BS bsSelected, boolean asBioSmiles,
                                   boolean allowUnmatchedRings, boolean addCrossLinks, String comment);

  public abstract String getRelationship(String smiles1, String smiles2);

  public abstract String reverseChirality(String smiles);

  public abstract void getSubstructureSets(String[] smarts, JmolNode[] atoms, int atomCount,
                                           int flags,
                         BS bsSelected, JmolList<BS> bitSets, JmolList<BS>[] vRings);
}
