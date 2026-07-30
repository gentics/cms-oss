package com.gentics.contentnode.object.parttype;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.render.RenderType.ParameterScope;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.velocity.SerializableVelocityTemplateWrapper;

/**
 * PartType 33 - Velocity
 */
public class VelocityPartType extends AbstractVelocityPartType implements TransformablePartType, Resolvable {
    
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(VelocityPartType.class);

	/**
	 * Name of input parameter for template
	 */
	public static final String INPUT_TEMPLATE = "template";

	/**
	 * Name of input parameter for macros
	 */
	public static final String INPUT_MACROS = "macros";

	/**
	 * Name of the rendertype parameter, which is set to "true" while the part "template" is rendered.
	 * This will make sure that the special VTL characters # and $ will be escaped when rendered from an inline editable part, which is included via &lt;node&gt;-Notation into the template part
	 */
	public static final String SAFE_INLINE_RENDERING = "gtx.safe_inline_rendering";

	/**
	 * Render the navigation. Gets all needed input parameters and starts
	 * rendering at startpage.
	 * @throws NodeException
	 */
	public String render() throws NodeException {
		// create Velocity context, populate
		VelocityContext context = new VelocityContext(createContext(true));
		return render(context);
	}

	/**
	 * Render with the given context
	 * @param context context
	 * @return rendered template
	 * @throws NodeException
	 */
	protected String render(VelocityContext context) throws NodeException {
		ConfigObject config = getInitParameters();

		context.put("ctx", context);

		StringWriter outwriter = new StringWriter();
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		int editMode = renderType.getEditMode();
		boolean editModeChanged = false;

		try {
			// when edit mode is edit or realedit, switch to preview mode
			if (editMode == RenderType.EM_ALOHA) {
				editModeChanged = true;
				renderType.setEditMode(RenderType.EM_ALOHA_READONLY);
			}
			if (editModeChanged) {
				renderType.setParameter(CMSResolver.ModeResolver.PARAM_OVERWRITE_EDITMODE, new Integer(editMode));
			}

			if (config.wrapper != null) {
				mergeTemplate(config.wrapper, context, outwriter);
			}

		} catch (ParseErrorException pee) {
			logger.error("ParseErrorException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + pee.getMessage());
			throw new NodeException("ParseErrorException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + pee.getMessage(), pee);
		} catch (ResourceNotFoundException rnfe) {
			logger.error("ResourceNotFoundException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + rnfe.getMessage());
			throw new NodeException("ResourceNotFoundException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + rnfe.getMessage(), rnfe);
		} catch (MethodInvocationException mie) {
			logger.error("MethodInvocationException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + mie.getMessage());
			throw new NodeException("MethodInvocationException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + mie.getMessage(), mie);
		} catch (IOException ioe) {
			logger.error("IOException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + ioe.getMessage());
			throw new NodeException("IOException while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + ioe.getMessage(), ioe);
		} catch (Throwable e) {
			String message = e instanceof StackOverflowError ? "Stack overflow" : e.getMessage();

			logger.error("Exception while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + message);
			throw new NodeException("Exception while merging velocity template for " + renderType.getStack().getUIReadableStack() + ". " + message, e);
		} finally {
			// when edit mode was changed, change it back
			if (editModeChanged) {
				renderType.setEditMode(editMode);
				if (editModeChanged) {
					renderType.setParameter(CMSResolver.ModeResolver.PARAM_OVERWRITE_EDITMODE, null);
				}
			}
		}

		return outwriter.toString();
	}

	/**
	 * Read all needed init parameters using the resolve() method. Populates
	 * instance variables.
	 * @throws NodeException
	 */
	protected ConfigObject getInitParameters() throws NodeException {
		ConfigObject config = new ConfigObject();
		// resolve the template
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		Tag topmostTag = renderType.getStack().getTopmostTag();

		if (topmostTag == null) {
			String message = "Couldn't get topmost tag from render stack";

			logger.error(message);
			throw new NodeException(message);
		}

		// We use the PropertyResolver because it doesn't check if the tag is enabled,
		// as it would cause a problem when the template part can't be resolved.
		PropertyResolver resolver = new PropertyResolver(topmostTag);
		try (ParameterScope p = renderType.withParameter(SAFE_INLINE_RENDERING, true)) {
			config.template = renderObjectWithoutRecursionCheck(resolver.resolve("parts." + INPUT_TEMPLATE), null);
		}
		// and the macros
		config.macros = renderObjectWithoutRecursionCheck(topmostTag.get(INPUT_MACROS), "");

		if (config.template == null) {
			logger.error("No template found.");
			throw new NodeException("No template found.");
		}

		// parse template
		config.wrapper = parseTemplate(config.template, config.macros);

		return config;
	}

	/**
	 * Helper method to render Values without recursion test
	 * @param object object to render
	 * @param defaultValue default value
	 * @return rendered object
	 * @throws NodeException
	 */
	protected String renderObjectWithoutRecursionCheck(Object object, String defaultValue) throws NodeException {
		// special behaviour for Values (disable the recursion test, since this
		// would prevent nested velocity tags with static templates)
		if (object instanceof Value) {
			// when the template is a value, we render it without recursion test
			return ((Value) object).render(TransactionManager.getCurrentTransaction().getRenderResult(), null, false, true);
		} else {
			// all other objects are normally rendered
			return ObjectTransformer.getString(object, defaultValue);
		}
	}

	/**
	 * Parse the templates given by the parameters.
	 * <ol>
	 *   <li>Concatenate the input strings to generate the full template source (macros always come first)</li>
	 *   <li>Check whether the template has been cached (cache key is the md5 sum of the template source)</li>
	 *   <li>Let Velocity parse the template source into a Template object. Use the next value of {@link #templateNamePostfixSequence} in the template name, so that no two templates have the same template name</li>
	 *   <li>Wrap the Template object into a {@link SerializableVelocityTemplateWrapper} instance that is put in the cache and returned from this object</li>
	 * </ol>
	 * @param inputTemplate The Velocity template of the navigation
	 * @param macros The Velocity macros to include
	 * @return A Template object.
	 * @throws NodeException
	 */
	protected SerializableVelocityTemplateWrapper parseTemplate(String inputTemplate, String macros) throws NodeException {
		String fullTemplate = inputTemplate;

		// when additional macros are given, we combine the macros with the template
		if (!StringUtils.isEmpty(macros)) {
			StringBuffer buffer = new StringBuffer(inputTemplate.length() + macros.length());

			// important: macros first!
			buffer.append(macros);
			buffer.append(inputTemplate);
			fullTemplate = buffer.toString();
		}

		return parseTemplate(fullTemplate);
	}

	@Override
	public Type getPropertyType() {
		return Type.VELOCITY;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}

	@Override
	public Property toProperty() throws NodeException {
		return null;
	}

	/**
	 * Wrapper for the rendered velocity template
	 */
	public class ConfigObject {
		/**
		 * Template to use to render navigation.
		 */
		protected String template;

		/**
		 * Velocity macros used to render
		 */
		protected String macros;

		/**
		 * Store all templates read from XML input.
		 */
		protected SerializableVelocityTemplateWrapper wrapper;

		/**
		 * Create an empty instance
		 */
		public ConfigObject() {
			this.template = null;
			this.macros = "";
			this.wrapper = null;
		}
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public Object get(String key) {
		// create Velocity context, populate
		try {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

			try {
				renderType.createCMSResolver();

				VelocityContext context = new VelocityContext(createContext(true));
				render(context);
				Object value = context.get(key);
				if (value instanceof RenderableResolvable) {
					value = ((RenderableResolvable) value).getWrappedObject();
				}
				return value;
			} finally {
				renderType.popCMSResolver();
			}
		} catch (NodeException e) {
			logger.error(String.format("Error while getting %s", key), e);
			return null;
		}
	}

	@Override
	public boolean canResolve() {
		return true;
	}
}
