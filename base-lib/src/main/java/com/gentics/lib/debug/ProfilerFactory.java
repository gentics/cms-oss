package com.gentics.lib.debug;

import java.util.HashMap;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 12.11.2003
 */
public class ProfilerFactory {
	private static HashMap m_profilers = new HashMap();

	public static Profiler getProfiler(String name) {
		Profiler p;

		p = (Profiler) m_profilers.get(name);
		if (p != null) {
			return p;
		}
		p = new Profiler(name);
		m_profilers.put(name, p);

		return p;
	}
}
