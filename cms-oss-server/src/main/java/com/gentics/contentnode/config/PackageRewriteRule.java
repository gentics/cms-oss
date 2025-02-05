package com.gentics.contentnode.config;

import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;


/**
 * The PackageRewriteRule handles the mapping from package files to URLs with Regex.
 * This is useful to make static content available from a dev package.
 * Additionally, it avoids that unwanted files in the respective dev packages are served.
 * Example of a mapping is:
 * File Location -> URL: packages/devpackage/files ->  /internal/devpackage/files/info.json
 */
public class PackageRewriteRule extends RewriteRegexRule {

	private Pattern NOT_ALLOWED_PATTERN = Pattern.compile("^/packages/(.*)");

	public PackageRewriteRule(String regex, String replacement) {
		super(regex, replacement);
		setTerminating(true);
	}

	@Override
	public Handler matchAndApply(Handler input)
			throws IOException {
		String target = input.getHttpURI().getPathQuery();

		// match packages to avoid serving restricted files
		var packageMatcher = NOT_ALLOWED_PATTERN.matcher(target);

		if (packageMatcher.matches()) {
			return new Handler(input) {
				@Override
				protected boolean handle(Response response, Callback callback) {
					final var message = "Not allowed to access package resources";
					Response.writeError(this, response, callback, 405, message);
					return true;
				}
			};
		}

		return super.matchAndApply(input);
	}
}
