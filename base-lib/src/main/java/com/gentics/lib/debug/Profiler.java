package com.gentics.lib.debug;

import java.util.Vector;

import com.gentics.lib.log.NodeLogger;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 12.11.2003
 */
public class Profiler {
	private String m_name;

	private Vector m_times = new Vector();

	private long m_startTime, m_stopTime;

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	public Profiler(String name) {
		m_name = name;
	}

	public void start() {
		logger.info("Profiling started for: " + m_name);
		m_startTime = System.currentTimeMillis();
	}

	public void addInfo(String key, Object info) {
		logger.info("Info for " + m_name + ": " + key + " = " + info);
	}

	public void mark(String name) {
		logger.info("Mark (" + name + ") reached at " + System.currentTimeMillis());
	}

	public void stop() {
		m_stopTime = System.currentTimeMillis();
		logger.info("Profiling finished. Total time: " + (m_stopTime - m_startTime));
	}
}
