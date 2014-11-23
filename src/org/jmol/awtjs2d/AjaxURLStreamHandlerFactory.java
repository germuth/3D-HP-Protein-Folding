package org.jmol.awtjs2d;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;
import java.util.Map;

/**
 * 
 * For handling URL file IO via AJAX in JavaScript version
 * 
 */

class AjaxURLStreamHandlerFactory implements URLStreamHandlerFactory {

	Map<String, AjaxURLStreamHandler> htFactories = new Hashtable<String, AjaxURLStreamHandler>();
	
	public URLStreamHandler createURLStreamHandler(String protocol) {
		AjaxURLStreamHandler fac = htFactories.get(protocol);
		if (fac == null)
			htFactories.put(protocol, fac = new AjaxURLStreamHandler(protocol));
		return (fac.protocol == null ? null : fac);
	}

}
