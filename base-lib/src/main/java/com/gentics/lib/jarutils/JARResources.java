/*
 * Class.java
 *
 * Created on 22. Juli 2004, 21:23
 */
package com.gentics.lib.jarutils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.gentics.lib.log.NodeLogger;

/**
 * JarResources: JarResources maps all resources included in the Jar file.
 */

public final class JARResources {

	// jar resource mapping tables
	private Hashtable htSizes = new Hashtable();

	private Hashtable htJarContents = new Hashtable();

	// the jar file
	private String jarFileName;

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Creates a new JarResource by opening a Jar File. All the Contents of the
	 * Jar Files are read and kept for easy access
	 * @param jarFileName a jar or zip file
	 */
	public JARResources(String jarFileName) {
		this.jarFileName = jarFileName;
		init();
	}

	/**
	 * Extracts a jar resource as a Binary Large Object
	 * @param name a resource name.
	 * @return a Binary Large Object for the given Resource File
	 */

	public byte[] getResource(String name) {
		return (byte[]) htJarContents.get(name);
	}

	/**
	 * initializes internal hash tables with Jar file resources.
	 */

	private void init() {

		try {
			// extracts just sizes only.
			ZipFile zf = new ZipFile(jarFileName);
			Enumeration e = zf.entries();

			while (e.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) e.nextElement();

				htSizes.put(ze.getName(), new Integer((int) ze.getSize()));
			}
			// closing
			zf.close();
			// extract resources and put them into the hashtable.
			FileInputStream fis = new FileInputStream(jarFileName);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ZipInputStream zis = new ZipInputStream(bis);
			ZipEntry ze = null;

			while ((ze = zis.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					continue;
				}
				int size = (int) ze.getSize();

				// -1 means unknown size.
				if (size == -1) {
					size = ((Integer) htSizes.get(ze.getName())).intValue();
				}
				byte[] b = new byte[(int) size];
				int rb = 0;
				int chunk = 0;

				while (((int) size - rb) > 0) {
					chunk = zis.read(b, rb, (int) size - rb);
					if (chunk == -1) {
						break;
					}
					rb += chunk;
				}
				// add to internal resource hashtable
				htJarContents.put(ze.getName(), b);
				// System.out.println("JAR: reading element " + ze.getName());
			}
			try {
				fis.close();
			} catch (Exception ex) {}
			try {
				bis.close();
			} catch (Exception ex) {}
			try {
				zis.close();
			} catch (Exception ex) {}
		} catch (NullPointerException e) {
			logger.debug("done.");
		} catch (FileNotFoundException e) {
			logger.error("error while initializing JARResources", e);
		} catch (IOException e) {
			logger.error("error while initializing JARResources", e);
		}
	}

	/**
	 * Dumps a zip entry into a string.
	 * @param ze a ZipEntry
	 */

	private String dumpZipEntry(ZipEntry ze) {

		StringBuffer sb = new StringBuffer();

		if (ze.isDirectory()) {
			sb.append("d ");
		} else {
			sb.append("f ");
		}
		if (ze.getMethod() == ZipEntry.STORED) {
			sb.append("stored   ");
		} else {
			sb.append("defalted ");
		}
		sb.append(ze.getName());
		sb.append("\t");
		sb.append("" + ze.getSize());
		if (ze.getMethod() == ZipEntry.DEFLATED) {
			sb.append("/" + ze.getCompressedSize());
		}
		return (sb.toString());
	}
}
