package org.jmol.api;

import java.io.IOException;
import java.io.OutputStream;

public interface JmolImageCreatorInterface {

  abstract public JmolImageCreatorInterface setViewer(JmolViewer viewer, double privateKey);

  abstract public String getClipboardText();

  /**
   * 
   * @param fileName
   * @param type
   * @param text
   * @param bytes
   * @param scripts
   * @param appendix (byte[] or Image ?)
   * @param quality
   * @return null (canceled) or a message starting with OK or an error message
   */
  abstract public Object createImage(String fileName, String type, String text,
                                     Object bytes, String[] scripts,
                                     Object appendix, int quality);

  abstract public Object getImageBytes(String type, int quality,
                                       String fileName, String[] scripts,
                                       Object objImage, Object appendix,
                                       OutputStream os) throws IOException;

  abstract String clipImage(JmolViewer viewer, String text);

}
