package org.jmol.awtjs2d;

import org.jmol.api.JmolFileInterface;
import org.jmol.util.TextFormat;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

/**
 * 
 * A class that mimics java.io.File
 * 
 */

class JmolFile implements JmolFileInterface {

  private String name;
	private String fullName;

	public JmolFile(String name) {
  	this.name = name.replace('\\','/');
  	fullName = name;
  	if (!fullName.startsWith("/") && FileManager.urlTypeIndex(name) < 0)
  		fullName = Viewer.jsDocumentBase + "/" + fullName;
  	fullName = TextFormat.simpleReplace(fullName, "/./", "/");
  	name = name.substring(name.lastIndexOf("/") + 1);
  }

  public JmolFileInterface getParentAsFile() {
  	int pt = fullName.lastIndexOf("/");
  	return (pt < 0 ? null : new JmolFile(fullName.substring(0, pt)));
  }

	public String getAbsolutePath() {
		return fullName;
	}

	public String getName() {
    return name;
	}

	public boolean isDirectory() {
		return fullName.endsWith("/");
	}

	public long length() {
		return 0; // can't do this, shouldn't be necessary
	}

}
