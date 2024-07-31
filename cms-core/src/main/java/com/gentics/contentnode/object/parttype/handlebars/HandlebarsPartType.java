package com.gentics.contentnode.object.parttype.handlebars;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.Synchronizer.Status;
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
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.resolving.ResolvableMapWrapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateLoader;

/**
 * PartType implementation for using {@link Handlebars} template engine
 */
public class HandlebarsPartType extends TextPartType {

	private static final long serialVersionUID = 6043235322865960951L;

	// TODO maybe use a size limited cache
	private final static TemplateCache CACHE = new ConcurrentMapTemplateCache().setReload(true);

	protected String templateName;

	public HandlebarsPartType(Value value) throws NodeException {
		super(value);
		String constructKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getContainer, v))
				.map(cont -> MiscUtils.execOrNull(ValueContainer::getConstruct, cont)).map(Construct::getKeyword)
				.orElse("<unknown>");
		String partKeyword = Optional.ofNullable(value).map(v -> MiscUtils.execOrNull(Value::getPart, v))
				.map(Part::getKeyname).orElse("<unknown>");

		templateName = String.format("%s.%s", constructKeyword, partKeyword);
	}

	@Override
	public String render(RenderResult result, String template) throws NodeException {
		// TODO: should we get the handlebars engine from the transaction, so that it is not generated (and populated with helpers) on each invocation?
		var handlebars = new Handlebars().infiniteLoops(true); // .with(CACHE);
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		List<TemplateLoader> templateLoaders = new ArrayList<>();

		try {
			renderType.createCMSResolver();
			CMSResolver cmsResolver = renderType.getCMSResolver();
			Node node = ObjectTransformer.get(Node.class, cmsResolver.get("node"));

			if (Synchronizer.getStatus() == Status.UP) {
				// TODO should we cache the registered helpers in the packages?
				for (String packageName : Synchronizer.getPackages(node)) {
					File packageDirectory = new File(ConfigurationValue.PACKAGES_PATH.get(), packageName);
					File handlebarsDirectory = new File(packageDirectory, "handlebars");
					File helpersDirectory = new File(handlebarsDirectory, "helpers");
					File partialsDirectory = new File(handlebarsDirectory, "partials");

					if (helpersDirectory.isDirectory()) {
						StringBuilder registerHelpers = new StringBuilder();

						for (File helperFile : helpersDirectory.listFiles((dir, name) -> StringUtils.endsWith(name, ".js"))) {
							String helperNameShort = StringUtils.removeEnd(helperFile.getName(), ".js");
							String helperName = String.format("%s.%s", packageName, helperNameShort);
							String helperFileContents = FileUtils.readFileToString(helperFile, Charset.forName("UTF-8"));
							String register = String.format("Handlebars.registerHelper('%s', %s)", helperName, helperFileContents);

							registerHelpers.append(register).append("\n");
						}

						if (!registerHelpers.isEmpty()) {
							handlebars.registerHelpers(packageName, registerHelpers.toString());
						}
					}

					if (partialsDirectory.isDirectory()) {
						templateLoaders.add(new PackageTemplateLoader(packageName, partialsDirectory));
					}
				}
			}

			if (!templateLoaders.isEmpty()) {
				handlebars.with(templateLoaders.toArray(new TemplateLoader[templateLoaders.size()]));
			}

			handlebars.registerHelpers(ConditionalHelpers.class);
			handlebars.registerHelpers(StringHelpers.class);
			handlebars.registerHelpers(HelperSource.class);

			List<String> list = List.of("one", "two", "three");
			String[] array = list.toArray(new String[list.size()]);

			StringTemplateSource source = new StringTemplateSource(templateName, getText());
			var handlebarsTemplate = handlebars.compile(source);
			Context context = Context.newBuilder(null)
					.resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
					.combine("cms", new ResolvableMapWrapper(cmsResolver))
					.combine("stringlist", list)
					.combine("stringarray", array)
					.build();
			return handlebarsTemplate.apply(context);
		} catch (Throwable e) {
			throw new NodeException(e);
		} finally {
			renderType.popCMSResolver();
		}
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.HANDLEBARS;
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
