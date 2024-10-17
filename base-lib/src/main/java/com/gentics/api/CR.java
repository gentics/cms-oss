package com.gentics.api;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.DatasourceType;
import com.gentics.api.portalnode.connector.HandleType;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.log.NodeLogger;

public class CR {
	/**
	 * use this logger for test-run output, use method .info() only
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(CR.class);

	private final static String DS_ID = "ds";

	private final static String HANDLE_ID = "handle";

	public static void main(String[] args) {
		try {
			System.setProperty("com.gentics.portalnode.portalcache", "false");

			// init CommandLineParser and CommandLine
			CommandLineParser parser = new GnuParser();
			CommandLine line = null;

			try {
				line = parser.parse(createOptions(), args);
			} catch (ParseException e) {
				logger.fatal("Invalid arguments found: " + e.getMessage(), e);
				System.exit(-1);
			}

			// empty line or help message
			if (line == null) {
				printHelpAndExit();
			}
			if (line.hasOption("help")) {
				printHelpAndExit();
			}

			if (!line.hasOption("handle")) {
				System.err.println("Missing option -handle");
				printHelpAndExit();
			}

			Properties handleProps = new Properties();
			try (FileInputStream st = new FileInputStream(line.getOptionValue("handle"))) {
				handleProps.load(st);
			}
			Map<String, String> handlePropsMap = new HashMap<String, String>();
			handlePropsMap.put("type", "jdbc");
			for (Object k : handleProps.keySet()) {
				String key = ObjectTransformer.getString(k, null);
				handlePropsMap.put(key, handleProps.getProperty(key));
			}

			Map<String, String> dsPropsMap = new HashMap<String, String>();
			dsPropsMap.put("cache", "false");
			dsPropsMap.put("sanitycheck", "false");
			dsPropsMap.put("autorepair", "false");
			dsPropsMap.put("sanitycheck2", "false");
			dsPropsMap.put("autorepair2", "false");
			if (line.hasOption("ds")) {
				Properties dsProps = new Properties();
				try (FileInputStream st = new FileInputStream(line.getOptionValue("ds"))) {
					dsProps.load(st);
				}
				for (Object k : dsProps.keySet()) {
					String key = ObjectTransformer.getString(k, null);
					dsPropsMap.put(key, dsProps.getProperty(key));
				}
			}

			boolean mccr = line.hasOption("mccr");

			PortalConnectorFactory.registerHandle(HANDLE_ID, HandleType.sql, handlePropsMap);
			PortalConnectorFactory.registerDatasource(DS_ID, mccr ? DatasourceType.mccr : DatasourceType.contentrepository, dsPropsMap,
					Arrays.asList(HANDLE_ID));

			List<Resolvable> objects = new ArrayList<>();
			Datasource ds = null;
			if (mccr) {
				MultichannellingDatasource mccrDs = PortalConnectorFactory.createDatasource(MultichannellingDatasource.class, DS_ID);
				if (line.hasOption("channel")) {
					mccrDs.setChannel(Integer.parseInt(line.getOptionValue("channel")));
				}
				ds = mccrDs;
			} else {
				ds = PortalConnectorFactory.createDatasource(Datasource.class, DS_ID);
			}

			if (line.hasOption("contentid")) {
				Resolvable object = PortalConnectorFactory.getContentObject(line.getOptionValue("contentid"), ds);
				if (object == null) {
					System.err.println("Could not find object " + line.getOptionValue("contentid"));
				} else {
					objects.add(object);
				}
			} else if (line.hasOption("query")) {
				Expression expr = ExpressionParser.getInstance().parse(line.getOptionValue("query"));
				DatasourceFilter filter = ds.createDatasourceFilter(expr);
				objects.addAll(ds.getResult(filter, null));
			}

			String[] attributes = null;
			if (line.hasOption("out")) {
				String outList = line.getOptionValue("out");
				attributes = StringUtils.split(outList, ",");
			}

			for (Resolvable obj : objects) {
				output(obj, attributes);
			}

			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			printHelpAndExit();
		}
	}

	/**
	 * Generate output for the given object
	 * @param object object
	 * @param attributes optional attributes to output
	 */
	private static void output(Resolvable object, String[] attributes) {
		if (attributes != null) {
			System.out.println("== Object " + object + " " + StringUtils.repeat("=", 89 - object.toString().length()));
			for (String attr : attributes) {
				System.out.println("-- begin " + attr + " " + StringUtils.repeat("-", 90 - attr.length()));
				System.out.println(object.get(attr));
				System.out.println("--  end  " + attr + " " + StringUtils.repeat("-", 90 - attr.length()));
			}
		} else {
			System.out.println(object);
		}
	}

	/**
	 * Private helper method to print the help screen and exit
	 */
	static private void printHelpAndExit() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("CR", createOptions());
		System.exit(0);
	}

	/**
	 * Create cli options
	 * @return cli options
	 */
	static private Options createOptions() {
		Options options = new Options();
		options.addOption("handle", true, "Handle Properties File");
		options.addOption("ds", true, "Datasource Properties file");
		options.addOption("channel", true, "Channel ID for MCCR Datasources");
		options.addOption("help", false, "Help");
		options.addOption("mccr", false, "MCCR Datasource");
		options.addOption("out", true, "Comma separated list of attributes for output");
		options.addOption("contentid", true, "Content ID of the object to load");
		options.addOption("query", true, "Query Expression to load objects");
		return options;
	}
}
