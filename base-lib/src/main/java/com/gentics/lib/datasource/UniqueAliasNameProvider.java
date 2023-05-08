/*
 * @author herbert
 * @date 08.01.2007
 * @version $Id: UniqueAliasNameProvider.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource;

/**
 * A simple class which provides unique alias names useable 
 * when joining multiple tables and needing unique names for them.
 * Used by the {@link com.gentics.lib.datasource.CNDatasource}
 * 
 * @author herbert
 */
public class UniqueAliasNameProvider {
	protected int uniqueCounter = 0;
	private String basename;
    
	/**
	 * creates a new UniqueAliasNameProvider object with the given
	 * basename - aliases will look like &lt;basename&gt;&lt;unique 
	 * number&gt; (e.g. cm1)
	 * @param basename the basename (e.g. cm, ca)
	 */
	public UniqueAliasNameProvider(String basename) {
		this.basename = basename;
	}
    
	/**
	 * Returns a unique alias for this tablename.
	 * (Since one UniqueAliasNameProvider should not be shared between
	 * threads anyway this method is not synchronized.)
	 * @return a new unique alias name for the given table.
	 */
	public String getUniqueAlias() {
		return this.basename + (++uniqueCounter);
	}

	/**
	 * Get the current unique counter
	 * @return unique counter
	 */
	public int getUniqueCounter() {
		return uniqueCounter;
	}
}
