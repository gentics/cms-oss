 /*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ContentNodeFactory.java,v 1.19.4.1 2011-02-10 13:43:37 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.ConstructFactory;
import com.gentics.contentnode.factory.object.ContentRepositoryFactory;
import com.gentics.contentnode.factory.object.DatasourceEntryFactory;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.factory.object.MarkupLanguageFactory;
import com.gentics.contentnode.factory.object.ObjectTagDefinitionFactory;
import com.gentics.contentnode.factory.object.OverviewFactory;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.factory.object.PartFactory;
import com.gentics.contentnode.factory.object.PublishWorkflowFactory;
import com.gentics.contentnode.factory.object.RegexFactory;
import com.gentics.contentnode.factory.object.RoleFactory;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.factory.object.TagFactory;
import com.gentics.contentnode.factory.object.TemplateFactory;
import com.gentics.contentnode.factory.object.UserGroupFactory;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.factory.object.ValueFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;

/**
 * The contentnodefactory is a wrapper for a nodefactory connection. A new instance
 * of the contentnodefactory has also a new connection to the factory. Like connections,
 * you should use a seperate contentnodefactory instance for every thread.
 *
 * Currently, each factory creates a new instance for each connection, so that the
 * caches can be cleared without influence on other threads.
 * 
 * TODO: think about this class, why is it needed?
 */
public class ContentNodeFactory {

	private final NodeFactory factory;

	private ContentNodeFactory(NodeFactory factory) {
		this.factory = factory;
	}

	/**
	 * get a new instance of the factory.
	 * @return A new instance of the factory
	 */
	public synchronized static ContentNodeFactory getInstance() {
		// this is a bit of a hack: as editmode is not yet implemented, we create a new factory for each access.
		NodeFactory nodeFactory = NodeFactory.getInstance();
		ContentNodeFactory factory = new ContentNodeFactory(nodeFactory);

		if (!nodeFactory.isInitialized()) {
			nodeFactory.initialize();
			// as each time a new nodefactory-instance is created, this is done every time.
			factory.registerFactories();

			factory.factory.initCaches();
		}
		return factory;
	}

	/**
	 * Register all objectfactories to the nodefactory.
	 * Be careful when adding new factories. Factories that produce objects of other factories should be registred after that factory.
	 * For example the PageFactory should be registered before the FolderFactory.
	 */
	private void registerFactories() {
		factory.registerObjectFactory(new ValueFactory());
		factory.registerObjectFactory(new PartFactory());
		factory.registerObjectFactory(new ConstructFactory());
		factory.registerObjectFactory(new ObjectTagDefinitionFactory());
		factory.registerObjectFactory(new TagFactory());
		factory.registerObjectFactory(new LanguageFactory());
		factory.registerObjectFactory(new OverviewFactory());
		factory.registerObjectFactory(new MarkupLanguageFactory());
		factory.registerObjectFactory(new SystemUserFactory());
		factory.registerObjectFactory(new DatasourceEntryFactory());
		factory.registerObjectFactory(new TemplateFactory());
		factory.registerObjectFactory(new FileFactory());
		factory.registerObjectFactory(new PageFactory());
		factory.registerObjectFactory(new FormFactory());
		factory.registerObjectFactory(new FolderFactory());
		factory.registerObjectFactory(new UserLanguageFactory());
		factory.registerObjectFactory(new UserGroupFactory());
		factory.registerObjectFactory(new PublishWorkflowFactory());
		factory.registerObjectFactory(new ContentRepositoryFactory());
		factory.registerObjectFactory(new RegexFactory());
		factory.registerObjectFactory(new RoleFactory());
		factory.registerObjectFactory(new SchedulerFactory());
	}

	/**
	 * Get the {@link NodeFactory} instance
	 * @return NodeFactory
	 */
	public NodeFactory getFactory() {
		return factory;
	}

	/**
	 * Reloads the configuration
	 */
	public void reloadConfiguration() {
		factory.reloadConfiguration();
	}

	/**
	 * Get the mapped ttype for a class.
	 * @param clazz the class of an object.
	 * @return the ttype for the given class, or 0 if not mapped.
	 */
	public int getTType(Class<? extends NodeObject> clazz) {
		return factory.getTType(clazz);
	}

	/**
	 * get the class for a ttype.
	 * @param objType the requested ttype.
	 * @return the registered class for the ttype, or null if the ttype is unknown.
	 */
	public Class<? extends NodeObject> getClass(int objType) {
		return factory.getClass(objType);
	}

	/**
	 * Get the page with given id
	 * @param id id of the page
	 * @return page or null if page not found
	 * @throws NodeException
	 */
	public Page getPage(Integer id) throws NodeException {
		return (Page) TransactionManager.getCurrentTransaction().getObject(Page.class, id);
	}

	/**
	 * Get the construct with given id
	 * @param id id of the construct
	 * @return construct or null if construct not found
	 * @throws NodeException
	 */
	public Construct getConstruct(Integer id) throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Construct.class, id);
	}

	/**
	 * Get the file with given id
	 * @param id id of the file
	 * @return file or null if file not found
	 * @throws NodeException
	 */
	public ImageFile getFile(Integer id) throws NodeException {
		return (ImageFile) TransactionManager.getCurrentTransaction().getObject(File.class, id);
	}

	/**
	 * Get the folder with given id
	 * @param id id of the folder
	 * @return folder or null if folder not found
	 * @throws NodeException
	 */
	public Folder getFolder(Integer id) throws NodeException {
		return (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, id);
	}

	/**
	 * Get the node with given id
	 * @param id id of the node
	 * @return node or null if node not found
	 * @throws NodeException
	 */
	public Node getNode(Integer id) throws NodeException {
		return (Node) TransactionManager.getCurrentTransaction().getObject(Node.class, id);
	}

	/**
	 * Get the part with given id
	 * @param id id of the part
	 * @return part or null if part not found
	 * @throws NodeException
	 */
	public Part getPart(Integer id) throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Part.class, id);
	}

	/**
	 * Get the template with given id
	 * @param id id of the template
	 * @return template or null if template not found
	 * @throws NodeException
	 */
	public Template getTemplate(Integer id) throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Template.class, id);
	}

	/**
	 * Get the value with given id
	 * @param id id of the value
	 * @return value or null if value not found
	 * @throws NodeException
	 */
	public Value getValue(Integer id) throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Value.class, id);
	}

	/**
	 * Get a nodeobject with given class and id
	 * @param clazz class of the nodeobject
	 * @param id id of the nodeobject
	 * @return nodeobject or null if nodeobject not found
	 * @throws NodeException
	 */
	public <T extends NodeObject> T getObject(Class<T> clazz, Integer id) throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(clazz, id);
	}

	/**
	 * Create and start a new transaction. Set the transaction as the current
	 * @param sessionId sessionId of the user which is associated with the transaction
	 * @param userId userId of the user that belongs to the session specified with sessionId
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @return new current transaction
	 * @throws NodeException
	 */
	public Transaction startTransaction(String sessionId, Integer userId, boolean useConnectionPool) throws NodeException {
		Transaction t = factory.createTransaction(sessionId, userId, useConnectionPool);

		TransactionManager.setCurrentTransaction(t);
		return t;
	}
    
	/**
	 * Create and start a new transaction. Set the transaction as the current
	 * @param sessionId sessionId of the user which is associated with the transaction
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @return new current transaction
	 * @throws NodeException
	 */
	public Transaction startTransaction(String sessionId, boolean useConnectionPool) throws NodeException {
		Transaction t = factory.createTransaction(sessionId, useConnectionPool);

		TransactionManager.setCurrentTransaction(t);
		return t;
	}
    
	/**
	 * Create and start a new transaction. Set the transaction as the current
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @return new current transaction
	 * @throws NodeException
	 */
	public Transaction startTransaction(boolean useConnectionPool) throws NodeException {
		Transaction t = factory.createTransaction(useConnectionPool);

		TransactionManager.setCurrentTransaction(t);
		return t;
	}
    
	/**
	 * Create and start a new transaction. Set the transaction as the current
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @param multiconnection true if you want a transaction which can be accessed from multiple threads
	 * @return new current transaction
	 * @throws NodeException
	 */
	public Transaction startTransaction(boolean useConnectionPool, boolean multiconnection) throws NodeException {
		Transaction t = factory.createTransaction(useConnectionPool, multiconnection);

		TransactionManager.setCurrentTransaction(t);
		return t;
	}

	/**
	 * Creates a new transaction and passes it on to the given handler.
	 * 
	 * <p>
	 * If there is already a currently active transaction, it will be restored before this method returns.
	 * 
	 * <p>
	 * The new transaction will not be set as the current transaction, but some of the transactions methods,
	 * for example {@link Transaction#getObject(Class, Object)}, may set the new transaction as current.
	 * 
	 * <p>
	 * The transaction will be automatically committed after the give WithTransction handler returns. If
	 * an exception occurs, the transaction will be rollbacked.
	 * 
	 * @param withTransaction
	 * 		  A callback that will be called with the new transaction.
	 * 
	 * @param useConnectionPool
	 * 		  Whether to use transaction pooling.
	 * 		  @see NodeFactory#createTransaction(boolean, boolean)
	 * 
	 * @param multiConnection
	 * 		  Whether to allocate a threadsafe transaction.
	 * 		  @see NodeFactory#createTransaction(boolean, boolean)
	 * 
	 * @throws TransactionException
	 * 		  If an error occurs during commit.
	 * 		  Also, any TransactionException from the WithTransaction callback are passed through unchanged.
	 * 
	 * @throws NodeException
	 * 		  If the WithTransaction callback throws a NodeException.
	 */
	public void withTransaction(WithTransaction withTransaction, boolean useConnectionPool, boolean multiConnection) throws NodeException {
		Transaction currentTransaction = null;

		try {
			currentTransaction = TransactionManager.getCurrentTransaction();
		} catch (TransactionException e) {}
		Transaction newTransaction = factory.createTransaction(useConnectionPool, multiConnection);

		try {
			withTransaction.withTransaction(newTransaction);
			newTransaction.commit();
		} catch (NodeException e) {
			newTransaction.rollback();
			throw e;
		} catch (RuntimeException e) {
			newTransaction.rollback();
			throw e;
		} finally {
			if (null != currentTransaction) {
				TransactionManager.setCurrentTransaction(currentTransaction);
			}
		}
	}

	/**
	 * Start the dirt queue worker
	 * @throws NodeException
	 */
	public void startDirtQueueWorker() throws NodeException {
		factory.startDirtQueueWorker();
	}

	/**
	 * Stop the dirt queue worker
	 * @throws NodeException
	 */
	public void stopDirtQueueWorker() throws NodeException {
		factory.stopDirtQueueWorker();
	}

	/**
	 * A callback interface used by the {@link #withTransaction(Transaction)} method.
	 */
	public interface WithTransaction {

		/**
		 * Will be invoked with a new transaction.
		 * 
		 * <p>
		 * If this method throws an Exception, the transaction will be automatically rollbacked.
		 * 
		 * @param t
		 * 		  A new transaction that will be automatically committed after this method returns.
		 */
		void withTransaction(Transaction t) throws NodeException;
	}
}
