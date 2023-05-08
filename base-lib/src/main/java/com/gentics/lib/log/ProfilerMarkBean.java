/*
 * @author herbert
 * @date 13.04.2007
 * @version $Id: ProfilerMarkBean.java,v 1.1 2007-04-13 11:17:17 herbert Exp $
 */
package com.gentics.lib.log;

import java.io.Serializable;

public class ProfilerMarkBean implements Serializable {
	private static final long serialVersionUID = -6702654062895975425L;
    
	private String element;
	private String objectKey;
	private long time;
	private boolean isBegin;
    
	public ProfilerMarkBean(String element, String objectKey, long time, boolean isBegin) {
		this.element = element;
		this.objectKey = objectKey;
		this.time = time;
		this.isBegin = isBegin;
	}

	public String getElement() {
		return element;
	}

	public boolean isBegin() {
		return isBegin;
	}

	public String getObjectKey() {
		return objectKey;
	}

	public long getTime() {
		return time;
	}
    
}
