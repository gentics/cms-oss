package com.gentics.lib.debug;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 26.08.2004
 */
public class Debug {

	/**
	 * CHANGE THIS, IF YOU WANT TO COMPILE-OUT ALL DEBUG SECTIONS!
	 */
	public static final boolean ENABLED = true;

	private static final HashMap writerMap = new HashMap();

	public static void init(Properties p) {
		if (p == null) {
			return;
		}
		Iterator it = p.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = (String) entry.getKey();

			if (key.startsWith("debug.")) {
				if (ObjectTransformer.getBoolean(entry.getValue(), false)) {
					PrintStream writer = System.out;

					writerMap.put(key.substring("debug.".length()), writer);
				} else {
					writerMap.remove(key.substring("debug.".length()));
				}
			}
		}
	}

	public static boolean isEnabled(String section) {
		return writerMap.containsKey(section);
	}

	/**
	 * returns only a valid printwriter, if isEnabled(section) returns true
	 * @param section
	 * @return
	 */
	public static PrintStream getWriter(String section) {
		return (PrintStream) writerMap.get(section);
	}

	public static void print(String section, String value) {
		if (Debug.ENABLED) {
			if (isEnabled(section)) {
				getWriter(section).print(value);
			} else if ("allways".equals(section)) {
				System.out.print(value);
			}
		}
	}

	public static void println(String section, String value) {
		if (Debug.ENABLED) {
			if (isEnabled(section)) {
				getWriter(section).println(value);
			} else if ("allways".equals(section)) {
				System.out.println(value);
			}
		}
	}
}
