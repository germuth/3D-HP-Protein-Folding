package org.jmol.api;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.util.Map;

import org.jmol.util.SB;
import org.jmol.viewer.Viewer;

public interface JmolDocument {

  void setStream(BufferedInputStream bis, boolean isBigEndian);

  void setStreamData(DataInputStream dataInputStream, boolean isBigEndian);

  long getPosition();

  SB getAllDataFiles(String binaryFileList, String firstFile);

  void getAllDataMapped(String replace, String string, Map<String, String> fileData);

  int swapBytesI(int nx);

  short swapBytesS(short s);

  void seek(long i);

  byte readByte() throws Exception;

  int readInt() throws Exception;

  long readLong() throws Exception;

  float readFloat() throws Exception;

  double readDouble() throws Exception;

  short readShort() throws Exception;

  int readUnsignedShort() throws Exception;

  String readString(int i) throws Exception;

  int readByteArray(byte[] b, int off, int len) throws Exception;

  void close();

  void setOutputStream(OutputStream os, Viewer viewer, double privateKey);

}
