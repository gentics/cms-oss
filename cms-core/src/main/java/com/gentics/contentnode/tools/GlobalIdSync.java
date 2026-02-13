package com.gentics.contentnode.tools;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.jaxb.JAXBHelper;

/**
 * Simple sync tool to synchronize the global id's of constructs and object
 * properties between different systems Usage:
 * 
 * <ol>
 * <li>Read the info from the source system:<br/>
 * <code>java -cp cms-server.jar com.gentics.contentnode.tools.GlobalIdSync -read construct,objprop -out info.json</code>
 * </li>
 * <li>Transfer the file info.json to the target system</li>
 * <li>Try to match the objects:<br/>
 * <code>java -cp cms-server.jar com.gentics.contentnode.tools.GlobalIdSync -match info.json -out info_match.json</code>
 * </li>
 * <li>Check and possibly modify the file info_match.json</li>
 * <li>Generate update SQL:<br/>
 * <code>java -cp cms-server.jar com.gentics.contentnode.tools.GlobalIdSync -sql info_match.json -out update.sql</code>
 * </li>
 * <li>Run the SQL Statements found in update.sql on the target system</li>
 * </ol>
 * 
 */
public class GlobalIdSync {

	/**
	 * Factory
	 */
	private static ContentNodeFactory factory;

	private final static String READ_PARAM_NAME = "read";

	private final static String OUT_PARAM_NAME = "out";

	private final static String MATCH_PARAM_NAME = "match";

	private final static String SQL_PARAM_NAME = "sql";

	private final static String EXEC_PARAM_NAME = "exec";

	/**
	 * Name of the "constructs" property
	 */
	private final static String CONSTRUCTS_PROP = "constructs";

	/**
	 * Name of the "objprops" property
	 */
	private final static String OBJTAGDEF_PROP = "objprops";

	/**
	 * Name of the "datasources" property
	 */
	private final static String DATASOURCES_PROP = "datasources";

	/**
	 * Main Program entry point
	 * 
	 * @param args
	 *            arguments
	 */
	public static void main(String[] args) {
		// init CommandLineParser and CommandLine
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		Options options = createOptions();

		try {
			line = parser.parse(createOptions(), args);
		} catch (ParseException e) {
			e.printStackTrace(System.err);
			printHelpAndExit(options);
		}

		if (line == null) {
			printHelpAndExit(options);
		}

		try {
			initFactory();
			startTransaction();

			// read the data for the given types
			if (line.hasOption(READ_PARAM_NAME)) {
				Writer out = null;

				if (line.hasOption(OUT_PARAM_NAME)) {
					out = new FileWriter(new File(line.getOptionValue(OUT_PARAM_NAME)));
				} else {
					out = new PrintWriter(System.out);
				}

				String[] readValues = line.getOptionValues(READ_PARAM_NAME);
				List<ObjectType> types = new Vector<ObjectType>();

				for (String readValue : readValues) {
					ObjectType objectType = ObjectType.valueOf(readValue);

					if (objectType != null && !types.contains(objectType)) {
						types.add(objectType);
					}
				}
				readObjectInfo(out, (ObjectType[]) types.toArray(new ObjectType[types.size()]));
			} else if (line.hasOption(MATCH_PARAM_NAME)) {
				Writer out = null;
				if (line.hasOption(OUT_PARAM_NAME)) {
					out = new FileWriter(new File(line.getOptionValue(OUT_PARAM_NAME)));
				} else {
					out = new PrintWriter(System.out);
				}

				matchObjects(new FileReader(new File(line.getOptionValue(MATCH_PARAM_NAME))), out);
			} else if (line.hasOption(SQL_PARAM_NAME)) {
				PrintWriter out = null;

				if (line.hasOption(OUT_PARAM_NAME)) {
					out = new PrintWriter(new File(line.getOptionValue(OUT_PARAM_NAME)), "UTF8");
				} else {
					out = new PrintWriter(System.out);
				}

				createUpdateSQL(new FileReader(new File(line.getOptionValue(SQL_PARAM_NAME))), out, false);
				out.flush();
				out.close();
			} else if (line.hasOption(EXEC_PARAM_NAME)) {
				PrintWriter out = null;

				if (line.hasOption(OUT_PARAM_NAME)) {
					out = new PrintWriter(new File(line.getOptionValue(OUT_PARAM_NAME)), "UTF8");
				} else {
					out = new PrintWriter(System.out);
				}

				createUpdateSQL(new FileReader(new File(line.getOptionValue(EXEC_PARAM_NAME))), out, true);
				out.flush();
				out.close();
				TransactionManager.getCurrentTransaction().commit();
			}

			System.exit(0);
		} catch (Exception e) {
			Transaction t = TransactionManager.getCurrentTransactionOrNull();
			if (t != null && t.isOpen()) {
				try {
					t.rollback();
				} catch (TransactionException e1) {
				}
			}
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	/**
	 * Get info for the given objecttypes and output into the given Writer
	 * 
	 * @param out
	 *            where to output the result
	 * @param objectTypes
	 *            list of object types
	 */
	public static void readObjectInfo(Writer out, ObjectType... objectTypes) throws Exception {
		JsonGenerator jg = new JsonFactory().createJsonGenerator(out);
		jg.useDefaultPrettyPrinter();

		ObjectMapper mapper = new ObjectMapper();

		ObjectNode root = mapper.createObjectNode();

		for (ObjectType objectType : objectTypes) {
			switch (objectType) {
			case construct:
				ArrayNode constructNodes = mapper.createArrayNode();
				List<Construct> constructs = getObjects(Construct.class, "SELECT id FROM construct");

				for (Construct construct : constructs) {
					constructNodes.add(getConstruct(construct, mapper));
				}
				root.put(CONSTRUCTS_PROP, constructNodes);
				break;

			case objprop:
				ArrayNode objPropNodes = mapper.createArrayNode();
				List<ObjectTagDefinition> objTagDefs = getObjects(ObjectTagDefinition.class, "SELECT id FROM objtag WHERE obj_id = 0");

				for (ObjectTagDefinition objTagDef : objTagDefs) {
					objPropNodes.add(getObjTagDef(objTagDef, mapper));
				}
				root.put(OBJTAGDEF_PROP, objPropNodes);
				break;
			case datasource:
				ArrayNode datasourceNodes = mapper.createArrayNode();
				List<Datasource> datasources = getObjects(Datasource.class, "SELECT id FROM datasource WHERE name is not null");

				for (Datasource datasource : datasources) {
					datasourceNodes.add(getDatasource(datasource, mapper));
				}
				root.put(DATASOURCES_PROP, datasourceNodes);
				break;
			}
		}

		mapper.writeValue(jg, root);
	}

	/**
	 * Match objects contained in the reader and write matching info into the
	 * writer
	 * 
	 * @param reader
	 *            reader for reading the object information
	 * @param writer
	 *            to write matching information
	 * @throws Exception
	 */
	public static void matchObjects(Reader reader, Writer writer) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ObjectMapper mapper = new ObjectMapper();
		JsonParser jsonParser = new JsonFactory().createJsonParser(reader);

		jsonParser.setCodec(mapper);

		JsonNode root = jsonParser.readValueAsTree();

		JsonNode constructs = root.get(CONSTRUCTS_PROP);
		if (constructs != null && !(constructs instanceof NullNode)) {
			for (Iterator<JsonNode> c = constructs.elements(); c.hasNext();) {
				final JsonNode constructNode = c.next();

				// first try to find the construct with given global id
				Construct construct = t.getObject(Construct.class, new NodeObject.GlobalId(constructNode.get("globalid").asText()));

				if (construct != null) {
					// we found the construct, so we are done
					c.remove();
				} else {
					final List<Integer> ids = new Vector<Integer>();

					// try to find the construct by keyword
					DBUtils.executeStatement("SELECT id FROM construct WHERE keyword = ?", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setString(1, constructNode.get("keyword").asText());
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								ids.add(rs.getInt("id"));
							}
						}
					});

					if (!ids.isEmpty()) {
						// found a construct with same keyword
						construct = t.getObject(Construct.class, ids.get(0));
						if (construct != null) {
							// match the given construct
							matchConstruct(constructNode, construct);
						}
					} else {
						c.remove();
					}
				}
			}
		}

		JsonNode objTagDefs = root.get(OBJTAGDEF_PROP);
		if (objTagDefs != null && !(objTagDefs instanceof NullNode)) {
			for (Iterator<JsonNode> o = objTagDefs.elements(); o.hasNext();) {
				final JsonNode objTagDefNode = o.next();

				// first try to find the objtag def with given global id
				ObjectTagDefinition objTagDef = t.getObject(ObjectTagDefinition.class, new GlobalId(objTagDefNode.get("globalid").asText()));

				if (objTagDef != null) {
					// we found the objtag def, so we are done
					o.remove();
				} else {
					final List<Integer> ids = new Vector<Integer>();

					// try to find the objtag def by keyword
					DBUtils.executeStatement("SELECT id FROM objtag WHERE name = ? AND obj_type = ? AND obj_id = 0", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setString(1, getString(objTagDefNode, "name"));
							stmt.setInt(2, getInt(objTagDefNode, "type"));
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								ids.add(rs.getInt("id"));
							}
						}
					});

					if (!ids.isEmpty()) {
						// found a objprop with same type and name
						objTagDef = t.getObject(ObjectTagDefinition.class, ids.get(0));
						if (objTagDef != null) {
							// match
							matchObjTagDef(objTagDefNode, objTagDef);
						} else {
							o.remove();
						}
					} else {
						o.remove();
					}
				}
			}
		}

		JsonNode datasources = root.get(DATASOURCES_PROP);
		if (datasources != null && !(datasources instanceof NullNode)) {
			for (Iterator<JsonNode> ds = datasources.elements(); ds.hasNext();) {
				final JsonNode dsNode = ds.next();

				// first try to find the datasource with given global id
				Datasource datasource = t.getObject(Datasource.class, new GlobalId(dsNode.get("globalid").asText()));

				if (datasource != null) {
					// we found the datasource, so we are done
					if (!matchDatasource(dsNode, datasource)) {
						ds.remove();
					}
				} else {
					final List<Integer> ids = new Vector<Integer>();

					// try to find the datasource by name
					DBUtils.executeStatement("SELECT id FROM datasource WHERE name = ?", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setString(1, getString(dsNode, "name"));
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								ids.add(rs.getInt("id"));
							}
						}
					});

					if (!ids.isEmpty()) {
						// found a datasource with same name
						datasource = t.getObject(Datasource.class, ids.get(0));
						if (datasource != null) {
							// match
							matchDatasource(dsNode, datasource);
						} else {
							ds.remove();
						}
					} else {
						ds.remove();
					}
				}
			}
		}

		JsonGenerator jg = new JsonFactory().createJsonGenerator(writer);

		jg.useDefaultPrettyPrinter();
		mapper.writeValue(jg, root);
	}

	/**
	 * Create the sql statements for updating the objects
	 * 
	 * @param in
	 *            reader to read the matching information
	 * @param out
	 *            for writing the sql statements
	 * @param execute
	 *            true if the statements shall be executed, false if not
	 * @throws Exception
	 */
	public static void createUpdateSQL(Reader in, PrintWriter out, boolean execute) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonParser jsonParser = new JsonFactory().createJsonParser(in);

		jsonParser.setCodec(mapper);

		JsonNode root = jsonParser.readValueAsTree();

		JsonNode constructs = root.get(CONSTRUCTS_PROP);
		if (constructs != null && !(constructs instanceof NullNode)) {
			for (Iterator<JsonNode> c = constructs.elements(); c.hasNext();) {
				final JsonNode constructNode = c.next();

				createUpdateSQL(constructNode, "construct", out, execute);
				if (constructNode.has("parts")) {
					for (Iterator<JsonNode> p = constructNode.get("parts").elements(); p.hasNext();) {
						JsonNode partNode = p.next();

						createUpdateSQL(partNode, "part", out, execute);
						if (partNode.has("value")) {
							createUpdateSQL(partNode.get("value"), "value", out, execute);
						}
					}
				}
			}
		}

		JsonNode objProps = root.get(OBJTAGDEF_PROP);
		if (objProps != null && !(objProps instanceof NullNode)) {
			for (Iterator<JsonNode> o = objProps.elements(); o.hasNext();) {
				final JsonNode objPropNode = o.next();

				createUpdateSQL(objPropNode, "objtag", out, execute);

				if (objPropNode.has("prop")) {
					createUpdateSQL(objPropNode.get("prop"), "objprop", out, execute);
				}
			}
		}

		JsonNode datasources = root.get(DATASOURCES_PROP);
		if (datasources != null && !(datasources instanceof NullNode)) {
			for (Iterator<JsonNode> o = datasources.elements(); o.hasNext();) {
				final JsonNode datasourceNode = o.next();

				createUpdateSQL(datasourceNode, "datasource", out, execute);

				if (datasourceNode.has("entries")) {
					for (Iterator<JsonNode> p = datasourceNode.get("entries").elements(); p.hasNext();) {
						JsonNode entryNode = p.next();

						createUpdateSQL(entryNode, "datasource_value", out, execute);
					}
				}
			}
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

		options.addOption(OptionBuilder.withDescription("Read the object info for the given object types").hasArgs().withValueSeparator(',').create(READ_PARAM_NAME));
		options.addOption(OptionBuilder.hasArg().withDescription("Filename for writing object information (use in combination with -read)").create(OUT_PARAM_NAME));
		options.addOption(OptionBuilder.withDescription("Match the object info read from the file").hasArg().create(MATCH_PARAM_NAME));
		options.addOption(OptionBuilder.withDescription("Create SQL Statements for updating the globalids").hasArg().create(SQL_PARAM_NAME));
		options.addOption(OptionBuilder.withDescription("Create and execute SQL Statements for updating the globalids").hasArg().create(EXEC_PARAM_NAME));
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

		formatter.printHelp("GlobalIdSync", options);
		System.exit(0);
	}

	/**
	 * Start a transaction
	 * 
	 * @throws NodeException
	 */
	private static void startTransaction() throws NodeException {
		factory.startTransaction(null, 1, true);
		ContentNodeHelper.setLanguageId(1);
	}

	/**
	 * Initialize the factory
	 * 
	 * @throws Exception
	 */
	private static void initFactory() throws Exception {
		// set test mode (no background jobs started)
		System.setProperty("com.gentics.contentnode.testmode", "true");
		// disable cache
		System.setProperty("com.gentics.portalnode.portalcache", "false");

		// this will initialize the configuration
		NodeConfigRuntimeConfiguration.getDefault();

		factory = ContentNodeFactory.getInstance();

		JAXBHelper.init(null);
	}

	/**
	 * Get the list of objects of given clazz fetched by the given sql statement
	 * 
	 * @param clazz
	 *            class
	 * @param sql
	 *            sql statement, must select the id
	 * @return list of objects
	 * @throws NodeException
	 */
	private static <T extends NodeObject> List<T> getObjects(Class<T> clazz, String sql) throws NodeException {
		return getObjects(clazz, sql, "id");
	}

	/**
	 * Get the list of objects of given clazz fetched by the given sql statement
	 * 
	 * @param clazz
	 *            class
	 * @param sql
	 *            sql statement
	 * @param idCol
	 *            id column
	 * @param params
	 *            optional parameters to be used in the statement
	 * @return list of objects
	 * @throws NodeException
	 */
	private static <T extends NodeObject> List<T> getObjects(Class<T> clazz, String sql, final String idCol, final Object... params) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final List<Integer> objIds = new Vector<Integer>();

		DBUtils.executeStatement(sql, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int paramCounter = 1;
				for (Object param : params) {
					stmt.setObject(paramCounter++, param);
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					objIds.add(rs.getInt(idCol));
				}
			}
		});
		return t.getObjects(clazz, objIds);
	}

	/**
	 * Match the object tag definition
	 * 
	 * @param objTagDefNode
	 *            json object
	 * @param objTagDef
	 *            node object
	 * @throws Exception
	 */
	private static void matchObjTagDef(JsonNode objTagDefNode, ObjectTagDefinition objTagDef) throws Exception {
		if (objTagDefNode instanceof ObjectNode) {
			ObjectNode oNode = (ObjectNode) objTagDefNode;

			oNode.put("replaces", ObjectTransformer.getString(objTagDef.getGlobalId(), null));
			ValueList values = objTagDef.getObjectTag().getValues();
			List<Value> valuesList = new Vector<Value>(values);
			JsonNode valuesNode = oNode.get("values");

			if (valuesNode instanceof ArrayNode) {
				matchNodeObjects((ArrayNode) valuesNode, valuesList, new Matcher<Value>() {

					public boolean matches(JsonNode node, Value value) throws Exception {
						return StringUtils.isEqual(getString(node, "keyword"), value.getPart().getKeyname());
					}

					public void matchSubElements(JsonNode node, Value value) throws Exception {
					}
				});
			}

			matchGlobalId(oNode.get("prop"), GlobalId.getGlobalId("objprop", objTagDef.getObjectPropId()));
		}
	}

	/**
	 * Match the construct
	 * 
	 * @param constructNode
	 *            json object
	 * @param construct
	 *            node object
	 * @throws Exception
	 */
	private static void matchConstruct(JsonNode constructNode, Construct construct) throws Exception {
		if (constructNode instanceof ObjectNode) {
			ObjectNode oNode = (ObjectNode) constructNode;

			oNode.put("replaces", ObjectTransformer.getString(construct.getGlobalId(), null));
			List<Part> parts = construct.getParts();
			JsonNode partsNode = oNode.get("parts");

			if (partsNode instanceof ArrayNode) {
				matchNodeObjects((ArrayNode) partsNode, parts, new Matcher<Part>() {
					public boolean matches(JsonNode node, Part part) throws Exception {
						return StringUtils.isEqual(part.getKeyname(), getString(node, "keyword"));
					}

					public void matchSubElements(JsonNode node, Part part) throws Exception {
						matchNodeObject(node.get("value"), part.getDefaultValue());
					}
				});
			}
		}
	}

	/**
	 * Match the datasource
	 * 
	 * @param dsNode
	 *            json object
	 * @param datasource
	 *            node object
	 * @return true if datasource needs to be replaced, false if not
	 * @throws Exception
	 */
	private static boolean matchDatasource(JsonNode dsNode, Datasource datasource) throws Exception {
		boolean diffFound = false;
		if (dsNode instanceof ObjectNode) {
			ObjectNode oNode = (ObjectNode) dsNode;
			String newGlobalId = ObjectTransformer.getString(datasource.getGlobalId(), null);

			if (!StringUtils.isEqual(dsNode.get("globalid").asText(), newGlobalId)) {
				oNode.put("replaces", newGlobalId);
				diffFound = true;
			}
			List<DatasourceEntry> entries = datasource.getEntries();
			JsonNode entriesNode = oNode.get("entries");
			if (entriesNode instanceof ArrayNode) {
				diffFound |= matchNodeObjects((ArrayNode) entriesNode, entries, new Matcher<DatasourceEntry>() {
					@Override
					public boolean matches(JsonNode node, DatasourceEntry entry) throws Exception {
						return StringUtils.isEqual(entry.getKey(), getString(node, "key"));
					}

					@Override
					public void matchSubElements(JsonNode node, DatasourceEntry entry) throws Exception {
					}
				});
			}
		}
		return diffFound;
	}

	/**
	 * Match the given list of json objects and node objects
	 * 
	 * @param arrayNode
	 *            json objects
	 * @param nodeObjects
	 *            node objects
	 * @param properties
	 *            properties to match
	 * @return true iff a diff was found
	 * @throws Exception
	 */
	private static <T extends NodeObject> boolean matchNodeObjects(ArrayNode arrayNode, Collection<T> nodeObjects, Matcher<T> matcher) throws Exception {
		boolean diffFound = false;
		for (Iterator<JsonNode> iter = arrayNode.iterator(); iter.hasNext();) {
			JsonNode jsonNode = iter.next();

			if (!(jsonNode instanceof ObjectNode)) {
				continue;
			}
			ObjectNode oNode = (ObjectNode) jsonNode;
			String globalId = getString(jsonNode, "globalid");

			// first try to find object by globalId
			if (nodeObjects.stream().filter(o -> StringUtils.isEqual(globalId, ObjectTransformer.getString(o.getGlobalId(), null))).findFirst().isPresent()) {
				iter.remove();
				continue;
			}

			for (T nodeObject : nodeObjects) {
				if (matcher.matches(jsonNode, nodeObject)) {
					if (StringUtils.isEqual(getString(jsonNode, "globalid"), ObjectTransformer.getString(nodeObject.getGlobalId(), null))) {
						iter.remove();
						break;
					} else {
						diffFound = true;
						oNode.put("replaces", ObjectTransformer.getString(nodeObject.getGlobalId(), null));
						matcher.matchSubElements(oNode, nodeObject);
						break;
					}
				}
			}
		}
		return diffFound;
	}

	/**
	 * Matches the given json node with the node object
	 * 
	 * @param jsonNode
	 *            json node
	 * @param nodeObject
	 *            node object
	 * @throws Exception
	 */
	private static void matchNodeObject(JsonNode jsonNode, NodeObject nodeObject) throws Exception {
		if (nodeObject != null && jsonNode != null && !(jsonNode instanceof NullNode)) {
			matchGlobalId(jsonNode, nodeObject.getGlobalId());
		}
	}

	/**
	 * Matches the given json node with the globalid
	 * 
	 * @param jsonNode
	 * @param globalId
	 * @throws Exception
	 */
	private static void matchGlobalId(JsonNode jsonNode, GlobalId globalId) throws Exception {
		if (globalId != null && jsonNode != null && !(jsonNode instanceof NullNode)) {
			if (!StringUtils.isEqual(jsonNode.get("globalid").asText(), ObjectTransformer.getString(globalId, null))) {
				((ObjectNode) jsonNode).put("replaces", ObjectTransformer.getString(globalId, null));
			}
		}
	}

	/**
	 * Transform the given objtag definition into a json node
	 * 
	 * @param objTagDef
	 *            objtag definition
	 * @param mapper
	 *            object mapper
	 * @return json node
	 * @throws Exception
	 */
	private static JsonNode getObjTagDef(ObjectTagDefinition objTagDef, ObjectMapper mapper) throws Exception {
		ObjectTag objectTag = objTagDef.getObjectTag();
		ObjectNode objectNode = mapper.createObjectNode();

		objectNode.put("globalid", objectTag.getGlobalId().toString());
		objectNode.put("name", objectTag.getName());
		objectNode.put("type", objectTag.getObjType());
		objectNode.put("descriptivename", ObjectTransformer.getString(objTagDef.getName(), null));

		ObjectNode propNode = mapper.createObjectNode();

		propNode.put("globalid", GlobalId.getGlobalId("objprop", objTagDef.getObjectPropId()).toString());
		objectNode.put("prop", propNode);

		ValueList values = objectTag.getValues();

		if (!values.isEmpty()) {
			ArrayNode valueNodes = mapper.createArrayNode();

			for (Value value : values) {
				valueNodes.add(getValue(value, mapper));
			}
			objectNode.put("values", valueNodes);
		}

		return objectNode;
	}

	/**
	 * Transform the given construct into a json node
	 * 
	 * @param construct
	 *            construct
	 * @param mapper
	 *            object mapper
	 * @return json node
	 * @throws Exception
	 */
	private static JsonNode getConstruct(Construct construct, ObjectMapper mapper) throws Exception {
		ObjectNode objectNode = mapper.createObjectNode();

		objectNode.put("globalid", construct.getGlobalId().toString());
		objectNode.put("keyword", construct.getKeyword());
		objectNode.put("name", ObjectTransformer.getString(construct.getName(), null));

		List<Part> parts = construct.getParts();

		if (!ObjectTransformer.isEmpty(parts)) {
			ArrayNode partNodes = mapper.createArrayNode();

			for (Part part : parts) {
				partNodes.add(getPart(part, mapper));
			}
			objectNode.put("parts", partNodes);
		}

		return objectNode;
	}

	/**
	 * Transform the given part into a json node
	 * 
	 * @param part
	 *            part
	 * @param mapper
	 *            object mapper
	 * @return json node
	 * @throws Exception
	 */
	private static JsonNode getPart(Part part, ObjectMapper mapper) throws Exception {
		ObjectNode objectNode = mapper.createObjectNode();

		objectNode.put("globalid", part.getGlobalId().toString());
		objectNode.put("keyword", part.getKeyname());
		objectNode.put("name", ObjectTransformer.getString(part.getName(), null));
		objectNode.put("value", getValue(part.getDefaultValue(), mapper));
		return objectNode;
	}

	/**
	 * Transform the given value into a json node
	 * 
	 * @param value
	 *            value
	 * @param mapper
	 *            object mapper
	 * @return json node
	 * @throws Exception
	 */
	private static JsonNode getValue(Value value, ObjectMapper mapper) throws Exception {
		if (value != null && value.getGlobalId() != null) {
			ObjectNode objectNode = mapper.createObjectNode();

			objectNode.put("globalid", value.getGlobalId().toString());
			objectNode.put("keyword", value.getPart().getKeyname());
			return objectNode;
		} else {
			return null;
		}
	}

	/**
	 * Transform the given datasource
	 * 
	 * @param datasource
	 *            datasource
	 * @param mapper
	 *            object mapper
	 * @return json node
	 * @throws Exception
	 */
	private static JsonNode getDatasource(Datasource datasource, ObjectMapper mapper) throws Exception {
		if (datasource != null && datasource.getGlobalId() != null) {
			ObjectNode datasourceNode = mapper.createObjectNode();

			datasourceNode.put("globalid", datasource.getGlobalId().toString());
			datasourceNode.put("name", datasource.getName());

			List<DatasourceEntry> entries = datasource.getEntries();

			if (!ObjectTransformer.isEmpty(entries)) {
				ArrayNode entryNodes = mapper.createArrayNode();

				for (DatasourceEntry entry : entries) {
					entryNodes.add(getDatasourceEntry(entry, mapper));
				}
				datasourceNode.put("entries", entryNodes);
			}

			return datasourceNode;
		} else {
			return null;
		}
	}

	/**
	 * Transform the given datasource entry
	 * 
	 * @param entry
	 *            datasource entry
	 * @param mapper
	 *            object mapper
	 * @return json node
	 * @throws Exception
	 */
	private static JsonNode getDatasourceEntry(DatasourceEntry entry, ObjectMapper mapper) throws Exception {
		if (entry != null && entry.getGlobalId() != null) {
			ObjectNode entryNode = mapper.createObjectNode();

			entryNode.put("globalid", entry.getGlobalId().toString());
			entryNode.put("key", entry.getKey());
			return entryNode;
		} else {
			return null;
		}
	}

	/**
	 * Create the update SQL Statement for the given node
	 * 
	 * @param node
	 *            node
	 * @param tableName
	 *            name of the table to which the globalid belongs
	 * @param out
	 *            writer where to print the statements
	 * @param execute
	 *            true to execute
	 * @throws Exception
	 */
	private static void createUpdateSQL(JsonNode node, String tableName, PrintWriter out, boolean execute) throws Exception {
		String globalIdString = getString(node, "globalid");
		String replacesIdString = getString(node, "replaces");

		if (!ObjectTransformer.isEmpty(globalIdString) && !ObjectTransformer.isEmpty(replacesIdString)
				&& !StringUtils.isEqual(globalIdString, replacesIdString)) {
			NodeObject.GlobalId globalId = new NodeObject.GlobalId(globalIdString);
			NodeObject.GlobalId replaceId = new NodeObject.GlobalId(replacesIdString);

			String updateSQL = "UPDATE " + tableName + " SET uuid = '" + globalId.toString() + "' WHERE uuid = '" + replaceId.toString() + "'";
			out.print(updateSQL);
			out.println(";");

			if (execute) {
				DBUtils.executeUpdate(updateSQL, null);
			}
		}
	}

	/**
	 * Get the given property from the json node as string
	 * 
	 * @param node
	 *            json node
	 * @param property
	 *            property name
	 * @return property as string or null
	 */
	private static String getString(JsonNode node, String property) {
		if (node.has(property)) {
			JsonNode propNode = node.get(property);

			if (propNode instanceof NullNode) {
				return null;
			} else {
				return propNode.asText();
			}
		} else {
			return null;
		}
	}

	/**
	 * Get the given property from the json node as int
	 * 
	 * @param node
	 *            json node
	 * @param property
	 *            property name
	 * @return property as int or -1
	 */
	private static int getInt(JsonNode node, String property) {
		if (node.has(property)) {
			JsonNode propNode = node.get(property);

			if (propNode instanceof NullNode) {
				return -1;
			} else {
				return propNode.asInt();
			}
		} else {
			return -1;
		}
	}

	/**
	 * Possible object types
	 */
	public static enum ObjectType {
		construct, objprop, datasource
	}

	/**
	 * Interface for a matcher
	 * 
	 * @param <T>
	 *            NodeObject class
	 */
	private static interface Matcher<T extends NodeObject> {

		/**
		 * Check whether the given json node matches the object
		 * 
		 * @param node
		 *            json node
		 * @param object
		 *            object
		 * @return true if the objects match, false if not
		 * @throws Exception
		 */
		boolean matches(JsonNode node, T object) throws Exception;

		/**
		 * Match subelements
		 * 
		 * @param node
		 *            json node
		 * @param object
		 *            object
		 * @throws Exception
		 */
		void matchSubElements(JsonNode node, T object) throws Exception;
	}
}
