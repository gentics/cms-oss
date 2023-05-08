package com.gentics.lib.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Stack;

import com.gentics.lib.log.NodeLogger;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 19.11.2003
 */
public abstract class DefaultConnectionManager implements Connector {
	private int m_counter = 0;

	private int m_maxConnection;

	private int m_connectionWaitTimeout = 0;

	private PoolConnection[] m_connections = null;

	private Stack m_freeConnections;

	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	protected DefaultConnectionManager() {
		this(20);
	}

	public DefaultConnectionManager(int maxConnections) {
		m_maxConnection = maxConnections;
		m_connections = new PoolConnection[maxConnections];
		m_freeConnections = new Stack();
	}

	private int getFreeConnection() throws SQLException {
		int freeId = -1;

		synchronized (m_freeConnections) {
			if (logger.isDebugEnabled()) {
				logger.debug("getting free connection");
			}
			if (m_freeConnections.isEmpty()) {
				if (m_counter >= m_maxConnection) {
					if (logger.isDebugEnabled()) {
						logger.debug("no free connections in the moment, maximum of " + m_maxConnection + " reached");
					}
					return -1;
				} else {
					freeId = m_counter++;
					if (logger.isDebugEnabled()) {
						logger.debug("new free connection created, currently " + m_counter + " in pool");
					}
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug(m_freeConnections.size() + " free connections (total: " + m_counter + ") available");
				}
				int retVal = ((Integer) m_freeConnections.pop()).intValue();

				if (logger.isDebugEnabled()) {
					logger.debug("getting connection #" + retVal);
				}
				return retVal;
			}
		}
		m_connections[freeId] = new PoolConnection(freeId, connect());
		if (logger.isDebugEnabled()) {
			logger.debug("getting new connection #" + freeId);
		}
		return freeId;
	}

	private void freeConnection(int id) {
		if (logger.isDebugEnabled()) {
			logger.debug("freeing connection #" + id);
		}
		synchronized (m_freeConnections) {
			m_freeConnections.push(new Integer(id));
			if (logger.isDebugEnabled()) {
				logger.debug(m_freeConnections.size() + " free connections (total: " + m_counter + ") available");
			}
			m_freeConnections.notifyAll();
		}
	}

	private void waitForFreeConnection() {
		synchronized (m_freeConnections) {
			try {
				m_freeConnections.wait(m_connectionWaitTimeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void releaseConnection(PoolConnection c) throws SQLException {
		freeConnection(c.getID());
	}

	private boolean testConnection(int id) {
		try {
			if (m_connections[id] == null) {
				return false;
			}
			if (m_connections[id].getConnection().isClosed()) {
				return false;
			}
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	public PoolConnection getConnection() throws SQLException {
		int freeConId;

		do {
			freeConId = getFreeConnection();
			if (freeConId == -1) {
				waitForFreeConnection();
			}
		} while (freeConId == -1);

		if (!testConnection(freeConId)) {
			try {
				if (m_connections[freeConId] != null) {
					Connection c = m_connections[freeConId].getConnection();

					if (c != null) {
						c.close();
					}
				}
			} catch (SQLException e) {}
			try {
				m_connections[freeConId] = new PoolConnection(freeConId, connect());
			} catch (SQLException e) {
				freeConnection(freeConId);
				throw e;
			}
		}
		return m_connections[freeConId];
	}

	/**
	 * @throws SQLException
	 */
	public synchronized void close() throws SQLException {
		// TODO wait for all connections to be released before closing them
		for (int i = 0; i < m_counter; i++) {
			try {
				freeConnection(i);
				Connection c = null;

				if (m_connections[i] != null) {
					c = m_connections[i].getConnection();
				}
				if (c != null) {
					c.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract Connection connect() throws SQLException;
}
