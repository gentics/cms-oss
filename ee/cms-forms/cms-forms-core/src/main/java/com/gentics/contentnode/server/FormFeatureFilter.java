package com.gentics.contentnode.server;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class FormFeatureFilter extends HttpFilter {

    public static final String FILTER_NAME = "Gentics CMS Forms Feature Filter";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        final var featureIsEnabled = NodeConfigRuntimeConfiguration.isFeature(Feature.FORMS);

        if (!featureIsEnabled) {
            res.sendError(400, "Forms Feature not enabled");
            return;
        }

        chain.doFilter(req, res);
    }
}
