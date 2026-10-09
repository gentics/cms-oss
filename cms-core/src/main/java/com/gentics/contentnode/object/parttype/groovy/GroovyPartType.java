package com.gentics.contentnode.object.parttype.groovy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.tools.GroovyClass;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.NodeObjectResolverContext;
import com.gentics.contentnode.resolving.ResolvableGetter;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.utils.GroovyUtils;

import groovy.lang.GroovyClassLoader;
import jakarta.ws.rs.core.Response.Status;

/**
 * Implementation of the Groovy PartType
 */
public class GroovyPartType extends TextPartType {

	private static final long serialVersionUID = 416330914123548280L;

	/**
	 * Create an instance
	 * @param value value
	 * @throws NodeException
	 */
	public GroovyPartType(Value value) throws NodeException {
		super(value);
	}

	/**
	 * Execute the groovy script
	 * @return
	 * @throws NodeException
	 */
	@ResolvableGetter
	public Object getExecute() throws NodeException {
		String code = getText();

		if (StringUtils.isBlank(code)) {
			return null;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		Tag tag = NodeObjectResolverContext.getNodeObject(Tag.class);

		if (null != tag) {
			renderType.push(tag);
		}
		try {
			renderType.createCMSResolver();
			try {
				CompilationUnit unit = GroovyUtils.getCurrentCompilationUnit();

				Value value = getValueObject();
				String constructKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getContainer, v))
						.map(cont -> MiscUtils.execOrNull(ValueContainer::getConstruct, cont)).map(Construct::getKeyword)
						.orElse("<unknown>");
				String partKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getPart, v))
						.map(Part::getKeyname).orElse("<unknown>");
				int valueId = Optional.ofNullable(value).map(Value::getId).orElse(0);
				String scriptClassName = "%s_%s_%d".formatted(constructKeyword, partKeyword, valueId);
				String scriptName = "%s.groovy".formatted(scriptClassName);

				// check whether the unit already contains the class
				GroovyClass groovyClass = GroovyUtils.findGroovyClass(unit, scriptClassName);

				// class does not exist, so add it to the compilation unit and compile
				if (groovyClass == null) {
					unit.addSource(scriptName, code);
					unit.compile(Phases.CLASS_GENERATION);

					// when compiled, add it to the class path
					groovyClass = GroovyUtils.findGroovyClass(unit, scriptClassName);
					if (groovyClass != null) {
						unit.getClassLoader().defineClass(groovyClass.getName(), groovyClass.getBytes());
					}
				}

				return GroovyUtils.call(unit.getClassLoader(), scriptClassName, script -> {
					GroovyUtils.injectCmsResolver(script);
				});
			} catch (CompilationFailedException e) {
				throw new NodeException(e);
			} finally {
				renderType.popCMSResolver();
			}
		} finally {
			if (tag != null) {
				renderType.pop(tag);
			}
		}
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.RICHTEXT;
	}

	@Override
	public void validateValue(Part part, Value value, ValueContainer container, Function<String, RestMappedException> exceptionSupplier)
			throws NodeException {
		String code = value.getValueText();
		if (StringUtils.isEmpty(code)) {
			// Nothing to validate
			return;
		}

		// the script must compile in every node, where it can be rendered (which may have different devtool packages assigned)
		Set<Node> nodes = getNodes(container);
		if (nodes.isEmpty()) {
			String errors = getCompilationErrors(code, null);
			if (errors != null) {
				throw exceptionSupplier.apply(I18NHelper.get("validation.groovy.compileerror.nonode", errors))
					.setMessageType(com.gentics.contentnode.rest.model.response.Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}
		for (Node node : nodes) {
			String errors = getCompilationErrors(code, node);
			if (errors != null) {
				throw exceptionSupplier.apply(I18NHelper.get("validation.groovy.compileerror", I18NHelper.getName(node), errors))
					.setMessageType(com.gentics.contentnode.rest.model.response.Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}
	}

	@Override
	public String getPartValidationMessageKey() {
		return "validation.groovy.part.failed";
	}

	@Override
	public String getTagPartValidationMessageKey() {
		return "validation.groovy.tag.part.failed";
	}

	/**
	 * Get the (master) nodes, in which values of the given container can be rendered
	 * @param container container (tag or construct)
	 * @return set of nodes (may be empty)
	 * @throws NodeException
	 */
	protected Set<Node> getNodes(ValueContainer container) throws NodeException {
		Collection<Node> nodes = Collections.emptyList();
		if (container instanceof Construct construct) {
			nodes = construct.getNodes();
		} else if (container instanceof Tag tag) {
			TagContainer tagContainer = tag.getContainer();
			if (tagContainer instanceof Template template) {
				nodes = template.getAssignedNodes();
			} else if (tagContainer instanceof Content content) {
				// the content is shared by all language variants and page variants
				nodes = new ArrayList<>();
				for (Page page : content.getPages()) {
					nodes.add(page.getOwningNode());
				}
			} else if (tagContainer instanceof LocalizableNodeObject<?> localizable) {
				nodes = Collections.singleton(localizable.getOwningNode());
			}
		}

		Set<Node> masterNodes = new LinkedHashSet<>();
		for (Node node : nodes) {
			if (node != null) {
				masterNodes.add(node.getMaster());
			}
		}
		return masterNodes;
	}

	/**
	 * Compile the given script for the given node (the classes of the devtool packages assigned to the node are available)
	 * @param code script
	 * @param node node (may be null)
	 * @return compilation errors or null, if the script compiles
	 * @throws NodeException
	 */
	protected String getCompilationErrors(String code, Node node) throws NodeException {
		CompilerConfiguration config = new CompilerConfiguration();
		// use a new classloader, so that the validated script is not defined in the classloader used for rendering
		GroovyClassLoader gcl = new GroovyClassLoader(RenderType.getGroovyBaseClassLoader(node), config);
		CompilationUnit unit = new CompilationUnit(config, null, gcl);
		unit.addSource("ValidatedScript.groovy", code);
		try {
			unit.compile(Phases.CLASS_GENERATION);
			return null;
		} catch (MultipleCompilationErrorsException e) {
			List<String> errors = new ArrayList<>();
			for (Message message : e.getErrorCollector().getErrors()) {
				if (message instanceof SyntaxErrorMessage syntaxError) {
					SyntaxException cause = syntaxError.getCause();
					errors.add(I18NHelper.get("validation.groovy.error", Integer.toString(cause.getLine()),
							Integer.toString(cause.getStartColumn()), cause.getOriginalMessage()));
				} else if (message instanceof ExceptionMessage exceptionMessage) {
					errors.add(exceptionMessage.getCause().getMessage());
				}
			}
			return errors.isEmpty() ? e.getMessage() : String.join("; ", errors);
		} catch (CompilationFailedException e) {
			return e.getMessage();
		}
	}

	@Override
	public String render(RenderResult result, String template) throws NodeException {
		return "";
	}
}
