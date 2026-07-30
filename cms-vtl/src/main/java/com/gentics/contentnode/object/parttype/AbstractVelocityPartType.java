/*
 * @author alexander
 * @date 11.04.2007
 * @version $Id: AbstractVelocityPartType.java,v 1.6 2010-11-16 12:46:24 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import com.gentics.api.contentnode.parttype.AbstractExtensiblePartType;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.velocity.SerializableVelocityTemplateWrapper;

/**
 * Abstract navigation parttype providing common methods for all navigation
 * parttypes and velocity parttype. Class could be separated into two classes,
 * one for all navigation parttypes and one only for the velocity partttype.
 */
public abstract class AbstractVelocityPartType extends AbstractExtensiblePartType {
	/**
	 * Sequence that is used as template name postfix. See {@link #getTemplateFromString(String)} for details.
	 */
	protected static final AtomicLong templateNamePostfixSequence = new AtomicLong();

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(AbstractVelocityPartType.class);

	/**
	 * Put string into string resource repository, read template from velocity,
	 * delete from string resource repository and return parsed template.
	 * Additionally, add velocity macros.
	 * 
	 * The name of the template will be made unique by adding a new value of {@link #templateNamePostfixSequence}.
	 * Uniqueness of templates is important so that the velocimacros of the template can safely be removed from the velocimacro factory, 
	 * When the {@link SerializableVelocityTemplateWrapper} instance of the template is garbage collected.
	 * @param template The template as string.
	 * @return The template object constructed from the string.
	 */
	protected Template getTemplateFromString(String template) throws Exception {
		// use the current thread's name as part of the template name, to
		// separate the velocimacros (template name is the "namespace" of
		// locally defined velocimacros)
		// Additionally, we add the tag id to the template name, so that different velocity tags do not interfer
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		Tag tag = renderType.getTopmostTag();

		StringBuilder templateNameBuilder = new StringBuilder();

		if (tag == null) {
			templateNameBuilder.append("Internal Template");
		} else {
			templateNameBuilder.append("Tag Template ").append(tag.getName());
		}
		templateNameBuilder.append("-").append(templateNamePostfixSequence.incrementAndGet());
		String templateName = templateNameBuilder.toString();
		StringResourceRepository srr = StringResourceLoader.getRepository();

		srr.putStringResource(templateName, template);
		Template tmp = Velocity.getTemplate(templateName);

		srr.removeStringResource(templateName);

		return tmp;
	}

	/**
	 * Parse the template given by the parameters.
	 * <ol>
	 *   <li>Check whether the template has been cached (cache key is the md5 sum of the template source)</li>
	 *   <li>Call {@link #getTemplateFromString(String)} to parse the template source into a Template</li>
	 *   <li>Wrap the Template object into a {@link SerializableVelocityTemplateWrapper} instance that is put in the cache and returned from this object</li>
	 * </ol>
	 * @param fullTemplate template source
	 * @return A Template object.
	 * @throws NodeException
	 */
	protected SerializableVelocityTemplateWrapper parseTemplate(String fullTemplate) throws NodeException {
		// the md5Sum of the template will be used as cache key and part of its name
		String md5Sum = StringUtils.md5(fullTemplate);

		// try to get parsed templates from portal cache
		Object tmpTemplates = getCachedObject(md5Sum);

		if (tmpTemplates instanceof SerializableVelocityTemplateWrapper) {
			logger.debug("Template found in cache.");
			SerializableVelocityTemplateWrapper wrapper = (SerializableVelocityTemplateWrapper)tmpTemplates;
			Template template = wrapper.getTemplate();

			// template might be null as it is stored as transient in the
			// wrapper
			// if it is null, treat as cache miss
			if (template != null) {
				return wrapper;
			} else {
				logger.warn("Template cached, but null. Do not use disk-based cache for templates!");
			}
		}

		Template tmp = null;

		try {
			tmp = getTemplateFromString(fullTemplate);
		} catch (Exception e) {
			logger.error("StringResourceLoader didn't return a valid template. " + e.getMessage());
			throw new NodeException("StringResourceLoader didn't return a valid template. " + e.getMessage(), e);
		}
        
		SerializableVelocityTemplateWrapper wrapper = new SerializableVelocityTemplateWrapper(tmp);

		putObjectIntoCache(md5Sum, wrapper);

		return wrapper;
	}

	/**
	 * Render the template wrapped by the wrapper and make sure that the wrapper is not garbage collected while the template is still rendered
	 * @param wrapper wrapper
	 * @param context velocity context
	 * @param writer write to receive the rendered template
	 * @throws ResourceNotFoundException
	 * @throws ParseErrorException
	 * @throws MethodInvocationException
	 * @throws IOException
	 */
	protected void mergeTemplate(SerializableVelocityTemplateWrapper wrapper, Context context, Writer writer)
			throws ResourceNotFoundException, ParseErrorException, MethodInvocationException, IOException {
		wrapper.getTemplate().merge(context, writer);

		// special hack to avoid the instance of SerializableVelocityTemplateWrapper to
		// be garbage collected while the template is still rendered.
		// If the SerializableVelocityTemplateWrapper is garbage collected, the finalize() method would dump the VM namespace of the template
		// to remove the local inline macros from the Velocity store (this is done to avoid memory leaks)
		// if this happens while the template is rendered, the macros would not be resolved any more.
		// accessing the config object after the call to getTemplate().merge() above will keep the reference, so the SerializableVelocityTemplateWrapper
		// will not be eligible for garbage collection to an inapropriate moment.
		wrapper.toString();
	}
}
