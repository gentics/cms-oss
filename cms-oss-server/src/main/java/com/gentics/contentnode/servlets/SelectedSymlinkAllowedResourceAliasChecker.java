package com.gentics.contentnode.servlets;

import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.AllowedResourceAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * An extension of {@link AllowedResourceAliasChecker} which will allow symlinks alias to arbitrary
 * targets, if the path is in the whitelist.
 */
public class SelectedSymlinkAllowedResourceAliasChecker extends AllowedResourceAliasChecker
{
    private static final Logger LOG = Log.getLogger(SelectedSymlinkAllowedResourceAliasChecker.class);

    private final Set<Pattern> allowedPaths;

    /**
     * @param contextHandler the context handler to use.
     */
    public SelectedSymlinkAllowedResourceAliasChecker(ContextHandler contextHandler, Set<String> allowedPaths) {
        super(contextHandler);
        this.allowedPaths = allowedPaths.stream().map(Pattern::compile).collect(Collectors.toSet());
    }

    @Override
    protected boolean check(String pathInContext, Path path) {
    	return allowedPaths.stream()
    			.filter(pattern -> pattern.matcher(path.toString()).matches())
    			.peek(pattern -> LOG.debug("Symlink {} allowed", path))
    			.findAny().isPresent();
    }
}
