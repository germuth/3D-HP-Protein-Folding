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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.awtjs;


import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolFont;
import org.jmol.viewer.Viewer;

/**
 * methods required by Jmol that access java.awt.Image
 * 
 * private to org.jmol.awt
 * 
 */
class Image {

	/**
   * @param imageobj  
	 * @param width 
	 * @param height 
	 * @return null
   */
	static int[] grabPixels(Object imageobj, int width, int height) {
		return null;
	}

	/**
   * @param gOffscreen  
	 * @param imageOffscreen 
	 * @param imageobj 
	 * @param width 
	 * @param height 
	 * @param bgcolor 
	 * @return null
   */
	static int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
			Object imageobj, int width, int height, int bgcolor) {
		return null;
	}

	/**
   * @param text  
	 * @param font3d 
	 * @param gObj 
	 * @param image 
	 * @param width 
	 * @param height 
	 * @param ascent 
   * @return null
   */
	public static int[] getTextPixels(String text, JmolFont font3d, Object gObj,
			Object image, int width, int height, int ascent) {
		return null;
	}

	/**
   * @param image  
	 * @param w 
	 * @param h 
   * @return null
   */
	static Object newBufferedImage(Object image, int w, int h) {
		return null;
	}

	/**
   * @param w  
	 * @param h 
   * @return null
   */
	static Object newBufferedImage(int w, int h) {
		return null;
	}

	/**
	 * @param windowWidth
	 * @param windowHeight
	 * @param pBuffer
	 * @param windowSize
	 * @param backgroundTransparent
	 * @return an Image
	 */
	static Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent) {
		return null;
	}

	/**
	 * @param image
	 * @param backgroundTransparent
	 * @return Graphics object
	 */
	static Object getStaticGraphics(Object image, boolean backgroundTransparent) {
		return null;
	}

	/**
   * @param image  
   * @return null
   */
	static Object getGraphics(Object image) {
		return null;
	}

	/**
	 * 
	 * @param g
	 * @param img
	 * @param x
	 * @param y
	 * @param width
	 *          unused in Jmol proper
	 * @param height
	 *          unused in Jmol proper
	 */
	static void drawImage(Object g, Object img, int x, int y, int width,
			int height) {
	}

	/**
   * @param image  
   */
	static void flush(Object image) {
	}

	/**
   * @param graphicForText  
   */
	static void disposeGraphics(Object graphicForText) {
	}

}
