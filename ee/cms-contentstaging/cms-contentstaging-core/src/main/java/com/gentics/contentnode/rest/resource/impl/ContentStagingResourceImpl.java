package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.require;
import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.etc.TriConsumer;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.TimestampTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.TrxAttribute;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.StageableChanneledNodeObject;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.object.StageableVersionedNodeObject;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.IdTypeMapRequest;
import com.gentics.contentnode.rest.model.response.ContentPackageFolderResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.rest.model.response.StagingStatusResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.ContentStagingResource;
import com.gentics.contentnode.rest.resource.impl.internal.JobProgress;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.rest.util.StagingModelBuilder;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.staging.StageableObjectType;
import com.gentics.contentnode.staging.StagedChannel;
import com.gentics.contentnode.staging.StagedFile;
import com.gentics.contentnode.staging.StagedFolder;
import com.gentics.contentnode.staging.StagedForm;
import com.gentics.contentnode.staging.StagedImage;
import com.gentics.contentnode.staging.StagedNode;
import com.gentics.contentnode.staging.StagedObjectVersion;
import com.gentics.contentnode.staging.StagedPage;
import com.gentics.contentnode.staging.StagingBase;
import com.gentics.contentnode.staging.StagingError;
import com.gentics.contentnode.staging.StagingErrorKind;
import com.gentics.contentnode.staging.StagingErrorRecommendation;
import com.gentics.contentnode.staging.StagingErrors;
import com.gentics.contentnode.staging.StoredChannel;
import com.gentics.contentnode.staging.StoredContentPackage;
import com.gentics.contentnode.staging.StoredFile;
import com.gentics.contentnode.staging.StoredFolder;
import com.gentics.contentnode.staging.StoredForm;
import com.gentics.contentnode.staging.StoredImage;
import com.gentics.contentnode.staging.StoredModel;
import com.gentics.contentnode.staging.StoredNode;
import com.gentics.contentnode.staging.StoredPage;
import com.gentics.contentnode.staging.StoredType;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Authenticated
@jakarta.ws.rs.Path("/content")
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_VIEW)
public class ContentStagingResourceImpl implements ContentStagingResource {

	private static final String FOUND_AT_LOG = "Found {} at {}";
	private static final String DOWNLOAD_EXTENSION = ".zip";
	private static final String METADATA_EXTENSION = ".json";
	private static final String FOLDER_EXTENSION = ".folder" + METADATA_EXTENSION;
	private static final String NODE_EXTENSION = ".node" + METADATA_EXTENSION;
	private static final String IMAGE_EXTENSION = ".image" + METADATA_EXTENSION;
	private static final String FILE_EXTENSION = ".file" + METADATA_EXTENSION;
	private static final String FORM_EXTENSION = ".form" + METADATA_EXTENSION;
	private static final String PAGE_EXTENSION = ".page" + METADATA_EXTENSION;
	private static final String CHANNEL_EXTENSION = ".channel" + METADATA_EXTENSION;
	private static final String BINARY_EXTENSION = ".bin";
	private static final Pattern METADATA_FILE_PATTERN = Pattern.compile(
			"(?<id>[a-f0-9]{4}\\.[a-f0-9\\-]+)(\\.(?<version>[0-9\\.]+))?\\.(?<type>folder|node|image|file|page|channel|form)\\.json",
			Pattern.CASE_INSENSITIVE
		);
	public static final String CONTENT_PACKAGE_METADATA_FILENAME = "metadata" + METADATA_EXTENSION;
	public static final String CONTENT_PACKAGE_IMPORT_ERRORS_FILENAME = "import-errors" + METADATA_EXTENSION;

	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(ContentStagingResource.class);

	/**
	 * Object Mapper for serialization/deserialization
	 */
	private static ObjectMapper mapper = MiscUtils.newObjectMapper();

	/**
	 * Keyed lock to synchronize create/update calls for the same filename
	 */
	private static final KeyLockManager FILENAME_LOCK = KeyLockManagers.newLock();

	/**
	 * Progress of the running or last job
	 */
	protected static Map<String, StagingJobController> imports = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	@GET
	@jakarta.ws.rs.Path("/package")
	public ContentPackageListResponse list(
				@BeanParam FilterParameterBean filterParams,
				@BeanParam SortParameterBean sortingParams,
				@BeanParam PagingParameterBean pagingParams
			) throws NodeException {
		Path path = checkFeatureGetPath();

		try (Trx trx = ContentNodeHelper.trx()) {
			// Sort
			Comparator<StagedContentPackage> sortComparator = (a, b) -> 0;
			if (sortingParams != null && StringUtils.isNotBlank(sortingParams.sort)) {
				String[] items = sortingParams.sort.split(",");
				Deque<Comparator<StagedContentPackage>> comparators = new ArrayDeque<>(items.length);
				for (String item : items) {
					if (StringUtils.isBlank(item)) {
						continue;
					}
					boolean desc;
					if (item.startsWith("-")) {
						desc = true;
						item = item.substring(1);
					} else if (item.startsWith("+")) {
						item = item.substring(1);
						desc = false;
					} else {
						desc = false;
					}
					switch (item) {
					case "name":
						comparators.push((a,b) -> ObjectUtils.compare(StringUtils.lowerCase(a.getName()), StringUtils.lowerCase(b.getName())) * (desc ? -1 : 1));
						break;
					case "description":
						comparators.push((a,b) -> ObjectUtils.compare(StringUtils.lowerCase(a.getDescription()), StringUtils.lowerCase(b.getDescription())) * (desc ? -1 : 1));
						break;
					case "nodes":
						comparators.push((a,b) -> (a.getNodes() - b.getNodes()) * (desc ? -1 : 1));
						break;
					case "folders":
						comparators.push((a,b) -> (a.getFolders() - b.getFolders()) * (desc ? -1 : 1));
						break;
					case "pages":
						comparators.push((a,b) -> (a.getPages() - b.getPages()) * (desc ? -1 : 1));
						break;
					case "files":
						comparators.push((a,b) -> (a.getFiles() - b.getFiles()) * (desc ? -1 : 1));
						break;
					case "images":
						comparators.push((a,b) -> (a.getImages() - b.getImages()) * (desc ? -1 : 1));
						break;
					default:
						logger.warn("Unknown sort field: " + item);
						break;
					}
				}
				sortComparator = new ComparatorChain(Arrays.asList(comparators.toArray()));
			}

			// Collect
			List<StagedContentPackage> packages = Files.walk(path, 1)
					.filter(p -> Files.isDirectory(p) && Files.exists(p.resolve(CONTENT_PACKAGE_METADATA_FILENAME)))
					.map(p -> {
						try {
							String lockKey = FileFactory.sanitizeName(p.getFileName().toString());
							return executeLocked(lockKey, () -> {
								try (InputStream is = Channels.newInputStream(FileChannel.open(p.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StandardOpenOption.READ).lock(0, Long.MAX_VALUE, true).channel())) {
									StagedContentPackage cp = StoredContentPackage.NODE2REST.apply(mapper.readValue(is, StoredContentPackage.class), new StagedContentPackage());
									fillContentPackageStatistic(p, cp);
									addImportProgress(cp);
									return cp;
								} catch (Throwable e) {
									throw new IllegalStateException(e);
								}
							});
						} catch (NodeException e) {
							throw new IllegalStateException(e);
						}
					}).collect(Collectors.toList());

			ContentPackageListResponse response = ListBuilder.from(packages, unchanged -> unchanged)
					.filter(ResolvableFilter.get(filterParams, "name", "description", "files", "folders", "pages", "images", "nodes"))
					.sort(sortComparator).page(pagingParams)
					.to(new ContentPackageListResponse());
			trx.success();
			return response;
		} catch (IOException | IllegalStateException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw new NodeException("Error providing list of Content Packages", e);
			}
		}
	}

	@Override
	@PUT
	@jakarta.ws.rs.Path("/package")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_CREATE)
	public ContentPackageLoadResponse create(StagedContentPackage model) throws NodeException {
		Path path = checkFeatureGetPath();

		if (StringUtils.isBlank(model.getName())) {
			throw new MissingFieldException("name");
		}
		try (Trx trx = ContentNodeHelper.trx()) {
			String sanitizedName = sanitizeCheck(model.getName(), false);

			ContentPackageLoadResponse response = new ContentPackageLoadResponse();

			response.setContentPackage(executeLocked(sanitizedName, () -> {
				try (OutputStream os = Channels.newOutputStream(FileChannel.open(Files.createDirectories(path.resolve(sanitizedName)).resolve(CONTENT_PACKAGE_METADATA_FILENAME),
								StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND).lock().channel())) {
					String originalName = model.getName();
					if (!originalName.equals(sanitizedName)) {
						model.setName(sanitizedName);
					}
					StoredContentPackage stored = StoredContentPackage.REST2NODE.apply(model, new StoredContentPackage());
					mapper.writeValue(os, stored);

					// Warn in a case of sanitized name
					response.setResponseInfo(new ResponseInfo(ResponseCode.OK,
							originalName.equals(sanitizedName) ? StringUtils.EMPTY : ("Warning: The content package is created with a sanitized name '" + sanitizedName + "'")));
					return StoredContentPackage.NODE2REST.apply(stored, model);
				} catch (FileAlreadyExistsException existsException) {
					throw new DuplicateValueException("name", model.getName());
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}));
			return response;
		} catch (IllegalStateException e) {
			throw new NodeException("Error creating Content Package '" + model.getName() + "'", e);
		}
	}

	@Override
	@GET
	@jakarta.ws.rs.Path("/package/{name}")
	public ContentPackageLoadResponse read(@PathParam("name") String name) throws NodeException {
		Path path = checkFeatureGetPath();
		
		try (Trx trx = ContentNodeHelper.trx()) {
			String sanitizedName = sanitizeCheck(name, false);

			ContentPackageLoadResponse response = new ContentPackageLoadResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			Optional<StagingJobController> optionalImport = Optional.ofNullable(imports.get(sanitizedName));

			response.setImport(optionalImport.map(StagingJobController::getJobStatus).orElse(null));

			if (!optionalImport.map(StagingJobController::getJobStatus).map(JobStatus::isRunning).orElse(false)) {
				response.setContentPackage(executeLocked(sanitizedName, () -> {
					Path pkgPath = path.resolve(name);
					StagedContentPackage contentPackage = loadFromFilesystem(pkgPath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class, 
							stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
					fillContentPackageStatistic(pkgPath, contentPackage);
					addImportProgress(contentPackage);
					return contentPackage;
				}));
			} else {
				response.setContentPackage(optionalImport.map(StagingJobController::getInitialState).map(pkg -> {
					addImportProgress(pkg);
					return pkg;
				}).orElse(null));
			}
			return response;
		} catch (IllegalStateException e) {
			throw new NodeException("Error reading Content Package '" + name + "'", e);
		}
	}

	@Override
	@POST
	@jakarta.ws.rs.Path("/package/{name}")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_UPDATE)
	public ContentPackageLoadResponse update(@PathParam("name") String name, StagedContentPackage model) throws NodeException {
		Path path = checkFeatureGetPath();
		
		try (Trx trx = ContentNodeHelper.trx()) {
			String lockKey = sanitizeCheck(name, true);

			ContentPackageLoadResponse response = new ContentPackageLoadResponse();

			StagedContentPackage cp = executeLocked(lockKey, () -> {
				String finalName = name;
				StoredContentPackage stored;
				try (InputStream is = Channels.newInputStream(FileChannel.open(path.resolve(finalName).resolve(CONTENT_PACKAGE_METADATA_FILENAME), 
								StandardOpenOption.READ).lock(0, Long.MAX_VALUE, true).channel())) {					
					stored = mapper.readValue(is, StoredContentPackage.class);
				} catch (NoSuchFileException noSuchFileException) {
					String message = "The Content Package '" + finalName + "' does not exist";
					logger.warn("Could not load package file from path " + path + ": " + noSuchFileException.getMessage(), noSuchFileException);
					response.setResponseInfo(new ResponseInfo(ResponseCode.NOTFOUND, message));
					return null;
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}

				// Rename 
				if (StringUtils.isNotBlank(model.getName()) && !finalName.equals(model.getName())) {
					String sanitizedName = FileFactory.sanitizeName(model.getName());
					String tmpFilename = DigestUtils.md2Hex(model.getName());
					if (Files.exists(path.resolve(sanitizedName))) {
						throw new DuplicateValueException("name", sanitizedName);
					}
					if (!sanitizedName.equals(model.getName())) {
						model.setName(sanitizedName);
					}
					try {
						FileUtils.copyDirectory(path.resolve(finalName).toFile(), path.resolve(tmpFilename).toFile());
						FileUtils.moveDirectory(path.resolve(tmpFilename).toFile(), path.resolve(model.getName()).toFile());
						FileUtils.deleteDirectory(path.resolve(finalName).toFile());
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
					finalName = model.getName();
				}
				updateStoredPackage(path.resolve(finalName), model, stored);

				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
				return StoredContentPackage.NODE2REST.apply(stored, model);
			});
			response.setContentPackage(cp);
			return response;
		} catch (IllegalStateException e) {
			throw new NodeException("Error updating Content Package '" + name + "'", e);
		}
	}

	@Override
	@DELETE
	@jakarta.ws.rs.Path("/package/{name}")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_DELETE)
	public GenericResponse delete(@PathParam("name") String name) throws NodeException {
		Path path = checkFeatureGetPath();
		
		try (Trx trx = ContentNodeHelper.trx()) {
			String lockKey = sanitizeCheck(name, true);			

			executeLocked(lockKey, () -> {
				try {
					FileUtils.deleteDirectory(path.resolve(name).toFile());
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully deleted content package"));
		} catch (IllegalStateException e) {
			throw new NodeException("Error deleting Content Package '" + name + "'", e);
		}
	}

	@Override
	@PUT
	@jakarta.ws.rs.Path("/package/{name}/{type}/{uuid}")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)
	public ContentPackageLoadResponse add(
			@PathParam("name") String name, 
			@PathParam("type") String objectTypeId, 
			@PathParam("uuid") String objectUuid, 
			@QueryParam("recursive") @DefaultValue("false") boolean recursive
	) throws NodeException {
		Path path = checkFeatureGetPath();
		ContentPackageLoadResponse response = new ContentPackageLoadResponse();
		try (Trx trx = ContentNodeHelper.trx()) {
			executeLocked(sanitizeCheck(name, true), () -> {
				Path currentPath = path.resolve(name);
				StagedContentPackage pkg = loadFromFilesystem(currentPath.resolve(CONTENT_PACKAGE_METADATA_FILENAME),
						StoredContentPackage.class,
						stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
				StageableObjectType objectType = StageableObjectType.parseTypeId(objectTypeId);
				switch (objectType) {
				case NODE:
					Node node = loadFromDatabase(Node.class, objectUuid);
					try (ChannelTrx ctrx = new ChannelTrx(node)) {
						Folder rootFolder = node.getFolder();
						currentPath = addFolderPath(currentPath, rootFolder, recursive, Optional.ofNullable(rootFolder.getChannel()));
					}
					break;
				case FOLDER:
					Folder folder = loadFromDatabase(Folder.class, objectUuid);
					try (ChannelTrx ctrx = new ChannelTrx(folder.getChannel())) {
						currentPath = addFolderPath(currentPath, folder.getFolder(), recursive, Optional.ofNullable(folder.getChannel()));
					}
					break;
				case FILE:
					File file = loadFromDatabase(File.class, objectUuid);
					try (ChannelTrx ctrx = new ChannelTrx(file.getChannel())) {
						currentPath = addFolderPath(currentPath, file.getFolder(), recursive, Optional.ofNullable(file.getChannel()));
						addStageable(currentPath, file, new StoredFile(), (n, s) -> StagingModelBuilder.getFile(n, s), Optional.empty(), Optional.empty());
					}
					break;
				case PAGE:
					Page page = loadFromDatabase(Page.class, objectUuid);
					try (ChannelTrx ctrx = new ChannelTrx(page.getChannel())) {
						currentPath = addFolderPath(currentPath, page.getFolder(), recursive, Optional.ofNullable(page.getChannel()));
						addVersionedStageable(currentPath, page, new StoredPage(), (n, s) -> StagingModelBuilder.getPage(n, s), Optional.empty());
					}
					break;
				case IMAGE:
					ImageFile image = loadFromDatabase(ImageFile.class, objectUuid);
					try (ChannelTrx ctrx = new ChannelTrx(image.getChannel())) {
						currentPath = addFolderPath(currentPath, image.getFolder(), recursive, Optional.ofNullable(image.getChannel()));
						addStageable(currentPath, image, new StoredImage(), (n, s) -> StagingModelBuilder.getImage(n, s), Optional.empty(), Optional.empty());
					}
					break;
				case FORM:
					Form form = loadFromDatabase(Form.class, objectUuid);
					try (ChannelTrx ctrx = new ChannelTrx(0)) {
						currentPath = addFolderPath(currentPath, form.getFolder(), recursive, Optional.empty());
						addVersionedStageable(currentPath, form, new StoredForm(), (n, s) -> StagingModelBuilder.getForm(n, s), Optional.empty());
					}
					break;
				}
				fillContentPackageStatistic(path.resolve(name), pkg);
				Integer updateTimestamp = updateStoredPackage(path.resolve(name), pkg);
				pkg.setTimestamp(updateTimestamp);
				addImportProgress(pkg);

				response.setContentPackage(pkg);
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
				return pkg;
			});
			return response;
		} catch (Throwable e) {
			if (e instanceof NodeException) {
				throw (NodeException) e;
			} else if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else if (e instanceof IllegalArgumentException) {
				response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, e.getMessage()));
				return response;
			} else {
				throw new NodeException("Error adding " + objectTypeId + " " + objectUuid + " to the ContentPackage '" + name + "'", e);
			}
		}	
	}

	@Override
	@DELETE
	@jakarta.ws.rs.Path("/package/{name}/{type}/{uuid}")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)
	public ContentPackageLoadResponse remove(@PathParam("name") String name, @PathParam("type") String objectTypeId, @PathParam("uuid") String objectUuid) throws NodeException {
		Path path = checkFeatureGetPath();
		ContentPackageLoadResponse response = new ContentPackageLoadResponse();
		try (Trx trx = ContentNodeHelper.trx()) {
			executeLocked(sanitizeCheck(name, true), () -> {
				Path currentPath = path.resolve(name);
				StagedContentPackage pkg = loadFromFilesystem(currentPath.resolve(CONTENT_PACKAGE_METADATA_FILENAME),
						StoredContentPackage.class,
						stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
				StageableObjectType objectType = StageableObjectType.parseTypeId(objectTypeId);
				StageableNodeObject nodeObject = loadFromDatabase(objectType.getNodeObjectClass(), objectUuid);
				
				if (!Node.class.equals(objectType.getNodeObjectClass())) {
					try (ChannelTrx ctrx = new ChannelTrx(nodeObject.maybeHasChannels().flatMap(ch -> {
						try {
							return ch.maybeGetChannel();
						} catch (NodeException e) {
							throw new IllegalStateException(e);
						}
					}).orElse(null))) {
						currentPath = findFolderPath(currentPath, nodeObject.getFolder());
					}
				} else {
					// remove root folder of node
					Node node = (Node) nodeObject;
					removeStageable(currentPath, node.getFolder(), false, Optional.empty());
				}
				currentPath = removeStageable(currentPath, nodeObject, false, Optional.empty());
				// if the folder is a root folder, remove the node/channel
				if (nodeObject instanceof Folder) {
					Folder folder = (Folder) nodeObject;
					if (folder.isChannelRoot()) {
						removeStageable(currentPath, folder.getChannel(), false, Optional.empty());
					} else if (folder.isRoot()) {
						removeStageable(currentPath, folder.getNode(), false, Optional.empty());
					}
				}

				fillContentPackageStatistic(path.resolve(name), pkg);
				Integer updateTimestamp = updateStoredPackage(path.resolve(name), pkg);
				pkg.setTimestamp(updateTimestamp);
				addImportProgress(pkg);

				response.setContentPackage(pkg);
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
				return pkg;
			});
			return response;
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else if (e instanceof IllegalArgumentException) {
				response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, e.getMessage()));
				return response;
			} else {
				throw new NodeException("Error removing " + objectTypeId + " " + objectUuid + " from the ContentPackage '" + name + "'", e);
			}
		}	
	}

	@Override
	@GET
	@jakarta.ws.rs.Path("/package/{name}/zip")
	@Produces({ "application/zip" })
	public Response download(@PathParam("name") String name) throws NodeException {
		Path path = checkFeatureGetPath();
		try (Trx trx = ContentNodeHelper.trx()) {
			return executeLocked(sanitizeCheck(name, true), () -> {
				Path packagePath = path.resolve(name);
				// Throw 404 on inexisting package / 409 on access error / 500 on fatal error
				StagedContentPackage lockReturn = loadFromFilesystem(
						packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class,
						stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
				// Clean off alien content
				fillContentPackageStatistic(packagePath, lockReturn);

				// Make a temp file for the zip to output.
				java.io.File tmpFile;
				try {
					tmpFile = java.io.File.createTempFile(System.currentTimeMillis() + "." + name, ".out.tmp");
				} catch (IOException e1) {
					throw new IllegalStateException(e1);
				}

				// Inflate the zip.
				try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(tmpFile), 1048576); ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
					Files.walk(packagePath)
						.filter(entry -> !Files.isDirectory(entry))
						.forEach(file -> {
							ZipEntry zipEntry = new ZipEntry(packagePath.relativize(file).toString());
							try {
								zos.putNextEntry(zipEntry);
								Files.copy(file, zos);
								zos.closeEntry();
							} catch (IOException e) {
								logger.error("Could not zip content package file " + file, e);
							}
						}
					);
				} catch (IOException e1) {
					throw new IllegalStateException(e1);
				}
				// Make a content-disposition filename
				String urlEncodedName;
				try {
					urlEncodedName = new URLCodec(Charsets.UTF_8.name()).encode(name);
				} catch (EncoderException e) {
					logger.warn("Could not url encode package name, falling back to sanitize check", e);
					urlEncodedName = sanitizeCheck(name, false);
				}
				// Serve the request
				return Response.status(200)
						.header("Content-Type", "application/zip")
						.header("Content-Disposition", "attachment; filename=\"" + sanitizeCheck(name, false) + DOWNLOAD_EXTENSION + "\"; filename*=UTF-8''" + urlEncodedName + DOWNLOAD_EXTENSION)
						.entity(new StreamingOutput() {
							@Override
							public void write(OutputStream output) throws IOException, WebApplicationException {
								// Serve the zip stream
								try (FileInputStream fis = new FileInputStream(tmpFile)) {
									IOUtils.copy(fis, output);
								} finally {
									// Delete the temp zip.
									FileUtils.deleteQuietly(tmpFile);
								}
							}
						}).build();
			});
		} catch (NodeException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			} else {
				throw new IllegalStateException("Error downloading Content Package '" + name + "'", e);
			}
		}
	}

	@Override
	@POST
	@jakarta.ws.rs.Path("/package/{name}/zip")
	@Consumes("multipart/form-data")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)
	public ContentPackageLoadResponse upload(@PathParam("name") String name, FormDataMultiPart multiPart) throws NodeException {
		Path path = checkFeatureGetPath();
		if (multiPart.getBodyParts().isEmpty()) {
			throw new NodeException(new CNI18nString("rest.file.upload.request_invalid").toString());
		}

		FormDataBodyPart upload = Optional.ofNullable(multiPart.getField("fileBinaryData"))
				.orElseThrow(() -> new NodeException(I18NHelper.get("rest.file.upload.request_invalid")));
		String fileName = Optional.ofNullable(multiPart.getField("fileName")).map(FormDataBodyPart::getValue).orElse(upload.getContentDisposition().getFileName());

		Path packagePath = path.resolve(name);
		Path backupPath = packagePath.getParent().resolve(name + ".old");
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentPackageLoadResponse response = new ContentPackageLoadResponse();
			response.setContentPackage(executeLocked(sanitizeCheck(name, true), () -> {
				// Throw 404 on inexisting package
				StagedContentPackage packageToUpdate = loadFromFilesystem(
						packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class,
						stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
				// Keep old dir as a backup
				packagePath.toFile().renameTo(backupPath.toFile());

				Object fileEntity = upload.getEntity();

				// Locate the body part.
				InputStream fileDataInputStream;
				if (fileEntity instanceof BodyPartEntity) {
					fileDataInputStream = ((BodyPartEntity)fileEntity).getInputStream();
				} else if (fileEntity instanceof String) {
					fileDataInputStream = new ByteArrayInputStream(((String)fileEntity).getBytes(StandardCharsets.UTF_8));
				} else if (fileEntity instanceof java.io.File) {
					try {
						fileDataInputStream = new FileInputStream((java.io.File) fileEntity);
					} catch (FileNotFoundException e) {
						throw new IllegalStateException(e);
					}
				} else {
					throw new WebApplicationException(new Exception("Unsupported file body part type"));
				}

				// Store the body stream into a temp file
				java.io.File tmpFile;
				try (InputStream is = fileDataInputStream) {
					tmpFile = java.io.File.createTempFile(fileName, ".in.tmp");
					IOUtils.copy(is, new FileOutputStream(tmpFile));
				} catch (IOException e1) {
					throw new IllegalStateException(e1);
				}

				// Import the zip contents
				try (ZipFile zipFile = new ZipFile(tmpFile)) {
					Enumeration<? extends ZipEntry> enu = zipFile.entries();
					while (enu.hasMoreElements()) {
						ZipEntry entry = enu.nextElement();
						Path entryPath = packagePath.resolve(entry.getName());
						Files.createDirectories(entryPath.getParent());
						if (entry.isDirectory()) {
							continue;
						}
						try (InputStream is = zipFile.getInputStream(entry); FileOutputStream fos = new FileOutputStream(entryPath.toFile())) {
							IOUtils.copy(is, fos);
						}
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}

				packageToUpdate = loadFromFilesystem(packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME),
						StoredContentPackage.class,
						stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
				// If the package name differs from the one of an API, prefer the API version, since it has been explicitly defined.
				if (!name.equals(packageToUpdate.getName())) {
					packageToUpdate.setName(name);
					try (OutputStream os = Channels.newOutputStream(FileChannel.open(packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), 
							StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).lock().channel())) {
						mapper.writeValue(os, packageToUpdate);
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}

				// Clean off alien content
				fillContentPackageStatistic(packagePath, packageToUpdate);
				addImportProgress(packageToUpdate);

				// Cleanup backups / temp
				try {
					FileUtils.deleteDirectory(backupPath.toFile());
					tmpFile.delete();
				} catch (IOException e) {
					throw new NodeException("Error removing backup of '" + name + "'", e);
				}
				return packageToUpdate;
			}));
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			return response;
		} catch (IllegalStateException e) {
			throw new NodeException("Error downloading Content Package '" + name + "'", e);
		} finally {
			// Restore backups
			if (Files.exists(backupPath)) {
				if (Files.exists(packagePath)) {
					try {
						FileUtils.deleteDirectory(packagePath.toFile());
					} catch (IOException e) {
						throw new NodeException("Error removing broken Content Package '" + name + "'", e);
					}
				}
				backupPath.toFile().renameTo(packagePath.toFile());
			}
		}
	}

	@Override
	@GET
	@jakarta.ws.rs.Path("/package/{name}/import/errors")
	public Response getImportErrors(@PathParam("name") String name) throws NodeException {
		Path pathRoot = checkFeatureGetPath();
		try (Trx trx = ContentNodeHelper.trx()) {
			String sanitizedName = sanitizeCheck(name, false);

			// check, whether the content package actually exists (if not, a 404 response will be generated)
			Path pkgPath = pathRoot.resolve(name);
			loadFromFilesystem(pkgPath.resolve(CONTENT_PACKAGE_METADATA_FILENAME),
					StoredContentPackage.class,
					stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);

			var contentPackageErrorFile = Path.of(
					pathRoot + "/" + sanitizedName + "/" + CONTENT_PACKAGE_IMPORT_ERRORS_FILENAME).toFile();
			if (contentPackageErrorFile.exists()) {
				return Response.ok()
						.entity(contentPackageErrorFile)
						.type(MediaType.APPLICATION_JSON)
						.build();
			} else {
				return Response.ok().build();
			}
		}
	}

	@Override
	@POST
	@jakarta.ws.rs.Path("/package/{name}/import")
	public GenericResponse importIntoCms(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs, @QueryParam("test") @DefaultValue("false") boolean isTest) throws NodeException {
		Path pathRoot = checkFeatureGetPath();

		try (Trx trx = ContentNodeHelper.trx()) {
			String sanitizedName = sanitizeCheck(name, false);
			Optional<JobStatus> existingImport = imports.entrySet().stream().filter(e -> e.getKey().equals(sanitizedName) && e.getValue().getJobStatus().isRunning()).map(e -> e.getValue().getJobStatus()).findAny();
			if (existingImport.isPresent()) {
				return existingImport.get();
			} else {
				return Operator.executeLocked(
						I18NHelper.get("content.staging.import", sanitizedName),
						waitMs,
						Operator.lock(LockType.contentPackage, sanitizedName),
						() -> {
							Path packagePath = pathRoot.resolve(name);
							StagedContentPackage contentPackage = loadFromFilesystem(
									packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class,
									stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
							fillContentPackageStatistic(packagePath, contentPackage);
							StagingJobController controller = new StagingJobController(sanitizedName, contentPackage, packagePath, isTest);
							imports.put(sanitizedName, controller);

							return controller.call();
						}, java.util.function.Function.identity());
			}
		} catch (IllegalStateException e) {
			throw new NodeException("Error starting import of Content Package '" + name + "'", e);
		}
	}

	@Override
	@POST
	@jakarta.ws.rs.Path("/package/{name}/export")
	@RequiredPerm(type = PermHandler.TYPE_CONTENTSTAGING, bit = PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)
	public GenericResponse exportFromCms(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		Path pathRoot = checkFeatureGetPath();

		try (Trx trx = ContentNodeHelper.trx()) {
			String sanitizedName = sanitizeCheck(name, false);
			Path packagePath = pathRoot.resolve(sanitizedName);

			return Operator.executeLocked(I18NHelper.get("content.staging.export", sanitizedName), waitMs, Operator.lock(LockType.contentPackage, sanitizedName), () -> {
				Set<Path> deferDeleting = new HashSet<>();
				try {
					Files.walk(packagePath).forEach(i -> {
						try {
							exportIntoStageable(i, packagePath, deferDeleting);
						} catch (NodeException e) {
							throw new IllegalStateException(e);
						}
					});
					deferDeleting.stream().forEach(p -> {
						try {
							if (Files.exists(p) && Files.isDirectory(p)) {
								FileUtils.deleteDirectory(p.toFile());
							} else {
								Files.deleteIfExists(p);
							}
						} catch (IOException e) {
							throw new IllegalStateException(e);
						}
					});
				} catch (Throwable e) {
					if (e.getCause() != null && e.getCause() instanceof NodeException) {
						throw (NodeException) e.getCause();
					} else {
						throw new IllegalStateException(e);
					}
				}

				return new GenericResponse(
						new Message(Type.SUCCESS, I18NHelper.get("content.staging.export.success", sanitizedName)),
						ResponseInfo.ok("Successfully exported package"));
			}, java.util.function.Function.identity());
		} catch (IllegalStateException e) {
			throw new NodeException("Error reading Content Package '" + name + "'", e);
		}
	}

	@Override
	@POST
	@jakarta.ws.rs.Path("/package/{name}/status")
	public StagingStatusResponse status(@PathParam("name") String name, IdTypeMapRequest request) throws NodeException {
		StagingStatusResponse response = new StagingStatusResponse();
		try (Trx trx = ContentNodeHelper.trx()) {
			response.setStagingStatus(checkStagingStatus(name, request.getIds(), request.languageVariants));
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			trx.success();
		}
		return response;
	}

	@GET
	@jakarta.ws.rs.Path("/package/{name}/list/{pathSegments: .*}")
	public ContentPackageFolderResponse folderList(@PathParam("name") String name, @PathParam("pathSegments") List<PathSegment> rawSegments, @QueryParam("filter") String filter) throws NodeException {
		Path path = checkFeatureGetPath();

		final List<PathSegment> segments = rawSegments.stream().filter(s -> StringUtils.isNotBlank(s.getPath())).collect(Collectors.toList());

		try (Trx trx = ContentNodeHelper.trx()) {
			ContentPackageFolderResponse response;
			JobStatus importStatus = imports.entrySet().stream()
					.filter(e -> e.getKey().equals(name))
					.map(e -> e.getValue().getJobStatus())
					.findAny().orElse(null);
			if (importStatus == null || !importStatus.isRunning()) {
				// Check that the package is well formed
				Path packagePath = path.resolve(name);
				StagedContentPackage cpackage = executeLocked(sanitizeCheck(name, true), () -> {
					StagedContentPackage contentPackage = loadFromFilesystem(
							packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class,
							stored -> StoredContentPackage.NODE2REST.apply(stored, new StagedContentPackage()), p -> name);
					return contentPackage;
				});
				ArrayDeque<String> walker = new ArrayDeque<>();
				List<StagingBase> items = new ArrayList<>();
				List<StagingError> errors = new ArrayList<>();

				StagingErrorKind filterByError = Optional.ofNullable(filter).map(StagingErrorKind::fromValue).orElse(null);

				processContentPackageItems(packagePath, cpackage, true,
						page -> {
							if (page.getParent().getFileName().toString().equals(walker.peek())) {
								StoredPage storedPage = loadFromFilesystem(page, StoredPage.class, s -> s);
								items.add(storedPage);
							}
						},
						folder -> {
							StoredFolder storedFolder = loadFromFilesystem(folder, StoredFolder.class, s -> s);
							String folderName = storedFolder.getName();
							if (segments.size() == walker.size() && folder.getParent().getFileName().toString().equals(walker.peek())) {
								items.add(storedFolder);
							} else if (segments.size() > walker.size() && segments.get(walker.size()).getPath().equals(folderName)) {
								// Right folder, use it
								walker.push(storedFolder.getId());
							}
						},
						file -> {
							if (segments.size() == walker.size() && file.getParent().getFileName().toString().equals(walker.peek())) {
								StoredFile storedFile = loadFromFilesystem(file, StoredFile.class, s -> s);
								items.add(storedFile);
								if (!file.getParent().resolve(makeBinaryFilename(storedFile)).toFile().exists()) {
									var stagingError = new StagingError(storedFile.getId(), file.toString(), null, StagingErrorKind.NO_BINARY);
									stagingError.setRecommendation(StagingErrorRecommendation.NOT_FOUND.toString());
									errors.add(stagingError);
								}
							}
						},
						image -> {
							if (segments.size() == walker.size() && image.getParent().getFileName().toString().equals(walker.peek())) {
								StoredImage storedImage = loadFromFilesystem(image, StoredImage.class, s -> s);
								items.add(storedImage);
								if (!image.getParent().resolve(makeBinaryFilename(storedImage)).toFile().exists()) {
									var stagingError = new StagingError(storedImage.getId(), image.toString(), null, StagingErrorKind.NO_BINARY);
									stagingError.setRecommendation(StagingErrorRecommendation.NOT_FOUND.toString());
									errors.add(stagingError);
								}
							}
						},
						form -> {
							if (segments.size() == walker.size() && form.getParent().getFileName().toString().equals(walker.peek())) {
								StoredForm storedForm = loadFromFilesystem(form, StoredForm.class, s -> s);
								items.add(storedForm);
							}
						},
						binary -> {
						},
						node -> {
							if (CollectionUtils.isEmpty(segments)) {
								StoredNode storedNode = loadFromFilesystem(node, StoredNode.class, s -> s);
								items.add(storedNode);
							}
						},
						channel -> {
							if (CollectionUtils.isEmpty(segments)) {
								StoredChannel storedChannel = loadFromFilesystem(channel, StoredChannel.class, s -> s);
								items.add(storedChannel);
							}
						},
						metadata -> {},
						error -> {
							StagingErrors storedFile = loadFromFilesystemUnsafe(error, StagingErrors.class, s -> s.toString());
							errors.addAll(storedFile.getErrors());
						},
						alien -> {
							logger.warn(String.format("Alien file in the content package [%s]: %s", cpackage.getName(), alien.toString()));
						},
						(dup, orig, id) -> {
							logger.warn(String.format("Duplicated file for id [%s] in the content package [%s]: orig: %s, dup: %s", id, cpackage.getName(), orig.toString(), dup.toString()));
							var stagingErrorDub = new StagingError(id, dup.toString(), "Duplication of " + orig.toString(), StagingErrorKind.DUPLICATION);
							stagingErrorDub.setRecommendation(StagingErrorRecommendation.DUPLICATION.toString());
							errors.add(stagingErrorDub);

							var stagingError = new StagingError(id, orig.toString(), "Duplication of " + dup.toString(), StagingErrorKind.DUPLICATION);
							stagingError.setRecommendation(StagingErrorRecommendation.DUPLICATION.toString());
							errors.add(stagingError);
						},
						exception -> {
							logger.error(String.format("Could not process the content package [%s]", cpackage.getName()));
							if (exception instanceof NodeException) {
								throw (NodeException) exception;
							} else if (exception.getCause() instanceof NodeException) {
								throw (NodeException) exception.getCause();
							} else {
								throw new NodeException(exception);
							}
						}
					);
				response = ContentPackageFolderResponse.fromContent(items, errors, filterByError);
			} else {
				response = new ContentPackageFolderResponse();
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			response.setImport(importStatus);
			return response;
		} catch (IllegalStateException e) {
			throw new NodeException("Error reading Content Package '" + name + "'", e);
		}
	}

	/**
	 * Check the license feature enabled, and find the content packages filesystem path
	 * 
	 * @return
	 * @throws NodeException
	 */
	private static final Path checkFeatureGetPath() throws NodeException {
		require(Feature.CONTENT_STAGING);
		Path rootPath = Paths.get(ConfigurationValue.CONTENT_PACKAGES_PATH.get());
		if (!Files.exists(rootPath)) {
			try {
				Files.createDirectories(rootPath);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} 
		}
		return rootPath;
	}

	/**
	 * Load a stored stageable object from the filesystem.
	 * @param path file to read
	 * @param storedClass stored stageable class
	 * @param adapter stored-to-stageable adapter method
	 * 
	 * @param <S> stored stageable type
	 * @param <T> output type
	 * @return
	 * @throws NodeException
	 */
	private static final <S extends StoredModel, T> T loadFromFilesystem(Path path, Class<S> storedClass, Function<S, T> adapter) throws NodeException {
		return loadFromFilesystem(path, storedClass, adapter, p -> getObjectId(p).orElse(""));
	}

	/**
	 * Load a stored stageable object from the filesystem.
	 * @param path file to read
	 * @param storedClass stored stageable class
	 * @param adapter stored-to-stageable adapter method
	 * @param nameGetter function to get the name/ID from the given path (for generating an error message)
	 * 
	 * @param <S> stored stageable type
	 * @param <T> output type
	 * @return
	 * @throws NodeException
	 */
	private static final <S extends StoredModel, T> T loadFromFilesystem(Path path, Class<S> storedClass, Function<S, T> adapter, Function<Path, String> nameGetter) throws NodeException {
		return adapter.apply(loadFromFilesystemUnsafe(path, storedClass, nameGetter));
	}


	/**
	 * Load a stored serializable object from the filesystem.
	 * @param path file to read
	 * @param storedClass stored stageable class
	 * @param nameGetter function to get the name/ID from the given path (for generating an error message)
	 *
	 * @param <S> stored serializable type
	 * @param <T> output type
	 * @return
	 * @throws NodeException
	 */
	private static final <S extends Serializable> S loadFromFilesystemUnsafe(Path path, Class<S> storedClass, Function<Path, String> nameGetter) throws NodeException {
		try (InputStream is = Channels.newInputStream(FileChannel.open(path, StandardOpenOption.READ).lock(0, Long.MAX_VALUE, true).channel())) {
			return mapper.readValue(is, storedClass);
		} catch (NoSuchFileException noSuchFileException) {
			String typeName = Optional.ofNullable(storedClass.getAnnotation(StoredType.class)).map(StoredType::value).orElse("object");
			I18nString message = new CNI18nString(typeName + ".notfound");
			message.setParameter("0", nameGetter.apply(path));
			throw new EntityNotFoundException(message.toString());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Get the Global ID from the stored filesystem model. Type agnostic.
	 *
	 * @param path file to read
	 * @return
	 */
	private static final String idFromFilesystem(Path path) {
		try {
			JSONObject json = new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
			return (json != null && json.has("id")) ? json.getString("id") : null;
		} catch (JSONException | IOException e) {
			logger.error("Error at getting ID from " + path, e);
			return null;
		}
	}

	/**
	 * Attempt an inflation of a given node object from the staged (stored) content. 
	 * @param <N>
	 * @param <S>
	 * @param storedObject staged source content
	 * @param nodeObject node object to inflate
	 * @param nodeObjectInflater inflater method
	 * @param maybeHaveOrphans if present, this storage will be filled with the path of current stored content for a later attempt, otherwise throws {@link EntityNotFoundException}.
	 * @param currentPath the path of the staged source object
	 * @return
	 * @throws NodeException
	 */
	private <N extends StageableNodeObject, S extends StoredModel> int inflateNodeObject(S storedObject, N nodeObject, BiConsumer<S, N> nodeObjectInflater, Optional<List<Path>> maybeHaveOrphans, Path currentPath) throws NodeException {
		try {
			// Apply full object data
			nodeObjectInflater.accept(storedObject, nodeObject);
			return 1;
		} catch (EntityNotFoundException e) {
			maybeHaveOrphans.map(haveOrphans -> {
				haveOrphans.add(currentPath);
				return haveOrphans;
			}).orElseThrow(() -> e);
		}
		// Remove the tags and other references...
		storedObject.makeSafe();
		// ... and try again
		nodeObjectInflater.accept(storedObject, nodeObject);
		return 0;
	}

	/**
	 * Import a stageable node object from its stored representation
	 * @param currentPath parent folder 
	 * @param pathRoot package folder
	 * @param nodeObjectClass node object class
	 * @param storedObject stored object dummy
	 * @param nodeObjectInflater node object inflater method
	 * @param node node into which the object is imported
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 * @throws NodeException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private <N extends StageableNodeObject, S extends StoredModel> int importFromStageable(Path currentPath, Path pathRoot, Class<N> nodeObjectClass, S storedObject, 
			BiConsumer<S, N> nodeObjectInflater, Optional<List<Path>> maybeHaveOrphans, Node node, Optional<BiConsumer<S, N>> maybeDoPostSave) throws NodeException, IOException {
		storedObject = (S) loadFromFilesystem(currentPath, storedObject.getClass(), s -> s);
		N nodeObject = null;
		try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			nodeObject = MiscUtils.load(nodeObjectClass, storedObject.getId(), false, PermHandler.ObjectPermission.edit);
		}

		// Keep / fix the hierarchy
		Folder parent = null;
		String parentFolderUuid = currentPath.getParent().getFileName().toString();
		if (GlobalId.isGlobalId(parentFolderUuid)) {
			parent = MiscUtils.load(Folder.class, parentFolderUuid, false);

			// If UUID belongs to the folder that does not (yet) exist, add the object to the orphans list for later resolution
			if (parent == null) {
				maybeHaveOrphans.map(haveOrphans -> {
					haveOrphans.add(currentPath);
					return haveOrphans;
				}).orElseThrow(() -> {
					I18nString message = new CNI18nString("folder.notfound");
					message.setParameter("0", parentFolderUuid);
					return new EntityNotFoundException(message.toString());
				});
				return 0;
			}
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		// Make a new object if necessary
		if (nodeObject == null) {
			if (!t.getPermHandler().canCreate(parent, nodeObjectClass, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", t.getTable(nodeObjectClass)), parent != null ? parent.getId().toString() : StringUtils.EMPTY), parent, PermType.create);
			}
			nodeObject = t.createObject(nodeObjectClass);
			nodeObject.setGlobalId(new GlobalId(storedObject.getId()));
		} else {
			// if the object was fetched from the wastebin, we restore it first
			if (nodeObject.isDeleted()) {
				nodeObject.restore();
			}

			// get editable copy
			nodeObject = t.getObject(nodeObject, true);
		}

		// move object if necessary
		if (parent != null && node != null) {
			if (nodeObject.maybeHasChannels().map(s -> {
				try {
					return s.isMaster();
				} catch (NodeException e) {
					return false;
				}
			}).orElse(true)) {
				nodeObject.setFolder(node, parent);
			}
		}

		if (storedObject.maybeVersioned().isPresent()) {
			return importFromVersionedStageable(currentPath.getParent(), pathRoot, nodeObject,
					(Class<S>) storedObject.getClass(), nodeObjectInflater, maybeHaveOrphans, maybeDoPostSave);
		} else {
			int result = inflateNodeObject(storedObject, nodeObject, nodeObjectInflater, maybeHaveOrphans, currentPath);

			// Independently of the result, we have to store the binary data along,
			// in order for the possible references to work with this object as fully functional binary, even if the tag data is to be added later.
			if (storedObject.hasBinary()) {
				java.io.File binary = currentPath.getParent().resolve(makeBinaryFilename(storedObject)).toFile();
				if (!binary.exists()) {
					binary.createNewFile();
				}
				try (FileInputStream fos = new FileInputStream(binary)) {
					File file = nodeObject.maybeBinary().get();
					file.setFileStream(fos);
					nodeObject.save();
				}
			} else {
				nodeObject.save();
			}

			// do any postsaving actions
			if (maybeDoPostSave.isPresent()) {
				maybeDoPostSave.get().accept(storedObject, nodeObject);
			}

			return result;
		}
	}

	/**
	 * Take the node object offline, if applicable.
	 * 
	 * @param version staged version to check the offline status at.
	 * @param versionedNodeObject object to take offline
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	private void maybeTakeVersionedStageableOffline(StagedObjectVersion version, StageableVersionedNodeObject versionedNodeObject) throws ReadOnlyException, NodeException {
		if (version.getOnline() != null || version.getTimeOff() != null) {
			int timeOff = Optional.ofNullable(version.getOnline())
					.filter(online -> !online)
					.map(offline -> 0)
					.or(() -> Optional.ofNullable(version.getTimeOff())
							.map(Integer::intValue)
							.filter(time -> time > 0))
					.orElse(-1);
			if (timeOff > -1) {
				versionedNodeObject.takeOffline(timeOff);
			}
		}
	}

	/**
	 * Import a stageable node object with versions from its stored representation
	 * @param pathRoot package folder
	 * @param nodeObjectInflater node object inflater method
	 * @param currentPath parent folder 
	 * @param nodeObjectClass node object class
	 * @param storedObject stored object dummy
	 * 
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 * @throws IOException
	 */
	private <N extends StageableNodeObject, S extends StoredModel> int importFromVersionedStageable(
			Path parentFolderPath, Path pathRoot, N currentNodeObject, Class<S> storedObjectClass,
			BiConsumer<S, N> nodeObjectInflater, Optional<List<Path>> maybeHaveOrphans,
			Optional<BiConsumer<S, N>> maybeDoPostSave) throws NodeException, IOException {
		String pattern = currentNodeObject.getGlobalId() + "\\.\\d+\\.\\d+\\" + currentNodeObject.getSuffix() + "\\" + METADATA_EXTENSION;
		AtomicReference<S> published = new AtomicReference<>();
		AtomicReference<S> scheduled = new AtomicReference<>();
		AtomicReference<S> current = new AtomicReference<>();

		Files.walk(parentFolderPath, 1)
			.filter(currentPath -> currentPath.getFileName().toString().matches(pattern))
			.forEach(currentVersionPath -> {
				try {
					S storedVersioned = (S) loadFromFilesystem(currentVersionPath, storedObjectClass, s -> s);
					StagedObjectVersion version = storedVersioned.maybeVersioned().get();
					switch (version.getType()) {
					case published:
						published.set(storedVersioned);
						break;
					case scheduled:
						scheduled.set(storedVersioned);
						break;
					case current:
						current.set(storedVersioned);
						break;
					}
				} catch (NodeException e) {
					throw new IllegalStateException(e);
				}
			});
		StageableVersionedNodeObject versionedNodeObject = (StageableVersionedNodeObject) currentNodeObject;
		int result = 0;

		// Process published
		if (published.get() != null) {
			StagedObjectVersion version = published.get().maybeVersioned().get();
			Path currentPath = parentFolderPath.resolve(currentNodeObject.getGlobalId() + "." + version.getVersionNumber() + currentNodeObject.getSuffix() + METADATA_EXTENSION);
			result += inflateNodeObject(published.get(), currentNodeObject, nodeObjectInflater, maybeHaveOrphans, currentPath);
			currentNodeObject.save();

			// optionally do postsave operations
			if (maybeDoPostSave.isPresent()) {
				maybeDoPostSave.get().accept(published.get(), currentNodeObject);
			}

			versionedNodeObject.publish(0, false);

			int publishAt = version.getPublishAt();
			int offlineAt = Optional.ofNullable(version.getTimeOff()).map(Integer::intValue).orElse(0);

			if (publishAt > 0) {
				if (offlineAt > 0) {
					if (offlineAt < publishAt) {
						maybeTakeVersionedStageableOffline(version, versionedNodeObject);
						versionedNodeObject.publish(publishAt, false);
					} else {
						versionedNodeObject.publish(publishAt, false);
						maybeTakeVersionedStageableOffline(version, versionedNodeObject);
					}
				} else {
					versionedNodeObject.publish(publishAt, false);
				}
			} else if (offlineAt > 0) {
				maybeTakeVersionedStageableOffline(version, versionedNodeObject);
			}
		}

		// Process scheduled
		if (scheduled.get() != null) {
			try (TimestampTrx time = TimestampTrx.add(1, TimeUnit.SECONDS)) {
				StagedObjectVersion version = scheduled.get().maybeVersioned().get();
				Path currentPath = parentFolderPath.resolve(currentNodeObject.getGlobalId() + "." + version.getVersionNumber() + currentNodeObject.getSuffix() + METADATA_EXTENSION);
				result += inflateNodeObject(scheduled.get(), currentNodeObject, nodeObjectInflater, maybeHaveOrphans, currentPath);
				currentNodeObject.save();

				int publishAt = version.getPublishAt();
				int offlineAt = Optional.ofNullable(version.getTimeOff()).map(Integer::intValue).orElse(0);

				if (publishAt > 0) {
					if (offlineAt > 0) {
						if (offlineAt < publishAt) {
							maybeTakeVersionedStageableOffline(version, versionedNodeObject);
							versionedNodeObject.publish(publishAt, false);
						} else {
							versionedNodeObject.publish(publishAt, false);
							maybeTakeVersionedStageableOffline(version, versionedNodeObject);
						}
					} else {
						versionedNodeObject.publish(publishAt, false);
					}
				} else if (offlineAt > 0) {
					maybeTakeVersionedStageableOffline(version, versionedNodeObject);
				}
			}
		}

		// Process current
		if (current.get() != null) {
			try (TimestampTrx time = TimestampTrx.add(2, TimeUnit.SECONDS)) {
				StagedObjectVersion version = current.get().maybeVersioned().get();
				Path currentPath = parentFolderPath.resolve(currentNodeObject.getGlobalId() + "." + version.getVersionNumber() + currentNodeObject.getSuffix() + METADATA_EXTENSION);
				result += inflateNodeObject(current.get(), currentNodeObject, nodeObjectInflater, maybeHaveOrphans, currentPath);			
				currentNodeObject.save();
				currentNodeObject.unlock();

				maybeTakeVersionedStageableOffline(version, versionedNodeObject);
			}

			// optionally do postsave operations
			if (maybeDoPostSave.isPresent()) {
				maybeDoPostSave.get().accept(current.get(), currentNodeObject);
			}
		}
		return result;
	}

	/**
	 * Export a stageable node object into the stored state to the filesystem
	 * @param currentPath parent folder 
	 * @param pathRoot package folder
	 * @param nodeObjectClass node object class
	 * @param storedObject stored object dummy
	 * @param deferDeleting a container for package items to be deleted after the export process
	 * @param nodeObjectInflater stored object inflater method
	 * 
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private <N extends StageableNodeObject, S extends StoredModel> void exportIntoStageable(Path currentPath, Path pathRoot, Class<N> nodeObjectClass, S storedObject, BiConsumer<N, S> modelInflater, Set<Path> deferDeleting) throws NodeException {
		if (storedObject.maybeVersioned().isPresent()) {
			// Sadly, we cannot apply exportIntoVersionedStageable() right away because of the generic type mismatch
			throw new IllegalArgumentException("Node object '" + storedObject.getId() + storedObject.getSuffix() + " is versioned and not applicable here. Use exportIntoVersionedStageable().");
		}
		storedObject = (S) loadFromFilesystem(currentPath, storedObject.getClass(), s -> s);
		N nodeObject = (N) loadFromDatabase(nodeObjectClass, storedObject.getId());

		Path folderPath;
		if (nodeObject instanceof Node) {
			folderPath = currentPath.getParent();
		} else {
			Optional<Node> maybeChannel = nodeObject.maybeHasChannels().flatMap(ch -> {
				try {
					return ch.maybeGetChannel();
				} catch (NodeException e) {
					throw new IllegalStateException(e);
				}
			});
			try (ChannelTrx ctrx = new ChannelTrx(maybeChannel.orElse(null))) {
				folderPath = addFolderPath(pathRoot, nodeObject.getFolder(), false, maybeChannel);
			}
		}
		addStageable(folderPath, nodeObject, storedObject, modelInflater, Optional.empty(), Optional.ofNullable(deferDeleting));
		if (storedObject.hasBinary()) {
			synchronizeBinary(folderPath, storedObject.getId(), storedObject.getSuffix(), nodeObject.maybeBinary(), Optional.ofNullable(deferDeleting));
		}
		if (!currentPath.toString().startsWith(folderPath.toString())) {
			deferDeleting.add(currentPath);
		}
	}

	/**
	 * Export a stageable node object with versions into the stored state to the filesystem
	 * @param currentPath parent folder 
	 * @param pathRoot package folder
	 * @param nodeObjectClass node object class
	 * @param storedObject stored object dummy
	 * @param deferDeleting a container for package items to be deleted after the export process
	 * @param nodeObjectInflater stored object inflater method
	 * 
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 */
	@SuppressWarnings("unchecked")
	private <N extends StageableVersionedNodeObject, S extends StoredModel> 
			void exportIntoVersionedStageable(Path currentPath, Path pathRoot, Class<N> nodeObjectClass, S storedObject, BiConsumer<N, S> modelInflater, Set<Path> deferDeleting) throws NodeException {
		storedObject = (S) loadFromFilesystem(currentPath, storedObject.getClass(), s -> s);
		try {
			N nodeObject = (N) loadFromDatabase(nodeObjectClass, storedObject.getId());
			Optional<Node> maybeChannel = nodeObject.maybeHasChannels().flatMap(ch -> {
				try {
					return ch.maybeGetChannel();
				} catch (NodeException e) {
					throw new IllegalStateException(e);
				}
			});
			try (ChannelTrx ctrx = new ChannelTrx(maybeChannel.orElse(null))) {
				Path folderPath = addFolderPath(pathRoot, nodeObject.getFolder(), false, maybeChannel);
				addVersionedStageable(folderPath, nodeObject, storedObject, modelInflater, Optional.ofNullable(deferDeleting));
				if (!currentPath.toString().startsWith(folderPath.toString())) {
					deferDeleting.add(currentPath);
				}
			}
		} catch (EntityNotFoundException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("NodeObject is missing in CMS: " + currentPath, e);
			}
			// when the object was not found in the CMS (any more), we also want to remove the file in the content package
			deferDeleting.add(currentPath);
		}
	}

	/**
	 * Import the node from the given path into the CMS
	 * @param currentPath path to a node or channel file
	 * @param pathRoot package root
	 * @param progress import progress
	 * @param maybeHaveOrphans storage for the paths that cannot be imported right away due to the orphaned links.
	 * @return pair of global node ID and root folder ID
	 * @throws NodeException
	 * @throws IOException
	 */
	private Pair<String, String> importNodeFromStageable(Path currentPath, Path pathRoot, JobProgress progress,
			Optional<List<Path>> maybeHaveOrphans) throws NodeException, IOException {
		String filename = currentPath.getFileName().toString();
		logger.info("Processing " + currentPath);

		AtomicReference<Pair<String, String>> returnedPair = new AtomicReference<>();

		int processed = 0;
		if (filename.endsWith(NODE_EXTENSION)) {
			processed = importFromStageable(currentPath, pathRoot, Node.class, new StoredNode(), (s, n) -> {
				returnedPair.set(Pair.of(s.getId(), s.getRootFolderId()));
				StagingModelBuilder.getNode(s, n);
			}, maybeHaveOrphans, null, Optional.of((s, n) -> {
				StagingModelBuilder.setFeatures(s, n);
				StagingModelBuilder.setFormTypes(s, n);
				StagingModelBuilder.setConstructs(s, n);
				StagingModelBuilder.setObjectProperties(s, n);
				StagingModelBuilder.setPackages(s, n);
				PermissionStore.getInstance().refreshObject(Node.TYPE_NODE, n.getFolder().getId());
			}));
		} else if (filename.endsWith(CHANNEL_EXTENSION)) {
			processed = importFromStageable(currentPath, pathRoot, Node.class, new StoredChannel(), (s, n) -> {
				returnedPair.set(Pair.of(s.getId(), s.getRootFolderId()));
				StagingModelBuilder.getChannel(s, n);
			}, maybeHaveOrphans, null, Optional.of((s, n) -> StagingModelBuilder.setFeatures(s, n)));
		}
		logger.info(" ... processed = " + processed);
		progress.incDone(processed);

		return returnedPair.get();
	}

	/**
	 * Import a stored file into CMS. Remove alien (unrecognizable) content from the package.
	 * @param currentPath a file to import
	 * @param pathRoot package root
	 * @param progress import progress
	 * @param maybeHaveOrphans storage for the paths that cannot be imported right away due to the orphaned links.
	 * @param errors a container for referenced objects, that have been changed before the import.
	 * @param node node into which the object shall be imported
	 * @throws NodeException
	 * @throws IOException
	 */
	private void importFromStageable(Path currentPath, Path pathRoot, JobProgress progress,
			Optional<List<Path>> maybeHaveOrphans, Set<StagingError> errors, Node node) throws NodeException, IOException {
		if (!Files.isDirectory(currentPath)) {
			// operator, which is called when a referenced object (content object), could not be found.
			com.gentics.contentnode.etc.Operator onReferenceNotFound = () -> {
				// if maybeHaveOrphans is not empty and the list does not yet contain the path,
				// we add the path to the list of objects
				maybeHaveOrphans.ifPresent(list -> {
					if (!list.contains(currentPath)) {
						list.add(currentPath);
					}
				});
			};

			String filename = currentPath.getFileName().toString();
			logger.info("Processing " + currentPath);
			int processed = 0;
			if (filename.endsWith(PAGE_EXTENSION)) {
				processed = importFromStageable(currentPath, pathRoot, Page.class, new StoredPage(),
						(n, s) -> StagingModelBuilder.getPage(currentPath, n, s, errors, onReferenceNotFound, node), maybeHaveOrphans, node,
						Optional.of((s, n) -> StagingModelBuilder.setMultichannellingRestrictions(s, n)));
			} else if (filename.endsWith(FORM_EXTENSION)) {
				processed = importFromStageable(currentPath, pathRoot, Form.class, new StoredForm(),
						(n, s) -> StagingModelBuilder.getForm(currentPath, n, s, errors, onReferenceNotFound), maybeHaveOrphans, node, Optional.empty());
			} else if (filename.endsWith(FILE_EXTENSION)) {
				processed = importFromStageable(currentPath, pathRoot, File.class, new StoredFile(),
						(s, n) -> StagingModelBuilder.getFile(currentPath, s, n, errors, onReferenceNotFound, node), maybeHaveOrphans, node,
						Optional.of((s, n) -> StagingModelBuilder.setMultichannellingRestrictions(s, n)));
			} else if (filename.endsWith(IMAGE_EXTENSION)) {
				processed = importFromStageable(currentPath, pathRoot, ImageFile.class, new StoredImage(),
						(s, n) -> StagingModelBuilder.getImage(currentPath, s, n, errors, onReferenceNotFound, node), maybeHaveOrphans, node,
						Optional.of((s, n) -> StagingModelBuilder.setMultichannellingRestrictions(s, n)));
			} else if (filename.endsWith(FOLDER_EXTENSION)) {
				processed = importFromStageable(currentPath, pathRoot, Folder.class, new StoredFolder(),
						(s, n) -> StagingModelBuilder.getFolder(currentPath, s, n, errors, onReferenceNotFound, node), maybeHaveOrphans, node,
						Optional.of((s, n) -> {
							StagingModelBuilder.setMultichannellingRestrictions(s, n);
							PermissionStore.getInstance().refreshObject(Folder.TYPE_FOLDER, n.getId());
						}));
			} else if (filename.endsWith(NODE_EXTENSION)
					|| filename.endsWith(CHANNEL_EXTENSION)
					|| filename.equals(CONTENT_PACKAGE_METADATA_FILENAME) 
					|| filename.endsWith(BINARY_EXTENSION)
					|| filename.endsWith(CONTENT_PACKAGE_IMPORT_ERRORS_FILENAME)) {
				// ignore - should already be processed
			} else {
				logger.warn("Removing alien file " + currentPath.toAbsolutePath());
				try {
					Files.delete(currentPath);
				} catch (IOException e) {
					logger.error("Cannot remove alien file " + currentPath.toAbsolutePath(), e);
				}
			}
			logger.info(" ... processed = " + processed);
			progress.incDone(processed);
		}
	}

	/**
	 * Export a CMS object into the stored state. Remove alien (unrecognizable) content from the package.
	 * @param currentPath a file to import
	 * @param pathRoot package root
	 * @param deferDeleting a container for package items to be deleted after the export process
	 * 
	 * @throws NodeException
	 */
	private void exportIntoStageable(Path currentPath, Path pathRoot, Set<Path> deferDeleting) throws NodeException {
		String filename = currentPath.getFileName().toString();
		try {
			if (Files.isDirectory(currentPath)) {
				Path folderMetadata = currentPath.getParent().resolve(filename + FOLDER_EXTENSION);
				Path packageMetadata = currentPath.resolve(CONTENT_PACKAGE_METADATA_FILENAME);
				if (Files.exists(folderMetadata)) {
					exportIntoStageable(folderMetadata, pathRoot, Folder.class, new StoredFolder(), (n, s) -> StagingModelBuilder.getFolder(n, s), deferDeleting);
				} else if (Files.exists(packageMetadata)) {
					logger.info("Content Package to export: " + currentPath.toAbsolutePath());
					// ignore
				} else {
					logger.warn("Removing alien folder " + currentPath.toAbsolutePath());
					deferDeleting.add(currentPath);
				}
			} else {
				if (filename.endsWith(PAGE_EXTENSION)) {
					exportIntoVersionedStageable(currentPath, pathRoot, Page.class, new StoredPage(), (n, s) -> StagingModelBuilder.getPage(n, s), deferDeleting);
				} else if (filename.endsWith(FORM_EXTENSION)) {
					exportIntoVersionedStageable(currentPath, pathRoot, Form.class, new StoredForm(), (n, s) -> StagingModelBuilder.getForm(n, s), deferDeleting);
				} else if (filename.endsWith(FILE_EXTENSION)) {
					exportIntoStageable(currentPath, pathRoot, File.class, new StoredFile(), (n, s) -> StagingModelBuilder.getFile(n, s), deferDeleting);
				} else if (filename.endsWith(IMAGE_EXTENSION)) {
					exportIntoStageable(currentPath, pathRoot, ImageFile.class, new StoredImage(), (n, s) -> StagingModelBuilder.getImage(n, s), deferDeleting);
				} else if (filename.endsWith(CHANNEL_EXTENSION)) {
					exportIntoStageable(currentPath, pathRoot, Node.class, new StoredChannel(), (n, s) -> StagingModelBuilder.getNode(n, s), deferDeleting);
				} else if (filename.endsWith(NODE_EXTENSION)) {
					exportIntoStageable(currentPath, pathRoot, Node.class, new StoredNode(), (n, s) -> StagingModelBuilder.getNode(n, s), deferDeleting);
				} else if (filename.endsWith(FOLDER_EXTENSION) 
						|| filename.equals(CONTENT_PACKAGE_METADATA_FILENAME) 
						|| filename.endsWith(BINARY_EXTENSION)) {
					// ignore - should already be processed
				} else {
					logger.warn("Removing alien file " + currentPath.toAbsolutePath());
					deferDeleting.add(currentPath);
				}
			}
		} catch (EntityNotFoundException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("No NodeObject found for the staging entity: " + currentPath, e);
			}
			// when the object was not found in the CMS (any more), we also want to remove the file in the content package, including all its peers
			if (filename.endsWith(FILE_EXTENSION) || filename.endsWith(IMAGE_EXTENSION)) {
				Path binaryPath = currentPath.resolveSibling(filename.substring(0, filename.lastIndexOf(METADATA_EXTENSION)) + BINARY_EXTENSION);
				if (Files.exists(binaryPath)) {
					deferDeleting.add(binaryPath);
				}
			} else if (filename.endsWith(CHANNEL_EXTENSION)) {
				Path folderPath = currentPath.resolveSibling(filename.substring(0, filename.lastIndexOf(CHANNEL_EXTENSION)));
				if (Files.exists(folderPath)) {
					deferDeleting.add(folderPath);
				}
				Path folderMetadataPath = currentPath.resolveSibling(filename.substring(0, filename.lastIndexOf(CHANNEL_EXTENSION)) + FOLDER_EXTENSION);
				if (Files.exists(folderMetadataPath)) {
					deferDeleting.add(folderMetadataPath);
				}
			} else if (filename.endsWith(NODE_EXTENSION)) {
				Path folderPath = currentPath.resolveSibling(filename.substring(0, filename.lastIndexOf(NODE_EXTENSION)));
				if (Files.exists(folderPath)) {
					deferDeleting.add(folderPath);
				}
				Path folderMetadataPath = currentPath.resolveSibling(filename.substring(0, filename.lastIndexOf(NODE_EXTENSION)) + FOLDER_EXTENSION);
				if (Files.exists(folderMetadataPath)) {
					deferDeleting.add(folderMetadataPath);
				}
			} else if (filename.endsWith(FOLDER_EXTENSION)) {
				Path folderPath = currentPath.resolveSibling(filename.substring(0, filename.lastIndexOf(FOLDER_EXTENSION)));
				if (Files.exists(folderPath)) {
					deferDeleting.add(folderPath);
				}
			} else if (Files.isDirectory(currentPath)) {
				Path channelMetadataPath = currentPath.resolveSibling(filename + CHANNEL_EXTENSION);
				if (Files.exists(channelMetadataPath)) {
					deferDeleting.add(channelMetadataPath);
				}
				Path nodeMetadataPath = currentPath.resolveSibling(filename + NODE_EXTENSION);
				if (Files.exists(nodeMetadataPath)) {
					deferDeleting.add(nodeMetadataPath);
				}
				Path folderMetadataPath = currentPath.resolveSibling(filename + FOLDER_EXTENSION);
				if (Files.exists(folderMetadataPath)) {
					deferDeleting.add(folderMetadataPath);
				}
			}
			deferDeleting.add(currentPath);
		}
	}

	/**
	 * Add the import progress info of the latest (current) import (if any)
	 * @param cp content package to modify
	 */
	private void addImportProgress(StagedContentPackage cp) {
		Optional.ofNullable(imports.get(cp.getName())).ifPresent(lastImport -> cp.setImport(lastImport.getJobStatus()));
	}

	/**
	 * Update content package at the filesystem, updating its timestamp.
	 *
	 * @param packageFolderPath
	 * @param pkg
	 * @return timestamp of an update.
	 * @throws NodeException
	 */
	private static final Integer updateStoredPackage(Path packageFolderPath, StagedContentPackage pkg) throws NodeException {
		return updateStoredPackage(packageFolderPath, pkg, new StoredContentPackage());
	}

	/**
	 * Update content package at the filesystem, updating its timestamp.
	 *
	 * @param packageFolderPath
	 * @param pkg
	 * @param stored model to update and save
	 * @return timestamp of an update.
	 * @throws NodeException
	 */
	private static final Integer updateStoredPackage(Path packageFolderPath, StagedContentPackage pkg, StoredContentPackage stored) throws NodeException {
		try (OutputStream os = Channels.newOutputStream(FileChannel.open(packageFolderPath.resolve(CONTENT_PACKAGE_METADATA_FILENAME),
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).lock().channel())) {
			stored = StoredContentPackage.REST2NODE.apply(pkg, stored);
			mapper.writeValue(os, stored);
			return stored.getTimestamp();
		} catch (FileAlreadyExistsException existsException) {
			throw new DuplicateValueException("name", pkg.getName());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Fill the package statistics. Remove alien (unrecognizable) content from the package.
	 * 
	 * @param path
	 * @param cp
	 * @throws NodeException
	 */
	private static void fillContentPackageStatistic(Path path, StagedContentPackage cp) throws NodeException {
		fillContentPackageStatistic(path, cp, false);
	}

	/**
	 * Fill the package statistics. Remove alien (unrecognizable) content from the package. Optionally throw an exception on duplicated content.
	 * 
	 * @param path
	 * @param cp
	 * @param throwOnDuplications set this if you want an exception thrown on a duplicated ID in the package
	 * @throws NodeException
	 */
	public static void fillContentPackageStatistic(Path path, StagedContentPackage cp, boolean throwOnDuplications) throws NodeException {
		processContentPackageItems(
				path, cp, false,
				page -> cp.setPages(cp.getPages() + 1),
				folder -> cp.setFolders(cp.getFolders() + 1),
				file -> cp.setFiles(cp.getFiles() + 1),
				image -> cp.setImages(cp.getImages() + 1),
				form -> cp.setForms(cp.getForms() + 1),
				binary -> {},
				node -> cp.setNodes(cp.getNodes() + 1),
				channel -> cp.setChannels(cp.getChannels() + 1),
				metadata -> {},
				error -> {},
				alien -> {
					logger.warn("Removing alien file " + alien.toAbsolutePath());
					try {
						Files.delete(alien);
					} catch (IOException e) {
						logger.error("Cannot remove alien file " + alien.toAbsolutePath(), e);
					}
				},
				(dup, orig, id) -> {
					if (throwOnDuplications) {
						throw new IllegalStateException(String.format("Duplicated element #[%s] found on paths [%s, %s]", id, dup, orig));
					} else {
						return;
					}

				},
				exception -> {
					logger.error("Failed to collect statistic for ContentPackage " + path.getFileName(), exception);
					// TODO drop 500 here?
				});
	}

	/**
	 * Walk over content staging package contents, performing operations on `doOnXXX` consumers.
	 *
	 * @param path package path
	 * @param cp package definition
	 * @param sort set to preserve depth-first order of execution
	 * @param doOnPage
	 * @param doOnFolder
	 * @param doOnFile
	 * @param doOnImage
	 * @param doOnForm
	 * @param doOnBinary
	 * @param doOnNode
	 * @param doOnChannel
	 * @param doOnMetadata
	 * @param doOnErrors
	 * @param doOnAlien
	 * @param doOnDups
	 * @param doOnException
	 * @throws NodeException
	 */
	public static void processContentPackageItems(Path path, StagedContentPackage cp, boolean sort,
				Consumer<Path> doOnPage, Consumer<Path> doOnFolder, Consumer<Path> doOnFile, Consumer<Path> doOnImage,
				Consumer<Path> doOnForm, Consumer<Path> doOnBinary, Consumer<Path> doOnNode, Consumer<Path> doOnChannel,
				Consumer<Path> doOnMetadata, Consumer<Path> doOnErrors, Consumer<Path> doOnAlien, TriConsumer<Path, Path, String> doOnDups, Consumer<Throwable> doOnException
			) throws NodeException {
		Stream<Path> walker = null;
		try {
			Map<String, Path> encounteredIDs = new HashMap<>();
			walker = Files.walk(path);
			if (sort) {
				walker = walker.sorted();
			}
			walker.forEach(i -> {
				try {
					if (encounteredBefore(i, encounteredIDs)) {
						String objectId = getObjectId(i).get();
						doOnDups.accept(i, encounteredIDs.get(objectId), objectId);
					}
					if (Files.isRegularFile(i)) {
						if (i.getFileName().toString().equals(CONTENT_PACKAGE_METADATA_FILENAME)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, "metadata", i));
							}
							doOnMetadata.accept(i);
						} else if (i.getFileName().toString().equals(CONTENT_PACKAGE_IMPORT_ERRORS_FILENAME)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, "errors", i));
							}
							doOnErrors.accept(i);
						} else if (i.getFileName().toString().endsWith(PAGE_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedPage.TYPE_NAME, i));
							}
							doOnPage.accept(i);
						} else if (i.getFileName().toString().endsWith(FORM_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedForm.TYPE_NAME, i));
							}
							doOnForm.accept(i);
						} else if (i.getFileName().toString().endsWith(FILE_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedFile.TYPE_NAME, i));
							}
							doOnFile.accept(i);
						} else if (i.getFileName().toString().endsWith(IMAGE_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedImage.TYPE_NAME, i));
							}
							doOnImage.accept(i);
						} else if (i.getFileName().toString().endsWith(NODE_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedNode.TYPE_NAME, i));
							}
							doOnNode.accept(i);
						} else if (i.getFileName().toString().endsWith(FOLDER_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedFolder.TYPE_NAME, i));
							}
							doOnFolder.accept(i);
						} else if (i.getFileName().toString().endsWith(CHANNEL_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, StagedChannel.TYPE_NAME, i));
							}
							doOnChannel.accept(i);
						} else if (i.getFileName().toString().endsWith(BINARY_EXTENSION)) {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, "binary data", i));
							}
							doOnBinary.accept(i);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug(String.format(FOUND_AT_LOG, "alien file", i));
							}
							doOnAlien.accept(i);
						}
					}
				} catch (NodeException e) {
					throw new IllegalStateException(e);
				}
			});
		} catch (Throwable e) {
			if (e.getCause() != null && e.getCause() instanceof NodeException) {
				e = e.getCause();
			}
			doOnException.accept(e);
		} finally {
			if (walker != null) {
				walker.close();
			}
		}
	}

	/**
	 * Check whether the path denotes the metadata file of an object, which we encountered before (by checking, whether the globalId is found in the given set).
	 * If the object was not encountered before, the globelId will be put into the set
	 * @param path path to a possible metadata file
	 * @param encounteredIDs set of already encountered IDs (may be modified)
	 * @return true iff the path denotes a metadata file and the entity was encountered before
	 */
	private static boolean encounteredBefore(Path path, Map<String, Path> encounteredIDs) {
		Optional<String> objectId = getObjectId(path);
		if (objectId.isPresent()) {
			if (encounteredIDs.containsKey(objectId.get())) {
				return true;
			} else {
				encounteredIDs.put(objectId.get(), path);
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Return the optional object id of the object, which is stored in the file denoted by the given path.
	 * @param path path to a possible metadata file
	 * @return optional global object ID
	 */
	public static Optional<String> getObjectId(Path path) {
		if (!Files.isDirectory(path)) {
			Matcher matcher = METADATA_FILE_PATTERN.matcher(path.getFileName().toString());
			if (matcher.matches()) {
				return Optional.of(matcher.group("id"));
			}
		}

		return Optional.empty();
	}

	/**
	 * Check if the given node object is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObject
	 * @param stagingPackageName
	 * @return
	 * @throws NodeException
	 */
	public static <T extends StageableNodeObject> StagingStatus checkStagingStatus(T nodeObject, String stagingPackageName) throws NodeException {
		return checkStagingStatus(Collections.singleton(nodeObject), stagingPackageName, T::getId).getOrDefault(nodeObject.getId(), new StagingStatus());
	}

	/**
	 * Check if the given objects are contained in the content package.
	 * This method expects a current transaction.
	 * 
	 * @param stagingPackageName package name
	 * @param ids map of object types to list of IDs
	 * @param useVariants true to also include variants (i.e. language variants of pages) of the queried objects
	 * @return staging status per object ID
	 * @throws NodeException
	 */
	public static Map<String, StagingStatus> checkStagingStatus(String stagingPackageName, Map<String, List<String>> ids, boolean useVariants) throws NodeException {
		Path pathRoot = checkFeatureGetPath();
		Transaction t = TransactionManager.getCurrentTransaction();

		Path packagePath = pathRoot.resolve(stagingPackageName);
		return executeLocked(sanitizeCheck(stagingPackageName, true), () -> {
			StoredContentPackage stagedPackage = loadFromFilesystem(packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class, stored -> stored, p -> stagingPackageName);
			Map<String, StagingStatus> statusMap = new HashMap<>();
			if (ids != null) {
				for (Entry<String, List<String>> entry : ids.entrySet()) {
					StageableObjectType objectType = StageableObjectType.parseTypeId(entry.getKey());
					List<? extends StageableNodeObject> objects = t.getObjectsByStringIds(objectType.getNodeObjectClass(), entry.getValue());
					statusMap.putAll(checkStagingStatus(objects, stagedPackage, packagePath, o -> o.getGlobalId().toString(), useVariants));
				}
			}
			return statusMap;
		});
	}

	/**
	 * Check if the given node object collection is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObjects
	 * @param stagingPackageName
	 * @return map of object ID / status.
	 * @throws NodeException
	 */
	public static <T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects, String stagingPackageName, Function<T, K> keyProvider) throws NodeException {
		return checkStagingStatus(nodeObjects, stagingPackageName, keyProvider, false);
	}

	/**
	 * Check if the given node object collection is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObjects
	 * @param stagingPackageName
	 * @param keyProvider
	 * @param useVariants should the statuc include the object variants, if there are any
	 * @return map of object ID / status.
	 * @throws NodeException
	 */
	public static <T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects, String stagingPackageName, Function<T, K> keyProvider, boolean useVariants) throws NodeException {
		java.nio.file.Path path = checkFeatureGetPath();
		String name = sanitizeCheck(stagingPackageName, false);
		try {
			return executeLocked(name, () -> {
				Path packagePath = path.resolve(name);
				// throw 404 on unknown name / 40x on access issues / 500 on fatal issues
				StoredContentPackage stagingPackage = loadFromFilesystem(packagePath.resolve(CONTENT_PACKAGE_METADATA_FILENAME), StoredContentPackage.class, stored -> stored, p -> name);
				return checkStagingStatus(nodeObjects, stagingPackage, packagePath, keyProvider, useVariants);
			});
		} catch (IllegalStateException e) {
			if (e.getCause() instanceof EntityNotFoundException) {
				logger.warn("No content package " + stagingPackageName + " found", e);
				return Collections.emptyMap();
			} else if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw new NodeException("Error reading Content Package '" + stagingPackageName + "'", e);
			}
		}
	}

	/**
	 * Check if the given node object collection is contained into the asked content package, and how recent its data is.
	 * 
	 * @param <T>
	 * @param <K>
	 * @param nodeObjects
	 * @param stagingPackage
	 * @param packagePath
	 * @param keyProvider
	 * @param nodeObjectResource
	 * @return
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private static <T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects, StoredContentPackage stagingPackage, Path packagePath, Function<T, K> keyProvider, boolean useVariants) throws NodeException {
		Map<K, StagingStatus> result = new HashMap<>();
		for (T nodeObject : nodeObjects) {
			Collection<T> variants = Collections.singleton(nodeObject);
			if (useVariants) {
				Optional<Collection<? extends StageableNodeObject>> maybeVariants = nodeObject.maybeGetVariants();
				if (maybeVariants.isPresent()) {
					variants = (Collection<T>) maybeVariants.get();
				}
			}
			for (T variant : variants) {
				StagingStatus stagingStatus = new StagingStatus();
				stagingStatus.setPackageName(stagingPackage.getName());
				String uuid = variant.getGlobalId().toString();
				try {
					Files.walk(packagePath).filter(i -> i.getFileName().toString().startsWith(uuid)).forEach(i -> {
						if (Files.isDirectory(i)) {
							if (Files.exists(i.getParent().resolve(i.getFileName() + FOLDER_EXTENSION))) {
								stagingStatus.setIncluded(true);
							} else if (Files.exists(i.getParent().resolve(i.getFileName() + CHANNEL_EXTENSION))) {
								stagingStatus.setIncluded(true);
							} else if (Files.exists(i.getParent().resolve(i.getFileName() + NODE_EXTENSION))) {
								stagingStatus.setIncluded(true);
							}
						} else {
							// Pages are checked by versions as well
							if (i.getFileName().toString().endsWith(PAGE_EXTENSION)) {
								stagingStatus.setIncluded(true);
							} else if (i.getFileName().toString().endsWith(FORM_EXTENSION)) {
								// Forms are checked by versions as well
								stagingStatus.setIncluded(true);
							} else if (i.getFileName().toString().endsWith(FILE_EXTENSION)) {
								stagingStatus.setIncluded(true);
							} else if (i.getFileName().toString().endsWith(IMAGE_EXTENSION)) {
								stagingStatus.setIncluded(true);
							} else if (i.getFileName().toString().endsWith(NODE_EXTENSION)) {
								stagingStatus.setIncluded(true);
							} else if (i.getFileName().toString().endsWith(FOLDER_EXTENSION) 
									|| i.getFileName().toString().endsWith(BINARY_EXTENSION)
									|| i.getFileName().toString().endsWith(CHANNEL_EXTENSION)
									|| i.getFileName().toString().endsWith(NODE_EXTENSION)
									|| i.getFileName().toString().equals(CONTENT_PACKAGE_METADATA_FILENAME) 
									) {
								// ignore
							}
						}
					});
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
				result.put(keyProvider.apply(variant), stagingStatus);
			}
		}
		return result;			
	}

	/**
	 * Sanitize the given name for the filesystem usage, and check if it equals the initial one.
	 * 
	 * @param name
	 * @param throwOnMismatch should the exception be thrown on the names mismatch?
	 * @return
	 * @throws NodeException
	 */
	private static String sanitizeCheck(String name, boolean throwOnMismatch) throws NodeException {
		String lockKey = FileFactory.sanitizeName(name);

		if (throwOnMismatch && !name.equals(lockKey)) {
			throw new RestMappedException(I18NHelper.get("content.staging.invalidname", name)).setMessageType(Message.Type.WARNING)
				.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		return lockKey;
	}

	/**
	 * Add or remove a binary data of the node object of given UUID.
	 * @param currentPath parent folder
	 * @param currentUuid object UUID
	 * @param maybeAddNew should the new binary be added / existing overwritten, if provided? If not, the binary is deleted
	 * @param maybeDeferDelete an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * 
	 * @throws NodeException
	 */
	private void synchronizeBinary(Path currentPath, String currentUuid, String suffix, Optional<File> maybeAddNew, Optional<Set<Path>> maybeDeferDelete) throws NodeException {
		String currentFilename = currentUuid + suffix + BINARY_EXTENSION;
		Path currentFile = currentPath.resolve(currentFilename);
		maybeAddNew.map(newFile -> {
			maybeDeferDelete.ifPresent(deferDelete -> {
				deferDelete.remove(currentFile);
			});
			try (FileInputStream fis = new FileInputStream(newFile.getBinFile())) {
				Files.deleteIfExists(currentFile);
				return Files.copy(fis, currentFile);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).orElseGet(() -> {
			maybeDeferDelete.map(deferred -> deferred.add(currentFile)).orElseGet(() -> {
				try {
					return Files.deleteIfExists(currentFile);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
			return 0L;
		});
	}

	/**
	 * Check the node object access permissions, consume it afterwards.
	 * @param nodeObject object
	 * @param consumer object consumer
	 * 
	 * @param <N> node object type
	 * @throws NodeException
	 */
	private final <N extends StageableNodeObject> void consumeViewable(N nodeObject, Consumer<N> consumer) throws NodeException {
		if (TransactionManager.getCurrentTransaction().getPermHandler().canView(nodeObject)) {
			consumer.accept(nodeObject);
		} else {
			throw new InsufficientPrivilegesException("No read privileges on " + nodeObject.getClass().getSimpleName() + " { " + nodeObject.getGlobalId() + " }", nodeObject, PermType.read);
		}
	}

	/**
	 * Load a node object for the reading from database by its class + UUID.
	 * 
	 * @param <S> node object type
	 * @param classOfS
	 * @param objectUuid
	 * @return
	 * @throws NodeException
	 */
	private final <S extends StageableNodeObject> S loadFromDatabase(Class<S> classOfS, String objectUuid) throws NodeException {
		return loadFromDatabase(classOfS, objectUuid, PermHandler.ObjectPermission.view, Optional.empty());
	}

	/**
	 * Load a node object with the given permission from database by its class + UUID.
	 * 
	 * @param <S> node object type
	 * @param classOfS
	 * @param objectUuid
	 * @param permission
	 * @param timestamp object version, if applicable
	 * @return
	 * @throws NodeException
	 */
	private final <S extends StageableNodeObject> S loadFromDatabase(Class<S> classOfS, String objectUuid,
			PermHandler.ObjectPermission permission, Optional<Integer> timestamp) throws NodeException {
		return MiscUtils.load(classOfS, objectUuid, permission);
	}

	/**
	 * Make a package path for the folder node object
	 * @param currentPath package path
	 * @param folder
	 * 
	 * @return
	 * @throws NodeException
	 */
	private final Path findFolderPath(Path currentPath, Folder folder) throws NodeException {
		List<Folder> folderPath = folder.getParents();
		Collections.reverse(folderPath);
		folderPath.add(folder);

		currentPath = folderPath.stream().map(f -> f.getGlobalId().toString()).reduce(currentPath, (p, c) -> p.resolve(c), (p, c) -> p.resolve(c));

		return currentPath;
	}

	/**
	 * Make a filesystem directory path for the folder node object
	 * @param currentPath
	 * @param folder
	 * @param recursive should all the child paths of a folder be processed?
	 * 
	 * @return
	 * @throws NodeException
	 */
	private final Path addFolderPath(Path currentPath, Folder folder, boolean recursive, Optional<Node> maybeChannel) throws NodeException {
		// Add node in place, Reroute to the channel, if required
		Node node = maybeChannel.orElse(folder.getNode());

		List<Folder> folderPath = folder.getParents();
		Collections.reverse(folderPath);
		folderPath.add(folder);

		// Trace + add folders if necessary
		for (Folder f : folderPath) {
			Path currentNodePath = currentPath;

			// for the root folder, also add the node/channel
			if (f.isRoot()) {
				if (node.isChannel()) {
					addStageable(currentPath, node, new StoredChannel(), (n, s) -> StagingModelBuilder.getChannel(n, s), Optional.empty(), Optional.empty());
				} else {
					addStageable(currentPath, node, new StoredNode(), (n, s) -> StagingModelBuilder.getNode(n, s), Optional.empty(), Optional.empty());
				}
			}

			consumeViewable(f, child -> addStageable(currentNodePath, child, new StoredFolder(), (n, s) -> StagingModelBuilder.getFolder(n, s), 
					(recursive && folder.getGlobalId().equals(child.getGlobalId())) ? Optional.of(this::consumeRecursiveFolder) : Optional.empty(), 
					Optional.empty(), node.isChannel()
			));
			currentPath = currentNodePath.resolve(f.getGlobalId().toString());
		}
		return currentPath;
	}

	/**
	 * Delete a stored stageable for the given stored model.
	 * @param currentPath
	 * @param storedModel
	 * @param leaveDirectory if the object is a directory, should it remain on the filesystem?
	 * @param maybeDeferDeletion an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * 
	 * @param <S>
	 * @return
	 * @throws NodeException
	 */
	private <S extends StoredModel> Path removeStageable(Path currentPath, S storedModel, boolean hasChannels, boolean leaveDirectory, Optional<Set<Path>> maybeDeferDeletion) throws NodeException {
		String currentUuid = storedModel.getId();
		String suffix = storedModel.getSuffix();
		return removeStageable(currentPath, currentUuid, suffix, leaveDirectory, storedModel.maybeVersioned().isPresent(), storedModel.hasBinary(), hasChannels, maybeDeferDeletion);
	}

	/**
	 * Delete a stored stageable for the given node object.
	 * @param currentPath
	 * @param nodeObject
	 * @param leaveDirectory if the object is a directory, should it remain on the filesystem?
	 * @param maybeDeferDeletion an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * 
	 * @param <N>
	 * @return
	 * @throws NodeException
	 */
	private <N extends StageableNodeObject> Path removeStageable(Path currentPath, N nodeObject, boolean leaveDirectory, Optional<Set<Path>> maybeDeferDeletion) throws NodeException {
		String currentUuid = nodeObject.getGlobalId().toString();
		String suffix = nodeObject.getSuffix();

		// If we are given a Folder path, we need to make sure we are acting in the parent folder, not in the Folder's one
		if (nodeObject.isContainer() && currentPath.toString().endsWith(currentUuid)) {
			currentPath = currentPath.getParent();
		}
		return removeStageable(currentPath, currentUuid, suffix, leaveDirectory, nodeObject.maybeVersioned().isPresent(), nodeObject.maybeBinary().isPresent(), nodeObject.maybeHasChannels().isPresent(), maybeDeferDeletion);
	}

	/**
	 * Remove a stored stageable object.
	 * @param currentPath parent folder
	 * @param currentUuid uuid of an object to delete
	 * @param suffix object's filesystem suffix
	 * @param leaveDirectory if the object is Folder (or other container), should its filesystem directory be deleted?
	 * @param hasVersions is the object versioned?
	 * @param hasBinary does the object have a binary data?
	 * @param maybeDeferDeletion an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * 
	 * @return
	 * @throws NodeException
	 */
	private Path removeStageable(Path currentPath, String currentUuid, String suffix, 
			boolean leaveDirectory, boolean hasVersions, boolean hasBinary, boolean hasChannels, 
			Optional<Set<Path>> maybeDeferDeletion) throws NodeException {
		String currentFilename = currentUuid + suffix + METADATA_EXTENSION;
		Path currentDirPath = currentPath.resolve(currentUuid);
		if (Files.exists(currentDirPath) && Files.isDirectory(currentDirPath) && !leaveDirectory) {
			maybeDeferDeletion.map(deferDeletion -> deferDeletion.add(currentDirPath)).orElseGet(() -> {
				try {
					FileUtils.deleteDirectory(currentDirPath.toFile());
					return false;
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
		}
		if (hasVersions) {
			String pattern = currentUuid + "\\.\\d+\\.\\d+\\" + suffix + "\\" + METADATA_EXTENSION;
			maybeDeferDeletion.map(deferDeletion -> {
				try {
					Files.walk(currentPath, 1)
						.filter(page -> page.getFileName().toString().matches(pattern))
						.forEach(deferDeletion::add);
					return 0;
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}				
			}).orElseGet(() -> {
				try {
					java.io.File temporaryDir = Files.createTempDirectory(currentFilename + ".tmp").toFile();
					Files.walk(currentPath, 1)
						.filter(page -> page.getFileName().toString().matches(pattern))
						.forEach(page -> {
							try {
								FileUtils.moveFileToDirectory(page.toAbsolutePath().toFile(), temporaryDir, true);
							} catch (IOException e) {
								logger.error("Could not move page '" + currentUuid + "' into trash folder. Reverting deletion", e);
								try {
									java.io.File[] toRevert = temporaryDir.listFiles();
									for (int i = 0; i < toRevert.length; i++) {
										FileUtils.moveFileToDirectory(toRevert[i], currentPath.toFile(), false);
									}
								} catch (IOException e1) {
									throw new IllegalStateException(e1);
								}
								throw new IllegalStateException(e);
							}
						});
					FileUtils.deleteDirectory(temporaryDir);
					return 0;
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
		} else {
			Path currentFilepath = currentPath.resolve(currentFilename);
			maybeDeferDeletion.map(deferDeletion -> {
				return deferDeletion.add(currentFilepath);
			}).orElseGet(() -> {
				try {
					return Files.deleteIfExists(currentFilepath);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
		}
		if (hasBinary) {
			synchronizeBinary(currentPath, currentUuid, suffix, Optional.empty(), maybeDeferDeletion);
		}
		if (hasChannels) {
			Path channelFilepath = currentPath.resolve(currentUuid + CHANNEL_EXTENSION);
			maybeDeferDeletion.map(deferDeletion -> {
				return deferDeletion.add(channelFilepath);
			}).orElseGet(() -> {
				try {
					return Files.deleteIfExists(channelFilepath);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
		}
		return currentPath;
	}

	/**
	 * Add a versioned node object into the filesystem stored package.
	 * @param currentPath folder path to put into
	 * @param nodeObject the node object
	 * @param storedObject the stored object to inflate into
	 * @param modelInflater the stored object inflater method
	 * @param maybeDeferDeletion an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * 
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 * @return the path of a created stored stageable
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private <N extends StageableVersionedNodeObject, S extends StoredModel> Path addVersionedStageable(Path currentPath, N nodeObject, S storedObject, BiConsumer<N, S> modelInflater, Optional<Set<Path>> maybeDeferDeletion) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String currentUuid = nodeObject.getGlobalId().toString();
		storedObject.setId(currentUuid);

		Optional<StageableChanneledNodeObject> maybeChannels = nodeObject.maybeHasChannels();
		removeStageable(currentPath, storedObject, maybeChannels.isPresent(), true, maybeDeferDeletion);

		NodeObjectVersion current = nodeObject.getVersion(); 
		NodeObjectVersion scheduled = nodeObject.getTimePubVersion();
		NodeObjectVersion published = nodeObject.isOnline() ? nodeObject.getPublishedVersion() : null;
		Map<StagedObjectVersion, N> versions = new HashMap<>(3);

		if (published != null) {
			// export the published version
			StagedObjectVersion version = StagingModelBuilder.getPublishedVersion(published);
			version.setOnline(nodeObject.isOnline());
			if (scheduled == null || published.equals(scheduled)) {
				version.setTimeOff(Optional.ofNullable(nodeObject.getTimeOff()).map(ContentNodeDate::getTimestamp).orElse(null));
				version.setPublishAt(Optional.ofNullable(nodeObject.getTimePub()).map(ContentNodeDate::getTimestamp).orElse(null));
			}
			versions.put(version, (N) t.getObject(nodeObject.getObjectInfo().getObjectClass(), nodeObject.getId(), published.getDate().getIntTimestamp()));
		}
		if (!current.equals(published)) {
			// there is a current version, which is different from the published one
			if (current.equals(scheduled)) {
				// the current version is also scheduled (for republish or taking offline)
				StagedObjectVersion version = StagingModelBuilder.getScheduledVersion(scheduled, nodeObject.getTimePub().getIntTimestamp());
				version.setTimeOff(Optional.ofNullable(nodeObject.getTimeOff()).map(ContentNodeDate::getTimestamp).orElse(null));
				versions.put(version, (N) t.getObject(nodeObject.getObjectInfo().getObjectClass(), nodeObject.getId(), scheduled.getDate().getIntTimestamp()));
			} else {
				// the current version is not scheduled
				versions.put(StagingModelBuilder.getCurrentVersion(current), nodeObject);
			}
		}
		if (scheduled != null && !scheduled.equals(current) && !scheduled.equals(published)) {
			// there is a scheduled version, which is neither the current nor the published one
			StagedObjectVersion version = StagingModelBuilder.getScheduledVersion(scheduled, nodeObject.getTimePub().getIntTimestamp());
			version.setTimeOff(Optional.ofNullable(nodeObject.getTimeOff()).map(ContentNodeDate::getTimestamp).orElse(null));
			versions.put(version, (N) t.getObject(nodeObject.getObjectInfo().getObjectClass(), nodeObject.getId(), scheduled.getDate().getIntTimestamp()));
		}

		// Store versions
		for (Entry<StagedObjectVersion, N> version: versions.entrySet()) {
			nodeObject = version.getValue();
			storedObject.maybeVersioned().get().clone(version.getKey());
			String filename = nodeObject.getGlobalId() + "." + version.getKey().getVersionNumber() + nodeObject.getSuffix() + METADATA_EXTENSION;

			// If the object is in channel, but not inherited, we need no creation of its file
			if (shouldCreateStored(nodeObject)) {
				Path currentFilepath = currentPath.resolve(filename);
				maybeDeferDeletion.ifPresent(deferDeletion -> deferDeletion.remove(currentFilepath));
				try (OutputStream os = Channels.newOutputStream(FileChannel.open(currentFilepath, 
						StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).lock().channel())) {
					modelInflater.accept(nodeObject, storedObject);
					mapper.writeValue(os, storedObject);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return currentPath;
	}

	/**
	 * Determine, whether the object should be exported into the content package.
	 * An object is exported, if it does not take part in multichannelling or is not inherited
	 * @param <N> type of the stageable node object
	 * @param nodeObject object in question
	 * @return true, iff it should be exported
	 * @throws NodeException
	 */
	private <N extends StageableNodeObject> boolean shouldCreateStored(N nodeObject) throws NodeException {
		Optional<StageableChanneledNodeObject> inheritableObject = nodeObject.maybeHasChannels();

		if (!inheritableObject.isPresent()) {
			return true;
		} else {
			return !inheritableObject.get().isInherited();
		}
	}

	/**
	 * Add non-versioned node object into the staging stored package.
	 * @param currentPath folder path to put into
	 * @param nodeObject the node object
	 * @param storedObject the stored object to inflate into
	 * @param modelInflater the stored object inflater method
	 * @param maybeRecursiveProvider if present and the node object is folder, its contents will be processed with this provider.
	 * @param maybeDeferDeletion an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * 
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 * @return the path of a created stored stageable
	 * @throws NodeException
	 */
	private <N extends StageableNodeObject, S extends StoredModel> 
	Path addStageable(Path currentPath, N nodeObject, S storedObject, BiConsumer<N, S> modelInflater, 
			Optional<BiConsumer<Path, N>> maybeRecursiveProvider, Optional<Set<Path>> maybeDeferDeletion) throws NodeException {
		return addStageable(currentPath, nodeObject, storedObject, modelInflater, maybeRecursiveProvider, maybeDeferDeletion, false);
	}

	/**
	 * Add non-versioned node object into the staging stored package.
	 * 
	 * @param <N> the node object type
	 * @param <S> the corresponding stored model type
	 * @param currentPath folder path to put into
	 * @param nodeObject the node object
	 * @param storedObject the stored object to inflate into
	 * @param modelInflater the stored object inflater method
	 * @param maybeRecursiveProvider if present and the node object is folder, its contents will be processed with this provider.
	 * @param maybeDeferDeletion an optional container for package items to be deleted after the export process. If not provided, the item is deleted right away.
	 * @param isChannel if true, the object is treated as channel child
	 * @return the path of a created stored stageable
	 * @throws NodeException
	 */
	private <N extends StageableNodeObject, S extends StoredModel> 
			Path addStageable(Path currentPath, N nodeObject, S storedObject, BiConsumer<N, S> modelInflater, 
					Optional<BiConsumer<Path, N>> maybeRecursiveProvider, Optional<Set<Path>> maybeDeferDeletion, boolean isChannel) throws NodeException {
		String currentUuid = nodeObject.getGlobalId().toString();
		String suffix = nodeObject.getSuffix();
		String currentFilename = currentUuid + suffix + METADATA_EXTENSION;

		// If we are given a Folder path, we need to make sure we are acting in the parent folder, not in the Folder's one
		if (nodeObject.isContainer() && currentPath.toString().endsWith(currentUuid)) {
			currentPath = currentPath.getParent();
		}
		removeStageable(currentPath, currentUuid, suffix, true, nodeObject.maybeVersioned().isPresent(), nodeObject.maybeBinary().isPresent(), isChannel, maybeDeferDeletion);

		if (nodeObject.maybeVersioned().isPresent()) {
			// Sadly, we cannot call addVersionedStageable() here, since we cannot keep the actual class data with changing its generic base.
			throw new IllegalArgumentException("Node object '" + currentUuid + suffix + "' is versioned. Use addVersionedStageable() to process it.");
		}

		// If the object is in channel, but not inherited, we need no creation of its file
		if (shouldCreateStored(nodeObject)) {
			Path currentFilepath = currentPath.resolve(currentFilename);
			maybeDeferDeletion.ifPresent(deferDeletion -> deferDeletion.remove(currentFilepath));
			try (OutputStream os = Channels.newOutputStream(FileChannel.open(currentFilepath, 
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).lock().channel())) {
				modelInflater.accept(nodeObject, storedObject);
				mapper.writeValue(os, storedObject);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} 
		}

		// Create path if necessary
		if (nodeObject.isContainer() && !Files.exists(currentPath.resolve(currentUuid))) {
			try {
				Files.createDirectories(currentPath.resolve(currentUuid));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} 
		}
		Path returnPath;
		if (nodeObject.isContainer()) {
			Path currentNodePath = currentPath.resolve(currentUuid);
			if (maybeRecursiveProvider.isPresent()) {
				maybeRecursiveProvider.get().accept(currentNodePath, nodeObject);
			}
			returnPath = currentNodePath;
		} else {
			if (nodeObject.maybeBinary().isPresent()) {
				synchronizeBinary(currentPath, currentUuid, suffix, nodeObject.maybeBinary(), maybeDeferDeletion);
			}
			returnPath = currentPath.resolve(currentFilename);
		}
		return returnPath;
	}

	/**
	 * Process a content of a folder
	 * @param currentPath a package path of this folder
	 * @param f
	 * 
	 * @throws NodeException
	 */
	private final void consumeRecursiveFolder(Path currentPath, Folder f) throws NodeException {
		for (Page page : f.getPages()) {
			try {
				consumeViewable(page, child -> addVersionedStageable(currentPath, page, new StoredPage(), (n, s) -> StagingModelBuilder.getPage(n, s), Optional.empty()));
			} catch (InsufficientPrivilegesException e) {
				logger.error("Could not recursively process the page " + page.getGlobalId() + " due to the insufficient privileges");
			}
		}
		for (File file : f.getFiles()) {
			try {
				consumeViewable(file, child -> addStageable(currentPath, file, new StoredFile(), (n, s) -> StagingModelBuilder.getFile(n, s), Optional.empty(), Optional.empty()));
			} catch (InsufficientPrivilegesException e) {
				logger.error("Could not recursively process the file " + file.getGlobalId() + " due to the insufficient privileges");
			}
		}
		for (ImageFile image : f.getImages()) {
			try {
				consumeViewable(image, child -> addStageable(currentPath, image, new StoredImage(), (n, s) -> StagingModelBuilder.getImage(n, s), Optional.empty(), Optional.empty()));
			} catch (InsufficientPrivilegesException e) {
				logger.error("Could not recursively process the image " + image.getGlobalId() + " due to the insufficient privileges");
			}
		}
		for (Form form : f.getForms(null)) {
			try {
				consumeViewable(form, child -> addVersionedStageable(currentPath, form, new StoredForm(), (n, s) -> StagingModelBuilder.getForm(n, s), Optional.empty()));
			} catch (InsufficientPrivilegesException e) {
				logger.error("Could not recursively process the form " + form.getGlobalId() + " due to the insufficient privileges");
			}
		}
		for (Folder folder : f.getChildFolders()) {
			try {
				consumeViewable(folder, child -> addStageable(currentPath, child, new StoredFolder(), (n, s) -> StagingModelBuilder.getFolder(n, s), Optional.of(this::consumeRecursiveFolder), Optional.empty()));
			} catch (InsufficientPrivilegesException e) {
				logger.error("Could not recursively process the folder " + folder.getGlobalId() + " due to the insufficient privileges");
			}
		}
	};

	/**
	 * Execute a supplier with a lock
	 * @param <T> return type
	 * @param lockKey lock key
	 * @param method method to execute
	 * @return return value
	 * @throws NodeException
	 */
	private static <T> T executeLocked(String lockKey, Supplier<T> method) throws NodeException {
		return unwrap(() -> FILENAME_LOCK.executeLocked(lockKey, () -> wrap(method)));
	}

	/**
	 * Execute an operator with a lock
	 * @param lockKey lock key
	 * @param method operator to execute
	 * @throws NodeException
	 */
	private static void executeLocked(String lockKey, com.gentics.contentnode.etc.Operator method) throws NodeException {
		unwrap(() -> FILENAME_LOCK.executeLocked(lockKey, () -> wrap(method)));
	}

	/**
	 * Make a binary data filename for the given object.
	 *
	 * @param storedObject
	 * @return
	 */
	private static final String makeBinaryFilename(StoredModel storedObject) {
		return storedObject.getId() + storedObject.getSuffix() + BINARY_EXTENSION;
	}

	private final class StagingJobController implements Callable<GenericResponse> {
		private final String name;
		private final JobProgress progress = new JobProgress().setDone(0).setTotal(0);
		private final Path packagePath;
		private final boolean isTest;
		private boolean running = true;
		private StagedContentPackage initialState;

		public StagingJobController(String name, StagedContentPackage initialState, Path packagePath, boolean isTest) {
			this.initialState = initialState;
			this.name = name;
			this.isTest = isTest;
			this.packagePath = packagePath;
			progress.setTotal(initialState.getChannels() + initialState.getFiles() + initialState.getFolders() + initialState.getForms() + initialState.getImages() + initialState.getNodes() + initialState.getPages());
		}

		@Override
		public JobStatus call() throws NodeException {
			progress.setStarted((int) (System.currentTimeMillis() / 1000L));

			// if import errors file exists, we delete it now
			FileUtils.deleteQuietly(packagePath.resolve(CONTENT_PACKAGE_IMPORT_ERRORS_FILENAME).toFile());

			Set<StagingError> errors = new TreeSet<>();
			try (TrxAttribute trxAttr = TrxAttribute.with(NodeFactory.UNLOCK_AT_TRX_COMMIT, true)) {
				List<Path> haveOrphans = new ArrayList<>();
				try {
					Map<String, Path> encounteredIDs = new HashMap<>();
					List<Pair<String, String>> importedNodes = new ArrayList<>();

					java.util.function.Consumer<? super Path> importNodeFromPath = path -> {
						try {
							importedNodes.add(importNodeFromStageable(path, packagePath, progress, Optional.of(haveOrphans)));
						} catch (Throwable e) {
							errors.add(StagingError.fromThrowable(idFromFilesystem(path), path, e));
							if (isTest) {
								logger.error("Error at test import of path: " + path, e);
							} else {
								if (e instanceof RuntimeException) {
									throw (RuntimeException) e;
								} else {
									throw new IllegalStateException(e);
								}
							}
						}
					};
					java.util.function.BiConsumer<Node, ? super Path> importFromPath = (node, path) -> {
						if (!encounteredBefore(path, encounteredIDs)) {
							try {
								importFromStageable(path, packagePath, progress, Optional.of(haveOrphans), errors, node);
							} catch (Throwable e) {
								errors.add(StagingError.fromThrowable(idFromFilesystem(path), path, e));
								if (isTest) {
									logger.error("Error at test import of path: " + path, e);
								} else {
									if (e instanceof RuntimeException) {
										throw (RuntimeException) e;
									} else {
										throw new IllegalStateException(e);
									}
								}
							}
						}
					};

					// first import all nodes
					try (Stream<Path> fileStream = Files.list(packagePath)) {
						fileStream.filter(
								p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(NODE_EXTENSION))
								.forEach(importNodeFromPath);
					}

					// second import all channels

					// create map of channel IDs to ChannelPath instances
					Map<String, ChannelPath> allChannels = null;
					try (Stream<Path> fileStream = Files.list(packagePath)) {
						allChannels = fileStream.filter(
								p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(CHANNEL_EXTENSION))
								.map(ChannelPath::new).collect(Collectors.toMap(cp -> getObjectId(cp.path).get(),
										java.util.function.Function.identity()));
					}
					Set<ChannelPath> roots = new HashSet<>();

					// every ChannelPath is either a root, or is added as child to its immediate master ChannelPath
					for (ChannelPath channelPath : allChannels.values()) {
						StoredChannel storedChannel = loadFromFilesystem(channelPath.path, StoredChannel.class, s -> s);
						if (allChannels.containsKey(storedChannel.getMasterNodeId())) {
							allChannels.get(storedChannel.getMasterNodeId()).addChild(channelPath);
						} else {
							roots.add(channelPath);
						}
					}

					// now recursively import all channels
					roots.forEach(cp -> cp.importRecursively(importNodeFromPath));

					// third import the remaining objects
					for (Pair<String, String> pair : importedNodes) {
						String nodeId = pair.getLeft();
						Node node = TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId);
						Path folderFile = packagePath.resolve(pair.getRight() + FOLDER_EXTENSION);
						Path folderDir = packagePath.resolve(pair.getRight());

						importRecursively(folderFile, node, importFromPath);
						importRecursively(folderDir, node, importFromPath);
					}
				} catch (Throwable e) {
					errors.add(StagingError.fromThrowable(null, packagePath, e));
					if (isTest) {
						logger.error("Error at test import of package: " + name, e);
					} else {
						if (e.getCause() != null && e.getCause() instanceof NodeException) {
							throw (NodeException) e.getCause();
						} else if (e instanceof NodeException) {
							throw (NodeException) (e);
						} else if (e.getCause() != null && e.getCause() instanceof NodeException) {
							throw (NodeException) e.getCause();
						} else {
							throw new NodeException(e);
						}
					}
				}
				int tries = 2; 

				// we need to increase the total by the number of orphans, which will be handled now
				progress.setTotal(progress.getTotal() + haveOrphans.size());
				while (!haveOrphans.isEmpty() && tries >= 0) {

					for (int i = haveOrphans.size()-1; i >= 0; i--) {
						Path orphaned = haveOrphans.remove(i);
						try {
							importFromStageable(orphaned, packagePath, progress, tries > 0 ? Optional.of(haveOrphans) : Optional.empty(), errors, null);
						} catch (Throwable e) {
							errors.add(StagingError.fromThrowable(idFromFilesystem(orphaned), orphaned, e));
							if (isTest) {
								logger.error("Error at test import of an orphaned path: " + orphaned, e);
							} else if (e instanceof NodeException) {
								throw (NodeException) (e);
							} else if (e.getCause() != null && e.getCause() instanceof NodeException) {
								throw (NodeException) e.getCause();
							} else {
								throw new NodeException(e);
							}
						}
					}
					tries -= 1;
				}
				if (isTest) {
					TransactionManager.getCurrentTransaction().rollback(false);
				}

				JobStatus response = new JobStatus().setRunning(false).setProgress(progress);
					response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
				Type status;
				String i18nKey;
				String[] i18nParams;
				boolean throwException = false;

				if (errors.isEmpty()) {
					status = Type.SUCCESS;
					i18nKey = isTest ? "content.staging.import.test.success" : "content.staging.import.success";
					i18nParams = new String[] {Integer.toString(progress.getDone()), name};
				} else {
					status = Type.CRITICAL;
					i18nKey = "content.staging.import.errors";
					i18nParams = new String[] {name, Integer.toString(errors.size())};
					throwException = !isTest;
				}

				if (throwException) {
					throw new RestMappedException("Error while importing", i18nKey, Arrays.asList(i18nParams));
				} else {
					response.addMessage(new Message(status, I18NHelper
							.get(i18nKey, i18nParams)));
					return response;
				}
			} finally {
				// this is a bit lame, we just make sure that progress.done is not greater than progress.total
				if (progress.getDone() > progress.getTotal()) {
					progress.setTotal(progress.getDone());
				}
				progress.setFinished((int) (System.currentTimeMillis() / 1000));
				running = false;
				StagingErrors stagingErrors = new StagingErrors();
				stagingErrors.setErrors(new ArrayList<>(errors));
				try (OutputStream os = Channels.newOutputStream(FileChannel.open(packagePath.resolve(CONTENT_PACKAGE_IMPORT_ERRORS_FILENAME),
						StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).lock().channel())) {
					mapper.writeValue(os, stagingErrors);
				} catch (IOException e) {
					logger.error("Unable to write content staging error file", e);
					throw new IllegalStateException(e);
				}
			}
		}

		/**
		 * Get the job status
		 * @return job status
		 */
		public JobStatus getJobStatus() {
			return new JobStatus(I18NHelper.get(running ? "job_running" : "job_not_running",
					I18NHelper.get("content.staging.import", name)), running).setProgress(progress);
		}

		/**
		 * Get the content package, which is currently imported
		 * @return content package
		 */
		public StagedContentPackage getInitialState() {
			return this.initialState;
		}

		/**
		 * Recursively import the objects from the given path with the consumer.
		 * If the path denotes a directory, the files in that directory will be imported first, before recursing into subdirectories
		 * @param current current path
		 * @param node node into which the objects will be imported
		 * @param consumer consumer
		 */
		protected void importRecursively(Path current, Node node, java.util.function.BiConsumer<Node, ? super Path> consumer) {
			if (Files.isDirectory(current)) {
				try {
					try (Stream<Path> fileStream = Files.list(current)) {
						fileStream.filter(Files::isRegularFile).forEach(path -> consumer.accept(node, path));
					}
					try (Stream<Path> fileStream = Files.list(current)) {
						fileStream.filter(Files::isDirectory).forEach(p -> importRecursively(p, node, consumer));
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				consumer.accept(node, current);
			}
		}
	}

	/**
	 * Class for encapsulation of a Path for a .channel.json metadata file with all children
	 */
	private final static class ChannelPath {
		/**
		 * Path to the .channel.json file
		 */
		private Path path;

		/**
		 * All direct children of the channel, if they are also included in the package
		 */
		private Set<ChannelPath> children = new HashSet<>();

		/**
		 * Create instance
		 * @param path path
		 */
		public ChannelPath(Path path) {
			this.path = path;
		}

		/**
		 * Add another instance as child
		 * @param child child instance
		 */
		public void addChild(ChannelPath child) {
			this.children.add(child);
		}

		/**
		 * Recursively import this channel and all its children with the given consumer
		 * @param consumer path consumer
		 */
		public void importRecursively(java.util.function.Consumer<? super Path> consumer) {
			consumer.accept(path);
			children.forEach(child -> child.importRecursively(consumer));
		}
	}
}
