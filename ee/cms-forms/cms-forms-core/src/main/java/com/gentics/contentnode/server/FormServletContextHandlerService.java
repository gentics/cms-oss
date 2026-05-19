package com.gentics.contentnode.server;

import com.gentics.lib.log.NodeLogger;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.ResourceServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;

public class FormServletContextHandlerService implements ServletContextHandlerService {

    /**
     * Logger
     */
    protected static NodeLogger log = NodeLogger.getNodeLogger(FormServletContextHandlerService.class);

    @Override
    public void init(ServletContextHandler context) {
        context.addServlet(getWebrootResourceServlet("formgen"), "/formgen/*");
        ServletHandler servletHandler = context.getServletHandler();
        try {
            addFilter(
                    servletHandler,
                    FormFeatureFilter.FILTER_NAME,
                    FormFeatureFilter.class,
                    "/formgen/**",
                    EnumSet.of(DispatcherType.REQUEST),
                    (holder) -> {}
            );
        } catch (Exception e) {
            log.error("Error while adding Forms filters: " + e.getMessage(), e);
        }
    }

    @Override
    public void onReloadConfiguration() {}

    /**
     * Get the holder for a {@link ResourceServlet} with the baseResource set to the given
     * relative path. Directory listing is forbidden.
     * @param path path relative to /webroot of the classpath
     * @return servlet holder instance
     */
    private static ServletHolder getWebrootResourceServlet(String path) {
        return getResourceServlet(Map.of(
                "baseResource", path,
                "pahtInfoOnly", "true",
                "dirAllowed", "false"));
    }

    /**
     * Get the holder for a {@link ResourceServlet} with the baseResource set to the given path,
     * which is made absolute. Directory listing is forbidden, etags will be generated and cacheControl is set to no-cache
     * @param path path
     * @return servlet holder instance
     */
    private static ServletHolder getStaticFileServletForPath(String path) {
        path = Path.of(path).toAbsolutePath().toString();
        return getResourceServlet(Map.of(
                "baseResource", path,
                "pathInfoOnly", "true",
                "dirAllowed", "false",
                "etags", "true",
                "cacheControl", "no-cache"));
    }

    /**
     * Get the holder for a {@link ResourceServlet} configured with the given init parameters
     * @param initParameters map of init parameters
     * @return servlet holder instance
     */
    private static ServletHolder getResourceServlet(Map<String, String> initParameters) {
        var servletHolder = new ServletHolder(ResourceServlet.class);
        servletHolder.setInitParameters(initParameters);
        return servletHolder;
    }
}
