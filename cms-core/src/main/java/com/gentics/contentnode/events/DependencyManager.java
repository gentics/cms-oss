/*
 * @author norbert
 * @date 18.01.2007
 * @version $Id: DependencyManager.java,v 1.15.4.1 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.events;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.Publisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;
import com.gentics.lib.util.AttributedThreadGroup;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

/**
 * Static dependency manager.
 * TODO eventually add eventmask to dependencies (to have the possibility to filter out dependencies that are not relevant to the triggered events)
 */
public final class DependencyManager {

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(DependencyManager.class);

	/**
	 * Object Mapper for conversion of dependent properties
	 */
	protected final static ObjectMapper mapper = new ObjectMapper();

	/**
	 * triggered dependencies as threadlocal, sorted list
	 */
	private static ThreadLocal<Map<Integer, Set<DependencyObject>>> triggeredDependencies = new ThreadLocal<Map<Integer, Set<DependencyObject>>>();

	/**
	 * counter for number of dirted objects
	 */
	protected static ThreadLocal<DirtCounter> dirtCounter = new ThreadLocal<DirtCounter>();

	/**
	 * flag to enable simulation mode
	 */
	protected static ThreadLocal<Boolean> simulationMode = new ThreadLocal<Boolean>();

	/**
	 * id of the dependency that shall be simulated
	 */
	protected static ThreadLocal<Integer> simulateDependency = new ThreadLocal<Integer>();

	/**
	 * object stack that caused the targeted dependency (while rendering in simulation mode)
	 */
	protected static ThreadLocal<Stack<NodeObject>> simulateDependencyStack = new ThreadLocal<Stack<NodeObject>>();

	/**
	 * TODO
	 */
	protected static ThreadLocal<Integer> loggingEventDepth = new ThreadLocal<Integer>();

	/**
	 * TODO
	 */
	protected static ThreadLocal<PrintWriter> loggingWriter = new ThreadLocal<PrintWriter>();

	protected final static String[] EMPTY_PROPS = new String[0];

	/**
	 * Access statistics
	 */
	protected static Statistics stats;

	/**
	 * Prepared dependencies as map. Keys are the page IDs, values are the lists of dependencies read from table dependencymap2
	 */
	protected static Map<Integer, List<Dependency>> preparedPageDependencies;

	/**
	 * Threadlocal boolean to check whether dependency triggering is on or off
	 */
	private static ThreadLocal<Boolean> depTriggering = new ThreadLocal<Boolean>();

	/**
	 * compile flag to enable only storing of direct dependencies (the dep_obj
	 * is always the rendered root object and dep_ele is always null)
	 * DON'T CHANGE THIS SETTING (dep_ele was removed from the dependencymap2)
	 */
	public final static boolean DIRECT_DEPENDENCIES = true;
    
	private static final String DIRTED_OBJECTS_TG_KEY = "DependencyManager.dirtedObjects";
    
	private static final String TRIGGERED_DEPENDENCIES_TG_KEY = "DependencyManager.triggeredDependencies";
    
	private static final String DIRT_COUNTER_TG_KEY = "DependencyManager.dirtCounter";
    
	private DependencyManager() {}

	/**
	 * Start the publish transaction (if not yet started) and lock the table dependencymap2
	 * @param factory content node factory
	 * @throws NodeException when the transaction could not be started or was already running
	 */
	public static void startPublishTransaction(ContentNodeFactory factory) throws NodeException {
		stats = new Statistics();
		preparedPageDependencies = Collections.synchronizedMap(new HashMap<Integer, List<Dependency>>());
	}

	/**
	 * Commit the publish transaction
	 */
	public static void commitPublishTransaction() {
		stats = null;
		preparedPageDependencies = null;
	}

	/**
	 * Rollback the publish transaction
	 */
	public static void rollbackPublishTransaction() {
		stats = null;
		preparedPageDependencies = null;
	}

	/**
	 * Get the logger
	 * @return the logger
	 */
	public static NodeLogger getLogger() {
		return logger;
	}

	/**
	 * Initialize the list of triggered dependencies for this thread
	 */
	public static void initDependencyTriggering() {
		initDependencyTriggering(false);
	}

	/**
	 * Initialize the list of triggered dependencies for this thread
	 * @param simulationModeFlag true when simulation mode shall be used (no real
	 *        dirting), false for real dirting
	 * @param dependencyId id of the targeted dependency
	 */
	public static void initDependencyTriggering(boolean simulationModeFlag) {
		setTriggeredDependencies(new HashMap<Integer, Set<DependencyObject>>());
		setDirtCounter(new DirtCounter());
		simulationMode.set(Boolean.valueOf(simulationModeFlag));
		loggingEventDepth.set(null);
		simulateDependencyStack.set(null);
		closeLoggingWriter();
		depTriggering.set(true);

		PublishQueue.initFastDependencyDirting();
	}

	public static void initDependencyRendering(boolean simulationModeFlag, Integer dependencyId) {
		simulationMode.set(Boolean.valueOf(simulationModeFlag));
		loggingEventDepth.set(null);
		simulateDependency.set(dependencyId);
		simulateDependencyStack.set(null);
	}

	/**
	 * reset the list of triggered dependencies for this thread
	 *
	 */
	public static void resetDependencyTriggering() {
		resetDependencyTriggering(false);
	}

	/**
	 * reset the list of triggered dependencies for this thread
	 *
	 */
	public static void resetDependencyTriggering(boolean closeActionLogging) {
		if (isLogging()) {
			PrintWriter out = loggingWriter.get();

			if (out != null) {
				checkDepthLevel(out, 0);
				if (closeActionLogging) {
					out.println("</action>");
				}
			}
		}
		setTriggeredDependencies(null);
		setDirtCounter(null);
		simulationMode.set(null);
		loggingEventDepth.set(null);
		simulateDependency.set(null);
		simulateDependencyStack.set(null);
		closeLoggingWriter();
		depTriggering.set(false);

		PublishQueue.cancelFastDependencyDirting();
	}

	/**
	 * Check whether dependency triggering is on or off
	 * @return true if dependency triggering is on, false if not
	 */
	public static boolean isDependencyTriggering() {
		return ObjectTransformer.getBoolean(depTriggering.get(), false);
	}

	protected static void closeLoggingWriter() {
		PrintWriter out = loggingWriter.get();

		if (out != null) {
			out.close();
		}
		loggingWriter.set(null);
	}

	public static void setLoggingWriter(PrintWriter out) {
		closeLoggingWriter();
		loggingWriter.set(out);
	}

	/**
	 * Get the threadlocal, sorted list of triggered dependencies for a node (or general)
	 * @param nodeId id of the node, for which the dependencies were triggered
	 * @return sorted list of triggered dependencies
	 */
	@SuppressWarnings("unchecked")
	protected static Set<DependencyObject> getTriggeredDependencies(int nodeId) {
		Map<Integer, Set<DependencyObject>> triggeredDeps = null;
		Object triggeredDepsObject = AttributedThreadGroup.getForCurrentThreadGroup(TRIGGERED_DEPENDENCIES_TG_KEY, triggeredDependencies);

		if (triggeredDepsObject instanceof Map) {
			triggeredDeps = (Map<Integer, Set<DependencyObject>>) triggeredDepsObject;
		} else {
			triggeredDeps = new HashMap<Integer, Set<DependencyObject>>();
			setTriggeredDependencies(triggeredDeps);
		}

		if (!triggeredDeps.containsKey(nodeId)) {
			triggeredDeps.put(nodeId, new HashSet<DependencyObject>());
		}

		return triggeredDeps.get(nodeId);      
	}

	protected static void setTriggeredDependencies(Map<Integer, Set<DependencyObject>> value) {
		AttributedThreadGroup.setForCurrentThreadGroup(TRIGGERED_DEPENDENCIES_TG_KEY, value, triggeredDependencies);
	}

	/**
	 * Get the threadlocal dirtcounter
	 * @return dirtcounter
	 */
	public static DirtCounter getDirtCounter() {
		DirtCounter counter = (DirtCounter) AttributedThreadGroup.getForCurrentThreadGroup(DIRT_COUNTER_TG_KEY, dirtCounter);

		return counter;
	}
    
	public static void setDirtCounter(DirtCounter value) {
		AttributedThreadGroup.setForCurrentThreadGroup(DIRT_COUNTER_TG_KEY, value, dirtCounter);
	}

	/**
	 * Clear all dependencies for the given dependency object
	 * @param dependent dependency object
	 * @throws NodeException
	 */
	public static void clearDependencies(final DependencyObject dependent) throws NodeException {
		if (logger.isDebugEnabled()) {
			logger.debug("Clearing dependencies for {" + dependent + "}");
		}
		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				// clear all dependencies for the given dependent object/combination
				Transaction t = TransactionManager.getCurrentTransaction();
				PreparedStatement st = null;

				long start = System.currentTimeMillis();
				try {
					Integer objType = getTTypeOrNull(dependent.getObjectClass());
					Object objId = dependent.getObjectId();
	
					StringBuffer sql = new StringBuffer("DELETE FROM dependencymap2 WHERE");
	
					if (objType == null) {
						sql.append(" dep_obj_type IS NULL");
					} else {
						sql.append(" dep_obj_type = ?");
					}
					if (objId == null) {
						sql.append(" AND dep_obj_id IS NULL");
					} else {
						sql.append(" AND dep_obj_id = ?");
					}
	
					RuntimeProfiler.beginMark(JavaParserConstants.DEPENDENCY_MANAGER_DELETE_PREPARE);
					st = t.prepareDeleteStatement(sql.toString());
					RuntimeProfiler.endMark(JavaParserConstants.DEPENDENCY_MANAGER_DELETE_PREPARE);
					int paramCounter = 1;
	
					if (objType != null) {
						st.setObject(paramCounter++, objType);
					}
					if (objId != null) {
						st.setObject(paramCounter++, objId);
					}
					RuntimeProfiler.beginMark(JavaParserConstants.DEPENDENCY_MANAGER_DELETE_EXECUTE, sql.toString());
					st.executeUpdate();
					RuntimeProfiler.endMark(JavaParserConstants.DEPENDENCY_MANAGER_DELETE_EXECUTE, sql.toString());
				} catch (SQLException e) {
					throw new NodeException("Error while clearing dependencies", e);
				} finally {
					t.closeStatement(st);
					if (stats != null) {
						stats.addClearTime(start);
					}
				}
			}
		}, true);
	}

	/**
	 * Remove all old dependencies from the db and store new ones
	 * @param dependencies list of dependencies that might contain old or new ones
	 * @throws NodeException
	 */
	public static void storeDependencies(final List<Dependency> dependencies) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node channel = t.getChannel();
		final Integer channelId = ObjectTransformer.getInteger(channel != null && channel.isChannel() ? channel.getId() : 0, 0);
		TransactionManager.execute(new Executable() {
			public void execute() throws NodeException {
				long start = System.currentTimeMillis();
				if (ObjectTransformer.isEmpty(dependencies)) {
					return;
				}

				// do not store dependencies, when in simulation mode (because
				// that would not be a simulation anymore)
				if (isSimulationMode()) {
					if (logger.isInfoEnabled()) {
						logger.info("Currently in simulation mode: not storing dependencies");
					}
					return;
				}

				// collect the ids of the old dependencies here
				int[] ids = new int[dependencies.size()];
				int idCounter = 0;

				int deletedDeps = 0;
				int storedDeps = 0;
				int updatedDeps = 0;

				for (Dependency dep : dependencies) {
					if (dep.isOld()) {
						dep.removeChannelId(channelId);
						if (dep.getChannelIds().isEmpty()) {
							ids[idCounter++] = dep.getId();
							deletedDeps++;
						} else {
							// add all stored dependent properties as still used dependent properties (so they won't be removed)
							Map<String, Set<Integer>> propsMap = dep.getStoredDependentProperties();
							for (Entry<String, Set<Integer>> entry : propsMap.entrySet()) {
								String prop = entry.getKey();
								Set<Integer> channelIds = entry.getValue();
								for (int cId : channelIds) {
									dep.addDependentProperty(cId, prop);
								}
							}
							if (dep.isModified()) {
								dep.preserveOtherDependentProperties(channelId);
								dep.update();
								updatedDeps++;
							}
						}
					} else if (dep.isNew()) {
						// store new dependency
						dep.store();
						storedDeps++;
					} else if (dep.isModified()) {
						dep.preserveOtherDependentProperties(channelId);
						// update dependency
						dep.update();
						updatedDeps++;
					}
				}

				Transaction t = TransactionManager.getCurrentTransaction();
				PreparedStatement st = null;

				try {
					// clear dependencies (if old deps found)
					if (idCounter > 0) {
						// now create the statement
						StringBuffer sql = new StringBuffer();

						sql.append("DELETE FROM dependencymap2 WHERE id in (");
						sql.append(StringUtils.repeat("?", idCounter, ","));
						sql.append(")");
						st = t.prepareDeleteStatement(sql.toString());

						// fill in the ids
						for (int i = 0; i < idCounter; i++) {
							st.setInt(i + 1, ids[i]);
						}

						// execute the statement
						st.executeUpdate();
					}

					// finally log the stats
					if (logger.isInfoEnabled()) {
						logger.info("Stored " + storedDeps + " new dependencies");
						logger.info("Deleted " + deletedDeps + " old dependencies");
						logger.info("Updated " + updatedDeps + " modified dependencies");
						logger.info((dependencies.size() - updatedDeps - storedDeps - deletedDeps) + " dependencies remain untouched");
					}
				} catch (SQLException e) {
					throw new NodeException("Error while clearing old dependencies", e);
				} finally {
					t.closeStatement(st);
					if (stats != null) {
						stats.addStoreTime(start);
					}
				}
			}
		}, true, 3);
	}

	/**
	 * Load the dependencies the given list of objects (of common type) depend on from the database into the map of dependency lists.
	 * @param objType common object type
	 * @param objIds object ids
	 * @return map of dependencies per object id
	 * @throws NodeException
	 */
	protected static Map<Integer, List<Dependency>> loadDependenciesForObjects(final int objType, final List<Integer> objIds) throws NodeException {
		if (ObjectTransformer.isEmpty(objIds)) {
			return Collections.emptyMap();
		}

		final Map<Integer, List<Dependency>> dependencies = new HashMap<Integer, List<Dependency>>();
		final List<Dependency> flatList = new ArrayList<Dependency>();

		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();

				long start = System.currentTimeMillis();
				PreparedStatement st = null;
				ResultSet res = null;
    
				try {
					String sql = null;

					sql = "SELECT * FROM dependencymap2 WHERE dep_obj_type = ? AND dep_obj_id IN (" + StringUtils.repeat("?", objIds.size(), ",") + ")";
					st = t.prepareStatement(sql);
					int propCounter = 1;

					st.setInt(propCounter++, objType);
					for (Integer objId : objIds) {
						st.setInt(propCounter++, objId);
					}

					// perform the statement and add the dependencies to the
					// list
					res = st.executeQuery();
					flatList.addAll(getDependencies(res));
				} catch (SQLException e) {
					throw new NodeException("Error while getting dependencies for {" + objIds.size() + "} objects", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(st);
					if (stats != null) {
						stats.addGetTime(start);
					}
				}
			}
		}, true);

		// transform the flat list into a map of lists
		for (Dependency dep : flatList) {
			Integer objId = ObjectTransformer.getInteger(dep.getDependent().getObjectId(), null);
			if (objId != null) {
				List<Dependency> objList = dependencies.get(objId);
				if (objList == null) {
					objList = new ArrayList<Dependency>();
					dependencies.put(objId, objList);
				}
				objList.add(dep);
			}
		}

		return dependencies;
	}

	/**
	 * Get all dependencies for the given object (that is: things this object depends on)
	 * @param object object
	 * @param channelId channel id, if the dependencies for the object shall be fetched for the given channel only (may be null)
	 * @param property property, for which the dependencies shall be returned (null to get dependencies for all properties)
	 * @return list of dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getDependenciesForObject(final NodeObject object, final Integer channelId, final String property) throws NodeException {
		if (object == null) {
			return Collections.emptyList();
		} else {
			Integer objType = getTTypeOrNull(object.getObjectInfo().getObjectClass());
			Integer objId = ObjectTransformer.getInteger(object.getId(), null);
			if (objType == null || objId == null) {
				return Collections.emptyList();
			}
			final List<Dependency> dependencies = new ArrayList<Dependency>();

			Node node = null;
			if (channelId != null) {
				 node = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId);
			}

			// check whether the dependencies have been prepared
			if (Page.TYPE_PAGE_INTEGER.equals(objType) && preparedPageDependencies != null && preparedPageDependencies.containsKey(objId)) {
				dependencies.addAll(preparedPageDependencies.get(objId));
			} else {
				List<Dependency> loadedDeps = loadDependenciesForObjects(objType, Collections.singletonList(objId)).get(objId);
				if (!ObjectTransformer.isEmpty(loadedDeps)) {
					dependencies.addAll(loadedDeps);
				}
			}

			// filter by dependency property if one given
			filterForProperty(dependencies, property);

			// filter by node
			filterForChannel(dependencies, node);

			return dependencies;
		}
	}

	/**
	 * Get all dependencies on the given object and the properties matching the
	 * eventmask
	 * @param object modified object
	 * @param properties list of modified properties. when this is an empty
	 *        list, only dependencies WITHOUT property name match, when it is
	 *        null, ALL dependencies on the object match
	 * @param eventMask event mask to match
	 * @param node if not null, only get the dependencies for the given node/channel
	 * @return list of all dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getAllDependencies(final NodeObject object, final String[] properties, final int eventMask, final Node node) throws NodeException {
		if (object == null) {
			return Collections.emptyList();
		} else if (properties != null && properties.length == 0) {
			// this is an optimization, there should never be entries with mod_ele AND mod_prop together NULL
			return Collections.emptyList();
		} else {
			final List<Dependency> dependencies = new Vector<Dependency>();
			TransactionManager.execute(new Executable() {
				/* (non-Javadoc)
				 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
				 */
				public void execute() throws NodeException {
					Transaction t = TransactionManager.getCurrentTransaction();

					long start = System.currentTimeMillis();
					PreparedStatement st = null;
					ResultSet res = null;
	
	    
					try {
						// for performance reasons, the real statement is split into two
						// parts, here comes the first part
	    
						StringBuffer sql = new StringBuffer();
						int propCounter = 0;
	    
						// when the object was deleted, we are not interested in mod_prop or ele!
						if (Events.isEvent(eventMask, Events.DELETE)) {
							sql.append("SELECT * FROM dependencymap2 WHERE (mod_obj_type = ? AND mod_obj_id = ?)");
							sql.append(" AND eventmask & ? > 0");
							st = t.prepareStatement(sql.toString());
							propCounter = 1;
							st.setObject(propCounter++, getTTypeOrNull(object.getObjectInfo().getObjectClass()));
							st.setObject(propCounter++, object.getId());
							st.setInt(propCounter++, eventMask);
						} else {
							sql.append("SELECT * FROM dependencymap2 WHERE (mod_obj_type = ? AND mod_obj_id = ? AND mod_ele_type IS NULL AND mod_ele_id IS NULL)");
							if (properties != null && properties.length > 0) {
								sql.append(" AND mod_prop IN (");
								sql.append(StringUtils.repeat("?", properties.length, ", "));
								sql.append(")");
							} else if (properties != null) {
								sql.append(" AND mod_prop IS NULL");
							}
							sql.append(" AND eventmask & ? > 0");
	
							st = t.prepareStatement(sql.toString());
							propCounter = 1;
							st.setObject(propCounter++, getTTypeOrNull(object.getObjectInfo().getObjectClass()));
							st.setObject(propCounter++, object.getId());
							if (properties != null && properties.length > 0) {
								for (int i = 0; i < properties.length; i++) {
									st.setString(propCounter++, properties[i]);
								}
							}
							st.setInt(propCounter++, eventMask);
						}
	    
						// perform the statement and add the dependencies to the list
						res = st.executeQuery();
						dependencies.addAll(getDependencies(res));
	    
						// close resources
						t.closeResultSet(res);
						t.closeStatement(st);
	    
						// here comes the second part
						sql.delete(0, sql.length());
						sql.append("SELECT * FROM dependencymap2 WHERE (mod_ele_type = ? AND mod_ele_id = ?)");
	    
						if (properties != null && properties.length > 0) {
							sql.append(" AND mod_prop IN (");
							sql.append(StringUtils.repeat("?", properties.length, ", "));
							sql.append(")");
						} else if (properties != null) {
							sql.append(" AND mod_prop IS NULL");
						}
						sql.append(" AND eventmask & ? > 0");
	
						st = t.prepareStatement(sql.toString());
						propCounter = 1;
						st.setObject(propCounter++, getTTypeOrNull(object.getObjectInfo().getObjectClass()));
						st.setObject(propCounter++, object.getId());
						if (properties != null && properties.length > 0) {
							for (int i = 0; i < properties.length; i++) {
								st.setString(propCounter++, properties[i]);
							}
						}
						st.setInt(propCounter++, eventMask);

						// perform the statement and add the dependencies to the list
						res = st.executeQuery();
						dependencies.addAll(getDependencies(res));

					} catch (SQLException e) {
						throw new NodeException("Error while getting dependencies", e);
					} finally {
						t.closeResultSet(res);
						t.closeStatement(st);
						if (stats != null) {
							stats.addGetTime(start);
						}
					}
				}
			}, true);

			// filter by channel
			filterForChannel(dependencies, node);

			// finally return the dependencies
			return dependencies;
		}
	}

	/**
	 * Get all dependencies on the given object/element combination and the properties matching the
	 * eventmask
	 * @param sourceObject parent of the modified object
	 * @param sourceElement modified element
	 * @param properties list of modified properties. when this is an empty
	 *        list, only dependencies WITHOUT property name match, when it is
	 *        null, ALL dependencies on the object match
	 * @param eventMask event mask to match
	 * @param node node for which the dependencies shall be fetched (may be null to get all)
	 * @return list of all dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getAllDependencies(final NodeObject sourceObject, final NodeObject sourceElement,
			final String[] properties, final int eventMask, final Node node) throws NodeException {
		if (sourceObject == null) {
			return getAllDependencies(sourceElement, properties, eventMask, node);
		} else if (sourceElement == null) {
			return getAllDependencies(sourceObject, properties, eventMask, node);
		}

		final List<Dependency> deps = new ArrayList<Dependency>();
		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();

				long start = System.currentTimeMillis();
				PreparedStatement st = null;
				ResultSet res = null;
	
				try {
					StringBuffer sql = new StringBuffer("SELECT * FROM dependencymap2 WHERE mod_obj_type = ? AND mod_obj_id = ?");
	
					if (sourceElement != null) {
						sql.append(" AND mod_ele_type = ? AND mod_ele_id = ?");
					}
					if (properties != null && properties.length > 0) {
						sql.append(" AND mod_prop IN (");
						StringUtils.repeat("?", properties.length, ", ");
						sql.append(")");
					} else if (properties != null) {
						sql.append(" AND mod_prop IS NULL");
					}
					sql.append(" AND eventmask & ? > 0");
	    
					st = t.prepareStatement(sql.toString());
					int propCounter = 1;
	
					st.setObject(propCounter++, getTTypeOrNull(sourceObject.getObjectInfo().getObjectClass()));
					st.setObject(propCounter++, sourceObject.getId());
					if (sourceElement != null) {
						st.setObject(propCounter++, getTTypeOrNull(sourceElement.getObjectInfo().getObjectClass()));
						st.setObject(propCounter++, sourceElement.getId());
					}
					if (properties != null && properties.length > 0) {
						for (int i = 0; i < properties.length; i++) {
							st.setString(propCounter++, properties[i]);
						}
					}
					st.setInt(propCounter++, eventMask);
	    
					res = st.executeQuery();
					deps.addAll(getDependencies(res));
				} catch (SQLException e) {
					throw new NodeException("Error while getting dependencies", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(st);
					if (stats != null) {
						stats.addGetTime(start);
					}
				}
			}
		}, true);

		// filter dependencies for node
		filterForChannel(deps, node);

		return deps;
	}

	/**
	 * Get all dependencies on the given objects and the properties matching the
	 * eventmask
	 * @param sourceObject parent of the modified object
	 * @param sourceElementClass class of the modified elements
	 * @param properties list of modified properties. when this is an empty
	 *        list, only dependencies WITHOUT property name match, when it is
	 *        null, ALL dependencies on the object match
	 * @param eventMask event mask to match
	 * @param node node for which to get the dependencies (may be null)
	 * @return list of all dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getAllDependencies(final NodeObject sourceObject, final Class<? extends NodeObject> sourceElementClass,
			final String[] properties, final int eventMask, final Node node) throws NodeException {
		final List<Dependency> deps = new ArrayList<Dependency>();

		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();

				long start = System.currentTimeMillis();
				PreparedStatement st = null;
				ResultSet res = null;
	
				try {
					StringBuffer sql = new StringBuffer("SELECT * FROM dependencymap2 WHERE mod_obj_type = ? AND mod_obj_id = ?");
	
					if (sourceElementClass != null && !sourceElementClass.equals(Object.class)) {
						sql.append(" AND mod_ele_type = ?");
					} else if (sourceElementClass == null) {
						sql.append(" AND mod_ele_type IS NULL");
					}
					if (properties != null && properties.length > 0) {
						sql.append(" AND mod_prop IN (");
						sql.append(StringUtils.repeat("?", properties.length, ", "));
						sql.append(")");
					} else if (properties != null) {
						sql.append(" AND mod_prop IS NULL");
					}
					sql.append(" AND eventmask & ? > 0");
	    
					st = t.prepareStatement(sql.toString());
					int propCounter = 1;
	
					st.setObject(propCounter++, getTTypeOrNull(sourceObject.getObjectInfo().getObjectClass()));
					st.setObject(propCounter++, sourceObject.getId());
					if (sourceElementClass != null && !sourceElementClass.equals(Object.class)) {
						st.setObject(propCounter++, getTTypeOrNull(sourceElementClass));
					}
					if (properties != null && properties.length > 0) {
						for (int i = 0; i < properties.length; i++) {
							st.setString(propCounter++, properties[i]);
						}
					}
					st.setInt(propCounter++, eventMask);
					res = st.executeQuery();
	
					deps.addAll(getDependencies(res));
				} catch (SQLException e) {
					throw new NodeException("Error while getting dependencies", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(st);
					if (stats != null) {
						stats.addGetTime(start);
					}
				}
			}
		}, true);

		// filter for node
		filterForChannel(deps, node);

		return deps;
	}

	/**
	 * Get all dependencies on the given objects and the properties matching the
	 * eventmask
	 * @param sourceObjectClass class of the parents of the modified object
	 * @param sourceElement modified element
	 * @param properties list of modified properties. when this is an empty
	 *        list, only dependencies WITHOUT property name match, when it is
	 *        null, ALL dependencies on the object match
	 * @param eventMask event mask to match
	 * @param node node to get the dependencies for (may be null to get all)
	 * @return list of all dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getAllDependencies(final Class<? extends NodeObject> sourceObjectClass, final NodeObject sourceElement,
			final String[] properties, final int eventMask, final Node node) throws NodeException {
		final List<Dependency> deps = new ArrayList<Dependency>();
		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();

				long start = System.currentTimeMillis();
				PreparedStatement st = null;
				ResultSet res = null;
	
				try {
					StringBuffer sql = new StringBuffer("SELECT * FROM dependencymap2 WHERE mod_obj_type = ?");
	
					if (sourceElement != null) {
						sql.append(" AND mod_ele_type = ? AND mod_ele_id = ?");
					} else {
						sql.append(" AND mod_ele_type IS NULL AND mod_ele_id IS NULL");
					}
					if (properties != null && properties.length > 0) {
						sql.append(" AND mod_prop IN (");
						StringUtils.repeat("?", properties.length, ", ");
						sql.append(")");
					} else if (properties != null) {
						sql.append(" AND mod_prop IS NULL");
					}
					sql.append(" AND eventmask & ? > 0");
	    
					st = t.prepareStatement(sql.toString());
					int propCounter = 1;
	
					st.setObject(propCounter++, getTTypeOrNull(sourceObjectClass));
					if (sourceElement != null) {
						st.setObject(propCounter++, getTTypeOrNull(sourceElement.getObjectInfo().getObjectClass()));
						st.setObject(propCounter++, sourceElement.getId());
					}
					if (properties != null && properties.length > 0) {
						for (int i = 0; i < properties.length; i++) {
							st.setString(propCounter++, properties[i]);
						}
					}
					st.setInt(propCounter++, eventMask);
					res = st.executeQuery();
	    
					deps.addAll(getDependencies(res));
				} catch (SQLException e) {
					throw new NodeException("Error while getting dependencies", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(st);
					if (stats != null) {
						stats.addGetTime(start);
					}
				}
			}
		}, true);

		// filter for node
		filterForChannel(deps, node);

		return deps;
	}

	/**
	 * Get all dependencies on the given objects and the properties matching the
	 * eventmask
	 * @param sourceObjectClass class of the parents of the modified objects
	 * @param sourceElementClass class of the modified elements
	 * @param properties list of modified properties. when this is an empty
	 *        list, only dependencies WITHOUT property name match, when it is
	 *        null, ALL dependencies on the object match
	 * @param eventMask event mask to match
	 * @param node node for which to get the dependencies (may be null to get all)
	 * @return list of all dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getAllDependencies(
			final Class<? extends NodeObject> sourceObjectClass,
			final Class<? extends NodeObject> sourceElementClass, final String[] properties, final int eventMask, final Node node) throws NodeException {
		final List<Dependency> deps = new ArrayList<Dependency>();
		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();

				long start = System.currentTimeMillis();
				PreparedStatement st = null;
				ResultSet res = null;
	
				try {
					StringBuffer sql = new StringBuffer("SELECT * FROM dependencymap2 WHERE mod_obj_type = ?");
	
					if (sourceElementClass != null && !sourceElementClass.equals(Object.class)) {
						sql.append(" AND mod_ele_type = ?");
					} else if (sourceElementClass == null) {
						sql.append(" AND mod_ele_type IS NULL");
					}
					if (properties != null && properties.length > 0) {
						sql.append(" AND mod_prop IN (");
						sql.append(StringUtils.repeat("?", properties.length, ", "));
						sql.append(")");
					} else if (properties != null) {
						sql.append(" AND mod_prop IS NULL");
					}
					sql.append(" AND eventmask & ? > 0");
	    
					st = t.prepareStatement(sql.toString());
					int propCounter = 1;
	
					st.setObject(propCounter++, getTTypeOrNull(sourceObjectClass));
					if (sourceElementClass != null && !sourceElementClass.equals(Object.class)) {
						st.setObject(propCounter++, getTTypeOrNull(sourceElementClass));
					}
					if (properties != null && properties.length > 0) {
						for (int i = 0; i < properties.length; i++) {
							st.setString(propCounter++, properties[i]);
						}
					}
					st.setInt(propCounter++, eventMask);
	    
					res = st.executeQuery();
					deps.addAll(getDependencies(res));
				} catch (SQLException e) {
					throw new NodeException("Error while getting dependencies", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(st);
					if (stats != null) {
						stats.addGetTime(start);
					}
				}
			}
		}, true);

		// filter for node
		filterForChannel(deps, node);

		return deps;
	}

	/**
	 * Get all dependencies on the given object and the properties matching the
	 * eventmask
	 * @param sourceObject source dependency
	 * @param properties list of modified properties. when this is an empty
	 *        list, only dependencies WITHOUT property name match, when it is
	 *        null, ALL dependencies on the object match
	 * @param eventMask event mask to match
	 * @param node node/channel for which to get the dependencies (may be null to fetch all)
	 * @return list of all dependencies
	 * @throws NodeException
	 */
	public static List<Dependency> getAllDependencies(DependencyObject sourceObject, String[] properties, int eventMask, Node node) throws NodeException {
		if (sourceObject.getObject() != null) {
			if (sourceObject.getElement() != null) {
				return getAllDependencies(sourceObject.getObject(), sourceObject.getElement(), properties, eventMask, node);
			} else if (sourceObject.getElementClass() != null) {
				return getAllDependencies(sourceObject.getObject(), sourceObject.getElementClass(), properties, eventMask, node);
			} else {
				return getAllDependencies(sourceObject.getObject(), properties, eventMask, node);
			}
		} else {
			if (sourceObject.getElement() != null) {
				return getAllDependencies(sourceObject.getObjectClass(), sourceObject.getElement(), properties, eventMask, node);
			} else {
				return getAllDependencies(sourceObject.getObjectClass(), sourceObject.getElementClass(), properties, eventMask, node);
			}
		}
	}

	public static Dependency createDependency(Class<? extends NodeObject> sourceObjectClass, Integer sourceObjectId,
			Class<? extends NodeObject> sourceElementClass, Integer sourceElementId, String sourceProperty,
			Class<? extends NodeObject> dependentObjectClass, Integer dependentObjectId, Class<? extends NodeObject> dependentElementClass,
			Integer dependentElementId, int eventMask) throws NodeException {
		return new DependencyImpl(new DependencyObject(sourceObjectClass, sourceObjectId, sourceElementClass, sourceElementId), sourceProperty,
				new DependencyObject(dependentObjectClass, dependentObjectId, dependentElementClass, dependentElementId), eventMask);
	}

	public static Dependency createDependency(NodeObject sourceObject,
			NodeObject sourceElement, String sourceProperty, NodeObject dependentObject,
			NodeObject dependentElement, int eventMask) throws NodeException {
		return createDependency(sourceObject.getObjectInfo().getObjectClass(), sourceObject.getId(),
				sourceElement != null ? sourceElement.getObjectInfo().getObjectClass() : null, sourceElement != null ? sourceElement.getId() : null, sourceProperty,
				dependentObject.getObjectInfo().getObjectClass(), dependentObject.getId(),
				dependentElement != null ? dependentElement.getObjectInfo().getObjectClass() : null, dependentElement != null ? dependentElement.getId() : null,
				eventMask);
	}

	public static Dependency createDependency(NodeObject source, String sourceProperty,
			NodeObject dependent, int eventMask) throws NodeException {
		return new DependencyImpl(new DependencyObject(source), sourceProperty, new DependencyObject(dependent), eventMask);
	}

	public static Dependency createDependency(DependencyObject source, String sourceProperty,
			DependencyObject dependent, int eventMask) throws NodeException {
		return new DependencyImpl(source, sourceProperty, dependent, eventMask);
	}

	/**
	 * Filter the given dependencies for the node/channel (if one is given)
	 * @param dependencies dependencies, that might be filtered
	 * @param node node (may be null)
	 * @throws NodeException
	 */
	public static void filterForChannel(List<Dependency> dependencies, Node node) throws NodeException {
		Object filteredChannelId = null;

		if (node != null) {
			if (node.isChannel()) {
				filteredChannelId = node.getId();
			} else {
				filteredChannelId = 0;
			}
		}

		// filter by channel
		if (filteredChannelId != null) {
			for (Iterator<Dependency> i = dependencies.iterator(); i.hasNext();) {
				Dependency dep = i.next();

				if (!dep.getChannelIds().contains(filteredChannelId)) {
					i.remove();
				}
			}
		}
	}

	/**
	 * Filter the given dependencies for the given property (if one is given)
	 * @param dependencies dependencies, that might be filtered
	 * @param property property to filter (may be null)
	 * @throws NodeException
	 */
	public static void filterForProperty(List<Dependency> dependencies, String property) throws NodeException {
		if (!StringUtils.isEmpty(property)) {
			for (Iterator<Dependency> i = dependencies.iterator(); i.hasNext();) {
				Dependency dep = i.next();

				if (!dep.getStoredDependentProperties().containsKey(property)) {
					i.remove();
				}
			}
		}
	}

	/**
	 * Get the list of dependencies with data given as resultset
	 * @param res resultset holding data of dependencies
	 * @return list of dependencies
	 * @throws NodeException
	 */
	protected static List<Dependency> getDependencies(ResultSet res) throws NodeException {
		List<Dependency> dependencies = new ArrayList<Dependency>();

		try {
			while (res.next()) {
				DependencyObject modObject = new DependencyObject(getObjectClassOrNull(res.getInt("mod_obj_type")), getObjectIdOrNull(res.getInt("mod_obj_id")),
						getObjectClassOrNull(res.getInt("mod_ele_type")), getObjectIdOrNull(res.getInt("mod_ele_id")));
				DependencyObject depObject = new DependencyObject(getObjectClassOrNull(res.getInt("dep_obj_type")), getObjectIdOrNull(res.getInt("dep_obj_id")),
						null, null);

				// create the dependency object
				Dependency newDep = new DependencyImpl(modObject, res.getString("mod_prop"), depObject, res.getInt("eventmask"), res.getInt("id"), res.getString("dep_prop"), res.getString("dep_channel_id"));
				// check whether the dependency was already found
				int index = Collections.binarySearch(dependencies, newDep);
				if (index >= 0) {
					// already found, so just merge the channel ids
					Dependency foundDep = dependencies.get(index);
					foundDep.merge(newDep);
				} else {
					// not yet found, so add the dependency
					dependencies.add(-index-1, newDep);
				}
			}
		} catch (SQLException e) {
			throw new NodeException("Error while getting dependencies", e);
		}

		return dependencies;
	}

	/**
	 * Get the object id as Object or null, if the object id is 0
	 * @param objectId object id
	 * @return object id as Object or null
	 */
	protected static Integer getObjectIdOrNull(int objectId) {
		return objectId <= 0 ? null : new Integer(objectId);
	}

	/**
	 * Get the object class for the given type id or null, if the object class
	 * is not known or 0
	 * @param typeId type id
	 * @return object class or null
	 * @throws NodeException
	 */
	protected static Class<? extends NodeObject> getObjectClassOrNull(int typeId) throws NodeException {
		return typeId <= 0 ? null : TransactionManager.getCurrentTransaction().getClass(typeId);
	}

	/**
	 * Get the TType for the class or null if no class set
	 * @param objectClass object class or null
	 * @return TType as Integer or null
	 * @throws NodeException
	 */
	protected static Integer getTTypeOrNull(Class<? extends NodeObject> objectClass) throws NodeException {
		if (objectClass == null) {
			return null;
		}
		int ttype = TransactionManager.getCurrentTransaction().getTType(objectClass);

		if (ttype == ContentFile.TYPE_IMAGE) {
			// We store images als '10008' into dependencymap,
			// so we need to convert it. rt#15038
			ttype = ContentFile.TYPE_FILE;
		}
		return new Integer(ttype);
	}

	/**
	 * Check whether the dependency manager is set to "simulation mode" for this thread
	 * @return true when simulation mode is active, false if not
	 */
	public static boolean isSimulationMode() {
		return ObjectTransformer.getBoolean(simulationMode.get(), false);
	}

	/**
	 * Check whether the dependency manager is logging or not
	 * @return true for logging, false for no logging
	 */
	public static boolean isLogging() {
		return loggingWriter.get() != null;
	}

	/**
	 * Implementation of a dependency
	 */
	public static class DependencyImpl implements Dependency, Comparable<Dependency> {
		protected DependencyObject source;
		protected DependencyObject dependent;
		protected String sourceProperty;
		protected int eventMask;

		/**
		 * Added dependent properties per channel
		 */
		protected Map<String, Set<Integer>> dependentProperties = new TreeMap<>();

		/**
		 * Stored dependent properties per channel
		 */
		protected Map<String, Set<Integer>> storedDepProperties = new TreeMap<>();

		/**
		 * internal id of the dependency
		 */
		protected int id = -1;

		/**
		 * flag to mark still existing dependencies
		 */
		protected boolean existing;

		/**
		 * Flag to mark dependencies, where the channel ids changed
		 */
		protected boolean channelIdsChanged = false;

		/**
		 * Channel IDs of this dependency. This list will always be sorted
		 */
		protected List<Integer> channelIds = new ArrayList<Integer>();

		/**
		 * List of merged dependencies (will be removed, when this dependency is stored)
		 */
		protected List<Integer> mergedDeps = new ArrayList<Integer>();

		/**
		 * Create an instance of the dependency
		 * @parma source source object
		 * @param sourceProperty source property
		 * @param dependent dependent object
		 * @param eventMask eventmask
		 */
		public DependencyImpl(DependencyObject source, String sourceProperty,
				DependencyObject dependent, int eventMask) throws NodeException {
			if (source == null || dependent == null) {
				throw new NodeException("Cannot generate dependency with null objects");
			}
			this.source = source;
			this.sourceProperty = sourceProperty;
			this.dependent = dependent;
			this.eventMask = eventMask;
			this.existing = true;
		}

		/**
		 * Create an instance of an already stored dependency
		 * @param source source object
		 * @param sourceProperty source property
		 * @param dependent dependent object
		 * @param eventMask eventmask
		 * @param id id
		 * @param dependentProperty dependent properties (comma separated list)
		 * @param depChannels channels for this dependencies (comma separated list)
		 * @throws NodeException
		 */
		public DependencyImpl(DependencyObject source, String sourceProperty,
				DependencyObject dependent, int eventMask, int id, String dependentProperty, String depChannels) throws NodeException {
			this(source, sourceProperty, dependent, eventMask);
			this.id = id;
			// set the existing flag to false, since we fetched this dep from
			// the db and do not known, whether it is still existing or should
			// be removed
			this.existing = false;

			// it is important to first set the dependent channels
			addChannels(depChannels);

			// check whether this is a JSON object, if not, make a map for all dependent channels
			if (!StringUtils.isEmpty(dependentProperty)) {
				// JSON objects always start with {
				if (dependentProperty.startsWith("{")) {
					try {
						// we parse the stored JSON into a map
						Map<?, ?> map = mapper.readValue(dependentProperty, Map.class);

						if (!map.isEmpty()) {
							// check whether the first key can be parsed to an integer
							if (ObjectTransformer.getInt(map.keySet().iterator().next(), -1) >= 0) {
								// this is the old format
								parseFromOldJsonFormat(map, dependentProperty);
							} else {
								// this is the new format
								parseFromNewJsonFormat(map, dependentProperty);
							}
						}
					} catch (IOException e) {
						throw new NodeException(String.format("Error while parsing dependent property %s", dependentProperty), e);
					}
				} else {
					// this is the fallback for previously stored dependencies
					List<String> props = Arrays.asList(dependentProperty.split(","));
					for (String prop : props) {
						storedDepProperties.computeIfAbsent(prop, key -> new TreeSet<>()).addAll(channelIds);
					}
				}
			}

			this.channelIdsChanged = false;
		}

		/**
		 * Parse the dependent property (which is already transformed into a map) from the old JSON format
		 * @param map map
		 * @param dependentProperty dependent property, like it is stored in the DB
		 */
		private void parseFromOldJsonFormat(Map<?, ?> map, String dependentProperty) {
			for (Entry<?, ?> entry : map.entrySet()) {
				// keys are expected to be integers (non-negative)
				int channelId = ObjectTransformer.getInt(entry.getKey(), -1);
				if (channelId >= 0) {
					// values are expected to be lists of strings
					Collection<?> collection = ObjectTransformer.getCollection(entry.getValue(), null);
					if (collection != null) {
						collection.stream().map(v -> ObjectTransformer.getString(v, null))
								.filter(v -> v != null).forEach(v -> {
									storedDepProperties.computeIfAbsent(v, key -> new TreeSet<>()).add(channelId);
								});
					} else {
						logger.warn(String.format("Ignoring %s as dependency list for channel %d", entry.getValue(), channelId));
					}
				} else {
					logger.warn(String.format("Ignoring non-integer key %s when parsing dependent property %s", entry.getKey(), dependentProperty));
				}
			}
		}

		/**
		 * Parse the dependent property (which is already transformed into a map) from the new JSON format
		 * @param map map
		 * @param dependentProperty dependent property, like it is stored in the DB
		 */
		private void parseFromNewJsonFormat(Map<?, ?> map, String dependentProperty) {
			for (Entry<?, ?> entry : map.entrySet()) {
				// keys are the property names
				String prop = ObjectTransformer.getString(entry.getKey(), null);
				if (prop != null) {
					// values are expected to be sets of integers
					Collection<?> collection = ObjectTransformer.getCollection(entry.getValue(), null);
					if (collection != null) {
						collection.stream().map(v -> ObjectTransformer.getInteger(v, null)).filter(v -> v != null)
								.forEach(v -> {
									storedDepProperties.computeIfAbsent(prop, key -> new TreeSet<>()).add(v);
								});
					} else {
						logger.warn(
								String.format("Ignoring %s as channel list for property %s", entry.getValue(), prop));
					}
				} else {
					logger.warn(String.format("Ignoring key  %s as dependent property", entry.getKey()));
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getSourceProperty()
		 */
		public String getSourceProperty() throws NodeException {
			return sourceProperty;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#store()
		 */
		public void store() throws NodeException {
			if (logger.isDebugEnabled()) {
				logger.debug("Storing dependency {" + this + "}");
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement st = null;

			try {
				RuntimeProfiler.beginMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_PREPARE);
				st = t.prepareInsertStatement(
						"INSERT INTO dependencymap2 (mod_obj_type, mod_obj_id, mod_ele_type, mod_ele_id, mod_prop, dep_obj_type, dep_obj_id, dep_channel_id, eventmask, dep_prop) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				RuntimeProfiler.endMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_PREPARE);
				if (source.getObjectClass() != null) {
					st.setObject(1, getTTypeOrNull(source.getObjectClass()));
				} else {
					st.setNull(1, Types.INTEGER);
				}
				st.setObject(2, source.getObjectId());
				if (source.getElementClass() != null) {
					st.setObject(3, getTTypeOrNull(source.getElementClass()));
				} else {
					st.setNull(3, Types.INTEGER);
				}
				st.setObject(4, source.getElementId());
				st.setString(5, sourceProperty);
                
				if (dependent.getObjectClass() != null) {
					st.setObject(6, getTTypeOrNull(dependent.getObjectClass()));
				} else {
					st.setNull(6, Types.INTEGER);
				}
				st.setObject(7, dependent.getObjectId());
				st.setObject(8, getChannelsForStoring());
				st.setInt(9, eventMask);
				st.setString(10, getDepPropForStoring());

				RuntimeProfiler.beginMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_EXECUTE);
				st.executeUpdate();
				RuntimeProfiler.endMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_EXECUTE);
			} catch (SQLException ex) {
				throw new NodeException("Error while adding dependency", ex);
			} finally {
				t.closeStatement(st);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#update()
		 */
		public void update() throws NodeException {
			if (logger.isDebugEnabled()) {
				logger.debug("Updating dependency {" + this + "}");
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement st = null;

			try {
				RuntimeProfiler.beginMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_PREPARE);
				st = t.prepareInsertStatement("UPDATE dependencymap2 SET dep_prop = ?, dep_channel_id = ? WHERE id = ?");
				RuntimeProfiler.endMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_PREPARE);
				st.setString(1, getDepPropForStoring());
				st.setString(2, getChannelsForStoring());
				st.setInt(3, id);

				RuntimeProfiler.beginMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_EXECUTE);
				st.executeUpdate();
				RuntimeProfiler.endMark(JavaParserConstants.DEPENDENCY_MANAGER_STORE_EXECUTE);
			} catch (SQLException ex) {
				throw new NodeException("Error while updating dependency", ex);
			} finally {
				t.closeStatement(st);
			}

			if (!mergedDeps.isEmpty()) {
				DBUtils.executeMassStatement("DELETE FROM dependencymap2 WHERE id IN ", null, mergedDeps, 1, null, Transaction.DELETE_STATEMENT);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getSource()
		 */
		public DependencyObject getSource() throws NodeException {
			return source;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getDependent()
		 */
		public DependencyObject getDependent() throws NodeException {
			return dependent;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer str = new StringBuffer();

			str.append("dep {").append(dependent).append("} -> source {").append(source).append("}");
			if (sourceProperty != null) {
				str.append(",prop {").append(sourceProperty).append("}");
			}
			str.append(String.format(", channels {%s}", channelIds));
			return str.toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			try {
				if (obj instanceof Dependency) {
					Dependency dep = (Dependency) obj;

					return source.equals(dep.getSource()) && dependent.equals(dep.getDependent())
							&& (sourceProperty == null ? dep.getSourceProperty() == null : sourceProperty.equals(dep.getSourceProperty()));
				} else {
					return false;
				}
			} catch (NodeException e) {
				logger.error("Error while comparing dependencies", e);
				return false;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return source.hashCode() + dependent.hashCode() + (sourceProperty != null ? sourceProperty.hashCode() : 0);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#triggerDependency(int, int, com.gentics.contentnode.object.Node)
		 */
		public void triggerDependency(int eventMask, int depth, Node node) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean attributeDirting = NodeConfigRuntimeConfiguration.isFeature(Feature.ATTRIBUTE_DIRTING);
			Set<DependencyObject> triggered = null;

			boolean depLogging = DependencyManager.isLogging();

			// start following dependency
			if (depLogging) {
				DependencyManager.startLogDependency(this, depth);
			}

			// trigger the event of the dependent object/element
			NodeObject depObject = dependent.getObject();

			if (depObject != null) {
				int defaultChannelId = 0;

				if (depObject instanceof LocalizableNodeObject<?>) {
					LocalizableNodeObject<?> locObject = (LocalizableNodeObject<?>) depObject;
					Node channel = locObject.getChannel();

					if (channel == null) {
						channel = locObject.getOwningNode();
					}
					if (channel != null) {
						defaultChannelId = ObjectTransformer.getInt(channel.getId(), 0);
					}
				}
				// collect the channel ids for which the dependency must be triggered
				List<Integer> toTrigger = new ArrayList<Integer>();
				if (node != null) {
					toTrigger.add(ObjectTransformer.getInt(node.getId(), 0));
				} else {
					// trigger for all channels
					for (Integer channelId : channelIds) {
						toTrigger.add(channelId == 0 ? defaultChannelId : channelId);
					}
				}

				// trigger the dependencies (if not done before)
				for (Integer channelId : toTrigger) {
					triggered = getTriggeredDependencies(channelId);
					if (attributeDirting || !triggered.contains(dependent)) {
						// if the channel, for which we trigger the dependency is a master node
						// we get the dependent objects for channel 0
						int depChannelId = channelId;
						Node channel = t.getObject(Node.class, channelId);
						if (channel != null && !channel.isChannel()) {
							depChannelId = 0;
						}
						if (logger.isDebugEnabled()) {
							logger.debug(StringUtils.repeat("  ", depth) + "follow dep {" + this + "}");
						}
						final int finalDepChannelId = depChannelId;
						List<String> propList = storedDepProperties.entrySet().stream()
								.filter(entry -> entry.getValue().contains(finalDepChannelId)).map(entry -> entry.getKey())
								.collect(Collectors.toList());
						depObject.triggerEvent(dependent, (String[]) propList.toArray(new String[propList.size()]), eventMask, depth + 1,
								channelId);
						triggered.add(dependent);
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug(StringUtils.repeat("  ", depth) + "(dep {" + this + "})");
						}
					}
				}
			}

			// end following dependency
			if (depLogging) {
				DependencyManager.endLogDependency(this, depth);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#isNew()
		 */
		public boolean isNew() {
			return id == -1;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#isOld()
		 */
		public boolean isOld() {
			return !existing;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#isModified()
		 */
		public boolean isModified() {
			if (!mergedDeps.isEmpty()) {
				return true;
			}
			if (channelIdsChanged) {
				return true;
			}
			return !Objects.deepEquals(dependentProperties, storedDepProperties);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#setExisting()
		 */
		public void setExisting() {
			existing = true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getId()
		 */
		public int getId() {
			return id;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Dependency dep) {
			try {
				// first compare the source object
				int result = ObjectTransformer.compareObjects(source, dep.getSource(), true);

				// source objects are equal, so compare source properties
				if (result == 0) {
					result = ObjectTransformer.compareObjects(sourceProperty, dep.getSourceProperty(), true);
				}

				// source objects and properties are equal, so compare dependent
				// objects
				if (result == 0) {
					result = ObjectTransformer.compareObjects(dependent, dep.getDependent(), true);
				}

				return result;
			} catch (NodeException e) {
				logger.error("Error while comparing objects", e);
				return 0;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getMask()
		 */
		public int getMask() {
			return eventMask;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getDependentProperties()
		 */
		public Map<String, Set<Integer>> getDependentProperties() {
			return dependentProperties;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getStoredDependentProperties()
		 */
		public Map<String, Set<Integer>> getStoredDependentProperties() {
			return storedDepProperties;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#addDependentProperty(java.lang.String)
		 */
		public void addDependentProperty(int channelId, String property) {
			if (StringUtils.isEmpty(property)) {
				return;
			}
			dependentProperties.computeIfAbsent(property, key -> new TreeSet<>()).add(channelId);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#addChannelId(int)
		 */
		public void addChannelId(int channelId) {
			// check whether the list already contains the channel id
			int index = Collections.binarySearch(channelIds, channelId);
			if (index < 0) {
				// list does not contain the channel id, so add it in a way that
				// the list stays sorted
				channelIds.add(-index - 1, channelId);
				channelIdsChanged = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#addChannels(java.lang.String)
		 */
		public void addChannels(String depChannels) {
			if (!StringUtils.isEmpty(depChannels)) {
				String[] channels = depChannels.split(",");
				for (String channel : channels) {
					addChannelId(ObjectTransformer.getInt(channel, 0));
				}
			} else {
				addChannelId(0);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#removeChannelId(int)
		 */
		public void removeChannelId(int channelId) {
			int index = Collections.binarySearch(channelIds, channelId);
			if (index >= 0) {
				channelIds.remove(index);
				channelIdsChanged = true;

				dependentProperties.values().forEach(set -> set.remove(channelId));
				dependentProperties.entrySet().removeIf(entry -> entry.getValue().isEmpty());

				storedDepProperties.values().forEach(set -> set.remove(channelId));
				storedDepProperties.entrySet().removeIf(entry -> entry.getValue().isEmpty());
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#getChannelIds()
		 */
		public List<Integer> getChannelIds() {
			return channelIds;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.events.Dependency#merge(com.gentics.contentnode.events.Dependency)
		 */
		public void merge(Dependency dep) {
			for (Integer channel : dep.getChannelIds()) {
				addChannelId(channel);
			}
			mergedDeps.add(dep.getId());
		}

		/**
		 * Get the dependent properties for storing
		 * @return String representing the dependent properties
		 * @throws NodeException
		 */
		public String getDepPropForStoring() throws NodeException {
			if (dependentProperties.isEmpty()) {
				return null;
			} else {
				// when all values (channelId sets) are identical, we store the properties in the "condensed" form (comma separated list of attribute names)
				// only when the channelId sets are different, we use the JSON format
				if (dependentProperties.values().stream().distinct().count() > 1) {
					try {
						return mapper.writeValueAsString(dependentProperties);
					} catch (JsonProcessingException e) {
						throw new NodeException(e);
					}
				} else {
					Set<String> keys = dependentProperties.keySet();
					return StringUtils.merge((String[]) keys.toArray(new String[keys.size()]), ",");
				}
			}
		}

		/**
		 * Get the channel ids for storing
		 * @return channel ids for storing
		 */
		public String getChannelsForStoring() {
			return StringUtils.merge((Integer[]) channelIds.toArray(new Integer[channelIds.size()]), ",");
		}

		@Override
		public void preserveOtherDependentProperties(Integer channelId) {
			for (Entry<String, Set<Integer>> entry : storedDepProperties.entrySet()) {
				String prop = entry.getKey();
				Set<Integer> channelIds = entry.getValue();

				for (Integer id : channelIds) {
					if (Objects.equals(id, channelId)) {
						continue;
					}

					addDependentProperty(id, prop);
				}
			}
		}
	}

	/**
	 * Internal helper class for the dirt count
	 */
	public static class DirtCounter {

		/**
		 * Internal counter
		 */
		protected int count = 0;

		/**
		 * Increase the count
		 */
		public void inc() {
			count++;
		}

		/**
		 * Get the current count
		 * @return current count
		 */
		public int getCount() {
			return count;
		}
	}

	/**
	 * this is a helper class which will assist in cleaning up the dependency
	 * map. It's visibility is set to public since some functions are used
	 * inside the
	 * @see CnMapPublisher class too.
	 * @author clemens
	 */
	public static class DependencyMapCleaner {

		/**
		 * refers the object type the cleaner was created for
		 */
		private static int objType = 0;
    
		/**
		 * once CLEANUP_THRESHOLD is reached while adding ids to the cleanup
		 * list a cleanup will be executed to prevent SQL statements from
		 * growing too big
		 */
		private final static int CLEANUP_TRESHOLD = 100;
        
		/**
		 * this is our list of ids to be cleaned up
		 */
		private static List<Integer> ids = new Vector<Integer>(CLEANUP_TRESHOLD);

		/**
		 * cleanup dependencies in dependencymap2 table starting from timestamp
		 * @param timestamp
		 */
		public static void cleanupDependencies(int timestamp) throws NodeException {
			try {
				setCleanObjType(Page.TYPE_PAGE);
				// iterate through offline pages and remove the dependencies
				PublishQueue.getOfflinePages(new SQLExecutor() {
					public void handleResultSet(ResultSet rs) throws SQLException,
								NodeException {
						while (rs.next()) {
							addCleanId(rs.getInt("o_id"));
						}
					}
				});
				doCleanup();

				// now go for objects that have been deleted
				// pages
				setCleanObjType(Page.TYPE_PAGE); // just to be sure
				PublishQueue.getDeletedObjects(Page.TYPE_PAGE, new SQLExecutor() {

					/* (non-Javadoc)
					 * @see com.gentics.lib.db.SQLExecutor#handleResultSet(java.sql.ResultSet)
					 */
					public void handleResultSet(ResultSet rs) throws SQLException,
								NodeException {
						while (rs.next()) {
							addCleanId(rs.getInt("o_id"));
						}
					}
				});
				doCleanup();                

				// folders
				setCleanObjType(Folder.TYPE_FOLDER);
				PublishQueue.getDeletedObjects(Folder.TYPE_FOLDER, new SQLExecutor() {

					/* (non-Javadoc)
					 * @see com.gentics.lib.db.SQLExecutor#handleResultSet(java.sql.ResultSet)
					 */
					public void handleResultSet(ResultSet rs) throws SQLException,
								NodeException {
						while (rs.next()) {
							addCleanId(rs.getInt("o_id"));
						}
					}
				});
				doCleanup();                

				// files
				setCleanObjType(ContentFile.TYPE_FILE);
				PublishQueue.getDeletedObjects(ContentFile.TYPE_FILE, new SQLExecutor() {

					/* (non-Javadoc)
					 * @see com.gentics.lib.db.SQLExecutor#handleResultSet(java.sql.ResultSet)
					 */
					public void handleResultSet(ResultSet rs) throws SQLException,
								NodeException {
						while (rs.next()) {
							addCleanId(rs.getInt("o_id"));
						}
					}
				});
				doCleanup();
			} catch (NodeException e) {
				throw e;
			} catch (Exception e) {
				throw new NodeException("unable to clean dependencies", e);
			}
		}

		/**
		 * set obj_type which will be cleaned, and clears the list of ids to be
		 * cleaned to prevent id/obj_type confusion
		 * @param objType
		 */
		private static void setCleanObjType(int objType) {
			DependencyMapCleaner.objType = objType;
			ids.clear();
		}
    
		/**
		 * add an id to the list of objects to be cleaned. once the
		 * CLEANUP_TRESHOLD is met a cleanup will be executed to prevent SQL
		 * statements from growing too big. @see #doCleanup()
		 * @param id
		 */
		private static void addCleanId(int id) throws NodeException {
			ids.add(new Integer(id));
			if (ids.size() == CLEANUP_TRESHOLD) {
				doCleanup();
			}
		}
    
		/**
		 * finally we'll fiddle around within the database: execute the cleanup,
		 * delete dependencies from the database according to the list of ids
		 * and the objType currently set. will clear the list of ids afterwards.
		 */
		private static void doCleanup() throws NodeException {
			final int idSize = ids.size();

			if (0 == idSize) {
				if (NodeLogger.getLogger(DependencyMapCleaner.class).isInfoEnabled()) {
					NodeLogger.getLogger(DependencyMapCleaner.class).info("skipping dependencymap cleanup for objType {" + objType + "} as no ids are set");
				}
				return;
			}
            
			if (0 == objType) {
				NodeLogger.getLogger(DependencyMapCleaner.class).error("could not execute dependencymap cleanup since no objType has been set");
				return;
			}

			TransactionManager.execute(new Executable() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * com.gentics.lib.base.factory.TransactionManager.Executable
				 * #execute()
				 */
				public void execute() throws NodeException {
					String qMarks = StringUtils.repeat("?,", idSize);

					// cut the trailing ","
					qMarks = qMarks.substring(0, qMarks.length() - 1);
					String sql = "DELETE FROM dependencymap2 " + "WHERE (mod_obj_type = ? " + "AND mod_obj_id IN (" + qMarks
							+ ")) OR (dep_obj_type = ? AND dep_obj_id IN (" + qMarks + "))";
					int x = 0;
					PreparedStatement st = null;
					Transaction t = null;
					try {
						t = TransactionManager.getCurrentTransaction();

						st = t.prepareDeleteStatement(sql);
						// set the object type
						st.setInt(1, objType);
						st.setInt(2 + idSize, objType);
						// prepare statement
						for (Integer id : ids) {
							x++;
							st.setInt(1 + x, id.intValue());
							st.setInt(2 + idSize + x, id.intValue());
						}

						st.executeUpdate();
					} catch (Exception e) {
						throw new NodeException("error while executing cleanup", e);
					} finally {
						if (t != null) {
							t.closeStatement(st);
						}
					}
					ids.clear();
				}
			}, true);
		}
	}

	/**
	 * Internal helper method to add an xml attribute to a node
	 * @param out writer
	 * @param attributeName name of the attribute
	 * @param value value of the attribute
	 */
	private static void addAttribute(PrintWriter out, String attributeName, Object value) {
		out.print(" ");
		out.print(attributeName);
		out.print("=\"");
		out.print(value);
		out.print("\"");
	}

	/**
	 * start loggin a dependency
	 * @param dependency dependency
	 * @param depth current depth of nested nodes
	 */
	public static void startLogDependency(Dependency dependency, int depth) {
		PrintWriter out = loggingWriter.get();

		if (out != null) {
			try {
				checkDepthLevel(out, depth);
				Transaction t = TransactionManager.getCurrentTransaction();

				out.print("<dependency");

				addAttribute(out, "id", new Integer(dependency.getId()));

				DependencyObject source = dependency.getSource();

				addAttribute(out, "mod_obj_type", new Integer(t.getTType(source.getObjectClass())));
				addAttribute(out, "mod_obj_id", source.getObjectId());

				if (source.getElementId() != null) {
					addAttribute(out, "mod_ele_type", new Integer(t.getTType(source.getElementClass())));
					addAttribute(out, "mod_ele_id", source.getElementId());
				} else if (source.getElementClass() != null) {
					addAttribute(out, "mod_ele_type", new Integer(t.getTType(source.getElementClass())));
				}
				if (!StringUtils.isEmpty(dependency.getSourceProperty())) {
					addAttribute(out, "mod_prop", dependency.getSourceProperty());
				}

				DependencyObject dependent = dependency.getDependent();

				addAttribute(out, "dep_obj_type", new Integer(t.getTType(dependent.getObjectClass())));
				addAttribute(out, "dep_obj_id", dependent.getObjectId());

				addAttribute(out, "mask", Events.toString(dependency.getMask()));

				out.println(">");
			} catch (NodeException e) {}
		}
	}

	/**
	 * end logging a dependency
	 * @param dependency dependency
	 * @param depth current depth of nested nodes
	 */
	public static void endLogDependency(Dependency dependency, int depth) {
		PrintWriter out = loggingWriter.get();

		if (out != null) {
			// we need to close all nested events until reaching this level
			checkDepthLevel(out, depth + 1);
			out.println("</dependency>");
			setCurrentLogEventDepth(depth);
		}
	}

	/**
	 * Log an event
	 * @param object event object
	 * @param property event properties
	 * @param eventMask event mask
	 * @param depth current depth of nested events
	 */
	public static void logEvent(DependencyObject object, String[] property, int eventMask,
			int depth) {
		PrintWriter out = loggingWriter.get();

		if (out != null) {
			checkDepthLevel(out, depth);
			try {
				Transaction t = TransactionManager.getCurrentTransaction();

				out.print("<event");
				addAttribute(out, "mask", Events.toString(eventMask));
				if (!ObjectTransformer.isEmpty(property)) {
					addAttribute(out, "prop", StringUtils.merge(property, ","));
				}

				addAttribute(out, "obj_type", new Integer(t.getTType(object.getObjectClass())));
				addAttribute(out, "obj_id", object.getObjectId());

				if (object.getElementId() != null) {
					addAttribute(out, "ele_type", new Integer(t.getTType(object.getElementClass())));
					addAttribute(out, "ele_id", object.getElementId());
				} else if (object.getElementClass() != null) {
					addAttribute(out, "ele_type", new Integer(t.getTType(object.getElementClass())));
				}

				out.println(">");

				setCurrentLogEventDepth(depth + 1);
			} catch (NodeException e) {}
		}
	}

	/**
	 * Log dirting an object
	 * @param object dirted object
	 */
	public static void logObjectDirt(NodeObject object) {
		PrintWriter out = loggingWriter.get();

		if (out != null) {
			try {
				Transaction t = TransactionManager.getCurrentTransaction();

				out.print("<dirt");
				addAttribute(out, "obj_type", new Integer(t.getTType(object.getObjectInfo().getObjectClass())));
				addAttribute(out, "obj_id", object.getId());
				out.println("/>");
			} catch (NodeException e) {}
		}
	}

	/**
	 * Close all eventually still open event nodes until reaching the given level
	 * @param out writer
	 * @param level current level
	 */
	protected static void checkDepthLevel(PrintWriter out, int level) {
		int currentLevel = getCurrentLogEventDepth();

		if (currentLevel != level) {
			while (currentLevel > level) {
				out.println("</event>");
				currentLevel--;
			}

			setCurrentLogEventDepth(level);
		}
	}

	/**
	 * Get the current depth of nested events
	 * @return current event depth
	 */
	protected static int getCurrentLogEventDepth() {
		return ObjectTransformer.getInt(loggingEventDepth.get(), 0);
	}

	/**
	 * Set the current depth of nested events
	 * @param depth event depth
	 */
	protected static void setCurrentLogEventDepth(int depth) {
		loggingEventDepth.set(depth);
	}

	/**
	 * Get the id of the targeted dependency (in simulation mode), -1 otherwise
	 * @return id of the targeted dependency or -1
	 */
	public static int getTargetedSimulationDependencyId() {
		return ObjectTransformer.getInt(simulateDependency.get(), -1);
	}

	/**
	 * Set the object stack for the targeted dependency
	 * @param objectStack object stack
	 */
	public static void setDependencyObjectStack(Stack<NodeObject> objectStack) {
		// first create a copy of the stack
		Stack<NodeObject> copy = new Stack<NodeObject>();

		copy.addAll(objectStack);
		simulateDependencyStack.set(copy);
	}

	/**
	 * Get the object stack for the targeted dependency or null
	 * @return object stack or null
	 */
	public static Stack<NodeObject> getDependencyObjectStack() {
		return simulateDependencyStack.get();
	}

	/**
	 * Log the statistics into the given render result
	 * @param renderResult render result
	 * @throws NodeException
	 */
	public static void printStatistics(RenderResult renderResult) throws NodeException {
		if (renderResult != null && stats != null) {
			renderResult.info(Publisher.class, "Loading deps: " + stats.getCount + " in " + stats.getTime + " ms");
			renderResult.info(Publisher.class, "Clearing deps: " + stats.clearCount + " in " + stats.clearTime + " ms");
			renderResult.info(Publisher.class, "Storing deps: " + stats.storeCount + " in " + stats.storeTime + " ms");
		}
	}

	/**
	 * Get the number of currently prepared dependencies
	 * @return number of prepared dependencies
	 */
	public static int getPreparedDependencyCount() {
		if (preparedPageDependencies != null) {
			return preparedPageDependencies.size();
		} else {
			return 0;
		}
	}

	/**
	 * Prepare the dependencies for the given list of pages
	 * @param pages list of pages to prepare
	 * @throws NodeException
	 */
	public static void prepareDependencies(List<Integer> pageIds) throws NodeException {
		if (preparedPageDependencies != null && !ObjectTransformer.isEmpty(pageIds)) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Preparing dependencies for %s", pageIds));
			}
			preparedPageDependencies.putAll(loadDependenciesForObjects(Page.TYPE_PAGE, pageIds));
		}
	}

	/**
	 * Remove the prepared page dependencies for the given page
	 * @param page page for which the prepared dependencies shall be removed
	 */
	public static void removePreparedDependencies(Page page) {
		if (preparedPageDependencies != null) {
			Integer pageId = ObjectTransformer.getInteger(page.getId(), null);
			if (pageId != null) {
				preparedPageDependencies.remove(pageId);
			}
		}
	}

	/**
	 * Get the object ids of prepared source objects of the given class. All those objects will probably be rendered together with the objects,
	 * for which the dependencies have been prepared.
	 * @param objectClass class of the dependent object
	 * @return set of object ids (may be empty, but never null)
	 * @throws NodeException
	 */
	public static Set<Integer> getPreparedSourceObjectIds(Class<? extends NodeObject> objectClass) throws NodeException {
		Set<Integer> objectIds = new HashSet<Integer>();

		if (objectClass != null && preparedPageDependencies != null) {
			for (List<Dependency> depList : preparedPageDependencies.values()) {
				for (Dependency dep : depList) {
					DependencyObject sourceObject = dep.getSource();
					if (sourceObject != null) {
						if (objectClass.equals(sourceObject.getObjectClass())) {
							objectIds.add(ObjectTransformer.getInteger(sourceObject.getObjectId(), null));
						}
					}
				}
			}
		}
		// remove the null object
		objectIds.remove(null);
		return objectIds;
	}

	/**
	 * Clear all prepared dependencies
	 */
	public static void clearPreparedDependencies() {
		if (preparedPageDependencies != null) {
			preparedPageDependencies.clear();
		}
	}

	/**
	 * Get a map holding information, which file is used in which node
	 * @return map of file ID to set of channel IDs
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static Map<Integer, Set<Integer>> getFileUsageMap() throws NodeException {
		final Map<Integer, Set<Integer>> usageMap = new THashMap();
		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();
				PreparedStatement st = null;
				ResultSet res = null;

				try {
					// make this statement streaming (read rows one by one), by setting ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY and ...
					st = t.prepareStatement("SELECT DISTINCT mod_obj_id, dep_channel_id FROM dependencymap2 WHERE mod_obj_type = ? AND dep_obj_type IN (?, ?)",
							ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
					// .. fetchsize to Integer.MIN_VALUE
					st.setFetchSize(Integer.MIN_VALUE);
					st.setInt(1, File.TYPE_FILE);
					st.setInt(2, Page.TYPE_PAGE);
					st.setInt(3, Folder.TYPE_FOLDER);

					res = st.executeQuery();

					while (res.next()) {
						int fileId = res.getInt("mod_obj_id");
						String channels = ObjectTransformer.getString(res.getString("dep_channel_id"), "0");

						Set<Integer> nodeIds = usageMap.get(fileId);
						if (nodeIds == null) {
							nodeIds = new THashSet();
							usageMap.put(fileId, nodeIds);
						}

						int[] channelIds = StringUtils.splitInt(channels, ",");
						for (int channelId : channelIds) {
							nodeIds.add(channelId);
						}
					}
				} catch (SQLException e) {
					throw new NodeException("Error while building file usage map", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(st);
				}
			}
		});
		return usageMap;
	}

	/**
	 * Check whether the given file is used by another object (page, folder)
	 * @param usageMap The file usage map as gotten from com.gentics.contentnode.events.DependencyManager.getFileUsageMap()
	 * @param file file to check
	 * @param node node for which to check
	 * @return true if the file is used, false if not
	 * @throws NodeException
	 */
	public static boolean isFileUsed(Map<Integer, Set<Integer>> usageMap,
			com.gentics.contentnode.object.File file, Node node) throws NodeException {
		if (usageMap != null) {
			int fileId = ObjectTransformer.getInt(file.getChannelVariant(node).getId(), 0);
			int nodeId = ObjectTransformer.getInt(node.getId(), 0);
			if (!node.isChannel()) {
				nodeId = 0;
			}
			Set<Integer> nodeIds = usageMap.get(fileId);
			if (ObjectTransformer.isEmpty(nodeIds)) {
				return false;
			} else if (nodeId == 0) {
				// When the file lies in a master node, we need to check if it
				// is used in a channel that is not a channel of that node.
				Set<Integer> filteredIds = new HashSet<>(nodeIds);

				for (Node channel : node.getAllChannels()) {
					filteredIds.remove(channel.getId());
				}

				return !filteredIds.isEmpty();
			} else {
				return nodeIds.contains(nodeId);
			}
		} else {
			throw new NodeException("Error while checking whether " + file + " is used in " + node + ": file usage map was not generated");
		}
	}

	/**
	 * Internal helper class for collecting statistics
	 */
	protected static class Statistics {
		/**
		 * Number of clears
		 */
		protected long clearCount = 0;

		/**
		 * Total clear time
		 */
		protected long clearTime = 0;

		/**
		 * Number of get calls
		 */
		protected long getCount = 0;

		/**
		 * Total get time
		 */
		protected long getTime = 0;

		/**
		 * Number of store counts
		 */
		protected long storeCount = 0;

		/**
		 * Total store time
		 */
		protected long storeTime = 0;

		/**
		 * Create an instance
		 */
		public Statistics() {
		}

		/**
		 * Add clear time
		 * @param start start timestamp
		 */
		public synchronized void addClearTime(long start) {
			clearCount++;
			clearTime += System.currentTimeMillis() - start;
		}

		/**
		 * Add get time
		 * @param start start timestamp
		 */
		public synchronized void addGetTime(long start) {
			getCount++;
			getTime += System.currentTimeMillis() - start;
		}

		/**
		 * Add store time
		 * @param start start timestamp
		 */
		public synchronized void addStoreTime(long start) {
			storeCount++;
			storeTime += System.currentTimeMillis() - start;
		}
	}
}
