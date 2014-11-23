package org.jmol.g3d;

import org.jmol.util.JmolFont;
import org.jmol.util.P3i;

class TextString extends P3i {
  
  String text;
  JmolFont font;
  int argb, bgargb;

  void setText(String text, JmolFont font, int argb, int bgargb, int x, int y, int z) {
    this.text = text;
    this.font = font;
    this.argb = argb;
    this.bgargb = bgargb;
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  @Override
  public String toString() {
    return super.toString() + " " + text;
  }
}
