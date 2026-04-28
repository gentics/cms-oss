package com.gentics.contentnode.forms;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.FormDataUtils;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.factory.AnyChannelTrx;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.cr.MeshURLRenderer;
import com.gentics.contentnode.publish.mesh.MeshPublisher.UrlEscapeUtils;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUrlFactory;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;
import com.gentics.contentnode.rest.model.formtype.BaseSetting;
import com.gentics.contentnode.rest.model.formtype.ElementConfig;
import com.gentics.contentnode.rest.model.formtype.PublishAsType;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.rest.model.formtype.TranslationSetting;
import com.gentics.lib.log.NodeLogger;

/**
 * Instances of this class will transform the data of a given form
 */
public class FormTransformer {
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(FormTransformer.class);

	/**
	 * Dummy ReferenceSetting for transforming the "successUrl" node/page reference
	 */
	protected final static ReferenceSetting SUCCESS_PAGE_REFERENCE = new ReferenceSetting("successUrl",
			Collections.emptyMap(), Collections.singleton(ReferenceType.page), PublishAsType.url);

	/**
	 * Dummy ReferenceSetting for transforming the "adminEmailSource" node/page reference
	 */
	protected final static ReferenceSetting ADMIN_MAIL_SOURCE_REFERENCE = new ReferenceSetting("adminEmailSource",
			Collections.emptyMap(), Collections.singleton(ReferenceType.page), PublishAsType.content);

	/**
	 * Transformation target, which defines how the references should be transformed
	 */
	protected TransformationTarget transformationTarget = TransformationTarget.publish;

	/**
	 * Callback, which will be called when unknown references are handled
	 */
	protected Operator unknownReferenceCallback = () -> {};

	/**
	 * Set the transformation target
	 * @param transformationTarget target
	 * @return fluent API
	 */
	public FormTransformer withTransformationTarget(TransformationTarget transformationTarget) {
		this.transformationTarget = transformationTarget;
		return this;
	}

	/**
	 * Set the callback for unknown references
	 * @param unknownReferenceCallback callback
	 * @return fluent API
	 */
	public FormTransformer withUnknownReferenceCallback(Operator unknownReferenceCallback) {
		this.unknownReferenceCallback = unknownReferenceCallback;
		return this;
	}

	/**
	 * Transform the given form to the JSON object
	 * @param form form
	 * @return JSON object
	 * @throws NodeException
	 */
	public ObjectNode transform(Form form) throws NodeException {
		return transform(form, form.getData().deepCopy());
	}

	/**
	 * Transform the given data for the form to the JSON object
	 * @param form form
	 * @param data form data to transform
	 * @return JSON object
	 * @throws NodeException
	 */
	public ObjectNode transform(Form form, ObjectNode data) throws NodeException {
		String name = I18NHelper.getName(form);
		FormTypeConfiguration typeConfig = Optional.ofNullable(FormConfigProvider.getConfig()).map(config -> config.get(form.getFormType())).orElseThrow(
				() -> new NodeException("Error while transforming form %s: Could not find configuration for form type %s"
						.formatted(name, form.getFormType())));

		transformationTarget.transform(form, data);

		List<String> languages = form.getLanguages();

		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		boolean handleDependencies = renderType != null ? renderType.doHandleDependencies() : false;
		boolean storeDependencies = renderType != null ? renderType.isStoreDependencies() : false;

		try (PropertyTrx linkWayTrx = new PropertyTrx("contentnode.linkway", "host");
				PropertyTrx fileLinkWayTrx = new PropertyTrx("contentnode.linkway_file", "host");
				PublishCacheTrx pcTrx = new PublishCacheTrx(false);
				RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, form, handleDependencies, false, false)) {
			// for the StaticUrlFactory, forbid auto detection of the linkway
			RenderUrlFactory renderUrlFactory = rTrx.get().getRenderUrlFactory();
			if (renderUrlFactory instanceof StaticUrlFactory) {
				((StaticUrlFactory) renderUrlFactory).setAllowAutoDetection(false);
			}

			FormDataUtils.handleElements(typeConfig, data, (element, elementType, elementConfig) -> {
				transformReferences(typeConfig, elementConfig, element, languages);
				handleLinks(typeConfig, elementConfig, element, languages);
			}, null);

			transformPageReference(data, "successPageId", "successNodeId", p -> {
				transformationTarget.transform(SUCCESS_PAGE_REFERENCE, data, languages,
						p != null ? p.getOwningNode() : null, p,
						Optional.of(Pair.of("successPageId", "successNodeId")));
			});
			transformPageReference(data, "adminEmailPageId", "adminEmailNodeId", p -> {
				transformationTarget.transform(ADMIN_MAIL_SOURCE_REFERENCE, data, languages,
						p != null ? p.getOwningNode() : null, p,
						Optional.of(Pair.of("adminEmailPageId", "adminEmailNodeId")));
			});

			// take over the collected dependencies
			if (handleDependencies && storeDependencies) {
				for (Dependency dep : rTrx.get().getDependencies()) {
					renderType.addDependency(dep.getSource(), dep.getSourceProperty());
				}
			}
		}

		return data;
	}

	/**
	 * Transform a page reference stored in two separate properties
	 * @param data data possibly holding the reference
	 * @param pageIdProperty name of the pageId property
	 * @param nodeIdProperty name of the nodeId property
	 * @param pageConsumer consumer handling the transformation for the given page
	 * @throws NodeException
	 */
	protected void transformPageReference(ObjectNode data, String pageIdProperty, String nodeIdProperty,
			Consumer<Page> pageConsumer) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		String pageId = data.optional(pageIdProperty).map(JsonNode::asText).orElse("");
		String nodeId = data.optional(nodeIdProperty).map(JsonNode::asText).orElse("");

		data.remove(pageIdProperty);
		data.remove(nodeIdProperty);

		Page referencedPage = null;
		int iNodeId = 0;

		if (!StringUtils.isBlank(pageId) && !Strings.CI.equals(pageId, "0")) {
			Node node = t.getObject(Node.class, nodeId);
			iNodeId = Optional.ofNullable(node).map(Node::getId).orElse(0);
			try (ChannelTrx cTrx = new ChannelTrx(iNodeId)) {
				referencedPage = t.getObject(Page.class, pageId);
			}
		}

		try (ChannelTrx cTrx = new ChannelTrx(iNodeId)) {
			pageConsumer.accept(referencedPage);
		}
	}

	/**
	 * Transform reference settings of the given element or property
	 * @param typeConfig type configuration
	 * @param elementType element type
	 * @param propertyOrElement property or element holding the settings
	 * @param languages languages of the form
	 * @throws NodeException
	 */
	protected void transformReferences(FormTypeConfiguration typeConfig, ElementConfig elementConfig,
			JsonNode propertyOrElement, List<String> languages) throws NodeException {
		Optional<JsonNode> optFormGridOptions = propertyOrElement.optional(FormDataUtils.FORM_GRID_OPTIONS);

		if (optFormGridOptions.isPresent() && elementConfig.settings != null) {
			for (BaseSetting setting : elementConfig.settings) {
				if (setting instanceof ReferenceSetting referenceSetting) {
					transformReference(referenceSetting, optFormGridOptions.get(), languages);
				}
			}
		}
	}

	/**
	 * Transform a single reference
	 * @param referenceSetting reference setting configuration
	 * @param formGridOptions JSON node holding the formGridOptions
	 * @param languages languages of the form
	 * @throws NodeException
	 */
	protected void transformReference(ReferenceSetting referenceSetting, JsonNode formGridOptions, List<String> languages)
			throws NodeException {
		Optional<JsonNode> optData = formGridOptions.optional(referenceSetting.id);
		if (optData.isPresent()) {
			JsonNode referenceData = optData.get();
			Transaction t = TransactionManager.getCurrentTransaction();

			String itemId = referenceData.optional("id").map(JsonNode::asText).orElse("");
			String nodeId = referenceData.optional("nodeId").map(JsonNode::asText).orElse("");
			String refTypeString = referenceData.optional("type").filter(JsonNode::isTextual).map(JsonNode::asText)
					.orElse("");
			if (!StringUtils.isBlank(itemId) && !Strings.CI.equals(itemId, "0")) {
				Node node = t.getObject(Node.class, nodeId);
				int iNodeId = Optional.ofNullable(node).map(Node::getId).orElse(0);
				try (ChannelTrx cTrx = new ChannelTrx(iNodeId)) {
					ReferenceType refType = ReferenceType.valueOf(refTypeString);
					LocalizableNodeObject<? extends NodeObject> referenced = null;
					switch (refType) {
					case file:
						referenced = t.getObject(File.class, itemId);
						break;
					case folder:
						referenced = t.getObject(Folder.class, itemId);
						break;
					case image:
						referenced = t.getObject(ImageFile.class, itemId);
						break;
					case page:
						referenced = t.getObject(Page.class, itemId);
						break;
					default:
						break;
					}

					if (referenced != null) {
						transformationTarget.transform(referenceSetting, (ObjectNode) formGridOptions, languages, node,
								referenced);
					} else {
						unknownReferenceCallback.operate();
					}
				} catch (IllegalArgumentException e) {
					// ignored
				}
			}
		}
	}

	/**
	 * Handle links in settings that allow richtext
	 * @param typeConfig type configuration
	 * @param elementConfig element configuration
	 * @param propertyOrElement property or element
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected void handleLinks(FormTypeConfiguration typeConfig, ElementConfig elementConfig,
			JsonNode propertyOrElement, List<String> languages) throws NodeException {
		Optional<JsonNode> optFormGridOptions = propertyOrElement.optional(FormDataUtils.FORM_GRID_OPTIONS);

		if (optFormGridOptions.isPresent() && elementConfig.settings != null) {
			for (BaseSetting setting : elementConfig.settings) {
				if (setting instanceof StringSetting stringSetting) {
					if (stringSetting.richContent) {
						transformationTarget.transform(stringSetting, (ObjectNode) optFormGridOptions.get(), languages);
					}
				} else if (setting instanceof TranslationSetting translationSetting) {
					if (translationSetting.richContent) {
						transformationTarget.transform(translationSetting, (ObjectNode) optFormGridOptions.get(), languages);
					}
				}
			}
		}
	}

	/**
	 * Do language fallback for the given object (if it is a page)
	 * @param <T> type of the object
	 * @param object object
	 * @param language target language
	 * @return target object
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends NodeObject> T languageFallback(T object, String language) throws NodeException {
		if (object instanceof Page page) {
			return (T) PageLanguageFallbackList.doFallback(page, LanguageFactory.get(language), page.getOwningNode());
		} else {
			return object;
		}
	}

	/**
	 * Render the page in all form languages and store the content as translated property in the node with given name
	 * @param node node where to store the content
	 * @param name property name
	 * @param page page to be rendered
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderPage(JsonNode node, String name, Page page, List<String> languages)
			throws NodeException {
		ObjectNode translatedRenderedPages = node.withObjectProperty(name).removeAll();
		for (String language : languages) {
			Page langPage = languageFallback(page, language);

			if (langPage != null) {
				translatedRenderedPages.put(language, langPage.render());
			}
		}
	}

	/**
	 * Render the given object as meshlink and store as translated property in the node with given name
	 * @param node node where to store the mesh links
	 * @param name property name
	 * @param object object to be linked
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderMeshlink(JsonNode node, String name, NodeObject object,
			List<String> languages) throws NodeException {
		// TODO
	}

	/**
	 * Render the URL to the given object and store as translated property in the node with given name
	 * @param node node where to store the URLs
	 * @param name property name
	 * @param object object for which the URL shall be rendered
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderUrl(JsonNode node, String name, NodeObject object, List<String> languages)
			throws NodeException {
		ObjectNode translatedUrls = node.withObjectProperty(name).removeAll();

		// generate the url factory with a static host link
		StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_HOST, RenderUrl.LINK_HOST, null);

		// we don't want the linkway to be changed
		urlFactory.setAllowAutoDetection(false);

		// disable link management
		urlFactory.setLinkManagement(LinkManagement.OFF);

		for (String language : languages) {
			NodeObject langObject = languageFallback(object, language);
			if (langObject != null) {
				try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, langObject, false, false,
						false)) {
					// Render the URL and set it as live URL
					RenderUrl renderUrl = urlFactory.createRenderUrl(langObject.getObjectInfo().getObjectClass(),
							langObject.getId());
					translatedUrls.put(language, renderUrl.toString());
				}
			}
		}
	}

	/**
	 * Transform the reference to global IDs
	 * @param jsonNode node holding the reference
	 * @param name name of the reference property
	 * @param node target node of the reference
	 * @param object target object of the reference
	 * @param idNames optional pair of names holding the reference IDs
	 * @throws NodeException
	 */
	protected static void toGlobalId(ObjectNode jsonNode, String name, Node node,
			LocalizableNodeObject<? extends NodeObject> object, Optional<Pair<String, String>> idNames)
			throws NodeException {
		ObjectNode base = idNames.map(ids -> jsonNode).orElseGet(() -> jsonNode.withObjectProperty(name));
		String itemIdName = idNames.map(Pair::getLeft).orElse("id");
		String nodeIdName = idNames.map(Pair::getRight).orElse("nodeId");

		if (object != null) {
			base.put(itemIdName, object.getMaster().getGlobalId().toString());
		} else {
			base.putNull(itemIdName);
		}
		if (node != null) {
			base.put(nodeIdName, node.getGlobalId().toString());
		} else {
			base.putNull(nodeIdName);
		}
	}

	/**
	 * Transform the reference to local IDs
	 * @param jsonNode node holding the reference
	 * @param name name of the reference property
	 * @param node target node of the reference
	 * @param object target object of the reference
	 * @param idNames optional pair of names holding the reference IDs
	 * @throws NodeException
	 */
	protected static void toLocalId(ObjectNode jsonNode, String name, Node node,
			LocalizableNodeObject<? extends NodeObject> object, Optional<Pair<String, String>> idNames)
			throws NodeException {
		ObjectNode base = idNames.map(ids -> jsonNode).orElseGet(() -> jsonNode.withObjectProperty(name));
		String itemIdName = idNames.map(Pair::getLeft).orElse("id");
		String nodeIdName = idNames.map(Pair::getRight).orElse("nodeId");

		if (object != null) {
			base.put(itemIdName, object.getMaster().getId());
		} else {
			base.putNull(itemIdName);
		}
		if (node != null) {
			base.put(nodeIdName, node.getId());
		} else {
			base.putNull(nodeIdName);
		}
	}

	/**
	 * Render URLS in text, if any.
	 * 
	 * @param text
	 * @param language
	 * @return
	 * @throws NodeException
	 */
	protected static final String renderUrls(String text, String language) throws NodeException {
		if (text == null) {
			// TODO check allow_links if possible
			return text;
		}
		String linkPattern = "\\{\\{"
				+ "[\\s]*(LINK)[\\s]*"
				+ "[|]"
				+ "[\\s]*(?<linkType>(PAGE|FILE|URL))"
				+ "[:]("
					+ "?:(?<=URL:)(?<url>[^|]*)|(?<!URL:)"
					+ "(?<nodeId>[a-zA-Z0-9\\-\\.]+)"
					+ "[:]"
					+ "(?<itemId>[a-zA-Z0-9\\-\\.]+)"
					+ "(?:[:](?<langCode>[a-zA-Z]{2}))?"
				+ ")[\\s]*[|][\\s]*"
				+ "(?<displayText>[^|]*)"
				+ "[\\s]*(?:[|][\\s]*(?<target>(_blank|_self|_top|_unfencedTop|_parent)))?[\\s]*"
				+ "\\}\\}";
		Pattern pattern = Pattern.compile(linkPattern);
		Matcher matcher = pattern.matcher(text);
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Deque<String> matches = new ArrayDeque<>(1);
			Transaction t = trx.getTransaction();
			while (matcher.find()) {
				String linkType = matcher.group("linkType");
				String nodeId = matcher.group("nodeId");
				String itemId = matcher.group("itemId");
				String displayText = matcher.group("displayText");
				Optional<String> maybeUrl = Optional.ofNullable(matcher.group("url"));
				Optional<String> maybeLangCode = Optional.ofNullable(matcher.group("langCode")).or(() -> Optional.ofNullable(language));
				Optional<String> maybeTarget = Optional.ofNullable(matcher.group("target"));
				Optional<Node> maybeChannelOrNode = Optional.ofNullable(t.getObject(Node.class, nodeId));
				Optional<Disinheritable<?>> maybeRenderedObject = Optional.empty();
				if ("FILE".equals(linkType)) {
					maybeRenderedObject = Optional.ofNullable(t.getObject(File.class, itemId));
					if (maybeRenderedObject.isEmpty()) {
						maybeRenderedObject = Optional.ofNullable(t.getObject(ImageFile.class, itemId));
					}
				} else if ("PAGE".equals(linkType)) {
					maybeRenderedObject = Optional.ofNullable(t.getObject(Page.class, itemId));
//					if (maybeLangCode.isPresent()) {
//						try {
//							Optional<Disinheritable<?>> maybeLocPage = maybeRenderedObject.map(Page.class::cast).flatMap(page -> {
//								try {
//									return Optional.ofNullable(page.getLanguageVariant(maybeLangCode.get(), channelOrNode.getId()));
//								} catch (NodeException e) {
//									throw new IllegalStateException(e);
//								}
//							});
//							if (maybeLocPage.isPresent()) {
//								maybeRenderedObject = maybeLocPage;
//							} else {
//								logger.warn(String.format("No '%s' localization found for %s", maybeLangCode.get(), maybeRenderedObject));
//							}
//						} catch (Throwable e) {
//							if (e.getCause() instanceof NodeException) {
//								throw (NodeException) e.getCause();
//							} else {
//								throw e;
//							}
//						}
//					}
				} else if ("URL".equals(linkType) && maybeUrl.isEmpty()) {
					throw new NodeException("No URL provided for " + matcher.toString());
				}
				String url;
				if ("URL".equals(linkType)) {
					url = maybeUrl.get();
				} else {
					Disinheritable<?> renderedObject = maybeRenderedObject.get();
//					if (!(renderedObject.getChannel() != null && renderedObject.getChannel().getId().intValue() == channelOrNode.getId().intValue()) 
//							&& !(renderedObject.getNode() != null && renderedObject.getNode().getId().intValue() == channelOrNode.getId().intValue())) {
//						// TODO more meaningful error / logging
//						throw new NodeException("Wrong node / channel for " + matcher.toString());
//					}
					url = MeshURLRenderer.renderDisinheritableUrl(renderedObject);
				}
				matches.addLast(String.format("<a href='%s' %s>%s</a>", 
						Matcher.quoteReplacement(UrlEscapeUtils.unescapeUrl(url)), 
						maybeTarget.map(target -> "target='" + target + "'").orElse(org.apache.commons.lang3.StringUtils.EMPTY), 
						Matcher.quoteReplacement(UrlEscapeUtils.unescapeUrl(displayText))
					));
			}
			String link;
			while ((link = matches.poll()) != null) {
				text = text.replaceFirst(linkPattern, link);
			}
		}
		return text;
	}

	/**
	 * Enum of possible transformation targets
	 */
	public static enum TransformationTarget {
		/**
		 * Transform the form for publishing. This will transform references defined by
		 * their "publishAs" settings
		 */
		publish,

		/**
		 * Transform the form for exporting it into a devtool package. This will
		 * transform references to global IDs
		 */
		toContentPackage,

		/**
		 * Transform the form for importing it from a devtool package. This will
		 * transform references to local IDs
		 */
		fromContentPackage;

		/**
		 * Transform the base form data
		 * @param form form
		 * @param data form data
		 * @throws NodeException
		 */
		public void transform(Form form, ObjectNode data) throws NodeException {
			switch (this) {
			case publish:
				data.put("cms_id", form.getId());
				data.put("type", form.getFormType());
				// schema.properties is required in the plugin
				data.withObjectProperty("schema").withObjectProperty("properties");
				break;
			case fromContentPackage:
			case toContentPackage:
			default:
				break;
			
			}
		}

		/**
		 * Transform the reference
		 * @param referenceSetting reference setting
		 * @param referenceNode node holding the reference
		 * @param languages form languages
		 * @param node target node of the reference
		 * @param referenced target object of the reference
		 * @throws NodeException
		 */
		public void transform(ReferenceSetting referenceSetting, ObjectNode referenceNode, List<String> languages,
				Node node, LocalizableNodeObject<? extends NodeObject> referenced) throws NodeException {
			transform(referenceSetting, referenceNode, languages, node, referenced, Optional.empty());
		}

		/**
		 * Transform the reference
		 * @param referenceSetting reference setting
		 * @param referenceNode node holding the reference
		 * @param languages form languages
		 * @param node target node of the reference
		 * @param referenced target object of the reference
		 * @param idNames optional pair of ID names
		 * @throws NodeException
		 */
		public void transform(ReferenceSetting referenceSetting, ObjectNode referenceNode, List<String> languages,
				Node node, LocalizableNodeObject<? extends NodeObject> referenced,
				Optional<Pair<String, String>> idNames) throws NodeException {
			switch (this) {
			case toContentPackage:
				toGlobalId(referenceNode, referenceSetting.id, node, referenced, idNames);
				break;
			case fromContentPackage:
				toLocalId(referenceNode, referenceSetting.id, node, referenced, idNames);
				break;
			case publish:
				switch (referenceSetting.publishAs) {
				case content:
					if (referenced instanceof Page page) {
						renderPage(referenceNode, referenceSetting.id, page, languages);
					}
					break;
				case meshlink:
					renderMeshlink(referenceNode, referenceSetting.id, referenced, languages);
					break;
				case url:
					renderUrl(referenceNode, referenceSetting.id, referenced, languages);
					break;
				}
				break;
			default:
				break;
			}
		}

		public void transform(StringSetting stringSetting, ObjectNode node, List<String> languages)
				throws NodeException {
			switch(this) {
			case toContentPackage:
				break;
			case fromContentPackage:
				break;
			case publish:
				break;
			default:
				break;
			}
		}

		public void transform(TranslationSetting stringSetting, ObjectNode node, List<String> languages)
				throws NodeException {
			switch(this) {
			case toContentPackage:
				break;
			case fromContentPackage:
				break;
			case publish:
				for (Entry<String, JsonNode> entry : node.optional(stringSetting.id).filter(JsonNode::isObject)
						.map(JsonNode::properties).orElse(Collections.emptySet())) {
					String language = entry.getKey();
					if (languages.contains(language)) {
						String text = entry.getValue().asText();
						text = renderUrls(text, language);
						entry.setValue(node.textNode(text));
					}
				}
				break;
			default:
				break;
			}
		}
	}
}
