package org.jmol.adapter.readers.xml;

import java.io.BufferedReader;

interface JmolXmlHandler {  

  void parseXML(XmlReader xmlReader, Object saxReader, BufferedReader reader) throws Exception;

}
