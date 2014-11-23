/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;
import java.util.Map;
import java.util.StringTokenizer;


import org.jmol.api.JmolAdapter;
import org.jmol.io.LimitedLineReader;
import org.jmol.util.Logger;
import org.jmol.util.Parser;


public class Resolver {

  private final static String classBase = "org.jmol.adapter.readers.";
  private final static String[] readerSets = new String[] {
    "cifpdb.", ";Cif;Pdb;",
    "molxyz.", ";Mol3D;Mol;Xyz;",
    "more.", ";BinaryDcd;Gromacs;Jcampdx;MdCrd;MdTop;Mol2;Pqr;P2n;TlsDataOnly;",
    "quantum.", ";Adf;Csf;Dgrid;GamessUK;GamessUS;Gaussian;GausianWfn;Jaguar;" +
                 "Molden;MopacGraphf;GenNBO;NWChem;Odyssey;Psi;Qchem;Spartan;SpartanSmol;" +
                 "WebMO;",
    "pymol.", ";PyMOL;",
    "simple.", ";Alchemy;Ampac;Cube;FoldingXyz;GhemicalMM;HyperChem;Jme;Mopac;MopacArchive;ZMatrix;", 
    "xtal.", ";Aims;Castep;Crystal;Dmol;Espresso;Gulp;MagRes;Shelx;Siesta;VaspOutcar;Wien2k;"
  };
  
  public final static String getReaderClassBase(String type) {
    String name = type + "Reader";
    if (type.startsWith("Xml"))
      return classBase + "xml." + name;
    String key = ";" + type + ";";
    for (int i = 1; i < readerSets.length; i += 2)
      if (readerSets[i].indexOf(key) >= 0)
        return classBase + readerSets[i - 1] + name;
    return classBase + "???." + name;
  }
  
  /**
   * From SmarterJmolAdapter.getFileTypeName(Object atomSetCollectionOrReader)
   * just return the file type with no exception issues
   * 
   * @param br
   * @return String file type
   */
  static String getFileType(BufferedReader br) {
    try {
      return determineAtomSetCollectionReader(br, false);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * the main method for reading files. Called from SmarterJmolAdapter when
   * reading a file, reading a set of files, or reading a ZIP file
   * 
   * @param fullName
   * @param type
   * @param bufferedReader
   * @param htParams
   * @param ptFile
   * @return an AtomSetCollection or a String error
   * @throws Exception
   */
  static Object getAtomCollectionReader(String fullName, String type,
                                        Object bufferedReader,
                                        Map<String, Object> htParams, int ptFile)
      throws Exception {
    AtomSetCollectionReader atomSetCollectionReader = null;
    String readerName;
    fullName = fullName.replace('\\', '/');
    String errMsg = null;
    if (type != null) {
      readerName = getReaderFromType(type);
      if (readerName == null)
        errMsg = "unrecognized file format type " + type;
      else
        Logger.info("The Resolver assumes " + readerName);
    } else {
      readerName = determineAtomSetCollectionReader((BufferedReader) bufferedReader, true);
      if (readerName.charAt(0) == '\n') {
        type = (String) htParams.get("defaultType");
        if (type != null) {
          // allow for MDTOP to specify default MDCRD
          type = getReaderFromType(type);
          if (type != null)
            readerName = type;
        }
      }
      if (readerName.charAt(0) == '\n')
        errMsg = "unrecognized file format for file " + fullName + "\n"
            + readerName;
      else if (readerName.equals("spt"))
        errMsg = JmolAdapter.NOTE_SCRIPT_FILE + fullName + "\n";
      else if (!fullName.equals("ligand"))
        Logger.info("The Resolver thinks " + readerName);
    }
    if (errMsg != null) {
      SmarterJmolAdapter.close(bufferedReader);
      return errMsg;
    }
    htParams.put("ptFile", Integer.valueOf(ptFile));
    if (ptFile <= 0)
      htParams.put("readerName", readerName);
    if (readerName.indexOf("Xml") == 0)
      readerName = "Xml";
    String className = null;
    Class<?> atomSetCollectionReaderClass;
    String err = null;
    try {
      try {
        className = getReaderClassBase(readerName);
        atomSetCollectionReaderClass = Class.forName(className);
        atomSetCollectionReader = (AtomSetCollectionReader) atomSetCollectionReaderClass
            .newInstance();
      } catch (Exception e) {
        err = "File reader was not found:" + className;
        Logger.error(err);
        return err;
      }
      return atomSetCollectionReader;
    } catch (Exception e) {
      err = "uncaught error in file loading for " + className;
      Logger.error(err);
      System.out.println(e.getMessage());
      return err;
    }
  }

  /**
   * a largely untested reader of the DOM - where in a browser there
   * is model actually in XML format already present on the page.
   * -- Egon Willighagen
   * 
   * @param DOMNode
   * @param htParams 
   * @return an AtomSetCollection or a String error
   * @throws Exception
   */
  static Object DOMResolve(Object DOMNode, Map<String, Object> htParams) throws Exception {
    String className = null;
    Class<?> atomSetCollectionReaderClass;
    AtomSetCollectionReader atomSetCollectionReader; 
    String atomSetCollectionReaderName = getXmlType((String) htParams.get("nameSpaceInfo"));
    if (Logger.debugging) {
      Logger.debug("The Resolver thinks " + atomSetCollectionReaderName);
    }
    htParams.put("readerName", atomSetCollectionReaderName);
    try {
      className = classBase + "xml.XmlReader";
      atomSetCollectionReaderClass = Class.forName(className);
      atomSetCollectionReader = (AtomSetCollectionReader) atomSetCollectionReaderClass.newInstance();
      return atomSetCollectionReader;
    } catch (Exception e) {
      String err = "File reader was not found:" + className;
      Logger.errorEx(err, e);
      return err;
    }
  }

  private static final String CML_NAMESPACE_URI = "http://www.xml-cml.org/schema";

  /**
   * the main resolver method. One of the great advantages of Jmol is that it can
   * smartly determine a file type from its contents. In cases where this is not possible,
   * one can force a file type using a prefix to a filename. For example:
   * 
   * load mol2::xxxx.whatever
   * 
   * This is only necessary for a few file types, where only numbers are involved --
   * molecular dynamics coordinate files, for instance (mdcrd).
   * 
   * We must do this in a very specific order. DON'T MESS WITH THIS!
   * 
   * @param bufferedReader
   * @param returnLines
   * @return readerName or a few lines, if requested, or null
   * @throws Exception
   */
  private static String determineAtomSetCollectionReader(BufferedReader bufferedReader, boolean returnLines)
      throws Exception {
    LimitedLineReader llr = new LimitedLineReader(bufferedReader, 16384);
    
    // first check just the first 64 bytes
    
    String leader = llr.getHeader(LEADER_CHAR_MAX).trim();

    for (int i = 0; i < fileStartsWithRecords.length; ++i) {
      String[] recordTags = fileStartsWithRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (leader.startsWith(recordTag))
          return recordTags[0];
      }
    }

    // PNG or BCD-encoded JPG or JPEG
    if (leader.indexOf("PNG") == 1 && leader.indexOf("PNGJ") >= 0)
      return "pngj"; // presume appended JMOL file
    if (leader.indexOf("PNG") == 1 || leader.indexOf("JPG") == 1
        || leader.indexOf("JFIF") == 6)
      return "spt"; // presume embedded script --- allows dragging into Jmol
    if (leader.startsWith("##TITLE"))
      return "Jcampdx";

    // now allow identification in first 16 lines
    // excluding those starting with "#"
    
    String[] lines = new String[16];
    int nLines = 0;
    for (int i = 0; i < lines.length; ++i) {
      lines[i] = llr.readLineWithNewline();
      if (lines[i].length() > 0)
        nLines++;
    }

    String readerName;
    if ((readerName = checkSpecial(nLines, lines, false)) != null)
      return readerName;

    if ((readerName = checkLineStarts(lines)) != null)
      return readerName;

    if ((readerName = checkHeaderContains(llr.getHeader(0))) != null)
      return readerName;

    if ((readerName = checkSpecial(nLines, lines, true)) != null)
      return readerName;
    
    return (returnLines ? "\n" + lines[0] + "\n" + lines[1] + "\n" + lines[2] + "\n" : null);
  }

  private static String checkHeaderContains(String header) throws Exception {
    for (int i = 0; i < headerContainsRecords.length; ++i) {
      String[] recordTags = headerContainsRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (header.indexOf(recordTag) < 0)
          continue;
        String type = recordTags[0];
        // for XML check for an error message from a server -- certainly not XML
        // but new CML format includes xmlns:xhtml="http://www.w3.org/1999/xhtml" in <cml> tag.
        return (!type.equals("Xml") ? type 
            : header.indexOf("<!DOCTYPE HTML PUBLIC") < 0
              && header.indexOf("XHTML") < 0 
              && (header.indexOf("xhtml") < 0 || header.indexOf("<cml") >= 0) 
              ? getXmlType(header) 
            : null);
      }
    }
    return null;
  }

  private static String checkLineStarts(String[] lines) {
    for (int i = 0; i < lineStartsWithRecords.length; ++i) {
      String[] recordTags = lineStartsWithRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        for (int k = 0; k < lines.length; ++k) {
          if (lines[k].startsWith(recordTag))
            return recordTags[0];
        }
      }
    }
    return null;
  }

  private static String getXmlType(String header) throws Exception  {
    if (header.indexOf("http://www.molpro.net/") >= 0) {
      return specialTags[SPECIAL_MOLPRO_XML][0];
    }
    if (header.indexOf("odyssey") >= 0) {
      return specialTags[SPECIAL_ODYSSEY_XML][0];
    }
    if (header.indexOf("C3XML") >= 0) {
      return specialTags[SPECIAL_CHEM3D_XML][0];
    }
    if (header.indexOf("arguslab") >= 0) {
      return specialTags[SPECIAL_ARGUS_XML][0];
    }
    if (header.indexOf("jvxl") >= 0) {
      return specialTags[SPECIAL_CML_XML][0];
    }
    if (header.indexOf(CML_NAMESPACE_URI) >= 0
        || header.indexOf("cml:") >= 0) {
      return specialTags[SPECIAL_CML_XML][0];
    }
    if (header.indexOf("XSD") >= 0) {
      return specialTags[SPECIAL_XSD_XML][0];
    }
    if (header.indexOf(">vasp") >= 0) {
      return specialTags[SPECIAL_VASP_XML][0];
    }
    if (header.indexOf("<GEOMETRY_INFO>") >= 0) {
      return specialTags[SPECIAL_QE_XML][0];
    }
    
    return specialTags[SPECIAL_CML_XML][0] + "(unidentified)";
  }

  private final static int SPECIAL_JME                = 0;
  private final static int SPECIAL_MOPACGRAPHF        = 1;
  //private final static int SPECIAL_MOL3D              = 2; // only by MOL3D::
  private final static int SPECIAL_ODYSSEY            = 3;
  private final static int SPECIAL_MOL                = 4;
  private final static int SPECIAL_XYZ                = 5;
  private final static int SPECIAL_FOLDINGXYZ         = 6;
  private final static int SPECIAL_CUBE               = 7;
  private final static int SPECIAL_ALCHEMY            = 8;
  private final static int SPECIAL_WIEN               = 9;
  private final static int SPECIAL_CASTEP             = 10;
  private final static int SPECIAL_AIMS               = 11;
  private final static int SPECIAL_CRYSTAL            = 12;
  private final static int SPECIAL_GROMACS            = 13;
  private final static int SPECIAL_GENNBO             = 14;
  
  // these next are needed by the XML reader
  
  public final static int SPECIAL_ARGUS_XML   = 15;
  public final static int SPECIAL_CML_XML     = 16;
  public final static int SPECIAL_CHEM3D_XML  = 17;
  public final static int SPECIAL_MOLPRO_XML  = 18;
  public final static int SPECIAL_ODYSSEY_XML = 19;
  public final static int SPECIAL_XSD_XML     = 20;
  public final static int SPECIAL_VASP_XML    = 21; 
  public final static int SPECIAL_QE_XML      = 22; 
 
  public final static int SPECIAL_ARGUS_DOM   = 23;
  public final static int SPECIAL_CML_DOM     = 24;
  public final static int SPECIAL_CHEM3D_DOM  = 25;
  public final static int SPECIAL_MOLPRO_DOM  = 26;
  public final static int SPECIAL_ODYSSEY_DOM = 27;
  public final static int SPECIAL_XSD_DOM     = 28; // not implemented
  public final static int SPECIAL_VASP_DOM    = 29; 
  
  public final static String[][] specialTags = {
    { "Jme" },
    { "MopacGraphf" },
    { "Mol3D" },
    { "Odyssey" },
    { "Mol" },
    
    { "Xyz" },
    { "FoldingXyz" },
    { "Cube" },
    { "Alchemy" },
    { "Wien2k" },
    
    { "Castep" },
    { "Aims" },  
    { "Crystal" },  

    { "Gromacs" },
    { "GenNBO" },
    
    { "XmlArgus" }, 
    { "XmlCml" },
    { "XmlChem3d" },
    { "XmlMolpro" },
    { "XmlOdyssey" },
    { "XmlXsd" },
    { "XmlVasp" },
    { "XmlQE" },

    { "XmlArgus(DOM)" }, //19
    { "XmlCml(DOM)" },
    { "XmlChem3d(DOM)" },
    { "XmlMolpro(DOM)" },
    { "XmlOdyssey(DOM)" },
    { "XmlXsd(DOM)" },
    { "XmlVasp(DOM)" },

    { "MdCrd" }

  };

  private final static String getReaderFromType(String type) {
    type = type.toLowerCase();
    String base = null;
    if ((base = checkType(specialTags, type)) != null)
      return base;
    if ((base = checkType(fileStartsWithRecords, type)) != null)
      return base;
    if ((base = checkType(lineStartsWithRecords, type)) != null)
      return base;
    return checkType(headerContainsRecords, type);
  }
  
  private final static String checkType(String[][] typeTags, String type) {
    for (int i = 0; i < typeTags.length; ++i)
      if (typeTags[i][0].toLowerCase().equals(type))
        return typeTags[i][0];
    return null;
  }
  
  ////////////////////////////////////////////////////////////////
  // file types that need special treatment
  ////////////////////////////////////////////////////////////////

  private final static String checkSpecial(int nLines, String[] lines,
                                           boolean isEnd) {
    // the order here is CRITICAL
    
    if (isEnd) {
      if (checkGromacs(lines))
        return specialTags[SPECIAL_GROMACS][0];
      if (checkCrystal(lines))
        return specialTags[SPECIAL_CRYSTAL][0];
      if (checkCastep(lines))
        return specialTags[SPECIAL_CASTEP][0];
    } else {
      if (nLines == 1 && lines[0].length() > 0
          && Character.isDigit(lines[0].charAt(0)))
        return specialTags[SPECIAL_JME][0]; //only one line, and that line starts with a number 
      if (checkMopacGraphf(lines))
        return specialTags[SPECIAL_MOPACGRAPHF][0]; //must be prior to checkFoldingXyz and checkMol
      if (checkOdyssey(lines))
        return specialTags[SPECIAL_ODYSSEY][0];
      if (checkMol(lines))
        return specialTags[SPECIAL_MOL][0];
      if (checkXyz(lines))
        return specialTags[SPECIAL_XYZ][0];
      if (checkAlchemy(lines[0]))
        return specialTags[SPECIAL_ALCHEMY][0];
      if (checkFoldingXyz(lines))
        return specialTags[SPECIAL_FOLDINGXYZ][0];
      if (checkCube(lines))
        return specialTags[SPECIAL_CUBE][0];
      if (checkWien2k(lines))
        return specialTags[SPECIAL_WIEN][0];
      if (checkAims(lines))
        return specialTags[SPECIAL_AIMS][0];
      if (checkGenNBO(lines))
        return specialTags[SPECIAL_GENNBO][0];
    }
    return null;
  }
  
  private static boolean checkAims(String[] lines) {

    // use same tokenizing mechanism as in AimsReader.java to also recognize
    // AIMS geometry files with indented keywords
    // use same tokenizing mechanism as in AimsReader.java 
    //  to reliably recognize FHI-aims files
    // "atom" is a VERY generic term; just "atom" breaks HIN reader. 
    // >= token.length are necessary to allow for comments at the end of valid lines
    //  (as perfectly legal in simple Fortran list based IO) 
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("mol 1"))
        return false;  /* hin format also uses "atom " */
      String[] tokens = Parser.getTokens(lines[i]);
      if (tokens.length == 0)
        continue;
      if (tokens[0].startsWith("atom") && tokens.length >= 5
          || tokens[0].startsWith("multipole") && tokens.length >= 6
          || tokens[0].startsWith("lattice_vector") && tokens.length >= 4)
        return true;
    }
    return false;
  }

  private static boolean checkAlchemy(String line) {
    /*
    11 ATOMS,    12 BONDS,     0 CHARGES
    */
    int pt;
    if ((pt = line.indexOf("ATOMS")) >= 0 && line.indexOf("BONDS") > pt)
      try {
        int n = Integer.parseInt(line.substring(0, pt).trim());
        return (n > 0);
      } catch (NumberFormatException nfe) {
        // ignore
      }
    return false;
  }

  private static boolean checkCastep(String[] lines) {
    for ( int i = 0; i<lines.length; i++ ) {
      if (lines[i].indexOf("Frequencies in         cm-1") == 1
          || lines[i].contains("CASTEP")
          || lines[i].toUpperCase().startsWith("%BLOCK LATTICE_ABC")
          || lines[i].toUpperCase().startsWith("%BLOCK LATTICE_CART")
          || lines[i].toUpperCase().startsWith("%BLOCK POSITIONS_FRAC")
          || lines[i].toUpperCase().startsWith("%BLOCK POSITIONS_ABS") 
          || lines[i].contains("<-- E")) return true;
    }
    return false;
  }

  private static boolean checkCrystal(String[] lines) {
    String s = lines[1].trim();
    if (s.equals("SLAB") ||s.equals("MOLECULE")
        || s.equals("CRYSTAL")
        || s.equals("POLYMER") || (s = lines[3]).equals("SLAB")
        || s.equals("MOLECULE") || s.equals("POLYMER"))
      return true;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].trim().equals("OPTGEOM"))
        return true;
    }
    return false;
  }
  
  private static boolean checkCube(String[] lines) {
    try {
      StringTokenizer tokens2 = new StringTokenizer(lines[2]);
      if (tokens2.countTokens() != 4)
        return false;
      Integer.parseInt(tokens2.nextToken());
      for (int i = 3; --i >= 0; )
        Parser.fVal(tokens2.nextToken());
      StringTokenizer tokens3 = new StringTokenizer(lines[3]);
      if (tokens3.countTokens() != 4)
        return false;
      Integer.parseInt(tokens3.nextToken());
      for (int i = 3; --i >= 0; )
        if (Parser.fVal(tokens3.nextToken()) < 0)
          return false;
      return true;
    } catch (NumberFormatException nfe) {
    }
    return false;
  }

  /**
   * @param lines First lines of the files.
   * @return Indicates if the file may be a Folding@Home file.
   */
  private static boolean checkFoldingXyz(String[] lines) {
    // Checking first line: <number of atoms> <protein name>
    StringTokenizer tokens = new StringTokenizer(lines[0].trim(), " \t");
    if (tokens.countTokens() < 2)
      return false;
    try {
      Integer.parseInt(tokens.nextToken().trim());
    } catch (NumberFormatException nfe) {
      return false;
    }
    
    // Checking second line: <atom number> ...
    String secondLine = lines[1].trim();
    if (secondLine.length() == 0)
        secondLine = lines[2].trim();
    tokens = new StringTokenizer(secondLine, " \t");
    if (tokens.countTokens() == 0)
      return false;
    try {
      Integer.parseInt(tokens.nextToken().trim());
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }
  
  private static boolean checkGenNBO(String[] lines) {
    // .31-.41 file or .nbo file
    return (lines[1].startsWith(" Basis set information needed for plotting orbitals")
      || lines[1].indexOf("s in the AO basis:") >= 0
      || lines[2].indexOf(" N A T U R A L   A T O M I C   O R B I T A L") >= 0);
  }
  
  private static boolean checkGromacs(String[] lines) {
    if (Parser.parseInt(lines[1]) == Integer.MIN_VALUE)
      return false;
    int len = -1;
    for (int i = 2; i < 16 && len != 0; i++)
      if ((len = lines[i].length()) != 69 && len != 45 && len != 0)
        return false;
    return true;
  }

  private static boolean checkMol(String[] lines) {
    String line4trimmed = ("X" + lines[3]).trim().toUpperCase();
    if (line4trimmed.length() < 7 || line4trimmed.indexOf(".") >= 0)
      return false;
    if (line4trimmed.endsWith("V2000") || line4trimmed.endsWith("V3000"))
      return true;
    try {
      int n1 = Integer.parseInt(lines[3].substring(0, 3).trim());
      int n2 = Integer.parseInt(lines[3].substring(3, 6).trim());
      return (n1 > 0 && n2 >= 0 && lines[0].indexOf("@<TRIPOS>") != 0
          && lines[1].indexOf("@<TRIPOS>") != 0 
          && lines[2].indexOf("@<TRIPOS>") != 0);
    } catch (NumberFormatException nfe) {
    }
    return false;
  }

  /**
   * @param lines First lines of the files.
   * @return Indicates if the file is a Mopac GRAPHF output file.
   */
  
  private static boolean checkMopacGraphf(String[] lines) {
    return (lines[0].indexOf("MOPAC-Graphical data") > 2); //nAtoms MOPAC-Graphical data
  }

  private static boolean checkOdyssey(String[] lines) {
    int i;
    for (i = 0; i < lines.length; i++)
      if (!lines[i].startsWith("C ") && lines[i].length() != 0)
        break;
    if (i >= lines.length 
        || lines[i].charAt(0) != ' ' 
        || (i = i + 2) + 1 >= lines.length)
      return false;
    try {
      // distinguishing between Spartan input and MOL file
      // MOL files have aaabbb.... on the data line
      // SPIN files have cc s on that line (c = charge; s = spin)
      // so the typical MOL file, with more parameters, will fail getting the spin
      int spin = Integer.parseInt(lines[i].substring(2).trim());
      int charge = Integer.parseInt(lines[i].substring(0, 2).trim());
      // and if it does not, then we get the next lines of info
      int atom1 = Integer.parseInt(lines[++i].substring(0, 2).trim());
      if (spin < 0 || spin > 5 || atom1 <= 0 || charge > 5)
        return false;
      // hard to believe we would get here for a MOL file
      float[] atomline = AtomSetCollectionReader.getTokensFloat(lines[i], null, 5);
      return !Float.isNaN(atomline[1]) && !Float.isNaN(atomline[2]) && !Float.isNaN(atomline[3]) && Float.isNaN(atomline[4]);
    } catch (Exception e) {
    }
    return false;
  }
  
 private static boolean checkWien2k(String[] lines) {
   return (lines[2].startsWith("MODE OF CALC=") 
       || lines[2].startsWith("             RELA")
       || lines[2].startsWith("             NREL"));
 }
 
  private static boolean checkXyz(String[] lines) {
    try {
      Integer.parseInt(lines[0].trim());
      return true;
    } catch (NumberFormatException nfe) {
    }
    return false;
  }


/*
  private void dumpLines(String[] lines) {
      for (int i = 0; i < lines.length; i++) {
        Logger.info("\nLine "+i + " len " + lines[i].length());
        for (int j = 0; j < lines[i].length(); j++)
          Logger.info("\t"+(int)lines[i].charAt(j));
      }
      Logger.info("");
  }

*/
  
  ////////////////////////////////////////////////////////////////
  // these test files that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  private final static int LEADER_CHAR_MAX = 64;
  
  private final static String[] sptContainsRecords = 
  { "spt", "# Jmol state", "# Jmol script" };
  
  private final static String[] cubeFileStartRecords =
  {"Cube", "JVXL", "#JVXL"};

  private final static String[] mol2Records =
  {"Mol2", "mol2", "@<TRIPOS>"};

  private final static String[] webmoFileStartRecords =
  {"WebMO", "[HEADER]"};
  
  private final static String[] moldenFileStartRecords =
  {"Molden", "[Molden"};

  private final static String[] dcdFileStartRecords =
  {"BinaryDcd", "T\0\0\0CORD", "\0\0\0TCORD"};

  private final static String[] tlsDataOnlyFileStartRecords =
  {"TlsDataOnly", "REFMAC\n\nTL", "REFMAC\r\n\r\n", "REFMAC\r\rTL"};
  
  private final static String[] zMatrixFileStartRecords =
  {"ZMatrix", "#ZMATRIX"};
  
  private final static String[] magResFileStartRecords =
  {"MagRes", "# magres"};

  private final static String[] pymolStartRecords =
  {"PyMOL", "}q" };

  private final static String[][] fileStartsWithRecords =
  { sptContainsRecords, cubeFileStartRecords, mol2Records, webmoFileStartRecords, 
    moldenFileStartRecords, dcdFileStartRecords, tlsDataOnlyFileStartRecords,
    zMatrixFileStartRecords, magResFileStartRecords, pymolStartRecords };

  ////////////////////////////////////////////////////////////////
  // these test lines that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  private final static String[] pqrLineStartRecords = 
  { "Pqr", "REMARK   1 PQR" };

  private final static String[] p2nLineStartRecords = 
  { "P2n", "REMARK   1 P2N" };

  private final static String[] pdbLineStartRecords = {
    "Pdb", "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK ",
    "DBREF ", "SEQADV", "SEQRES", "MODRES", 
    "HELIX ", "SHEET ", "TURN  ",
    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",
    "ATOM  ", "HETATM", "MODEL ", "LINK  ",
  };

  private final static String[] shelxLineStartRecords =
  { "Shelx", "TITL ", "ZERR ", "LATT ", "SYMM ", "CELL " };

  private final static String[] cifLineStartRecords =
  { "Cif", "data_", "_publ" };

  private final static String[] ghemicalMMLineStartRecords =
  { "GhemicalMM", "!Header mm1gp", "!Header gpr" };

  private final static String[] jaguarLineStartRecords =
  { "Jaguar", "  |  Jaguar version", };

  private final static String[] mdlLineStartRecords = 
  { "Mol", "$MDL " };

  private final static String[] spartanSmolLineStartRecords =
  { "SpartanSmol", "INPUT=" };

  private final static String[] csfLineStartRecords =
  { "Csf", "local_transform" };
  
  private final static String[] mdTopLineStartRecords =
  { "MdTop", "%FLAG TITLE" };
  
  private final static String[] hyperChemLineStartRecords = 
  { "HyperChem", "mol 1" };

  private final static String[] vaspOutcarLineStartRecords = 
  { "VaspOutcar", " vasp.", " INCAR:" };
  
  private final static String[][] lineStartsWithRecords =
  { cifLineStartRecords, pqrLineStartRecords, p2nLineStartRecords,
    pdbLineStartRecords, shelxLineStartRecords, 
    ghemicalMMLineStartRecords, jaguarLineStartRecords, 
    mdlLineStartRecords, spartanSmolLineStartRecords, csfLineStartRecords, 
    mol2Records, mdTopLineStartRecords, hyperChemLineStartRecords,
    vaspOutcarLineStartRecords
    };

  

  ////////////////////////////////////////////////////////////////
  // contains formats
  ////////////////////////////////////////////////////////////////

  private final static String[] xmlContainsRecords = 
  { "Xml", "<?xml", "<atom", "<molecule", "<reaction", "<cml", "<bond", ".dtd\"",
    "<list>", "<entry", "<identifier", "http://www.xml-cml.org/schema/cml2/core" };

  private final static String[] gaussianContainsRecords =
  { "Gaussian", "Entering Gaussian System", "Entering Link 1", "1998 Gaussian, Inc." };

  /*
  private final static String[] gaussianWfnRecords =
  { "GaussianWfn", "MO ORBITALS" };
  */

  private final static String[] ampacContainsRecords =
  { "Ampac", "AMPAC Version" };
  
  private final static String[] mopacContainsRecords =
  { "Mopac", "MOPAC 93 (c) Fujitsu", 
    "MOPAC FOR LINUX (PUBLIC DOMAIN VERSION)",
    "MOPAC:  VERSION  6", "MOPAC   7", "MOPAC2", "MOPAC (PUBLIC" };

  private final static String[] qchemContainsRecords = 
  { "Qchem", "Welcome to Q-Chem", "A Quantum Leap Into The Future Of Chemistry" };

  private final static String[] gamessUKContainsRecords =
  { "GamessUK", "GAMESS-UK", "G A M E S S - U K" };

  private final static String[] gamessUSContainsRecords =
  { "GamessUS", "GAMESS" };

  private final static String[] spartanBinaryContainsRecords =
  { "SpartanSmol" , "|PropertyArchive", "_spartan", "spardir", "BEGIN Directory Entry Molecule" };

  private final static String[] spartanContainsRecords =
  { "Spartan", "Spartan" };  // very old Spartan files?

  private final static String[] adfContainsRecords =
  { "Adf", "Amsterdam Density Functional" };
  
  private final static String[] dgridContainsRecords =
  { "Dgrid", "BASISFILE   created by DGrid" };
  
  private final static String[] dmolContainsRecords =
  { "Dmol", "DMol^3" };

  private final static String[] gulpContainsRecords =
  { "Gulp", "GENERAL UTILITY LATTICE PROGRAM" };
  
  private final static String[] psiContainsRecords =
  { "Psi", "    PSI  3", "PSI3:"};
 
  private final static String[] nwchemContainsRecords =
  { "NWChem", " argument  1 = "};

  private final static String[] uicrcifContainsRecords =
  { "Cif", "Crystallographic Information File"};
  
  private final static String[] crystalContainsRecords =
  { "Crystal", "*                                CRYSTAL"};

  private final static String[] espressoContainsRecords =
  { "Espresso", "Program PWSCF", "Program PHONON" }; 

  private final static String[] siestaContainsRecords =
  { "Siesta", "MD.TypeOfRun", "SolutionMethod", "MeshCutoff", 
    "WELCOME TO SIESTA" };

  private final static String[] mopacArchiveContainsRecords =
  { "MopacArchive", "SUMMARY OF PM" };
  
  
  private final static String[][] headerContainsRecords =
  { sptContainsRecords, xmlContainsRecords, gaussianContainsRecords, 
    ampacContainsRecords, mopacContainsRecords, qchemContainsRecords, 
    gamessUKContainsRecords, gamessUSContainsRecords,
    spartanBinaryContainsRecords, spartanContainsRecords, mol2Records, adfContainsRecords, psiContainsRecords,
    nwchemContainsRecords, uicrcifContainsRecords, dgridContainsRecords, crystalContainsRecords, 
    dmolContainsRecords, gulpContainsRecords, espressoContainsRecords, siestaContainsRecords,
    mopacArchiveContainsRecords
  };
}

