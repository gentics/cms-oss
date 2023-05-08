/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: StructureCopy.java,v 1.16 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.lib.db.Connector;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.PoolConnection;

/**
 * Main cmdline tool to copy a database structure.
 */
public class StructureCopy {

	/**
	 * configured properties
	 */
	protected Properties properties;

	/**
	 * database connection (if the connection is handled by StructureCopy)
	 */
	protected Connection conn;

	/**
	 * Transaction (if StructureCopy is run within a transaction, that handles the connection)
	 */
	protected Transaction t;

	/**
	 * configured tables
	 */
	protected Tables tables;

	/**
	 * constant for maximum recursion depth
	 */
	protected final static int MAX_DEPTH = 100;

	/**
	 * pattern to find properties
	 */
	protected static Pattern findProperties = Pattern.compile("\\$\\{([a-zA-Z0-9\\._]+)\\}");

	/**
	 * old setting for autocommit
	 */
	protected boolean oldAutocommit;

	/**
	 * constant for the jaxb context
	 */
	public final static String JAXB_CONTEXT = "com.gentics.contentnode.dbcopy.jaxb";

	/**
	 * copy controller instance
	 */
	protected CopyController copyController;

	/**
	 * dbhandle for the copy process
	 */
	protected DBHandle dbHandle;

	/**
	 * Main method, interprets the given command arguments and starts the copy
	 * @param args cmd line arguments
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
				// create the copy-controller
				String copyControllerClassName = cmd.getOptionValue("o");
				Object copyController = Class.forName(copyControllerClassName).newInstance();

				if (!(copyController instanceof CopyController)) {
					throw new InstantiationException(
							"Configured copy-controller {" + copyControllerClassName + "} must implement {" + CopyController.class.getName() + "}");
				}

				StructureCopy copy = new StructureCopy(cmd.getOptionValue("c"), (CopyController) copyController, cmd.getOptionValue("url"),
						cmd.getOptionValue("d"), cmd.getOptionValue("u", null), cmd.getOptionValue("p", null), props);

				try {
					long start = System.currentTimeMillis();

					copy.startCopy();
					Map<ObjectKey, DBObject> objectStructure = copy.getObjectStructure(cmd.hasOption("v"));

					if (cmd.hasOption("m")) {
						printMemoryDebug();
					}
					if (cmd.hasOption("preview")) {
						System.out.print(copy.getObjectStats(objectStructure));
					} else {
						copy.copyStructure(objectStructure, cmd.hasOption("v"));
					}
					if (cmd.hasOption("m")) {
						printMemoryDebug();
					}
					copy.finishCopy();
					long duration = System.currentTimeMillis() - start;

					if (cmd.hasOption("v")) {
						System.out.println(
								"Copied " + objectStructure.size() + " records in " + duration + " ms (" + (objectStructure.size() * 1000 / duration) + " records/s)");
					}

					// finally print the id's of the new objects from the
					// roottable (each id in a single line)
					Table rootTable = copy.tables.getTable(copy.tables.getRoottable());

					for (DBObject element : objectStructure.values()) {
						if (element.getSourceTable().equals(rootTable)) {
							System.out.println(element.newId);
						}
					}
				} catch (Exception e) {
					copy.handleErrors(e);
					throw e;
				}
			}

			// check for required options
		} catch (OutOfMemoryError e) {
			System.out.println(
					"Encountered OutOfMemoryError. Alter $NODECOPY_MEMORY in node.conf, to provide NodeCopy with more heap space via the -Xmx parameter (defaults to 256m)."
							+ e.getLocalizedMessage());
		} catch (MissingOptionException e) {
			// print help
			System.out.println("Not all required options were given: " + e.getLocalizedMessage());
			printHelpAndExit(options);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void printMemoryDebug() {
		System.out.println("User requested memory debug - running garbage collection.");
		System.gc();
		System.gc();
		Runtime runtime = Runtime.getRuntime();
		double totalMemory = runtime.totalMemory();
		double freeMemory = runtime.freeMemory();
		double maxMemory = runtime.maxMemory();
		double usedMemory = totalMemory - freeMemory;

		System.out.println("Total Memory :  " + totalMemory / 1024 / 1024 + " MB");
		System.out.println("Free  Memory :  " + freeMemory / 1024 / 1024 + " MB");
		System.out.println("Max   Memory :  " + maxMemory / 1024 / 1024 + " MB");
		System.out.println("Used  Memory :  " + usedMemory / 1024 / 1024 + " MB");
	}

	/**
	 * Create an instance of the Structure copy and check for inconsistent
	 * configuration. When this constructor is used, StructureCopy will handle the db connection by itself.
	 * @param configFilePath path to the config file
	 * @param copyController copy controller
	 * @param connectionURL database connection url
	 * @param driverClass name of the database driver class to use
	 * @param userName username (may be null)
	 * @param password user password (may be null)
	 * @param properties properties given for the copy process
	 * @throws Exception
	 */
	public StructureCopy(String configFilePath, CopyController copyController,
			String connectionURL, String driverClass, String userName, String password,
			Properties properties) throws Exception {
		this.copyController = copyController;
		this.properties = properties;
		// read the configuration file
		tables = readConfiguration(configFilePath);
		conn = establishDBConnection(connectionURL, driverClass, userName, password);

		try {
			if (!tables.checkConsistency(this)) {
				throw new Exception("Inconsistent configuration");
			}
		} finally {}
	}

	/**
	 * Create an instance of the StructureCopy and check for inconsistent configuration. When this 
	 * constructor is used, the connection from the given transaction is used. The connection will
	 * be committed or rolled back after use, but not closed.
	 * @param inputStream input stream of the configuration
	 * @param copyController copy controller
	 * @param t transaction
	 * @param properties properties given for the copy process
	 * @throws Exception
	 */
	public StructureCopy(InputStream inputStream, CopyController copyController,
			Transaction t, Properties properties) throws Exception {
		this.copyController = copyController;
		this.properties = properties;
		// read the configuration file
		tables = readConfiguration(inputStream);
		this.t = t;

		if (!tables.checkConsistency(this)) {
			throw new Exception("Inconsistent configuration");
		}
	}

	/**
	 * Method called before the copy process starts. Begins a database
	 * transaction.
	 * @throws StructureCopyException
	 */
	public void startCopy() throws StructureCopyException {
		try {
			// start a transaction
			if (conn != null) {
				oldAutocommit = conn.getAutoCommit();
				conn.setAutoCommit(false);

				// register a new instance of the connector with DB
				dbHandle = DB.addConnector(new StructureCopyConnector());
			}

			// start the process in the controller
			copyController.startCopy(this);
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		}
	}

	/**
	 * Method handling Exceptions. Rolls back the transaction
	 * @param e exception to be handles
	 * @throws SQLException
	 */
	public void handleErrors(Exception e) throws StructureCopyException {
		try {
			// remove the dbhandle
			if (dbHandle != null) {
				DB.closeConnector(dbHandle);
				dbHandle = null;
			}

			if (conn != null) {
				// rollback the transaction
				if (!conn.getAutoCommit()) {
					conn.rollback();
				}
			} else if (t != null) {
				// rollback the transaction, but don't stop it
				t.rollback(false);
			}
		} catch (SQLException e1) {
			throw new StructureCopyException(e1);
		} catch (TransactionException e1) {
			throw new StructureCopyException(e1);
		} finally {
			try {
				copyController.handleErrors(this, e);
			} finally {
				try {
					if (conn != null) {
						conn.setAutoCommit(oldAutocommit);
						conn.close();
					}
				} catch (SQLException e2) {
					throw new StructureCopyException(e2);
				}
			}
		}
	}

	/**
	 * Finish the copy process. Commit the transaction and close the connection.
	 * @throws SQLException
	 */
	public void finishCopy() throws StructureCopyException {
		// finish in the controller
		copyController.finishCopy(this);

		try {
			if (dbHandle != null) {
				// remove the dbhandle
				DB.closeConnector(dbHandle);
				dbHandle = null;
			}

			if (conn != null) {
				// commit the db transaction
				if (!conn.getAutoCommit()) {
					conn.commit();
				}
				conn.setAutoCommit(oldAutocommit);
				conn.close();
			} else if (t != null) {
				// commit the transaction (but don't close it)
				t.commit(false);
			}
			copyController.postCommit(this);
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		} catch (TransactionException e) {
			throw new StructureCopyException(e);
		}
	}

	/**
	 * Get the object structure
	 * @param verbose true for verbose
	 * @return map of objects
	 * @throws StructureCopyException
	 */
	public Map<ObjectKey, DBObject> getObjectStructure(boolean verbose) throws StructureCopyException {
		return tables.getObjectStructure(this, getConnection(), verbose);
	}

	/**
	 * Copy the structure, update all links to copied objects
	 * @param objectStructure object structure to copy
	 * @throws StructureCopyException
	 */
	public void copyStructure(Map<ObjectKey, DBObject> objectStructure) throws StructureCopyException {
		copyStructure(objectStructure, false);
	}

	/**
	 * Copy the structure, update all links to copied objects
	 * @param objectStructure object structure to copy
	 * @param verbose true for verbose mode
	 * @throws StructureCopyException
	 */
	public void copyStructure(Map<ObjectKey, DBObject> objectStructure, boolean verbose) throws StructureCopyException {
		tables.copyStructure(this, objectStructure, getConnection(), verbose);
	}

	/**
	 * Print all dependencies for the objects fetched from the given table
	 * @param table source table
	 * @param objectStructure map of all objects
	 */
	protected void printDependencies(Table table, Map<ObjectKey, DBObject> objectStructure) {
		for (Map.Entry<ObjectKey, DBObject> entry : objectStructure.entrySet()) {
			DBObject object = (DBObject) entry.getValue();

			if (object.getSourceTable().equals(table)) {
				printDependencies(object, objectStructure);
			}
		}
	}

	/**
	 * Print all depencencies for the given object
	 * @param object object to print
	 * @param objectStructure map of all objects
	 */
	protected void printDependencies(DBObject object, Map<ObjectKey, DBObject> objectStructure) {
		if (object == null) {
			return;
		}

		Stack<DBObject> path = new Stack<DBObject>();
		DBObject currentObject = object;

		while (currentObject != null) {
			path.push(currentObject);
			currentObject = currentObject.getCreationCauseObject();
		}
		Collections.reverse(path);
		boolean first = true;

		for (DBObject element : path) {
			if (first) {
				first = false;
			} else {
				System.out.print(" -> ");
			}
			System.out.print(element);
		}
		System.out.println();
	}

	/**
	 * Get object statistics. The output format of this may not be changed,
	 * because the automatic tests rely on this.
	 * @param objectStructure map of all objects
	 * @return object statistics (how many objects per table are contained in
	 *         the object map)
	 */
	public String getObjectStats(Map<ObjectKey, DBObject> objectStructure) {
		String output = new String();
		// prepare the counters for the tables
		Map<Table, Counter> tableCounters = new HashMap<Table, Counter>();

		for (Table table : tables.tablesMap.values()) {
			tableCounters.put(table, new Counter());
		}

		// count objects per table
		for (DBObject o : objectStructure.values()) {
			tableCounters.get(o.getSourceTable()).inc();
		}

		// print all counters (alphabetically ordered)
		List<String> names = new Vector<String>();

		for (Map.Entry<Table, Counter> entry : tableCounters.entrySet()) {
			names.add(entry.getKey().getId());
		}
		Collections.sort(names);

		for (String name : names) {
			output += tableCounters.get(tables.getTable(name)) + "\t" + name + "\n";
		}

		return output;
	}

	/**
	 * Dump the object structure
	 * @param objectStructure map of all objects
	 */
	protected void dumpObjects(Map<ObjectKey, DBObject> objectStructure) {
		for (Map.Entry<ObjectKey, DBObject> entry : objectStructure.entrySet()) {
			entry.getValue().dump(System.out);
		}
	}

	/**
	 * Dump the object structure for objects from the given table
	 * @param objectStructure map of all objects
	 * @param table source table
	 */
	protected void dumpObjects(Map<ObjectKey, DBObject> objectStructure, Table table) {
		for (Map.Entry<ObjectKey, DBObject> entry : objectStructure.entrySet()) {
			DBObject object = entry.getValue();

			if (object.getSourceTable().equals(table)) {
				object.dump(System.out);
			}
		}
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
	 * Read the configuration from the given config file path
	 * @param configFilePath path to the config file
	 * @return tables object
	 * @throws Exception
	 * @deprecated use {@link #readConfiguration(InputStream)} instead
	 */
	public static Tables readConfiguration(String configFilePath) throws Exception {
		JAXBContext context = JAXBContext.newInstance(JAXB_CONTEXT);
		Unmarshaller unmarshaller = context.createUnmarshaller();

		unmarshaller.setValidating(true);

		return (Tables) unmarshaller.unmarshal(new File(configFilePath));
	}

	/**
	 * Read the configuration from the given input stream
	 * @param inputStream input stream
	 * @return tables object
	 * @throws JAXBException
	 */
	public static Tables readConfiguration(InputStream inputStream) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(JAXB_CONTEXT);
		Unmarshaller unmarshaller = context.createUnmarshaller();

		unmarshaller.setValidating(true);

		return (Tables) unmarshaller.unmarshal(inputStream);
	}

	/**
	 * print the usage message and exit
	 * @param options cmd line options
	 */
	protected static void printHelpAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		formatter.printHelp("java " + StructureCopy.class.getName(), options, true);
		System.exit(0);
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
		options.addOption(
				OptionBuilder.withLongOpt("controller").withArgName("class-name").hasArg().withDescription("fully qualified class name of the copy controller").isRequired(true).create(
						"o"));
		options.addOption(OptionBuilder.withLongOpt("help").withDescription("shows this message").create("h"));
		options.addOption(OptionBuilder.withLongOpt("url").withArgName("connection-url").hasArg().withDescription("connection url").isRequired(true).create());
		options.addOption(
				OptionBuilder.withLongOpt("driverClass").withArgName("driver class").hasArg().withDescription("jdbc driver class").isRequired(true).create("d"));
		options.addOption(
				OptionBuilder.withLongOpt("username").withArgName("username").hasArg().withDescription("name of the database user").isRequired(true).create("u"));
		options.addOption(OptionBuilder.withLongOpt("password").withArgName("password").hasArg().withDescription("password of the database user").create("p"));
		options.addOption(OptionBuilder.withLongOpt("preview").withDescription("show preview of copied objecs, but do not modify the database").create());
		options.addOption(OptionBuilder.withLongOpt("verbose").withDescription("be verbose").create("v"));
		options.addOption(OptionBuilder.withArgName("property=value").hasArg().withValueSeparator().withDescription("declare properties").create("D"));
		options.addOption(OptionBuilder.withLongOpt("memdebug").withDescription("output memory debug information.").create("m"));

		return options;
	}

	/**
	 * Resolve properties encoded in the string as ${property.name}
	 * @param string string holding encoded properties
	 * @return string with the properties resolved
	 */
	public String resolveProperties(String string) {
		return resolveProperties(string, null);
	}

	public String resolveProperties(String string, Resolvable res) {
		// avoid NPE here
		if (string == null) {
			return null;
		}
		// create a matcher
		Matcher m = findProperties.matcher(string);
		StringBuffer output = new StringBuffer();
		int startIndex = 0;

		while (m.find()) {
			// copy static string between the last found system property and
			// this one
			if (m.start() > startIndex) {
				output.append(string.substring(startIndex, m.start()));
			}
			String property = m.group(1);

			if (property.startsWith("object.") && res != null) {
				output.append(res.get(property.substring(7)));
			} else {
				output.append(properties.getProperty(property, ""));
			}
			startIndex = m.end();
		}
		// if some trailing static string exists, copy it
		if (startIndex < string.length()) {
			output.append(string.substring(startIndex));
		}

		return output.toString();
	}

	/**
	 * Get the db connection
	 * @return connection
	 */
	public Connection getConnection() {
		if (conn != null) {
			return conn;
		} else if (t != null) {
			return t.getConnection();
		} else {
			return null;
		}
	}

	/**
	 * Get the configured tables
	 * @return configured tables
	 */
	public Tables getTables() {
		return tables;
	}

	/**
	 * Get the used copy controller
	 * @return copy controller
	 */
	public CopyController getCopyController() {
		return copyController;
	}

	/**
	 * Get the dbHandle
	 * @return the dbHandle
	 */
	public DBHandle getDbHandle() {
		if (dbHandle != null) {
			return dbHandle;
		} else if (t != null) {
			return t.getDBHandle();
		} else {
			return null;
		}
	}

	/**
	 * Get the transaction, if the StructureCopy was created using a transaction
	 * @return transaction or null
	 */
	public Transaction getTransaction() {
		return t;
	}

	/**
	 * Class for object keys.
	 */
	public static class ObjectKey {

		/**
		 * source table
		 */
		protected Table table;

		/**
		 * id of the object
		 */
		protected Object id;

		public Table getTable() {
			return table;
		}

		public Object getId() {
			return id;
		}

		/**
		 * Create an object key
		 * @param table source table
		 * @param id object's id
		 */
		private ObjectKey(Table table, Object id) {
			this.table = table;
			this.id = id;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return table.hashCode() + id.hashCode();
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ObjectKey) {
				return ((ObjectKey) obj).table.equals(table) && ((ObjectKey) obj).id.equals(id);
			} else {
				return false;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "key " + id + " in " + table;
		}

		/**
		 * Get the given is as ObjectKey
		 * @param table table
		 * @param id object id
		 * @return object key or null (when id is null)
		 */
		public static ObjectKey getObjectKey(Table table, Object id) {
			if (id instanceof ObjectKey) {
				return (ObjectKey) id;
			} else if (id != null) {
				return new StructureCopy.ObjectKey(table, id);
			} else {
				return null;
			}
		}
	}

	/**
	 * Helper class for counters
	 */
	public static class Counter {

		/**
		 * internal counter
		 */
		protected int c = 0;

		/**
		 * Increment the counter
		 */
		public void inc() {
			c++;
		}

		/**
		 * Get the current counter value
		 * @return counter value
		 */
		public int getValue() {
			return c;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return Integer.toString(c);
		}
	}

	/**
	 * Internal implementation of a Connector, which is registered with {@link DB}.
	 */
	public class StructureCopyConnector implements Connector {

		/**
		 * The poolConnection, which actually just wraps the connection of the StructureCopy object
		 */
		protected PoolConnection poolConnection;

		/**
		 * Create an instance of the connector
		 */
		public StructureCopyConnector() {
			// create the poolconnection instance
			poolConnection = new PoolConnection(0, StructureCopy.this.getConnection());
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.Connector#close()
		 */
		public void close() throws SQLException {// closing this connector does nothing, because the connection is closed elsewhere
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.Connector#getConnection()
		 */
		public PoolConnection getConnection() throws SQLException {
			return poolConnection;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.Connector#releaseConnection(com.gentics.lib.db.PoolConnection)
		 */
		public void releaseConnection(PoolConnection c) throws SQLException {// releasing the poolconnection does nothing here
		}
	}
}
