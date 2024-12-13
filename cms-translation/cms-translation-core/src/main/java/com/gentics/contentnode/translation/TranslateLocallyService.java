package com.gentics.contentnode.translation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.lib.log.NodeLogger;

/**
 * Translate using {@linkplain https://github.com/XapaJIaMnu/translateLocally}.
 * Should be installed locally beforehand, both the service and language models
 */
public class TranslateLocallyService implements TranslationService {

	protected final NodeLogger logger = NodeLogger.getNodeLogger(getClass());
	protected final Map<String, Map<String, String>> languages = new HashMap<>();

	public TranslateLocallyService() {
		init();
	}

	private void init() {
		try {
			String[] args = new String[] { "/bin/bash", "-c", "translateLocally", "-l" };
			Process proc = new ProcessBuilder(args).start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.indexOf(" To invoke do -m ") > -0) {
					String model = line.substring(line.indexOf(" To invoke do -m ") + 17);
					String[] langs = model.split("-");
					if (langs.length > 1) {
						languages.computeIfAbsent(langs[0], unused -> new HashMap<>()).put(langs[1], model);
					}
				}
			}

			proc.waitFor();
		} catch (Exception e) {
			logger.warn("Cannot initialize translateLocally", e);
			languages.clear();
		}
	}

	@Override
	public String translate(String textToTranslate, String sourceLanguage, String targetLanguage) throws Exception {
		return languages.getOrDefault(sourceLanguage, Collections.emptyMap()).entrySet().stream().filter(lang -> lang.getKey().equalsIgnoreCase(targetLanguage)).findAny()
			.map(model -> {
				try {
					String[] args = new String[] {"/bin/bash", "-c",  "echo", "\"" + textToTranslate.replace("\"", "\\\"") + "\"", "|", "translateLocally", "-m", model.getValue() };
					Process proc = new ProcessBuilder(args).start();
	
					BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					String line = "";
					StringBuilder sb = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					proc.waitFor();
	
					return sb.toString();
				} catch (Exception e) {
					logger.warn("Cannot translateLocally", e);
					return textToTranslate;
				}
			}).orElseGet(() -> {
				logger.warn(String.format("No local translator for %s-%s found. Returning original text.", sourceLanguage, targetLanguage));
				return textToTranslate;
			});
	}
}
