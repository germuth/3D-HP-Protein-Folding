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

package org.jmol.awt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.net.URL;

import javax.swing.JPanel;

import org.jmol.api.ApiPlatform;
import org.jmol.io2.JpegEncoder;
import org.jmol.util.Escape;
import org.jmol.util.JmolFont;
import org.jmol.viewer.Viewer;

/**
 * methods required by Jmol that access java.awt.Image
 * 
 * private to org.jmol.awt
 * 
 */
class Image {

  static Object createImage(Object data) {
    if (data instanceof URL)
      return Toolkit.getDefaultToolkit().createImage((URL) data);
    if (data instanceof String)
      return Toolkit.getDefaultToolkit().createImage((String) data);
    if (Escape.isAB(data))
      return Toolkit.getDefaultToolkit().createImage((byte[]) data);
    return null;
  }

  /**
   * @param display
   * @param image
   * @throws InterruptedException
   */
  static void waitForDisplay(Object display, Object image)
      throws InterruptedException {
    // this is important primarily for retrieving images from 
    // files, as in set echo ID myimage "image.gif"
    if (display == null)
      display = new JPanel();
    MediaTracker mediaTracker = new MediaTracker((Component) display);
    int rnd = (int) (Math.random() * 100000);
    mediaTracker.addImage((java.awt.Image) image, rnd);
    mediaTracker.waitForID(rnd);
  }

  static int getWidth(Object image) {
    return ((java.awt.Image) image).getWidth(null);
  }

  static int getHeight(Object image) {
    return ((java.awt.Image) image).getHeight(null);
  }

  static Object getJpgImage(ApiPlatform apiPlatform, Viewer viewer,
                            int quality, String comment) {
    BufferedImage eImage = (BufferedImage) viewer.getScreenImageBuffer(null);
    if (eImage == null)
      return null;
    if (quality < 0)
      quality = 75;
    Object bytes = JpegEncoder.getBytes(apiPlatform, eImage, quality, comment);
    viewer.releaseScreenImage();
    return bytes;
  }

  static int[] grabPixels(Object imageobj, int width, int height, 
                          int[] pixels, int startRow, int nRows) {
    java.awt.Image image = (java.awt.Image) imageobj;
    PixelGrabber pixelGrabber = (pixels == null ? new PixelGrabber(image, 0,
        0, width, height, true) : new PixelGrabber(image, 0, startRow, width, nRows, pixels, 0,
            width));
    try {
      pixelGrabber.grabPixels();
    } catch (InterruptedException e) {
      return null;
    }
    return (int[]) pixelGrabber.getPixels();
  }

  static int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                 Object imageobj, int width, int height,
                                 int bgcolor) {
    Graphics g = (Graphics) gOffscreen;
    java.awt.Image image = (java.awt.Image) imageobj;
    int width0 = image.getWidth(null);
    int height0 = image.getHeight(null);
    if (g instanceof Graphics2D) {
      ((Graphics2D) g).setComposite(AlphaComposite.getInstance(
          AlphaComposite.SRC_IN, 1.0f));
      g.setColor(bgcolor == 0 ? new Color(0, 0, 0, 0) : new Color(bgcolor));
      g.fillRect(0, 0, width, height);
      ((Graphics2D) g).setComposite(AlphaComposite.getInstance(
          AlphaComposite.SRC_OVER, 1.0f));
      g.drawImage(image, 0, 0, width, height, 0, 0, width0, height0, null);
    } else {
      g.clearRect(0, 0, width, height);
      g.drawImage(image, 0, 0, width, height, 0, 0, width0, height0, null);
    }
    return grabPixels(imageOffscreen, width, height, null, 0, 0);
  }

  public static int[] getTextPixels(String text, JmolFont font3d, Object gObj,
                                    Object image, int width, int height,
                                    int ascent) {
    Graphics g = (Graphics) gObj;
    g.setColor(Color.black);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.white);
    g.setFont((Font) font3d.font);
    g.drawString(text, 0, ascent);
    return grabPixels(image, width, height, null, 0, 0);
  }

  static Object newBufferedImage(Object image, int w, int h) {
    return new BufferedImage(w, h, ((BufferedImage) image).getType());
  }

  static Object newBufferedImage(int w, int h) {
    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  }

  /*
  private final static DirectColorModel rgbColorModelT =
    new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000);

  private final static int[] sampleModelBitMasksT =
  { 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000 };
  */

  private final static DirectColorModel rgbColorModel = new DirectColorModel(
      24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x00000000);

  private final static int[] sampleModelBitMasks = { 0x00FF0000, 0x0000FF00,
      0x000000FF };

  /**
   * @param windowWidth
   * @param windowHeight
   * @param pBuffer
   * @param windowSize
   * @param backgroundTransparent
   * @return an Image
   */
  static Object allocateRgbImage(int windowWidth, int windowHeight,
                                 int[] pBuffer, int windowSize,
                                 boolean backgroundTransparent) {
    //backgroundTransparent not working with antialiasDisplay. I have no idea why. BH 9/24/08
    /* DEAD CODE   if (false && backgroundTransparent)
          return new BufferedImage(
              rgbColorModelT,
              Raster.createWritableRaster(
                  new SinglePixelPackedSampleModel(
                      DataBuffer.TYPE_INT,
                      windowWidth,
                      windowHeight,
                      sampleModelBitMasksT), 
                  new DataBufferInt(pBuffer, windowSize),
                  null),
              false, 
              null);
    */
    return new BufferedImage(rgbColorModel, Raster.createWritableRaster(
        new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, windowWidth,
            windowHeight, sampleModelBitMasks), new DataBufferInt(pBuffer,
            windowSize), null), false, null);
  }

  /**
   * @param image
   * @param backgroundTransparent
   * @return Graphics object
   */
  static Object getStaticGraphics(Object image, boolean backgroundTransparent) {
    Graphics2D g2d = ((BufferedImage) image).createGraphics();
    //if (backgroundTransparent) {
    // what here?
    //}
    // miguel 20041122
    // we need to turn off text antialiasing on OSX when
    // running in a web browser
    
    // Despite the above comment, 13.1.8 adds text antialiasing
    // for all conditions -- Bob Hanson, Oct. 27, 2012
    
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    // I don't know if we need these or not, but cannot hurt to have them
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_OFF);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_SPEED);
    return g2d;
  }

  static Object getGraphics(Object image) {
    return ((java.awt.Image) image).getGraphics();
  }

  /**
   * 
   * @param g
   * @param img
   * @param x
   * @param y
   * @param width
   *        unused in Jmol proper
   * @param height
   *        unused in Jmol proper
   */
  static void drawImage(Object g, Object img, int x, int y, int width,
                        int height) {
    ((Graphics) g).drawImage((java.awt.Image) img, x, y, null);
  }

  static void flush(Object image) {
    ((java.awt.Image) image).flush();
  }

  static void disposeGraphics(Object graphicForText) {
    ((java.awt.Graphics) graphicForText).dispose();
  }

}
