package com.gentics.contentnode.tools;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * CmdLine Tool to migrate the configuration into a yml file.
 * Reads the configuration from the given URL, generates the diff from the default configuration and stores the diff into the given file
 */
public class MigrateConfiguration {
	/**
	 * Main method
	 * @param args cmdline arguments
	 */
	public static void main(String[] args) {
		// init CommandLineParser and CommandLine
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		Options options = createOptions();

		try {
			line = parser.parse(createOptions(), args);
		} catch (ParseException e) {
			printHelpAndExit(options);
		}

		if (line == null) {
			printHelpAndExit(options);
		}

		init(line.getOptionValue("config"));
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setDefaultMergeable(true);

		try {
			InputStream defaultConfigStream = NodeConfigRuntimeConfiguration.class.getResourceAsStream("config.yml");
			@SuppressWarnings("unchecked")
			Map<String, Object> defaultConfig = mapper.readValue(defaultConfigStream, Map.class);

			File configFile = new File(line.getOptionValue("out"));

			mapper.writeValue(configFile, diff(NodeConfigRuntimeConfiguration.loadConfiguration(), defaultConfig));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the command line options
	 * 
	 * @return command line options
	 */
	@SuppressWarnings("static-access")
	private static Options createOptions() {
		Options options = new Options();

		options.addOption(OptionBuilder.isRequired().hasArg().withDescription("URL to get configuration").create("config"));
		options.addOption(OptionBuilder.isRequired().hasArg().withDescription("Filename for writing the migrated configuration").create("out"));
		return options;
	}

	/**
	 * Private helper method to print the help screen and exit
	 * 
	 * @param options
	 *            command line options
	 */
	private static void printHelpAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		formatter.printHelp("MigrateConfiguration", options);
		System.exit(0);
	}

	/**
	 * Initialize the configuration
	 * 
	 * @param configUrl
	 *            config URL
	 */
	private static void init(String configUrl) {
		// set test mode (no background jobs started)
		System.setProperty("com.gentics.contentnode.testmode", "true");
		// disable cache
		System.setProperty("com.gentics.portalnode.portalcache", "false");

//		System.setProperty(NodeConfigRuntimeConfiguration.PROPERTY_CN_URL, configUrl);
	}

	/**
	 * Recursively generate the diff between the config and the default config
	 * @param config config
	 * @param defaultConfig default config
	 * @return diff
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> diff(Map<String, Object> config, Map<String, Object> defaultConfig) {
		Map<String, Object> diff = new LinkedHashMap<>();

		for (String key : config.keySet()) {
			if (!defaultConfig.containsKey(key)) {
				diff.put(key, config.get(key));
			} else {
				Object value = config.get(key);
				Object defaultValue = defaultConfig.get(key);

				// values are not completely equal, so inspect
				if (!Objects.deepEquals(value, defaultValue)) {
					if (value instanceof Map) {
						if (defaultValue instanceof Map) {
							Map<String, Object> keyDiff = diff((Map<String, Object>)value, (Map<String, Object>)defaultValue);
							if (!keyDiff.isEmpty()) {
								diff.put(key, keyDiff);
							}
						} else {
							diff.put(key, value);
						}
					} else {
						diff.put(key, value);
					}
				}
			}
		}

		return diff;
	}
}
