/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.script;

import org.jmol.util.JmolList;
import java.util.Arrays;
import java.util.Hashtable;

import java.util.Map;

//import javax.vecmath.Point3f;
//import javax.vecmath.Point4f;
//import javax.vecmath.Vector3f;

import org.jmol.util.ArrayUtil;
//import org.jmol.util.Escape;
import org.jmol.util.Logger;
//import org.jmol.util.Measure;

public class T {
  public int tok;
  public Object value;
  public int intValue = Integer.MAX_VALUE;

  //integer tokens or have a value that is (more likely to be) non-null
  //null token values can cause problems in Eval.statementAsString()
  public T(int tok) {
    this.tok = tok;
  }

  public final static T t(int tok, int intValue, Object value) {
    T token = new T(tok);
    token.intValue = intValue;
    token.value = value;
    return token;
  }
 
  public final static T o(int tok, Object value) {
    T token = new T(tok);
    token.value = value;
    return token;
  }

  public final static T n(int tok, int intValue) {
    T token = new T(tok);
    token.intValue = intValue;
    return token;
  }

  public final static T i(int intValue) {
    T token = new T(integer);
    token.intValue = intValue;
    return token;
  }

  public final static int nada       =  0;
  public final static int integer    =  2;
  public final static int decimal    =  3;
  public final static int string     =  4;
  
  final static int seqcode    =  5;
  final static int hash       =  6;  // associative array; Hashtable
  final static int varray     =  7;  // JmolList<ScriptVariable>
  final static int point3f    =  8;
  final static int point4f    =  9;  
  public final static int bitset     =  10;
  
  public final static int matrix3f   = 11;  
  public final static int matrix4f   = 12;  
  // listf "list-float" is specifically for xxx.all.bin, 
  // but it could be developed further
  final static int listf             = 13;     
  final private static int keyword   = 14;
  

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string",
    "seqcode", "hash", "array", "point", "point4", "bitset",
    "matrix3f",  "matrix4f", "listf", "keyword"
  };

  public static boolean tokAttr(int a, int b) {
    return (a & b) == (b & b);
  }
  
  public static boolean tokAttrOr(int a, int b1, int b2) {
    return (a & b1) == (b1 & b1) || (a & b2) == (b2 & b2);
  }
  
 

  // TOKEN BIT FIELDS
  
  // first 9 bits are generally identifier bits
  // or bits specific to a type
  
  /* bit flags:
   * 
   * parameter bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *    |   |   |   |   |   |   |     
   *  x                  xxxxxxxxxxx setparam  "set THIS ...."
   *  x     x                        strparam
   *  x    x                         intparam
   *  x   x                          floatparam
   *  x  x                           booleanparam
   * xx                              deprecatedparam
   * x                   xxxxxxxxxxx misc
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *                   x             sciptCommand
   *                  xx             atomExpressionCommand
   *                 x x           o implicitStringCommand (parsing of @{x})
   *                 x x           x implicitStringCommand (no initial parsing of @{x})
   *                x  x             mathExpressionCommand
   *               xx  x             flowCommand
   *              x    x             shapeCommand
   *             x                   noArgs
   *            x                    defaultON
   *                     xxxxxxxxxxx uniqueID (may include math flags)
   * 
   *              
   * math bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *    FFFF    FFFF    FFFF    FFFF
   *           x                     expression
   *          xx                     predefined set
   * x       x x                     atomproperty
   * x      xx x                     strproperty
   * x     x x x                     intproperty
   * x    x  x x                     floatproperty
   * x   x     x                     mathproperty
   *    x      x                     mathfunc
   *        
   *        
   *                           xxxxx unique id 1 to 0x1F (31)
   *                          x      min
   *                         x       max
   *                         xx      average
   *                        x        sum
   *                        x x      sum2
   *                        xx       stddev
   *                        xxx      selectedfloat  (including just the atoms selected)
   *                       x         fullfloat (including all atoms, not just selected)
   *                       x???      [available] 
   *                       xxxx      minmaxmask (all)
   *                     xx          maximum number of parameters for function
   *                    x            settable
   *                   
   * 3         2         1         0
   * 0987654321098765432109876543210
   *   x       x                     mathop
   *   x       x           x         comparator
   *                            xxxx unique id (0 to 15)
   *                        xxxx     precedence
   *
   *                        
   * 
   */
   
  //
  // parameter bit flags
  //
  
  final static int setparam          = (1 << 29); // parameter to set command
  final static int misc              = (1 << 30); // misc parameter
  final static int deprecatedparam   = setparam | misc;
  
  public final static int identifier =  misc;

  public final static int scriptCommand            = (1 << 12);
  
  // the command assumes an atom expression as the first parameter
  // -- center, define, delete, display, hide, restrict, select, subset, zap
  final static int atomExpressionCommand  = (1 << 13) | scriptCommand;
  
  // this implicitString flag indicates that then entire command is an implied quoted string  
  // -- ODD echo, hover, label, message, pause  -- do NOT parse variables the same way
  // -- EVEN help, javascript, cd, gotocmd -- allow for single starting variable
  final static int implicitStringCommand     = (1 << 14) | scriptCommand;
  
  // this implicitExpression flag indicates that phrases surrounded 
  // by ( ) should be considered the same as { }. 
  // -- elseif, forcmd, ifcmd, print, returncmd, set, var, whilecmd
  final static int mathExpressionCommand = (1 << 15) | scriptCommand;
  
  // program flow commands include:
  // -- breakcmd, continuecmd, elsecmd, elseif, end, endifcmd, switch, case, 
  //    forcmd, function, ifcmd, whilecmd
  final static int flowCommand        = (1 << 16) | mathExpressionCommand;

  // these commands will be handled specially
  final static int shapeCommand   = (1 << 17) | scriptCommand;

  // Command argument compile flags
  
  final static int noArgs         = (1 << 18);
  final static int defaultON      = (1 << 19);
  
  final static int expression           = (1 << 20);
  final static int predefinedset        = (1 << 21) | expression;
  
  public final static int atomproperty  = (1 << 22) | expression | misc; 
  // all atom properties are either a member of one of the next three groups,
  // or they are a point/vector, in which case they are just atomproperty
  public final static int strproperty   = (1 << 23) | atomproperty; // string property
  public final static int intproperty   = (1 << 24) | atomproperty; // int parameter
  public final static int floatproperty = (1 << 25) | atomproperty; // float parameter

  public final static int PROPERTYFLAGS = strproperty | intproperty | floatproperty;

  // parameters that can be set using the SET command
  public final static int strparam   = (1 << 23) | setparam; // string parameter
  public final static int intparam   = (1 << 24) | setparam; // int parameter
  public final static int floatparam = (1 << 25) | setparam; // float parameter
  public final static int booleanparam = (1 << 26) | setparam; // boolean parameter
  private final static int paramTypes = (strparam | intparam | floatparam | booleanparam);
  
  // note: the booleanparam and the mathproperty bits are the same, but there is no
  //       conflict because mathproperty is only checked in ScriptEvaluator.getBitsetProperty
  //       meaning it is coming after a "." as in {*}.min
  
  final static int mathproperty         = (1 << 26) | expression | misc; // {xxx}.nnnn
  final static int mathfunc             = (1 << 27) | expression;  
  final static int mathop               = (1 << 28) | expression;
  final static int comparator           = mathop | (1 << 8);
  
  public final static int center       = 1 | atomExpressionCommand;
  public final static int define       = 2 | atomExpressionCommand | expression;
  public final static int delete       = 3 | atomExpressionCommand;
  public final static int display      = 4 | atomExpressionCommand | deprecatedparam;
  final static int fixed        = 5 | atomExpressionCommand | expression; // Jmol 12.0.RC15
  final static int hide         = 6 | atomExpressionCommand;
  final static int restrict     = 7 | atomExpressionCommand;
//final static int select       see mathfunc
  final static int subset       = 8 | atomExpressionCommand | predefinedset;
  final static int zap          = 9 | atomExpressionCommand | expression;

  final static int print        = 1 | mathExpressionCommand;
  final static int returncmd    = 2 | mathExpressionCommand;
  final static int set          = 3 | mathExpressionCommand | expression;
  final static int var          = 4 | mathExpressionCommand;
  final static int log          = 5 | mathExpressionCommand;
  //final static int prompt     see mathfunc
  
  public final static int echo  = 1 /* must be odd */ | implicitStringCommand | shapeCommand | setparam;
  final static int help         = 2 /* must be even */ | implicitStringCommand;
  public final static int hover = 3 /* must be odd */ | implicitStringCommand | defaultON;
//final static int javascript   see mathfunc
//final static int label        see mathfunc
  final static int message      = 5 /* must be odd */ | implicitStringCommand;
  public final static int pause = 7 /* must be odd */ | implicitStringCommand;

  //these commands control flow
  //sorry about GOTO!
//final static int function     see mathfunc
//final static int ifcmd        see mathfunc
  final static int elseif       = 2 | flowCommand;
  final static int elsecmd      = 3 | flowCommand | noArgs;
  final static int endifcmd     = 4 | flowCommand | noArgs;
//final static int forcmd       see mathfunc
  final static int whilecmd     = 6 | flowCommand;
  final static int breakcmd     = 7 | flowCommand;
  final static int continuecmd  = 8 | flowCommand;
  final static int end          = 9 | flowCommand | expression;
  final static int switchcmd    = 10 | flowCommand;
  final static int casecmd      = 11 | flowCommand;
  final static int catchcmd     = 12 | flowCommand;
  final static int defaultcmd   = 13 | flowCommand;
  public final static int trycmd       = 14 | flowCommand | noArgs;
  
  final static int animation    = scriptCommand | 1;
  final static int assign       = scriptCommand | 2;
  final static int background   = scriptCommand | 3 | deprecatedparam;
  final static int bind         = scriptCommand | 4;
  final static int bondorder    = scriptCommand | 5;
  final static int calculate    = scriptCommand | 6;
//final static int cache        see mathfunc
  final static int cd           = scriptCommand | 8 /* must be even */| implicitStringCommand | expression; // must be even
  final static int centerAt     = scriptCommand | 9;
//final static int color        see intproperty
//final static int configuration see intproperty
  public final static int connect = scriptCommand | 10;
  final static int console      = scriptCommand | 11 | defaultON;
//final static int data         see mathfunc
  final static int delay        = scriptCommand | 13 | defaultON;
  public final static int depth = scriptCommand | 14 | intparam | defaultON;
  final static int exit         = scriptCommand | 15 | noArgs;
  final static int exitjmol     = scriptCommand | 16 | noArgs;
//final static int file         see intproperty
  final static int font         = scriptCommand | 18;
  public final static int frame        = scriptCommand | 19;
//final static int getproperty  see mathfunc
  final static int gotocmd      = scriptCommand | 20 /*must be even*/| implicitStringCommand;
  public final static int hbond = scriptCommand | 22 | deprecatedparam | expression | defaultON;
  final static int history      = scriptCommand | 23 | deprecatedparam;
  final static int initialize   = scriptCommand | 24 | noArgs;
  final static int invertSelected = scriptCommand | 25;
//final static int load         see mathfunc
  final static int loop         = scriptCommand | 26 | defaultON;
  final static int mapProperty  = scriptCommand | 28 | expression;
  final static int minimize     = scriptCommand | 30;
//final static int model        see mathfunc
//final static int measure      see mathfunc
  final static int move         = scriptCommand | 32;
  public final static int moveto = scriptCommand | 34;
  public final static int navigate = scriptCommand | 35;
//final static int quaternion   see mathfunc
  final static int parallel     = flowCommand   | 36;
  final static int plot         = scriptCommand | 37;
  final static int pop          = scriptCommand | 38 | noArgs; //internal only
  final static int process      = flowCommand   | 39;
//  final static int prompt     see mathfunc
  final static int push         = scriptCommand | 40 | noArgs; //internal only
  final static int quit         = scriptCommand | 41 | noArgs;
  final static int ramachandran = scriptCommand | 42 | expression;
  public final static int redomove = scriptCommand | 43;
  final static int refresh      = scriptCommand | 44 | noArgs;
  final static int reset        = scriptCommand | 45;
  final static int restore      = scriptCommand | 46;
  public final static int resume = scriptCommand | 47 | noArgs;
  public final static int rotate       = scriptCommand | 48 | defaultON;
  final static int rotateSelected = scriptCommand | 49;
  public final static int save  = scriptCommand | 50;
//final static int script   see mathfunc
  public final static int selectionhalos = scriptCommand | 51 | deprecatedparam | defaultON;
  final static int show         = scriptCommand | 52;
  public final static int slab  = scriptCommand | 53 | intparam | defaultON;
  final static int spin         = scriptCommand | 55 | deprecatedparam | defaultON;
  public final static int ssbond = scriptCommand | 56 | deprecatedparam | defaultON;
  final static int step         = scriptCommand | 58 | noArgs;
  final static int stereo       = scriptCommand | 59 | defaultON;
//final static int structure    see intproperty
  final static int sync         = scriptCommand | 60;
  final static int timeout      = scriptCommand | 62 | setparam;
  public final static int translate    = scriptCommand | 64;
  final static int translateSelected   = scriptCommand | 66;
  final static int unbind              = scriptCommand | 68;
  public final static int undomove     = scriptCommand | 69;
  public final static int vibration    = scriptCommand | 70;
  //final static int write   see mathfunc
  final static int zoom                = scriptCommand | 72;
  final static int zoomTo              = scriptCommand | 74;

  // shapes:
  
  public final static int axes         = shapeCommand | 2 | deprecatedparam | defaultON;
//final static int boundbox     see mathproperty
//final static int contact      see mathfunc
  public final static int dipole       = shapeCommand | 6;
  public final static int draw         = shapeCommand | 8;
  public final static int frank        = shapeCommand | 10 | deprecatedparam | defaultON;
  public final static int isosurface   = shapeCommand | 12;
  public final static int lcaocartoon  = shapeCommand | 14;
  public final static int measurements = shapeCommand | 16 | setparam;
  public final static int mo           = shapeCommand | 18 | expression;
  public final static int pmesh        = shapeCommand | 20;
  public final static int plot3d       = shapeCommand | 22;
  public final static int polyhedra    = shapeCommand | 24;
  //public final static int spacefill see floatproperty
  public final static int struts       = shapeCommand | 26 | defaultON | expression;
  public final static int unitcell     = shapeCommand | 28 | deprecatedparam | expression | predefinedset | defaultON;
  public final static int vector       = shapeCommand | 30;
  public final static int wireframe    = shapeCommand | 32 | defaultON;


  

  //
  // atom expression terms
  //
  
  final static int expressionBegin     = expression | 1;
  final static int expressionEnd       = expression | 2;
  public final static int all          = expression | 3;
  public final static int branch       = expression | 4;
  final static int coord               = expression | 6;
  final static int dollarsign          = expression | 7;
  final static int per                 = expression | 8;
  public final static int isaromatic   = expression | 9;
  final static int leftbrace           = expression | 10;
  public final static int none                = expression | 11;
  public final static int off          = expression | 12; //for within(dist,false,...)
  public final static int on           = expression | 13; //for within(dist,true,...)
  final static int rightbrace          = expression | 14;
  final static int semicolon           = expression | 15;

  // generated by compiler:
  
  public final static int spec_alternate       = expression | 31;
  public final static int spec_atom            = expression | 32;
  public final static int spec_chain           = expression | 33;
  public final static int spec_model           = expression | 34;  // /3, /4
  final static int spec_model2                 = expression | 35;  // 1.2, 1.3
  public final static int spec_name_pattern    = expression | 36;
  public final static int spec_resid           = expression | 37;
  public final static int spec_seqcode         = expression | 38;
  public final static int spec_seqcode_range   = expression | 39;

  final static int amino                = predefinedset | 2;
  public final static int dna           = predefinedset | 4;
  public final static int hetero        = predefinedset | 6 | deprecatedparam;
  final static int helixalpha           = predefinedset | 7;   // Jmol 12.1.14
  final static int helix310             = predefinedset | 8;   // Jmol 12.1.14
  final static int helixpi              = predefinedset | 10; 
  public final static int hydrogen      = predefinedset | 12 | deprecatedparam;
  public final static int nucleic       = predefinedset | 14;
  public final static int protein       = predefinedset | 16;
  public final static int purine        = predefinedset | 18;
  public final static int pyrimidine    = predefinedset | 20;
  public final static int rna           = predefinedset | 22;
  public final static int solvent       = predefinedset | 24 | deprecatedparam;
  public final static int sidechain     = predefinedset | 26;
  public final static int surface              = predefinedset | 28;
  final static int thismodel            = predefinedset | 30;
  public final static int sheet         = predefinedset | 32;
  public final static int spine         = predefinedset | 34;  // 11.9.34
  // these next are predefined in the sense that they are known quantities
  public final static int carbohydrate    = predefinedset | 36;
  final static int clickable              = predefinedset | 38;
  final static int displayed              = predefinedset | 40;
  public final static int hidden                 = predefinedset | 42;
  public final static int specialposition = predefinedset | 44;
  final static int visible                = predefinedset | 46;
  final static int basemodel              = predefinedset | 48;

  
  static int getPrecedence(int tokOperator) {
    return ((tokOperator >> 4) & 0xF);  
  }


  final static int leftparen    = 0 | mathop | 1 << 4;
  final static int rightparen   = 1 | mathop | 1 << 4;

  final static int opIf         = 1 | mathop | 2 << 4 | setparam;   // set ?
  final static int colon        = 2 | mathop | 2 << 4;

  final static int comma        = 0 | mathop | 3 << 4;

  final static int leftsquare   = 0 | mathop | 4 << 4;
  final static int rightsquare  = 1 | mathop | 4 << 4;

  final static int opOr         = 0 | mathop | 5 << 4;
  final static int opXor        = 1 | mathop | 5 << 4;
  public final static int opToggle = 2 | mathop | 5 << 4;

  final static int opAnd        = 0 | mathop | 6 << 4;
 
  final static int opNot        = 0 | mathop | 7 << 4;

  final static int opAND        = 0 | mathop | 8 << 4;

  final static int opGT         = 0 | comparator | 9 << 4;
  final static int opGE         = 1 | comparator | 9 << 4;
  final static int opLE         = 2 | comparator | 9 << 4;
  final static int opLT         = 3 | comparator | 9 << 4;
  public final static int opEQ  = 4 | comparator | 9 << 4;
  final static int opNE         = 6 | comparator | 9 << 4;
   
  final static int minus        = 0 | mathop | 10 << 4;
  final static int plus         = 1 | mathop | 10 << 4;
 
  final static int divide         = 0 | mathop | 11 << 4;
  final static int times          = 1 | mathop | 11 << 4;
  public final static int percent = 2 | mathop | 11 << 4;
  final static int leftdivide     = 3 | mathop | 11 << 4;  //   quaternion1 \ quaternion2
  
  final static int unaryMinus   = 0 | mathop | 12 << 4;
  final static int minusMinus   = 1 | mathop | 12 << 4;
  final static int plusPlus     = 2 | mathop | 12 << 4;
  final static int timestimes   = 3 | mathop | 12 << 4;
  
  
  final static int propselector = 1 | mathop | 13 << 4;

  final static int andequals    = 2 | mathop | 13 << 4;

  // these atom and math properties are invoked after a ".":
  // x.atoms
  // myset.bonds
  
  // .min and .max, .average, .sum, .sum2, .stddev, and .all 
  // are bitfields added to a preceding property selector
  // for example, x.atoms.max, x.atoms.all
  // .all gets incorporated as minmaxmask
  // .selectedfloat is a special flag used by mapPropety() and plot()
  // to pass temporary float arrays to the .bin() function
  // .allfloat is a special flag for colorShape() to get a full
  // atom float array
  
  final static int minmaxmask /*all*/ = 0xF << 5; 
  public final static int min           = 1 << 5;
  public final static int max           = 2 << 5;
  public final static int average       = 3 << 5;
  public final static int sum           = 4 << 5;
  public final static int sum2          = 5 << 5;
  public final static int stddev        = 6 << 5;
  public final static int selectedfloat = 7 << 5; //not user-selectable
  public final static int allfloat      = 8 << 5; //not user-selectable

  final static int settable           = 1 << 11;
  
  // bits 0 - 4 are for an identifier -- DO NOT GO OVER 31!
  // but, note that we can have more than 1 provided other parameters differ
  
  // ___.xxx math properties and all atom properties 
    
  public final static int atoms     = 1 | mathproperty;
  public final static int bonds     = 2 | mathproperty | deprecatedparam;
  final static int length           = 3 | mathproperty;
  final static int lines            = 4 | mathproperty;
  public final static int reverse   = 5 | mathproperty;
  final static int size             = 6 | mathproperty;
  public final static int type      = 8 | mathproperty;
  public final static int boundbox  = 9 | mathproperty | deprecatedparam | shapeCommand | defaultON;
  public final static int xyz       =10 | mathproperty | atomproperty | settable;
  public final static int fracxyz   =11 | mathproperty | atomproperty | settable;
  public final static int screenxyz =12 | mathproperty | atomproperty | settable;
  public final static int fuxyz     =13 | mathproperty | atomproperty | settable;
  public final static int unitxyz   =14 | mathproperty | atomproperty;
  public final static int vibxyz    =15 | mathproperty | atomproperty | settable;
  final static int w                =16 | mathproperty;
  final static int keys             =17 | mathproperty; 
  
  // occupancy, radius, and structure are odd, because they takes different meanings when compared
  
  public final static int occupancy     = intproperty | floatproperty | 1 | settable;
  public final static int radius        = intproperty | floatproperty | 2 | deprecatedparam | settable;
  public final static int structure     = intproperty | strproperty   | 3 | setparam | scriptCommand;

  // any new int, float, or string property should be added also to LabelToken.labelTokenIds
  // and the appropriate Atom.atomPropertyXXXX() method
  
  public final static int atomtype      = strproperty | 1 | settable;
  public final static int atomname      = strproperty | 2 | settable;
  public final static int altloc        = strproperty | 3;
  public final static int chain         = strproperty | 4;
  public final static int element       = strproperty | 5 | settable;
  public final static int group         = strproperty | 6;
  public final static int group1        = strproperty | 7;
  public final static int sequence      = strproperty | 8;
  public final static int identify      = strproperty | 9;
  public final static int insertion     = strproperty |10;
  public final static int shape         = strproperty |11;
  public final static int strucid       = strproperty |12;
  public final static int symbol        = strproperty |13 | settable;
  public final static int symmetry      = strproperty |14 | predefinedset;

  public final static int atomno        = intproperty | 1 | settable;
  public final static int atomid        = intproperty | 2;
  public final static int atomindex     = intproperty | 3;
  public final static int bondcount     = intproperty | 4;
  public final static int cell          = intproperty | 5;
  public final static int configuration = intproperty | 6 | scriptCommand;
  //color: see xxx(a, b, c, d)
  public final static int elemisono     = intproperty | 7;
  public final static int elemno        = intproperty | 8 | settable;
  //file: see xxx(a)
  public final static int formalcharge  = intproperty | 9 | setparam | settable;
  public final static int groupid       = intproperty | 10;
  public final static int groupindex    = intproperty | 11;
  public final static int model         = intproperty | 12 | scriptCommand;
  public final static int modelindex    = intproperty | 13;
  public final static int molecule      = intproperty | 14;
  public final static int polymer       = intproperty | 15;
  public final static int polymerlength = intproperty | 16;
  public final static int resno         = intproperty | 17;
  public final static int site          = intproperty | 18;
  public final static int strucno       = intproperty | 19;
  public final static int valence       = intproperty | 20 | settable;

  // float values must be multiplied by 100 prior to comparing to integer values

  // max 31 here
  
  public final static int adpmax          = floatproperty | 1;
  public final static int adpmin          = floatproperty | 2;
  public final static int covalent        = floatproperty | 3;
  public final static int eta             = floatproperty | 4; // Jmol 12.0.RC23
  public final static int mass            = floatproperty | 5;
  public final static int omega           = floatproperty | 6;
  public final static int phi             = floatproperty | 7;
  public final static int psi             = floatproperty | 8;
  public final static int screenx         = floatproperty | 9;
  public final static int screeny         = floatproperty | 10;
  public final static int screenz         = floatproperty | 11;
  public final static int straightness    = floatproperty | 12;
  public final static int surfacedistance = floatproperty | 13;
  public final static int theta           = floatproperty | 14; // Jmol 12.0.RC23
  public final static int unitx           = floatproperty | 15;
  public final static int unity           = floatproperty | 16;
  public final static int unitz           = floatproperty | 17;
  public final static int atomx           = floatproperty | 1 | settable;
  public final static int atomy           = floatproperty | 2 | settable;
  public final static int atomz           = floatproperty | 3 | settable;
  public final static int fracx           = floatproperty | 4 | settable;
  public final static int fracy           = floatproperty | 5 | settable;
  public final static int fracz           = floatproperty | 6 | settable;
  public final static int fux             = floatproperty | 7 | settable;
  public final static int fuy             = floatproperty | 8 | settable;
  public final static int fuz             = floatproperty | 9 | settable;
  public final static int hydrophobic     = floatproperty | 10 | settable | predefinedset;
  public final static int ionic           = floatproperty | 11 | settable;
  public final static int partialcharge   = floatproperty | 12 | settable;
  public final static int property        = floatproperty | 13 | mathproperty | setparam | settable;
  public final static int selected        = floatproperty | 14 | settable | predefinedset;
  public final static int temperature     = floatproperty | 15 | settable;
  public final static int vanderwaals     = floatproperty | 16 | settable | setparam;
  public final static int vectorscale     = floatproperty | 17 | floatparam;
  public final static int vibx            = floatproperty | 18 | settable;
  public final static int viby            = floatproperty | 19 | settable;
  public final static int vibz            = floatproperty | 20 | settable;
  public final static int x               = floatproperty | 21 | settable;
  public final static int y               = floatproperty | 22 | settable;
  public final static int z               = floatproperty | 23 | settable;
  
  public final static int backbone     = floatproperty | shapeCommand | 1 | predefinedset | defaultON | settable;
  public final static int cartoon      = floatproperty | shapeCommand | 2 | defaultON | settable;
  public final static int dots         = floatproperty | shapeCommand | 3 | defaultON;
  public final static int ellipsoid    = floatproperty | shapeCommand | 4 | defaultON;
  public final static int geosurface   = floatproperty | shapeCommand | 5 | defaultON;
  public final static int halo         = floatproperty | shapeCommand | 6 | defaultON | settable;
  public final static int meshRibbon   = floatproperty | shapeCommand | 7 | defaultON | settable;
  public final static int ribbon       = floatproperty | shapeCommand | 9 | defaultON | settable;
  public final static int rocket       = floatproperty | shapeCommand | 10 | defaultON | settable;
  public final static int spacefill    = floatproperty | shapeCommand | 11 | defaultON | settable;
  public final static int star         = floatproperty | shapeCommand | 12 | defaultON | settable;
  public final static int strands      = floatproperty | shapeCommand | 13 | deprecatedparam | defaultON | settable;
  public final static int trace        = floatproperty | shapeCommand | 14 | defaultON | settable;

  // mathfunc               means x = somefunc(a,b,c)
  // mathfunc|mathproperty  means x = y.somefunc(a,b,c)
  // 
  // maximum number of parameters is set by the << 9 shift
  // the min/max mask requires that the first number here must not exceed 63
  // the only other requirement is that these numbers be unique


  static int getMaxMathParams(int tokCommand) {
    return  ((tokCommand >> 9) & 0x7);
  }

  // 0 << 9 indicates that ScriptMathProcessor 
  // will check length in second stage of compilation

  // xxx(a,b,c,d,e,...)
  
  public final static int angle     = 1 | 0 << 9 | mathfunc;
  public final static int array     = 2 | 0 << 9 | mathfunc;
  final static int axisangle        = 3 | 0 << 9 | mathfunc;
  public final static int color     = 4 | 0 << 9 | mathfunc | intproperty | scriptCommand | deprecatedparam | settable;
  final static int compare          = 5 | 0 << 9 | mathfunc | scriptCommand;
  public final static int connected = 6 | 0 << 9 | mathfunc;
  public final static int data      = 7 | 0 << 9 | mathfunc | scriptCommand;
  public final static int format    = 8 | 0 << 9 | mathfunc | mathproperty | strproperty | settable;
  final static int function         = 9 | 0 << 9 | mathfunc | flowCommand;
  final static int getproperty      = 10 | 0 << 9 | mathfunc | scriptCommand;
  public final static int label     = 11 /* must be odd */| 0 << 9 | mathfunc | mathproperty | strproperty | settable | implicitStringCommand | shapeCommand | defaultON | deprecatedparam; 
  public final static int helix     = 12 | 0 << 9 | mathfunc | predefinedset;
  public final static int measure   = 13 | 0 << 9| mathfunc | shapeCommand | deprecatedparam | defaultON;
  final static int now              = 14 | 0 << 9 | mathfunc;
  public final static int plane     = 15 | 0 << 9 | mathfunc;
  public final static int point     = 16 | 0 << 9 | mathfunc;
  final static int quaternion       = 17 | 0 << 9 | mathfunc | scriptCommand;
  final static int sort             = 18 | 0 << 9 | mathfunc | mathproperty;
  final static int count            = 19 | 0 << 9 | mathfunc | mathproperty;
  public final static int within    = 20 | 0 << 9 | mathfunc;
  final static int write            = 21 | 0 << 9 | mathfunc | scriptCommand;
  final static int cache            = 22 | 0 << 9 | mathfunc | scriptCommand; // new in Jmol 13.1.2
  // xxx(a)
  
  final static int acos         = 3 | 1 << 9 | mathfunc;
  final static int sin          = 4 | 1 << 9 | mathfunc;
  final static int cos          = 5 | 1 << 9 | mathfunc;
  final static int sqrt         = 6 | 1 << 9 | mathfunc;
  public final static int file  = 7 | 1 << 9 | mathfunc | intproperty | scriptCommand;
  final static int forcmd       = 8 | 1 << 9 | mathfunc | flowCommand;
  final static int ifcmd        = 9 | 1 << 9 | mathfunc | flowCommand;
  final static int abs          = 10 | 1 << 9 | mathfunc;
  final static int javascript   = 12 /* must be even */| 1 << 9 | mathfunc | implicitStringCommand;

  // ___.xxx(a)
  
  // a.distance(b) is in a different set -- distance(b,c) -- because it CAN take
  // two parameters and it CAN be a dot-function (but not both together)
  
  final static int div          = 0 | 1 << 9 | mathfunc | mathproperty;
  final static int dot          = 1 | 1 << 9 | mathfunc | mathproperty;
  final static int join         = 2 | 1 << 9 | mathfunc | mathproperty;
  final static int mul          = 3 | 1 << 9 | mathfunc | mathproperty;
  final static int split        = 4 | 1 << 9 | mathfunc | mathproperty;
  final static int sub          = 5 | 1 << 9 | mathfunc | mathproperty;
  public final static int trim         = 6 | 1 << 9 | mathfunc | mathproperty;  
  public final static int volume = 7 | 1 << 9 | mathfunc | mathproperty | floatproperty;  
  final static int col           = 8 | 1 << 9 | mathfunc | mathproperty;
  final static int row           = 9 | 1 << 9 | mathfunc | mathproperty;

  // xxx(a,b)
  
  public final static int cross = 1 | 2 << 9 | mathfunc;
  final static int load         = 2 | 2 << 9 | mathfunc | scriptCommand;
  final static int random       = 4 | 2 << 9 | mathfunc;
  final static int script       = 5 | 2 << 9 | mathfunc | scriptCommand;
  public final static int substructure = 6 | 2 << 9 | mathfunc | intproperty | strproperty;
  final static int search       = 7 | 2 << 9 | mathfunc;
  final static int smiles       = 8 | 2 << 9 | mathfunc;
  public final static int contact = 9 | 2 << 9 | mathfunc | shapeCommand;


  // ___.xxx(a,b)

  // note that distance is here because it can take two forms:
  //     a.distance(b)
  // and
  //     distance(a,b)
  //so it can be a math property and it can have up to two parameters
  
  final static int add          = 1 | 2 << 9 | mathfunc | mathproperty;
  public final static int distance     = 2 | 2 << 9 | mathfunc | mathproperty;
  final static int find         = 4 | 3 << 9 | mathfunc | mathproperty;
  final static int replace      = 3 | 2 << 9 | mathfunc | mathproperty;

  // xxx(a,b,c)
  
  final static int hkl          = 1 | 3 << 9 | mathfunc;
  final static int intersection = 2 | 3 << 9 | mathfunc;
  final static int prompt       = 3 | 3 << 9 | mathfunc | mathExpressionCommand;
  final static int select       = 4 | 3 << 9 | mathfunc | atomExpressionCommand;

  // ___.xxx(a,b,c)
  
  final static int bin          = 1 | 3 << 9 | mathfunc | mathproperty;
  public final static int symop = 2 | 3 << 9 | mathfunc | mathproperty | intproperty; 

  // anything beyond 3 are set "unlimited"

  // set parameters 
  
  // deprecated or handled specially in ScriptEvaluator
  
  final static int bondmode           = deprecatedparam | 1;  
  final static int fontsize           = deprecatedparam | 2;
  final static int measurementnumbers = deprecatedparam | 3;
  final static int scale3d            = deprecatedparam | 4;
  final static int togglelabel        = deprecatedparam | 5;

  // handled specially in ScriptEvaluator

  public final static int backgroundmodel  = setparam | 2;
  public final static int debug            = setparam | 4;
  public final static int defaultlattice   = setparam | 6;
  public final static int highlight        = setparam | 8;// 12.0.RC14
  public final static int showscript       = setparam | 10;
  public final static int specular         = setparam | 12;
  public final static int trajectory       = setparam | 14;
  public final static int undo             = setparam | 16;
  public final static int usercolorscheme  = setparam | 18;

  // full set of all Jmol "set" parameters

  public final static int appletproxy                    = strparam | 2;
  public final static int atomtypes                      = strparam | 4;
  public final static int axescolor                      = strparam | 6;
  public final static int axis1color                     = strparam | 8;
  public final static int axis2color                     = strparam | 10;
  public final static int axis3color                     = strparam | 12;
  public final static int backgroundcolor                = strparam | 14;
  public final static int boundboxcolor                  = strparam | 16;
  public final static int currentlocalpath               = strparam | 18;
  public final static int dataseparator                  = strparam | 20;
  public final static int defaultanglelabel              = strparam | 22;
  public final static int defaultlabelpdb                = strparam | 23;
  public final static int defaultlabelxyz                = strparam | 24;
  public final static int defaultcolorscheme             = strparam | 25;
  public final static int defaultdirectory               = strparam | 26;
  public final static int defaultdistancelabel           = strparam | 27;
  public final static int defaultdropscript              = strparam | 28;
  public final static int defaultloadfilter              = strparam | 29;
  public final static int defaultloadscript              = strparam | 30;
  public final static int defaults                       = strparam | 32;
  public final static int defaulttorsionlabel            = strparam | 34;
  public final static int defaultvdw                     = strparam | 35;
  public final static int edsurlcutoff                   = strparam | 36;
  public final static int edsurlformat                   = strparam | 37;
  public final static int energyunits                    = strparam | 38; 
  public final static int filecachedirectory             = strparam | 39;
  public final static int forcefield                     = strparam | 40;
  public final static int helppath                       = strparam | 41;
  public final static int hoverlabel                     = strparam | 42;
  public final static int language                       = strparam | 44;
  public final static int loadformat                     = strparam | 45;
  public final static int loadligandformat               = strparam | 46;
  public final static int logfile                        = strparam | 47;
  public final static int measurementunits               = strparam | 48; 
  public final static int nmrurlformat                   = strparam | 49;
  public final static int pathforallfiles                = strparam | 50;
  public final static int picking                        = strparam | 52;
  public final static int pickingstyle                   = strparam | 54;
  public final static int picklabel                      = strparam | 56;
  public final static int propertycolorscheme            = strparam | 58;
  public final static int quaternionframe                = strparam | 60;
  public final static int smilesurlformat                = strparam | 62;
  public final static int smiles2dimageformat            = strparam | 64;
  public final static int unitcellcolor                  = strparam | 66;
  
  public final static int axesscale                      = floatparam | 2;
  public final static int bondtolerance                  = floatparam | 4;
  public final static int cameradepth                    = floatparam | 6;
  public final static int defaultdrawarrowscale          = floatparam | 8;
  public final static int defaulttranslucent             = floatparam | 10;
  public final static int dipolescale                    = floatparam | 12;
  public final static int ellipsoidaxisdiameter          = floatparam | 14;
  public final static int gestureswipefactor             = floatparam | 15;
  public final static int hbondsangleminimum             = floatparam | 16;
  public final static int hbondsdistancemaximum          = floatparam | 17;
  public final static int hoverdelay                     = floatparam | 18;
  public final static int loadatomdatatolerance          = floatparam | 19;  
  public final static int minbonddistance                = floatparam | 20;
  public final static int minimizationcriterion          = floatparam | 21;
  public final static int mousedragfactor                = floatparam | 22;
  public final static int mousewheelfactor               = floatparam | 23;
  public final static int multiplebondradiusfactor       = floatparam | 24;
  public final static int multiplebondspacing            = floatparam | 25;
  public final static int navfps                         = floatparam | 26;
  public final static int navigationdepth                = floatparam | 27;
  public final static int navigationslab                 = floatparam | 28;
  public final static int navigationspeed                = floatparam | 30;
  public final static int navx                           = floatparam | 32;
  public final static int navy                           = floatparam | 34;
  public final static int navz                           = floatparam | 36;
  public final static int pointgroupdistancetolerance    = floatparam | 38;
  public final static int pointgrouplineartolerance      = floatparam | 40;
  public final static int rotationradius                 = floatparam | 44;
  public final static int scaleangstromsperinch          = floatparam | 46;
  public final static int sheetsmoothing                 = floatparam | 48;
  public final static int slabrange                      = floatparam | 49;
  public final static int solventproberadius             = floatparam | 50;
  public final static int spinfps                        = floatparam | 52;
  public final static int spinx                          = floatparam | 54;
  public final static int spiny                          = floatparam | 56;
  public final static int spinz                          = floatparam | 58;
  public final static int stereodegrees                  = floatparam | 60;
  public final static int strutdefaultradius             = floatparam | 62;
  public final static int strutlengthmaximum             = floatparam | 64;
  public final static int vibrationperiod                = floatparam | 68;
  public final static int vibrationscale                 = floatparam | 70;
  public final static int visualrange                    = floatparam | 72;

  public final static int ambientpercent                 = intparam | 2;               
  public final static int animationfps                   = intparam | 4;
  public final static int axesmode                       = intparam | 6;
  public final static int bondradiusmilliangstroms       = intparam | 8;
  public final static int delaymaximumms                 = intparam | 10;
  public final static int diffusepercent                 = intparam | 14;
  public final static int dotdensity                     = intparam | 15;
  public final static int dotscale                       = intparam | 16;
  public final static int ellipsoiddotcount              = intparam | 17;  
  public final static int helixstep                      = intparam | 18;
  public final static int hermitelevel                   = intparam | 19;
  public final static int historylevel                   = intparam | 20;
  public final static int isosurfacepropertysmoothingpower=intparam | 21;
  public final static int loglevel                       = intparam | 22;
  public final static int meshscale                      = intparam | 23;
  public final static int minimizationsteps              = intparam | 24;
  public final static int minpixelselradius              = intparam | 25;
  public final static int percentvdwatom                 = intparam | 26;
  public final static int perspectivemodel               = intparam | 27;
  public final static int phongexponent                  = intparam | 28;
  public final static int pickingspinrate                = intparam | 30;
  public final static int propertyatomnumberfield        = intparam | 31;
  public final static int propertyatomnumbercolumncount  = intparam | 32;
  public final static int propertydatacolumncount        = intparam | 34;
  public final static int propertydatafield              = intparam | 36;
  public final static int repaintwaitms                  = intparam | 37;
  public final static int ribbonaspectratio              = intparam | 38;
  public final static int scriptreportinglevel           = intparam | 40;
  public final static int smallmoleculemaxatoms          = intparam | 42;
  public final static int specularexponent               = intparam | 44;
  public final static int specularpercent                = intparam | 46;
  public final static int specularpower                  = intparam | 48;
  public final static int strandcount                    = intparam | 50;
  public final static int strandcountformeshribbon       = intparam | 52;
  public final static int strandcountforstrands          = intparam | 54;
  public final static int strutspacing                   = intparam | 56;
  public final static int zdepth                         = intparam | 58;
  public final static int zslab                          = intparam | 60;
  public final static int zshadepower                    = intparam | 62;

  public final static int allowembeddedscripts           = booleanparam | 2;
  public final static int allowgestures                  = booleanparam | 4;
  public final static int allowkeystrokes                = booleanparam | 5;
  public static final int allowmodelkit                  = booleanparam | 6; // Jmol 12.RC15
  public final static int allowmoveatoms                 = booleanparam | 7; // Jmol 12.1.21
  public static final int allowmultitouch                = booleanparam | 8; // Jmol 11.9.24
  public final static int allowrotateselected            = booleanparam | 9;
  public final static int antialiasdisplay               = booleanparam | 10;
  public final static int antialiasimages                = booleanparam | 12;
  public final static int antialiastranslucent           = booleanparam | 14;
  public final static int appendnew                      = booleanparam | 16;
  public final static int applysymmetrytobonds           = booleanparam | 18;
  public final static int atompicking                    = booleanparam | 20;
  public final static int autobond                       = booleanparam | 22;
  public final static int autofps                        = booleanparam | 24;
//  public final static int autoloadorientation            = booleanparam | 26;
  public final static int axesmolecular                  = booleanparam | 28;
  public final static int axesorientationrasmol          = booleanparam | 30;
  public final static int axesunitcell                   = booleanparam | 32;
  public final static int axeswindow                     = booleanparam | 34;
  public final static int bondmodeor                     = booleanparam | 36;
  public final static int bondpicking                    = booleanparam | 38;
// set mathproperty  public final static int bonds                          = booleanparam | 40;
  public final static int cartoonbaseedges               = booleanparam | 42;
  public final static int cartoonrockets                 = booleanparam | 43;
  public final static int cartoonfancy                   = booleanparam | 44;
  public final static int celshading                     = booleanparam | 45;
  public final static int chaincasesensitive             = booleanparam | 46;
  public final static int colorrasmol                    = booleanparam | 47;
  public final static int debugscript                    = booleanparam | 48;
  public final static int defaultstructuredssp           = booleanparam | 49;
  public final static int disablepopupmenu               = booleanparam | 50;
  public final static int displaycellparameters          = booleanparam | 52;
  public final static int dotsselectedonly               = booleanparam | 53;
  public final static int dotsurface                     = booleanparam | 54;
  public final static int dragselected                   = booleanparam | 55;
  public final static int drawhover                      = booleanparam | 56;
  public final static int drawpicking                    = booleanparam | 57;
  public final static int dsspcalchydrogen               = booleanparam | 58;
  public final static int dynamicmeasurements            = booleanparam | 59;
  public final static int ellipsoidarcs                  = booleanparam | 60;  
  public final static int ellipsoidaxes                  = booleanparam | 61;  
  public final static int ellipsoidball                  = booleanparam | 62;  
  public final static int ellipsoiddots                  = booleanparam | 63;  
  public final static int ellipsoidfill                  = booleanparam | 64;  
  public final static int filecaching                    = booleanparam | 66;
  public final static int fontcaching                    = booleanparam | 68;
  public final static int fontscaling                    = booleanparam | 69;
  public final static int forceautobond                  = booleanparam | 70;
  public final static int fractionalrelative             = booleanparam | 72;
// see shapecommand public final static int frank                          = booleanparam | 72;
  public final static int greyscalerendering             = booleanparam | 74;
  public final static int hbondsbackbone                 = booleanparam | 76;
  public final static int hbondsrasmol                   = booleanparam | 77;
  public final static int hbondssolid                    = booleanparam | 78;
// see predefinedset  public final static int hetero                         = booleanparam | 80;
  public final static int hidenameinpopup                = booleanparam | 82;
  public final static int hidenavigationpoint            = booleanparam | 84;
  public final static int hidenotselected                = booleanparam | 86;
  public final static int highresolution                 = booleanparam | 88;
// see predefinedset  public final static int hydrogen                       = booleanparam | 90;
  public final static int imagestate                     = booleanparam | 92;
  public static final int iskiosk                        = booleanparam | 93; // 11.9.29
  public final static int isosurfacekey                  = booleanparam | 94;
  public final static int isosurfacepropertysmoothing    = booleanparam | 95;
  public final static int justifymeasurements            = booleanparam | 96;
  public final static int languagetranslation            = booleanparam | 97;
  public final static int legacyautobonding              = booleanparam | 98;
  public final static int logcommands                    = booleanparam | 99;
  public final static int loggestures                    = booleanparam | 100;
  public final static int measureallmodels               = booleanparam | 101;
  public final static int measurementlabels              = booleanparam | 102;
  public final static int messagestylechime              = booleanparam | 103;
  public final static int minimizationrefresh            = booleanparam | 104;
  public final static int minimizationsilent             = booleanparam | 105;
  public final static int modelkitmode                   = booleanparam | 106;  // 12.0.RC15
  public final static int monitorenergy                  = booleanparam | 107;
  public final static int multiprocessor                 = booleanparam | 108;
  public final static int navigatesurface                = booleanparam | 109;
  public final static int navigationmode                 = booleanparam | 110;
  public final static int navigationperiodic             = booleanparam | 111;
  public final static int partialdots                    = booleanparam | 112; // 12.1.46
  public final static int pdbaddhydrogens                = booleanparam | 113;
  public final static int pdbgetheader                   = booleanparam | 114;
  public final static int pdbsequential                  = booleanparam | 115;
  public final static int perspectivedepth               = booleanparam | 116;
  public final static int preservestate                  = booleanparam | 117;
  public final static int rangeselected                  = booleanparam | 118;
  public final static int refreshing                     = booleanparam | 120;
  public final static int ribbonborder                   = booleanparam | 122;
  public final static int rocketbarrels                  = booleanparam | 124;
  public final static int saveproteinstructurestate      = booleanparam | 126;
  public final static int scriptqueue                    = booleanparam | 128;
  public final static int selectallmodels                = booleanparam | 130;
  public final static int selecthetero                   = booleanparam | 132;
  public final static int selecthydrogen                 = booleanparam | 134;
  // see commands public final static int selectionhalo                  = booleanparam | 136;
  public final static int showaxes                       = booleanparam | 138;
  public final static int showboundbox                   = booleanparam | 140;
  public final static int showfrank                      = booleanparam | 142;
  public final static int showhiddenselectionhalos       = booleanparam | 144;
  public final static int showhydrogens                  = booleanparam | 146;
  public final static int showkeystrokes                 = booleanparam | 148;
  public final static int showmeasurements               = booleanparam | 150;
  public final static int showmultiplebonds              = booleanparam | 152;
  public final static int shownavigationpointalways      = booleanparam | 154;
// see intparam  public final static int showscript                     = booleanparam | 156;
  public final static int showtiming                     = booleanparam | 158;
  public final static int showunitcell                   = booleanparam | 160;
  public final static int slabbyatom                     = booleanparam | 162;
  public final static int slabbymolecule                 = booleanparam | 164;
  public final static int slabenabled                    = booleanparam | 166;
  public final static int smartaromatic                  = booleanparam | 168;
// see predefinedset  public final static int solvent                        = booleanparam | 170;
  public final static int solventprobe                   = booleanparam | 172;
// see intparam  public final static int specular                       = booleanparam | 174;
  public final static int ssbondsbackbone                = booleanparam | 176;
  public final static int statusreporting                = booleanparam | 178;
  public final static int strutsmultiple                 = booleanparam | 179;
  public final static int syncmouse                      = booleanparam | 180;
  public final static int syncscript                     = booleanparam | 182;
  public final static int testflag1                      = booleanparam | 184;
  public final static int testflag2                      = booleanparam | 186;
  public final static int testflag3                      = booleanparam | 188;
  public final static int testflag4                      = booleanparam | 190;
  public final static int tracealpha                     = booleanparam | 191;
  public final static int twistedsheets                  = booleanparam | 192;
  public final static int usearcball                     = booleanparam | 193;
  public final static int useminimizationthread          = booleanparam | 194;
  public final static int usenumberlocalization          = booleanparam | 196;
  public final static int vectorsymmetry                 = booleanparam | 197;
  public final static int waitformoveto                  = booleanparam | 198;
  public final static int windowcentered                 = booleanparam | 199;
  public final static int wireframerotation              = booleanparam | 200;
  public final static int zerobasedxyzrasmol             = booleanparam | 202;
  public final static int zoomenabled                    = booleanparam | 204;
  public final static int zoomlarge                      = booleanparam | 206;
  public final static int zshade                         = booleanparam | 208;

  
  // misc

  final static int absolute      = misc  | 2;
  final static int addhydrogens  = misc  | 4;
  final static int adjust        = misc  | 6;
  final static int align         = misc  | 8;
  final static int allconnected  = misc  | 10;
  final static int angstroms     = misc  | 12;
  final static int anisotropy    = misc  | 14;
  final static int append        = misc  | 15;
  final static int arc           = misc  | 16 | expression;
  final static int area          = misc  | 18;
  final static int aromatic      = misc  | 20 | predefinedset;
  final static int arrow         = misc  | 22;
  final static int as            = misc  | 24; // for LOAD and ISOSURFACE only
  final static int atomicorbital = misc  | 26;
  public final static int auto   = misc  | 28;
  public final static int axis   = misc  | 30;
  final static int babel         = misc  | 32;
  final static int babel21       = misc  | 34; 
  final static int back          = misc  | 36;
  final static int barb          = misc  | 37;
  public final static int backlit = misc  | 38;
  public final static int basepair      = misc  | 40;
  final static int binary        = misc  | 42;
  final static int blockdata     = misc  | 44;
  final static int bondset       = misc  | 46;
  final static int bottom        = misc  | 47;
  public final static int brillouin     = misc  | 48;
  final static int cancel        = misc  | 50;
  public final static int cap    = misc  | 51 | expression;
  final static int cavity        = misc  | 52;
  final static int centroid      = misc  | 53;
  final static int check         = misc  | 54;
  final static int chemical      = misc  | 55;
  final static int circle        = misc  | 56;
  public final static int clash         = misc  | 57;
  final static int clear         = misc  | 58;
  final static int clipboard     = misc  | 60;
  final static int collapsed     = misc  | 62;
  final static int colorscheme   = misc  | 64;
  final static int command       = misc  | 66;
  final static int commands      = misc  | 68;
  final static int constraint    = misc  | 70;
  final static int contour       = misc  | 72;
  public final static int contourlines  = misc  | 74;
  final static int contours      = misc  | 76;
  final static int corners       = misc  | 78;
  public final static int create = misc  | 80;
  final static int criterion     = misc  | 81;
  final static int crossed       = misc  | 82;
  final static int curve         = misc  | 84;
  final static int cutoff        = misc  | 86;
  final static int cylinder      = misc  | 88;
  final static int density        = misc  | 90;
  final static int dssp           = misc  | 91;
  final static int diameter       = misc  | 92;
  final static int direction      = misc  | 94;
  final static int discrete       = misc  | 96;
  final static int displacement   = misc  | 98;
  final static int distancefactor = misc  | 100;
  final static int dotted         = misc  | 102;
  final static int downsample     = misc  | 104;
  final static int drawing        = misc  | 105;
  final static int eccentricity   = misc  | 106;
  final static int ed             = misc  | 108 | expression;
  final static int edges          = misc  | 110;
  final static int energy         = misc  | 111;
  final static int error          = misc  | 112; 
  final static int facecenteroffset = misc  | 113;
  public final static int fill    = misc  | 114;
  final static int filter         = misc  | 116;
  public final static int first   = misc  | 118;
  final static int fixedtemp      = misc  | 122;
  final static int flat           = misc  | 124;
  final static int fps            = misc  | 126 | expression;
  final static int from           = misc  | 128;
  public final static int front   = misc  | 130;
  final static int frontedges     = misc  | 132;
  public final static int frontlit = misc  | 134;
  public final static int frontonly = misc  | 136;
  public final static int full            = misc  | 137;
  final static int fullplane       = misc  | 138;
  public final static int fullylit = misc  | 140;
  final static int functionxy     = misc  | 142;
  final static int functionxyz    = misc  | 144;
  final static int gridpoints     = misc  | 146;
  final static int homo           = misc  | 149;
  final static int id             = misc  | 150 | expression;
  final static int ignore         = misc  | 152;
  final static int inchi          = misc  | 153;
  final static int inchikey       = misc  | 154;
  final static int image          = misc  | 155;
  final static int in             = misc  | 156;
  final static int increment      = misc  | 157;
  public final static int info    = misc  | 158;
  final static int inline         = misc  | 159;
  final static int insideout      = misc  | 160;
  final static int interior       = misc  | 162;
  final static int internal       = misc  | 164;
  public final static int intramolecular = misc  | 165;
  public final static int intermolecular = misc  | 166;
  public final static int jmol    = misc  | 168;
  public final static int last    = misc  | 169;
  final static int lattice        = misc  | 170;
  final static int lighting       = misc  | 171;
  public final static int left    = misc  | 172;
  final static int line           = misc  | 174;
  final static int link           = misc  | 175;
  final static int linedata       = misc  | 176;
  public final static int list    = misc  | 177; // just "list"
  final static int lobe           = misc  | 178;
  final static int lonepair       = misc  | 180;
  final static int lp             = misc  | 182;
  final static int lumo           = misc  | 184;
  final static int manifest       = misc  | 186;
  final static int maxset         = misc  | 190;
  final static int menu           = misc  | 191;
  final static int mep            = misc  | 192;
  public final static int mesh    = misc  | 194;
  final static int middle         = misc  | 195;
  final static int minset         = misc  | 196;
  final static int mlp            = misc  | 198;
  final static int mode           = misc  | 200;
  public final static int modify         = misc  | 201;
  public final static int modifyorcreate = misc  | 202;
  final static int modelbased     = misc  | 204;
  final static int molecular      = misc  | 205;
  final static int monomer        = misc  | 206;
  final static int morph          = misc  | 207;
  public final static int movie          = misc  | 208;
  final static int mrc            = misc  | 209;
  final static int msms           = misc  | 210;
  final static int name           = misc  | 211;
  public final static int nci            = misc  | 212;
  public final static int next    = misc  | 213;
  final static int nmr            = misc  | 214;
  public final static int nocontourlines  = misc  | 215;
  final static int nocross        = misc  | 216;
  final static int nodebug        = misc  | 217;
  public final static int nodots  = misc  | 218;
  final static int noedges        = misc  | 220;
  public final static int nofill  = misc  | 222;
  final static int nohead         = misc  | 224;
  final static int noload         = misc  | 226;
  public final static int nomesh  = misc  | 228;
  final static int noplane        = misc  | 230;
  final static int normal         = misc  | 232;
  public final static int notfrontonly  = misc  | 234;
  public final static int notriangles   = misc  | 236;
  final static int obj            = misc  | 238;
  final static int object         = misc  | 240;
  final static int offset         = misc  | 242;
  final static int offsetside     = misc  | 244;
  final static int once           = misc  | 246;
  final static int only           = misc  | 248;
  final static int opaque         = misc  | 250;
  final static int orbital        = misc  | 252;
  final static int orientation    = misc  | 253;
  final static int origin         = misc  | 254; // 12.1.51
  final static int out            = misc  | 255;
  final static int packed         = misc  | 256;
  final static int palindrome     = misc  | 258;
  final static int parameters     = misc  | 259;
  public final static int path           = misc  | 260;
  final static int pdb            = misc  | 262 | expression;
  final static int pdbheader      = misc  | 264;
  final static int period         = misc  | 266;
  final static int perpendicular  = misc  | 268;
  final static int phase          = misc  | 270;
  public final static int play    = misc  | 272;
  public final static int playrev = misc  | 274;
  final static int pocket         = misc  | 276;
  final static int pointgroup     = misc  | 278;
  final static int pointsperangstrom = misc  | 280;
  final static int polygon        = misc  | 282;
  public final static int prev    = misc  | 284;
  public final static int probe   = misc  | 285;
  public final static int pymol   = misc  | 286;
  final static int rad            = misc  | 287;
  final static int radical        = misc  | 288;
  public final static int range   = misc  | 290;
  public final static int rasmol  = misc  | 292;
  final static int reference      = misc  | 294;
  final static int remove         = misc  | 295;
  public final static int residue = misc  | 296;
  final static int resolution     = misc  | 298;
  final static int reversecolor   = misc  | 300;
  public final static int rewind  = misc  | 302;
  public final static int right   = misc  | 304;
  final static int rotate45       = misc  | 306;
  public final static int rotation = misc  | 308;
  final static int rubberband     = misc  | 310;
  public final static int sasurface      = misc  | 312;
  final static int scale          = misc  | 314;
  final static int scene          = misc  | 315; // Jmol 12.3.32
  final static int selection      = misc  | 316;
  final static int shapely        = misc  | 320;
  final static int sigma          = misc  | 322;
  final static int sign           = misc  | 323;
  final static int silent         = misc  | 324;
  final static int solid          = misc  | 326;
  final static int spacegroup     = misc  | 328;
  public final static int sphere  = misc  | 330;
  final static int squared        = misc  | 332;
  final static int state          = misc  | 334;
  final static int stop           = misc  | 338;
  final static int supercell      = misc  | 339;//
  final static int ticks          = misc  | 340; 
  final static int title          = misc  | 342;
  final static int titleformat    = misc  | 344;
  final static int to             = misc  | 346 | expression;
  final static int top            = misc  | 348 | expression;
  final static int torsion        = misc  | 350;
  final static int transform      = misc  | 352;
  public final static int translation   = misc  | 354;
  public final static int translucent   = misc  | 356;
  public final static int triangles     = misc  | 358;
  final static int url             = misc  | 360 | expression;
  final static int user            = misc  | 362;
  final static int val             = misc  | 364;
  final static int variable        = misc  | 366;
  final static int variables       = misc  | 368;
  final static int vertices        = misc  | 370;
  final static int spacebeforesquare      = misc  | 371;
  final static int width           = misc  | 372;
  
  
  // predefined Tokens: 
  
  final static T tokenSpaceBeforeSquare = o(spacebeforesquare, " ");
  final static T tokenOn  = t(on, 1, "on");
  final static T tokenOff = t(off, 0, "off");
  final static T tokenAll = o(all, "all");
  final static T tokenIf = o(ifcmd, "if");
  public final static T tokenAnd = o(opAnd, "and");
  public final static T tokenAND = o(opAND, "");
  public final static T tokenOr  = o(opOr, "or");
  public final static T tokenAndFALSE = o(opAnd, "and");
  public final static T tokenOrTRUE = o(opOr, "or");
  public final static T tokenOpIf  = o(opIf, "?");
  public final static T tokenComma = o(comma, ",");
  final static T tokenDefineString = t(define, string, "@");
  final static T tokenPlus = o(plus, "+");
  final static T tokenMinus = o(minus, "-");
  final static T tokenTimes = o(times, "*");
  final static T tokenDivide = o(divide, "/");

  public final static T tokenLeftParen = o(leftparen, "(");
  public final static T tokenRightParen = o(rightparen, ")");
  final static T tokenArraySquare = o(array, "[");
  final static T tokenArraySelector = o(leftsquare, "[");
 
  public final static T tokenExpressionBegin = o(expressionBegin, "expressionBegin");
  public final static T tokenExpressionEnd   = o(expressionEnd, "expressionEnd");
  public final static T tokenConnected       = o(connected, "connected");
  final static T tokenCoordinateBegin = o(leftbrace, "{");
  final static T tokenRightBrace = o(rightbrace, "}");
  final static T tokenCoordinateEnd = tokenRightBrace;
  final static T tokenColon           = o(colon, ":");
  final static T tokenSetCmd          = o(set, "set");
  final static T tokenSet             = t(set, '=', "");
  final static T tokenSetArray        = t(set, '[', "");
  final static T tokenSetProperty     = t(set, '.', "");
  final static T tokenSetVar          = t(set, '=', "var");
  final static T tokenEquals          = o(opEQ, "=");
  final static T tokenScript          = o(script, "script");
  final static T tokenSwitch          = o(switchcmd, "switch");
    
  private static Map<String, T> tokenMap = new Hashtable<String, T>();
  public static void addToken(String ident, T token) {
    tokenMap.put(ident, token);
  }
  
  public static T getTokenFromName(String name) {
    // this one needs to NOT be lower case for ScriptCompiler
    return tokenMap.get(name);
  }
  
  public static int getTokFromName(String name) {
    T token = getTokenFromName(name.toLowerCase());
    return (token == null ? nada : token.tok);
  }


  
  /**
   * note: nameOf is a very inefficient mechanism for getting 
   * the name of a token. But it is only used for error messages
   * and listings of variables and such.
   * 
   * @param tok
   * @return     the name of the token or 0xAAAAAA
   */
  public static String nameOf(int tok) {
    for (T token : tokenMap.values()) {
      if (token.tok == tok)
        return "" + token.value;
    }
    return "0x"+Integer.toHexString(tok);
   }
   
  @Override
  public String toString() {
    return "Token["
        + astrType[tok < keyword ? tok : keyword]
        + "("+(tok%(1<<9))+"/0x" + Integer.toHexString(tok) + ")"
        + ((intValue == Integer.MAX_VALUE) ? "" : " intValue=" + intValue
            + "(0x" + Integer.toHexString(intValue) + ")")
        + ((value == null) ? "" : value instanceof String ? " value=\"" + value
            + "\"" : " value=" + value) + "]";
  }
  
  ////////command sets ///////

  /**
   * retrieves an unsorted list of viable commands that could be
   * completed by this initial set of characters. If fewer than
   * two characters are given, then only the "preferred" command
   * is given (measure, not monitor, for example), and in all cases
   * if both a singular and a plural might be returned, only the
   * singular is returned.
   * 
   * @param strBegin initial characters of the command, or null
   * @return UNSORTED semicolon-separated string of viable commands
   */
  public static String getCommandSet(String strBegin) {
    String cmds = "";
    Map<String, Boolean> htSet = new Hashtable<String, Boolean>();
    int nCmds = 0;
    String s = (strBegin == null || strBegin.length() == 0 ? null : strBegin
        .toLowerCase());
    boolean isMultiCharacter = (s != null && s.length() > 1);
    for (Map.Entry<String, T> entry : tokenMap.entrySet()) {
      String name = entry.getKey();
      T token = entry.getValue();
      if ((token.tok & scriptCommand) != 0
          && (s == null || name.indexOf(s) == 0)
          && (isMultiCharacter || ((String) token.value).equals(name)))
        htSet.put(name, Boolean.TRUE);
    }
    for (Map.Entry<String, Boolean> entry : htSet.entrySet()) {
      String name = entry.getKey();
      if (name.charAt(name.length() - 1) != 's'
          || !htSet.containsKey(name.substring(0, name.length() - 1)))
        cmds += (nCmds++ == 0 ? "" : ";") + name;
    }
    return cmds;
  }
  
  public static JmolList<T> getAtomPropertiesLike(String type) {
    type = type.toLowerCase();
    JmolList<T> v = new  JmolList<T>();
    boolean isAll = (type.length() == 0);
    for (Map.Entry<String, T> entry : tokenMap.entrySet()) {
      String name = entry.getKey();
      if (name.charAt(0) == '_')
        continue;
      T token = entry.getValue();
      if (tokAttr(token.tok, atomproperty) && (isAll || name.toLowerCase().startsWith(type))) {
        if (isAll || !((String) token.value).toLowerCase().startsWith(type))
          token = o(token.tok, name);
        v.addLast(token);
      }
    }
    return (v.size() == 0 ? null : v);
  }

  public static String[] getTokensLike(String type) {
    int attr = (type.equals("setparam") ? setparam 
        : type.equals("misc") ? misc 
        : type.equals("mathfunc") ? mathfunc : scriptCommand);
    int notattr = (attr == setparam ? deprecatedparam : nada);
    JmolList<String> v = new  JmolList<String>();
    for (Map.Entry<String, T> entry : tokenMap.entrySet()) {
      String name = entry.getKey();
      T token = entry.getValue();
      if (tokAttr(token.tok, attr) && (notattr == nada || !tokAttr(token.tok, notattr)))
        v.addLast(name);
    }
    String[] a = v.toArray(new String[v.size()]);
    Arrays.sort(a);
    return a;
  }

  public static int getSettableTokFromString(String s) {
    int tok = getTokFromName(s);
    return (tok != nada && tokAttr(tok, settable) 
          && !tokAttr(tok, mathproperty) ? tok : nada);
  }

  public static String completeCommand(Map<String, T> map, boolean isSet, 
                                       boolean asCommand, 
                                       String str, int n) {
    if (map == null)
      map = tokenMap;
    else
      asCommand = false;
    JmolList<String> v = new  JmolList<String>();
    str = str.toLowerCase();
    for (String name : map.keySet()) {
      if (!name.startsWith(str))
        continue;
      int tok = getTokFromName(name);
      if (asCommand ? tokAttr(tok, scriptCommand) 
          : isSet ? tokAttr(tok, setparam) && !tokAttr(tok, deprecatedparam) 
          : true)
        v.addLast(name);
    }
    return ArrayUtil.sortedItem(v, n);
  }

  static {

    Object[] arrayPairs  = {

    // atom expressions

      "(",            tokenLeftParen,
      ")",            tokenRightParen,
      "and",          tokenAnd,
      "&",            null,
      "&&",           null,
      "or",           tokenOr,
      "|",            null,
      "||",           null,
      "?",            tokenOpIf,
      ",",            tokenComma,
      "+=",           new T(andequals),
      "-=",           null,
      "*=",           null,
      "/=",           null,
      "\\=",          null,
      "&=",           null,
      "|=",           null,
      "not",          new T(opNot),
      "!",            null,
      "xor",          new T(opXor),
    //no-- don't do this; it interferes with define
    //  "~",            null,
      "tog",          new T(opToggle),
      "<",            new T(opLT),
      "<=",           new T(opLE),
      ">=",           new T(opGE),
      ">",            new T(opGT),
      "=",            tokenEquals,
      "==",           null,
      "!=",           new T(opNE),
      "<>",           null,
      "within",       new T(within),
      ".",            new T(per),
      "[",            new T(leftsquare),
      "]",            new T(rightsquare),
      "{",            new T(leftbrace),
      "}",            new T(rightbrace),
      "$",            new T(dollarsign),
      "%",            new T(percent),
      ":",            tokenColon,
      ";",            new T(semicolon),
      "++",           new T(plusPlus),
      "--",           new T(minusMinus),
      "**",           new T(timestimes),
      "+",            tokenPlus,
      "-",            tokenMinus,
      "*",            tokenTimes,
      "/",            tokenDivide,
      "\\",           new T(leftdivide),
    
    // commands
        
      "animation",         new T(animation),
      "anim",              null,
      "assign",            new T(assign),
      "axes",              new T(axes),
      "backbone",          new T(backbone),
      "background",        new T(background),
      "bind",              new T(bind),
      "bondorder",         new T(bondorder),
      "boundbox",          new T(boundbox),
      "boundingBox",       null,
      "break",             new T(breakcmd),
      "calculate",         new T(calculate),
      "cartoon",           new T(cartoon),
      "cartoons",          null,
      "case",              new T(casecmd),
      "catch",             new T(catchcmd),
      "cd",                new T(cd),
      "center",            new T(center),
      "centre",            null,
      "centerat",          new T(centerAt),
      "color",             new T(color),
      "colour",            null,
      "compare",           new T(compare),
      "configuration",     new T(configuration),
      "conformation",      null,
      "config",            null,
      "connect",           new T(connect),
      "console",           new T(console),
      "contact",           new T(contact),
      "contacts",          null,
      "continue",          new T(continuecmd),
      "data",              new T(data),
      "default",           new T(defaultcmd),
      "define",            new T(define),
      "@",                 null,
      "delay",             new T(delay),
      "delete",            new T(delete),
      "density",           new T(density),
      "depth",             new T(depth),
      "dipole",            new T(dipole),
      "dipoles",           null,
      "display",           new T(display),
      "dot",               new T(dot),
      "dots",              new T(dots),
      "draw",              new T(draw),
      "echo",              new T(echo),
      "ellipsoid",         new T(ellipsoid),
      "ellipsoids",        null,
      "else",              new T(elsecmd),
      "elseif",            new T(elseif),
      "end",               new T(end),
      "endif",             new T(endifcmd),
      "exit",              new T(exit),
      "file",              new T(file),
      "files",             null,
      "font",              new T(font),
      "for",               new T(forcmd),
      "format",            new T(format),
      "frame",             new T(frame),
      "frames",            null,
      "frank",             new T(frank),
      "function",          new T(function),
      "functions",         null,
      "geosurface",        new T(geosurface),
      "getProperty",       new T(getproperty),
      "goto",              new T(gotocmd),
      "halo",              new T(halo),
      "halos",             null,
      "helix",             new T(helix),
      "helixalpha",        new T(helixalpha),
      "helix310",          new T(helix310),
      "helixpi",           new T(helixpi),
      "hbond",             new T(hbond),
      "hbonds",            null,
      "help",              new T(help),
      "hide",              new T(hide),
      "history",           new T(history),
      "hover",             new T(hover),
      "if",                new T(ifcmd),
      "in",                new T(in),
      "initialize",        new T(initialize),
      "invertSelected",    new T(invertSelected),
      "isosurface",        new T(isosurface),
      "javascript",        new T(javascript),
      "label",             new T(label),
      "labels",            null,
      "lcaoCartoon",       new T(lcaocartoon),
      "lcaoCartoons",      null,
      "load",              new T(load),
      "log",               new T(log),
      "loop",              new T(loop),
      "measure",           new T(measure),
      "measures",          null,
      "monitor",           null,
      "monitors",          null,
      "meshribbon",        new T(meshRibbon),
      "meshribbons",       null,
      "message",           new T(message),
      "minimize",          new T(minimize),
      "minimization",      null,
      "mo",                new T(mo),
      "model",             new T(model),
      "models",            null,
      "move",              new T(move),
      "moveTo",            new T(moveto),
      "navigate",          new T(navigate),
      "navigation",        null,
      "origin",            new T(origin),
      "out",               new T(out),
      "parallel",          new T(parallel),
      "pause",             new T(pause),
      "wait",              null,
      "plot",              new T(plot),
      "plot3d",            new T(plot3d),
      "pmesh",             new T(pmesh),
      "polygon",           new T(polygon),
      "polyhedra",         new T(polyhedra),
      "print",             new T(print),
      "process",           new T(process),
      "prompt",            new T(prompt),
      "quaternion",        new T(quaternion),
      "quaternions",       null,
      "quit",              new T(quit),
      "ramachandran",      new T(ramachandran),
      "rama",              null,
      "refresh",           new T(refresh),
      "reset",             new T(reset),
      "unset",             null,
      "restore",           new T(restore),
      "restrict",          new T(restrict),
      "return",            new T(returncmd),
      "ribbon",            new T(ribbon),
      "ribbons",           null,
      "rocket",            new T(rocket),
      "rockets",           null,
      "rotate",            new T(rotate),
      "rotateSelected",    new T(rotateSelected),
      "save",              new T(save),
      "script",            tokenScript,
      "source",            null,
      "select",            new T(select),
      "selectionHalos",    new T(selectionhalos),
      "selectionHalo",     null,
      "showSelections",    null,
      "set",               tokenSetCmd,
      "sheet",             new T(sheet),
      "show",              new T(show),
      "slab",              new T(slab),
      "spacefill",         new T(spacefill),
      "cpk",               null,
      "spin",              new T(spin),
      "ssbond",            new T(ssbond),
      "ssbonds",           null,
      "star",              new T(star),
      "stars",             null,
      "step",              new T(step),
      "steps",             null,
      "stereo",            new T(stereo),
      "strand",            new T(strands),
      "strands",           null,
      "structure",         new T(structure),
      "_structure",        null,
      "strucNo",           new T(strucno),
      "struts",            new T(struts),
      "strut",             null,
      "subset",            new T(subset),
      "switch",            tokenSwitch,
      "synchronize",       new T(sync),
      "sync",              null,
      "trace",             new T(trace),
      "translate",         new T(translate),
      "translateSelected", new T(translateSelected),
      "try",               new T(trycmd),
      "unbind",            new T(unbind),
      "unitcell",          new T(unitcell),
      "var",               new T(var),
      "vector",            new T(vector),
      "vectors",           null,
      "vibration",         new T(vibration),
      "while",             new T(whilecmd),
      "wireframe",         new T(wireframe),
      "write",             new T(write),
      "zap",               new T(zap),
      "zoom",              new T(zoom),
      "zoomTo",            new T(zoomTo),
                            
      //                   show parameters
  
      "atom",              new T(atoms),
      "atoms",             null,
      "axis",              new T(axis),
      "axisangle",         new T(axisangle),
      "basepair",          new T(basepair),
      "basepairs",         null,
      "orientation",       new T(orientation),
      "orientations",      null,
      "pdbheader",         new T(pdbheader),                          
      "polymer",           new T(polymer),
      "polymers",          null,
      "residue",           new T(residue),
      "residues",          null,
      "rotation",          new T(rotation),
      "row",               new T(row),
      "sequence",          new T(sequence),
      "shape",             new T(shape),
      "state",             new T(state),
      "symbol",            new T(symbol),
      "symmetry",          new T(symmetry),
      "spaceGroup",        new T(spacegroup),
      "transform",         new T(transform),
      "translation",       new T(translation),
      "url",               new T(url),
  
      // misc
  
      "abs",             new T(abs),
      "absolute",        new T(absolute),
      "acos",            new T(acos),
      "add",             new T(add),
      "adpmax",          new T(adpmax),
      "adpmin",          new T(adpmin),
      "align",           new T(align),
      "all",             tokenAll,
      "altloc",          new T(altloc),
      "altlocs",         null,
      "amino",           new T(amino),
      "angle",           new T(angle),
      "array",           new T(array),
      "as",              new T(as),
      "atomID",          new T(atomid),
      "_atomID",         null,
      "_a",              null, 
      "atomIndex",       new T(atomindex),
      "atomName",        new T(atomname),
      "atomno",          new T(atomno),
      "atomType",        new T(atomtype),
      "atomX",           new T(atomx),
      "atomY",           new T(atomy),
      "atomZ",           new T(atomz),
      "average",         new T(average),
      "babel",           new T(babel),
      "babel21",         new T(babel21), 
      "back",            new T(back),
      "backlit",         new T(backlit),
      "baseModel",       new T(basemodel), // Jmol 12.3.19
      "bin",             new T(bin),
      "bondCount",       new T(bondcount),
      "bottom",          new T(bottom),
      "branch",          new T(branch),
      "brillouin",       new T(brillouin),
      "bzone",           null,
      "wignerSeitz",     null,
      "cache",           new T(cache), // Jmol 12.3.24 
      "carbohydrate",    new T(carbohydrate),
      "cell",            new T(cell),
      "chain",           new T(chain),
      "chains",          null,
      "clash",           new T(clash),
      "clear",           new T(clear),
      "clickable",       new T(clickable),
      "clipboard",       new T(clipboard),
      "connected",       new T(connected),
      "constraint",      new T(constraint),
      "contourLines",    new T(contourlines),
      "coord",           new T(coord),
      "coordinates",     null,
      "coords",          null,
      "cos",             new T(cos),
      "cross",           new T(cross),
      "covalent",        new T(covalent),
      "direction",       new T(direction),
      "displacement",    new T(displacement),
      "displayed",       new T(displayed),
      "distance",        new T(distance),
      "div",             new T(div),
      "DNA",             new T(dna),
      "dotted",          new T(dotted),
      "DSSP",            new T(dssp),
      "element",         new T(element),
      "elemno",          new T(elemno),
      "_e",              new T(elemisono),
      "error",           new T(error),
      "fill",            new T(fill),
      "find",            new T(find),
      "fixedTemperature",new T(fixedtemp),
      "forcefield",      new T(forcefield),
      "formalCharge",    new T(formalcharge),
      "charge",          null, 
      "eta",             new T(eta),
      "front",           new T(front),
      "frontlit",        new T(frontlit),
      "frontOnly",       new T(frontonly),
      "fullylit",        new T(fullylit),
      "fx",              new T(fracx),
      "fy",              new T(fracy),
      "fz",              new T(fracz),
      "fxyz",            new T(fracxyz),
      "fux",             new T(fux),
      "fuy",             new T(fuy),
      "fuz",             new T(fuz),
      "fuxyz",           new T(fuxyz),
      "group",           new T(group),
      "groups",          null,
      "group1",          new T(group1),
      "groupID",         new T(groupid),
      "_groupID",        null, 
      "_g",              null, 
      "groupIndex",      new T(groupindex),
      "hidden",          new T(hidden),
      "highlight",       new T(highlight),
      "hkl",             new T(hkl),
      "hydrophobic",     new T(hydrophobic),
      "hydrophobicity",  null,
      "hydro",           null,
      "id",              new T(id),
      "identify",        new T(identify),
      "ident",           null,
      "image",           new T(image),
      "info",            new T(info),
      "inline",          new T(inline),
      "insertion",       new T(insertion),
      "insertions",      null, 
      "intramolecular",  new T(intramolecular),
      "intra",           null,
      "intermolecular",  new T(intermolecular),
      "inter",           null,
      "ionic",           new T(ionic),
      "ionicRadius",     null,
      "isAromatic",      new T(isaromatic),
      "Jmol",            new T(jmol),
      "join",            new T(join),
      "keys",            new T(keys),
      "last",            new T(last),
      "left",            new T(left),
      "length",          new T(length),
      "lines",           new T(lines),
      "list",            new T(list),
      "mass",            new T(mass),
      "max",             new T(max),
      "mep",             new T(mep),
      "mesh",            new T(mesh),
      "middle",          new T(middle),
      "min",             new T(min),
      "mlp",             new T(mlp),
      "mode",            new T(mode),
      "modify",          new T(modify),
      "modifyOrCreate",  new T(modifyorcreate),
      "molecule",        new T(molecule),
      "molecules",       null, 
      "modelIndex",      new T(modelindex),
      "monomer",         new T(monomer),
      "morph",           new T(morph),
      "movie",           new T(movie),
      "mul",             new T(mul),
      "nci",             new T(nci),
      "next",            new T(next),
      "noDots",          new T(nodots),
      "noFill",          new T(nofill),
      "noMesh",          new T(nomesh),
      "none",            new T(none),
      "null",            null,
      "inherit",         null,
      "normal",          new T(normal),
      "noContourLines",  new T(nocontourlines),
      "notFrontOnly",    new T(notfrontonly),
      "noTriangles",     new T(notriangles),
      "now",             new T(now),
      "nucleic",         new T(nucleic),
      "occupancy",       new T(occupancy),
      "off",             tokenOff, 
      "false",           null, 
      "on",              tokenOn,
      "true",            null, 
      "omega",           new T(omega),
      "only",            new T(only),
      "opaque",          new T(opaque),
      "partialCharge",   new T(partialcharge),
      "phi",             new T(phi),
      "plane",           new T(plane),
      "planar",          null,
      "play",            new T(play),
      "playRev",         new T(playrev),
      "point",           new T(point),
      "points",          null,
      "pointGroup",      new T(pointgroup),
      "polymerLength",   new T(polymerlength),
      "previous",        new T(prev),
      "prev",            null,
      "probe",           new T(probe),
      "property",        new T(property),
      "properties",      null,
      "protein",         new T(protein),
      "psi",             new T(psi),
      "purine",          new T(purine),
      "PyMOL",           new T(pymol),
      "pyrimidine",      new T(pyrimidine),
      "random",          new T(random),
      "range",           new T(range),
      "rasmol",          new T(rasmol),
      "replace",         new T(replace),
      "resno",           new T(resno),
      "resume",          new T(resume),
      "rewind",          new T(rewind),
      "reverse",         new T(reverse),
      "right",           new T(right),
      "RNA",             new T(rna),
      "rubberband",      new T(rubberband),
      "saSurface",       new T(sasurface),
      "scale",           new T(scale),
      "scene",           new T(scene),
      "search",          new T(search),
      "smarts",          null,
      "selected",        new T(selected),
      "shapely",         new T(shapely),
      "sidechain",       new T(sidechain),
      "sin",             new T(sin),
      "site",            new T(site),
      "size",            new T(size),
      "smiles",          new T(smiles),
      "substructure",    new T(substructure),  // 12.0 substructure-->smiles (should be smarts, but for legacy reasons, need this to be smiles
      "solid",           new T(solid),
      "sort",            new T(sort),
      "specialPosition", new T(specialposition),
      "sqrt",            new T(sqrt),
      "split",           new T(split),
      "stddev",          new T(stddev),
      "straightness",    new T(straightness),
      "structureId",     new T(strucid),
      "supercell",       new T(supercell),
      "sub",             new T(sub),
      "sum",             new T(sum), // sum
      "sum2",            new T(sum2), // sum of squares
      "surface",         new T(surface),
      "surfaceDistance", new T(surfacedistance),
      "symop",           new T(symop),
      "sx",              new T(screenx),
      "sy",              new T(screeny),
      "sz",              new T(screenz),
      "sxyz",            new T(screenxyz),
      "temperature",     new T(temperature),
      "relativeTemperature", null,
      "theta",           new T(theta),
      "thisModel",       new T(thismodel),
      "ticks",           new T(ticks),
      "top",             new T(top),
      "torsion",         new T(torsion),
      "trajectory",      new T(trajectory),
      "trajectories",    null,
      "translucent",     new T(translucent),
      "triangles",       new T(triangles),
      "trim",            new T(trim),
      "type",            new T(type),
      "ux",              new T(unitx),
      "uy",              new T(unity),
      "uz",              new T(unitz),
      "uxyz",            new T(unitxyz),
      "user",            new T(user),
      "valence",         new T(valence),
      "vanderWaals",     new T(vanderwaals),
      "vdw",             null,
      "vdwRadius",       null,
      "visible",         new T(visible),
      "volume",          new T(volume),
      "vx",              new T(vibx),
      "vy",              new T(viby),
      "vz",              new T(vibz),
      "vxyz",            new T(vibxyz),
      "xyz",             new T(xyz),
      "w",               new T(w),
      "x",               new T(x),
      "y",               new T(y),
      "z",               new T(z),

      // more misc parameters
      "addHydrogens",    new T(addhydrogens),
      "allConnected",    new T(allconnected),
      "angstroms",       new T(angstroms),
      "anisotropy",      new T(anisotropy),
      "append",          new T(append),
      "arc",             new T(arc),
      "area",            new T(area),
      "aromatic",        new T(aromatic),
      "arrow",           new T(arrow),
      "auto",            new T(auto),
      "barb",            new T(barb),
      "binary",          new T(binary),
      "blockData",       new T(blockdata),
      "cancel",          new T(cancel),
      "cap",             new T(cap),
      "cavity",          new T(cavity),
      "centroid",        new T(centroid),
      "check",           new T(check),
      "chemical",        new T(chemical),
      "circle",          new T(circle),
      "collapsed",       new T(collapsed),
      "col",             new T(col),
      "colorScheme",     new T(colorscheme),
      "command",         new T(command),
      "commands",        new T(commands),
      "contour",         new T(contour),
      "contours",        new T(contours),
      "corners",         new T(corners),
      "count",           new T(count),
      "criterion",       new T(criterion),
      "create",          new T(create),
      "crossed",         new T(crossed),
      "curve",           new T(curve),
      "cutoff",          new T(cutoff),
      "cylinder",        new T(cylinder),
      "diameter",        new T(diameter),
      "discrete",        new T(discrete),
      "distanceFactor",  new T(distancefactor),
      "downsample",      new T(downsample),
      "drawing",         new T(drawing),
      "eccentricity",    new T(eccentricity),
      "ed",              new T(ed),
      "edges",           new T(edges),
      "energy",          new T(energy),
      "exitJmol",        new T(exitjmol),
      "faceCenterOffset",new T(facecenteroffset),
      "filter",          new T(filter),
      "first",           new T(first),
      "fixed",           new T(fixed),
      "fix",             null,
      "flat",            new T(flat),
      "fps",             new T(fps),
      "from",            new T(from),
      "frontEdges",      new T(frontedges),
      "full",            new T(full),
      "fullPlane",       new T(fullplane),
      "functionXY",      new T(functionxy),
      "functionXYZ",     new T(functionxyz),
      "gridPoints",      new T(gridpoints),
      "homo",            new T(homo),
      "ignore",          new T(ignore),
      "InChI",           new T(inchi),
      "InChIKey",        new T(inchikey),
      "increment",       new T(increment),
      "insideout",       new T(insideout),
      "interior",        new T(interior),
      "intersection",    new T(intersection),
      "intersect",       null,
      "internal",        new T(internal),
      "lattice",         new T(lattice),
      "line",            new T(line),
      "lineData",        new T(linedata),
      "link",            new T(link),
      "lobe",            new T(lobe),
      "lonePair",        new T(lonepair),
      "lp",              new T(lp),
      "lumo",            new T(lumo),
      "manifest",        new T(manifest),
      "mapProperty",     new T(mapProperty),
      "map",             null,
      "maxSet",          new T(maxset),
      "menu",            new T(menu),
      "minSet",          new T(minset),
      "modelBased",      new T(modelbased),
      "molecular",       new T(molecular),
      "mrc",             new T(mrc),
      "msms",            new T(msms),
      "name",            new T(name),
      "nmr",             new T(nmr),
      "noCross",         new T(nocross),
      "noDebug",         new T(nodebug),
      "noEdges",         new T(noedges),
      "noHead",          new T(nohead),
      "noLoad",          new T(noload),
      "noPlane",         new T(noplane),
      "object",          new T(object),
      "obj",             new T(obj),
      "offset",          new T(offset),
      "offsetSide",      new T(offsetside),
      "once",            new T(once),
      "orbital",         new T(orbital),
      "atomicOrbital",   new T(atomicorbital),
      "packed",          new T(packed),
      "palindrome",      new T(palindrome),
      "parameters",      new T(parameters),
      "path",            new T(path),
      "pdb",             new T(pdb),
      "period",          new T(period),
      "periodic",        null,
      "perpendicular",   new T(perpendicular),
      "perp",            null,
      "phase",           new T(phase),
      "pocket",          new T(pocket),
      "pointsPerAngstrom", new T(pointsperangstrom),
      "radical",         new T(radical),
      "rad",             new T(rad),
      "reference",       new T(reference),
      "remove",          new T(remove),
      "resolution",      new T(resolution),
      "reverseColor",    new T(reversecolor),
      "rotate45",        new T(rotate45),
      "selection",       new T(selection),
      "sigma",           new T(sigma),
      "sign",            new T(sign),
      "silent",          new T(silent),
      "sphere",          new T(sphere),
      "squared",         new T(squared),
      "stop",            new T(stop),
      "title",           new T(title),
      "titleFormat",     new T(titleformat),
      "to",              new T(to),
      "value",           new T(val),
      "variable",        new T(variable),
      "variables",       new T(variables),
      "vertices",        new T(vertices),
      "width",           new T(width),

      // set params

      "backgroundModel",                          new T(backgroundmodel),
      "celShading",                                new T(celshading),
      "debug",                                    new T(debug),
      "defaultLattice",                           new T(defaultlattice),
      "measurements",                             new T(measurements),
      "measurement",                              null,
      "scale3D",                                  new T(scale3d),
      "toggleLabel",                              new T(togglelabel),
      "userColorScheme",                          new T(usercolorscheme),
      "timeout",                                  new T(timeout),
      "timeouts",                                 null,
      
      // string
      
      "appletProxy",                              new T(appletproxy),
      "atomTypes",                                new T(atomtypes),
      "axesColor",                                new T(axescolor),
      "axis1Color",                               new T(axis1color),
      "axis2Color",                               new T(axis2color),
      "axis3Color",                               new T(axis3color),
      "backgroundColor",                          new T(backgroundcolor),
      "bondmode",                                 new T(bondmode),
      "boundBoxColor",                            new T(boundboxcolor),
      "boundingBoxColor",                         null,
      "currentLocalPath",                         new T(currentlocalpath),
      "dataSeparator",                            new T(dataseparator),
      "defaultAngleLabel",                        new T(defaultanglelabel),
      "defaultColorScheme",                       new T(defaultcolorscheme),
      "defaultColors",                            null,
      "defaultDirectory",                         new T(defaultdirectory),
      "defaultDistanceLabel",                     new T(defaultdistancelabel),
      "defaultDropScript",                        new T(defaultdropscript), 
      "defaultLabelPDB",                          new T(defaultlabelpdb),
      "defaultLabelXYZ",                          new T(defaultlabelxyz),
      "defaultLoadFilter",                        new T(defaultloadfilter),
      "defaultLoadScript",                        new T(defaultloadscript),
      "defaults",                                 new T(defaults),
      "defaultTorsionLabel",                      new T(defaulttorsionlabel),
      "defaultVDW",                               new T(defaultvdw),
      "edsUrlCutoff",                             new T(edsurlcutoff),
      "edsUrlFormat",                             new T(edsurlformat),
      "energyUnits",                              new T(energyunits),
      "fileCacheDirectory",                       new T(filecachedirectory),
      "fontsize",                                 new T(fontsize),
      "helpPath",                                 new T(helppath),
      "hoverLabel",                               new T(hoverlabel),
      "language",                                 new T(language),
      "loadFormat",                               new T(loadformat),
      "loadLigandFormat",                         new T(loadligandformat),
      "logFile",                                  new T(logfile),
      "measurementUnits",                         new T(measurementunits),
      "nmrUrlFormat",                             new T(nmrurlformat),
      "pathForAllFiles",                          new T(pathforallfiles),
      "picking",                                  new T(picking),
      "pickingStyle",                             new T(pickingstyle),
      "pickLabel",                                new T(picklabel),
      "propertyColorScheme",                      new T(propertycolorscheme),
      "quaternionFrame",                          new T(quaternionframe),
      "smilesUrlFormat",                          new T(smilesurlformat),
      "smiles2dImageFormat",                      new T(smiles2dimageformat),
      "unitCellColor",                            new T(unitcellcolor),

      // float
      
      "axesScale",                                new T(axesscale),
      "axisScale",                                null, // legacy
      "bondTolerance",                            new T(bondtolerance),
      "cameraDepth",                              new T(cameradepth),
      "defaultDrawArrowScale",                    new T(defaultdrawarrowscale),
      "defaultTranslucent",                       new T(defaulttranslucent),
      "dipoleScale",                              new T(dipolescale),
      "ellipsoidAxisDiameter",                    new T(ellipsoidaxisdiameter),
      "gestureSwipeFactor",                       new T(gestureswipefactor),
      "hbondsAngleMinimum",                       new T(hbondsangleminimum),
      "hbondsDistanceMaximum",                    new T(hbondsdistancemaximum),
      "hoverDelay",                               new T(hoverdelay),
      "loadAtomDataTolerance",                    new T(loadatomdatatolerance),
      "minBondDistance",                          new T(minbonddistance),
      "minimizationCriterion",                    new T(minimizationcriterion),
      "mouseDragFactor",                          new T(mousedragfactor),
      "mouseWheelFactor",                         new T(mousewheelfactor),
      "navFPS",                                   new T(navfps),
      "navigationDepth",                          new T(navigationdepth),
      "navigationSlab",                           new T(navigationslab),
      "navigationSpeed",                          new T(navigationspeed),
      "navX",                                     new T(navx),
      "navY",                                     new T(navy),
      "navZ",                                     new T(navz),
      "pointGroupDistanceTolerance",              new T(pointgroupdistancetolerance),
      "pointGroupLinearTolerance",                new T(pointgrouplineartolerance),
      "radius",                                   new T(radius),
      "rotationRadius",                           new T(rotationradius),
      "scaleAngstromsPerInch",                    new T(scaleangstromsperinch),
      "sheetSmoothing",                           new T(sheetsmoothing),
      "slabRange",                                new T(slabrange),
      "solventProbeRadius",                       new T(solventproberadius),
      "spinFPS",                                  new T(spinfps),
      "spinX",                                    new T(spinx),
      "spinY",                                    new T(spiny),
      "spinZ",                                    new T(spinz),
      "stereoDegrees",                            new T(stereodegrees),
      "strutDefaultRadius",                       new T(strutdefaultradius),
      "strutLengthMaximum",                       new T(strutlengthmaximum),
      "vectorScale",                              new T(vectorscale),
      "vectorSymmetry",                           new T(vectorsymmetry),
      "vibrationPeriod",                          new T(vibrationperiod),
      "vibrationScale",                           new T(vibrationscale),
      "visualRange",                              new T(visualrange),

      // int

      "ambientPercent",                           new T(ambientpercent),
      "ambient",                                  null, 
      "animationFps",                             new T(animationfps),
      "axesMode",                                 new T(axesmode),
      "bondRadiusMilliAngstroms",                 new T(bondradiusmilliangstroms),
      "delayMaximumMs",                           new T(delaymaximumms),
      "diffusePercent",                           new T(diffusepercent),
      "diffuse",                                  null, 
      "dotDensity",                               new T(dotdensity),
      "dotScale",                                 new T(dotscale),
      "ellipsoidDotCount",                        new T(ellipsoiddotcount),
      "helixStep",                                new T(helixstep),
      "hermiteLevel",                             new T(hermitelevel),
      "historyLevel",                             new T(historylevel),
      "lighting",                                 new T(lighting),
      "logLevel",                                 new T(loglevel),
      "meshScale",                                new T(meshscale),
      "minimizationSteps",                        new T(minimizationsteps),
      "minPixelSelRadius",                        new T(minpixelselradius),
      "percentVdwAtom",                           new T(percentvdwatom),
      "perspectiveModel",                         new T(perspectivemodel),
      "phongExponent",                            new T(phongexponent),
      "pickingSpinRate",                          new T(pickingspinrate),
      "propertyAtomNumberField",                  new T(propertyatomnumberfield),
      "propertyAtomNumberColumnCount",            new T(propertyatomnumbercolumncount),
      "propertyDataColumnCount",                  new T(propertydatacolumncount),
      "propertyDataField",                        new T(propertydatafield),
      "repaintWaitMs",                            new T(repaintwaitms),
      "ribbonAspectRatio",                        new T(ribbonaspectratio),
      "scriptReportingLevel",                     new T(scriptreportinglevel),
      "showScript",                               new T(showscript),
      "smallMoleculeMaxAtoms",                    new T(smallmoleculemaxatoms),
      "specular",                                 new T(specular),
      "specularExponent",                         new T(specularexponent),
      "specularPercent",                          new T(specularpercent),
      "specPercent",                              null,
      "specularPower",                            new T(specularpower),
      "specpower",                                null, 
      "strandCount",                              new T(strandcount),
      "strandCountForMeshRibbon",                 new T(strandcountformeshribbon),
      "strandCountForStrands",                    new T(strandcountforstrands),
      "strutSpacing",                             new T(strutspacing),
      "zDepth",                                   new T(zdepth),
      "zSlab",                                    new T(zslab),
      "zshadePower",                              new T(zshadepower),

      // boolean

      "allowEmbeddedScripts",                     new T(allowembeddedscripts),
      "allowGestures",                            new T(allowgestures),
      "allowKeyStrokes",                          new T(allowkeystrokes),
      "allowModelKit",                            new T(allowmodelkit),
      "allowMoveAtoms",                           new T(allowmoveatoms),
      "allowMultiTouch",                          new T(allowmultitouch),
      "allowRotateSelected",                      new T(allowrotateselected),
      "antialiasDisplay",                         new T(antialiasdisplay),
      "antialiasImages",                          new T(antialiasimages),
      "antialiasTranslucent",                     new T(antialiastranslucent),
      "appendNew",                                new T(appendnew),
      "applySymmetryToBonds",                     new T(applysymmetrytobonds),
      "atomPicking",                              new T(atompicking),
      "autobond",                                 new T(autobond),
      "autoFPS",                                  new T(autofps),
//      "autoLoadOrientation",                      new Token(autoloadorientation),
      "axesMolecular",                            new T(axesmolecular),
      "axesOrientationRasmol",                    new T(axesorientationrasmol),
      "axesUnitCell",                             new T(axesunitcell),
      "axesWindow",                               new T(axeswindow),
      "bondModeOr",                               new T(bondmodeor),
      "bondPicking",                              new T(bondpicking),
      "bonds",                                    new T(bonds),
      "bond",                                     null, 
      "cartoonBaseEdges",                         new T(cartoonbaseedges),
      "cartoonFancy",                             new T(cartoonfancy),
      "cartoonRockets",                           new T(cartoonrockets),
      "chainCaseSensitive",                       new T(chaincasesensitive),
      "colorRasmol",                              new T(colorrasmol),
      "debugScript",                              new T(debugscript),
      "defaultStructureDssp",                     new T(defaultstructuredssp),
      "disablePopupMenu",                         new T(disablepopupmenu),
      "displayCellParameters",                    new T(displaycellparameters),
      "dotsSelectedOnly",                         new T(dotsselectedonly),
      "dotSurface",                               new T(dotsurface),
      "dragSelected",                             new T(dragselected),
      "drawHover",                                new T(drawhover),
      "drawPicking",                              new T(drawpicking),
      "dsspCalculateHydrogenAlways",              new T(dsspcalchydrogen),
      "dynamicMeasurements",                      new T(dynamicmeasurements),
      "ellipsoidArcs",                            new T(ellipsoidarcs),
      "ellipsoidAxes",                            new T(ellipsoidaxes),
      "ellipsoidBall",                            new T(ellipsoidball),
      "ellipsoidDots",                            new T(ellipsoiddots),
      "ellipsoidFill",                            new T(ellipsoidfill),
      "fileCaching",                              new T(filecaching),
      "fontCaching",                              new T(fontcaching),
      "fontScaling",                              new T(fontscaling),
      "forceAutoBond",                            new T(forceautobond),
      "fractionalRelative",                       new T(fractionalrelative),
// see commands     "frank",                                    new Token(frank),
      "greyscaleRendering",                       new T(greyscalerendering),
      "hbondsBackbone",                           new T(hbondsbackbone),
      "hbondsRasmol",                             new T(hbondsrasmol),
      "hbondsSolid",                              new T(hbondssolid),
      "hetero",                                   new T(hetero),
      "hideNameInPopup",                          new T(hidenameinpopup),
      "hideNavigationPoint",                      new T(hidenavigationpoint),
      "hideNotSelected",                          new T(hidenotselected),
      "highResolution",                           new T(highresolution),
      "hydrogen",                                 new T(hydrogen),
      "hydrogens",                                null,
      "imageState",                               new T(imagestate),
      "isKiosk",                                  new T(iskiosk),
      "isosurfaceKey",                            new T(isosurfacekey),
      "isosurfacePropertySmoothing",              new T(isosurfacepropertysmoothing),
      "isosurfacePropertySmoothingPower",         new T(isosurfacepropertysmoothingpower),
      "justifyMeasurements",                      new T(justifymeasurements),
      "languageTranslation",                      new T(languagetranslation),
      "legacyAutoBonding",                        new T(legacyautobonding),
      "logCommands",                              new T(logcommands),
      "logGestures",                              new T(loggestures),
      "measureAllModels",                         new T(measureallmodels),
      "measurementLabels",                        new T(measurementlabels),
      "measurementNumbers",                       new T(measurementnumbers),
      "messageStyleChime",                        new T(messagestylechime),
      "minimizationRefresh",                      new T(minimizationrefresh),
      "minimizationSilent",                       new T(minimizationsilent),
      "modelkitMode",                             new T(modelkitmode),
      "monitorEnergy",                            new T(monitorenergy),
      "multipleBondRadiusFactor",                 new T(multiplebondradiusfactor),
      "multipleBondSpacing",                      new T(multiplebondspacing),
      "multiProcessor",                           new T(multiprocessor),
      "navigateSurface",                          new T(navigatesurface),
      "navigationMode",                           new T(navigationmode),
      "navigationPeriodic",                       new T(navigationperiodic),
      "partialDots",                              new T(partialdots),
      "pdbAddHydrogens",                          new T(pdbaddhydrogens),
      "pdbGetHeader",                             new T(pdbgetheader),
      "pdbSequential",                            new T(pdbsequential),
      "perspectiveDepth",                         new T(perspectivedepth),
      "preserveState",                            new T(preservestate),
      "rangeSelected",                            new T(rangeselected),
      "redoMove",                                 new T(redomove),
      "refreshing",                               new T(refreshing),
      "ribbonBorder",                             new T(ribbonborder),
      "rocketBarrels",                            new T(rocketbarrels),
      "saveProteinStructureState",                new T(saveproteinstructurestate),
      "scriptQueue",                              new T(scriptqueue),
      "selectAllModels",                          new T(selectallmodels),
      "selectHetero",                             new T(selecthetero),
      "selectHydrogen",                           new T(selecthydrogen),
// see commands     "selectionHalos",                           new Token(selectionhalo),
      "showAxes",                                 new T(showaxes),
      "showBoundBox",                             new T(showboundbox),
      "showBoundingBox",                          null,
      "showFrank",                                new T(showfrank),
      "showHiddenSelectionHalos",                 new T(showhiddenselectionhalos),
      "showHydrogens",                            new T(showhydrogens),
      "showKeyStrokes",                           new T(showkeystrokes),
      "showMeasurements",                         new T(showmeasurements),
      "showMultipleBonds",                        new T(showmultiplebonds),
      "showNavigationPointAlways",                new T(shownavigationpointalways),
// see intparam      "showScript",                               new Token(showscript),
      "showTiming",                               new T(showtiming),
      "showUnitcell",                             new T(showunitcell),
      "slabByAtom",                               new T(slabbyatom),
      "slabByMolecule",                           new T(slabbymolecule),
      "slabEnabled",                              new T(slabenabled),
      "smartAromatic",                            new T(smartaromatic),
      "solvent",                                  new T(solvent),
      "solventProbe",                             new T(solventprobe),
// see intparam     "specular",                                 new Token(specular),
      "ssBondsBackbone",                          new T(ssbondsbackbone),
      "statusReporting",                          new T(statusreporting),
      "strutsMultiple",                           new T(strutsmultiple),
      "syncMouse",                                new T(syncmouse),
      "syncScript",                               new T(syncscript),
      "testFlag1",                                new T(testflag1),
      "testFlag2",                                new T(testflag2),
      "testFlag3",                                new T(testflag3),
      "testFlag4",                                new T(testflag4),
      "traceAlpha",                               new T(tracealpha),
      "twistedSheets",                            new T(twistedsheets),
      "undo",                                     new T(undo),
      "undoMove",                                 new T(undomove),
      "useArcBall",                               new T(usearcball),
      "useMinimizationThread",                    new T(useminimizationthread),
      "useNumberLocalization",                    new T(usenumberlocalization),
      "waitForMoveTo",                            new T(waitformoveto),
      "windowCentered",                           new T(windowcentered),
      "wireframeRotation",                        new T(wireframerotation),
      "zeroBasedXyzRasmol",                       new T(zerobasedxyzrasmol),
      "zoomEnabled",                              new T(zoomenabled),
      "zoomLarge",                                new T(zoomlarge),
      "zShade",                                   new T(zshade),

    };

    T tokenLast = null;
    String stringThis;
    T tokenThis;
    String lcase;
    for (int i = 0; i + 1 < arrayPairs.length; i += 2) {
      stringThis = (String) arrayPairs[i];
      lcase = stringThis.toLowerCase();
      tokenThis = (T) arrayPairs[i + 1];
      if (tokenThis == null)
        tokenThis = tokenLast;
      if (tokenThis.value == null)
        tokenThis.value = stringThis;
      if (tokenMap.get(lcase) != null)
        Logger.error("duplicate token definition:" + lcase);
      tokenMap.put(lcase, tokenThis);
      tokenLast = tokenThis;
    }
    arrayPairs = null;
    //Logger.info(arrayPairs.length + " script command tokens");
  }

  public static int getParamType(int tok) {
    if (!tokAttr(tok, setparam))
      return nada;
    return tok & paramTypes;
  }

}
