/*
 * DatasourceSTRUCT.java
 *
 * Created on 07. August 2004, 18:42
 */

package com.gentics.lib.datasource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dietmar
 */
public class DatasourceHandleSTRUCT {
	public String ID = null;

	public String typeID = null;

	public DatasourceDefinitionSTRUCT typeRef = null;

	public Map<String, String> parameterMap = null;

	/** Creates a new instance of DatasourceSTRUCT */
	public DatasourceHandleSTRUCT() {
		parameterMap = new HashMap<String, String>();
	}
}
