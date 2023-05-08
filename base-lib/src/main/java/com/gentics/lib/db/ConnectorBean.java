/*
 * ConnectorBean.java
 *
 * Created on 22. Jaenner 2004, 15:32
 */

package com.gentics.lib.db;

/**
 * @author andreas
 */
public class ConnectorBean {

	private Connector connector;

	/** Creates a new instance of ConnectorBean */
	public ConnectorBean() {}

	public Connector getConnector() {
		return this.connector;
	}

	public void setConnector(Connector con) {
		if (con != null) {
			this.connector = con;
		}
	}

}
