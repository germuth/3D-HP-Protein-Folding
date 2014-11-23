/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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
package org.jmol.viewer;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.Viewer.ACCESS;

import org.jmol.api.JmolDocument;
import org.jmol.api.JmolDomReaderInterface;
import org.jmol.api.JmolFileAdapterInterface;
import org.jmol.api.JmolFileInterface;
import org.jmol.api.JmolFilesReaderInterface;
import org.jmol.api.JmolViewer;
import org.jmol.api.ApiPlatform;
import org.jmol.api.ZInputStream;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.jmol.util.JmolList;
import java.util.Hashtable;
import java.util.List;

import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.io.Base64;
import org.jmol.io.DataReader;
import org.jmol.io.FileReader;
import org.jmol.io.JmolBinary;


// updated 

public class FileManager {

  private Viewer viewer;

  FileManager(Viewer viewer) {
    this.viewer = viewer;
    clear();
  }

  void clear() {
    fullPathName = fileName = nameAsGiven = viewer.getZapName();
    spardirCache = null;
  }

  private void setLoadState(Map<String, Object> htParams) {
    if (viewer.getPreserveState()) {
      htParams.put("loadState", viewer.getLoadState(htParams));
    }
  }

  private String pathForAllFiles = "";
  
  String getPathForAllFiles() {
    return pathForAllFiles;
  }
  
  String setPathForAllFiles(String value) {
    if (value.length() > 0 && !value.endsWith("/") && !value.endsWith("|"))
        value += "/";
    return pathForAllFiles = value;
  }

  public String nameAsGiven = "zapped";
  public String fullPathName;
  public String fileName;

  void setFileInfo(String[] fileInfo) {
    // used by ScriptEvaluator dataFrame and load methods to temporarily save the state here
    fullPathName = fileInfo[0];
    fileName = fileInfo[1];
    nameAsGiven = fileInfo[2];
  }

  String[] getFileInfo() {
    // used by ScriptEvaluator dataFrame method
    return new String[] { fullPathName, fileName, nameAsGiven };
  }

  String getFullPathName() {
    return fullPathName != null ? fullPathName : nameAsGiven;
  }

  String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  // for applet proxy
  private URL appletDocumentBaseURL = null;
  private String appletProxy;

  String getAppletDocumentBase() {
    return (appletDocumentBaseURL == null ? "" : appletDocumentBaseURL.toString());
  }

  void setAppletContext(String documentBase) {
    try {
      
      appletDocumentBaseURL = (documentBase.length() == 0 ? null : new URL((URL) null, documentBase, null));
    } catch (MalformedURLException e) {
      // never mind
    }
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy == null || appletProxy.length() == 0 ? null
        : appletProxy);
  }

  String getFileTypeName(String fileName) {
    int pt = fileName.indexOf("::");
    if (pt >= 0)
      return fileName.substring(0, pt);
    if (fileName.startsWith("="))
      return "pdb";
    Object br = getUnzippedBufferedReaderOrErrorMessageFromName(fileName, null,
        true, false, true, true);
    if (br instanceof BufferedReader)
      return viewer.getModelAdapter().getFileTypeName(br);
    if (br instanceof ZInputStream) {
      String zipDirectory = getZipDirectoryAsString(fileName);
      if (zipDirectory.indexOf("JmolManifest") >= 0)
        return "Jmol";
      return viewer.getModelAdapter().getFileTypeName(
          JmolBinary.getBufferedReaderForString(zipDirectory));
    }
    if (Escape.isAS(br)) {
      return ((String[]) br)[0];
    }
    return null;
  }

  private String getZipDirectoryAsString(String fileName) {
    Object t = getBufferedInputStreamOrErrorMessageFromName(
        fileName, fileName, false, false, null, false);
    return JmolBinary.getZipDirectoryAsStringAndClose((BufferedInputStream) t);
  }

  /////////////// createAtomSetCollectionFromXXX methods /////////////////

  // where XXX = File, Files, String, Strings, ArrayData, DOM, Reader

  /*
   * note -- createAtomSetCollectionFromXXX methods
   * were "openXXX" before refactoring 11/29/2008 -- BH
   * 
   * The problem was that while they did open the file, they
   * (mostly) also closed them, and this was confusing.
   * 
   * The term "clientFile" was replaced by "atomSetCollection"
   * here because that's what it is --- an AtomSetCollection,
   * not a file. The file is closed at this point. What is
   * returned is the atomSetCollection object.
   * 
   * One could say this is just semantics, but there were
   * subtle bugs here, where readers were not always being 
   * closed explicitly. In the process of identifying Out of
   * Memory Errors, I felt it was necessary to clarify all this.
   * 
   * Apologies to those who feel the original clientFile notation
   * was more generalizable or understandable. 
   * 
   */
  Object createAtomSetCollectionFromFile(String name,
                                         Map<String, Object> htParams,
                                         boolean isAppend) {
    if (htParams.get("atomDataOnly") == null) {
      setLoadState(htParams);
    }
    name = viewer.resolveDatabaseFormat(name);
    int pt = name.indexOf("::");
    String nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    String fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.getAtomSetCollectionFromFile(" + nameAsGiven
        + ")" + (name.equals(nameAsGiven) ? "" : " //" + name));
    String[] names = classifyName(nameAsGiven, true);
    if (names.length == 1)
      return names[0];
    String fullPathName = names[0];
    String fileName = names[1];
    htParams.put("fullPathName", (fileType == null ? "" : fileType + "::")
        + fullPathName.replace('\\', '/'));
    if (viewer.getMessageStyleChime() && viewer.getDebugScript())
      viewer.scriptStatus("Requesting " + fullPathName);
    FileReader fileReader = new FileReader(this, viewer, fileName, fullPathName, nameAsGiven,
        fileType, null, htParams, isAppend);
    fileReader.run();
    return fileReader.getAtomSetCollection();
  }

  Object createAtomSetCollectionFromFiles(String[] fileNames,
                                          Map<String, Object> htParams,
                                          boolean isAppend) {
    setLoadState(htParams);
    String[] fullPathNames = new String[fileNames.length];
    String[] namesAsGiven = new String[fileNames.length];
    String[] fileTypes = new String[fileNames.length];
    for (int i = 0; i < fileNames.length; i++) {
      int pt = fileNames[i].indexOf("::");
      String nameAsGiven = (pt >= 0 ? fileNames[i].substring(pt + 2)
          : fileNames[i]);
      String fileType = (pt >= 0 ? fileNames[i].substring(0, pt) : null);
      String[] names = classifyName(nameAsGiven, true);
      if (names.length == 1)
        return names[0];
      fullPathNames[i] = names[0];
      fileNames[i] = names[0].replace('\\', '/');
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }
    htParams.put("fullPathNames", fullPathNames);
    htParams.put("fileTypes", fileTypes);
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, namesAsGiven,
        fileTypes, null, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  Object createAtomSetCollectionFromString(String strModel,
                                           SB loadScript,
                                           Map<String, Object> htParams,
                                           boolean isAppend,
                                           boolean isLoadVariable) {
    if (!isLoadVariable)
      DataManager.getInlineData(loadScript, strModel, isAppend, viewer
          .getDefaultLoadFilter());
    setLoadState(htParams);
    boolean isAddH = (strModel.indexOf(JC.ADD_HYDROGEN_TITLE) >= 0);
    String[] fnames = (isAddH ? getFileInfo() : null);
    FileReader fileReader = new FileReader(this, viewer, "string", "string", "string", null,
        JmolBinary.getBufferedReaderForString(strModel), htParams, isAppend);
    fileReader.run();
    if (fnames != null)
      setFileInfo(fnames);
    if (!isAppend && !(fileReader.getAtomSetCollection() instanceof String)) {
      viewer.zap(false, true, false);
      fullPathName = fileName = (strModel == JC.MODELKIT_ZAP_STRING ? JC.MODELKIT_ZAP_TITLE
          : "string");
    }
    return fileReader.getAtomSetCollection();
  }

  Object createAtomSeCollectionFromStrings(String[] arrayModels,
                                           SB loadScript,
                                           Map<String, Object> htParams,
                                           boolean isAppend) {
    if (!htParams.containsKey("isData")) {
      String oldSep = "\"" + viewer.getDataSeparator() + "\"";
      String tag = "\"" + (isAppend ? "append" : "model") + " inline\"";
      SB sb = new SB();
      sb.append("set dataSeparator \"~~~next file~~~\";\ndata ").append(tag);
      for (int i = 0; i < arrayModels.length; i++) {
        if (i > 0)
          sb.append("~~~next file~~~");
        sb.append(arrayModels[i]);
      }
      sb.append("end ").append(tag).append(";set dataSeparator ")
          .append(oldSep);
      loadScript.appendSB(sb);
    }
    setLoadState(htParams);
    Logger.info("FileManager.getAtomSetCollectionFromStrings(string[])");
    String[] fullPathNames = new String[arrayModels.length];
    DataReader[] readers = new DataReader[arrayModels.length];
    for (int i = 0; i < arrayModels.length; i++) {
      fullPathNames[i] = "string[" + i + "]";
      readers[i] = newDataReader(arrayModels[i]);
    }
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, fullPathNames,
        null, readers, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  Object createAtomSeCollectionFromArrayData(List<Object> arrayData,
                                             Map<String, Object> htParams,
                                             boolean isAppend) {
    // NO STATE SCRIPT -- HERE WE ARE TRYING TO CONSERVE SPACE
    Logger.info("FileManager.getAtomSetCollectionFromArrayData(Vector)");
    int nModels = arrayData.size();
    String[] fullPathNames = new String[nModels];
    DataReader[] readers = new DataReader[nModels];
    for (int i = 0; i < nModels; i++) {
      fullPathNames[i] = "String[" + i + "]";
      readers[i] = newDataReader(arrayData.get(i));
    }
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, fullPathNames,
        null, readers, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  private JmolFilesReaderInterface newFilesReader(String[] fullPathNames,
                                                  String[] namesAsGiven,
                                                  String[] fileTypes,
                                                  DataReader[] readers,
                                                  Map<String, Object> htParams,
                                                  boolean isAppend) {
    JmolFilesReaderInterface fr = (JmolFilesReaderInterface) Interface
        .getOptionInterface("io2.FilesReader");
    fr.set(this, viewer, fullPathNames, namesAsGiven, fileTypes, readers, htParams,
        isAppend);
    return fr;
  }

  private DataReader newDataReader(Object data) {
    String reader = (data instanceof String ? "String"
        : Escape.isAS(data) ? "Array" 
        : data instanceof JmolList<?> ? "List" : null);
    if (reader == null)
      return null;
    DataReader dr = (DataReader) Interface.getOptionInterface("io2." + reader + "DataReader");
    return dr.setData(data);
  }

  Object createAtomSetCollectionFromDOM(Object DOMNode,
                                        Map<String, Object> htParams) {
    JmolDomReaderInterface aDOMReader = (JmolDomReaderInterface) Interface.getOptionInterface("io2.DOMReadaer");
    aDOMReader.set(this, viewer, DOMNode, htParams);
    aDOMReader.run();
    return aDOMReader.getAtomSetCollection();
  }

  /**
   * not used in Jmol project -- will close reader
   * 
   * @param fullPathName
   * @param name
   * @param reader
   * @param htParams 
   * @return fileData
   */
  Object createAtomSetCollectionFromReader(String fullPathName, String name,
                                           Object reader,
                                           Map<String, Object> htParams) {
    FileReader fileReader = new FileReader(this, viewer, name, fullPathName, name, null,
        reader, htParams, false);
    fileReader.run();
    return fileReader.getAtomSetCollection();
  }

  /////////////// generally useful file I/O methods /////////////////

  // mostly internal to FileManager and its enclosed classes

  BufferedInputStream getBufferedInputStream(String fullPathName) {
    Object ret = getBufferedReaderOrErrorMessageFromName(fullPathName,
        new String[2], true, true);
    return (ret instanceof BufferedInputStream ? (BufferedInputStream) ret
        : null);
  }

  public Object getBufferedInputStreamOrErrorMessageFromName(String name,
                                                             String fullName,
                                                             boolean showMsg,
                                                             boolean checkOnly,
                                                             byte[] outputBytes, boolean allowReader) {
    byte[] cacheBytes = (fullName == null || pngjCache == null ? null : JmolBinary
        .getCachedPngjBytes(this, fullName));
    if (cacheBytes == null)
      cacheBytes = (byte[]) cacheGet(name, true);
    BufferedInputStream bis = null;
    Object ret = null;
    String errorMessage = null;
    try {
      if (cacheBytes == null) {
        boolean isPngjBinaryPost = (name.indexOf("?POST?_PNGJBIN_") >= 0);
        boolean isPngjPost = (isPngjBinaryPost || name.indexOf("?POST?_PNGJ_") >= 0);
        if (name.indexOf("?POST?_PNG_") > 0 || isPngjPost) {
          Object o = viewer.getImageAs(isPngjPost ? "PNGJ" : "PNG", -1, 0, 0,
              null, null);
          if (!Escape.isAB(o))
            return o;
          if (isPngjBinaryPost) {
            outputBytes = (byte[]) o;
            name = TextFormat.simpleReplace(name, "?_", "=_");
          } else {
            name = new SB().append(name).append("=").appendSB(
                Base64.getBase64((byte[]) o)).toString();
          }
        }
        int iurl = urlTypeIndex(name);
        boolean isURL = (iurl >= 0);
        String post = null;
        if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
          post = name.substring(iurl + 6);
          name = name.substring(0, iurl);
        }
        boolean isApplet = (appletDocumentBaseURL != null);
        JmolFileAdapterInterface fai = viewer.getFileAdapter();

        if (isApplet || isURL) {
          if (isApplet && isURL && appletProxy != null)
            name = appletProxy + "?url=" + urlEncode(name);
          URL url = (isApplet ? new URL(appletDocumentBaseURL, name, null)
              : new URL((URL) null, name, null));
          if (checkOnly)
            return null;
          name = url.toString();
          if (showMsg && name.toLowerCase().indexOf("password") < 0)
            Logger.info("FileManager opening " + name);
          ret = fai.getBufferedURLInputStream(url, outputBytes, post);
          if (ret instanceof SB) {
            SB sb = (SB) ret;
            if (allowReader && !JmolBinary.isBase64(sb))
              return JmolBinary.getBufferedReaderForString(sb.toString());
            ret = JmolBinary.getBISForStringXBuilder(sb);
          } else if (Escape.isAB(ret)) {
            ret = new BufferedInputStream(new ByteArrayInputStream((byte[]) ret));
          }
        } else if ((cacheBytes = (byte[]) cacheGet(name, true)) == null) {
          if (showMsg)
            Logger.info("FileManager opening " + name);
          ret = fai.getBufferedFileInputStream(name);
        }
        if (ret instanceof String)
          return ret;
      }

      if (cacheBytes == null)
        bis = (BufferedInputStream) ret;
      else
        bis = new BufferedInputStream(new ByteArrayInputStream(cacheBytes));
      if (checkOnly) {
        bis.close();
        bis = null;
      }
      return bis;
    } catch (Exception e) {
      try {
        if (bis != null)
          bis.close();
      } catch (IOException e1) {
      }
      errorMessage = "" + e;
    }
    return errorMessage;
  }
  
  private String urlEncode(String name) {
    try {
      return URLEncoder.encode(name, "utf-8");
    } catch (UnsupportedEncodingException e) {
      return name;
    }
  }

  /**
   * just check for a file as being readable. Do not go into a zip file
   * 
   * @param filename
   * @return String[2] where [0] is fullpathname and [1] is error message or null
   */
  String[] getFullPathNameOrError(String filename) {
    String[] names = classifyName(filename, true);
    if (names == null || names[0] == null || names.length < 2)
      return new String[] { null, "cannot read file name: " + filename };
    String name = names[0];
    String fullPath = names[0].replace('\\', '/');
    name = JmolBinary.getZipRoot(name);
    Object errMsg = getBufferedInputStreamOrErrorMessageFromName(name, fullPath, false, true, null, false);
    return new String[] { fullPath,
        (errMsg instanceof String ? (String) errMsg : null) };
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary,
                                                 boolean doSpecialLoad) {
    Object data = cacheGet(name, false);
    boolean isBytes = Escape.isAB(data);
    byte[] bytes = (isBytes ? (byte[]) data : null);
    if (name.startsWith("cache://")) {
      if (data == null)
        return "cannot read " + name;
      if (isBytes) {
        bytes = (byte[]) data;
      } else {
        return JmolBinary.getBufferedReaderForString((String) data);
      }
    }
    String[] names = classifyName(name, true);
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = names[0].replace('\\', '/');
    return getUnzippedBufferedReaderOrErrorMessageFromName(names[0], bytes,
        false, isBinary, false, doSpecialLoad);
  }

  public String getEmbeddedFileState(String fileName) {
    String[] dir = null;
    dir = getZipDirectory(fileName, false);
    if (dir.length == 0) {
      String state = viewer.getFileAsString4(fileName, Integer.MAX_VALUE, false, true);
      return (state.indexOf(JC.EMBEDDED_SCRIPT_TAG) < 0 ? ""
          : JmolBinary.getEmbeddedScript(state));
    }
    for (int i = 0; i < dir.length; i++)
      if (dir[i].indexOf(".spt") >= 0) {
        String[] data = new String[] { fileName + "|" + dir[i], null };
        getFileDataOrErrorAsString(data, Integer.MAX_VALUE, false, false);
        return data[1];
      }
    return "";
  }

  public Object getUnzippedBufferedReaderOrErrorMessageFromName(
                                                                String name,
                                                                byte[] bytes,
                                                                boolean allowZipStream,
                                                                boolean asInputStream,
                                                                boolean isTypeCheckOnly,
                                                                boolean doSpecialLoad) {
    String[] subFileList = null;
    String[] info = (bytes == null && doSpecialLoad ? getSpartanFileList(name) : null);
    String name00 = name;
    if (info != null) {
      if (isTypeCheckOnly)
        return info;
      if (info[2] != null) {
        String header = info[1];
        Map<String, String> fileData = new Hashtable<String, String>();
        if (info.length == 3) {
          // we need information from the output file, info[2]
          String name0 = getObjectAsSections(info[2], header, fileData);
          fileData.put("OUTPUT", name0);
          info = JmolBinary.spartanFileList(name, fileData.get(name0));
          if (info.length == 3) {
            // might have a second option
            name0 = getObjectAsSections(info[2], header, fileData);
            fileData.put("OUTPUT", name0);
            info = JmolBinary.spartanFileList(info[1], fileData.get(name0));
          }
        }
        // load each file individually, but return files IN ORDER
        SB sb = new SB();
        if (fileData.get("OUTPUT") != null)
          sb.append(fileData.get(fileData.get("OUTPUT")));
        String s;
        for (int i = 2; i < info.length; i++) {
          name = info[i];
          name = getObjectAsSections(name, header, fileData);
          Logger.info("reading " + name);
          s = fileData.get(name);
          sb.append(s);
        }
        s = sb.toString();
        if (spardirCache == null)
          spardirCache = new Hashtable<String, byte[]>();
        spardirCache.put(name00.replace('\\', '/'), s.getBytes());
        return JmolBinary.getBufferedReaderForString(s);
      }
      // continuing...
      // here, for example, for an SPT file load that is not just a type check
      // (type check is only for application file opening and drag-drop to
      // determine if
      // script or load command should be used)
    }

    if (bytes == null && pngjCache != null )
      bytes = JmolBinary.getCachedPngjBytes(this, name);
    String fullName = name;
    if (name.indexOf("|") >= 0) {
      subFileList = TextFormat.splitChars(name, "|");
      if (bytes == null)
        Logger.info("FileManager opening " + name);
      name = subFileList[0];
    }
    Object t = (bytes == null ? getBufferedInputStreamOrErrorMessageFromName(
        name, fullName, true, false, null, !asInputStream) : new BufferedInputStream(
        new ByteArrayInputStream(bytes)));
    try {
      if (t instanceof String)
        return t;
      if (t instanceof BufferedReader)
        return t;
      BufferedInputStream bis = (BufferedInputStream) t;
      if (JmolBinary.isGzipS(bis)) {
        do {
          bis = new BufferedInputStream(JmolBinary.newGZIPInputStream(bis));
        } while (JmolBinary.isGzipS(bis));
      }
      if (JmolBinary.isCompoundDocumentStream(bis)) {
        JmolDocument doc = (JmolDocument) Interface
            .getOptionInterface("io2.CompoundDocument");
        doc.setStream(bis, true);
        return JmolBinary.getBufferedReaderForString(doc.getAllDataFiles(
            "Molecule", "Input").toString());
      }
      bis = JmolBinary.checkPngZipStream(bis);
      if (JmolBinary.isZipStream(bis)) {
        if (allowZipStream)
          return JmolBinary.newZipInputStream(bis);
        if (asInputStream)
          return JmolBinary.getZipFileContents(bis, subFileList, 1, true);
        String s = (String) JmolBinary.getZipFileContents(bis, subFileList, 1,
            false);
        bis.close();
        return JmolBinary.getBufferedReaderForString(s);
      }
      return (asInputStream ? bis : JmolBinary.getBufferedReader(bis, null));
    } catch (Exception ioe) {
      return ioe.toString();
    }
  }

  private String[] getSpartanFileList(String name) {
      // check for .spt file type -- Jmol script
      if (name.endsWith(".spt"))
        return new String[] { null, null, null }; // DO NOT actually load any file
      // check for zipped up spardir -- we'll automatically take first file there
      if (name.endsWith(".spardir.zip"))
        return new String[] { "SpartanSmol", "Directory Entry ", name + "|output"};
      name = name.replace('\\', '/');
      if (!name.endsWith(".spardir") && name.indexOf(".spardir/") < 0)
        return null; 
      // look for .spardir or .spardir/...
      int pt = name.lastIndexOf(".spardir");
      if (pt < 0)
        return null;
      if (name.lastIndexOf("/") > pt) {
        // a single file in the spardir directory is requested
        return new String[] { "SpartanSmol", "Directory Entry ",
            name + "/input", name + "/archive",
            name + "/Molecule:asBinaryString", name + "/proparc" };      
      }
      return new String[] { "SpartanSmol", "Directory Entry ", name + "/output" };
  }

  /**
   * delivers file contents and directory listing for a ZIP/JAR file into sb
   * 
   * @param name
   * @param header
   * @param fileData
   * @return name of entry
   */
  private String getObjectAsSections(String name, String header,
                                     Map<String, String> fileData) {
    if (name == null)
      return null;
    String[] subFileList = null;
    boolean asBinaryString = false;
    String name0 = name.replace('\\', '/');
    if (name.indexOf(":asBinaryString") >= 0) {
      asBinaryString = true;
      name = name.substring(0, name.indexOf(":asBinaryString"));
    }
    SB sb = null;
    if (fileData.containsKey(name0))
      return name0;
    if (name.indexOf("#JMOL_MODEL ") >= 0) {
      fileData.put(name0, name0 + "\n");
      return name0;
    }
    String fullName = name;
    if (name.indexOf("|") >= 0) {
      subFileList = TextFormat.splitChars(name, "|");
      name = subFileList[0];
    }
    BufferedInputStream bis = null;
    try {
      Object t = getBufferedInputStreamOrErrorMessageFromName(name, fullName,
          false, false, null, false);
      if (t instanceof String) {
        fileData.put(name0, (String) t + "\n");
        return name0;
      }
      bis = (BufferedInputStream) t;
      if (JmolBinary.isCompoundDocumentStream(bis)) {
        JmolDocument doc = (JmolDocument) Interface
            .getOptionInterface("io2.CompoundDocument");
        doc.setStream(bis, true);
        doc.getAllDataMapped(name.replace('\\', '/'), "Molecule", fileData);
      } else if (JmolBinary.isZipStream(bis)) {
        JmolBinary.getAllZipData(bis, subFileList, name.replace('\\', '/'), "Molecule",
            fileData);
      } else if (asBinaryString) {
        // used for Spartan binary file reading
        JmolDocument bd = (JmolDocument) Interface
            .getOptionInterface("io2.BinaryDocument");
        bd.setStream(bis, false);
        sb = new SB();
        //note -- these headers must match those in ZipUtil.getAllData and CompoundDocument.getAllData
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        try {
          while (true)
            sb.append(Integer.toHexString(bd.readByte() & 0xFF)).appendC(' ');
        } catch (Exception e1) {
          sb.appendC('\n');
        }
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      } else {
        BufferedReader br = JmolBinary.getBufferedReader(
            JmolBinary.isGzipS(bis) ? new BufferedInputStream(JmolBinary.newGZIPInputStream(bis)) : bis, null);
        String line;
        sb = new SB();
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        while ((line = br.readLine()) != null) {
          sb.append(line);
          sb.appendC('\n');
        }
        br.close();
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      }
    } catch (Exception ioe) {
      fileData.put(name0, ioe.toString());
    }
    if (bis != null)
      try {
        bis.close();
      } catch (Exception e) {
        //
      }
    if (!fileData.containsKey(name0))
      fileData.put(name0, "FILE NOT FOUND: " + name0 + "\n");
    return name0;
  }

  /**
   * 
   * @param fileName
   * @param addManifest
   * @return [] if not a zip file;
   */
  public String[] getZipDirectory(String fileName, boolean addManifest) {
    Object t = getBufferedInputStreamOrErrorMessageFromName(fileName, fileName,
        false, false, null, false);
    return JmolBinary.getZipDirectoryAndClose((BufferedInputStream) t, addManifest);
  }

  public Object getFileAsBytes(String name, OutputStream os, boolean  allowZip) {
    // ?? used by eval of "WRITE FILE"
    // will be full path name
    if (name == null)
      return null;
    String fullName = name;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = TextFormat.splitChars(name, "|");
      name = subFileList[0];
      allowZip = true;
    }
    Object t = getBufferedInputStreamOrErrorMessageFromName(name, fullName, false, false,
        null, false);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = (BufferedInputStream) t;
      Object bytes = (os != null || subFileList == null || subFileList.length <= 1
            || !allowZip || !JmolBinary.isZipStream(bis) 
            && !JmolBinary.isPngZipStream(bis) ? JmolBinary.getStreamAsBytes(
            bis, os)
            : JmolBinary.getZipFileContentsAsBytes(bis, subFileList, 1));
      bis.close();
      return bytes;
    } catch (Exception ioe) {
      return ioe.toString();
    }
  }

  /**
   * 
   * @param data
   *        [0] initially path name, but returned as full path name; [1]file
   *        contents (directory listing for a ZIP/JAR file) or error string
   * @param nBytesMax
   * @param doSpecialLoad
   * @param allowBinary 
   * @return true if successful; false on error
   */

  boolean getFileDataOrErrorAsString(String[] data, int nBytesMax,
                                     boolean doSpecialLoad, boolean allowBinary) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data, false,
        doSpecialLoad);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    try {
      BufferedReader br = (BufferedReader) t;
      SB sb = SB.newN(8192);
      String line;
      if (nBytesMax == Integer.MAX_VALUE) {
        line = br.readLine();
        if (allowBinary || line != null && line.indexOf('\0') < 0
            && (line.length() != 4 || line.charAt(0) != 65533
            || line.indexOf("PNG") != 1)) {
          sb.append(line).appendC('\n');
          while ((line = br.readLine()) != null)
            sb.append(line).appendC('\n');
        }
      } else {
        int n = 0;
        int len;
        while (n < nBytesMax && (line = br.readLine()) != null) {
          if (nBytesMax - n < (len = line.length()) + 1)
            line = line.substring(0, nBytesMax - n - 1);
          sb.append(line).appendC('\n');
          n += len + 1;
        }
      }
      br.close();
      data[1] = sb.toString();
      return true;
    } catch (Exception ioe) {
      data[1] = ioe.toString();
      return false;
    }
  }

  void loadImage(String name, String echoName) {
    Object image = null;
    Object info = null;
    String fullPathName = "";
    while (true) {
      if (name == null)
        break;
      String[] names = classifyName(name, true);
      if (names == null) {
        fullPathName = "cannot read file name: " + name;
        break;
      }
      ApiPlatform apiPlatform = viewer.apiPlatform;
      fullPathName = names[0].replace('\\', '/');
      if (fullPathName.indexOf("|") > 0) {
        Object ret = getFileAsBytes(fullPathName, null, true);
        if (!Escape.isAB(ret)) {
          fullPathName = "" + ret;
          break;
        }
        image = (viewer.isJS ? ret : apiPlatform.createImage(ret));
      } else if (viewer.isJS) {
      } else if (urlTypeIndex(fullPathName) >= 0) {
        try {
          image = apiPlatform.createImage(new URL((URL) null, fullPathName,
              null));
        } catch (Exception e) {
          fullPathName = "bad URL: " + fullPathName;
          break;
        }
      } else {
        image = apiPlatform.createImage(fullPathName);
      }
      /**
       * @j2sNative
       * 
       *            info = [echoName, fullPathName];
       * 
       */
      {
        if (image == null)
          break;
      }
      try {
        if (!apiPlatform.waitForDisplay(info, image)) {
          image = null;
          break;
        }
        /**
         * 
         * note -- JavaScript just returns immediately, because we must wait
         * for the image to load, and it is single-threaded
         * 
         * @j2sNative
         * 
         *            return;
         */
        {}

      } catch (Exception e) {
        System.out.println(e.toString());
        fullPathName = e.toString() + " opening " + fullPathName;
        image = null;
        break;
      }
      if (apiPlatform.getImageWidth(image) < 1) {
        fullPathName = "invalid or missing image " + fullPathName;
        image = null;
        break;
      }
      break;
    }
    viewer.loadImageData(image, fullPathName, echoName, null);
  }

  public final static int URL_LOCAL = 3;
  private final static String[] urlPrefixes = { "http:", "https:", "ftp:",
      "file:" };

  public static int urlTypeIndex(String name) {
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        return i;
      }
    }
    return -1;
  }
  
  public static boolean isLocal(String fileName) {
    if (fileName == null)
      return false;
    int itype = urlTypeIndex(fileName);
    return (itype < 0 || itype == FileManager.URL_LOCAL);
  }



  /**
   * 
   * @param name
   * @param isFullLoad
   * @return [0] full path name, [1] file name without path, [2] full URL
   */
  public String[] classifyName(String name, boolean isFullLoad) {
    if (name == null)
      return new String[] { null };
    boolean doSetPathForAllFiles = (pathForAllFiles.length() > 0);
    if (name.startsWith("?")) {
       if ((name = viewer.dialogAsk("load", name.substring(1))) == null)
         return new String[] { isFullLoad ? "#CANCELED#" : null };
       doSetPathForAllFiles = false;
    }
    JmolFileInterface file = null;
    URL url = null;
    String[] names = null;
    if (name.startsWith("cache://")) {
      names = new String[3];
      names[0] = names[2] = name;
      names[1] = stripPath(names[0]);
      return names;
    }
    name = viewer.resolveDatabaseFormat(name);
    if (name.indexOf(":") < 0 && name.indexOf("/") != 0)
      name = addDirectory(viewer.getDefaultDirectory(), name);
    if (appletDocumentBaseURL != null) {
      // This code is only for the applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:/" + name;
        //        else if (name.indexOf("/") == 0 && viewer.isSignedApplet())
        //        name = "file:" + name;
        url = new URL(appletDocumentBaseURL, name, null);
      } catch (MalformedURLException e) {
        return new String[] { isFullLoad ? e.toString() : null };
      }
    } else {
      // This code is for the app -- no local file reading for headless
      if (urlTypeIndex(name) >= 0 
          || viewer.isRestricted(ACCESS.NONE) 
          || viewer.isRestricted(ACCESS.READSPT) 
              && !name.endsWith(".spt") && !name.endsWith("/")) {
        try {
          url = new URL((URL) null, name, null);
        } catch (MalformedURLException e) {
          return new String[] { isFullLoad ? e.toString() : null };
        }
      } else {
        file = viewer.apiPlatform.newFile(name);
        names = new String[] { file.getAbsolutePath(), file.getName(),
            "file:/" + file.getAbsolutePath().replace('\\', '/') };
      }
    }
    if (url != null) {
      names = new String[3];
      names[0] = names[2] = url.toString();
      names[1] = stripPath(names[0]);
    }
    if (doSetPathForAllFiles) {
      String name0 = names[0];
      names[0] = pathForAllFiles + names[1];
      Logger.info("FileManager substituting " + name0 + " --> " + names[0]);
    }
    if (isFullLoad && (file != null || urlTypeIndex(names[0]) == URL_LOCAL)) {
      String path = (file == null ? TextFormat.trim(names[0].substring(5), "/")
          : names[0]);
      int pt = path.length() - names[1].length() - 1;
      if (pt > 0) {
        path = path.substring(0, pt);
        setLocalPath(viewer, path, true);
      }
    }
    return names;
  }

  private static String addDirectory(String defaultDirectory, String name) {
    if (defaultDirectory.length() == 0)
      return name;
    char ch = (name.length() > 0 ? name.charAt(0) : ' ');
    String s = defaultDirectory.toLowerCase();
    if ((s.endsWith(".zip") || s.endsWith(".tar")) && ch != '|' && ch != '/')
      defaultDirectory += "|";
    return defaultDirectory
        + (ch == '/'
            || ch == '/'
            || (ch = defaultDirectory.charAt(defaultDirectory.length() - 1)) == '|'
            || ch == '/' ? "" : "/") + name;
  }

  String getDefaultDirectory(String name) {
    String[] names = classifyName(name, true);
    if (names == null)
      return "";
    name = fixPath(names[0]);
    return (name == null ? "" : name.substring(0, name.lastIndexOf("/")));
  }

  private static String fixPath(String path) {
    path = path.replace('\\', '/');
    path = TextFormat.simpleReplace(path, "/./", "/");
    int pt = path.lastIndexOf("//") + 1;
    if (pt < 1)
      pt = path.indexOf(":/") + 1;
    if (pt < 1)
      pt = path.indexOf("/");
    if (pt < 0)
      return null;
    String protocol = path.substring(0, pt);
    path = path.substring(pt);

    while ((pt = path.lastIndexOf("/../")) >= 0) {
      int pt0 = path.substring(0, pt).lastIndexOf("/");
      if (pt0 < 0)
        return TextFormat.simpleReplace(protocol + path, "/../", "/");
      path = path.substring(0, pt0) + path.substring(pt + 3);
    }
    if (path.length() == 0)
      path = "/";
    return protocol + path;
  }

  public String getFilePath(String name, boolean addUrlPrefix,
                            boolean asShortName) {
    String[] names = classifyName(name, false);
    return (names == null || names.length == 1 ? "" : asShortName ? names[1]
        : addUrlPrefix ? names[2] 
        : names[0] == null ? "" 
        : names[0].replace('\\', '/'));
  }

  private final static String[] urlPrefixPairs = { "http:", "http://", "www.",
      "http://www.", "https:", "https://", "ftp:", "ftp://", "file:",
      "file:///" };

  public static String getLocalUrl(JmolFileInterface file) {
    // entering a url on a file input box will be accepted,
    // but cause an error later. We can fix that...
    // return null if there is no problem, the real url if there is
    if (file.getName().startsWith("="))
      return file.getName();
    String path = file.getAbsolutePath().replace('\\', '/');
    for (int i = 0; i < urlPrefixPairs.length; i++)
      if (path.indexOf(urlPrefixPairs[i]) == 0)
        return null;
    // looking for /xxx/xxxx/file://...
    for (int i = 0; i < urlPrefixPairs.length; i += 2)
      if (path.indexOf(urlPrefixPairs[i]) > 0)
        return urlPrefixPairs[i + 1]
            + TextFormat.trim(path.substring(path.indexOf(urlPrefixPairs[i])
                + urlPrefixPairs[i].length()), "/");
    return null;
  }

  public static JmolFileInterface getLocalDirectory(JmolViewer viewer, boolean forDialog) {
    String localDir = (String) viewer
        .getParameter(forDialog ? "currentLocalPath" : "defaultDirectoryLocal");
    if (forDialog && localDir.length() == 0)
      localDir = (String) viewer.getParameter("defaultDirectoryLocal");
    if (localDir.length() == 0)
      return (viewer.isApplet() ? null : viewer.apiPlatform.newFile(System
          .getProperty("user.dir", ".")));
    if (viewer.isApplet() && localDir.indexOf("file:/") == 0)
      localDir = localDir.substring(6);
    JmolFileInterface f = viewer.apiPlatform.newFile(localDir);
    return f.isDirectory() ? f : f.getParentAsFile();
  }

  /**
   * called by getImageFileNameFromDialog 
   * called by getOpenFileNameFromDialog
   * called by getSaveFileNameFromDialog
   * 
   * called by classifyName for any full file load
   * called from the CD command
   * 
   * currentLocalPath is set in all cases
   *   and is used specifically for dialogs as a first try
   * defaultDirectoryLocal is set only when not from a dialog
   *   and is used only in getLocalPathForWritingFile or
   *   from an open/save dialog.
   * In this way, saving a file from a dialog doesn't change
   *   the "CD" directory. 
   * Neither of these is saved in the state, but 
   * 
   * 
   * @param viewer
   * @param path
   * @param forDialog
   */
  public static void setLocalPath(JmolViewer viewer, String path,
                                  boolean forDialog) {
    while (path.endsWith("/") || path.endsWith("\\"))
      path = path.substring(0, path.length() - 1);
    viewer.setStringProperty("currentLocalPath", path);
    if (!forDialog)
      viewer.setStringProperty("defaultDirectoryLocal", path);
  }

  public static String getLocalPathForWritingFile(JmolViewer viewer, String file) {
    if (file.indexOf("file:/") == 0)
      return file.substring(6);
    if (file.indexOf("/") == 0 || file.indexOf(":") >= 0)
      return file;
    JmolFileInterface dir = getLocalDirectory(viewer, false);
    return (dir == null ? file : fixPath(dir.toString() + "/" + file));
  }

  public static String setScriptFileReferences(String script, String localPath,
                                               String remotePath,
                                               String scriptPath) {
    if (localPath != null)
      script = setScriptFileReferences(script, localPath, true);
    if (remotePath != null)
      script = setScriptFileReferences(script, remotePath, false);
    script = TextFormat.simpleReplace(script, "\1\"", "\"");
    if (scriptPath != null) {
      while (scriptPath.endsWith("/"))
        scriptPath = scriptPath.substring(0, scriptPath.length() - 1);
      for (int ipt = 0; ipt < scriptFilePrefixes.length; ipt++) {
        String tag = scriptFilePrefixes[ipt];
        script = TextFormat.simpleReplace(script, tag + ".", tag + scriptPath);
      }
    }
    return script;
  }

  /**
   * Sets all local file references in a script file to point to files within
   * dataPath. If a file reference contains dataPath, then the file reference is
   * left with that RELATIVE path. Otherwise, it is changed to a relative file
   * name within that dataPath. 
   * 
   * Only file references starting with "file://" are changed.
   * 
   * @param script
   * @param dataPath
   * @param isLocal 
   * @return revised script
   */
  private static String setScriptFileReferences(String script, String dataPath,
                                                boolean isLocal) {
    if (dataPath == null)
      return script;
    boolean noPath = (dataPath.length() == 0);
    JmolList<String> fileNames = new  JmolList<String>();
    JmolBinary.getFileReferences(script, fileNames);
    JmolList<String> oldFileNames = new  JmolList<String>();
    JmolList<String> newFileNames = new  JmolList<String>();
    int nFiles = fileNames.size();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name0 = fileNames.get(iFile);
      String name = name0;
      if (isLocal == isLocal(name)) {
        int pt = (noPath ? -1 : name.indexOf("/" + dataPath + "/"));
        if (pt >= 0) {
          name = name.substring(pt + 1);
        } else {
          pt = name.lastIndexOf("/");
          if (pt < 0 && !noPath)
            name = "/" + name;
          if (pt < 0 || noPath)
            pt++;
          name = dataPath + name.substring(pt);
        }
      }
      Logger.info("FileManager substituting " + name0 + " --> " + name);
      oldFileNames.addLast("\"" + name0 + "\"");
      newFileNames.addLast("\1\"" + name + "\"");
    }
    return TextFormat.replaceStrings(script, oldFileNames, newFileNames);
  }

  public static String[] scriptFilePrefixes = new String[] { "/*file*/\"",
      "FILE0=\"", "FILE1=\"" };

  public static String stripPath(String name) {
    int pt = Math.max(name.lastIndexOf("|"), name.lastIndexOf("/"));
    return name.substring(pt + 1);
  }

  public static String fixFileNameVariables(String format, String fname) {
    String str = TextFormat.simpleReplace(format, "%FILE", fname);
    if (str.indexOf("%LC") < 0)
      return str;
    fname = fname.toLowerCase();
    str = TextFormat.simpleReplace(str, "%LCFILE", fname);
    if (fname.length() == 4)
      str = TextFormat.simpleReplace(str, "%LC13", fname.substring(1, 3));
    return str;
  }

  public Map<String, byte[]> pngjCache;
  public Map<String, byte[]> spardirCache;
  
  public void clearPngjCache(String fileName) {
    if (fileName == null || pngjCache != null && pngjCache.containsKey(getCanonicalName(JmolBinary.getZipRoot(fileName))))
      pngjCache = null;
  }


  private Map<String, Object> cache = new Hashtable<String, Object>();
  void cachePut(String key, Object data) {
    key = key.replace('\\', '/');
    if (Logger.debugging)
      Logger.info("cachePut " + key);
    if (data == null || data.equals(""))
      cache.remove(key);
    else
      cache.put(key, data);
  }
  
  public Object cacheGet(String key, boolean bytesOnly) {
    key = key.replace('\\', '/');
    if (Logger.debugging && cache.containsKey(key))
      Logger.info("cacheGet " + key);
    Object data = cache.get(key);
    return (bytesOnly && (data instanceof String) ? null : data);
  }

  void cacheClear() {
    cache.clear();
  }

  public int cacheFileByName(String fileName, boolean isAdd) {
    if (fileName == null || !isAdd && fileName.equalsIgnoreCase("")) {
      cacheClear();
      return -1;
    }
    Object data;
    if (isAdd) {
      fileName = viewer.resolveDatabaseFormat(fileName);
      data = getFileAsBytes(fileName, null, true);
      if (data instanceof String)
        return 0;
      cachePut(fileName, data);
    } else {
      data = cache.remove(fileName.replace('\\', '/'));
    }
    return (data == null ? 0 : data instanceof String ? ((String) data).length()
        : ((byte[]) data).length);
  }

  Map<String, Integer> cacheList() {
    Map<String, Integer> map = new Hashtable<String, Integer>();
    for (Map.Entry<String, Object> entry : cache.entrySet())
      map.put(entry.getKey(), Integer
          .valueOf(Escape.isAB(entry.getValue()) ? ((byte[]) entry
              .getValue()).length : entry.getValue().toString().length()));
    return map;
  }

  public String getCanonicalName(String pathName) {
    String[] names = classifyName(pathName, true);
    return (names == null ? pathName : names[2]);
  }

}
