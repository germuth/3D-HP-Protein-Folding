package org.jmol.io2;

import java.io.IOException;


import org.jmol.io.DataReader;
import org.jmol.util.JmolList;



/**
 * 
 * VectorDataReader subclasses BufferedReader and overrides its
 * read, readLine, mark, and reset methods so that JmolAdapter 
 * works with Vector<String> arrays without any further adaptation. 
 * 
 */

public class ListDataReader extends DataReader {
  private JmolList<String> data;
  private int pt;
  private int len;

  public ListDataReader() {
    super();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public DataReader setData(Object data) {
    this.data = (JmolList<String>) data;
    len = this.data.size();
    return this;
  }

  @Override
  public int read(char[] buf, int off, int len) throws IOException {
    return readBuf(buf, off, len);
  }

  @Override
  public String readLine() {
    return (pt < len ? data.get(pt++) : null);
  }

  /**
   * 
   * @param ptr
   */
  public void mark(long ptr) {
    //ignore ptr.
    ptMark = pt;
  }

  @Override
  public void reset() {
    pt = ptMark;
  }
}