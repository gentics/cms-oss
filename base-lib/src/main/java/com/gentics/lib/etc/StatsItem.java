package com.gentics.lib.etc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * Statistics item
 */
public class StatsItem {
	/**
	 * Total time spent (in ns)
	 */
	protected AtomicLong totalTime = new AtomicLong();

	/**
	 * Number of samples
	 */
	protected AtomicLong numSamples = new AtomicLong();

	/**
	 * Number of samples per category
	 */
	protected Map<String, SubSamples> samplesPerCategory = new ConcurrentHashMap<String, SubSamples>();

	/**
	 * Start time of the current sample
	 */
	protected ThreadLocal<Long> startTime = new ThreadLocal<Long>() {
		/* (non-Javadoc)
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		protected Long initialValue() {
			return 0L;
		};
	};

	/**
	 * Current nesting level
	 */
	protected ThreadLocal<Integer> nestLevel = new ThreadLocal<Integer>() {
		/* (non-Javadoc)
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		protected Integer initialValue() {
			return 0;
		};
	};

	/**
	 * Get the total time (in ms)
	 * @return total time
	 */
	public long getTotalTime() {
		return totalTime.get() / 1000000L;
	}

	/**
	 * Get the number of samples
	 * @return number of samples
	 */
	public long getNumSamples() {
		return numSamples.get();
	}

	/**
	 * Get the average time in ms
	 * @return average time
	 */
	public long getAverageTime() {
		long totalMs = getTotalTime();
		if (numSamples.get() == 0) {
			return 0;
		} else {
			return Math.round((double)totalMs / (double)numSamples.get());
		}
	}

	/**
	 * Get the average number of samples per second
	 * @return average number of samples per second
	 */
	public long getAverageSamples() {
		long totalMs = getTotalTime();
		double totalS = totalMs / 1000.;
		double avg = numSamples.get() / totalS;
		return Math.round(avg);
	}

	/**
	 * Get statistics info
	 * @return stats info
	 */
	public String getInfo() {
		StringBuilder str = new StringBuilder();
		str.append("Invocations: ").append(numSamples).append("\tTime: ").append(getTotalTime()).append(" ms\tAvg: ").append(getAverageTime()).append(" ms/inv")
				.append("\t").append(getAverageSamples()).append(" inv/s");
		if (!samplesPerCategory.isEmpty()) {
			for (Map.Entry<String, SubSamples> entry : samplesPerCategory.entrySet()) {
				str.append("\n\t").append(entry.getKey()).append(": ").append(entry.getValue());
			}
		}
		return str.toString();
	}

	/**
	 * Start measuring a sample
	 */
	public void start() {
		int currentNestLevel = nestLevel.get();
		if (currentNestLevel == 0) {
			startTime.set(System.nanoTime());
		}
		nestLevel.set(currentNestLevel + 1);
	}

	/**
	 * Stop measuring a sample
	 */
	public void stop() {
		stop(1);
	}

	/**
	 * Stop measuring samples
	 * @param samples number of samples
	 */
	public void stop(int samples) {
		stop(samples, null);
	}

	/**
	 * Stop measuring samples
	 * @param samples number of samples
	 * @param category category of the samples
	 */
	public void stop(int samples, String category) {
		int currentNestLevel = nestLevel.get();
		if (currentNestLevel > 0) {
			currentNestLevel--;
			nestLevel.set(currentNestLevel);
			if (!ObjectTransformer.isEmpty(category)) {
				increaseNestedCategory(category, samples);
			}
			if (currentNestLevel == 0) {
				long duration = System.nanoTime() - startTime.get();
				totalTime.addAndGet(duration);
				numSamples.addAndGet(samples);
				if (!ObjectTransformer.isEmpty(category)) {
					increaseCategory(category, samples, duration);
				}
				startTime.set(0L);
			}
		} else {
			startTime.set(0L);
		}
	}

	/**
	 * Increase the number of samples for the given category
	 * @param category category
	 * @param samples number of samples
	 * @param duration duration in ns
	 */
	protected void increaseCategory(String category, int samples, long duration) {
		if (!samplesPerCategory.containsKey(category)) {
			synchronized (samplesPerCategory) {
				if (!samplesPerCategory.containsKey(category)) {
					samplesPerCategory.put(category, new SubSamples());
				}
			}
		}
		samplesPerCategory.get(category).increase(samples, duration);
	}

	/**
	 * Increase the number of nested samples for the given category
	 * @param category category
	 * @param samples number of samples
	 */
	protected void increaseNestedCategory(String category, int samples) {
		if (!samplesPerCategory.containsKey(category)) {
			synchronized (samplesPerCategory) {
				if (!samplesPerCategory.containsKey(category)) {
					samplesPerCategory.put(category, new SubSamples());
				}
			}
		}
		samplesPerCategory.get(category).increaseNested(samples);
	}

	/**
	 * Subsample class
	 */
	protected static class SubSamples {
		/**
		 * Number of samples
		 */
		protected AtomicLong subSamples = new AtomicLong();

		/**
		 * Number of nested samples
		 */
		protected AtomicLong nestedSamples = new AtomicLong();

		/**
		 * Total duration in ns
		 */
		protected AtomicLong subDuration = new AtomicLong();

		/**
		 * Increase the number of samples and the duration
		 * @param samples number of samples
		 * @param duration duration
		 */
		protected void increase(int samples, long duration) {
			subSamples.addAndGet(samples);
			subDuration.addAndGet(duration);
		}

		/**
		 * Increase the number of nested samples
		 * @param samples number of nested samples
		 */
		protected void increaseNested(int samples) {
			nestedSamples.addAndGet(samples);
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(subSamples.get()).append(" in ").append(subDuration.get() / 1000000L).append(" ms (nested ").append(nestedSamples.get()).append(")");
			return str.toString();
		}
	}
}
