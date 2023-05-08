/*
 * @author herbert
 * @date 05.04.2007
 * @version $Id: PageRenderResult.java,v 1.1 2007-04-10 14:53:56 herbert Exp $
 */
package com.gentics.contentnode.publish;

import java.util.List;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderResultWrapper;

public class PageRenderResult extends RenderResultWrapper {

	private boolean needsRepublish;
	private boolean allowRepublish;

	public PageRenderResult(RenderResult wrapped) {
		super(wrapped);
		this.messages = (List) wrapped.getMessages();
	}
    
	public void setNeedsRepublish(boolean needsRepublish) {
		this.needsRepublish = needsRepublish;
	}
    
	public boolean needsRepublish() {
		return this.needsRepublish;
	}

	public void setAllowRepublish(boolean allowRepublish) {
		this.allowRepublish = allowRepublish;
	}

	/**
	 * Returns true if the current rendering is in a publish run which allows republishing.
	 */
	public boolean allowRepublishing() {
		return this.allowRepublish;
	}
}
