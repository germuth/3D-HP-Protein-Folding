package org.jmol.awtjs2d;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownServiceException;

import org.jmol.api.JmolFileAdapterInterface;

public class JmolFileAdapter implements JmolFileAdapterInterface {

  public Object getBufferedFileInputStream(String name) {
  	// this could be replaced by JavaScript
    try {
      throw new UnknownServiceException("No local file reading in JavaScript version of Jmol");
    } catch (IOException e) {
      return e.toString();
    }
  }

	public Object getBufferedURLInputStream(URL url, byte[] outputBytes,
			String post) {
		try {
			JmolURLConnection conn = (JmolURLConnection) url.openConnection();
			if (outputBytes != null)
				conn.outputBytes(outputBytes);
			else if (post != null)
				conn.outputString(post);
			return conn.getStringXBuilder();
		} catch (IOException e) {
			return e.toString();
		}
	}


}
