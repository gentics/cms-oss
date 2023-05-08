package com.gentics.contentnode.publish;

import com.gentics.lib.etc.IWorkPhase;

/**
 * {@link AutoCloseable} wrapper around an instance of {@link IWorkPhase}.
 * The wrapped instance may also be null
 */
public class WorkPhaseHandler implements AutoCloseable {
	/**
	 * Wrapped instance
	 */
	protected IWorkPhase wrapped;

	/**
	 * Create an instance and {@link IWorkPhase#begin()} the workphase (if not null)
	 * @param wrapped wrapped instance (may be null)
	 */
	public WorkPhaseHandler(IWorkPhase wrapped) {
		this.wrapped = wrapped;

		if (this.wrapped != null) {
			this.wrapped.begin();
		}
	}

	/**
	 * Mark a unit of work done
	 */
	public void work() {
		work(1);
	}

	/**
	 * Mark the given number of units done
	 * @param units done units
	 */
	public void work(int units) {
		if (this.wrapped != null) {
			this.wrapped.doneWork(units);
		}
	}

	/**
	 * Add a unit of work
	 */
	public void addWork() {
		addWork(1);
	}

	/**
	 * Add units of work
	 * @param units work units
	 */
	public void addWork(int units) {
		if (this.wrapped != null) {
			this.wrapped.addWork(units);
		}
	}

	@Override
	public void close() {
		if (this.wrapped != null) {
			this.wrapped.done();
		}
	}
}
