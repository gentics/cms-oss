package com.gentics.contentnode.translation;

import static com.gentics.contentnode.etc.JsoupValidator.VALIDATOR;

import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Service to combine all tags into a combined/resolved string.
 */
public class TagResolveService {

	public static final NodeLogger logger = NodeLogger.getNodeLogger(TagResolveService.class);
	static final Pattern NODE_TAGS_PATTERN = Pattern.compile("(<node [a-z]+\\d*>)",
			Pattern.CASE_INSENSITIVE);
	static final Pattern RESOLVED_TAGS_PATTERN = Pattern.compile("\\{\\{(.+?)}}", Pattern.DOTALL);

	/**
	 * This function resolves and translates content tags.
	 *
	 * @param contentTags        The list of content tags.
	 * @param translationService The translation service to use for translating the resolved text.
	 * @param sourceLanguageCode The source language code for translation.
	 * @param targetLanguageCode The target language code for translation.
	 * @throws Exception If an error occurs during the process.
	 */
	public void resolveAndTranslateContentTags(
			Node node,
			List<Tag> contentTags,
			TranslationService translationService,
			String sourceLanguageCode,
			String targetLanguageCode
	) throws Exception {
		final var linkKeyword = AlohaRenderer.getMagicLinkConstructKeyword(node);
		final var tagsToResolve = contentTags.stream()
				.filter(tag -> !tag.getName().contains(linkKeyword))
				.toList();

		for (var contentTagToResolve : tagsToResolve) {
			var textProperties = this.getTextProperties(contentTagToResolve);

			for (var textProperty : textProperties) {
				var resolvableText = textProperty.getStringValue();

				// todo: check why markup is broken before
				if (!VALIDATOR.test(resolvableText)) {
					logger.error(new CNI18nString("feature.translation.error.markup_invalid").toString());
				}

				var untranslatedTagMap = this.getOccurringTagsFromResolvableText(contentTags,
						resolvableText, linkKeyword);
				final var resolvedText = this.resolveTags(untranslatedTagMap, resolvableText);

				final var resolvedAndTranslatedText = translationService.translate(
						resolvedText,
						sourceLanguageCode,
						targetLanguageCode);

				final var unresolvedAndTranslatedText = this.unresolveTags(untranslatedTagMap,
						resolvedAndTranslatedText);
				textProperty.setStringValue(unresolvedAndTranslatedText);
				this.updateContentTagsListToTranslatedValues(resolvedAndTranslatedText, untranslatedTagMap);
			}
		}
	}

	/**
	 * This method resolves the tags in the given text. It replaces the tag in the resolvable text
	 * with the content of the text property. The content of the text property is wrapped in double
	 * curly braces.
	 *
	 * @param untranslatedTagMap The map of untranslated tags.
	 * @param resolvableText     The text in which to resolve the tags.
	 * @return The text with the tags resolved.
	 */
	private String resolveTags(Map<String, Tag> untranslatedTagMap, String resolvableText) {
		var resolvedText = resolvableText;
		for (var tag : untranslatedTagMap.entrySet()) {
			var tagStringValueOpt = this.getResolvablePartProperty(tag.getValue());
			if (tagStringValueOpt.isPresent()) {
				var tagContent = tagStringValueOpt.get().getStringValue();
				tagContent = String.format("{{%s}}", tagContent);
				resolvedText = resolvedText.replaceAll(tag.getKey(), tagContent);
			}
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
	private String unresolveTags(Map<String, Tag> resolvedTagMap, String resolvedText) {
		var unresolvedText = resolvedText;

		final Matcher matcher = RESOLVED_TAGS_PATTERN.matcher(unresolvedText);
		List<String> stringsToUnresolve = new ArrayList<>();
		while (matcher.find()) {
			stringsToUnresolve.add(matcher.group(0));
		}

		List<Map.Entry<String, Tag>> translatedTagList = new ArrayList<>(
				resolvedTagMap.entrySet());

		if (translatedTagList.size() != stringsToUnresolve.size()) {
			return unresolvedText;
		}

		Collections.reverse(stringsToUnresolve);

		for (int i = 0; i < translatedTagList.size(); i++) {
			var stringToUnresolve = stringsToUnresolve.get(i);
			var nodeTag = translatedTagList.get(i).getKey();

			unresolvedText = unresolvedText.replace(stringToUnresolve, nodeTag);
		}

		return unresolvedText;
	}

	/**
	 * This method updates the list of content tags with their translated values. It iterates over the
	 * untranslatedTagMap and replaces the stringValues with the translated values.
	 *
	 * @param resolvedAndTranslatedText The text that has been resolved and translated.
	 * @param untranslatedTagMap        The map of tags that have not been translated.
	 */
	private void updateContentTagsListToTranslatedValues(
			String resolvedAndTranslatedText,
			Map<String, Tag> untranslatedTagMap
	) {
		final Matcher matcher = RESOLVED_TAGS_PATTERN.matcher(resolvedAndTranslatedText);

		List<String> resolvedTags = new ArrayList<>();
		while (matcher.find()) {
			resolvedTags.add(matcher.group(1));
		}

		if (resolvedTags.size() != untranslatedTagMap.size()) {
			logger.error(
					"Number of resolved tags in translated text and map of untranslated tags must be equals.");
			return;
		}
		Collections.reverse(resolvedTags);

		List<Map.Entry<String, Tag>> unTranslatedTagList = new ArrayList<>(
				untranslatedTagMap.entrySet());

		for (int i = 0; i < unTranslatedTagList.size(); i++) {
			var contentTag = unTranslatedTagList.get(i).getValue();
			var translatedValue = resolvedTags.get(i);

			this.getResolvablePartProperty(contentTag).ifPresent(untranslatedProperty ->
					untranslatedProperty.setStringValue(translatedValue)
			);
		}
	}

	/**
	 * This method retrieves the tags that occur in the resolvable text from a given list of content
	 * tags.
	 *
	 * @param contentTags    The list of content tags to search in.
	 * @param resolvableText The text in which to find the tags.
	 * @return A map with the names of the tags as keys and the tags themselves as values.
	 * @throws RuntimeException If a tag is found in the resolvable text but not in the list of
	 *                          content tags.
	 */
	private Map<String, Tag> getOccurringTagsFromResolvableText(
			List<Tag> contentTags,
			String resolvableText,
			String linkKeyword
	) {
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
					.filter(tag -> tag.getName().contains(linkKeyword))
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
