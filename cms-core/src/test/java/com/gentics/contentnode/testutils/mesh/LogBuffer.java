package com.gentics.contentnode.testutils.mesh;

import java.util.function.Consumer;

import org.testcontainers.containers.output.OutputFrame;

/**
 * Log Consumer that collects log
 */
public class LogBuffer implements Consumer<OutputFrame> {
	/**
	 * Internal buffer
	 */
	protected StringBuffer buffer = new StringBuffer();

	@Override
	public void accept(OutputFrame outputFrame) {
		buffer.append(outputFrame.getUtf8String());
	}

	/**
	 * Get the current buffered log
	 * @return buffered log
	 */
	public String get() {
		return buffer.toString();
	}

	/**
	 * Reset the buffer
	 */
	public void reset() {
		buffer = new StringBuffer();
	}
}
