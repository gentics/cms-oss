/*
 * Class.java
 *
 * Created on 22. Juli 2004, 21:44
 */

package com.gentics.lib.jarutils;

import java.io.File;

/**
 * @author Dietmar
 */
public class JARFileFilter implements java.io.FileFilter {

	/** Creates a new instance of Class */
	public JARFileFilter() {}

	public boolean accept(File CurrentFile) {
		boolean ret = false;
		String FileName = CurrentFile.getName();

		if (FileName.endsWith(".jar")) {
			ret = true;
		}
		return ret;
	}

}
