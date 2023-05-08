/*
 * DatasourceDefinition.java
 *
 * Created on 07. August 2004, 18:33
 */

package com.gentics.lib.datasource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dietmar
 */
public class DatasourceDefinitionSTRUCT {

	public Map<String, String> definitionParameter = null;

	public String typeid = null;

	/** Creates a new instance of DatasourceDefinition */
	public DatasourceDefinitionSTRUCT() {
		definitionParameter = new HashMap<String, String>();
	}

}
