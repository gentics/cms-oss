/*
 * @author herbert
 * @date 02.03.2007
 * @version $Id: GenticsImageStore.java,v 1.13.4.1 2011-02-04 13:17:26 norbert Exp $
 */
package com.gentics.contentnode.image;

import static com.gentics.contentnode.factory.Trx.execute;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Hex;
import org.apache.jcs.JCS;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.upload.FileInformation;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.HandleDependenciesTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.PublishRenderResult;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.cache.JCSPortalCache;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.image.GenticsImageStore;
import com.gentics.lib.image.GenticsImageStoreResizeResponse;
import com.gentics.lib.image.ResizeFilter;
import com.gentics.lib.image.SmarterResizeFilter;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * Class which i extracted from GenticsImageStoreServlet and removed (most)
 * servlet dependencies - so it can be used within the javaparser
 *
 * @author herbert
 */
public class CNGenticsImageStore extends GenticsImageStore {


	/**
	 * base path of the GenticsImageStore
	 */
	public final static String IMAGESTORE_BASEPATH = "/GenticsImageStore";

	protected boolean useEDate = true;

	/**
	 * Regular expression
	 */
	private final static String CDATA_REG_EXP = "(?i)^cdata\\s*\\[.*?";

	/**
	 * Regular expression to determine if the string matches an CSS url attribute.
	 * (ex: "background: url(/GenticsImageStore/)")
	 */
	private static final String CSS_URL_ATTRIBUTE_REG_EXPR = "(?i)^url\\s*\\(.*?";

	public final static Pattern TRANSFORM_PATTERN = Pattern.compile("(?<width>[0-9]+|auto)/(?<height>[0-9]+|auto)/"+
	"(?:(?<mode>prop|force|smart)|cropandresize/(?<cropmode>prop|force|smart)/(?<tlx>[0-9]+)/(?<tly>[0-9]+)/(?<cw>[0-9]+)/(?<ch>[0-9]+))");

	/**
	 * Finds Imagestore URLs
	 */
	public final static Pattern SANE_IMAGESTORE_URL_PATTERN = Pattern
			.compile("(?:(?:https?:)?//(?:[^@:/]+(?::[^@/]+)?@)?(?<host>[^/@:\"']+))?/GenticsImageStore/(?<transform>" + TRANSFORM_PATTERN.toString() + ")(?=/)"
					+ "(?<imageurl>[^\"'\\s<>]+)");

	/**
	 * flag to mark job as being successful (or not)
	 */
	protected boolean success = true;

	/**
	 * last error message
	 */
	protected String errorMessage = null;

	private NodeConfig config;

	private Transaction transaction;

	private File storageDirectory;

	private GenticsImageStoreResult imageStoreResult;

	public final static String JPEG_QUALITY_CONF = "image_resizer_jpeg_quality";

	public CNGenticsImageStore(NodeConfig config) {
		this(config, null);
	}

	public CNGenticsImageStore(NodeConfig config, Transaction transaction) {
		this.config = config;
		this.transaction = transaction;
		this.storageDirectory = new File(ConfigurationValue.GIS_PATH.get());
		// set the jpeg quality (if configured)
		Double jpegQuality = ObjectTransformer.getDouble(config.getDefaultPreferences().getProperty(JPEG_QUALITY_CONF), null);
		setJPEGQuality(jpegQuality);
	}

	/**
	 * Set whether the EDate shall be used in the cache key or not
	 * @param useEDate true if the edate shall be used, false if tno
	 */
	public void setUseEDate(boolean useEDate) {
		this.useEDate = useEDate;
	}

	/**
	 * Fetches all information from nodes
	 *
	 * @return a map of nodes. The key will be the host of the node. The value
	 *         will contain the node object of this node
	 * @throws NodeException
	 */
	public static Map<String, Node> fetchNodeInformation() throws NodeException {
		// get all node ids
		final List<Integer> nodeIds = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT id FROM node", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}
			}
		});

		// get the node objects
		List<Node> nodes = TransactionManager.getCurrentTransaction().getObjects(Node.class, nodeIds);
		Map<String, Node> nodeMap = new HashMap<String, Node>();

		for (Node node : nodes) {
			nodeMap.put(node.getHostname(), node);
		}

		return nodeMap;
	}

	/**
	 * Do the resizing of an image and return the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param cropandresize whether the image shall be cropped and resized
	 * @param topleftx x coordinate of the top left corner for cropping
	 * @param toplefty y coordinate of the top left corner for cropping
	 * @param cropwidth width for cropping
	 * @param cropheight height for cropping
	 * @param fileUrlForCacheKey file id as string
	 * @param queryString query string (may be null)
	 * @param cookies array of cookies to be added when fetching the image from the URL (may be null or empty)
	 * @return the fileinformation of the resized image
	 * @throws ServletException
	 * @throws IOException
	 * @throws NodeException
	 */
	public FileInformation doResizing(String mode, String width, String height, boolean cropandresize, String topleftx, String toplefty,
			String cropwidth, String cropheight, String fileUrlForCacheKey, String queryString, Cookie[] cookies, Map<String, String> headers) throws ServletException, IOException,
				NodeException {
		// first try to get the image from the cache
		Object cacheKey = null;
		if (cropandresize) {
			if (useEDate) {
				cacheKey = createCropCacheKey(fileUrlForCacheKey, mode, width, height, topleftx, toplefty, cropwidth, cropheight);
			} else {
				cacheKey = createCropCacheKey(fileUrlForCacheKey, mode, width, height, topleftx, toplefty, cropwidth, cropheight, 0);
			}
		} else {
			if (useEDate) {
				cacheKey = createCacheKey(fileUrlForCacheKey, mode, width, height);
			} else {
				cacheKey = createCacheKey(fileUrlForCacheKey, mode, width, height, 0);
			}
		}

		String imageUrl = config.getDefaultPreferences().getProperty("contentnode.url");

		return doResizing(cacheKey, mode, width, height, cropandresize, topleftx, toplefty, cropwidth, cropheight, imageUrl, fileUrlForCacheKey, queryString, cookies,
				headers);
	}

	@Override
	protected void putImageIntoCache(FileInformation resizedImage, Object cacheKey) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Putting resized image into cache - key {%s}", cacheKey));
		}
		super.putImageIntoCache(resizedImage, cacheKey);
	}

	/**
	 * Do the resizing of an image and return the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param cropandresize whether the image shall be cropped and resized
	 * @param topleftx x coordinate of the top left corner for cropping
	 * @param toplefty y coordinate of the top left corner for cropping
	 * @param cropwidth width for cropping
	 * @param cropheight height for cropping
	 * @param image image
	 * @return the fileinformation of the resized image
	 * @throws ServletException
	 * @throws IOException
	 * @throws NodeException
	 */
	public FileInformation doResizing(String mode, String width, String height, boolean cropandresize, String topleftx, String toplefty,
			String cropwidth, String cropheight, ImageFile image) throws ServletException, IOException,
				NodeException {
		// first try to get the image from the cache
		Object cacheKey = null;
		if (cropandresize) {
			if (useEDate) {
				cacheKey = createCropCacheKey(Integer.toString(image.getId()), mode, width, height, topleftx, toplefty, cropwidth, cropheight);
			} else {
				cacheKey = createCropCacheKey(Integer.toString(image.getId()), mode, width, height, topleftx, toplefty, cropwidth, cropheight, 0);
			}
		} else {
			if (useEDate) {
				cacheKey = createCacheKey(Integer.toString(image.getId()), mode, width, height);
			} else {
				cacheKey = createCacheKey(Integer.toString(image.getId()), mode, width, height, 0);
			}
		}

		// Verify width/height parameters ...
		// Either two integers, or one integer and one 'auto'
		Integer widthInt = ObjectTransformer.getInteger(width, null);
		Integer heightInt = ObjectTransformer.getInteger(height, null);

		if (widthInt == null || heightInt == null) {
			if ((widthInt == null && heightInt == null) || (!"auto".equals(width) && !"auto".equals(height))) {
				throw new IllegalArgumentException(
						"Invalid width/height - One has to be a valid integer while the other might be 'auto'. width: {" + width + "} height: {" + height + "}");
			}
		}

		// get image from cache
		if (cacheKey == null) {
			logger.warn("Could not create cachekey for image {" + image + "}, not using cache.");
		} else {
			FileInformation cachedImage = getCachedImage(cacheKey);
			if (cachedImage != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Image was fetched from cache - key {" + cacheKey + "}");
				}
				// image was fetched from the cache
				return cachedImage;
			}
		}

		FileInformation resizedImage = execute(img -> invokeGIS(mode, width, height, cropandresize, topleftx, toplefty, cropwidth,
				cropheight, img), image);

		// put the resized image into the cache
		putImageIntoCache(resizedImage, cacheKey);
		return resizedImage;
	}

	/**
	 * Renders all images from the imagestoreimage table that are not up-to-date.
	 * @param publisher gets kept alive if specified
	 * @return information about processed images
	 * @throws NodeException
	 */
	public GenticsImageStoreResult renderImages(CnMapPublisher publisher) throws NodeException {
		imageStoreResult = new GenticsImageStoreResult(0, 0);
		Transaction t = TransactionManager.getCurrentTransaction();
		final Map<Integer, ImageDescription> toUpdate = new HashMap<Integer, CNGenticsImageStore.ImageDescription>();
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT ii.id, ii.contentfile_id, ii.transform FROM imagestoreimage ii LEFT JOIN contentfile cf ON cf.id = ii.contentfile_id WHERE IFNULL(ii.hash_orig <> cf.md5, TRUE)", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					toUpdate.put(rs.getInt("id"), new ImageDescription(rs.getInt("id"), rs.getInt("contentfile_id"), rs.getString("transform")));
				}
			}
		}, Transaction.UPDATE_STATEMENT);
		for (Entry<Integer, ImageDescription> entry : toUpdate.entrySet()) {
			if (publisher != null) {
				publisher.keepContentmapsAlive();
			}
			String hash = resizeImage(entry.getValue());
			ImageFile imageFile = t.getObject(ImageFile.class, entry.getValue().contentfileId);
			DBUtils.executeUpdate("UPDATE imagestoreimage SET edate = ?, hash = ?, hash_orig = ? WHERE id = ?",
					new Object[] { imageFile.getEDate().getTimestamp(), hash, imageFile.getMd5(), entry.getKey() });
		}
		return imageStoreResult;
	}

	/**
	 * Creates hardlinks in the pub folder according to the imagestoretarget entries
	 */
	public void createLinks(FilePublisher fp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final HashMap<FileDescription, String> links = new HashMap<FileDescription, String>();
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT ii.id, it.node_id, ii.hash, ii.contentfile_id, ii.transform FROM imagestoretarget it JOIN imagestoreimage ii ON it.imagestoreimage_id = ii.id WHERE ii.hash IS NOT NULL", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					links.put(new FileDescription(new ImageDescription(rs.getInt("id"), rs.getInt("contentfile_id"), rs.getString("transform")), rs.getInt("node_id")), rs.getString("hash"));
				}
			}
		}, Transaction.UPDATE_STATEMENT);

		Map<Integer, String> modifiedHashes = new HashMap<>();

		for (Entry<FileDescription, String> entry : links.entrySet()) {
			FileDescription desc = entry.getKey();
			String hash = modifiedHashes.getOrDefault(desc.description.id, entry.getValue());
			Path source = Paths.get(storageDirectory.toString(), hash.substring(0,2), hash);

			// if the resized image does not exist, we need to create it
			if (!Files.exists(source)) {
				String newHash = resizeImage(desc.description);
				// the hash of the resized image might have changed, we need to update the info in the DB
				if (!StringUtils.isEqual(hash, newHash)) {
					DBUtils.executeUpdate("UPDATE imagestoreimage SET hash = ? WHERE id = ?", new Object[] {newHash, desc.description.id});
					modifiedHashes.put(desc.description.id, newHash);
					hash = newHash;
					source = Paths.get(storageDirectory.toString(), hash.substring(0,2), hash);
				}
			}
			try (ChannelTrx trx = new ChannelTrx(desc.nodeId)) {
				ImageFile cf = t.getObject(ImageFile.class, desc.description.contentfileId);
				if (cf == null) {
					throw new NodeException("Couldn't find image { " + desc.description.contentfileId + " } to link");
				}
				Node node = t.getObject(Node.class, desc.nodeId);
				Path dest;

				try (HandleDependenciesTrx noDependenciesTrx = new HandleDependenciesTrx(false)) {
					dest = Paths.get(
						fp.getPublishDir().toString(),
						FilePublisher.PUBDIR_NEW,
						node.getHostname(),
						"GenticsImageStore" + File.separatorChar + desc.description.transform,
						StaticUrlFactory.getPublishPath(cf, true));
				}

				if (!Files.exists(source)) {
					// when the resized image still not exists, we throw an error
					throw new NodeException("Error while creating link to " + dest + ": GIS Image " + source + " does not exist");
				}

				Files.createDirectories(dest.getParent());
				Files.createLink(dest, source);
			} catch (IOException e) {
				throw new NodeException("Couldn't hardlink image to publish directory.", e);
			}
		}
	}
	/**
	 * Gets path from matcher 'matcher'.
	 * @param matcher Matcher of the regular expression
	 * @param group Group number
	 * @return
	 */
	public static String getPath(Matcher matcher, int group) {
		String path = matcher.group(group);
		if (matcher.group().matches(CSS_URL_ATTRIBUTE_REG_EXPR)) {
			path = path.substring(0, path.lastIndexOf(")"));
		} else if (matcher.group().matches(CDATA_REG_EXP)) {
			path = path.replaceAll("(\\])*$", "");
		}
		return path;
	}

	/**
	 * Removes unreferenced entries in imagestoretarget and imagestoreimage and image files not referenced by imagestoreimage.
	 * @throws NodeException
	 */
	public void cleanupImages() throws NodeException {
		final List<Integer> staleTargets = new ArrayList<Integer>();
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT it.id from imagestoretarget it left join publish_imagestoretarget pit on pit.imagestoretarget_id = it.id where pit.imagestoretarget_id IS NULL", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					staleTargets.add(rs.getInt("id"));
				}
					}
		}, Transaction.UPDATE_STATEMENT);

		DBUtils.executeMassStatement("DELETE FROM imagestoretarget where id in ", "", staleTargets, 1, null, Transaction.DELETE_STATEMENT);

		final List<Integer> staleImages = new ArrayList<Integer>();
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT ii.id from imagestoreimage ii left join imagestoretarget it on it.imagestoreimage_id = ii.id WHERE it.imagestoreimage_id IS NULL", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					staleImages.add(rs.getInt("id"));
						}
					}
		}, Transaction.UPDATE_STATEMENT);

		// add imagestoreimages referencing images, that are in the wastebin
		DBUtils.executeStatement("SELECT ii.id FROM imagestoreimage ii LEFT JOIN contentfile cf ON ii.contentfile_id = cf.id WHERE cf.deleted != 0", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					staleImages.add(rs.getInt("id"));
				}
			}
		}, Transaction.UPDATE_STATEMENT);

		DBUtils.executeMassStatement("DELETE FROM imagestoreimage where id in ", "", staleImages, 1, new SQLExecutor() {
		}, Transaction.DELETE_STATEMENT);

		Set<String> existingHashes = new HashSet<String>();
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT hash from imagestoreimage WHERE hash is not null", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					existingHashes.add(rs.getString("hash"));
		}
		}
		}, Transaction.UPDATE_STATEMENT);
		if (storageDirectory.exists()) {
			for (File f : storageDirectory.listFiles()) {
				if (f.isDirectory()) {
					for(File f2 : f.listFiles()) {
						if (f2.isFile() && !existingHashes.contains(f2.getName())) {
							f2.delete();
			}
			}
			}
		}
		}
		}

	/**
	 * Resize a single image
	 * @param task the description of the file to resize
	 * @return hash of resized file
	 * @throws NodeException
	 */
	protected String resizeImage(ImageDescription task) throws NodeException {

		// check whether publish process shall be interrupted
		PublishRenderResult.checkInterrupted();

		RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_HANDLEPAGE);
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			// flag if image should be cropped before resize
			boolean cropAndResize = false;

			Matcher m = TRANSFORM_PATTERN.matcher(task.transform);
			if (!m.matches()) {
				throw new NodeException("Couldn't parse " + task.transform);
			}
			String width = m.group("width");
			String height = m.group("height");
			String mode = m.group("mode");
			if (mode == null) {
				mode = m.group("cropmode");
				cropAndResize = true;
			}

			// crop parameters
			String topleft_x = m.group("tlx");
			String topleft_y = m.group("tly");
			String cropwidth = m.group("cw");
			String cropheight = m.group("ch");

			ImageFile image = t.getObject(ImageFile.class, task.contentfileId);
			// TODO think about that, why not abort fatal?
			if (image == null) {
				logger.error("Could not find image {" + task.contentfileId + "} for resizing");
				throw new NodeException("Could not find image {" + task.contentfileId + "} for resizing");
			}

			RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_CREATECACHEKEY);

			Object cacheKey = null;

			if (cropAndResize) {
				cacheKey = createCropCacheKey(image.getId() + "", mode, width, height, topleft_x, topleft_y, cropwidth,
						cropheight, image.getEDate().getIntTimestamp());
			} else {
				cacheKey = createCacheKey(image.getId() + "", mode, width, height, image.getEDate().getIntTimestamp());
			}

			RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_CREATECACHEKEY);

			if (cacheKey != null) {

				RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_GETCACHE);

				FileInformation resizedImage = getCachedImage(cacheKey);

				RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_GETCACHE);
				File originalFile = image.getBinFile();
				if (resizedImage == null) {
					try {
						// TODO load image from dbfiles, not originalfilepath? like this problem situations like disabled nodes
						// are avoided.
						try {
							RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_RESIZE);
							resizedImage = invokeGIS(mode, width, height, cropAndResize, topleft_x, topleft_y, cropwidth, cropheight,
								image);
							imageStoreResult.setResized(imageStoreResult.getResized()+1);

						} finally {
							RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_RESIZE);
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Successfully resized image {" + originalFile.getAbsolutePath() + "} to {" + width + "/" + height + "/" + mode + "}");
						}
						// put the image into the cache
					} catch (NodeException e) {
						handleError("Error while resizing image {" + image.getId() + "} imagePath: {" + originalFile.getAbsolutePath() + "}", e, true);
					}
				} else if (logger.isDebugEnabled()) {
					logger.debug("Fetched resized image {" + originalFile.getAbsolutePath() + "} from cache");
				}

				// store the file at the filepath
				if (resizedImage != null) {
					imageStoreResult.setTotal(imageStoreResult.getTotal()+1);
					try {
						RuntimeProfiler.beginMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_WRITE);
						File resizedFile = writeHashedImage(resizedImage);
						RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_WRITE);

						if (logger.isDebugEnabled()) {
							logger.debug("Written resized image to {" + resizedFile + "}");
						}
						long lastModified = originalFile.lastModified();

						if (logger.isDebugEnabled()) {
							logger.debug("Setting modification date ... to {" + lastModified + "}");
						}
						if (lastModified > 0L) {
							if (!resizedFile.setLastModified(lastModified)) {
								logger.warn("Unable to set modification timestamp of {" + resizedFile + "} to {" + lastModified + "}");
							}
						} else {
							logger.warn("Unable to retrieve modification timestamp from {" + originalFile.getAbsolutePath() + "}");
						}
						return resizedFile.getName();
					} catch (IOException e) {
						handleError("Error while writing to target file for image {" + image + "}", e, true);
						throw new NodeException(e);
					}
				}
			} else {
				handleError("Could not create cacheKey for image {" + image + "}", null, true);
			}

			throw new NodeException("Couldn't resize image");
		} finally {
			RuntimeProfiler.endMark(JavaParserConstants.PUBLISHER_WRITEFS_IMAGERESIZER_HANDLEPAGE);
		}
	}

	/**
	 * Creates an image file on disk named by a hash of the content of the image
	 * @param resizedImage the source image
	 * @return a File object representing the newly created file
	 * @throws IOException
	 */
	private File writeHashedImage(FileInformation resizedImage) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.reset();
			InputStream is = resizedImage.getInputStream();

			if (is == null) {
				// This happens when the underlying file of resizedImage is empty. In this
				// case an error has already been logged that the image could not be resized.
				// Just create an empty input stream, so that the publish run can continue.
				is = new ByteArrayInputStream(new byte[0]);
			}

			File tempFile = File.createTempFile("resize", "tmp");
			OutputStream os = new FileOutputStream(tempFile);
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesread;
			while ((bytesread=is.read(buffer))!=-1) {
				md.update(buffer, 0, bytesread);
				os.write(buffer,0,bytesread);
			}
			is.close();
			os.close();
			String digest = Hex.encodeHexString(md.digest());
			File targetFile = new File(storageDirectory, digest.substring(0, 2) + "/" + digest);
			targetFile.getParentFile().mkdirs();
			if (targetFile.exists()) {
				Files.delete(targetFile.toPath());
			}
			Files.move(tempFile.toPath(), targetFile.toPath());
			return targetFile;
		} catch (NoSuchAlgorithmException e) {
			// Should not happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle an error, store the error message and log the error
	 * @param message error message
	 * @param e exception (may be null)
	 * @param fail true when the job shall fail, false if not
	 */
	protected void handleError(String message, Exception e, boolean fail) {
		if (e != null) {
			this.errorMessage = message + ": " + e.getLocalizedMessage();
			logger.error(message, e);
		} else {
			this.errorMessage = message;
			logger.error(message);
		}
		if (fail) {
			success = false;
		}
	}

	/**
	 * Get the image information for the image that will be published into the
	 * given file (full path)
	 * Warning: Things will go wrong horribly if you don't specify allImageData.
	 * @param host the image is looked up in the node identified by this hostname
	 * @param fullImagePath full publish path of the image
	 * @param allImageData map containing all image data (may be null)
	 * @return image information or null if not found
	 * @throws TransactionException
	 */
	protected static ImageInformation getImageInformation(String host, String fullImagePath, Map<String, ImageInformation> allImageData) throws NodeException {

		if (fullImagePath == null) {
			return null;
		}

		fullImagePath = fullImagePath.replaceAll("\\\\", "/");
		fullImagePath = fullImagePath.replaceAll("//+", "/");
		// grep filename for faster indexed query
		String imageName = fullImagePath.substring(fullImagePath.lastIndexOf('/') + 1);

		// make sure the full image path starts with an /
		if (!fullImagePath.startsWith("/")) {
			fullImagePath = "/" + fullImagePath;
		}

		// when the map of all image data is given, we get the data from the map
		if (allImageData != null) {
			return (ImageInformation) allImageData.get(host + fullImagePath);
		}

		// Warning: The code below does not correctly determine the publish path for images in multichannelling environments.
		// If you don't specify a complete allImageData in the method call, you're doomed.
		final ImageInformation[] image = new ImageInformation[1];

		final String paramFullImagePath = fullImagePath;
		DBUtils.executeStatement("select c.id id, cn.id nodeId, c.edate edate from contentfile c "
				+ " left join folder cf on c.folder_id = cf.id "
				+ " left join node cn on cf.node_id = cn.id "
				+ " left join folder nf on nf.id = cn.folder_id " 
						+ "where ("
							+ " concat('/', cn.pub_dir, '/', cf.pub_dir, '/', c.name) = ?"
							+ " OR concat('/', nf.pub_dir, '/', cf.pub_dir, '/', c.name) = ?"
							+ ") AND c.name = ? AND c.deleted = 0",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setString(1, paramFullImagePath);
						stmt.setString(2, paramFullImagePath);
						stmt.setString(3, imageName);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						if (rs.next()) {
							image[0] = new ImageInformation(rs.getInt("id"), rs.getInt("nodeId"), paramFullImagePath, rs.getInt("edate"));
							while (rs.next()) {
								// found another image with the same publish
								// path
								logger.error("Found another image for publish path {" + paramFullImagePath + "}: " + rs.getInt("id") + " (using image "
										+ image[0].getFileId() + ")");
							}
						}
					}
				});
		return image[0];
	}

	private Connection getConnection(boolean preferTransaction) throws NodeException {
		if (preferTransaction && transaction != null) {
			return null;
		} else {
			return config.getConnection();
		}
	}

	/**
	 * Return the connection to the pool
	 * @param conn connection
	 */
	private void releaseConnection(Connection conn) {
		if (conn != null) {
			config.returnConnection(conn);
		}
	}

	//
	// /**
	// * Create a statement
	// * @param conn connection (may be null, if the transaction shall be used)
	// * @return the Statement
	// * @throws SQLException
	// */
	// private Statement createStatement(Connection conn) throws SQLException {
	// if (conn != null) {
	// return conn.createStatement();
	// } else {
	// return transaction.getStatement();
	// }
	// }

	/**
	 * Prepare a statement
	 * @param conn connection (may be null, if the transaction shall be used)
	 * @param sql Statement sql
	 * @param type Statement type
	 * @return the Prepared Statement
	 * @throws SQLException
	 */
	private PreparedStatement prepareStatement(Connection conn, String sql, int type) throws SQLException, TransactionException {
		if (conn != null) {
			return conn.prepareStatement(sql);
		} else {
			return transaction.prepareStatement(sql, type);
		}
	}

	/**
	 * Close the given statement
	 * @param conn connection (may be null)
	 * @param pst statement to close
	 * @throws SQLException
	 */
	private void close(Connection conn, PreparedStatement pst) {
		if (conn != null) {
			try {
				pst.close();
			} catch (SQLException e) {}
		} else if (transaction != null) {
			transaction.closeStatement(pst);
		}
	}

	/**
	 * Close the given ResultSet
	 * @param conn connection (may be null)
	 * @param res ResultSet to close
	 * @throws SQLException
	 */
	private void close(Connection conn, ResultSet res) {
		if (conn != null) {
			try {
				res.close();
			} catch (SQLException e) {}
		} else if (transaction != null) {
			transaction.closeResultSet(res);
		}
	}

	/**
	 * Returns the eDate for the file
	 *
	 * @param fileId
	 * @return eDate for given fileId or 0 when the file could not be found.
	 */
	protected int getEDateFromFileId(String fileId) throws NodeException {
		int eDate = 0;
		// get the edate for the image and construct the cache key
		Connection conn = null;
		PreparedStatement pSt = null;
		ResultSet res = null;
		try {
			conn = getConnection(false);
			pSt = prepareStatement(conn, "select edate from contentfile where id = ?", Transaction.SELECT_STATEMENT);
			pSt.setString(1, fileId);
			res = pSt.executeQuery();
			if (res.next()) {
				eDate = res.getInt("edate");
			} else {
				logger.warn("Image with id {" + fileId + "} not found in table contentfile");
			}
		} catch (SQLException e) {
			logger.error("Error while creating cachekey for image {" + fileId + "}", e);
		} finally {
			close(conn, res);
			close(conn, pSt);
			releaseConnection(conn);
		}
		return eDate;
	}

	/**
	 * Create the cache key for the cropped and resized image
	 * @param fileId fileId of the file
	 * @param mode resizing mode
	 * @param width new width (may also be "auto")
	 * @param height (may also be "auto")
	 * @return cache key
	 * @throws TransactionException
	 */
	protected Object createCropCacheKey(String fileId, String mode, String width, String height, String topx, String topy, String cwidth, String cheight) throws NodeException {
		int eDate = getEDateFromFileId(fileId);
		if (eDate <= 0) {
			return null;
		}
		return createCropCacheKey(fileId, mode, width, height, topx, topy, cwidth, cheight, eDate);
	}

	/**
	 * Create the cache key for the resized image
	 * @param fileId fileId of the file
	 * @param mode resizing mode
	 * @param width new width (may also be "auto")
	 * @param height (may also be "auto")
	 * @return cache key
	 * @throws TransactionException
	 */
	protected Object createCacheKey(String fileId, String mode, String width, String height) throws NodeException {
		int eDate = getEDateFromFileId(fileId);
		if (eDate <= 0) {
			return null;
		}
		return createCacheKey(fileId, mode, width, height, eDate);
	}

	public static class ImageVariant {

		public final String fieldKey;
		public final ImageDescription description;
		public final ImageInformation information;

		public ImageVariant(String fieldKey, ImageDescription description, ImageInformation information) {
			super();
			this.description = description;
			this.information = information;
			this.fieldKey = fieldKey;
		}
	}

	/**
	 * Inner helper class for storing image information (image id, filepath and
	 * hostname of the node)
	 */
	public static class ImageInformation {

		/**
		 * file id
		 */
		protected int fileId;

		/**
		 * node id
		 */
		protected int nodeId;

		/**
		 * file path of the image
		 */
		protected String filePath;

		/**
		 * edate of the image
		 */
		protected int edate;

		/**
		 * Create instance of the ImageInformation object
		 * @param fileId file id
		 * @param nodeId node id
		 * @param filePath file path
		 * @param edate edate of the image
		 */
		public ImageInformation(int fileId, int nodeId, String filePath, int edate) {
			this.fileId = fileId;
			this.nodeId = nodeId;
			this.filePath = filePath;
			this.edate = edate;
		}

		/**
		 * get the fileid
		 * @return fileid
		 */
		public int getFileId() {
			return fileId;
		}

		/**
		 * Get the nodeId
		 * @return nodeId
		 */
		public int getNodeId() {
			return nodeId;
		}

		/**
		 * get the file path
		 * @return file path
		 */
		public String getFilePath() {
			return filePath;
		}

		/**
		 * Get the edate
		 * @return edate
		 */
		public int getEdate() {
			return edate;
		}
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}


	/**
	 * Parses GenticsImageStore Links in pages that have just been published
	 * written to the publish table. To be called after inserts/updates to the
	 * publish table.
	 *
	 * @param publishId
	 *            ID of the publish entry to process
	 * @param pageNode
	 *            node of the page
	 * @param source
	 *            source of the page
	 * @param nodes
	 *            map of hostnames to nodes, will be created on demand if null
	 *            is passed
	 * @throws NodeException
	 */
	public static <K, I extends GisImageInitiator<K>> void processGISUrls(final I initiator, Node pageNode, String source, Map<String, Node> nodes, Map<String, ImageInformation> allImageData,
				BiFunction<FileDescription, I, Integer> gisLinkSaver, BiConsumer<I, Set<Integer>> gisLinkCleaner) throws NodeException {

		Set<Integer> targetIds = new HashSet<Integer>();
		// get all image src'es for resized images
		Matcher m = SANE_IMAGESTORE_URL_PATTERN.matcher(source);

		while (m.find()) {
			String hostname = m.group("host");

			// check whether publish process shall be interrupted
			PublishRenderResult.checkInterrupted();

			// Change the used current Host when this GIS call references a
			// foreign node
			if (hostname == null) {
				hostname = pageNode.getHostname();
			}

			String imageUrl = m.group("imageurl");
			ImageInformation image = getImage(hostname, imageUrl, allImageData);
			if (image != null || initiator.initiateIfNotFound()) {
				int fileId = image != null ? image.getFileId() : 0;
				int nodeId = image != null ? image.getNodeId() : pageNode.getId(); // TODO or make 0?
				String transform = m.group("transform");
				ImageDescription idesc = new ImageDescription(0, fileId, transform);
				FileDescription fdesc = new FileDescription(idesc, nodeId);
				initiator.setImageData(imageUrl, transform);
				targetIds.add(gisLinkSaver.apply(fdesc, initiator));
			} else {
				RenderResult rr = TransactionManager.getCurrentTransaction().getRenderResult();
				if (rr != null) {
					rr.warn(CNGenticsImageStore.class, "Couldn't find an image for " + m.group());
				}
			}
		}
		gisLinkCleaner.accept(initiator, targetIds);
	}

	/**
	 * Get the image information for the given hostname and image URL
	 * @param hostname hostname
	 * @param imageUrl image URL
	 * @param allImageData map containing all image data
	 * @return image information object or null if not found
	 * @throws NodeException
	 */
	public static ImageInformation getImage(String hostname, String imageUrl, Map<String, ImageInformation> allImageData) throws NodeException {
		ImageInformation image = getImageInformation(hostname, imageUrl, allImageData);
		while (image == null && !imageUrl.isEmpty()) {
			imageUrl = imageUrl.substring(0, imageUrl.length() - 1);
			image = getImageInformation(hostname, imageUrl, allImageData);
		}

		return image;
	}

	public static List<ImageVariant> collectImageVariants(int nodeId) throws NodeException {
		List<ImageVariant> result = new ArrayList<>();
		DBUtils.executeStatement("SELECT distinct mit.entity_id entity_id, mit.entity_type entity_type, mit.field_key field_key, isi.contentfile_id contentfile_id, mit.transform transform, mit.webrootpath webrootpath, isi.edate edate "
				+ " from meshpublish_imagestoretarget mit "
				+ " left join imagestoretarget ist on mit.imagestoretarget_id = ist.id "
				+ " left join imagestoreimage isi on ist.imagestoreimage_id = isi.id "
				+ " where mit.node_id = ? ", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, nodeId);
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					ImageInformation info = new ImageInformation(rs.getInt("contentfile_id"), nodeId, rs.getString("webrootpath"), rs.getInt("edate"));
					ImageDescription desc = new ImageDescription(rs.getInt("entity_id"), rs.getInt("entity_type"), rs.getString("transform"));
					result.add(new ImageVariant(rs.getString("field_key"), desc, info));
				}
			}
		}, Transaction.UPDATE_STATEMENT);
		return result;
	}

	/**
	 * Stores a link between the specified image file and publish entry,
	 * creating the image file db entry if it doesn't yet exist. Variant for {@link MeshPublisher}.
	 *
	 * @param fdesc
	 *            the requested image file
	 * @param initiator
	 *            
	 * @return
	 * @throws NodeException
	 */
	public static int storeGISLink(final FileDescription fdesc, final MeshPublisherGisImageInitiator initiator) throws NodeException {
		int ifid = fdesc.description.contentfileId != 0 ? storeImageFileEntry(fdesc) : 0;
		final boolean[] exists = new boolean[] { false };
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT imagestoretarget_id from meshpublish_imagestoretarget where node_id = ? and entity_id = ? and entity_type = ? and field_key = ? and webrootpath = ? and transform = ? and imagestoretarget_id = ? ", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				Object[] fkey = initiator.getInitiatorForeignKey();
				stmt.setInt(1, (int) fkey[0]);
				stmt.setInt(2, (int) fkey[1]);
				stmt.setInt(3, (int) fkey[2]);
				stmt.setString(4, (String) fkey[3]);
				stmt.setString(5, (String) fkey[4]);
				stmt.setString(6, (String) fkey[5]);
				stmt.setInt(7, ifid);
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				exists[0] = rs.next();
			}
		}, Transaction.UPDATE_STATEMENT);

		if (!exists[0]) {
			int length = initiator.getInitiatorForeignKey().length;
			Object[] fkey = new Object[length + 1];
			System.arraycopy(initiator.getInitiatorForeignKey(), 0, fkey, 0, length);
			fkey[length] = ifid;
			DBUtils.executeInsert("INSERT INTO meshpublish_imagestoretarget (node_id, entity_id, entity_type, field_key, webrootpath, transform, imagestoretarget_id) values (?,?,?,?,?,?,?)", fkey);
		}
		return ifid;
	}

	/**
	 * Stores a link between the specified image file and publish entry,
	 * creating the image file db entry if it doesn't yet exist. Variant for {@link FilePublisher}.
	 *
	 * @param fdesc
	 *            the requested image file
	 * @param initiator
	 *            the publish entry to associate the image file with
	 * @return imagestoretarget_id created/used
	 * @throws NodeException
	 */
	private static int storeGISLink(final FileDescription fdesc, final FilePublisherGisImageInitiator initiator) throws NodeException {
		int ifid = storeImageFileEntry(fdesc);
		final boolean[] exists = new boolean[] { false };
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT publish_id, imagestoretarget_id from publish_imagestoretarget where publish_id = ? and imagestoretarget_id = ? ", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, initiator.getInitiatorForeignKey());
				stmt.setInt(2, ifid);
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				exists[0] = rs.next();
			}
		}, Transaction.UPDATE_STATEMENT);
		if (!exists[0]) {
			DBUtils.executeInsert("INSERT INTO publish_imagestoretarget (publish_id, imagestoretarget_id) values (?,?)", new Object[] { initiator.getInitiatorForeignKey(), ifid });
		}
		return ifid;
	}

	/**
	 * Stores an imagetarget description in the DB if it doesn't exist yet
	 *
	 * @param fdesc
	 * @return the ID of the matching imagestoretarget entry
	 * @throws NodeException
	 */
	private static synchronized int storeImageFileEntry(final FileDescription fdesc) throws NodeException {
		final int idid = storeImageDescription(fdesc.description);
		final Integer[] targetid = new Integer[] { null };
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT id FROM imagestoretarget WHERE imagestoreimage_id = ? and node_id = ? ", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, idid);
				stmt.setInt(2, fdesc.nodeId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					targetid[0] = rs.getInt("id");
				}
			}
		}, Transaction.UPDATE_STATEMENT);
		if (targetid[0] == null) {
			targetid[0] = DBUtils.executeInsert("INSERT INTO imagestoretarget (imagestoreimage_id, node_id) values (?, ?)", new Object[] { idid, fdesc.nodeId }).get(0);
		}
		return targetid[0];
	}

	/**
	 * Stores an image description in the DB if it doesn't exist yet
	 *
	 * @param description
	 *            the image description to store
	 * @return the ID of the the matching imagestoreimage record
	 * @throws NodeException
	 */
	private static synchronized int storeImageDescription(final ImageDescription description) throws NodeException {
		final Integer[] descid = new Integer[] { null };
		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		DBUtils.executeStatement("SELECT id FROM imagestoreimage WHERE contentfile_id = ? and transform = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, description.contentfileId);
				stmt.setString(2, description.transform);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					descid[0] = rs.getInt("id");
				}
			}
		}, Transaction.UPDATE_STATEMENT);
		if (descid[0] == null) {
			descid[0] = DBUtils.executeInsert("INSERT INTO imagestoreimage (contentfile_id, transform) values (?, ?)",
					new Object[] { description.contentfileId, description.transform }).get(0);
		}
		return descid[0];
	}

	/**
	 * This immutable class represents an entry in the imagestoreimage table.
	 * @author escitalopram
	 *
	 */
	public static class ImageDescription {
		public final int id;
		public final int contentfileId;
		public final String transform;

		public ImageDescription(int id, int contentfileId, String transform) {
			this.id = id;
			this.contentfileId = contentfileId;
			this.transform = transform;
		}
	}

	/**
	 * This immutable class represents an entry in the imagestoretarget table.
	 * @author escitalopram
	 *
	 */
	public static class FileDescription {
		public final int nodeId;
		public final ImageDescription description;

		public FileDescription(ImageDescription description, int nodeId) {
			this.description = description;
			this.nodeId = nodeId;
		}
	}

	/**
	 * Removes unused GenticsImageStore Links associated with the specified
	 * publish entry
	 *
	 * @param publishId
	 *            remove links for the entry with this publish_id
	 * @param usedTargetIds imagetarget ids used by the publish entry
	 * @throws NodeException
	 */
	private static void deleteExcessGISLinksForPublishId(final FilePublisherGisImageInitiator initiator, Set<Integer> usedTargetIds) throws NodeException {
		IntegerColumnRetriever retr = new IntegerColumnRetriever("imagestoretarget_id") {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, initiator.getInitiatorForeignKey());
			}
		};
		// We need to access data written in the publish transaction, so we declare this to be an update statement
		DBUtils.executeStatement("SELECT imagestoretarget_id from publish_imagestoretarget where publish_id = ?", retr, Transaction.UPDATE_STATEMENT);
		Set<Integer> existingTargetIds = new HashSet<>(retr.getValues());
		Set<Integer> excessTargetIds = new HashSet<>(existingTargetIds);
		excessTargetIds.removeAll(usedTargetIds);
		if (excessTargetIds.size() > 0) {
			DBUtils.executeMassStatement("DELETE from publish_imagestoretarget WHERE publish_id = ? and imagestoretarget_id in", "", new ArrayList<Integer>(
					excessTargetIds), 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, initiator.getInitiatorForeignKey());
				}
			}, Transaction.DELETE_STATEMENT);
		}
	}

	public static void deleteExcessGISLinksForPublishId(final MeshPublisherGisImageInitiator initiator, Set<Integer> usedTargetIds) throws NodeException {
		Object[] fkey = initiator.getInitiatorForeignKey();
		IntegerColumnRetriever retr = new IntegerColumnRetriever("imagestoretarget_id") {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, (Integer) fkey[0]);
				stmt.setInt(2, (Integer) fkey[1]);
				stmt.setInt(3, (Integer) fkey[2]);
				stmt.setString(4, (String) fkey[3]);
			}
		};
		// We need to access data written in the publish transaction, so we declare this to be an update statement
		DBUtils.executeStatement("SELECT imagestoretarget_id from meshpublish_imagestoretarget where node_id = ? and entity_id = ? and entity_type = ? and field_key = ?", retr, Transaction.UPDATE_STATEMENT);
		Set<Integer> existingTargetIds = new HashSet<>(retr.getValues());
		Set<Integer> excessTargetIds = new HashSet<>(existingTargetIds);
		excessTargetIds.removeAll(usedTargetIds);
		if (excessTargetIds.size() > 0) {
			DBUtils.executeMassStatement("DELETE from meshpublish_imagestoretarget WHERE node_id = ? and entity_id = ? and entity_type = ? and field_key = ? and imagestoretarget_id in", "", 
					excessTargetIds, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, (Integer) fkey[0]);
					stmt.setInt(2, (Integer) fkey[1]);
					stmt.setInt(3, (Integer) fkey[2]);
					stmt.setString(4, (String) fkey[3]);
				}
			}, Transaction.DELETE_STATEMENT);
		}
	}

	/**
	 * Parses the specified publish entries and updates their publish_imagestoretarget, etc. entries.
	 * @param allImageData lookup map for published image paths, optional (theoretically)
	 * @throws NodeException
	 */
	public static void parseForImages(Map<String, ImageInformation> allImageData) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// We need to access data written in the publish transaction, so we declare this to be an update statement.
		final List<Integer> handled = new ArrayList<>();
		final List<Integer> toResize = new ArrayList<>();
		DBUtils.executeStatement("SELECT p.id, IF(source LIKE '%/GenticsImageStore/%', TRUE, FALSE) has_img FROM publish p INNER JOIN node n ON p.node_id = n.id where p.active = 1 AND p.updateimagestore = 1", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					handled.add(rs.getInt("id"));
					if (rs.getBoolean("has_img")) {
						toResize.add(rs.getInt("id"));
					}
				}
			}
		}, Transaction.UPDATE_STATEMENT);

		for (final Integer id : toResize) {
			final String[] source = new String[1];
			final int[] nodeId = new int[1];
			// We need to access data written in the publish transaction, so we declare this to be an update statement.
			DBUtils.executeStatement("SELECT node_id, source from publish where id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, id);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					rs.next();
					source[0] = rs.getString("source");
					nodeId[0] = rs.getInt("node_id");
				}
			}, Transaction.UPDATE_STATEMENT);
			Node n = t.getObject(Node.class, nodeId[0], -1, false);
			CNGenticsImageStore.processGISUrls(new FilePublisherGisImageInitiator(id), n, source[0], null, allImageData, CNGenticsImageStore::storeGISLink, CNGenticsImageStore::deleteExcessGISLinksForPublishId);
		}
		if (!handled.isEmpty()) {
			DBUtils.executeMassStatement("UPDATE publish set updateimagestore = 0 where id in ", "", handled, 1, null, Transaction.UPDATE_STATEMENT);
		}
	}

	/**
	 * Clear the cache
	 */
	public static void clearCache() {
		PortalCache cache;
		try {
			cache = PortalCache.getCache(CACHEREGION);

			if (cache != null) {
				cache.clear();
				logger.debug("Cache has been cleared");
			}
		} catch (PortalCacheException e) {
			logger.warn("Error while clearing cache", e);
		}
	}

	/**
	 * Get Cache statistics (if the cache region exists and is a JCS cache)
	 * @return cache stats or empty string
	 */
	public static String getCacheStats() {
		PortalCache cache;
		try {
			cache = PortalCache.getCache(CACHEREGION);
			if (cache instanceof JCSPortalCache) {
				JCS jcsCache = ((JCSPortalCache) cache).getJcsCache();
				return jcsCache.getStats();
			} else {
				return "";
			}
		} catch (PortalCacheException e) {
			logger.warn("Error while counting cache entries", e);
			return "";
		}
	}

	/**
	 * Invokes the GIS and returns the resized image
	 * 
	 * @param mode
	 * @param width
	 * @param height
	 * @param cropImage
	 * @param topleftx
	 * @param toplefty
	 * @param cwidth
	 * @param cheight
	 * @param sourceImage
	 * @return FileInformation containing the resized image data
	 * @throws ServletException
	 */
	protected FileInformation invokeGIS(String mode, String width, String height, boolean cropImage, String topleftx, String toplefty, String cwidth, String cheight,
			ImageFile sourceImage) throws NodeException {
		String filePath = sourceImage.getBinFile().getAbsolutePath();

		mode = transformMode(mode);
		String filterChain = "smart".equals(mode) ? SmarterResizeFilter.class.getCanonicalName() : ResizeFilter.class.getCanonicalName();
		Properties filterChainProperties = new Properties();

		if (ObjectTransformer.getInt(width, -1) > 0) {
			filterChainProperties.put("WIDTH", width);
		}
		if (ObjectTransformer.getInt(height, -1) > 0) {
			filterChainProperties.put("HEIGHT", height);
		}
		filterChainProperties.put("MODE", mode);
		if (factorLimit > 0) {
			filterChainProperties.put("FACTORLIMIT", new Double(factorLimit));
		}

		Properties cropProperties = new Properties();
		if (cropImage) {
			cropProperties.put("cropandresize", "true");
			cropProperties.put("TOPLEFTX", topleftx);
			cropProperties.put("TOPLEFTY", toplefty);
			cropProperties.put("WIDTH", cwidth);
			cropProperties.put("HEIGHT", cheight);
		}

		if (jpegQuality != null) {
			filterChainProperties.put("JPEG_QUALITY", jpegQuality);
		}

		if (!StringUtils.isEmpty(filePath)) {
			filterChainProperties.put("filePath", filePath);
		}

		GenticsImageStoreResizeResponse response;

		boolean lock = false;
		Semaphore s = semaphore;
		TimeUnit unit = tryTimeoutUnit;
		long timeout = tryTimeout;
		try {
			if (s != null) {
				lock = s.tryAcquire(timeout, unit);
				if (!lock) {
					throw new NodeException(String.format("Timeout (%d %s) while waiting for semaphore", timeout, unit));
				}
			}
			try {
				response = handleResizeAction(filePath, null, null, null, cropImage, null, filterChain, filterChainProperties, cropProperties);
				if (response != null) {
					return response.getFileInformation();
				} else {
					throw new NodeException("Image resize failed with unknown reason.");
				}
			} catch (Exception e) {
				throw new NodeException("Image resize failed with internal reason.", e);
			}
		} catch (NodeException | InterruptedException e1) {
			String msg = "FilterImageAction failed on file {" + filePath + "}";

			if (useOriginalIfResizeFails) {
				logger.error(msg + " - using original image data.");
				// An error happened, so read in the file and store it in an FileInformation.
				// Note: this is done, so we don't store a FileInformation object in the cache which 
				// contains only a reference to a file..
				File file = new File(filePath);
				byte[] bytes = new byte[(int) file.length()];

				FileInputStream is = null;

				try {
					is = new FileInputStream(file);
					// Read in the bytes
					int offset = 0;
					int numRead = 0;

					while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) > 0) {
						offset += numRead;
					}
				} catch (IOException e) {
					logger.fatal("Error while reading original file contents.");
					throw new NodeException("Error while reading original file contents.");
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {// not interrested in this exception
						}
					}
				}
				return new FileInformation(sourceImage.getFilename(), file.length(), sourceImage.getFiletype(), bytes);
			} else {
				throw new NodeException(msg);
			}
		} finally {
			if (lock && s != null) {
				s.release();
			}
		}
	}
}
