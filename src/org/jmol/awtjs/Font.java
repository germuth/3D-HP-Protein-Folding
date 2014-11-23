package org.jmol.awtjs;

import org.jmol.util.JmolFont;

/**
 * methods required by Jmol that access java.awt.Font
 * 
 * private to org.jmol.awt
 * 
 */

class Font {

	static Object newFont(String fontFace, boolean isBold, boolean isItalic,
			float fontSize) {
		return null;
	}

	static Object getFontMetrics(Object graphics, Object font) {
		return null;
	}

	static int getAscent(Object fontMetrics) {
		return 0;
	}

	static int getDescent(Object fontMetrics) {
		return 0;
	}

	static int stringWidth(JmolFont font, Object fontMetrics, String text) {
		return 0;
	}
}
