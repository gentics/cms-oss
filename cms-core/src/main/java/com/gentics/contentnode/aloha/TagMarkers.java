package com.gentics.contentnode.aloha;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Annotation of the tags of a page rendered in readonly mode with {@link AlohaRenderer#TAG_MARKERS}, without inserting
 * an element: {@link #mark(String, String)} annotates the rendered code of a single tag, {@link #resolve(String)} the
 * elements around tags, which rendered only text, in the rendered page.
 */
public final class TagMarkers {
	/**
	 * Pattern for an html start tag (group 1: element name)
	 */
	private static final Pattern START_TAG_PATTERN = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*>", Pattern.DOTALL);

	/**
	 * HTML elements without a closing tag
	 */
	private static final Set<String> VOID_ELEMENTS = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
			"param", "source", "track", "wbr");

	/**
	 * Start of the temporary marker around tag output that is only text, followed by the annotations
	 */
	private static final String MARKER_START = "<!--gtxtagmarker ";

	/**
	 * End of the temporary marker around tag output that is only text
	 */
	private static final String MARKER_END = "<!--/gtxtagmarker-->";

	/**
	 * Pattern for a temporary marker that contains no other marker (group 1: annotations, group 2: marked code)
	 */
	private static final Pattern INNERMOST_MARKER_PATTERN = Pattern.compile(
			"<!--gtxtagmarker ([^>]*)-->((?:(?!<!--/?gtxtagmarker).)*?)<!--/gtxtagmarker-->", Pattern.DOTALL);

	/**
	 * Pattern for a start tag, followed only by white space (group 1: start tag, group 2: element name)
	 */
	private static final Pattern PARENT_START_PATTERN = Pattern.compile("(<([a-zA-Z][a-zA-Z0-9]*)\\b[^<>]*>)\\s*", Pattern.DOTALL);

	private TagMarkers() {
	}

	/**
	 * Annotate the rendered code of a tag, without inserting an element:
	 * <ul>
	 * <li>code consisting of elements only: every top-level element, which is not annotated yet, gets the annotations</li>
	 * <li>code consisting of text only: the code is surrounded with a temporary marker, which {@link #resolve(String)} resolves</li>
	 * <li>empty code, code mixing text and elements, or code with an element that is not closed: unchanged</li>
	 * </ul>
	 * @param code rendered code of the tag
	 * @param annotations data- attribute annotations of the tag
	 * @return annotated code
	 */
	public static String mark(String code, String annotations) {
		String trimmed = code.strip();

		if (trimmed.isEmpty()) {
			return code;
		}
		int offset = code.indexOf(trimmed);
		List<Integer> elementStarts = new ArrayList<>();
		boolean hasText = false;
		int pos = 0;

		while (pos < trimmed.length()) {
			char c = trimmed.charAt(pos);

			if (trimmed.startsWith("<!--", pos)) {
				int commentEnd = trimmed.indexOf("-->", pos + 4);

				if (commentEnd < 0) {
					return code;
				}
				pos = commentEnd + 3;
			} else if (c == '<' && pos + 1 < trimmed.length() && trimmed.charAt(pos + 1) == '/') {
				// closing tag without an element
				return code;
			} else if (c == '<') {
				Matcher startTag = START_TAG_PATTERN.matcher(trimmed).region(pos, trimmed.length());

				if (!startTag.lookingAt()) {
					hasText = true;
					pos++;
					continue;
				}
				elementStarts.add(pos);
				String name = startTag.group(1);

				if (VOID_ELEMENTS.contains(name.toLowerCase()) || startTag.group().endsWith("/>")) {
					pos = startTag.end();
				} else {
					pos = findElementEnd(trimmed, name, startTag.end());
					if (pos < 0) {
						return code;
					}
				}
			} else {
				hasText |= !Character.isWhitespace(c);
				pos++;
			}
		}

		if (elementStarts.isEmpty()) {
			if (!hasText) {
				return code;
			}
			return code.substring(0, offset) + MARKER_START + annotations + "-->" + trimmed + MARKER_END
					+ code.substring(offset + trimmed.length());
		}
		if (hasText) {
			return code;
		}

		StringBuilder annotated = new StringBuilder(code);

		// annotate from the back, so that the positions of the elements before do not change
		for (int i = elementStarts.size() - 1; i >= 0; i--) {
			Matcher startTag = START_TAG_PATTERN.matcher(trimmed).region(elementStarts.get(i), trimmed.length());

			startTag.lookingAt();
			if (!startTag.group().contains("data-gcn-tagid=")) {
				annotated.insert(offset + startTag.end(1), " " + annotations);
			}
		}
		return annotated.toString();
	}

	/**
	 * Resolve the temporary markers, which {@link #mark(String, String)} put around tags, that rendered only text,
	 * innermost first: if the marker is the only content of the element around it (white space aside), and that element
	 * is not annotated yet, the element gets the annotations. The markers are removed in any case.
	 * @param template rendered page
	 * @return rendered page without markers
	 */
	public static String resolve(String template) {
		if (!template.contains(MARKER_START)) {
			return template;
		}
		StringBuilder templateBuilder = new StringBuilder(template);
		Matcher marker = INNERMOST_MARKER_PATTERN.matcher(templateBuilder);

		while (marker.find()) {
			String annotations = marker.group(1);
			String marked = marker.group(2);
			int start = marker.start();
			int end = marker.end();

			templateBuilder.replace(start, end, marked);

			int parentStart = templateBuilder.lastIndexOf("<", start - 1);

			if (parentStart >= 0) {
				Matcher parent = PARENT_START_PATTERN.matcher(templateBuilder).region(parentStart, start);

				if (parent.matches() && !VOID_ELEMENTS.contains(parent.group(2).toLowerCase()) && !parent.group(1).contains("data-gcn-tagid=")) {
					Matcher parentEnd = Pattern.compile("\\s*</" + Pattern.quote(parent.group(2)) + "\\s*>", Pattern.CASE_INSENSITIVE)
							.matcher(templateBuilder).region(start + marked.length(), templateBuilder.length());

					if (parentEnd.lookingAt()) {
						templateBuilder.insert(parent.end(2), " " + annotations);
					}
				}
			}
			marker.reset(templateBuilder);
		}
		return templateBuilder.toString();
	}

	/**
	 * Find the end of the element with the given name, counting only the start and end tags of elements with that name
	 * @param code code
	 * @param name element name
	 * @param from position after the start tag of the element
	 * @return position after the end tag of the element, -1 if the element is not closed
	 */
	private static int findElementEnd(String code, String name, int from) {
		Matcher tagMatcher = Pattern.compile("<(/?)" + Pattern.quote(name) + "\\b[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
				.matcher(code).region(from, code.length());
		int count = 1;

		while (tagMatcher.find()) {
			if (!tagMatcher.group(1).isEmpty()) {
				count--;
			} else if (!tagMatcher.group().endsWith("/>")) {
				count++;
			}
			if (count == 0) {
				return tagMatcher.end();
			}
		}
		return -1;
	}
}
