/*
 * @author alexander
 * @date 04.06.2007
 * @version $Id: SerializableVelocityTemplateWrapper.java,v 1.2 2008-05-26 15:05:57 norbert Exp $
 */
package com.gentics.lib.render.velocity;

import java.io.Serializable;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeSingleton;

/**
 * Wrap velocity template in serializable object, since it is not serializable
 * (needed for JCS caching). This does not make the template itself
 * serializable!
 * 
 * When an instance of this class is garbage collected, all references to the Template instance
 * are lost, so the local namespace of the template can be cleared.
 */
public class SerializableVelocityTemplateWrapper implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -307344517621305235L;

	/**
	 * The velocity template to wrap. Marked transient to treat as cache miss if
	 * cache tried to serialize it.
	 */
	private transient Template template;

	/**
	 * Create a new wrapper around the velocity template.
	 * @param template The velocity template to wrap.
	 */
	public SerializableVelocityTemplateWrapper(Template template) {
		this.template = template;
	}

	/**
	 * Get the wrapped velocity template.
	 * @return The wrapped velocity template.
	 */
	public Template getTemplate() {
		return template;
	}

	/**
	 * Set the wrapped velocity template.
	 * @param template The velocity template to wrap.
	 */
	public void setTemplate(Template template) {
		this.template = template;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		// dump the velocimacro namespace
		RuntimeSingleton.dumpVMNamespace(template.getName());
	}
}
