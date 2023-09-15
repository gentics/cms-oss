/*
 * @author Stefan Hepp
 * @date 24.12.2006
 * @version $Id: FilePublisher.java,v 1.41.4.1 2011-02-04 13:17:26 norbert Exp $
 */
package com.gentics.contentnode.publish;

import static com.gentics.contentnode.rest.util.MiscUtils.doBuffered;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.image.CNGenticsImageStore;
import com.gentics.contentnode.image.CNGenticsImageStore.ImageInformation;
import com.gentics.contentnode.image.GenticsImageStoreResult;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.Publisher.WrittenFile;
import com.gentics.contentnode.render.PublishRenderResult;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.db.ObjectRetrievalSQLExecutor;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.exception.PublishException;

/**
 * The Filepublisher writes the result of a publishrun into the filesystem.
 */
public class FilePublisher {

	private File publishDir;
	private File dbfilesDir;
	private NodeConfig config;
	private boolean useUtf8;

	private static NodeLogger logger = NodeLogger.getNodeLogger(FilePublisher.class);

	public static final String PUBDIR_NEW = "pub_new";
	private static final String PUBDIR = "pub";
	private static final String PUBDIR_OLD = "pub_old";
	private static final String PUBDIR_FAIL = "pub_fail";

	private FileUtils fileUtils = new FileUtilsImpl();
	private RenderResult renderResult;

	/**
	 * buffer used to write pages. (Yes, we are not thread safe.)
	 */
	private char[] buf = new char[4 * 1024];

	/**
	 * feature 'publish_node_resolve_direct' which simply means that the node_id
	 * is taken directly from the publish table, instead of looking for the
	 * right folder.
	 */
	private boolean publishResolveDirect = false;

	/**
	 * Map containing the image information for all images written into the filesystem
	 */
	protected Map<String, ImageInformation> allImageData = null;

	public FilePublisher(NodeConfig config, RenderResult renderResult) throws NodeException {
		this.config = config;
		this.renderResult = renderResult;
		initialize();
	}

	/**
	 * initialize all path-names.
	 * @throws PublishException
	 */
	private void initialize() throws NodeException {

		NodePreferences prefs = config.getDefaultPreferences();

		// Get publish directory ...
		publishDir = new File(ConfigurationValue.PUBLISH_PATH.get());

		dbfilesDir = new File(ConfigurationValue.DBFILES_PATH.get());
		useUtf8 = prefs.getFeature("utf8");

		renderResult.info(FilePublisher.class,
				"Initialized FilePublisher: publishDir: {" + publishDir.getAbsolutePath() + "}, dbfilesDir: {" + dbfilesDir.getAbsolutePath() + "}, useUtf8: {"
				+ useUtf8 + "}");

		if (config.getDefaultPreferences().getFeature("symlink_files") && !fileUtils.supportsSymlinks()) {
			renderResult.error(FilePublisher.class,
					"Feature symlink_files was activated, but loaded fileUtils class does not support symlinks ! {" + fileUtils.getClass().getName() + "}");
		}
		if (config.getDefaultPreferences().getFeature("hardlink_files") && !fileUtils.supportsSymlinks()) {
			renderResult.error(FilePublisher.class,
					"Feature hardlink_files was activated, but loaded fileUtils class does not support symlinks ! {" + fileUtils.getClass().getName() + "}");
		}
		if (config.getDefaultPreferences().getFeature("symlink_files") && config.getDefaultPreferences().getFeature("hardlink_files")) {
			renderResult.warn(FilePublisher.class, "Feature symlink_files and hardlink_files is both activated ! Using symlinks !");
		}

		publishResolveDirect = config.getDefaultPreferences().getFeature("publish_node_resolve_direct");
		if (publishResolveDirect) {
			renderResult.info(FilePublisher.class, "We will be using node_id directly from publish table instead of resolving through folder_id.");
		}
	}

	public File getPublishDir() {
		return publishDir;
	}

	public File getDbFilesDir() {
		return dbfilesDir;
	}

	/**
	 * initialize the filewriter for writing. create a new output directory.
	 * @return true, if the writer could be initialized, else false.
	 * @throws NodeException
	 */
	public boolean initializeWriter() throws NodeException {

		// create pub_new
		File newDir = new File(getPublishDir(), PUBDIR_NEW);

		if (logger.isInfoEnabled()) {
			logger.info("Checking output directory {" + newDir.getAbsolutePath() + "}.");
		}

		if (newDir.exists()) {
			// Hmm .. delete it.. (check for running pub first!)
			renderResult.info(FilePublisher.class, "Output directory {" + newDir.getAbsolutePath() + "} exists, deleting.");
			if (!deleteDir(newDir)) {
				logger.error("Error inizializing filewrite: pub_new not re-created!");
				throw new PublishException("Error inizializing filewrite: pub_new not re-created!");
			}
			renderResult.info(FilePublisher.class, "Deleted old output directory.");
		}

		if (!newDir.mkdirs()) {
			throw new PublishException("Error initializing filewrite: unable to create directory: {" + newDir.getAbsolutePath() + "}");
		}
		return true;
	}

	/**
	 * Get the number of pages to write into the filesystem
	 * @return
	 * @throws NodeException
	 */
	public int getPagesToWrite() throws NodeException {
		// get the node ids that have publishing into the filesystem enabled
		final IntegerColumnRetriever nodeIds = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM node WHERE publish_fs = 1 AND publish_fs_pages = 1", nodeIds);

		if (nodeIds.getValues().isEmpty()) {
			return 0;
		}

		ObjectRetrievalSQLExecutor executor = new ObjectRetrievalSQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int columnIndex = 1;
				for (Integer nodeId : nodeIds.getValues()) {
					stmt.setInt(columnIndex++, nodeId);
				}
			}
		};

		// NOTE: this SELECT statement is deliberately declared as UPDATE
		// statement (by executing the method DBUtils.executeUpdateStatement) to
		// have it done with the writing connection. Otherwise the data written
		// by the publish process would not be seen.
		DBUtils.executeUpdateStatement(
				"SELECT count(page_id) c FROM publish WHERE active = 1 AND node_id IN (" + StringUtils.repeat("?", nodeIds.getValues().size(), ",") + ")",
				executor);
		if (executor.retrievedValue()) {
			return executor.getIntegerValue();
		} else {
			renderResult.fatal(FilePublisher.class, "Error while trying to fetch number of pages to write.");
			return 0;
		}
	}

	/**
	 * Get collection of nodes which have publishing into the filesystem activated
	 * @return collection of nodes that are published into the filesystem
	 * @throws NodeException
	 */
	public Collection<Node> getNodesToWrite() throws NodeException {
		final IntegerColumnRetriever nodeIds = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM node WHERE publish_fs = 1", nodeIds);

		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObjects(Node.class, nodeIds.getValues());
	}

	/**
	 * write all pages into the filesystem.
	 * @param phase
	 * @param cnMapPublisher CnMapPublisher instance
	 */
	public void writePages(final IWorkPhase phase, final CnMapPublisher cnMapPublisher) throws NodeException {
		long startWritePage = System.currentTimeMillis();
		int pageCount = 0;
		boolean success = false;

		try {
			final IntegerColumnRetriever nodeIds = new IntegerColumnRetriever("id");
			DBUtils.executeStatement("SELECT id FROM node WHERE publish_fs = 1 AND publish_fs_pages = 1", nodeIds);

			if (nodeIds.getValues().isEmpty()) {
				return;
			}

			// NOTE: the SELECT statements are deliberately declared as UPDATE
			// statement (by executing the method DBUtils.executeUpdateStatement) to
			// have it done with the writing connection. Otherwise the data written
			// by the publish process would not be seen.

			// read the id's of the publish table entries.
			final List<Integer> publishIds = new ArrayList<Integer>();

			DBUtils.executeStatement(
					"SELECT id FROM publish WHERE active = 1 AND node_id IN (" + StringUtils.repeat("?", nodeIds.getValues().size(), ",") + ")",
					new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					int columnIndex = 1;
					for (Integer nodeId : nodeIds.getValues()) {
						stmt.setInt(columnIndex++, nodeId);
					}
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						publishIds.add(rs.getInt("id"));
					}
				}
			}, Transaction.UPDATE_STATEMENT);

			pageCount = publishIds.size();
			renderResult.info(FilePublisher.class, "Starting to write " + pageCount + " pages into filesystem");

			boolean niceUrls = NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS);

			doBuffered(publishIds, 100, idList -> {
				String bindParams = StringUtils.repeat("?", idList.size(), ",");
				PrepareStatement prep = ps -> {
					int pCounter = 0;
					for (int id : idList) {
						ps.setInt(++pCounter, id);
					}
				};

				// prepare alternate URLs
				Map<Integer, Set<String>> altUrlMap = new HashMap<>();
				if (niceUrls) {
					altUrlMap.putAll(DBUtils.select("SELECT publish_id, url FROM publish_alt_url WHERE publish_id IN ("+bindParams+")", prep, rs -> {
						Map<Integer, Set<String>> temp = new HashMap<>();
						while (rs.next()) {
							temp.computeIfAbsent(rs.getInt("publish_id"), key -> new HashSet<>()).add(rs.getString("url"));
						}
						return temp;
					}, Transaction.UPDATE_STATEMENT));
				}

				// now we write the sources into the filesystem
				DBUtils.select("SELECT publish.id, publish.source, publish.filename, publish.page_id, publish.path, "
						+ "publish.folder_id, publish.pdate, publish.node_id, publish.nice_url FROM publish WHERE id IN ("
						+ bindParams + ")", prep, rs -> {
							while (rs.next()) {
								// check whether publish process shall be interrupted
								PublishRenderResult.checkInterrupted();

								if (cnMapPublisher != null) {
									cnMapPublisher.keepContentmapsAlive();
								}

								int id = rs.getInt("id");
								Integer pageId = new Integer(rs.getInt("page_id"));
								Integer folderId = new Integer(rs.getInt("folder_id"));
								String filename = rs.getString("filename");
								String path = rs.getString("path");
								int pDate = rs.getInt("pdate");
								Reader source = rs.getCharacterStream("source");
								Integer nodeId = new Integer(rs.getInt("node_id"));
								if (rs.wasNull()) {
									nodeId = null;
								}

								Set<String> alternateAndNiceUrls = null;
								if (niceUrls) {
									alternateAndNiceUrls = new HashSet<>();
									String niceUrl = rs.getString("nice_url");
									if (niceUrl != null) {
										alternateAndNiceUrls.add(niceUrl);
									}
									alternateAndNiceUrls.addAll(altUrlMap.getOrDefault(id, Collections.emptySet()));
								}

								writePage(pageId, folderId, path, filename, pDate, source, nodeId, alternateAndNiceUrls);
								phase.doneWork();
							}
							return null;
						}, Transaction.UPDATE_STATEMENT);
			});

			success = true;
		} finally {
			if (success && pageCount > 0) {
				long duration = System.currentTimeMillis() - startWritePage;

				if (duration == 0) {
					duration = 1;
				}
				renderResult.info(FilePublisher.class,
						"Written " + pageCount + " pages into filesystem in " + duration + " ms (" + (pageCount * 1000 / duration) + " pages/sec, avg. "
						+ (duration / pageCount) + " ms/page)");
			}
		}
	}

	public int getFilesToWrite() throws NodeException {
		ObjectRetrievalSQLExecutor executor = new ObjectRetrievalSQLExecutor();

		DBUtils.executeStatement(
				"SELECT count(contentfile.id) c FROM contentfile, folder, node " + "WHERE contentfile.folder_id = folder.id AND folder.node_id = node.id "
				+ "AND node.publish_fs = 1 AND node.publish_fs_files = 1 AND contentfile.deleted = 0 AND folder.deleted = 0",
				executor);
		if (executor.retrievedValue()) {
			return executor.getIntegerValue();
		} else {
			renderResult.fatal(FilePublisher.class, "Error while trying to fetch number of files to write.");
			return 0;
		}
	}

	/**
	 * Write all dbfiles into the filesystem.
	 * @param phase publish phase
	 * @param cnMapPublisher CnMapPublisher instance
	 */
	public void writeFiles(IWorkPhase phase, CnMapPublisher cnMapPublisher) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences defaultPreferences = t.getNodeConfig().getDefaultPreferences();
		boolean tagImageResizer = defaultPreferences.isFeature(Feature.TAG_IMAGE_RESIZER);
		boolean niceUrls = defaultPreferences.isFeature(Feature.NICE_URLS);

		if (tagImageResizer) {
			allImageData = new HashMap<String, ImageInformation>();
		}

		// iterate over all nodes, which need to be written into the filesystem
		Collection<Node> nodesToWrite = getNodesToWrite().stream().filter(Node::doPublishFilesystemFiles).collect(Collectors.toList());
		Publisher.writeFiles(phase, nodesToWrite, allImageData, renderResult, niceUrls, Optional.of(this::writeFile), Optional.ofNullable(cnMapPublisher));
	}


	/**
	 * Use the GenticsImageStore to write all resized images into the filesystem
	 * @param phase current publish phase
	 * @param cnMapPublisher CnMapPublisher instance (must not be null)
	 * @throws NodeException
	 */
	public void writeTagImageResizer(IWorkPhase phase, CnMapPublisher cnMapPublisher) throws NodeException {
		NodePreferences prefs = config.getDefaultPreferences();

		if (!prefs.isFeature(Feature.TAG_IMAGE_RESIZER)) {
			logger.debug("Image Resizer not enabled.");
			return;
		}

		logger.debug("Invoking Image Resizer.");
		CNGenticsImageStore imageStore = new CNGenticsImageStore(config, TransactionManager.getCurrentTransaction());

		renderResult.info(FilePublisher.class, "Starting GenticsImageStore ...");
		long start = System.currentTimeMillis();

		CNGenticsImageStore.parseForImages(allImageData);
		long durationParse = System.currentTimeMillis() - start;
		renderResult.info(FilePublisher.class, "Parsing finished after " + durationParse + "ms");

		imageStore.cleanupImages();
		long durationCleanup = System.currentTimeMillis() - start - durationParse;
		renderResult.info(FilePublisher.class, "Cleanup finished after " + durationCleanup + "ms");

		GenticsImageStoreResult result = imageStore.renderImages(cnMapPublisher);
		long durationRender = System.currentTimeMillis() - start - durationCleanup - durationParse;
		renderResult.info(FilePublisher.class, "Rendering finished after " + durationRender + "ms");

		imageStore.createLinks(this);
		long durationLink = System.currentTimeMillis() - start - durationRender - durationCleanup - durationParse;
		renderResult.info(FilePublisher.class, "Linking finished after " + durationLink + "ms");

		long duration = System.currentTimeMillis() - start;
		renderResult.info(FilePublisher.class, "GenticsImageStore processed " + result.getTotal() + " images, " + result.getResized()
				+ " were not found in cache and needed to be resized");
		renderResult.info(FilePublisher.class, "GenticsImageStore duration: " + duration + " ms.");
		if (result.getResized() > 0) {
			renderResult.info(FilePublisher.class, "GenticsImageStore resized in avg: " + duration / result.getResized() + " ms/image");
		}
		renderResult.info(FilePublisher.class, "GenticsImageStore done.");
	}

	/**
	 * finalize filewrite, switch the output directories.
	 * @throws PublishException if an error happens during finalization..
	 */
	public void finalizeWriter(boolean success) throws PublishException {

		// move to pub or pub_fail
		File newDir = new File(getPublishDir(), PUBDIR_NEW);

		if (!newDir.exists()) {
			logger.error("Error finalizing filewrite: pub_new does not exist!");
			throw new PublishException("Error finalizing filewrite: pub_new does not exist!");
		}

		if (success) {

			File oldDir = new File(getPublishDir(), PUBDIR_OLD);

			if (oldDir.exists()) {
				if (!deleteDir(oldDir)) {
					logger.error("Error finalizing filewrite: could not delete pub_old!");
					throw new PublishException("Error finalizing filewrite: could not delete pub_old!");
				}
			}

			File pubDir = new File(getPublishDir(), PUBDIR);

			if (pubDir.exists()) {
				if (!pubDir.renameTo(oldDir)) {
					logger.error("Error finalizing filewrite: could not move pub to pub_old!");
					throw new PublishException("Error finalizing filewrite: could not move pub to pub_old!");
				}
			}

			if (!newDir.renameTo(pubDir)) {
				logger.error("Error finalizing filewrite: could not move pub_new to pub!");
				throw new PublishException("Error finalizing filewrite: could not move pub_new to pub!");
			}

		} else {

			File failDir = new File(getPublishDir(), PUBDIR_FAIL);

			if (failDir.exists()) {
				if (!deleteDir(failDir)) {
					logger.error("Error finalizing filewrite: could not delete pub_fail!");
					throw new PublishException("Error finalizing filewrite: could not delete pub_fail!");
				}
			}

			if (!newDir.renameTo(failDir)) {
				logger.error("Error finalizing filewrite: could not move pub_new to pub_fail!");
				throw new PublishException("Error finalizing filewrite: could not move pub_new to pub_fail!");
			}

		}

	}

	/**
	 * write a single page into the filesystem.
	 * @param pageId
	 * @param folderId
	 * @param path
	 * @param filename
	 * @param pDate
	 * @param source
	 * @param nodeId
	 * @param niceAndalternateUrls optional alternate or nice URLs
	 * @return true on success.
	 */
	private boolean writePage(Integer pageId, Integer folderId, String path, String filename, int pDate, Reader source, Integer nodeId, Set<String> niceAndalternateUrls) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (logger.isDebugEnabled()) {
			logger.debug("Writing Page {" + filename + "} into path: {" + path + "} ... ");
		}

		Node node = null;

		if (publishResolveDirect) {
			node = t.getObject(Node.class, nodeId);
			if (node == null) {
				logger.error("Unable to fetch node {" + nodeId + "} to publish page {" + pageId + "} let's try to fetch it through the folder_id");
			}
		}

		if (node == null) {
			Folder folder = t.getObject(Folder.class, folderId);

			if (folder == null) {
				// whoops, error, skip!
				throw new PublishException(
						"Unable to find folder with id {" + folderId + "} to publish page {" + pageId + "} - {" + filename
						+ "}. Perform one of the following actions on the page to solve this inconsistency: republish, delete or take offline.");
			}
			node = folder.getNode();
		}

		if (!node.doPublishFilesystem()) {
			logger.debug("Node {" + node.getHostname() + "} has publish into filesystem disabled.");
			return false;
		}

		String file = new StringBuffer(path).append(filename).toString();
		String fpath = new StringBuffer(getPublishDir().getAbsolutePath()).append(File.separator).append(PUBDIR_NEW).append(File.separator).append(path).toString();

		File filepath = new File(fpath);

		if (filepath.exists() && !filepath.isDirectory()) {
			logger.error("Could not write new page '" + file + "' [" + pageId + "]; path is not a valid directory.");
			throw new PublishException("Could not write new page {" + file + "} / {" + pageId + "} - path is not a valid directory.");
		}

		if (!filepath.exists()) {
			if (!filepath.mkdirs()) {
				logger.error("Could not write new page '" + file + "' [" + pageId + "]; path could not be created.");
				throw new PublishException(
						"Could not write new page {" + file + "} / {" + pageId + "} - path could not be created. {" + filepath.getAbsolutePath() + "}");
			}
		}

		File outFile = new File(filepath, filename);

		if (outFile.exists()) {
			logger.error("Could not write new page '" + file + "' [" + pageId + "]; file exists.");
			// ignore for now - same implementation as in PHP
			// throw new PublishException("Could not write new page {" + file + "} / {" + pageId + "} - file exists." + outFile.getAbsolutePath());
		}

		try {
			// Simply write UTF8 if utf8 is enabled.. or latin1 if disabled.. i see no use to check utf8 feature here...
			Charset charset;

			if (node.isUtf8()) {
				charset = Charset.forName("UTF8");
			} else {
				charset = getNonUTF8Encoding();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Writing file using charset: {" + charset + "}");
			}

			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), charset));

			int c;
			int counter = 0;

			while ((c = source.read(buf, 0, buf.length)) != -1) {
				out.write(buf, 0, c);
				counter += c;
			}

			out.close();

			if (logger.isDebugEnabled()) {
				logger.debug("Written {" + outFile.length() + "} bytes into {" + outFile.getCanonicalPath() + "} - read: {" + counter + "} characters");
			}

			if (pDate > 0) {
				if (!outFile.setLastModified(((long) pDate) * 1000)) {
					logger.warn("Could not set modification date for '" + file + "' [" + pageId + "].");
				}
			} else {
				logger.warn("Could not set modification date for '" + file + "' [" + pageId + "]; pDate {" + pDate + "}.");
			}

			if (!ObjectTransformer.isEmpty(niceAndalternateUrls)) {
				for (String niceUrl : niceAndalternateUrls) {
					File niceUrlFile = new File(new StringBuffer(getPublishDir().getAbsolutePath()).append(File.separator).append(PUBDIR_NEW).append(File.separator)
							.append(niceUrl).toString());
					niceUrlFile.getParentFile().mkdirs();

					// create a hardlink
					Files.createLink(FileSystems.getDefault().getPath(niceUrlFile.getAbsolutePath()), FileSystems.getDefault().getPath(outFile.getAbsolutePath()));
				}
			}

		} catch (IOException e) {
			logger.error("Could not write new file '" + file + "' [" + pageId + "].", e);
			throw new PublishException("Could not write new page {" + file + "} / {" + pageId + "}", e);
		}

		return true;
	}

	public static Charset getNonUTF8Encoding() {
		Charset charset;

		try {
			charset = Charset.forName("windows-1252");
		} catch (IllegalCharsetNameException e) {
			logger.error("Unable to retrieve charset windows-1252 - trying ISO-8859-1", e);
			charset = Charset.forName("ISO-8859-1");
		}
		return charset;
	}

	public static String getPath(boolean leadingSlash, boolean trailingSlash, String...segments) {
		StringBuilder path = new StringBuilder();

		for (String segment : segments) {
			if (ObjectTransformer.isEmpty(segment)) {
				continue;
			}
			if (path.length() > 0 && path.charAt(path.length() - 1) != '/') {
				if (!segment.startsWith("/")) {
					path.append("/");
				}
				path.append(segment);
			} else {
				if (segment.equals("/")) {
					continue;
				}
				if (segment.startsWith("/")) {
					path.append(segment.substring(1));
				} else {
					path.append(segment);
				}
			}
		}

		if (path.length() == 0) {
			if (leadingSlash || trailingSlash) {
				return "/";
			} else {
				return "";
			}
		}

		if (leadingSlash && path.charAt(0) != '/') {
			path.insert(0, "/");
		}
		if (trailingSlash && path.charAt(path.length() - 1) != '/') {
			path.append("/");
		}

		int startIndex = 0;
		int endIndex = path.length();
		if (!leadingSlash && path.charAt(0) == '/') {
			startIndex = 1;
		}
		if (!trailingSlash && path.charAt(path.length() - 1) == '/') {
			endIndex = path.length() - 1;
		}

		return path.substring(startIndex, endIndex);
	}

	/**
	 * write a file to the filesystem.
	 * @param file file data
	 * @return true on success.
	 * @throws PublishException if an error happens during file writing ..
	 */
	private boolean writeFile(WrittenFile file) throws PublishException, NodeException {
		return writeFile(file.fileId, file.filename, file.path, file.filesize, file.eDate, file.niceAndAlternateURLs);
	}

	/**
	 * write a file to the filesystem.
	 * @param fileId
	 * @param filename
	 * @param path
	 * @param filesize
	 * @param eDate
	 * @param niceAndalternateUrls optional set of nice and alternate URLs
	 * @return true on success.
	 * @throws PublishException if an error happens during file writing ..
	 */
	private boolean writeFile(int fileId, String filename, String path, int filesize, int eDate, Set<String> niceAndalternateUrls) throws PublishException {

		if (logger.isDebugEnabled()) {
			logger.debug("Writing file {" + filename + "} into {" + path + "} - expected filesize: {" + filesize + "}");
		}

		String file = new StringBuffer(path).append(filename).toString();
		String fpath = new StringBuffer(getPublishDir().getAbsolutePath()).append(File.separator).append(PUBDIR_NEW).append(File.separator).append(path).toString();

		File inFile = new File(getDbFilesDir(), fileId + ".bin");

		if (!inFile.exists() || !inFile.isFile()) {
			logger.error("Could not write new file '" + file + "' [" + fileId + "]; dbfile does not exist.");
			return false;
			// throw new PublishException("Could not write new file {" + file + "} / {" + fileId + "} - dbfile does not exist. {" + inFile.getAbsolutePath() + "}");
		}

		File filepath = new File(fpath);

		if (filepath.exists() && !filepath.isDirectory()) {
			logger.error("Could not write new file '" + file + "' [" + fileId + "]; path is not a valid directory.");
			throw new PublishException(
					"Could not write new file {" + file + "} / {" + fileId + "} - path is not a valid directory. {" + filepath.getAbsolutePath() + "}");
		}

		if (!filepath.exists()) {
			if (!filepath.mkdirs()) {
				logger.error("Could not write new file '" + file + "' [" + fileId + "]; path could not be created.");
				throw new PublishException("Could not write new file {" + file + "} / {" + fileId + "} - path could not be created.");
			}
		}

		File outFile = new File(filepath, filename);

		if (outFile.exists()) {
			logger.error("Could not write new file '" + file + "' [" + fileId + "]; file exists.");
			// do not throw exception for now (same implementation as in PHP)
			// throw new PublishException("Could not write new file {" + file + "} / {" + fileId + "} - file exists {" + outFile.getAbsolutePath() + "}");
		}

		try {
			if (fileUtils.supportsSymlinks()) {
				boolean success = false;

				if (config.getDefaultPreferences().getFeature("hardlink_files")) {
					success = fileUtils.createLink(inFile, outFile);
				} else if (config.getDefaultPreferences().getFeature("symlink_files")) {
					success = fileUtils.createSymlink(inFile, outFile);
				} else {
					fileUtils.createCopy(inFile, outFile);
				}
				if (!success) {
					logger.warn("Unable to create link for file - trying to create copy.");
					fileUtils.createCopy(inFile, outFile);
				}
			} else {
				fileUtils.createCopy(inFile, outFile);
			}

			if (!ObjectTransformer.isEmpty(niceAndalternateUrls)) {
				for (String url : niceAndalternateUrls) {
					File niceUrlFile = new File(new StringBuffer(getPublishDir().getAbsolutePath()).append(File.separator).append(PUBDIR_NEW).append(File.separator)
							.append(url).toString());
					niceUrlFile.getParentFile().mkdirs();

					// create a hardlink
					Files.createLink(FileSystems.getDefault().getPath(niceUrlFile.getAbsolutePath()), FileSystems.getDefault().getPath(outFile.getAbsolutePath()));
				}
			}

		} catch (IOException e) {
			throw new PublishException("Error while writing file {" + file + "} / {" + fileId + "}", e);
		}

		if (eDate > 0) {
			if (!outFile.setLastModified(((long) eDate) * 1000)) {
				logger.warn("Could not set modification date for '" + file + "' [" + fileId + "].");
			}
		} else {
			logger.warn("Could not set modification date for '" + file + "' [" + fileId + "]; eDate {" + eDate + "}.");
		}

		return true;
	}

	/**
	 * delete a directory and all its content.
	 * @param dir directory to delete.
	 * @return true on success, else false.
	 */
	private boolean deleteDir(File dir) {
		try {
			return fileUtils.deleteDirectory(dir);
		} catch (IOException e) {
			logger.error("Error while deleting driectory {" + dir + "}", e);
			return false;
		}
	}
}
