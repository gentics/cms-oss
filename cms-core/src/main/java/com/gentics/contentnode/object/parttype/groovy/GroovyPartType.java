package com.gentics.contentnode.object.parttype.groovy;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.tools.GroovyClass;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.CMSResolver;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.ResolvableGetter;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.utils.GroovyUtils;

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

		renderType.createCMSResolver();
		try {
			CMSResolver cmsResolver = renderType.getCMSResolver();
			Node node = ObjectTransformer.get(Node.class, cmsResolver.get("node")).getMaster();
			CompilationUnit unit = renderType.getCompilationUnit(node);

			Value value = getValueObject();
			String constructKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getContainer, v))
					.map(cont -> MiscUtils.execOrNull(ValueContainer::getConstruct, cont)).map(Construct::getKeyword)
					.orElse("<unknown>");
			String partKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getPart, v))
					.map(Part::getKeyname).orElse("<unknown>");
			int valueId = Optional.ofNullable(value).map(Value::getId).orElse(0);
			String scriptClassName = "%s_%s_%d".formatted(constructKeyword, partKeyword, valueId);
			String scriptName = "%s.groovy".formatted(scriptClassName);

			unit.addSource(scriptName, code);
			unit.compile(Phases.CLASS_GENERATION);

			for (GroovyClass groovyClass : unit.getClasses()) {
				if (Strings.CI.equals(groovyClass.getName(), scriptClassName)) {
					unit.getClassLoader().defineClass(groovyClass.getName(), groovyClass.getBytes());
				}
			}

			return GroovyUtils.call(unit.getClassLoader(), scriptClassName, script -> {
				script.setProperty("cms", cmsResolver);
			});
		} catch (CompilationFailedException e) {
			throw new NodeException(e);
		} finally {
			renderType.popCMSResolver();
		}
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.RICHTEXT;
	}

}
