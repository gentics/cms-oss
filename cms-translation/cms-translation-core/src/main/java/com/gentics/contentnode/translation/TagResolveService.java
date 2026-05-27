package com.gentics.contentnode.translation;

import static com.gentics.contentnode.etc.JsoupValidator.VALIDATOR;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.TranslationException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.TranslationStats;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.lib.log.NodeLogger;

/**
 * Service to combine all tags into a combined/resolved string.
 */
public class TagResolveService {

	public static final NodeLogger logger = NodeLogger.getNodeLogger(TagResolveService.class);
	private static final Pattern NODE_TAGS_PATTERN = Pattern.compile("(<node [a-z]+\\d*>)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern RESOLVED_TAGS_PATTERN = Pattern.compile(
			"<span data-translate-id=\"(.*?)\">(.*?)<\\/span>", Pattern.DOTALL);
	private final int linkConstructId;

	/**
	 * Node ctor.
	 * 
	 * @param node
	 * @throws NodeException
	 */
	public TagResolveService(Node node) throws NodeException {
		this(getConstructIdForLinks(node));
	}

	/**
	 * Non-node ctor. Avoid to use, unless absolutely sure.
	 * 
	 * @param linkConstructId
	 * @throws NodeException
	 */
	protected TagResolveService(int linkConstructId) throws NodeException {
		this.linkConstructId = linkConstructId;
	}

	/**
	 * This method retrieves the construct ID for links associated with a given node.
	 *
	 * @param node The node for which to retrieve the construct ID.
	 * @return The construct ID associated with the links of the given node.
	 * @throws NodeException If an error occurs while retrieving the construct ID.
	 */
	private static int getConstructIdForLinks(Node node) throws NodeException {
		final var linkKeyword = AlohaRenderer.getMagicLinkConstructKeyword(node);

		return DBUtils.select("SELECT id FROM construct WHERE keyword = ?",
				pst -> pst.setString(1, linkKeyword), DBUtils.firstInt("id"));
	}

	/**
	 * This function resolves and translates content tags.
	 *
	 * @param contentTags        The list of content tags.
	 * @param translationService The translation service to use for translating the resolved text.
	 * @param sourceLanguageCode The source language code for translation.
	 * @param targetLanguageCode The target language code for translation.
	 * @param stats              Statistics
	 * @throws Exception If an error occurs during the process.
	 */
	public void resolveAndTranslateContentTags(
			List<Tag> contentTags,
			TranslationService translationService,
			String sourceLanguageCode,
			String targetLanguageCode, TranslationStats stats
	) throws Exception {
		final var tagsToResolve = contentTags.stream()
				.filter(tag -> tag.getConstructId() != this.linkConstructId)
				.toList();

		for (var contentTagToResolve : tagsToResolve) {
			var textProperties = this.getTextProperties(contentTagToResolve);

			for (var textProperty : textProperties) {
				var resolvableText = textProperty.getStringValue();
				textProperty.setStringValue(resolveAndTranslateTextContent(resolvableText, contentTags, translationService, sourceLanguageCode, targetLanguageCode, stats));
			}
		}
	}

	/**
	 * Resolve and translate text content.
	 * 
	 * @param resolvableText     The text to resolve and translate
	 * @param contentTags        The whole list of content tags.
	 * @param translationService The translation service to use for translating the resolved text.
	 * @param sourceLanguageCode The source language code for translation.
	 * @param targetLanguageCode The target language code for translation.
	 * @param stats              Statistics
	 * @return
	 * @throws Exception
	 */
	protected String resolveAndTranslateTextContent(
			String resolvableText,
			List<Tag> contentTags,
			TranslationService translationService,
			String sourceLanguageCode,
			String targetLanguageCode, 
			TranslationStats stats
	) throws Exception {
		if (StringUtils.isBlank(resolvableText)) {
			return resolvableText;
		}
		var isMarkupCorrupt = false;
		if (!VALIDATOR.test(resolvableText.replaceAll(String.valueOf(NODE_TAGS_PATTERN), ""))) {
			isMarkupCorrupt = true;
			logger.warn(I18NHelper.get("feature.translation.error.markup_invalid"));
		}

		final var untranslatedTagMap = this.getOccurringTagsFromResolvableText(contentTags,
				resolvableText);
		final var resolvedText = this.resolveTags(untranslatedTagMap, resolvableText);

		final var resolvedAndTranslatedText = translationService.translate(
				resolvedText,
				sourceLanguageCode,
				targetLanguageCode);

		if (stats != null) {
			stats.addRequest(resolvedText.length(), resolvedAndTranslatedText.length());
		}

		final var unresolvedAndTranslatedText = this.unresolveTags(untranslatedTagMap,
				resolvedAndTranslatedText);

		final var cleanHtml = unresolvedAndTranslatedText.replaceAll(String.valueOf(NODE_TAGS_PATTERN), "");
		if (!isMarkupCorrupt && !VALIDATOR.test(cleanHtml)) {
			// if debug logging is enabled, we also log what has been sent to the translation service
			// and what has been received
			// but we log this as error (since the result was invalid markup)
			if (logger.isDebugEnabled()) {
				logger.error("Translated from %s into %s resulted in invalid markup:\n%s\ntranslation:\n%s".formatted(sourceLanguageCode,
						targetLanguageCode, resolvedText, resolvedAndTranslatedText));
			}
			throw new TranslationException(I18NHelper.get("feature.translation.error",
					I18NHelper.get("feature.translation.error.reason.markup_invalid")));
		}
		return unresolvedAndTranslatedText;
	}

	/**
	 * This method resolves the tags in the given text. It replaces the tag in the resolvable text
	 * with the content of the text property. The content of the text property is wrapped in a span.
	 *
	 * @param untranslatedTagMap The map of untranslated tags.
	 * @param resolvableText     The text in which to resolve the tags.
	 * @return The text with the tags resolved.
	 */
	private String resolveTags(Map<String, Tag> untranslatedTagMap, String resolvableText) {
		var resolvedText = resolvableText;
		for (var tag : untranslatedTagMap.entrySet()) {
			var tagStringValueOpt = this.getResolvablePartProperty(tag.getValue());
			var tagContent = "";

			if (tagStringValueOpt.isPresent()) {
				tagContent = tagStringValueOpt.get().getStringValue();
			}
			tagContent = String.format("<span data-translate-id=\"%s\">%s</span>",
					tag.getValue().getId(), tagContent);
			resolvedText = resolvedText.replaceAll(tag.getKey(), tagContent);
		}
		return resolvedText;
	}

	/**
	 * This function undoes the tag resolution performed by the resolveTags function.
	 *
	 * @param resolvedTagMap A map where the keys are the original tag names and the values are the
	 *                       resolved tag contents.
	 * @param resolvedText   The text with resolved tags.
	 * @return The original text with unresolved tags.
	 */
	private String unresolveTags(Map<String, Tag> resolvedTagMap, String resolvedText)
			throws NodeException {
		var unresolvedText = resolvedText;

		final Matcher matcher = RESOLVED_TAGS_PATTERN.matcher(unresolvedText);
		List<Map.Entry<String, Tag>> translatedTagList = new ArrayList<>(
				resolvedTagMap.entrySet());

		while (matcher.find()) {
			var tagToUnresolve = matcher.group(0);
			var idAndText = this.extractIdAndTranslatedText(tagToUnresolve);
			var replacementId = idAndText.getLeft();
			var translatedText = idAndText.getRight();

			var originalTag = translatedTagList.stream()
					.filter(tag -> tag.getValue().getId() == replacementId.intValue()).findFirst()
					.orElseThrow(()-> new NodeException("Could not find tag with id " + replacementId + " to unresolve tag"));
			unresolvedText = unresolvedText.replace(tagToUnresolve, originalTag.getKey());

			// update tags with translated text
			this.getResolvablePartProperty(originalTag.getValue()).ifPresent(untranslatedProperty ->
					untranslatedProperty.setStringValue(translatedText)
			);
		}
		return unresolvedText;
	}

	/**
	 * This method is used to extract the ID and the translated text from a given tag.
	 *
	 * @param tagToUnresolve The tag from which the ID and translated text are to be extracted.
	 * @return A pair containing the extracted ID as an Integer and the translated text as a String.
	 * @throws NodeException If there is an error while extracting the ID and translated text.
	 */
	private Pair<Integer, String> extractIdAndTranslatedText(String tagToUnresolve)
			throws NodeException {
		Matcher matcher = RESOLVED_TAGS_PATTERN.matcher(tagToUnresolve);

		if (matcher.find()) {
			var matchedId = matcher.group(1);
			var matchedText = matcher.group(2);

			try {
				int id = Integer.parseInt(matchedId);
				return Pair.of(id, matchedText);
			} catch (NumberFormatException e) {
				throw new NodeException("Unable to parse replacementId: " + matchedId + " to an integer", e);
			}
		}
		throw new NodeException("Unable to extract replacementId or translated text from resolved tag");
	}

	/**
	 * This method retrieves the tags that occur in the resolvable text from a given list of content
	 * tags.
	 *
	 * @param contentTags    The list of content tags to search in.
	 * @param resolvableText The text in which to find the tags.
	 * @return A map with the names of the tags as keys and the tags themselves as values.
	 */
	private Map<String, Tag> getOccurringTagsFromResolvableText(List<Tag> contentTags,
			String resolvableText) {
		Map<String, Tag> tagMap = new LinkedHashMap<>();
		final var matcher = NODE_TAGS_PATTERN.matcher(resolvableText);

		while (matcher.find()) {
			var matchedNodeTag = matcher.group(0).trim();
			var correspondingTag = contentTags.stream()
					.filter(tag -> matchedNodeTag
							.replace("<node", "")
							.replace(">", "")
							.trim()
							.equals(tag.getName()))
					.findAny();
			correspondingTag.ifPresent(tag -> tagMap.put(matchedNodeTag, tag));
		}

		return tagMap;
	}

	/**
	 * This method retrieves the text property from a given content tag.
	 *
	 * @param contentTag The content tag from which to retrieve the text property.
	 * @return An Optional containing the text property if it exists, otherwise an empty Optional.
	 */
	private List<Property> getTextProperties(Tag contentTag) {
		List<Property> textProperties = new ArrayList<>();
		for (var contentTagProperty : contentTag.getProperties().entrySet()) {
			if (contentTagProperty != null && !StringUtils.isBlank(
					contentTagProperty.getValue().getStringValue())) {
				var propertyToTranslate = contentTagProperty.getValue();
				textProperties.add(propertyToTranslate);
			}
		}
		return textProperties;
	}

	/**
	 * This method is used to retrieve a resolvable property from a given content tag.
	 *
	 * @param contentTag The Tag object from which the property is to be retrieved.
	 * @return An Optional<Property> object. If a property with key "text" and non-blank string value
	 * exists, it is wrapped into an Optional and returned. Otherwise, an empty Optional is returned.
	 */
	private Optional<Property> getResolvablePartProperty(Tag contentTag) {
		for (var contentTagProperty : contentTag.getProperties().entrySet()) {
			if (contentTagProperty != null
					&& "text".equals(contentTagProperty.getKey())
					&& !StringUtils.isBlank(contentTagProperty.getValue().getStringValue())
			) {
				var propertyToTranslate = contentTagProperty.getValue();
				return Optional.of(propertyToTranslate);
			}
		}
		return Optional.empty();
	}

}
