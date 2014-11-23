package org.jmol.adapter.readers.pymol;

import org.jmol.util.JmolList;
import java.util.Hashtable;

import java.util.Map;

import org.jmol.api.JmolDocument;
import org.jmol.util.SB;
import org.jmol.viewer.Viewer;

/**
 * generic Python Pickle file reader
 * only utilizing records needed for PyMOL.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
class PickleReader {

  private JmolDocument binaryDoc;
  private JmolList<Object> list = new  JmolList<Object>();
  private JmolList<Integer> marks = new  JmolList<Integer>();
  private JmolList<Object> build = new  JmolList<Object>();
  private boolean logging;
  private Viewer viewer;


  final private static byte APPEND = 97; /* a */
  final private static byte APPENDS = 101; /* e */
  final private static byte BINFLOAT = 71; /* G */
  final private static byte BININT = 74; /* J */
  final private static byte BININT1 = 75; /* K */
  final private static byte BININT2 = 77; /* M */
  final private static byte BINPUT = 113; /* q */
  final private static byte BINSTRING = 84; /* T */
  final private static byte BINUNICODE = 87; /* X */
  final private static byte BUILD = 98; /* b */
  final private static byte EMPTY_DICT = 125; /* } */
  final private static byte EMPTY_LIST = 93; /* ] */
  final private static byte GLOBAL = 99; /* c */
  final private static byte LONG_BINPUT = 114; /* r */
  final private static byte MARK = 40; /* ( */
  final private static byte NONE = 78; /* N */
  final private static byte OBJ = 111; /* o */
  final private static byte SETITEM = 115; /* s */
  final private static byte SETITEMS = 117; /* u */
  final private static byte SHORT_BINSTRING = 85; /* U */
  final private static byte STOP = 46; /* . */

  //  final private static byte BINGET = 104; /* h */
  //  final private static byte BINPERSID = 81; /* Q */
  //  final private static byte DICT = 100; /* d */
  //  final private static byte DUP = 50; /* 2 */
  //  final private static byte EMPTY_TUPLE = 41; /* ) */
  //  final private static byte FLOAT = 70; /* F */
  //  final private static byte GET = 103; /* g */
  //  final private static byte INST = 105; /* i */
  //  final private static byte INT = 73; /* I */
  //  final private static byte LIST = 108; /* l */
  //  final private static byte LONG = 76; /* L */
  //  final private static byte LONG_BINGET = 106; /* j */
  //  final private static byte PERSID = 80; /* P */
  //  final private static byte POP = 48; /* 0 */
  //  final private static byte POP_MARK = 49; /* 1 */
  //  final private static byte PUT = 112; /* p */
  //  final private static byte REDUCE = 82; /* R */
  //  final private static byte STRING = 83; /* S */
  //  final private static byte TUPLE = 116; /* t */
  //  final private static byte UNICODE = 86; /* V */

  PickleReader(JmolDocument doc, Viewer viewer) {
    binaryDoc = doc;
    logging = (viewer.getLogFile().length() > 0);
    if (logging)
      this.viewer = viewer;
  }

  private void log(String s) {
    viewer.log(s + "\0");
  }
  
  @SuppressWarnings("unchecked")
  Map<String, Object> getMap() throws Exception {
    String s, module, name;
    byte b;
    int i, mark;
    double d;
    Object o;
    byte[] a;
    Map<String, Object> map;
    JmolList<Object> l;
    boolean going = true;

    while (going) {
      b = binaryDoc.readByte();
      switch (b) {
      case EMPTY_DICT: //}
        push(new Hashtable<String, Object>());
        break;
      case APPEND:
        o = pop();
        ((JmolList<Object>) peek()).addLast(o);
        break;
      case APPENDS:
        l = getObjects(getMark());
        ((JmolList<Object>) peek()).addAll(l);
        break;
      case BINFLOAT:
        d = binaryDoc.readDouble();
        push(Double.valueOf(d));
        break;
      case BININT:
        i = binaryDoc.readInt();
        push(Integer.valueOf(i));
        break;
      case BININT1:
        i = binaryDoc.readByte() & 0xff;
        push(Integer.valueOf(i));
        break;
      case BININT2:
        i = (binaryDoc.readByte() & 0xff | ((binaryDoc.readByte() & 0xff) << 8)) & 0xffff;
        push(Integer.valueOf(i));
        break;
      case BINPUT:
        i = binaryDoc.readByte();
        //unnec? temp.put(Integer.valueOf(i), peek());
        break;
      case LONG_BINPUT:
        i = binaryDoc.readInt();
        //unnec? temp.put(Integer.valueOf(i), peek());
        break;
      case SHORT_BINSTRING:
        i = binaryDoc.readByte();
        a = new byte[i];
        binaryDoc.readByteArray(a, 0, i);
        s = new String(a, "UTF-8");
        push(s);
        break;
      case BINSTRING:
        i = binaryDoc.readInt();
        a = new byte[i];
        binaryDoc.readByteArray(a, 0, i);
        s = new String(a, "UTF-8");
        push(s);
        break;
      case BINUNICODE:
        i = binaryDoc.readInt();
        a = new byte[i];
        binaryDoc.readByteArray(a, 0, i);
        s = new String(a, "UTF-8");
        push(s);
        break;
      case EMPTY_LIST:
        push(new  JmolList<Object>());
        break;
      case GLOBAL:
        module = readString();
        name = readString();
        push(new String[] { "global", module, name });
        break;
      case BUILD:
        o = pop();
        build.addLast(o);
        //System.out.println("build");
        break;
      case MARK:
        i = list.size();
        if (logging)
          log("\n " + Integer.toHexString((int) binaryDoc.getPosition()) + " [");
        marks.addLast(Integer.valueOf(i));
        break;
      case NONE:
        push(null);
        break;
      case OBJ:
        //System.out.println("OBJ");
        push(getObjects(getMark()));
        break;
      case SETITEM:
        o = pop();
        s = (String) pop();
        ((Map<String, Object>) peek()).put(s, o);
        break;
      case SETITEMS:
        mark = getMark();
        l = getObjects(mark);
        map = (Map<String, Object>) peek();
        for (i = l.size(); --i >= 0;) {
          o = l.get(i);
          s = (String) l.get(--i);
          map.put(s, o);
        }
        break;
      case STOP:
        going = false;
        break;
      default:

        // not used?
        System.out.println("PyMOL reader error: " + b + " "
            + binaryDoc.getPosition());

        //        switch (b) {
        //        case BINGET:
        //          i = binaryDoc.readByte();
        //          push(temp.remove(Integer.valueOf(i)));
        //          break;
        //        case BINPERSID:
        //          s = (String) pop();
        //          push(new Object[] { "persid", s }); // for now
        //          break;
        //        case DICT:
        //          map = new Hashtable<String, Object>();
        //          mark = getMark();
        //          for (i = list.size(); i >= mark;) {
        //            o = list.remove(--i);
        //            s = (String) list.remove(--i);
        //            map.put(s, o);
        //          }
        //          push(map);
        //          break;
        //        case DUP:
        //          push(peek());
        //          break;
        //        case EMPTY_TUPLE:
        //          push(new Point3f());
        //          break;
        //        case FLOAT:
        //          s = readString();
        //          push(Double.valueOf(s));
        //          break;
        //        case GET:
        //          s = readString();
        //          o = temp.remove(s);
        //          push(o);
        //          break;
        //        case INST:
        //          l = getObjects(getMark());
        //          module = readString();
        //          name = readString();
        //          push(new Object[] { "inst", module, name, l });
        //          break;
        //        case INT:
        //          s = readString();
        //          try {
        //            push(Integer.valueOf(Integer.parseInt(s)));
        //          } catch (Exception e) {
        //            System.out.println("INT too large: " + s + " @ " + binaryDoc.getPosition());
        //            push(Integer.valueOf(Integer.MAX_VALUE));
        //          }
        //          break;
        //        case LIST:
        //          push(getObjects(getMark()));
        //          break;
        //        case LONG:
        //          i = (int) binaryDoc.readLong();
        //          push(Long.valueOf(i));
        //          break;
        //        case LONG_BINGET:
        //          i = binaryDoc.readInt();
        //          push(temp.remove(Integer.valueOf(i)));
        //          break;
        //        case PERSID:
        //          s = readString();
        //          push(new Object[] { "persid", s });
        //          break;
        //        case POP:
        //          pop();
        //          break;
        //        case POP_MARK:
        //          getObjects(getMark());
        //          break;
        //        case PUT:
        //          s = readString();
        //          temp.put(s, peek());
        //          break;
        //        case REDUCE:
        //          push(new Object[] { "reduce", pop(), pop() });
        //          break;
        //        case STRING:
        //          s = readString();
        //          push(Escape.unescapeUnicode(s));
        //          break;
        //        case TUPLE:
        //          l = getObjects(getMark());
        //          Point3f pt = new Point3f();
        //          pt.x = ((Double) l.get(0)).floatValue();
        //          pt.y = ((Double) l.get(1)).floatValue();
        //          pt.z = ((Double) l.get(2)).floatValue();
        //          break;
        //        case UNICODE:
        //          a = readLineBytes();
        //          s = new String(a, "UTF-8");
        //          push(s);
        //          break;
        //        }
      }
    }
    if (logging)
      log("");
    return (Map<String, Object>) list.remove(0);
  }
  
  private JmolList<Object> getObjects(int mark) {
    int n = list.size() - mark;
    JmolList<Object> args = new  JmolList<Object>();
    for (int j = 0; j < n; j++)
      args.addLast(null);
    for (int j = n, i = list.size(); --i >= mark;)
      args.set(--j, list.remove(i));
    return args;
  }

  //  private byte[] readLineBytes() throws Exception {
  //    String s = readString();
  //    return s.getBytes();
  //  }

  private String readString() throws Exception {
    SB sb = new SB();
    while (true) {
      byte b = binaryDoc.readByte();
      if (b == 0xA)
        break;
      sb.appendC((char) b);
    }
    return sb.toString();
  }

  private int getMark() {
    return marks.remove(marks.size() - 1).intValue();
  }

  private void push(Object o) {
    if (logging
        && (o instanceof String || o instanceof Double || o instanceof Integer))
      log((o instanceof String ? "'" + o + "'" : o) + ", ");
    list.addLast(o);
  }

  private Object peek() {
    return list.get(list.size() - 1);
  }

  private Object pop() {
    return list.remove(list.size() - 1);
  }

}
