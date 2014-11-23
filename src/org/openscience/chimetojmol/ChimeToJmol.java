package org.openscience.chimetojmol;

/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 
 * @author Bob Hanson <hansonr@stolaf.edu>
 */

public class ChimeToJmol {

  /*
   * Demonstrates a simple way to include an optional console along with the applet.
   * 
   */
  public static void main(String[] argv) {
    JFrame frame = new JFrame("ChimeToJmol");
    frame.addWindowListener(new ApplicationCloser());
    frame.setSize(700, 700);
    Container contentPane = frame.getContentPane();
    JPanel panel = new ChimePanel();
    contentPane.add(panel);
    frame.setVisible(true);
  }

  static class ApplicationCloser extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }


}
