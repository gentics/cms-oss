/*
 * @author norbert
 * @date 11.11.2009
 * @version $Id: CnMapPublishHandler.java,v 1.2 2010-01-29 15:18:46 norbert Exp $
 */
package com.gentics.api.contentnode.publish;

import java.util.Map;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;

/**
 * Interface for specific implementations that add additional functionality to
 * publish into a ContentRepository. Implementations of this interface can be
 * hooked into the publish process for specific ContentRepositories. The lifecycle is as follows:
 * <ol>
 *   <li>{@link #init(Map)} Called once during the initialization process of the publish handler to take the instance into service</li>
 *   <li>{@link #open(long)} Called once for every publish handler when a transaction is started to write into the content repository</li>
 *   <li>{@link #createObject(Resolvable)}, {@link #updateObject(Resolvable)}, {@link #deleteObject(Resolvable)} Repeatedly called for every created, updated, deleted object during the publish process or instant publishing</li>
 *   <li>{@link #commit()} OR {@link #rollback()} Called once for every publish handler at the end of every transaction (commit on success, rollback on failure)</li>
 *   <li>{@link #close()} Called once for every publish handler at the end of every transaction</li>
 *   <li>{@link #destroy()} Called once during shutdown to take the publish handler out of service</li>
 * </ol>
 * When any of that methods throws a CnMapPublishException, the publish process will fail (and the message of the exception will be shown in the publish log).
 * <br/>
 * When instant publishing is not used for the content repository, the publish handlers are only used during a publish process and the whole publish process is
 * done in a single transaction.
 * <br/>
 * When instant publishing is used for the content repository, the publish handlers are also used for every object, which is instantly published into the content repository (which is done in a transaction).
 * Additionally, the publish process will use a new transaction for every object it publishes.
 * <br/>
 * Once a publish handler is initialized for a content repository, it will stay in service until either
 * <ol>
 * <li>The server is shut down OR</li>
 * <li>The publish handler entry is removed or modified. In this case, the existing instance will be taken out of service and a new instance will be created and initialized.</li>
 * </ol>
 */
public interface CnMapPublishHandler {
	/**
	 * Constant for the key of the parameter passed to the {@link #init(Map)}
	 * method holding the datasource ID Implementations can use this datasource
	 * ID to get a datasource instance via
	 * {@link PortalConnectorFactory#createDatasource(String)} or
	 * {@link PortalConnectorFactory#createDatasource(Class, String)}.
	 */
	final static String DS_ID = "gtx_dsid";

	/**
	 * This method is called once for every configured instance to take the
	 * handler into service. Implementations may read external configuration,
	 * and initialize needed resources.
	 * @param parameters of parameters given in the configuration
	 * @throws CnMapPublishException when the initialization of the handler
	 *         failed with an unrecoverable error
	 */
	void init(Map parameters) throws CnMapPublishException;

	/**
	 * This method is called once for every configured instance for each publish
	 * run. Implementations may open connections, prepare resources, etc. The
	 * handle methods {@link #createObject(Resolvable)},
	 * {@link #updateObject(Resolvable)} and {@link #deleteObject(Resolvable)}
	 * will only be called after calling this method and until one of the
	 * methods {@link #commit()} or {@link #rollback()} is called to mark the
	 * (successful or not successful) end of the publish process.
	 * @param timestamp timestamp of the publish process
	 * @throws CnMapPublishException when an unrecoverable error occurred
	 */
	void open(long timestamp) throws CnMapPublishException;

	/**
	 * This method is called for every object, which is created in the
	 * ContentRepository (was created/published since the last publish process
	 * into the respective ContentRepository).
	 * @param object resolvable object containing all data that are written into
	 *        the ContentRepository
	 * @throws CnMapPublishException when an unrecoverable error occurred
	 */
	void createObject(Resolvable object) throws CnMapPublishException;

	/**
	 * This method is called for every object, which needs to be updated in the
	 * ContentRepository (was changed since the last publish process).
	 * @param object resolvable object containing all data that are written into
	 *        the ContentRepository
	 * @throws CnMapPublishException when an unrecoverable error occurred
	 */
	void updateObject(Resolvable object) throws CnMapPublishException;

	/**
	 * This method is called for every object, which is removed from the ContentRepository (was deleted or taken offline since the last publish run).
	 * @param object resolvable object containing what? TODO does it contain all data from the ContentRepository or only the contentid?
	 * @throws CnMapPublishException when an unrecoverable error occurred
	 */
	void deleteObject(Resolvable object) throws CnMapPublishException;

	/**
	 * This method is called at the end of a successful publish process
	 */
	void commit();

	/**
	 * This method is called at the end of a failed publish process
	 */
	void rollback();

	/**
	 * This method is called once at the end of any publish process.
	 * Implementations should free all resources and close all connections which
	 * were allocated/opened in {@link #open(long)}.
	 */
	void close();

	/**
	 * This method is called at the end of the lifetime of this handler
	 * instance. Implementations should close all connections and free all
	 * resources which were allocated in {@link #init(Map)}.
	 */
	void destroy();
}
