/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolDocument;
import org.jmol.api.ZInputStream;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public class FileReader {
  /**
   * 
   */
  private final FileManager fm;
  private final Viewer viewer;
  private String fileNameIn;
  private String fullPathNameIn;
  private String nameAsGivenIn;
  private String fileTypeIn;
  private Object atomSetCollection;
  private Object reader;
  private Map<String, Object> htParams;
  private boolean isAppend;
  private byte[] bytes;

  public FileReader(FileManager fileManager, Viewer viewer, String fileName, String fullPathName, String nameAsGiven,
      String type, Object reader, Map<String, Object> htParams,
      boolean isAppend) {
    fm = fileManager;
    this.viewer = viewer;
    fileNameIn = fileName;
    fullPathNameIn = fullPathName;
    nameAsGivenIn = nameAsGiven;
    fileTypeIn = type;
    this.reader = (reader instanceof BufferedReader ? reader : reader instanceof Reader ? new BufferedReader((Reader) reader) : null);
    this.bytes = (Escape.isAB(reader) ? (byte[]) reader : null);
    this.htParams = htParams;
    this.isAppend = isAppend;
  }

  public void run() {

    if (!isAppend && viewer.displayLoadErrors)
      viewer.zap(false, true, false);

    String errorMessage = null;
    Object t = null;
    if (reader == null) {
      if (fileTypeIn == null)
        fileTypeIn = JmolBinary.getBinaryType(fullPathNameIn);
      boolean isBinary = JmolBinary.checkBinaryType(fileTypeIn);
      t = fm.getUnzippedBufferedReaderOrErrorMessageFromName(fullPathNameIn,
          bytes, true, isBinary, false, true);
      if (t == null || t instanceof String) {
        errorMessage = (t == null ? "error opening:" + nameAsGivenIn
            : (String) t);
        if (!errorMessage.startsWith("NOTE:"))
          Logger.error("file ERROR: " + fullPathNameIn + "\n" + errorMessage);
        atomSetCollection = errorMessage;
        return;
      }
      if (t instanceof BufferedReader) {
        reader = t;
      } else if (t instanceof ZInputStream) {
        String name = fullPathNameIn;
        isBinary = (JmolBinary.getBinaryType(name) != null);
        String[] subFileList = null;
        if (name.indexOf("|") >= 0 && !name.endsWith(".zip")) {
          subFileList = TextFormat.splitChars(name, "|");
          name = subFileList[0];
        }
        if (subFileList != null)
          htParams.put("subFileList", subFileList);
        InputStream zis = (InputStream) t;
        String[] zipDirectory = fm.getZipDirectory(name, true);
        atomSetCollection = t = JmolBinary
            .getAtomSetCollectionOrBufferedReaderFromZip(viewer
                .getModelAdapter(), zis, name, zipDirectory, htParams, false,
                isBinary);
        try {
          zis.close();
        } catch (Exception e) {
          //
        }
      }
    }
    if (t instanceof BufferedInputStream) {
      JmolDocument bd = (JmolDocument) Interface
          .getOptionInterface("io2.BinaryDocument");
      bd.setStream((BufferedInputStream) t, true);
      reader = bd;
    }
    if (reader != null) {
      atomSetCollection = viewer.getModelAdapter().getAtomSetCollectionReader(
          fullPathNameIn, fileTypeIn, reader, htParams);
      if (!(atomSetCollection instanceof String))
        atomSetCollection = viewer.getModelAdapter().getAtomSetCollection(
            atomSetCollection);
      try {
        if (reader instanceof BufferedReader)
          ((BufferedReader) reader).close();
        else if (reader instanceof JmolDocument)
          ((JmolDocument) reader).close();
      } catch (IOException e) {
        // ignore
      }
    }

    if (atomSetCollection instanceof String)
      return;

    if (!isAppend && !viewer.displayLoadErrors)
      viewer.zap(false, true, false);

    fm.fullPathName = fullPathNameIn;
    fm.nameAsGiven = nameAsGivenIn;
    fm.fileName = fileNameIn;

  }
  
  public Object getAtomSetCollection() {
    return atomSetCollection;
  }


}