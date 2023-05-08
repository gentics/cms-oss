package com.gentics.lib.datasource;

import java.util.LinkedList;
import java.util.Random;

import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.lib.log.NodeLogger;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 17.09.2004
 * @deprecated use {@link com.gentics.lib.datasource.RoundRobinHandlePool} instead
 */
public class RandomHandlePool implements HandlePool {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	protected DatasourceHandle[] handles;

	protected DatasourceHandle dsHandle;

	private Random rnd;

	private int currentElement;

	/**
	 * string representation of the handle pool
	 */
	private String stringRepresentation;

	public RandomHandlePool(LinkedList handlesList) {

		if (handlesList == null) {
			throw new IllegalArgumentException("Cannot handle null-handles");
		}
		if (handlesList.size() == 0) {
			throw new IllegalArgumentException("Cannot handle empty handle-array");
		}
		rnd = new Random(System.currentTimeMillis());
		this.handles = (DatasourceHandle[]) handlesList.toArray();
		stringRepresentation = null;
		this.currentElement = 0;
		this.dsHandle = getHandle();
	}

	public RandomHandlePool(DatasourceHandle[] handles) {
		if (handles == null) {
			throw new IllegalArgumentException("Cannot handle null-handles");
		}
		if (handles.length == 0) {
			throw new IllegalArgumentException("Cannot handle empty handle-array");
		}
		rnd = new Random(System.currentTimeMillis());
		this.handles = handles;
		stringRepresentation = null;
		this.currentElement = 0;
		this.dsHandle = getHandle();
	}

	private DatasourceHandle getRandomHandle() {
		this.currentElement = this.rnd.nextInt(handles.length);
		this.dsHandle = (DatasourceHandle) handles[this.currentElement];
		if (logger.isDebugEnabled()) {
			logger.debug(
					"getRandomHandle() datasourceHandle[" + this.dsHandle.getClass().getName() + "] Number:" + (new Integer(this.currentElement).toString()));
		}
		return this.dsHandle;
	}

	public DatasourceHandle getHandle() {
		// this.currentElement = this.rnd.nextInt(handles.length);
		// this.dsHandle = (DatasourceHandle) handles[this.currentElement];
		return this.getRandomHandle();
	}

	public String getTypeID() {
		String rcTypeID = null;

		if (this.dsHandle != null) {
			if (this.dsHandle.getDatasourceDefinition() != null) {
				if (this.dsHandle.getDatasourceDefinition().getID() != null) {
					rcTypeID = this.dsHandle.getDatasourceDefinition().getID();
				} else {
					logger.error("getTypeID - current datasourcehandles' datasourcedefinition ID is null!");
				}
			} else {
				logger.error("getTypeID - current datasourcehandles' datasource definition is null!");
			}
		} else {

			logger.error("getTypeID - current datasource handle is null!");

		}
		return rcTypeID;

		// if ( handles[this.currentElement] == null ) return null;
		// return
		// handles[this.currentElement].getDatasourceDefinition().getID();
	}

	public void close() {
		for (int i = 0; i < handles.length; i++) {
			DatasourceHandle handle = handles[i];

			handle.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		synchronized (this) {
			if (stringRepresentation == null) {
				StringBuffer buffer = new StringBuffer();

				for (int i = 0; i < handles.length; i++) {
					if (i != 0) {
						buffer.append("-");
					}
					buffer.append(handles[i]);
				}
				stringRepresentation = buffer.toString();
			}
			return stringRepresentation;
		}
	}
}
