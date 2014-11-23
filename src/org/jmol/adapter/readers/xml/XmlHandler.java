/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.adapter.readers.xml;

import java.io.BufferedReader;

import org.jmol.util.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

/**
 * a SAX handler
 */

public class XmlHandler extends DefaultHandler implements JmolXmlHandler {

  private XmlReader xmlReader;

  public XmlHandler() {
    // for reflection
  }
  
  public void parseXML(XmlReader xmlReader, Object saxReaderObj, BufferedReader reader) throws Exception {
    this.xmlReader = xmlReader;
    XMLReader saxReader = (XMLReader) saxReaderObj;
    saxReader.setFeature("http://xml.org/sax/features/validation", false);
    saxReader.setFeature("http://xml.org/sax/features/namespaces", true);
    saxReader.setEntityResolver(this);
    saxReader.setContentHandler(this);
    saxReader.setErrorHandler(this);
    InputSource is = new InputSource(reader);
    is.setSystemId("foo");
    saxReader.parse(is);
  }

  @Override
  public void startDocument() {
  }

  @Override
  public void endDocument() {
  }

  /*
   * see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
   * startElement and endElement should be extended in each reader
   */

  private String debugContext = "";

  @Override
  public void startElement(String namespaceURI, String localName, String qName,
                           Attributes attributes) {
    xmlReader.atts.clear();
    for (int i = attributes.getLength(); --i >= 0;)
      xmlReader.atts.put(attributes.getLocalName(i), attributes.getValue(i));
    if (Logger.debugging) {
      debugContext += " " + localName;
      Logger.debug(debugContext);
    }
    xmlReader.processStartElement(localName);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    if (Logger.debugging) {
      Logger.debug("");
      debugContext = debugContext.substring(0, debugContext.lastIndexOf(" "));
    }
    xmlReader.processEndElement(localName);
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    if (xmlReader.keepChars) {
      if (xmlReader.chars == null) {
        xmlReader.chars = new String(ch, start, length);
      } else {
        xmlReader.chars += new String(ch, start, length);
      }
    }
  }

  // Methods for entity resolving, e.g. getting a DTD resolved

  public InputSource resolveEntity(String name, String publicId,
                                   String baseURI, String systemId) {
    if (Logger.debugging) {
      Logger.debug("Not resolving this:" + "\n      name: " + name
          + "\n  systemID: " + systemId + "\n  publicID: " + publicId
          + "\n   baseURI: " + baseURI);
    }
    return null;
  }

  @Override
  public InputSource resolveEntity(String publicID, String systemID)
      throws SAXException {
    if (Logger.debugging) {
      Logger.debug("Jmol SAX EntityResolver not resolving:" + "\n  publicID: "
          + publicID + "\n  systemID: " + systemID);
    }
    return null;
  }

  @Override
  public void error(SAXParseException exception) {
    Logger.error("SAX ERROR:" + exception.getMessage());
  }

  @Override
  public void fatalError(SAXParseException exception) {
    Logger.error("SAX FATAL:" + exception.getMessage());
  }

  @Override
  public void warning(SAXParseException exception) {
    Logger.warn("SAX WARNING:" + exception.getMessage());
  }

}
