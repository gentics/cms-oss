package com.gentics.contentnode.etc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;

/**
 * Provider for the paths to FormGen CSS and JS Files
 */
public class FormGenProvider implements ConfigurationService {
	public final static String FORMGEN_CSS_PATH = "_expo/static/css";

	public final static String FORMGEN_JS_PATH = "_expo/static/js/web";

	public final static String WEBROOT_PATH = "webroot/formgen/";

	public final static String WEBROOT_CSS_PATH = WEBROOT_PATH + FORMGEN_CSS_PATH;

	public final static String WEBROOT_JS_PATH = WEBROOT_PATH + FORMGEN_JS_PATH;

	public final static String CSS_EXTENSION = ".css";

	public final static String JS_EXTENSION = ".js";

	public final static String PATH_PATTERN = "/formgen/%s/%s";

	protected static NodeLogger logger = NodeLogger.getNodeLogger(FormGenProvider.class);

	protected static List<String> cssPaths = new ArrayList<>();

	protected static List<String> jsPaths = new ArrayList<>();

	/**
	 * Get the CSS Paths
	 * @return CSS Paths
	 */
	public static List<String> getCSSPaths() {
		return cssPaths;
	}

	/**
	 * JS Paths
	 * @return JS Paths
	 */
	public static List<String> getJSPaths() {
		return jsPaths;
	}

	@Override
	public List<Message> init(NodePreferences preferences) {
		String fsPath = ConfigurationValue.FORMGEN_PATH.get();

		if (StringUtils.isBlank(fsPath)) {
			cssPaths.clear();
			jsPaths.clear();
			URL location = getClass().getProtectionDomain().getCodeSource().getLocation();

			if (Strings.CI.equals(location.getProtocol(), "file")) {
				try (JarFile jar = new JarFile(location.getFile())) {
					jar.stream().map(JarEntry::getName).forEach(name -> {
						File f = new File(name);

						if (Strings.CI.startsWith(name, WEBROOT_CSS_PATH) && Strings.CI.endsWith(name, CSS_EXTENSION)) {
							cssPaths.add(PATH_PATTERN.formatted(FORMGEN_CSS_PATH, f.getName()));
						} else if (Strings.CI.startsWith(name, WEBROOT_JS_PATH) && Strings.CI.endsWith(name, JS_EXTENSION)) {
							jsPaths.add(PATH_PATTERN.formatted(FORMGEN_JS_PATH, f.getName()));
						}
					});
				} catch (IOException e) {
					logger.error("Error while reading jar file", e);
				}
				location.getFile();
			}
		} else {
			File cssDirectory = new File(fsPath, FORMGEN_CSS_PATH);
			if (cssDirectory.isDirectory()) {
				for (final File fileEntry : cssDirectory.listFiles()) {
					if (fileEntry.getName().endsWith(CSS_EXTENSION)) {
						cssPaths.add(PATH_PATTERN.formatted(FORMGEN_CSS_PATH, fileEntry.getName()));
					}
				}
			}

			File jsDirectory = new File(fsPath, FORMGEN_JS_PATH);
			if (jsDirectory.isDirectory()) {
				for (final File fileEntry : jsDirectory.listFiles()) {
					if (fileEntry.getName().endsWith(JS_EXTENSION)) {
						jsPaths.add(PATH_PATTERN.formatted(FORMGEN_JS_PATH, fileEntry.getName()));
					}
				}
			}
		}

		return Collections.emptyList();
	}

	@Override
	public List<Message> check() {
		return Collections.emptyList();
	}
}
