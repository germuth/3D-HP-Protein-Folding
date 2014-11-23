package org.jmol.awtjs;

import org.jmol.util.JmolFont;

/**
 * 
 * WebGL interface
 * 
 * @author Bob Hanson
 *
 */
public class Platform extends org.jmol.awtjs2d.Platform {

	// differences for WebGL are only in the area of fonts and images
  // (which are not implemented yet, anyway)

  @Override
	public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent) {
		return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
				windowSize, backgroundTransparent);
	}

  @Override
	public void disposeGraphics(Object gOffscreen) {
		Image.disposeGraphics(gOffscreen);
	}

  @Override
	public void drawImage(Object g, Object img, int x, int y, int width,
			int height) {
		Image.drawImage(g, img, x, y, width, height);
	}

  @Override
	public int[] grabPixels(Object imageobj, int width, int height, 
                          int[] pixels, int startRow, int nRows) {
    return null; // can't do this with a 3D canvas
	}

  @Override
	public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
			Object imageobj, int width, int height, int bgcolor) {
		return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width,
				height, bgcolor);
	}

  @Override
	public int[] getTextPixels(String text, JmolFont font3d, Object gObj,
			Object image, int width, int height, int ascent) {
		return Image
				.getTextPixels(text, font3d, gObj, image, width, height, ascent);
	}

  @Override
	public void flushImage(Object imagePixelBuffer) {
		Image.flush(imagePixelBuffer);
	}

  @Override
	public Object getGraphics(Object image) {
		return Image.getGraphics(image);
	}

  @Override
	public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
		return Image.getStaticGraphics(image, backgroundTransparent);
	}

  @Override
	public Object newBufferedImage(Object image, int w, int h) {
		return Image.newBufferedImage(image, w, h);
	}

  @Override
	public Object newOffScreenImage(int w, int h) {
		return Image.newBufferedImage(w, h);
	}

	// /// FONT

  @Override
	public int fontStringWidth(JmolFont font, Object fontMetrics, String text) {
		return Font.stringWidth(font, fontMetrics, text);
	}

  @Override
	public int getFontAscent(Object fontMetrics) {
		return Font.getAscent(fontMetrics);
	}

  @Override
	public int getFontDescent(Object fontMetrics) {
		return Font.getDescent(fontMetrics);
	}

  @Override
	public Object getFontMetrics(JmolFont font, Object graphics) {
		return Font.getFontMetrics(graphics, font);
	}

  @Override
	public Object newFont(String fontFace, boolean isBold, boolean isItalic,
			float fontSize) {
		return Font.newFont(fontFace, isBold, isItalic, fontSize);
	}

}
