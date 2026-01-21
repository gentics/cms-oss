package com.gentics.contentnode.object.parttype.handlebars;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

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
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.resolving.ResolvableMapWrapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.StringTemplateSource;

/**
 * PartType implementation for using {@link Handlebars} template engine
 */
public class HandlebarsPartType extends TextPartType {
	private static final long serialVersionUID = 6043235322865960951L;

	public HandlebarsPartType(Value value) throws NodeException {
		super(value);
	}

	@Override
	public String render(RenderResult result, String template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		renderType.createCMSResolver();
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

			Value value = getValueObject();

			String constructKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getContainer, v))
					.map(cont -> MiscUtils.execOrNull(ValueContainer::getConstruct, cont)).map(Construct::getKeyword)
					.orElse("<unknown>");
			String partKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getPart, v))
					.map(Part::getKeyname).orElse("<unknown>");
			int valueId = Optional.ofNullable(value).map(Value::getId).orElse(0);
			String templateName = String.format("%s/%s/%d", constructKeyword, partKeyword, valueId);

			CMSResolver cmsResolver = renderType.getCMSResolver();
			Node node = ObjectTransformer.get(Node.class, cmsResolver.get("node")).getMaster();
			Handlebars handlebars = renderType.getHandlebars(node);

			StringTemplateSource source = new StringTemplateSource(templateName, getText());
			Template handlebarsTemplate = handlebars.compile(source);
			Context context = Context.newBuilder(null)
					.resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
					.combine("cms", new ResolvableMapWrapper(cmsResolver))
					.build();
			return handlebarsTemplate.apply(context);
		} catch (NodeException e) {
			throw e;
		} catch (Throwable e) {
			throw new NodeException(e);
		} finally {
			// when edit mode was changed, change it back
			if (editModeChanged) {
				renderType.setEditMode(editMode);
				if (editModeChanged) {
					renderType.setParameter(CMSResolver.ModeResolver.PARAM_OVERWRITE_EDITMODE, null);
				}
			}
			renderType.popCMSResolver();
		}
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.RICHTEXT;
	}

	/**
	 * Implementation of {@link FileTemplateLoader} that loads templates (partials) from a package
	 */
	public static class PackageTemplateLoader extends FileTemplateLoader {
		protected String packagePrefix;

		public PackageTemplateLoader(String packageName, File basedir) {
			super(basedir);
			this.packagePrefix = String.format("%s.", packageName);
		}

		@Override
		public String resolve(String uri) {
			if (StringUtils.startsWith(uri, packagePrefix)) {
				return super.resolve(StringUtils.removeStart(uri, packagePrefix));
			} else {
				return null;
			}
		}

		@Override
		protected URL getResource(String location) throws IOException {
			if (StringUtils.isBlank(location)) {
				return null;
			}
			return super.getResource(location);
		}
	}
}
