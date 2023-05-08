package com.gentics.contentnode.config;

import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Request;


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
		setHandling(false);
	}

	@Override
	public String matchAndApply(String target, HttpServletRequest request,
			HttpServletResponse response)
			throws IOException {
		// match packages to avoid serving restricted files
		var packageMatcher = NOT_ALLOWED_PATTERN.matcher(target);

		if (packageMatcher.matches()) {
			final var REASON = "Not allowed to access package resources";
			Request.getBaseRequest(request).getResponse().setStatusWithReason(405, REASON);
			response.sendError(405, REASON);
			response.getWriter().print(REASON);

			return target;
		}

		return super.matchAndApply(target, request, response);
	}

}
