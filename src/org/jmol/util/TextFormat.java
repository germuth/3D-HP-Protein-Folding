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

package org.jmol.util;



//import java.text.DecimalFormat;






public class TextFormat {

//  private final static DecimalFormat[] formatters = new DecimalFormat[10];

  private final static String[] formattingStrings = { "0", "0.0", "0.00", "0.000",
      "0.0000", "0.00000", "0.000000", "0.0000000", "0.00000000", "0.000000000" };
  private final static String zeros = "0000000000000000000000000000000000000000";

  private final static float[] formatAdds = { 0.5f, 0.05f, 0.005f, 0.0005f,
    0.00005f, 0.000005f, 0.0000005f, 0.00000005f, 0.000000005f, 0.0000000005f };

  private final static Boolean[] useNumberLocalization = new Boolean[1];
  {
    useNumberLocalization[0] = Boolean.TRUE;
  }
  
  public static void setUseNumberLocalization(boolean TF) {
    useNumberLocalization[0] = (TF ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * a simple alternative to DecimalFormat (which Java2Script does not have
   * and which is quite too complex for our use here.
   * 
   * @param value
   * @param decimalDigits
   * @return  formatted decimal
   */
  public static String formatDecimal(float value, int decimalDigits) {
    if (decimalDigits == Integer.MAX_VALUE 
        || value == Float.NEGATIVE_INFINITY || value == Float.POSITIVE_INFINITY || Float.isNaN(value))
      return "" + value;
    int n;
    if (decimalDigits < 0) {
      decimalDigits = -decimalDigits;
      if (decimalDigits > formattingStrings.length)
        decimalDigits = formattingStrings.length;
      if (value == 0)
        return formattingStrings[decimalDigits] + "E+0";
      //scientific notation
      n = 0;
      double d;
      if (Math.abs(value) < 1) {
        n = 10;
        d = value * 1e-10;
      } else {
        n = -10;
        d = value * 1e10;
      }
      String s = ("" + d).toUpperCase();
      int i = s.indexOf("E");
      n = Parser.parseInt(s.substring(i + 1)) + n;
      return (i < 0 ? "" + value : formatDecimal(Parser.parseFloatStr(s.substring(
          0, i)), decimalDigits - 1)
          + "E" + (n >= 0 ? "+" : "") + n);
    }

    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
//    DecimalFormat formatter = formatters[decimalDigits];
//    if (formatter == null)
//      formatter = formatters[decimalDigits] = new DecimalFormat(
//          formattingStrings[decimalDigits]);
//    String s = formatter.format(value);

    String s1 = ("" + value).toUpperCase();
    boolean isNeg = s1.startsWith("-");
    if (isNeg)
      s1 = s1.substring(1);
    int pt = s1.indexOf(".");
    if (pt < 0)
      return s1 + formattingStrings[decimalDigits].substring(1);
    int pt1 = s1.indexOf("E-");
    if (pt1 > 0) {
      n = Parser.parseInt(s1.substring(pt1 + 1));
      // 3.567E-2
      // 0.03567
      s1 = "0." + zeros.substring(0, -n - 1) + s1.substring(0, 1) + s1.substring(2, pt1);
      pt = 1; 
    }

    pt1 = s1.indexOf("E");
    // 3.5678E+3
    // 3567.800000000
    // 1.234E10 %3.8f -> 12340000000.00000000
    if (pt1 > 0) {
      n = Parser.parseInt(s1.substring(pt1 + 1));
      s1 = s1.substring(0, 1) + s1.substring(2, pt1) + zeros;
      s1 = s1.substring(0, n + 1) + "." + s1.substring(n + 1);
      pt = s1.indexOf(".");
    } 
    // "234.345667  len == 10; pt = 3
    // "  0.0 "  decimalDigits = 1
    
    int len = s1.length();
    int pt2 = decimalDigits + pt + 1;
    if (pt2 < len && s1.charAt(pt2) >= '5') {
//      System.out.print(value + " " + s1 + "/" + s + " ");
      return formatDecimal(
          value + (isNeg ? -1 : 1) * formatAdds[decimalDigits], decimalDigits);
    }

    SB sb = SB.newS(s1.substring(0, (decimalDigits == 0 ? pt
        : ++pt)));
    for (int i = 0; i < decimalDigits; i++, pt++) {
      if (pt < len)
        sb.appendC(s1.charAt(pt));
      else
        sb.appendC('0');
    }
    s1 = (isNeg ? "-" : "") + sb;
//    System.out.print(value + " " + s1 + "/");
//    System.out.println(s);

    return (Boolean.TRUE.equals(useNumberLocalization[0]) ? s1 : s1.replace(',',
        '.'));
  }

  public static String formatF(float value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    return formatS(formatDecimal(value, precision), width, 0, alignLeft, zeroPad);
  }

  /**
   * 
   * @param value
   * @param width
   * @param precision
   * @param alignLeft
   * @param zeroPad
   * @param allowOverflow IGNORED
   * @return formatted string
   */
  public static String formatD(double value, int width, int precision,
                              boolean alignLeft, boolean zeroPad, boolean allowOverflow) {
    return formatS(formatDecimal((float)value, -1 - precision), width, 0, alignLeft, zeroPad);
  }

  /**
   * 
   * @param value       
   * @param width       number of columns
   * @param precision   precision > 0 ==> precision = number of characters max from left
   *                    precision < 0 ==> -1 - precision = number of char. max from right
   * @param alignLeft
   * @param zeroPad     generally for numbers turned strings
   * @return            formatted string
   */
  public static String formatS(String value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    if (value == null)
      return "";
    int len = value.length();
    if (precision != Integer.MAX_VALUE && precision > 0
        && precision < len)
      value = value.substring(0, precision);
    else if (precision < 0 && len + precision >= 0)
      value = value.substring(len + precision + 1);

    int padLength = width - value.length();
    if (padLength <= 0)
      return value;
    boolean isNeg = (zeroPad && !alignLeft && value.charAt(0) == '-');
    char padChar = (zeroPad ? '0' : ' ');
    char padChar0 = (isNeg ? '-' : padChar);

    SB sb = new SB();
    if (alignLeft)
      sb.append(value);
    sb.appendC(padChar0);
    for (int i = padLength; --i > 0;)
      // this is correct, not >= 0
      sb.appendC(padChar);
    if (!alignLeft)
      sb.append(isNeg ? padChar + value.substring(1) : value);
    return sb.toString();
  }

  public static String formatStringS(String strFormat, String key, String strT) {
    return formatString(strFormat, key, strT, Float.NaN, Double.NaN, false);
  }

  public static String formatStringF(String strFormat, String key, float floatT) {
    return formatString(strFormat, key, null, floatT, Double.NaN, false);
  }

  public static String formatStringI(String strFormat, String key, int intT) {
    return formatString(strFormat, key, "" + intT, Float.NaN, Double.NaN, false);
  }
   
  /**
   * sprintf emulation uses (almost) c++ standard string formats 's' string 'i'
   * or 'd' integer 'f' float/decimal 'p' point3f 'q' quaternion/plane/axisangle
   * ' with added "i" in addition to the insipid "d" (digits?)
   * 
   * @param strFormat
   * @param list
   * @param values
   * @return formatted string
   */
  public static String sprintf(String strFormat, String list, Object[] values) {
    if (values == null)
      return strFormat;
    int n = list.length();
    if (n == values.length)
      try {
        for (int o = 0; o < n; o++) {
          if (values[o] == null)
            continue;
          switch (list.charAt(o)) {
          case 's':
            strFormat = formatString(strFormat, "s", (String) values[o],
                Float.NaN, Double.NaN, true);
            break;
          case 'f':
            strFormat = formatString(strFormat, "f", null, ((Float) values[o])
                .floatValue(), Double.NaN, true);
            break;
          case 'i':
            strFormat = formatString(strFormat, "d", "" + values[o], Float.NaN,
                Double.NaN, true);
            strFormat = formatString(strFormat, "i", "" + values[o], Float.NaN,
                Double.NaN, true);
            break;
          case 'd':
            strFormat = formatString(strFormat, "e", null, Float.NaN,
                ((Double) values[o]).doubleValue(), true);
            break;
          case 'p':
            P3 pVal = (P3) values[o];
            strFormat = formatString(strFormat, "p", null, pVal.x, Double.NaN,
                true);
            strFormat = formatString(strFormat, "p", null, pVal.y, Double.NaN,
                true);
            strFormat = formatString(strFormat, "p", null, pVal.z, Double.NaN,
                true);
            break;
          case 'q':
            Point4f qVal = (Point4f) values[o];
            strFormat = formatString(strFormat, "q", null, qVal.x, Double.NaN,
                true);
            strFormat = formatString(strFormat, "q", null, qVal.y, Double.NaN,
                true);
            strFormat = formatString(strFormat, "q", null, qVal.z, Double.NaN,
                true);
            strFormat = formatString(strFormat, "q", null, qVal.w, Double.NaN,
                true);
            break;
          case 'S':
            String[] sVal = (String[]) values[o];
            for (int i = 0; i < sVal.length; i++)
              strFormat = formatString(strFormat, "s", sVal[i], Float.NaN,
                  Double.NaN, true);
            break;
          case 'F':
            float[] fVal = (float[]) values[o];
            for (int i = 0; i < fVal.length; i++)
              strFormat = formatString(strFormat, "f", null, fVal[i],
                  Double.NaN, true);
            break;
          case 'I':
            int[] iVal = (int[]) values[o];
            for (int i = 0; i < iVal.length; i++)
              strFormat = formatString(strFormat, "d", "" + iVal[i], Float.NaN,
                  Double.NaN, true);
            for (int i = 0; i < iVal.length; i++)
              strFormat = formatString(strFormat, "i", "" + iVal[i], Float.NaN,
                  Double.NaN, true);
            break;
          case 'D':
            double[] dVal = (double[]) values[o];
            for (int i = 0; i < dVal.length; i++)
              strFormat = formatString(strFormat, "e", null, Float.NaN,
                  dVal[i], true);
          }

        }
        return simpleReplace(strFormat, "%%", "%");
      } catch (Exception e) {
        //
      }
    System.out.println("TextFormat.sprintf error " + list + " " + strFormat);
    return simpleReplace(strFormat, "%", "?");
  }

  /**
   * generic string formatter  based on formatLabel in Atom
   * 
   * 
   * @param strFormat   .... %width.precisionKEY....
   * @param key      any string to match
   * @param strT     replacement string or null
   * @param floatT   replacement float or Float.NaN
   * @param doubleT  replacement double or Double.NaN -- for exponential
   * @param doOne    mimic sprintf    
   * @return         formatted string
   */

  private static String formatString(String strFormat, String key, String strT,
                                    float floatT, double doubleT, boolean doOne) {
    if (strFormat == null)
      return null;
    if ("".equals(strFormat))
      return "";
    int len = key.length();
    if (strFormat.indexOf("%") < 0 || len == 0 || strFormat.indexOf(key) < 0)
      return strFormat;

    String strLabel = "";
    int ich, ichPercent, ichKey;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) >= 0
        && (ichKey = strFormat.indexOf(key, ichPercent + 1)) >= 0;) {
      if (ich != ichPercent)
        strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ichKey > ichPercent + 6) {
        strLabel += '%';
        continue;//%12.10x
      }
      try {
        boolean alignLeft = false;
        if (strFormat.charAt(ich) == '-') {
          alignLeft = true;
          ++ich;
        }
        boolean zeroPad = false;
        if (strFormat.charAt(ich) == '0') {
          zeroPad = true;
          ++ich;
        }
        char ch;
        int width = 0;
        while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
          width = (10 * width) + (ch - '0');
          ++ich;
        }
        int precision = Integer.MAX_VALUE;
        boolean isExponential = false;
        if (strFormat.charAt(ich) == '.') {
          ++ich;
          if ((ch = strFormat.charAt(ich)) == '-') {
            isExponential = true;
            ++ich;
          } 
          if ((ch = strFormat.charAt(ich)) >= '0' && ch <= '9') {
            precision = ch - '0';
            ++ich;
          }
          if (isExponential)
            precision = -precision - (strT == null ? 1 : 0);
        }
        String st = strFormat.substring(ich, ich + len);
        if (!st.equals(key)) {
          ich = ichPercent + 1;
          strLabel += '%';
          continue;
        }
        ich += len;
        if (!Float.isNaN(floatT))
          strLabel += formatF(floatT, width, precision, alignLeft,
              zeroPad);
        else if (strT != null)
          strLabel += formatS(strT, width, precision, alignLeft,
              zeroPad);
        else if (!Double.isNaN(doubleT))
          strLabel += formatD(doubleT, width, precision, alignLeft,
              zeroPad, true);
        if (doOne)
          break;
      } catch (IndexOutOfBoundsException ioobe) {
        ich = ichPercent;
        break;
      }
    }
    strLabel += strFormat.substring(ich);
    //if (strLabel.length() == 0)
      //return null;
    return strLabel;
  }

  /**
   * 
   * formatCheck   checks p and q formats and duplicates if necessary
   *               "%10.5p xxxx" ==> "%10.5p%10.5p%10.5p xxxx" 
   * 
   * @param strFormat
   * @return    f or dupicated format
   */
  public static String formatCheck(String strFormat) {
    if (strFormat == null || strFormat.indexOf('p') < 0 && strFormat.indexOf('q') < 0)
      return strFormat;
    strFormat = simpleReplace(strFormat, "%%", "\1");
    strFormat = simpleReplace(strFormat, "%p", "%6.2p");
    strFormat = simpleReplace(strFormat, "%q", "%6.2q");
    String[] format = split(strFormat, '%');
    SB sb = new SB();
    sb.append(format[0]);
    for (int i = 1; i < format.length; i++) {
      String f = "%" + format[i];
      int pt;
      if (f.length() >= 3) {
        if ((pt = f.indexOf('p')) >= 0)
          f = fdup(f, pt, 3);
        if ((pt = f.indexOf('q')) >= 0)
          f = fdup(f, pt, 4);
      }
      sb.append(f);
    }
    return sb.toString().replace('\1', '%');
  }

  /**
   * 
   * fdup      duplicates p or q formats for formatCheck
   *           and the format() function.
   * 
   * @param f
   * @param pt
   * @param n
   * @return     %3.5q%3.5q%3.5q%3.5q or %3.5p%3.5p%3.5p
   */
  private static String fdup(String f, int pt, int n) {
    char ch;
    int count = 0;
    for (int i = pt; --i >= 1; ) {
      if (Character.isDigit(ch = f.charAt(i)))
        continue;
      switch (ch) {
      case '.':
        if (count++ != 0)
          return f;
        continue;
      case '-':
        if (i != 1)
          return f;
        continue;
      default:
        return f;
      }
    }
    String s = f.substring(0, pt + 1);
    SB sb = new SB();
    for (int i = 0; i < n; i++)
      sb.append(s);
    sb.append(f.substring(pt + 1));
    return sb.toString();
  }

  /**
   * 
   *  proper splitting, even for Java 1.3 -- if the text ends in the run,
   *  no new line is appended.
   * 
   * @param text
   * @param run
   * @return  String array
   */
  public static String[] splitChars(String text, String run) {
    if (text.length() == 0)
      return new String[0];
    int n = 1;
    int i = text.indexOf(run);
    String[] lines;
    int runLen = run.length();
    if (i < 0 || runLen == 0) {
      lines = new String[1];
      lines[0] = text;
      return lines;
    }
    int len = text.length() - runLen;
    for (; i >= 0 && i < len; n++)
      i = text.indexOf(run, i + runLen);
    lines = new String[n];
    i = 0;
    int ipt = 0;
    int pt = 0;
    for (; (ipt = text.indexOf(run, i)) >= 0 && pt + 1 < n;) {
      lines[pt++] = text.substring(i, ipt);
      i = ipt + runLen;
    }
    if (text.indexOf(run, len) != len)
      len += runLen;
    lines[pt] = text.substring(i, len);
    return lines;
  }

  /**
   * Does a clean replace of any of the characters in str with strTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param strTo
   * @return  replaced string
   */
  public static String replaceAllCharacters(String str, String strFrom,
                                            String strTo) {
    for (int i = strFrom.length(); --i >= 0;) {
      String chFrom = strFrom.substring(i, i + 1);
      str = simpleReplace(str, chFrom, strTo);
    }
    return str;
  }
  
  /**
   * Does a clean replace of any of the characters in str with chrTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param chTo
   * @return  replaced string
   */
  public static String replaceAllCharacter(String str, String strFrom,
                                            char chTo) {
    if (str == null)
      return null;
    for (int i = strFrom.length(); --i >= 0;)
      str = str.replace(strFrom.charAt(i), chTo);
    return str;
  }
  
  /**
   * Does a clean replace of strFrom in str with strTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param strTo
   * @return  replaced string
   */
  public static String simpleReplace(String str, String strFrom, String strTo) {
    if (str == null || str.indexOf(strFrom) < 0 || strFrom.equals(strTo))
      return str;
    int fromLength = strFrom.length();
    if (fromLength == 0)
      return str;
    boolean isOnce = (strTo.indexOf(strFrom) >= 0);
    int ipt;
    while (str.indexOf(strFrom) >= 0) {
      SB s = new SB();
      int ipt0 = 0;
      while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
        s.append(str.substring(ipt0, ipt)).append(strTo);
        ipt0 = ipt + fromLength;
      }
      s.append(str.substring(ipt0));
      str = s.toString();
      if (isOnce)
        break;
    }

    return str;
  }

  public static String trim(String str, String chars) {
    if (chars.length() == 0)
      return str.trim();
    int len = str.length();
    int k = 0;
    while (k < len && chars.indexOf(str.charAt(k)) >= 0)
      k++;
    int m = str.length() - 1;
    while (m > k && chars.indexOf(str.charAt(m)) >= 0)
      m--;
    return str.substring(k, m + 1);
  }

  public static String[] split(String text, char ch) {
    return splitChars(text, "" + ch);
  }
  
  public static void lFill(SB s, String s1, String s2) {
    s.append(s2);
    int n = s1.length() - s2.length();
    if (n > 0)
      s.append(s1.substring(0, n));
  }
  
  public static void rFill(SB s, String s1, String s2) {
    int n = s1.length() - s2.length();
    if (n > 0)
      s.append(s1.substring(0, n));
    s.append(s2);
  }
  
  public static String safeTruncate(float f, int n) {
    if (f > -0.001 && f < 0.001)
      f = 0;
    return (f + "         ").substring(0,n);
  }

  public static boolean isWild(String s) {
    return s != null && (s.indexOf("*") >= 0 || s.indexOf("?") >= 0);
  }

  public static boolean isMatch(String s, String strWildcard,
                                boolean checkStar, boolean allowInitialStar) {
    int ich = 0;
    int cchWildcard = strWildcard.length();
    int cchs = s.length();
    if (cchs == 0 || cchWildcard == 0)
      return (cchs == cchWildcard || cchWildcard == 1 && strWildcard.charAt(0) == '*');
    boolean isStar0 = (checkStar && allowInitialStar ? strWildcard.charAt(0) == '*' : false);
    if (isStar0 && strWildcard.charAt(cchWildcard - 1) == '*')
      return (cchWildcard < 3 || s.indexOf(strWildcard.substring(1,
          cchWildcard - 1)) >= 0); 
    String qqq = "????";
    while (qqq.length() < s.length())
      qqq += qqq;
    if (checkStar) {
      if (allowInitialStar && isStar0)
        strWildcard = qqq + strWildcard.substring(1);
      if (strWildcard.charAt(ich = strWildcard.length() - 1) == '*')
        strWildcard = strWildcard.substring(0, ich) + qqq;
      cchWildcard = strWildcard.length();
    }

    if (cchWildcard < cchs)
      return false;

    ich = 0;

    // atom name variant (trimLeadingMarks == false)

    // -- each ? matches ONE character if not at end
    // -- extra ? at end ignored

    //group3 variant (trimLeadingMarks == true)

    // -- each ? matches ONE character if not at end
    // -- extra ? at beginning reduced to match length
    // -- extra ? at end ignored

    while (cchWildcard > cchs) {
      if (allowInitialStar && strWildcard.charAt(ich) == '?') {
        ++ich;
      } else if (strWildcard.charAt(ich + cchWildcard - 1) != '?') {
        return false;
      }
      --cchWildcard;
    }

    for (int i = cchs; --i >= 0;) {
      char charWild = strWildcard.charAt(ich + i);
      if (charWild == '?')
        continue;
      if (charWild != s.charAt(i) && (charWild != '\1' || s.charAt(i) != '?'))
          return false;
    }
    return true;
  }

  public static String join(String[] s, char c, int i0) {
    if (s.length < i0)
      return null;
    SB sb = new SB();
    sb.append(s[i0++]);
    for (int i = i0; i < s.length; i++)
      sb.appendC(c).append(s[i]);
    return sb.toString();
  }

  public static String replaceQuotedStrings(String s, JmolList<String> list,
                                            JmolList<String> newList) {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      String name = list.get(i);
      String newName = newList.get(i);
      if (!newName.equals(name))
        s = simpleReplace(s, "\"" + name + "\"", "\"" + newName
            + "\"");
    }
    return s;
  }

  public static String replaceStrings(String s, JmolList<String> list,
                                      JmolList<String> newList) {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      String name = list.get(i);
      String newName = newList.get(i);
      if (!newName.equals(name))
        s = simpleReplace(s, name, newName);
    }
    return s;
  }

  /**
   * For @{....}
   * 
   * @param script
   * @param ichT
   * @param len
   * @return     position of "}"
   */
  public static int ichMathTerminator(String script, int ichT, int len) {
    int nP = 1;
    char chFirst = '\0';
    char chLast = '\0';
    while (nP > 0 && ++ichT < len) {
      char ch = script.charAt(ichT);
      if (chFirst != '\0') {
        if (chLast == '\\') {
          ch = '\0';
        } else if (ch == chFirst) {
          chFirst = '\0';
        }
        chLast = ch;
        continue;
      }
      switch(ch) {
      case '\'':
      case '"':
        chFirst = ch;
        break;
      case '{':
        nP++;
        break;
      case '}':
        nP--;
        break;
      }
    }
    return ichT;
  }

  /**
   * used by app to separate a command line into three sections:
   * 
   * prefix....;cmd ........ token
   * 
   * where token can be a just-finished single or double quote or
   * a string of characters
   * 
   * @param cmd
   * @return String[] {prefix, cmd..... token}
   */
  public static String[] splitCommandLine(String cmd) {
    String[] sout = new String[3];
    boolean isEscaped1 = false;
    boolean isEscaped2 = false;
    boolean isEscaped = false;
    if (cmd.length() == 0)
      return null;
    int ptQ = -1;
    int ptCmd = 0;
    int ptToken = 0;
    for (int i = 0; i < cmd.length(); i++) {
      switch(cmd.charAt(i)) {
      case '"':
        if (!isEscaped && !isEscaped1) {
          isEscaped2 = !isEscaped2;
          if (isEscaped2)
            ptQ = ptToken = i;
        }
        break;
      case '\'':
        if (!isEscaped && !isEscaped2) {
          isEscaped1 = !isEscaped1;
          if (isEscaped1)
            ptQ = ptToken = i;
        }
        break;
      case '\\':
        isEscaped = !isEscaped;
        continue;
      case ' ':
        if (!isEscaped && !isEscaped1 && !isEscaped2) {
          ptToken = i + 1;
          ptQ = -1;
        }
        break;
      case ';':
        if (!isEscaped1 && !isEscaped2) {
          ptCmd = ptToken = i + 1;
          ptQ = -1;
        }
        break;
      default:
        if (!isEscaped1 && !isEscaped2)
          ptQ = -1;
      }
      isEscaped = false;        
     }
    sout[0] = cmd.substring(0, ptCmd);
    sout[1] = (ptToken == ptCmd ? cmd.substring(ptCmd) : cmd.substring(ptCmd, (ptToken > ptQ ? ptToken : ptQ)));
    sout[2] = (ptToken == ptCmd ? null : cmd.substring(ptToken));
    return sout;
  }

}
