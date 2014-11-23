package org.jmol.io2;

import org.jmol.api.ZInputStream;

import java.io.InputStream;
import java.util.zip.ZipInputStream;
public class JmolZipInputStream extends ZipInputStream implements ZInputStream {
  
  public JmolZipInputStream(InputStream in) {
    super(in);
  }
  
}