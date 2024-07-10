package com.gentics.contentnode.tools.update;

/**
 * Logger for some action
 */
public class Logger implements AutoCloseable {
	protected long start;

	/**
	 * Create an instance, will output the message to stdout (without newline)
	 * @param message message
	 */
	public Logger(String message) {
		start = System.currentTimeMillis();
		System.out.print(message + "...");
	}

	/**
	 * Outputs a dot to stdout
	 */
	public void dot() {
		System.out.print(".");
	}

	/**
	 * Output "done" to stdout (followed by newline)
	 */
	@Override
	public void close() {
		long duration = System.currentTimeMillis() - start;
		System.out.println();
		System.out.println(String.format("...done, (took %d ms)", duration));
	}
}
