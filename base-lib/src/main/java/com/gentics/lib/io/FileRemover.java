package com.gentics.lib.io;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

/**
 * Implements removing of files
 */
public class FileRemover implements Runnable {

	/**
	 * Name of the system property to configure the interval in ms
	 */
	public final static String INTERVAL_PARAM = "com.gentics.tests.fileremover.interval";

	/**
	 * Logger instance
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(FileRemover.class);

	/**
	 * File queue
	 */
	protected static Queue<FileWithInsertionTime> fileQueue = new ConcurrentLinkedQueue<FileWithInsertionTime>();

	/**
	 * Thread that does the background removal
	 */
	protected static Thread removeThread = null;

	/**
	 * Interval for tries to remove a file (in milliseconds)
	 */
	protected static int interval = 10000;

	/**
	 * Attempt to remove the file, if that is not possible, add the file to the queue
	 * @param file file to be removed
	 */
	public static void removeFile(File file) {
		if (!attemptRemoveFile(file)) {
			fileQueue.add(new FileWithInsertionTime(file));
			startThread();
		}
	}

	/**
	 * Attempt to remove the file
	 * @param file file
	 * @return true if the file was removed or does not exist, false if removal
	 *         has to be attempted later
	 */
	protected static boolean attemptRemoveFile(File file) {
		if (file == null) {
			return true;
		}
		if (!file.exists()) {
			return true;
		}
		return file.delete();
	}

	/**
	 * Start the thread if not yet running
	 */
	protected static synchronized void startThread() {
		if (removeThread == null) {
			interval = ObjectTransformer.getInt(System.getProperty(INTERVAL_PARAM), interval);
			removeThread = new Thread(new FileRemover());
			removeThread.start();
		}
	}

	/**
	 * Shut down the FileRemover. Stop the thread (if any running)
	 */
	public static void shutdown() {
		if (removeThread != null) {
			removeThread.interrupt();
			removeThread = null;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (!Thread.interrupted()) {
			try {
				Thread.sleep(interval);
				FileWithInsertionTime top = null;

				do {
					top = fileQueue.peek();
					if (top != null) {
						if (top.isOldEnough()) {
							top = fileQueue.poll();
							if (top != null) {
								removeFile(top.getFile());
							}
						} else {
							top = null;
						}
					}
				} while (top != null);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	/**
	 * Helper class to encapsulate a file with its insertion time
	 */
	protected static class FileWithInsertionTime {

		/**
		 * File
		 */
		protected File file = null;

		/**
		 * Insertion timestamp in ms
		 */
		protected long insertionTime = System.currentTimeMillis();

		/**
		 * Create an instance
		 * @param file file
		 */
		public FileWithInsertionTime(File file) {
			this.file = file;
		}

		/**
		 * Get the file
		 * @return file
		 */
		public File getFile() {
			return file;
		}

		/**
		 * Check whether the file is long enough in the queue
		 * @return true if the file is long enough in the queue, false if not
		 */
		public boolean isOldEnough() {
			return (System.currentTimeMillis() - insertionTime) > interval;
		}
	}
}
