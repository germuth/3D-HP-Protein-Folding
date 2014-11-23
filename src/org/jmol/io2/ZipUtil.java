/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

package org.jmol.io2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader; //import java.net.URL;
//import java.net.URLConnection;
import org.jmol.util.JmolList;
import java.util.Date;
import java.util.Hashtable;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipOutputStream;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolDocument;
import org.jmol.api.JmolFileInterface;
import org.jmol.api.JmolZipUtility;
import org.jmol.api.ZInputStream;
import org.jmol.io.JmolBinary;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class ZipUtil implements JmolZipUtility {

  private final static String SCENE_TAG = "###scene.spt###";

  public ZipUtil() {
    // for reflection
  }

  public ZInputStream newZipInputStream(InputStream is) {
    return newZIS(is);
  }

  private static ZInputStream newZIS(InputStream is) {
    return (is instanceof ZInputStream ? (ZInputStream) is
        : is instanceof BufferedInputStream ? new JmolZipInputStream(is)
            : new JmolZipInputStream(new BufferedInputStream(is)));
  }

  /**
   * reads a ZIP file and saves all data in a Hashtable so that the files may be
   * organized later in a different order. Also adds a #Directory_Listing entry.
   * 
   * Files are bracketed by BEGIN Directory Entry and END Directory Entry lines,
   * similar to CompoundDocument.getAllData.
   * 
   * @param is
   * @param subfileList
   * @param name0
   *        prefix for entry listing
   * @param binaryFileList
   *        |-separated list of files that should be saved as xx xx xx hex byte
   *        strings. The directory listing is appended with ":asBinaryString"
   * @param fileData
   */
  public void getAllZipData(InputStream is, String[] subfileList, String name0,
                            String binaryFileList, Map<String, String> fileData) {
    getAllZipDataStatic(is, subfileList, name0, binaryFileList, fileData);
  }
  
  private static void getAllZipDataStatic(InputStream is, String[] subfileList, String name0,
                            String binaryFileList, Map<String, String> fileData) {
    ZipInputStream zis = (ZipInputStream) newZIS(is);
    ZipEntry ze;
    SB listing = new SB();
    binaryFileList = "|" + binaryFileList + "|";
    String prefix = TextFormat.join(subfileList, '/', 1);
    String prefixd = null;
    if (prefix != null) {
      prefixd = prefix.substring(0, prefix.indexOf("/") + 1);
      if (prefixd.length() == 0)
        prefixd = null;
    }
    try {
      while ((ze = zis.getNextEntry()) != null) {
        String name = ze.getName();
        if (prefix != null && prefixd != null
            && !(name.equals(prefix) || name.startsWith(prefixd)))
          continue;
        //System.out.println("ziputil: " + name);
        listing.append(name).appendC('\n');
        String sname = "|" + name.substring(name.lastIndexOf("/") + 1) + "|";
        boolean asBinaryString = (binaryFileList.indexOf(sname) >= 0);
        byte[] bytes = JmolBinary.getStreamBytes(zis, ze.getSize());
        String str;
        if (asBinaryString) {
          str = getBinaryStringForBytes(bytes);
          name += ":asBinaryString";
        } else {
          str = JmolBinary.fixUTF(bytes);
        }
        str = "BEGIN Directory Entry " + name + "\n" + str
            + "\nEND Directory Entry " + name + "\n";
        fileData.put(name0 + "|" + name, str);
      }
    } catch (Exception e) {
    }
    fileData.put("#Directory_Listing", listing.toString());
  }

  private static String getBinaryStringForBytes(byte[] bytes) {
    SB ret = new SB();
    for (int i = 0; i < bytes.length; i++)
      ret.append(Integer.toHexString(bytes[i] & 0xFF)).appendC(' ');
    return ret.toString();
  }

  /**
   * iteratively drills into zip files of zip files to extract file content or
   * zip file directory. Also works with JAR files.
   * 
   * Does not return "__MACOS" paths
   * 
   * @param bis
   * @param list
   * @param listPtr
   * @param asBufferedInputStream
   *        for Pmesh
   * @return directory listing or subfile contents
   */
  public Object getZipFileContents(BufferedInputStream bis, String[] list,
                                   int listPtr, boolean asBufferedInputStream) {
    SB ret;
    if (list == null || listPtr >= list.length)
      return getZipDirectoryAsStringAndClose(bis);
    String fileName = list[listPtr];
    ZipInputStream zis = new ZipInputStream(bis);
    ZipEntry ze;
    //System.out.println("fname=" + fileName);
    try {
      boolean isAll = (fileName.equals("."));
      if (isAll || fileName.lastIndexOf("/") == fileName.length() - 1) {
        ret = new SB();
        while ((ze = zis.getNextEntry()) != null) {
          String name = ze.getName();
          if (isAll || name.startsWith(fileName))
            ret.append(name).appendC('\n');
        }
        String str = ret.toString();
        if (asBufferedInputStream)
          return new BufferedInputStream(new ByteArrayInputStream(str
              .getBytes()));
        return str;
      }
      boolean asBinaryString = false;
      if (fileName.indexOf(":asBinaryString") > 0) {
        fileName = fileName.substring(0, fileName.indexOf(":asBinaryString"));
        asBinaryString = true;
      }
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = JmolBinary.getStreamBytes(zis, ze.getSize());
        //System.out.println("ZipUtil::ZipEntry.name = " + ze.getName() + " " + bytes.length);
        if (JmolBinary.isZipFile(bytes))
          return getZipFileContents(new BufferedInputStream(
              new ByteArrayInputStream(bytes)), list, ++listPtr,
              asBufferedInputStream);
        if (asBufferedInputStream)
          return new BufferedInputStream(new ByteArrayInputStream(bytes));
        if (asBinaryString) {
          ret = new SB();
          for (int i = 0; i < bytes.length; i++)
            ret.append(Integer.toHexString(bytes[i] & 0xFF)).appendC(' ');
          return ret.toString();
        }
        return JmolBinary.fixUTF(bytes);
      }
    } catch (Exception e) {
    }
    return "";
  }

  public byte[] getZipFileContentsAsBytes(BufferedInputStream bis,
                                          String[] list, int listPtr) {
    byte[] ret = new byte[0];
    String fileName = list[listPtr];
    if (fileName.lastIndexOf("/") == fileName.length() - 1)
      return ret;
    try {
      bis = JmolBinary.checkPngZipStream(bis);
      ZipInputStream zis = new ZipInputStream(bis);
      ZipEntry ze;
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = JmolBinary.getStreamBytes(zis, ze.getSize());
        if (JmolBinary.isZipFile(bytes) && ++listPtr < list.length)
          return getZipFileContentsAsBytes(new BufferedInputStream(
              new ByteArrayInputStream(bytes)), list, listPtr);
        return bytes;
      }
    } catch (Exception e) {
    }
    return ret;
  }

  public String getZipDirectoryAsStringAndClose(BufferedInputStream bis) {
    SB sb = new SB();
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(bis, false);
      bis.close();
    } catch (Exception e) {
      Logger.error(e.toString());
    }
    for (int i = 0; i < s.length; i++)
      sb.append(s[i]).appendC('\n');
    return sb.toString();
  }

  public String[] getZipDirectoryAndClose(BufferedInputStream bis,
                                          boolean addManifest) {
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(bis, addManifest);
      bis.close();
    } catch (Exception e) {
      Logger.error(e.toString());
    }
    return s;
  }

  private String[] getZipDirectoryOrErrorAndClose(BufferedInputStream bis,
                                                  boolean addManifest)
      throws IOException {
    bis = JmolBinary.checkPngZipStream(bis);
    JmolList<String> v = new  JmolList<String>();
    ZipInputStream zis = new ZipInputStream(bis);
    ZipEntry ze;
    String manifest = null;
    while ((ze = zis.getNextEntry()) != null) {
      String fileName = ze.getName();
      if (addManifest && isJmolManifest(fileName))
        manifest = getZipEntryAsString(zis);
      else if (!fileName.startsWith("__MACOS")) // resource fork not nec.
        v.addLast(fileName);
    }
    zis.close();
    if (addManifest)
      v.add(0, manifest == null ? "" : manifest + "\n############\n");
    return v.toArray(new String[v.size()]);
  }

  private static String getZipEntryAsString(InputStream is) throws IOException {
    return JmolBinary.fixUTF(JmolBinary.getStreamBytes(is, -1));
  }

  private static boolean isJmolManifest(String thisEntry) {
    return thisEntry.startsWith("JmolManifest");
  }

  /**
   * caches an entire pngj file's contents into a Map
   * 
   * @param bis
   * @param fileName
   * @param cache
   * @return file listing, separated by \n
   */
  public String cacheZipContents(BufferedInputStream bis, String fileName,
                                 Map<String, byte[]> cache) {
    ZipInputStream zis = (ZipInputStream) newZipInputStream(bis);
    ZipEntry ze;
    SB listing = new SB();
    long n = 0;
    try {
      while ((ze = zis.getNextEntry()) != null) {
        String name = ze.getName();
        listing.append(name).appendC('\n');
        long nBytes = ze.getSize();
        byte[] bytes = JmolBinary.getStreamBytes(zis, nBytes);
        n += bytes.length;
        cache.put(fileName + "|" + name, bytes);
      }
      zis.close();
    } catch (Exception e) {
      try {
        zis.close();
      } catch (IOException e1) {
      }
      return null;
    }
    Logger.info("ZipUtil cached " + n + " bytes from " + fileName);
    return listing.toString();
  }

  public String getGzippedBytesAsString(byte[] bytes) {
    return staticGetGzippedBytesAsString(bytes);
  }

  static String staticGetGzippedBytesAsString(byte[] bytes) {
    try {
      InputStream is = new ByteArrayInputStream(bytes);
      do {
        is = new BufferedInputStream(new GZIPInputStream(is, 512));
      } while (JmolBinary.isGzipS(is));
      String s = getZipEntryAsString(is);
      is.close();
      return s;
    } catch (Exception e) {
      return "";
    }
  }

  public InputStream getGzippedInputStream(byte[] bytes) {
    try {
      InputStream is = new ByteArrayInputStream(bytes);
      do {
        is = new BufferedInputStream(new GZIPInputStream(is, 512));
      } while (JmolBinary.isGzipS(is));
      return is;
    } catch (Exception e) {
      return null;
    }
  }

  public InputStream newGZIPInputStream(BufferedInputStream bis)
      throws IOException {
    return new GZIPInputStream(bis, 512);
  }

  private String addPngFileBytes(String name, byte[] ret, int iFile,
                                Hashtable<Object, String> crcMap,
                                boolean isSparDir, String newName, int ptSlash,
                                JmolList<Object> v) {
    CRC32 crc = new CRC32();
    crc.update(ret, 0, ret.length);
    Long crcValue = Long.valueOf(crc.getValue());
    // only add to the data list v when the data in the file is new
    if (crcMap.containsKey(crcValue)) {
      // let newName point to the already added data
      newName = crcMap.get(crcValue);
    } else {
      if (isSparDir)
        newName = newName.replace('.', '_');
      if (crcMap.containsKey(newName)) {
        // now we have a conflict. To different files with the same name
        // append "[iFile]" to the new file name to ensure it's unique
        int pt = newName.lastIndexOf(".");
        if (pt > ptSlash) // is a file extension, probably
          newName = newName.substring(0, pt) + "[" + iFile + "]"
              + newName.substring(pt);
        else
          newName = newName + "[" + iFile + "]";
      }
      v.addLast(name);
      v.addLast(newName);
      v.addLast(ret);
      crcMap.put(crcValue, newName);
    }
    return newName;
  }

  public Object writeZipFile(FileManager fm, Viewer viewer, String outFileName,
                             JmolList<Object> fileNamesAndByteArrays, String msg) {
    byte[] buf = new byte[1024];
    long nBytesOut = 0;
    long nBytes = 0;
    Logger.info("creating zip file " + (outFileName == null ? "" : outFileName)
        + "...");
    String fullFilePath = null;
    String fileList = "";
    try {
      ByteArrayOutputStream bos = (outFileName == null
          || outFileName.startsWith("http://") ? new ByteArrayOutputStream()
          : null);
      ZipOutputStream os = new ZipOutputStream(
          bos == null ? (OutputStream) new FileOutputStream(outFileName) : bos);
      for (int i = 0; i < fileNamesAndByteArrays.size(); i += 3) {
        String fname = (String) fileNamesAndByteArrays.get(i);
        byte[] bytes = null;
        if (fname.indexOf("file:/") == 0) {
          fname = fname.substring(5);
          if (fname.length() > 2 && fname.charAt(2) == ':') // "/C:..." DOS/Windows
            fname = fname.substring(1);
        } else if (fname.indexOf("cache://") == 0) {
          Object data = fm.cacheGet(fname, false);
          fname = fname.substring(8);
          bytes = (Escape.isAB(data) ? (byte[]) data : ((String) data)
              .getBytes());
        }
        String fnameShort = (String) fileNamesAndByteArrays.get(i + 1);
        if (fnameShort == null)
          fnameShort = fname;
        if (bytes == null)
          bytes = (byte[]) fileNamesAndByteArrays.get(i + 2);
        String key = ";" + fnameShort + ";";
        if (fileList.indexOf(key) >= 0) {
          Logger.info("duplicate entry");
          continue;
        }
        fileList += key;
        os.putNextEntry(new ZipEntry(fnameShort));
        int nOut = 0;
        if (bytes == null) {
          // get data from disk
          FileInputStream in = new FileInputStream(fname);
          int len;
          while ((len = in.read(buf, 0, 1024)) > 0) {
            os.write(buf, 0, len);
            nOut += len;
          }
          in.close();
        } else {
          // data are already in byte form
          os.write(bytes, 0, bytes.length);
          nOut += bytes.length;
        }
        nBytesOut += nOut;
        os.closeEntry();
        Logger.info("...added " + fname + " (" + nOut + " bytes)");
      }
      os.close();
      Logger.info(nBytesOut + " bytes prior to compression");
      if (bos != null) {
        byte[] bytes = bos.toByteArray();
        if (outFileName == null)
          return bytes;
        fullFilePath = outFileName;
        nBytes = bytes.length;
        String ret = JmolBinary.postByteArray(fm, outFileName, bytes);
        if (ret.indexOf("Exception") >= 0)
          return ret;
        msg += " " + ret;
      } else {
        JmolFileInterface f = viewer.apiPlatform.newFile(outFileName);
        fullFilePath = f.getAbsolutePath().replace('\\', '/');
        nBytes = f.length();
      }
    } catch (IOException e) {
      Logger.info(e.toString());
      return e.toString();
    }
    return msg + " " + nBytes + " " + fullFilePath;
  }

  public String getSceneScript(String[] scenes, Map<String, String> htScenes,
                               JmolList<Integer> list) {
    // no ".spt.png" -- that's for the sceneScript-only version
    // that we will create here.
    // extract scenes based on "pause scene ..." commands
    int iSceneLast = 0;
    int iScene = 0;
    SB sceneScript = new SB().append(SCENE_TAG).append(
        " Jmol ").append(Viewer.getJmolVersion()).append(
        "\n{\nsceneScripts={");
    for (int i = 1; i < scenes.length; i++) {
      scenes[i - 1] = TextFormat.trim(scenes[i - 1], "\t\n\r ");
      int[] pt = new int[1];
      iScene = Parser.parseIntNext(scenes[i], pt);
      if (iScene == Integer.MIN_VALUE)
        return "bad scene ID: " + iScene;
      scenes[i] = scenes[i].substring(pt[0]);
      list.addLast(Integer.valueOf(iScene));
      String key = iSceneLast + "-" + iScene;
      htScenes.put(key, scenes[i - 1]);
      if (i > 1)
        sceneScript.append(",");
      sceneScript.appendC('\n').append(Escape.eS(key)).append(": ")
          .append(Escape.eS(scenes[i - 1]));
      iSceneLast = iScene;
    }
    sceneScript.append("\n}\n");
    if (list.size() == 0)
      return "no lines 'pause scene n'";
    sceneScript
        .append("\nthisSceneRoot = '$SCRIPT_PATH$'.split('_scene_')[1];\n")
        .append(
            "thisSceneID = 0 + ('$SCRIPT_PATH$'.split('_scene_')[2]).split('.')[1];\n")
        .append(
            "var thisSceneState = '$SCRIPT_PATH$'.replace('.min.png','.all.png') + 'state.spt';\n")
        .append("var spath = ''+currentSceneID+'-'+thisSceneID;\n")
        .append("print thisSceneRoot + ' ' + spath;\n")
        .append("var sscript = sceneScripts[spath];\n")
        .append("var isOK = true;\n")
        .append("try{\n")
        .append("if (thisSceneRoot != currentSceneRoot){\n")
        .append(" isOK = false;\n")
        .append("} else if (sscript != '') {\n")
        .append(" isOK = true;\n")
        .append("} else if (thisSceneID <= currentSceneID){\n")
        .append(" isOK = false;\n")
        .append("} else {\n")
        .append(" sscript = '';\n")
        .append(" for (var i = currentSceneID; i < thisSceneID; i++){\n")
        .append(
            "  var key = ''+i+'-'+(i + 1); var script = sceneScripts[key];\n")
        .append("  if (script = '') {isOK = false;break;}\n")
        .append("  sscript += ';'+script;\n")
        .append(" }\n")
        .append("}\n}catch(e){print e;isOK = false}\n")
        .append(
            "if (isOK) {"
                + wrapPathForAllFiles("script inline @sscript",
                    "print e;isOK = false") + "}\n")
        .append("if (!isOK){script @thisSceneState}\n")
        .append(
            "currentSceneRoot = thisSceneRoot; currentSceneID = thisSceneID;\n}\n");
    return sceneScript.toString();
  }

  private static String wrapPathForAllFiles(String cmd, String strCatch) {
    String vname = "v__" + ("" + Math.random()).substring(3);
    return "# Jmol script\n{\n\tVar "
        + vname
        + " = pathForAllFiles\n\tpathForAllFiles=\"$SCRIPT_PATH$\"\n\ttry{\n\t\t"
        + cmd + "\n\t}catch(e){" + strCatch + "}\n\tpathForAllFiles = " + vname
        + "\n}\n";
  }

  public Object createZipSet(FileManager fm, Viewer viewer, String fileName,
                             String script, String[] scripts,
                             boolean includeRemoteFiles) {
    JmolList<Object> v = new  JmolList<Object>();
    JmolList<String> fileNames = new  JmolList<String>();
    Hashtable<Object, String> crcMap = new Hashtable<Object, String>();
    boolean haveSceneScript = (scripts != null && scripts.length == 3 && scripts[1]
        .startsWith(SCENE_TAG));
    boolean sceneScriptOnly = (haveSceneScript && scripts[2].equals("min"));
    if (!sceneScriptOnly) {
      JmolBinary.getFileReferences(script, fileNames);
      if (haveSceneScript)
        JmolBinary.getFileReferences(scripts[1], fileNames);
    }
    boolean haveScripts = (!haveSceneScript && scripts != null && scripts.length > 0);
    if (haveScripts) {
      script = wrapPathForAllFiles("script " + Escape.eS(scripts[0]), "");
      for (int i = 0; i < scripts.length; i++)
        fileNames.addLast(scripts[i]);
    }
    int nFiles = fileNames.size();
    if (fileName != null)
      fileName = fileName.replace('\\', '/');
    String fileRoot = fileName;
    if (fileRoot != null) {
      fileRoot = fileName.substring(fileName.lastIndexOf("/") + 1);
      if (fileRoot.indexOf(".") >= 0)
        fileRoot = fileRoot.substring(0, fileRoot.indexOf("."));
    }
    JmolList<String> newFileNames = new  JmolList<String>();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name = fileNames.get(iFile);
      boolean isLocal = !viewer.isJS && FileManager.isLocal(name);
      String newName = name;
      // also check that somehow we don't have a local file with the same name as
      // a fixed remote file name (because someone extracted the files and then used them)
      if (isLocal || includeRemoteFiles) {
        int ptSlash = name.lastIndexOf("/");
        newName = (name.indexOf("?") > 0 && name.indexOf("|") < 0 ? TextFormat
            .replaceAllCharacters(name, "/:?\"'=&", "_") : FileManager
            .stripPath(name));
        newName = TextFormat.replaceAllCharacters(newName, "[]", "_");
        boolean isSparDir = (fm.spardirCache != null && fm.spardirCache
            .containsKey(name));
        if (isLocal && name.indexOf("|") < 0 && !isSparDir) {
          v.addLast(name);
          v.addLast(newName);
          v.addLast(null); // data will be gotten from disk
        } else {
          // all remote files, and any file that was opened from a ZIP collection
          Object ret = (isSparDir ? fm.spardirCache.get(name) : fm
              .getFileAsBytes(name, null, true));
          if (!Escape.isAB(ret))
            return ret;
          newName = addPngFileBytes(name, (byte[]) ret, iFile,
              crcMap, isSparDir, newName, ptSlash, v);
        }
        name = "$SCRIPT_PATH$" + newName;
      }
      crcMap.put(newName, newName);
      newFileNames.addLast(name);
    }
    if (!sceneScriptOnly) {
      script = TextFormat.replaceQuotedStrings(script, fileNames, newFileNames);
      v.addLast("state.spt");
      v.addLast(null);
      v.addLast(script.getBytes());
    }
    if (haveSceneScript) {
      if (scripts[0] != null) {
        v.addLast("animate.spt");
        v.addLast(null);
        v.addLast(scripts[0].getBytes());
      }
      v.addLast("scene.spt");
      v.addLast(null);
      script = TextFormat.replaceQuotedStrings(scripts[1], fileNames,
          newFileNames);
      v.addLast(script.getBytes());
    }
    String sname = (haveSceneScript ? "scene.spt" : "state.spt");
    v.addLast("JmolManifest.txt");
    v.addLast(null);
    String sinfo = "# Jmol Manifest Zip Format 1.1\n" + "# Created "
        + (new Date()) + "\n" + "# JmolVersion " + Viewer.getJmolVersion()
        + "\n" + sname;
    v.addLast(sinfo.getBytes());
    v.addLast("Jmol_version_"
        + Viewer.getJmolVersion().replace(' ', '_').replace(':', '.'));
    v.addLast(null);
    v.addLast(new byte[0]);
    if (fileRoot != null) {
      Object bytes = viewer.getImageAsWithComment("PNG", -1, -1, -1, null,
          null, null, JC.embedScript(script));
      if (Escape.isAB(bytes)) {
        v.addLast("preview.png");
        v.addLast(null);
        v.addLast(bytes);
      }
    }
    return JmolBinary.writeZipFile(fm, viewer, fileName, v, "OK JMOL");
  }

  public Object getAtomSetCollectionOrBufferedReaderFromZip(
                                                            JmolAdapter adapter,
                                                            InputStream is,
                                                            String fileName,
                                                            String[] zipDirectory,
                                                            Map<String, Object> htParams,
                                                            int subFilePtr,
                                                            boolean asBufferedReader,
                                                            boolean asBufferedInputStream) {

    // we're here because user is using | in a load file name
    // or we are opening a zip file.

    boolean doCombine = (subFilePtr == 1);
    htParams.put("zipSet", fileName);
    String[] subFileList = (String[]) htParams.get("subFileList");
    if (subFileList == null)
      subFileList = checkSpecialInZip(zipDirectory);
    String subFileName = (subFileList == null
        || subFilePtr >= subFileList.length ? null : subFileList[subFilePtr]);
    if (subFileName != null
        && (subFileName.startsWith("/") || subFileName.startsWith("\\")))
      subFileName = subFileName.substring(1);
    int selectedFile = 0;
    if (subFileName == null && htParams.containsKey("modelNumber")) {
      selectedFile = ((Integer) htParams.get("modelNumber")).intValue();
      if (selectedFile > 0 && doCombine)
        htParams.remove("modelNumber");
    }

    // zipDirectory[0] is the manifest if present
    String manifest = (String) htParams.get("manifest");
    boolean useFileManifest = (manifest == null);
    if (useFileManifest)
      manifest = (zipDirectory.length > 0 ? zipDirectory[0] : "");
    boolean haveManifest = (manifest.length() > 0);
    if (haveManifest) {
      if (Logger.debugging)
        Logger.info("manifest for  " + fileName + ":\n" + manifest);
    }
    boolean ignoreErrors = (manifest.indexOf("IGNORE_ERRORS") >= 0);
    boolean selectAll = (manifest.indexOf("IGNORE_MANIFEST") >= 0);
    boolean exceptFiles = (manifest.indexOf("EXCEPT_FILES") >= 0);
    if (selectAll || subFileName != null)
      haveManifest = false;
    if (useFileManifest && haveManifest) {
      String path = JmolBinary.getManifestScriptPath(manifest);
      if (path != null)
        return JmolAdapter.NOTE_SCRIPT_FILE + fileName + path + "\n";
    }
    JmolList<Object> vCollections = new  JmolList<Object>();
    Map<String, Object> htCollections = (haveManifest ? new Hashtable<String, Object>()
        : null);
    int nFiles = 0;
    // 0 entry is manifest

    // check for a Spartan directory. This is not entirely satisfying,
    // because we aren't reading the file in the proper sequence.
    // this code is a hack that should be replaced with the sort of code
    // running in FileManager now.

    Object ret = checkSpecialData(is, zipDirectory);
    if (ret instanceof String)
      return ret;
    SB data = (SB) ret;
    try {
      if (data != null) {
        BufferedReader reader = new BufferedReader(new StringReader(data
            .toString()));
        if (asBufferedReader) {
          return reader;
        }
        ret = adapter.getAtomSetCollectionFromReader(fileName, reader, htParams);
        if (ret instanceof String)
          return ret;
        if (ret instanceof AtomSetCollection) {
          AtomSetCollection atomSetCollection = (AtomSetCollection) ret;
          if (atomSetCollection.errorMessage != null) {
            if (ignoreErrors)
              return null;
            return atomSetCollection.errorMessage;
          }
          return atomSetCollection;
        }
        if (ignoreErrors)
          return null;
        return "unknown reader error";
      }
      if (is instanceof BufferedInputStream)
        is = JmolBinary.checkPngZipStream((BufferedInputStream) is);
      ZipInputStream zis = (ZipInputStream) JmolBinary.newZipInputStream(is);
      ZipEntry ze;
      if (haveManifest)
        manifest = '|' + manifest.replace('\r', '|').replace('\n', '|') + '|';
      while ((ze = zis.getNextEntry()) != null
          && (selectedFile <= 0 || vCollections.size() < selectedFile)) {
        if (ze.isDirectory())
          continue;
        String thisEntry = ze.getName();
        if (subFileName != null && !thisEntry.equals(subFileName))
          continue;
        if (subFileName != null)
          htParams.put("subFileName", subFileName);
        if (isJmolManifest(thisEntry) || haveManifest
            && exceptFiles == manifest.indexOf("|" + thisEntry + "|") >= 0)
          continue;
        byte[] bytes = JmolBinary.getStreamBytes(zis, ze.getSize());
//        String s = new String(bytes);
//        System.out.println("ziputil " + s.substring(0, 100));
        if (JmolBinary.isZipFile(bytes)) {
          BufferedInputStream bis = new BufferedInputStream(
              new ByteArrayInputStream(bytes));
          String[] zipDir2 = JmolBinary.getZipDirectoryAndClose(bis, true);
          bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
          Object atomSetCollections = getAtomSetCollectionOrBufferedReaderFromZip(
              adapter, bis, fileName + "|" + thisEntry, zipDir2, htParams,
              ++subFilePtr, asBufferedReader, asBufferedInputStream);
          if (atomSetCollections instanceof String) {
            if (ignoreErrors)
              continue;
            return atomSetCollections;
          } else if (atomSetCollections instanceof AtomSetCollection
              || atomSetCollections instanceof JmolList<?>) {
            if (haveManifest && !exceptFiles)
              htCollections.put(thisEntry, atomSetCollections);
            else
              vCollections.addLast(atomSetCollections);
          } else if (atomSetCollections instanceof BufferedReader) {
            if (doCombine)
              zis.close();
            return atomSetCollections; // FileReader has requested a zip file
            // BufferedReader
          } else {
            if (ignoreErrors)
              continue;
            zis.close();
            return "unknown zip reader error";
          }
        } else if (asBufferedInputStream) {
          if (JmolBinary.isGzipB(bytes))
            return getGzippedInputStream(bytes);
          BufferedInputStream bis = new BufferedInputStream(
              new ByteArrayInputStream(bytes));
          if (doCombine)
            zis.close();
          return bis;
        } else {
          String sData;
          if (JmolBinary.isCompoundDocumentArray(bytes)) {
            JmolDocument jd = (JmolDocument) Interface
                .getInterface("jmol.util.CompoundDocument");
            jd.setStream(new BufferedInputStream(
                new ByteArrayInputStream(bytes)), true);
            sData = jd.getAllDataFiles("Molecule", "Input").toString();
          } else if (JmolBinary.isGzipB(bytes)) {
            sData = JmolBinary.getGzippedBytesAsString(bytes);
          } else {
            sData = JmolBinary.fixUTF(bytes);
          }
          BufferedReader reader = new BufferedReader(new StringReader(sData));
          if (asBufferedReader) {
            if (doCombine)
              zis.close();
            return reader;
          }
          String fname = fileName + "|" + ze.getName();

          ret = adapter.getAtomSetCollectionFromReader(fname, reader, htParams);

          if (!(ret instanceof AtomSetCollection)) {
            if (ignoreErrors)
              continue;
            zis.close();
            return "" + ret;
          }
          if (haveManifest && !exceptFiles)
            htCollections.put(thisEntry, ret);
          else
            vCollections.addLast(ret);
          AtomSetCollection a = (AtomSetCollection) ret;
          if (a.errorMessage != null) {
            if (ignoreErrors)
              continue;
            zis.close();
            return a.errorMessage;
          }
        }
      }

      if (doCombine)
        zis.close();

      // if a manifest exists, it sets the files and file order

      if (haveManifest && !exceptFiles) {
        String[] list = TextFormat.split(manifest, '|');
        for (int i = 0; i < list.length; i++) {
          String file = list[i];
          if (file.length() == 0 || file.indexOf("#") == 0)
            continue;
          if (htCollections.containsKey(file))
            vCollections.addLast(htCollections.get(file));
          else if (Logger.debugging)
            Logger.info("manifested file " + file + " was not found in "
                + fileName);
        }
      }
      if (!doCombine)
        return vCollections;
      AtomSetCollection result = new AtomSetCollection("Array", null, null,
          vCollections);
      if (result.errorMessage != null) {
        if (ignoreErrors)
          return null;
        return result.errorMessage;
      }
      if (nFiles == 1)
        selectedFile = 1;
      if (selectedFile > 0 && selectedFile <= vCollections.size())
        return vCollections.get(selectedFile - 1);
      return result;

    } catch (Exception e) {
      if (ignoreErrors)
        return null;
      Logger.error("" + e);
      return "" + e;
    } catch (Error er) {
      Logger.errorEx(null, er);
      return "" + er;
    }
  }

  /**
   * called by SmarterJmolAdapter to see if we have a Spartan directory and, if so,
   * open it and get all the data into the correct order.
   * 
   * @param is
   * @param zipDirectory
   * @return String data for processing
   */  
  private static SB checkSpecialData(InputStream is, String[] zipDirectory) {
    boolean isSpartan = false;
    // 0 entry is not used here
    for (int i = 1; i < zipDirectory.length; i++) {
      if (zipDirectory[i].endsWith(".spardir/")
          || zipDirectory[i].indexOf("_spartandir") >= 0) {
        isSpartan = true;
        break;
      }
    }
    if (!isSpartan)
      return null;
    SB data = new SB();
    data.append("Zip File Directory: ").append("\n").append(
        Escape.escapeStrA(zipDirectory, true)).append("\n");
    Map<String, String> fileData = new Hashtable<String, String>();
    getAllZipDataStatic(is, new String[] {}, "", "Molecule", fileData);
    String prefix = "|";
    String outputData = fileData.get(prefix + "output");
    if (outputData == null)
      outputData = fileData.get((prefix = "|" + zipDirectory[1]) + "output");
    data.append(outputData);
    String[] files = getSpartanFileList(prefix, getSpartanDirs(outputData));
    for (int i = 2; i < files.length; i++) {
      String name = files[i];
      if (fileData.containsKey(name))
        data.append(fileData.get(name));
      else
        data.append(name + "\n");
    }
    return data;
  }

  /**
   * 
   * Special loading for file directories. This method is called from the
   * FileManager via SmarterJmolAdapter. It's here because Resolver is the place
   * where all distinctions are made.
   * 
   * In the case of spt files, no need to load them; here we are just checking
   * for type.
   * 
   * In the case of .spardir directories, we need to provide a list of the
   * critical files that need loading and concatenation for the
   * SpartanSmolReader.
   * 
   * we return an array for which:
   * 
   * [0] file type (class prefix) or null for SPT file [1] header to add for
   * each BEGIN/END block (ignored) [2...] files to load and concatenate
   * 
   * @param name
   * @param type
   * @return array detailing action for this set of files
   */
  public String[] spartanFileList(String name, String type) {
    // make list of required files
    String[] dirNums = getSpartanDirs(type);
    if (dirNums.length == 0 && name.endsWith(".spardir.zip")
        && type.indexOf(".zip|output") >= 0) {
      // try again, with the idea that 
      String sname = name.replace('\\', '/');
      int pt = name.lastIndexOf(".spardir");
      pt = sname.lastIndexOf("/");
      // mac directory zipped up?
      sname = name + "|" + name.substring(pt + 1, name.length() - 4);
      return new String[] { "SpartanSmol", sname, sname + "/output" };
    }
    return getSpartanFileList(name, dirNums);
  }

  /**
   * read the output file from the Spartan directory and decide from that what
   * files need to be read and in what order - usually M0001 or a set of Profiles.
   * But Spartan saves the Profiles in alphabetical order, not numerical. So we
   * fix that here.
   * 
   * @param outputFileData
   * @return String[] list of files to read
   */
  private static String[] getSpartanDirs(String outputFileData) {
    if (outputFileData == null)
      return new String[]{};
    if (outputFileData.startsWith("java.io.FileNotFoundException")
        || outputFileData.startsWith("FILE NOT FOUND")
        || outputFileData.indexOf("<html") >= 0)
      return new String[] { "M0001" };
    JmolList<String> v = new  JmolList<String>();
    String token;
    String lasttoken = "";
    try {
      StringTokenizer tokens = new StringTokenizer(outputFileData, " \t\r\n");
      while (tokens.hasMoreTokens()) {
        // profile file name is just before each right-paren:
        /*
         * MacSPARTAN '08 ENERGY PROFILE: x86/Darwin 130
         * 
         * Dihedral Move : C3 - C2 - C1 - O1 [ 4] -180.000000 .. 180.000000
         * Dihedral Move : C2 - C1 - O1 - H3 [ 4] -180.000000 .. 180.000000
         * 
         * 1 ) -180.00 -180.00 -504208.11982719 2 ) -90.00 -180.00
         * -504200.18593376
         * 
         * ...
         * 
         * 24 ) 90.00 180.00 -504200.18564495 25 ) 180.00 180.00
         * -504208.12129747
         * 
         * Found a local maxima E = -504178.25455465 [ 3 3 ]
         * 
         * 
         * Reason for exit: Successful completion Mechanics CPU Time : 1:51.42
         * Mechanics Wall Time: 12:31.54
         */
        if ((token = tokens.nextToken()).equals(")"))
          v.addLast(lasttoken);
        else if (token.equals("Start-") && tokens.nextToken().equals("Molecule"))
          v.addLast(TextFormat.split(tokens.nextToken(), '"')[1]);
        lasttoken = token;
      }
    } catch (Exception e) {
      //
    }
    return v.toArray(new String[v.size()]);
  }
  
  /**
   * returns the list of files to read for every Spartan spardir. Simple numbers
   * are assumed to be Profiles; others are models.
   * 
   * @param name
   * @param dirNums
   * @return String[] list of files to read given a list of directory names
   * 
   */
  private static String[] getSpartanFileList(String name, String[] dirNums) {    
    String[] files = new String[2 + dirNums.length*5];
    files[0] = "SpartanSmol";
    files[1] = "Directory Entry ";
    int pt = 2;
    name = name.replace('\\', '/');
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1);
    for (int i = 0; i < dirNums.length; i++) {
      String path = name + (Character.isDigit(dirNums[i].charAt(0)) ? 
          "/Profile." + dirNums[i] : "/" + dirNums[i]);
      files[pt++] = path + "/#JMOL_MODEL " + dirNums[i];
      files[pt++] = path + "/input";
      files[pt++] = path + "/archive";
      files[pt++] = path + "/Molecule:asBinaryString";
      files[pt++] = path + "/proparc";
    }
    return files;
  }

  /**
   * called by SmarterJmolAdapter to see if we can automatically assign a file
   * from the zip file. If so, return a subfile list for this file. The first
   * element of the list is left empty -- it would be the zipfile name. 
   * 
   * Assignment can be made if (1) there is only one file in the collection or
   * (2) if the first file is xxxx.spardir/
   * 
   * Note that __MACOS? files are ignored by the ZIP file reader.
   * 
   * @param zipDirectory
   * @return subFileList
   */
  static String[] checkSpecialInZip(String[] zipDirectory) {
    String name;
    return (zipDirectory.length < 2 ? null 
        : (name = zipDirectory[1]).endsWith(".spardir/") || zipDirectory.length == 2 ?
        new String[] { "",
          (name.endsWith("/") ? name.substring(0, name.length() - 1) : name) } 
        : null);
  }

  public byte[] getCachedPngjBytes(FileManager fm, String pathName) {
    if (pathName.indexOf(".png") < 0)
      return null;
    Logger.info("FileManager checking PNGJ cache for " + pathName);
    String shortName = shortSceneFilename(pathName);
    if (fm.pngjCache == null && !cachePngjFile(fm, new String[] { pathName, null }))
      return null;
    Map<String, byte[]> pngjCache = fm.pngjCache;
    boolean isMin = (pathName.indexOf(".min.") >= 0);
    if (!isMin) {
      String cName = fm.getCanonicalName(JmolBinary.getZipRoot(pathName));
      if (!pngjCache.containsKey(cName)
          && !cachePngjFile(fm, new String[] { pathName, null }))
        return null;
      if (pathName.indexOf("|") < 0)
        shortName = cName;
    }
    if (pngjCache.containsKey(shortName)) {
      Logger.info("FileManager using memory cache " + shortName);
      return pngjCache.get(shortName);
    }
    for (String key : pngjCache.keySet())
      System.out.println(key);
    System.out.println("FileManager memory cache (" + pngjCache.size()
        + ") did not find " + pathName + " as " + shortName);
    if (!isMin || !cachePngjFile(fm, new String[] { pathName, null }))
      return null;
    Logger.info("FileManager using memory cache " + shortName);
    return pngjCache.get(shortName);
  }

  public boolean cachePngjFile(FileManager fm, String[] data) {
    Map<String, byte[]> pngjCache = fm.pngjCache = new Hashtable<String, byte[]>();
    data[1] = null;
    if (data[0] == null)
      return false;
    data[0] = JmolBinary.getZipRoot(data[0]);
    String shortName = shortSceneFilename(data[0]);
    try {
      data[1] = cacheZipContents(
          JmolBinary.checkPngZipStream((BufferedInputStream) fm.getBufferedInputStreamOrErrorMessageFromName(
              data[0], null, false, false, null, false)), shortName, fm.pngjCache);
    } catch (Exception e) {
      return false;
    }
    if (data[1] == null)
      return false;
    byte[] bytes = data[1].getBytes();
    pngjCache.put(fm.getCanonicalName(data[0]), bytes); // marker in case the .all. file is changed
    if (shortName.indexOf("_scene_") >= 0) {
      pngjCache.put(shortSceneFilename(data[0]), bytes); // good for all .min. files of this scene set
      bytes = pngjCache.remove(shortName + "|state.spt");
      if (bytes != null) 
        pngjCache.put(shortSceneFilename(data[0] + "|state.spt"), bytes);
    }
    for (String key: pngjCache.keySet())
      System.out.println(key);
    return true;
  }

  private static String shortSceneFilename(String pathName) {
    int pt = pathName.indexOf("_scene_") + 7;
    if (pt < 7)
      return pathName;
    String s = "";
    if (pathName.endsWith("|state.spt")) {
      int pt1 = pathName.indexOf('.', pt);
      if (pt1 < 0)
        return pathName;
      s = pathName.substring(pt, pt1);
    }
    int pt2 = pathName.lastIndexOf("|");
    return pathName.substring(0, pt) + s
        + (pt2 > 0 ? pathName.substring(pt2) : "");
  }


}
