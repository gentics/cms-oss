/*
 * @author norbert
 * @date 06.10.2006
 * @version $Id: CopyCheck.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.nodecopy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * Helper to check the number of copied objects for a given nodecopy
 * configuration. Needed by the junit test case
 * {@link com.gentics.tests.nodecopy.PlausibilityTest}.
 * TODO: add checks for workflow copy
 */
public class CopyCheck {

	/**
	 * Configuration properties
	 */
	protected Properties properties;

	/**
	 * Database connection
	 */
	protected Connection conn;

	/**
	 * Main method (when the CopyCheck is used as cmdline tool)
	 * @param args cmdline arguments
	 */
	public static void main(String[] args) {
		Options options = createCmdLineOptions();
		CommandLineParser parser = new GnuParser();

		try {
			CommandLine cmd = parser.parse(options, args);

			Properties props = new Properties();
			String[] arguments = cmd.getArgs();

			for (int i = 0; i < arguments.length; i++) {
				String[] parts = arguments[i].split("=");

				props.setProperty(parts[0].trim(), parts[1].trim());
			}

			// print help
			if (cmd.hasOption("h")) {
				printHelpAndExit(options);
			} else {
				CopyCheck copy = new CopyCheck(cmd.getOptionValue("c"), cmd.getOptionValue("url"), cmd.getOptionValue("d"), cmd.getOptionValue("u", null),
						cmd.getOptionValue("p", null), props);

				System.out.print(copy.getCopyEstimation());
			}

			// check for required options
		} catch (MissingOptionException e) {
			// print help
			System.out.println("Not all required options were given: " + e.getLocalizedMessage());
			printHelpAndExit(options);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create an instance of the Structure copy and check for
	 * @param configFilePath path to the config file
	 * @param connectionURL database connection url
	 * @param driverClass database driver class
	 * @param userName username
	 * @param password password
	 * @param properties configuration properties
	 * @throws Exception
	 */
	public CopyCheck(String configFilePath, String connectionURL, String driverClass,
			String userName, String password, Properties properties) throws Exception {
		this.properties = properties;
		// read the configuration file
		conn = establishDBConnection(connectionURL, driverClass, userName, password);
	}

	/**
	 * Get the number of rows that will be copied. The exact format of the
	 * output must match the output of the
	 * {@link com.gentics.lib.cmd.dbcopy.StructureCopy} tool (the junit test
	 * relies on this)
	 * @return estimation for the number of copied rows
	 * @throws SQLException
	 */
	public String getCopyEstimation() throws SQLException {
		String output = new String();
		Map counters = new HashMap();

		// node
		int nodeId = Integer.parseInt(properties.getProperty("node"));

		counters.put("node", new Integer(getCount("select count(*) from node where id = " + nodeId)));
		counters.put("folder", new Integer(getCount("select count(*) from folder where node_id = " + nodeId)));
		counters.put("page",
				new Integer(isCopyPage() ? +getCount("select count(*) from page left join folder on page.folder_id = folder.id where folder.node_id = " + nodeId) : 0));
		counters.put("content",
				new Integer(
				isCopyPage()
						? getCount(
								"select count(distinct content.id) from content left join page on content.id = page.content_id left join folder on page.folder_id = folder.id where folder.node_id = "
										+ nodeId)
										: 0));
		counters.put("contentset",
				new Integer(
				isCopyPage()
						? getCount(
								"select count(distinct contentset.id) from contentset left join page on contentset.id = page.contentset_id left join folder on page.folder_id = folder.id where folder.node_id = "
										+ nodeId)
										: 0));
		// perm
		int permCount = 0;

		if (isCopyPerm()) {
			permCount += getCount(
					"select count(distinct usergroup_id, o_type, o_id) from perm left join folder on perm.o_type in (10001, 10002) and perm.o_id = folder.id where folder.node_id = "
							+ nodeId);
			if (isCopyPage()) {
				permCount += getCount(
						"select count(distinct usergroup_id, o_type, o_id) from perm left join page on perm.o_type = 10007 and perm.o_id = page.id left join folder on page.folder_id = folder.id where perm.o_type = 10007 and folder.node_id = "
								+ nodeId);
			}
			if (isCopyTemplate()) {
				permCount += getCount(
						"select count(distinct usergroup_id, o_type, o_id) from perm left join template on perm.o_type = 10006 and perm.o_id = template.id left join template_folder on template_folder.template_id = template.id left join folder on template_folder.folder_id = folder.id where perm.o_type = 10006 and folder.node_id = "
								+ nodeId);
			}
			if (isCopyFile()) {
				permCount += getCount(
						"select count(distinct usergroup_id, o_type, o_id) from perm left join contentfile on perm.o_type in (10008, 10011) and perm.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where perm.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
			}
		}
		counters.put("perm", new Integer(permCount));

		// construct_node
		counters.put("construct_node", new Integer(getCount("select count(*) from construct_node where node_id = " + nodeId)));

		// node_contentgroup
		counters.put("node_contentgroup", new Integer(getCount("select count(*) from node_contentgroup where node_id = " + nodeId)));

		// template_folder
		counters.put("template_folder",
				new Integer(
				getCount(
						"select count(distinct template_folder.template_id, template_folder.folder_id) from template_folder left join folder on template_folder.folder_id = folder.id where folder.node_id = "
								+ nodeId)));

		// contentfile
		counters.put("contentfile",
				new Integer(
				isCopyFile()
						? getCount(
								"select count(distinct contentfile.id) from contentfile left join folder on contentfile.folder_id = folder.id where folder.node_id = " + nodeId)
								: 0));
		// objtag
		int objtagCount = 0;

		objtagCount += getCount(
				"select count(distinct objtag.id) from objtag left join node on objtag.obj_type = 10001 and objtag.obj_id = node.id where objtag.obj_type = 10001 and node.id = "
						+ nodeId);
		objtagCount += getCount(
				"select count(distinct objtag.id) from objtag left join folder on objtag.obj_type = 10002 and objtag.obj_id = folder.id where objtag.obj_type = 10002 and folder.node_id = "
						+ nodeId);
		if (isCopyTemplate()) {
			objtagCount += getCount(
					"select count(distinct objtag.id) from objtag left join template on objtag.obj_type = 10006 and objtag.obj_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where objtag.obj_type = 10006 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyPage()) {
			objtagCount += getCount(
					"select count(distinct objtag.id) from objtag left join page on objtag.obj_type = 10007 and objtag.obj_id = page.id left join folder on page.folder_id = folder.id where objtag.obj_type = 10007 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyFile()) {
			objtagCount += getCount(
					"select count(distinct objtag.id) from objtag left join contentfile on objtag.obj_type in (10008, 10011) and objtag.obj_id = contentfile.id left join folder on contentfile.folder_id = folder.id where objtag.obj_type in (10008, 10011) and folder.node_id = "
							+ nodeId);
		}
		counters.put("objtag", new Integer(objtagCount));

		// ds_obj
		int ds_objCount = 0;

		ds_objCount += getCount(
				"select count(distinct ds_obj.id) from ds_obj left join objtag on ds_obj.objtag_id = objtag.id left join node on objtag.obj_type = 10001 and objtag.obj_id = node.id where objtag.obj_type = 10001 and node.id = "
						+ nodeId);
		ds_objCount += getCount(
				"select count(distinct ds_obj.id) from ds_obj left join objtag on ds_obj.objtag_id = objtag.id left join folder on objtag.obj_type = 10002 and objtag.obj_id = folder.id where objtag.obj_type = 10002 and folder.node_id = "
						+ nodeId);
		if (isCopyTemplate()) {
			ds_objCount += getCount(
					"select count(distinct ds_obj.id) from ds_obj left join templatetag on ds_obj.templatetag_id = templatetag.id left join template on templatetag.template_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where folder.node_id = "
							+ nodeId);
			ds_objCount += getCount(
					"select count(distinct ds_obj.id) from ds_obj left join objtag on ds_obj.objtag_id = objtag.id left join template on objtag.obj_type = 10006 and objtag.obj_id = template.id left join template_folder on template_folder.template_id = template.id left join folder on template_folder.folder_id = folder.id where objtag.obj_type = 10006 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyPage()) {
			ds_objCount += getCount(
					"select count(distinct ds_obj.id) from ds_obj left join contenttag on ds_obj.contenttag_id = contenttag.id left join content on contenttag.content_id = content.id left join page on content.id = page.content_id left join folder on page.folder_id = folder.id where folder.node_id = "
							+ nodeId);
			ds_objCount += getCount(
					"select count(distinct ds_obj.id) from ds_obj left join objtag on ds_obj.objtag_id = objtag.id left join page on objtag.obj_type = 10007 and objtag.obj_id = page.id left join folder on page.folder_id = folder.id where objtag.obj_type = 10007 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyFile()) {
			ds_objCount += getCount(
					"select count(distinct ds_obj.id) from ds_obj left join objtag on ds_obj.objtag_id = objtag.id left join contentfile on objtag.obj_type in (10008, 10011) and objtag.obj_id = contentfile.id left join folder on contentfile.folder_id = folder.id where objtag.obj_type in (10008, 10011) and folder.node_id = "
							+ nodeId);
		}
		counters.put("ds_obj", new Integer(ds_objCount));

		// ds
		int dsCount = 0;

		dsCount += getCount(
				"select count(distinct ds.id) from ds left join objtag on ds.objtag_id = objtag.id left join node on objtag.obj_type = 10001 and objtag.obj_id = node.id where objtag.obj_type = 10001 and node.id = "
						+ nodeId);
		dsCount += getCount(
				"select count(distinct ds.id) from ds left join objtag on ds.objtag_id = objtag.id left join folder on objtag.obj_type = 10002 and objtag.obj_id = folder.id where objtag.obj_type = 10002 and folder.node_id = "
						+ nodeId);
		if (isCopyTemplate()) {
			dsCount += getCount(
					"select count(distinct ds.id) from ds left join templatetag on ds.templatetag_id = templatetag.id left join template on templatetag.template_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where folder.node_id = "
							+ nodeId);
			dsCount += getCount(
					"select count(distinct ds.id) from ds left join objtag on ds.objtag_id = objtag.id left join template on objtag.obj_type = 10006 and objtag.obj_id = template.id left join template_folder on template_folder.template_id = template.id left join folder on template_folder.folder_id = folder.id where objtag.obj_type = 10006 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyPage()) {
			dsCount += getCount(
					"select count(distinct ds.id) from ds left join contenttag on ds.contenttag_id = contenttag.id left join content on contenttag.content_id = content.id left join page on content.id = page.content_id left join folder on page.folder_id = folder.id where folder.node_id = "
							+ nodeId);
			dsCount += getCount(
					"select count(distinct ds.id) from ds left join objtag on ds.objtag_id = objtag.id left join page on objtag.obj_type = 10007 and objtag.obj_id = page.id left join folder on page.folder_id = folder.id where objtag.obj_type = 10007 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyFile()) {
			dsCount += getCount(
					"select count(distinct ds.id) from ds left join objtag on ds.objtag_id = objtag.id left join contentfile on objtag.obj_type in (10008, 10011) and objtag.obj_id = contentfile.id left join folder on contentfile.folder_id = folder.id where objtag.obj_type in (10008, 10011) and folder.node_id = "
							+ nodeId);
		}
		counters.put("ds", new Integer(dsCount));

		// contenttag
		int contenttagCount = 0;

		if (isCopyPage()) {
			contenttagCount += getCount(
					"select count(distinct contenttag.id) from contenttag left join content on contenttag.content_id = content.id left join page on page.content_id = content.id left join folder on page.folder_id = folder.id where folder.node_id = "
							+ nodeId);
		}
		counters.put("contenttag", new Integer(contenttagCount));

		// value
		int valueCount = 0;

		valueCount += getCount(
				"select count(distinct value.id) from value left join objtag on value.objtag_id = objtag.id left join node on objtag.obj_type = 10001 and objtag.obj_id = node.id where objtag.obj_type = 10001 and node.id = "
						+ nodeId);
		valueCount += getCount(
				"select count(distinct value.id) from value left join objtag on value.objtag_id = objtag.id left join folder on objtag.obj_type = 10002 and objtag.obj_id = folder.id where objtag.obj_type = 10002 and folder.node_id = "
						+ nodeId);
		if (isCopyTemplate()) {
			valueCount += getCount(
					"select count(distinct value.id) from value left join templatetag on value.templatetag_id = templatetag.id left join template on templatetag.template_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where folder.node_id = "
							+ nodeId);
			valueCount += getCount(
					"select count(distinct value.id) from value left join objtag on value.objtag_id = objtag.id left join template on objtag.obj_type = 10006 and objtag.obj_id = template.id left join template_folder on template_folder.template_id = template.id left join folder on template_folder.folder_id = folder.id where objtag.obj_type = 10006 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyPage()) {
			valueCount += getCount(
					"select count(distinct value.id) from value left join contenttag on value.contenttag_id = contenttag.id left join content on contenttag.content_id = content.id left join page on content.id = page.content_id left join folder on page.folder_id = folder.id where folder.node_id = "
							+ nodeId);
			valueCount += getCount(
					"select count(distinct value.id) from value left join objtag on value.objtag_id = objtag.id left join page on objtag.obj_type = 10007 and objtag.obj_id = page.id left join folder on page.folder_id = folder.id where objtag.obj_type = 10007 and folder.node_id = "
							+ nodeId);
		}
		if (isCopyFile()) {
			valueCount += getCount(
					"select count(distinct value.id) from value left join objtag on value.objtag_id = objtag.id left join contentfile on objtag.obj_type in (10008, 10011) and objtag.obj_id = contentfile.id left join folder on contentfile.folder_id = folder.id where objtag.obj_type in (10008, 10011) and folder.node_id = "
							+ nodeId);
		}
		counters.put("value", new Integer(valueCount));

		// workflowlink, triggerevent, 
		int workflowlinkCount = 0;
		int triggereventCount = 0;
		int eventpropCount = 0;
		int eventpropeditableCount = 0;
		int reactionpropCount = 0;
		int reactionpropeditableCount = 0;

		if (isCopyWorkflow()) {
			workflowlinkCount += getCount(
					"select count(distinct workflowlink.o_id) from workflowlink left join node on workflowlink.o_type = 10001 and workflowlink.o_id = node.id where workflowlink.o_type = 10001 and node.id = "
							+ nodeId);
			triggereventCount += getCount(
					"select count(distinct triggerevent.id) from triggerevent left join workflowlink on triggerevent.workflowlink_id = workflowlink.id left join node on workflowlink.o_type = 10001 and workflowlink.o_id = node.id where workflowlink.o_type = 10001 and node.id = "
							+ nodeId);
			eventpropCount += getCount(
					"select count(distinct eventprop.id) from eventprop left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join node on workflowlink.o_type = 10001 and workflowlink.o_id = node.id where workflowlink.o_type = 10001 and node.id = "
							+ nodeId);
			reactionpropCount += getCount(
					"select count(distinct reactionprop.id) from reactionprop left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join node on workflowlink.o_type = 10001 and workflowlink.o_id = node.id where workflowlink.o_type = 10001 and node.id = "
							+ nodeId);
			eventpropeditableCount += getCount(
					"select count(distinct eventpropeditable.id) from eventpropeditable left join eventprop on eventpropeditable.eventprop_id = eventprop.id left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join node on workflowlink.o_type = 10001 and workflowlink.o_id = node.id where workflowlink.o_type = 10001 and node.id = "
							+ nodeId);
			reactionpropeditableCount += getCount(
					"select count(distinct reactionpropeditable.id) from reactionpropeditable left join reactionprop on reactionpropeditable.reactionprop_id = reactionprop.id left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join node on workflowlink.o_type = 10001 and workflowlink.o_id = node.id where workflowlink.o_type = 10001 and node.id = "
							+ nodeId);

			workflowlinkCount += getCount(
					"select count(distinct workflowlink.o_id) from workflowlink left join folder on workflowlink.o_type = 10002 and workflowlink.o_id = folder.id where workflowlink.o_type = 10002 and folder.node_id = "
							+ nodeId);
			triggereventCount += getCount(
					"select count(distinct triggerevent.id) from triggerevent left join workflowlink on triggerevent.workflowlink_id = workflowlink.id left join folder on workflowlink.o_type = 10002 and workflowlink.o_id = folder.id where workflowlink.o_type = 10002 and folder.node_id = "
							+ nodeId);
			eventpropCount += getCount(
					"select count(distinct eventprop.id) from eventprop left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join folder on workflowlink.o_type = 10002 and workflowlink.o_id = folder.id where workflowlink.o_type = 10002 and folder.node_id = "
							+ nodeId);
			reactionpropCount += getCount(
					"select count(distinct reactionprop.id) from reactionprop left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join folder on workflowlink.o_type = 10002 and workflowlink.o_id = folder.id where workflowlink.o_type = 10002 and folder.node_id = "
							+ nodeId);
			eventpropeditableCount += getCount(
					"select count(distinct eventpropeditable.id) from eventpropeditable left join eventprop on eventpropeditable.eventprop_id = eventprop.id left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join folder on workflowlink.o_type = 10002 and workflowlink.o_id = folder.id where workflowlink.o_type = 10002 and folder.node_id = "
							+ nodeId);
			reactionpropeditableCount += getCount(
					"select count(distinct reactionpropeditable.id) from reactionpropeditable left join reactionprop on reactionpropeditable.reactionprop_id = reactionprop.id left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join folder on workflowlink.o_type = 10002 and workflowlink.o_id = folder.id where workflowlink.o_type = 10002 and folder.node_id = "
							+ nodeId);

			if (isCopyTemplate()) {
				workflowlinkCount += getCount(
						"select count(distinct workflowlink.o_id) from workflowlink left join template on workflowlink.o_type = 10006 and workflowlink.o_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where workflowlink.o_type = 10006 and folder.node_id = "
								+ nodeId);
				triggereventCount += getCount(
						"select count(distinct triggerevent.id) from triggerevent left join workflowlink on triggerevent.workflowlink_id = workflowlink.id left join template on workflowlink.o_type = 10006 and workflowlink.o_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where workflowlink.o_type = 10006 and folder.node_id = "
								+ nodeId);
				eventpropCount += getCount(
						"select count(distinct eventprop.id) from eventprop left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join template on workflowlink.o_type = 10006 and workflowlink.o_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where workflowlink.o_type = 10006 and folder.node_id = "
								+ nodeId);
				reactionpropCount += getCount(
						"select count(distinct reactionprop.id) from reactionprop left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join template on workflowlink.o_type = 10006 and workflowlink.o_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where workflowlink.o_type = 10006 and folder.node_id = "
								+ nodeId);
				eventpropeditableCount += getCount(
						"select count(distinct eventpropeditable.id) from eventpropeditable left join eventprop on eventpropeditable.eventprop_id = eventprop.id left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join template on workflowlink.o_type = 10006 and workflowlink.o_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where workflowlink.o_type = 10006 and folder.node_id = "
								+ nodeId);
				reactionpropeditableCount += getCount(
						"select count(distinct reactionpropeditable.id) from reactionpropeditable left join reactionprop on reactionpropeditable.reactionprop_id = reactionprop.id left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join template on workflowlink.o_type = 10006 and workflowlink.o_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where workflowlink.o_type = 10006 and folder.node_id = "
								+ nodeId);
			}
			if (isCopyPage()) {
				workflowlinkCount += getCount(
						"select count(distinct workflowlink.o_id) from workflowlink left join page on workflowlink.o_type = 10007 and workflowlink.o_id = page.id left join folder on page.folder_id = folder.id where workflowlink.o_type = 10007 and folder.node_id = "
								+ nodeId);
				triggereventCount += getCount(
						"select count(distinct triggerevent.id) from triggerevent left join workflowlink on triggerevent.workflowlink_id = workflowlink.id left join page on workflowlink.o_type = 10007 and workflowlink.o_id = page.id left join folder on page.folder_id = folder.id where workflowlink.o_type = 10007 and folder.node_id = "
								+ nodeId);
				eventpropCount += getCount(
						"select count(distinct eventprop.id) from eventprop left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join page on workflowlink.o_type = 10007 and workflowlink.o_id = page.id left join folder on page.folder_id = folder.id where workflowlink.o_type = 10007 and folder.node_id = "
								+ nodeId);
				reactionpropCount += getCount(
						"select count(distinct reactionprop.id) from reactionprop left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join page on workflowlink.o_type = 10007 and workflowlink.o_id = page.id left join folder on page.folder_id = folder.id where workflowlink.o_type = 10007 and folder.node_id = "
								+ nodeId);
				eventpropeditableCount += getCount(
						"select count(distinct eventpropeditable.id) from eventpropeditable left join eventprop on eventpropeditable.eventprop_id = eventprop.id left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join page on workflowlink.o_type = 10007 and workflowlink.o_id = page.id left join folder on page.folder_id = folder.id where workflowlink.o_type = 10007 and folder.node_id = "
								+ nodeId);
				reactionpropeditableCount += getCount(
						"select count(distinct reactionpropeditable.id) from reactionpropeditable left join reactionprop on reactionpropeditable.reactionprop_id = reactionprop.id left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join page on workflowlink.o_type = 10007 and workflowlink.o_id = page.id left join folder on page.folder_id = folder.id where workflowlink.o_type = 10007 and folder.node_id = "
								+ nodeId);
			}
			if (isCopyFile()) {
				workflowlinkCount += getCount(
						"select count(distinct workflowlink.o_id) from workflowlink left join contentfile on workflowlink.o_type in (10008, 10011) and workflowlink.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where workflowlink.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
				triggereventCount += getCount(
						"select count(distinct triggerevent.id) from triggerevent left join workflowlink on triggerevent.workflowlink_id = workflowlink.id left join contentfile on workflowlink.o_type in (10008, 10011) and workflowlink.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where workflowlink.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
				eventpropCount += getCount(
						"select count(distinct eventprop.id) from eventprop left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join contentfile on workflowlink.o_type in (10008, 10011) and workflowlink.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where workflowlink.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
				reactionpropCount += getCount(
						"select count(distinct reactionprop.id) from reactionprop left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join contentfile on workflowlink.o_type in (10008, 10011) and workflowlink.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where workflowlink.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
				eventpropeditableCount += getCount(
						"select count(distinct eventpropeditable.id) from eventpropeditable left join eventprop on eventpropeditable.eventprop_id = eventprop.id left join workflowlink on eventprop.workflowlink_id = workflowlink.id left join contentfile on workflowlink.o_type in (10008, 10011) and workflowlink.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where workflowlink.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
				reactionpropeditableCount += getCount(
						"select count(distinct reactionpropeditable.id) from reactionpropeditable left join reactionprop on reactionpropeditable.reactionprop_id = reactionprop.id left join workflowlink on reactionprop.workflowlink_id = workflowlink.id left join contentfile on workflowlink.o_type in (10008, 10011) and workflowlink.o_id = contentfile.id left join folder on contentfile.folder_id = folder.id where workflowlink.o_type in (10008, 10011) and folder.node_id = "
								+ nodeId);
			}
		}
		counters.put("workflowlink", new Integer(workflowlinkCount));
		counters.put("triggerevent", new Integer(triggereventCount));
		counters.put("eventprop", new Integer(eventpropCount));
		counters.put("eventpropeditable", new Integer(eventpropeditableCount));
		counters.put("reactionprop", new Integer(reactionpropCount));
		counters.put("reactionpropeditable", new Integer(reactionpropeditableCount));

		// template
		int templateCount = 0;

		if (isCopyTemplate()) {
			templateCount += getCount(
					"select count(distinct template.id) from template left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where folder.node_id = "
							+ nodeId);
		}
		counters.put("template", new Integer(templateCount));

		// templategroup
		int templategroupCount = 0;

		if (isCopyTemplate()) {
			templategroupCount += getCount(
					"select count(distinct templategroup.id) from templategroup left join template on templategroup.id = template.templategroup_id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where folder.node_id = "
							+ nodeId);
		}
		counters.put("templategroup", new Integer(templategroupCount));

		// templatetag
		int templatetagCount = 0;

		if (isCopyTemplate()) {
			templatetagCount += getCount(
					"select count(distinct templatetag.id) from templatetag left join template on templatetag.template_id = template.id left join template_folder on template.id = template_folder.template_id left join folder on template_folder.folder_id = folder.id where folder.node_id = "
							+ nodeId);
		}
		counters.put("templatetag", new Integer(templatetagCount));

		// objprop_node
		int objprop_nodeCount = 0;

		objprop_nodeCount += getCount("select count(distinct objprop_id, node_id) from objprop_node where node_id = " + nodeId);
		counters.put("objprop_node", new Integer(objprop_nodeCount));

		List names = new Vector(counters.keySet());

		Collections.sort(names);
		for (Iterator iter = names.iterator(); iter.hasNext();) {
			String name = (String) iter.next();

			output += counters.get(name) + "\t" + name + "\n";
		}

		return output;
	}

	/**
	 * Helper method to get a count for the given sql statement (which must be a
	 * "select count(...) ...")
	 * @param sqlStatement sql count statement
	 * @return count
	 * @throws SQLException
	 */
	protected int getCount(String sqlStatement) throws SQLException {
		Statement st = conn.createStatement();
		ResultSet res = st.executeQuery(sqlStatement);

		try {
			if (res.next()) {
				return res.getInt(1);
			} else {
				System.err.println(sqlStatement + ": no results!");
				return 0;
			}
		} finally {
			res.close();
		}
	}

	/**
	 * Check whether page copy is on
	 * @return true for page copy, false for no page copy
	 */
	protected boolean isCopyPage() {
		return "yes".equals(properties.getProperty("copypage", "no"));
	}

	/**
	 * Check whether file copy is on
	 * @return true for file copy, false for no file copy
	 */
	protected boolean isCopyFile() {
		return "yes".equals(properties.getProperty("copyfile", "no"));
	}

	/**
	 * Check whether template copy is on
	 * @return true for template copy, false for no template copy
	 */
	protected boolean isCopyTemplate() {
		return "yes".equals(properties.getProperty("copytemplate", "no"));
	}

	/**
	 * Check whether permission copy is on
	 * @return true for permission copy, false for no permission copy
	 */
	protected boolean isCopyPerm() {
		return "yes".equals(properties.getProperty("copyperm", "no"));
	}

	/**
	 * Check whether workflow copy is on
	 * @return true for workflow copy, false for no workflow copy
	 */
	protected boolean isCopyWorkflow() {
		return "yes".equals(properties.getProperty("copyworkflow", "no"));
	}

	/**
	 * Create the cmd line options
	 * @return cmd line options
	 */
	protected static Options createCmdLineOptions() {
		Options options = new Options();

		options.addOption(
				OptionBuilder.withLongOpt("config").withArgName("path-to-configfile").hasArg().withDescription("configuration file of the copied tables").isRequired(true).create(
						"c"));
		options.addOption(OptionBuilder.withLongOpt("help").withDescription("shows this message").create("h"));
		options.addOption(OptionBuilder.withLongOpt("url").withArgName("connection-url").hasArg().withDescription("connection url").isRequired(true).create());
		options.addOption(
				OptionBuilder.withLongOpt("driverClass").withArgName("driver class").hasArg().withDescription("jdbc driver class").isRequired(true).create("d"));
		options.addOption(
				OptionBuilder.withLongOpt("username").withArgName("username").hasArg().withDescription("name of the database user").isRequired(true).create("u"));
		options.addOption(OptionBuilder.withLongOpt("password").withArgName("password").hasArg().withDescription("password of the database user").create("p"));
		options.addOption(OptionBuilder.withArgName("property=value").hasArg().withValueSeparator().withDescription("declare properties").create("D"));

		return options;
	}

	/**
	 * Establish the database connection
	 * @param connectionURL connection URL
	 * @param driverClass driver class
	 * @param userName username
	 * @param password password
	 * @return connection
	 * @throws Exception
	 */
	protected Connection establishDBConnection(String connectionURL, String driverClass,
			String userName, String password) throws Exception {
		// first load the driver class
		Class.forName(driverClass);

		// now try to create a connection
		return DriverManager.getConnection(connectionURL, userName, password);
	}

	/**
	 * print the usage message and exit
	 * @param options cmd line options
	 */
	protected static void printHelpAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		formatter.printHelp("java " + CopyCheck.class.getName(), options, true);
		System.exit(0);
	}
}
