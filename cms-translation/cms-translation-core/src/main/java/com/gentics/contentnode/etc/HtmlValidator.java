package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.w3c.tidy.Tidy;

public class HtmlValidator implements Validator {

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());
	private Tidy parser;
	private final Pattern pattern = Pattern.compile("<(.|\\n)*?>");


	public HtmlValidator() {
		this.parser = new Tidy();
	}


	@Override
	public boolean isValid(String html) throws NodeException {
		if (this.parser == null) {
			var errorMsg = "Error while initializing HTML Parser";
			logger.log(Level.ERROR, "Error while initializing HTML Parser");
			throw new NodeException(errorMsg);
		}

		if (!this.containsHtml(html)) {
			return true;
		}

		var inputStream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
		this.parser.parse(inputStream, new ByteArrayOutputStream());

		return parser.getParseErrors() == 0 && parser.getParseWarnings() == 0;
	}

	private boolean containsHtml(String text) {
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		this.parser.parse(stream, new ByteArrayOutputStream());
		return pattern.matcher(text).find();
	}


}