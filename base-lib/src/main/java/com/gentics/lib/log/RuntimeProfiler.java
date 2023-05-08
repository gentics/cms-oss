package com.gentics.lib.log;

import java.io.EOFException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

public final class RuntimeProfiler {

	private RuntimeProfiler() {}

	/**
	 * timestamp when the profiler was last enabled.
	 * @see System#currentTimeMillis()
	 */
	private static long lastEnabledTimestamp = 0;

	private static boolean enabled = false;

	private static Map runningInvocationStore = Collections.synchronizedMap(new HashMap());

	private static Map deepestInvocationStore = Collections.synchronizedMap(new HashMap());

	private static Collection recordedInvocationStore = new Vector();

	private static NodeLogger logger = NodeLogger.getNodeLogger(RuntimeProfiler.class);

	private static int sessionCounter = 0;

	/**
	 * default value for the timeout
	 */
	private final static int TIMEOUT_DEFAULT = 30 * 60;

	/**
	 * Profiler timeout in seconds.
	 */
	private static int profilerTimeout = TIMEOUT_DEFAULT;

	/**
	 * Timer used to schedule tasks to auto-disable profiling run.
	 */
	private static Timer timer;

	/**
	 * Dependent on the java version this is either 'true' meaning that all timings are in
	 * nano seconds, 'false' in java 1.4 when timings are milli seconds.
	 */
	private static boolean currentTimeInNanos = false;
    
	/**
	 * This contains either 'System.currentTimeMillis' or 'System.nanoTime' depending on the java version.
	 */
	private static Method currentTimeMethod;

	private static RuntimeProfilerWriter profilerWriter;

	private static Thread profilerWriterThread;
    
	/**
	 * list of marks to exclude
	 */
	private static Properties excludes;
    
	/**
	 * value of properties in excludes to ignore whole mark.
	 */
	private static final String EXCLUDE_IGNORE_MARK = "ignoremark";
    
	/**
	 * value of properties in excludes to ignore only the invocation information of a mark.
	 */
	private static final String EXCLUDE_IGNORE_INVOCATION = "ignoreinvocation";

	public static final String CONFIGURATION_ROOT_PATH_KEY = "profiler.config.rootpath";
    
	private static String mode = "exclude";
    
	static {
		try {
			currentTimeMethod = System.class.getMethod("nanoTime", null);
			currentTimeInNanos = true;
		} catch (Exception e) {
			try {
				// because of any reason stacktrace was printed during startup - so not outputting exception..
				logger.warn("Unable to find 'nanoTime' method. - Using currentTimeMillis.");
				currentTimeMethod = System.class.getMethod("currentTimeMillis", null);
				currentTimeInNanos = false;
			} catch (Exception e1) {
				logger.fatal("Unable to find method System.currentTimeMillis", e1);
			}
		}

		boolean settingsLoaded = false;
		// load the custom profiler.properties (if existent) or the
		// minimal.settings from the profiler servlet otherwise
		File propFile = new File(getConfigurationRootPath(), "profiler.properties");

		try {
			if (propFile.exists()) {
				loadSettings(new FileInputStream(propFile));
				settingsLoaded = true;
			}
		} catch (Exception e) {
			logger.fatal("Error while loading settings from " + propFile.getAbsolutePath());
		}

		if (!settingsLoaded) {
			loadSettings(RuntimeProfiler.class.getResourceAsStream("minimal.properties"));
		}
	}

	/**
	 * Returns the configured rootpath
	 *  
	 * @param path
	 */
	public static String getConfigurationRootPath() {
		return ObjectTransformer.getString(System.getProperty(CONFIGURATION_ROOT_PATH_KEY), "");
	}

	public static void destroy() {
		timer.cancel();
		timer = null;
	}

	/**
	 * Set the given settings to the runtime profiler
	 * @param props new profiler settings
	 * @throws IllegalStateException when the profiler is currently enabled
	 */
	public static void setSettings(Properties props) {
		if (enabled) {
			throw new IllegalStateException("Cannot change profiler settings while the profiler is enabled");
		} else {
			excludes = new Properties();
			excludes.putAll(props);
			if (excludes.get("mode") != null) {
				mode = excludes.get("mode").toString();
			}
			if (!isExcludeMode()) {
				excludes.put("mark." + ComponentsConstants.GENTICSROOT, "true");
			}
			profilerTimeout = ObjectTransformer.getInt(excludes.get("timeout"), TIMEOUT_DEFAULT);
		}
	}

	public static Map getSettings() {
		return Collections.unmodifiableMap(excludes);
	}

	/**
	 * load profiler.properties file for include, excludes and timeout.
	 */
	public static void loadSettings(InputStream inputStream) {
		Properties props = new Properties();

		try {
			props.load(inputStream);
			setSettings(props);
		} catch (IOException e) {
			logger.error("Error while loading settings", e);
		}
	}
    
	/**
	 * Dependent on the java version this is either 'true' meaning that all
	 * timings are in nano seconds, 'false' in java 1.4 when timings are milli
	 * seconds.
	 */
	public static boolean isCurrentTimeInNanos() {
		return currentTimeInNanos;
	}
    
	protected static long getCurrentTime() {
		try {
			return ((Long) currentTimeMethod.invoke(null, null)).longValue();
		} catch (Exception e) {
			logger.fatal("Unable to get current time..", e);
			return 0;
		}
	}

	private synchronized static Timer getTimer() {
		if (timer == null) {
			timer = new Timer(true);
		}
	
		return timer;
	}

	/**
	 * remembered information of a single profilingspot.
	 * @author laurin
	 */
	public static class InvocationInformation implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1235277718645184357L;

		private long startTime;

		private long endTime;

		private String element;

		private Object instanceKey;

		private int recursionCounter = 0;

		private InvocationInformation mother;

		private List children;
        
		private long exclusiveCounter = 0;
        
		private long exclusiveStart = 0;

		public InvocationInformation(String element, Object instanceKey,
				InvocationInformation mother, long time) {
			this.startTime = time;
			this.element = element;
			this.instanceKey = instanceKey;
			this.recursionCounter = 1;
			this.mother = mother;
			this.children = new Vector();
			resume(time);
		}

		public void addChild(InvocationInformation child) {
			children.add(child);
		}

		public InvocationInformation getMother() {
			return mother;
		}

		public List getChildren() {
			return children;
		}

		public void setEndTime(long endTime) {
			this.endTime = endTime;
			pause(endTime);
		}

		public long getDuration() {
			return endTime - startTime;
		}

		public long getEndTime() {
			return endTime;
		}

		public long getStartTime() {
			return startTime;
		}

		public String getElement() {
			return element;
		}

		public Object getInstanceKey() {
			return instanceKey;
		}

		public void increaseRecursionCounter() {
			recursionCounter++;
		}

		public void decreaseRecursionCounter() {
			if (recursionCounter > 0) {
				recursionCounter--;
			}
		}

		public int getRecursionCounter() {
			return recursionCounter;
		}

		public void pause(long time) {
			if (exclusiveStart > 0) {
				exclusiveCounter += time - exclusiveStart;
				exclusiveStart = 0;
			} else {
				exclusiveStart--;
			}
		}

		public void resume(long time) {
			if (exclusiveStart < 0) {
				exclusiveStart++;
			} else {
				exclusiveStart = time;
			}
		}

		public long getExclusiveCounter() {
			return exclusiveCounter;
		}
	}

	public static void beginMark(String element) {
		beginMark(element, null);
	}

	/**
	 * asserts that there is currently no mother invocation in the store.
	 *
	 */
	public static void assertNoMotherInvocation() {
		if (!enabled) {
			return;
		}
		InvocationInformation mother = (InvocationInformation) deepestInvocationStore.get(Thread.currentThread());

		if (mother != null) {
			logger.error("Error, there was a deeper invocation ! {" + mother.getElement() + "} {" + mother.getInstanceKey() + "}");
		}
	}
    
	/**
	 * Clears the stored mother invocation for this thread.
	 * This should be called after you have called the endMark for the root object.
	 */
	public static void clearMotherInvocation() {
		if (!enabled) {
			return;
		}
		deepestInvocationStore.put(Thread.currentThread(), null);
	}

	/**
	 * begin recording and remember running element.
	 * @param element
	 * @param instanceKey
	 */
	public static void beginMark(String element, Object instanceKey) {
		if (enabled) {
			beginMark(element, instanceKey, getCurrentTime());
		}
	}
    
	protected static void beginMark(String element, Object instanceKey, long time) {
		if (enabled) {

			// handle excludes
			int ex = handleExcludes(element);

			if (ex == 1) {
				InvocationInformation mother = (InvocationInformation) deepestInvocationStore.get(Thread.currentThread());

				if (mother != null) {
					mother.pause(time);
				}
				return;
			} else if (ex == 2) {
				instanceKey = null;
			}
            
			if (profilerWriter != null) {
				profilerWriter.add(new ProfilerMarkBean(element, null == instanceKey ? null : instanceKey.toString(), time, true));
				return;
			}

			// profile generalized variant of this spot.
			if (instanceKey != null) {
				beginMark(element, null, time);
			}

			InvocationInformation mother = (InvocationInformation) deepestInvocationStore.get(Thread.currentThread());

			if (mother != null) {
				mother.pause(time);
			}

			String key = getKey(Thread.currentThread(), element, instanceKey);
            
			// Even within a recursion we want to create a new invocation information.
			InvocationInformation invoc = new InvocationInformation(element, instanceKey == null ? null : instanceKey.toString(), mother, time);
            
			if (runningInvocationStore.containsKey(key)) {
				// the same invocation was reported again (recursive invocation)
				InvocationInformation info = (InvocationInformation) runningInvocationStore.get(key);

				info.increaseRecursionCounter();
			} else {
				// get mother mark
				runningInvocationStore.put(key, invoc);
			}
			logger.debug("beginmark: element {" + key + "}");
			// remember current invocation in mother mark, if not root
			if (mother != null) {
				mother.addChild(invoc);
			}
			deepestInvocationStore.put(Thread.currentThread(), invoc);
		}
	}

	public static void endMark(String element) {
		endMark(element, null);
	}

	/**
	 * stop time recording of the current hotspot and remember recorded
	 * information.
	 * @param element
	 * @param instanceKey
	 */
	public static void endMark(String element, Object instanceKey) {
		if (enabled) {
			endMark(element, instanceKey, getCurrentTime());
		}
	}
    
	/**
	 * determine weither the given element has to be ignored completely, or unset the instancekey. 
	 * @param element the mark.
	 * @return 1 when the mark has to be ignored completely, 2 if the instance has to be ignored, 0 for non-ignore.
	 */
	private static int handleExcludes(String element) {
		// add prefix for property file match
		element = "mark." + element;
		// handle marks defined in exclude properties file
		if (excludes.containsKey(element)) {
			// ignore mark completely in exclude mode
			if (EXCLUDE_IGNORE_MARK.equals(excludes.get(element))) {
				return 1;
				// ignore instancekey
			} else if (EXCLUDE_IGNORE_INVOCATION.equals(excludes.get(element))) {
				return 2;
				// unspecific, ignore in exclude mode, don't ignore in include mode.
			} else {
				if (isExcludeMode()) {
					return 1;
				} else {
					return 0;
				}
			}
		} else {
			// not listed, don't ignore in exclude mode, ignore in include mode.
			if (isExcludeMode()) {
				return 0;
			} else {
				return 1;
			}
		}
	}
    
	public static boolean isExcludeMode() {
		return "exclude".equals(mode);
	}
    
	protected static void endMark(String element, Object instanceKey, long time) {
		if (enabled) {
            
			// handle excludes
			int ex = handleExcludes(element);

			if (ex == 1) {
				// resume the 
				InvocationInformation deepest = (InvocationInformation) deepestInvocationStore.get(Thread.currentThread());

				if (deepest != null) {
					deepest.resume(time);
				}

				return;
			} else if (ex == 2) {
				instanceKey = null;
			}
            
			if (profilerWriter != null) {
				profilerWriter.add(new ProfilerMarkBean(element, null == instanceKey ? null : instanceKey.toString(), time, false));
				return;
			}
			String key = getKey(Thread.currentThread(), element, instanceKey);
			InvocationInformation information = (InvocationInformation) runningInvocationStore.get(key);

			if (information != null) {
				// decrease the recursion counter
				information.decreaseRecursionCounter();
				if (information.getRecursionCounter() == 0) {
					// when the top recursion layer is reached again, we set the
					// end time and move the element into the recorded
					// invocation store
					information.setEndTime(time);
					runningInvocationStore.remove(key);
					recordedInvocationStore.add(information);
				}
				InvocationInformation child = (InvocationInformation) deepestInvocationStore.get(Thread.currentThread());

				if (logger.isDebugEnabled()) {
					logger.debug("endmark:   element {" + key + "}");
				}
				if (child == null) {
					logger.fatal("Called endMark without an open mark. (Ie. beginMark was called less often than endMark)");
				} else {
					if (child != information) {
						if (child.getElement() != null && !child.getElement().equals(information.getElement())) {
							logger.debug(
									"Called endMark with different instancekey than the deepest invocation was recorded. child was: element{" + child.getElement()
									+ "} instanceKey{" + child.getInstanceKey() + "} call was: element{" + information.getElement() + "} instanceKey{"
									+ information.getInstanceKey() + "}");
						}
						child.setEndTime(time);
					}
                    
					// set deepest mark to mother, because current mark is finished
					deepestInvocationStore.put(Thread.currentThread(), child.getMother());

					if (child.getMother() != null) {
						child.getMother().resume(time);
					}
				}

			} else {
				logger.warn("element {" + key + "} was not found in runningStore.");
			}

			// profile generalized variant of this spot.
			if (instanceKey != null) {
				endMark(element, null, time);
			}
		}
	}

	/**
	 * helper to generate a unique string, identifying the hotspot profiled.
	 * @param thread
	 * @param element
	 * @param instanceKey
	 * @return
	 */
	private static String getKey(Thread thread, String element, Object instanceKey) {
		StringBuffer str = new StringBuffer();

		str.append(thread.getName()).append(element).append(instanceKey);
		return str.toString();
	}

	/**
	 * Allows setting of all recorded objects.
	 * This can be used to restore previously stored snapshots.
	 * 
	 * @param recordedInvocationStore
	 */
	public static void setRecordedInvocationInformations(Collection recordedInvocationStore) {
		RuntimeProfiler.recordedInvocationStore = recordedInvocationStore;
	}

	/**
	 * get a copy of all recorded objects. currently running objects are not included.
	 * @return unmodifieableMap
	 */
	public static List getRecordedInvocationInformations() {
		return new Vector(recordedInvocationStore);
	}

	/**
	 * start recording, nothing happens if already started.
	 */
	public static void startRecording() {
		startRecording(null);
	}
    
	/**
	 * start recording, nothing happens if already started.
	 */
	public static void startRecording(String fileName) {
		if (!enabled) {
            
			// loadSettings();
            
			if (fileName != null && !"".equals(fileName)) {
				profilerWriter = new RuntimeProfilerWriter(new File(fileName));
				profilerWriterThread = new Thread(profilerWriter, "Profiler Writer");
				profilerWriterThread.start();
			}
		}
		enabled = true;
		lastEnabledTimestamp = System.currentTimeMillis();
		if (profilerTimeout != -1) {
			getTimer().schedule(getTimeoutTask(), profilerTimeout * 1000);
		}
	}
    
	/**
	 * Returns the seconds remaining until the timeout is reached
	 * @return -1 if not enabled, otherwise timeout in seconds until profiling is disabled again
	 */
	public static int getRemainingTimeout() {
		return !enabled || profilerTimeout == -1 ? -1 : ((int) (lastEnabledTimestamp + profilerTimeout * 1000 - System.currentTimeMillis()) / 1000);
	}

	private static TimerTask getTimeoutTask() {
		return new TimerTask() {
			public void run() {
				if (lastEnabledTimestamp + profilerTimeout * 1000 <= System.currentTimeMillis()) {
					enabled = false;
				}
			}
		};
	}

	/**
	 * end recording, currently running elements will be discarded.
	 */
	public static void endRecording() {
		enabled = false;
		runningInvocationStore.clear();
		if (profilerWriter != null && profilerWriterThread != null) {
			profilerWriterThread.interrupt();
			while (profilerWriterThread.isAlive()) {
				logger.info("Thread is still alive... waiting");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error("Got interrupted while waiting for thread to die.", e);
				}
			}
			profilerWriter = null;
			profilerWriterThread = null;
		}
	}

	/**
	 * weither recording is curerntly running.
	 * @return
	 */
	public static boolean isEnabled() {
		return enabled;
	}

	/**
	 * clear recorded data. will not stop running recording, and running data.
	 */
	public static void reset() {
		try {
			recordedInvocationStore.clear();
		} catch (UnsupportedOperationException e) {
			logger.debug("Got exception when trying to reset invocation store - instead replacing object.", e);
			recordedInvocationStore = new Vector();
		}
	}

	/**
	 * counts currently running recordings
	 * @return
	 */
	public static long countRunning() {
		return runningInvocationStore.size();
	}

	public static long countRecorded() {
		return recordedInvocationStore.size();
	}

	public static synchronized void sessionCreated() {
		sessionCounter++;
	}

	public static synchronized void sessionDestroyed() {
		sessionCounter--;
	}

	public static int getSessionCount() {
		return sessionCounter;
	}

	/**
	 * Sets the profiler timeout which will automatically deactivate the
	 * profiler after the given amount of time.
	 * @param profilerRunTimeout timeout in seconds.
	 */
	public static void setProfilerTimeout(int profilerRunTimeout) {
		RuntimeProfiler.profilerTimeout = profilerRunTimeout;
	}
    
	public static void loadProfilerMarks(String fileName) {
		try {
			File countFile = new File(fileName + ".count");
			int counter = -1;

			if (countFile.exists()) {
				FileInputStream in = new FileInputStream(countFile);
				byte[] buf = new byte[1024];
				int c = in.read(buf);

				counter = Integer.parseInt(new String(buf, 0, c));
			}
			loadProfilerMarks(new FileInputStream(new File(fileName)), counter);
		} catch (Exception e) {
			logger.error("Error while loading profiler marks from file {" + fileName + "}");
		}
	}

	public static void loadProfilerMarks(InputStream inputStream, int markcount) {
		try {
			endRecording();
			reset();
			startRecording();
			ObjectInputStream input = new ObjectInputStream(inputStream);
			Object obj;
			int i = 0;

			while ((obj = input.readObject()) != null) {
				logger.debug("Loading object (count: {" + (i++) + "})");
				if (markcount != -1 && (i % 10000) == 0) {
					int promille = (int) ((float) 1000 / (float) markcount * i);

					logger.info("  Loading object ... " + promille + "%o - " + i + "/" + markcount);
				}
				if (obj instanceof ProfilerMarkBean) {
					ProfilerMarkBean mark = (ProfilerMarkBean) obj;

					if (mark.isBegin()) {
						beginMark(mark.getElement(), mark.getObjectKey(), mark.getTime());
					} else {
						endMark(mark.getElement(), mark.getObjectKey(), mark.getTime());
					}
				} else if (obj instanceof Collection) {
					setRecordedInvocationInformations((Collection) obj);
				} else {
					throw new RuntimeException("Read unsupported object: {" + obj.getClass().getName() + "}");
				}
			}
		} catch (EOFException e) {
			// Ignore it.
			logger.debug("Got EOF Exception while loading profiler mark. - completely normal.", e);
		} catch (Exception e) {
			logger.error("Error while loading profiler marks.", e);
		}
		endRecording();
	}

	public static void loadSnapshot(String file) {
		loadProfilerMarks(file);
	}

	public static void loadSnapshot(InputStream inputStream) {
		loadProfilerMarks(inputStream, -1);
	}
}
