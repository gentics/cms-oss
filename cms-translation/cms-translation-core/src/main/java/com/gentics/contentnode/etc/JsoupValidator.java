package com.gentics.contentnode.etc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;


/**
 * Validator implementation using Jsoup <a href="https://jsoup.org/">...</a>
 */
public class JsoupValidator implements Validator {

	/**
	 * A validator function that verifies html markup
	 */
	public static final Predicate<String> VALIDATOR = (String html) -> {
		Validator validator = new JsoupValidator();
		return validator.isValid(html);
	};

	/**
	 * Specifies the allowed tags. i.e.: How strict the check is.
	 */
	Safelist safelist = Safelist.relaxed();

	@Override
	public boolean isValid(String html) {
		return Jsoup.isValid(html, safelist);
	}

	@Override
	public List<String> validateAndCollectErrors(String html) {
		Parser parser = Parser.htmlParser().setTrackErrors(10);

		Document document = Jsoup.parse(html, StringUtils.EMPTY, parser);
		document.outputSettings().prettyPrint(false);

		List<String> errors = new ArrayList<>();
		for (var parseError : document.parser().getErrors()) {
			errors.add(parseError.getErrorMessage());
		}

		return errors;
	}

}