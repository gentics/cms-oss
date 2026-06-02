package com.gentics.contentnode.etc;

import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validator implementation using Jsoup <a href="https://jsoup.org/">...</a>
 */
public class JsoupValidator implements Validator {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(JsoupValidator.class);

	/**
	 * A validator function that verifies html markup
	 */
	public static final Predicate<String> VALIDATOR = (String html) -> {
		Validator validator = new JsoupValidator();
		var errors = validator.validateAndCollectErrors(html);
		errors.forEach(logger::error);

		return errors.isEmpty();
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