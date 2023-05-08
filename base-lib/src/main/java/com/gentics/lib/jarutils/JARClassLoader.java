/*
 * Class.java
 *
 * Created on 22. Juli 2004, 21:15
 */
package com.gentics.lib.jarutils;

// import
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Dietmar
 */
public class JARClassLoader extends URLClassLoader {
	private URL url = null;

	/** Creates a new instance of Class */
	public JARClassLoader(URL url) {
		super(new URL[] { url });
		this.url = url;
	}

	/* !TODO i need a method for retrieving the main class?? */
	// not yet

	/* method for loading a class from this jar file */
	public Class retrieveClass(String ClassName) throws ClassNotFoundException {
		Class ret = this.loadClass(ClassName);

		return ret;
	}
}
