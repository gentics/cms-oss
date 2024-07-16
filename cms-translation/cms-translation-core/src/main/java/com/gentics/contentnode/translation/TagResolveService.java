package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class TagResolveService {

	static final Pattern NODE_TAGS_PATTERN = Pattern.compile("(<node [a-z]+\\d*>)",
			Pattern.CASE_INSENSITIVE);
	static final Pattern RESOLVED_TAGS_PATTERN = Pattern.compile("\\{\\{(.+?)}}", Pattern.DOTALL);
	static final int HTML_TEXT_PART_TYPE_ID = 2;


	public void resolveAndTranslateContentTags(
			List<Tag> contentTags,
			TranslationService translationService,
			String sourceLanguageCode,
			String targetLanguageCode
	) throws Exception {
		var tagsToResolve = contentTags.stream()
				.filter(tag -> HTML_TEXT_PART_TYPE_ID == tag.getConstructId())
				.toList();

		for (var contentTagToResolve : tagsToResolve) {
			var resolvedTextOpt = this.getTextProperty(contentTagToResolve);

			if (resolvedTextOpt.isPresent()) {
				var resolvableText = resolvedTextOpt.get().getStringValue();
				var untranslatedTagMap = this.getOccurringTagsFromResolvableText(contentTags,
						resolvableText);
				var resolvedText = this.resolveTags(untranslatedTagMap, resolvableText);

				var resolvedAndTranslatedText = translationService.translate(
						resolvedText,
						sourceLanguageCode,
						targetLanguageCode);

				// todo: unresolve resolvedAndTranslatedText
				resolvedTextOpt.get().setStringValue(resolvedAndTranslatedText);
				this.updateContentTagsListToTranslatedValues(resolvedAndTranslatedText, untranslatedTagMap);
			}
		}
	}

	/**
	 * This method resolves the tags in the given text.
	 * It iterates over the untranslatedTagMap and for each tag, it retrieves the text property of the tag.
	 * If the text property is present, it replaces the tag in the resolvable text with the content of the text property.
	 * The content of the text property is wrapped in double curly braces.
	 *
	 * @param untranslatedTagMap The map of untranslated tags.
	 * @param resolvableText The text in which to resolve the tags.
	 * @return The text with the tags resolved.
	 */
	private String resolveTags(Map<String, Tag> untranslatedTagMap, String resolvableText) {
		var resolvedText = resolvableText;
		for (var tag : untranslatedTagMap.entrySet()) {
			var tagStringValueOpt = this.getTextProperty(tag.getValue());
			if (tagStringValueOpt.isPresent()) {
				var tagContent = tagStringValueOpt.get().getStringValue();
				tagContent = String.format("{{%s}}", tagContent);
				resolvedText = resolvedText.replaceAll(tag.getKey(), tagContent);
			}
		}
		return resolvedText;
	}

	/**
	 * This method updates the list of content tags with their translated values.
	 * It iterates over the untranslatedTagMap and replaces the stringValues with the translated values.
	 *
	 * @param resolvedAndTranslatedText The text that has been resolved and translated.
	 * @param untranslatedTagMap The map of tags that have not been translated.
	 * @throws NodeException If the number of resolved tags in the translated text and the map of untranslated tags are not equal.
	 */
	private void updateContentTagsListToTranslatedValues(
			String resolvedAndTranslatedText,
			Map<String, Tag> untranslatedTagMap
	) throws NodeException {
		final Matcher matcher = RESOLVED_TAGS_PATTERN.matcher(resolvedAndTranslatedText);

		List<String> resolvedTags = new ArrayList<>();
		while (matcher.find()) {
			resolvedTags.add(matcher.group(1));
		}

		if (resolvedTags.size() != untranslatedTagMap.size()) {
			throw new NodeException(
					"Number of resolved tags in translated text and map of untranslated tags must be equals");
		}

		Collections.reverse(resolvedTags); // todo check if the order can be guaranteed

		List<Map.Entry<String, Tag>> unTranslatedTagList = new ArrayList<>(
				untranslatedTagMap.entrySet());

		for (int i = 0; i < unTranslatedTagList.size(); i++) {
			var contentTag = unTranslatedTagList.get(i).getValue();
			var translatedValue = resolvedTags.get(i);

			this.getTextProperty(contentTag).ifPresent(untranslatedProperty ->
					untranslatedProperty.setStringValue(translatedValue)
			);
		}
	}

	/**
	 * This method retrieves the tags that occur in the resolvable text from a given list of content tags.
	 * It iterates over the resolvable text and for each tag that it finds, it adds the tag to a map with the tag's name as the key and the tag itself as the value.
	 *
	 * @param contentTags The list of content tags to search in.
	 * @param resolvableText The text in which to find the tags.
	 * @return A map with the names of the tags as keys and the tags themselves as values.
	 * @throws RuntimeException If a tag is found in the resolvable text but not in the list of content tags.
	 */
	private Map<String, Tag> getOccurringTagsFromResolvableText(
			List<Tag> contentTags,
			String resolvableText
	) {
		Map<String, Tag> tagMap = new HashMap<>();
		final var matcher = NODE_TAGS_PATTERN.matcher(resolvableText);

		while (matcher.find()) {
			var matchedNodeTag = matcher.group(0).trim();
			var correspondingTag = contentTags.stream()
					.filter(tag -> matchedNodeTag.contains(
							tag.getName())) //todo: contains ok? gtxlink1 and gtxlik11 collide
					.findAny()
					.orElseThrow(() -> new RuntimeException("Corresponding tag  not found"));
			tagMap.put(matchedNodeTag, correspondingTag);
		}

		return tagMap;
	}

	/**
	 * This method retrieves the text property from a given content tag.
	 *
	 * @param contentTag The content tag from which to retrieve the text property.
	 * @return An Optional containing the text property if it exists, otherwise an empty Optional.
	 */
	private Optional<Property> getTextProperty(Tag contentTag) {
		for (var contentTagProperty : contentTag.getProperties().entrySet()) {
			if (contentTagProperty != null && !StringUtils.isBlank(
					contentTagProperty.getValue().getStringValue())) {
				var propertyToTranslate = contentTagProperty.getValue();
				return Optional.of(propertyToTranslate);
			}
		}
		return Optional.empty();
	}


}
