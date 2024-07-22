package com.gentics.contentnode.object.parttype;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.base.MapResolver;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateLoader;

public class HandlebarsPartType extends TextPartType {

	private static final long serialVersionUID = 6043235322865960951L;

	private final static ResolvableValueResolver INSTANCE = new ResolvableValueResolver();

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
		var handlebars = new Handlebars(); // .with(CACHE);
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		List<TemplateLoader> templateLoaders = new ArrayList<>();

		try {
			renderType.createCMSResolver();
			CMSResolver cmsResolver = renderType.getCMSResolver();

			Node node = cmsResolver.getNode();
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

			if (!templateLoaders.isEmpty()) {
				handlebars.with(templateLoaders.toArray(new TemplateLoader[templateLoaders.size()]));
			}

			handlebars.registerHelpers(ConditionalHelpers.class);
			handlebars.registerHelpers(StringHelpers.class);

//			Map<String, Object> map = new HashMap<>();
//			Map<String, Object> submap = new HashMap<>();
//			submap.put("one", "eins drunter");
//			submap.put("two", "zwei drunter");
//			submap.put("three", "drei drunter");
//			map.put("one", "eins");
//			map.put("two", "zwei");
//			map.put("three", "drei");
//			map.put("sub", submap);

			StringTemplateSource source = new StringTemplateSource(templateName, getText());
			var handlebarsTemplate = handlebars.compile(source);
			Context context = Context.newBuilder(null)
					.resolver(INSTANCE, MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
					.combine("cms", cmsResolver)
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

	public static class ResolvableValueResolver implements ValueResolver {

		@Override
		public Object resolve(Object context, String name) {
			if (context instanceof Resolvable) {
				Object resolved = ((Resolvable) context).get(name);
				if (resolved != null) {
					return resolved;
				}
			}
			return ValueResolver.UNRESOLVED;
		}

		@Override
		public Object resolve(Object context) {
			return ValueResolver.UNRESOLVED;
		}

		@Override
		public Set<Entry<String, Object>> propertySet(Object context) {
			return Collections.emptySet();
		}
	}

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

	public static class ResolvableMap implements Map<String, Object> {
		protected Resolvable wrapped;

		public ResolvableMap(Resolvable wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public Object get(Object key) {
			Object value = wrapped.get(ObjectTransformer.getString(key, ""));
			if (value instanceof Resolvable) {
				value = new ResolvableMap((Resolvable) value);
			}
			return value;
		}

		@Override
		public Object put(String key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> keySet() {
			return Collections.emptySet();
		}

		@Override
		public Collection<Object> values() {
			return Collections.emptySet();
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return Collections.emptySet();
		}
	}
}
