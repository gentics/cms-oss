/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: FileResource.java,v 1.3.6.3 2011-03-28 10:55:36 johannes2 Exp $
 */
package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.getItemList;
import static com.gentics.contentnode.rest.util.MiscUtils.getMatchingSystemUsers;
import static com.gentics.contentnode.rest.util.MiscUtils.getUrlDuplicationMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.MultiPart;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionLockManager;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.DisinheritUtils;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.FileCopyRequest;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.FileUsageListResponse;
import com.gentics.contentnode.rest.model.response.FolderUsageListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.MultiFileLoadResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.FileUploadManipulatorFileSave;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.staging.StagingUtil;
import com.gentics.contentnode.validation.map.inputchannels.FileDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.FileNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.InputChannel;
import com.gentics.contentnode.validation.map.inputchannels.MimeTypeInputChannel;
import com.gentics.contentnode.validation.util.ValidationUtils;
import com.gentics.contentnode.validation.validator.ValidationException;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

/**
 * Resource for loading and manipulating Files in GCN
 *
 * @author norbert
 */
@Path("/file")
public class FileResourceImpl extends AuthenticatedContentNodeResource implements FileResource {

	@Context
	HttpServletRequest servletRequest;

	/**
	 * Keyed lock to synchronize create/update calls for the same filename
	 */
	private static final TransactionLockManager<GenericResponse> fileNameLock = new TransactionLockManager<>();

	/**
	 * Default constructor
	 */
	public FileResourceImpl() {}

	@PostConstruct
	public void initialize() {
		// Attention: Multipart requests require seperate authentication handling within the resouce. Basically you just need to set the
		// session data and reinvoke this method.
		if ((getSessionId() == null || getSessionSecret() == null) && servletRequest != null && servletRequest.getContentType() != null
				&& servletRequest.getContentType().startsWith("multipart")) {
			logger.debug(
					"This request is a multipart request either sessionId or sessionSecret have not been set yet. The restcall may fail when no authentication will be invoked in the rest method.");
			return;
		}
		super.initialize();
	}

	@Override
	public FileListResponse list(
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam FileListParameterBean fileListParams,
			@BeanParam FilterParameterBean filterParams,
			@BeanParam SortParameterBean sortingParams,
			@BeanParam PagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		Transaction t = getTransaction();
		FolderResourceImpl folderResource = new FolderResourceImpl();

		folderResource.setTransaction(t);

		boolean channelIdSet = false;
		boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

		try {
			channelIdSet = setChannelToTransaction(fileListParams.nodeId);

			try (WastebinFilter filter = folderResource.getWastebinFilter(includeWastebin, inFolder.folderId)) {
				com.gentics.contentnode.object.Folder folder = folderResource.getFolder(inFolder.folderId, false);
				List<File> files = folderResource.getFilesOrImagesFromFolder(folder, ContentFile.TYPE_FILE,
					Folder.FileSearch.create().setSearchString(filterParams.query).setNiceUrlSearch(fileListParams.niceUrl)
						.setEditors(getMatchingSystemUsers(editableParams.editor, editableParams.editorIds))
						.setCreators(getMatchingSystemUsers(editableParams.creator, editableParams.creatorIds))
						.setEditedBefore(editableParams.editedBefore).setEditedSince(editableParams.editedSince)
						.setCreatedBefore(editableParams.createdBefore).setCreatedSince(editableParams.createdSince)
						.setRecursive(inFolder.recursive).setInherited(fileListParams.inherited).setOnline(fileListParams.online).setBroken(fileListParams.broken)
						.setUsed(fileListParams.used).setUsedIn(fileListParams.usedIn).setWastebin(wastebinParams.wastebinSearch == WastebinSearch.only));

				if (wastebinParams.wastebinSearch == WastebinSearch.only) {
					Wastebin.ONLY.filter(files);
				}

				Collection<Reference> refs = new ArrayList<>();

				if (fileListParams.folder) {
					refs.add(Reference.FOLDER);
				}

				Map<String, String> fieldMap = new HashMap<>();
				fieldMap.put("niceUrl", "nice_url");
				fieldMap.put("alternateUrls", "alternate_urls");
				fieldMap.put("fileSize", "size");
				fieldMap.put("fileType", "type");

				ResolvableComparator<File> comparator = ResolvableComparator.get(
					sortingParams,
					fieldMap,
					// From AbstractContentObject
					"id", "ttype", "ispage", "isfolder", "isfile", "isimage", "istag",
					// From ContentFile
					"datei", "file", "bild", "image", "name", "size", "sizeb", "readablesize", "sizekb",
					"sizeKB", "sizemb", "sizeMB", "folder_id", "folder", "ordner", "node",
					"extension", "creator", "ersteller", "editor", "bearbeiter", "createtimestamp",
					"createtimstamp", "createdate", "edittimestamp", "editdate", "type", "object",
					"url", "width", "sizex", "height", "sizey", "dpix", "dpiy", "dpi", "fpx", "fpy",
					"gis_resisable", "ismaster", "inherited", "nice_url", "alternate_urls", "niceUrl", "alternateUrls", "fileSize", "fileType");
				FileListResponse list = ListBuilder.from(files, file -> ModelBuilder.getFile(file, refs))
					.sort(comparator)
					.page(pagingParams)
					.to(new FileListResponse());

				list.setStagingStatus(StagingUtil.checkStagingStatus(files, inFolder.stagingPackageName, o -> o.getGlobalId().toString()));
				return list;
			}
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FileListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new FileListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting files or images for folder " + inFolder.folderId, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FileListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				// reset transaction channel
				t.resetChannel();
			}
		}
	}

	@Override
	@GET
	@Path("/content/load/{id}")
	public Response loadContent(@PathParam("id") final String id, @QueryParam("nodeId") Integer nodeId) {
		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			final File file = MiscUtils.load(File.class, id);

			MediaType mediaType = null;
			try {
				mediaType = MediaType.valueOf(file.getFiletype());
			} catch (IllegalArgumentException e) {
				mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
			}

			final InputStream inputStream = file.getFileStream();

			if (inputStream == null) {
				throw new NodeException("Could not open file with id {" + id + "} for reading.");
			}

			return Response.ok(new StreamingOutput() {
				public void write(OutputStream outputStream) throws IOException, WebApplicationException {

					// Warning. Keep in mind that the transaction has already been committed when reaching this point.
					// The CommittingResponseFilter has already committed the transaction.
					try {
						byte buf[] = new byte[1024];
						int len;

						while ((len = inputStream.read(buf)) > 0) {
							outputStream.write(buf, 0, len);
						}
					} catch (Exception e) {
						throw new WebApplicationException(e);
					}
				}
			}, mediaType).build();
		} catch (NodeException e) {
			throw new WebApplicationException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#load(java.lang.String)
	 */
	@GET
	@Path("/load/{id}")
	public FileLoadResponse load(
			@PathParam("id") String id,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@DefaultValue("false") @QueryParam("construct") boolean construct,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("package") String stagingPackageName
			) {
		boolean isChannelIdSet = false;
		Transaction transaction = getTransaction();

		try {
			// Set the channel context from which to retrieve the image,
			// if `nodeId' is provided.
			isChannelIdSet = setChannelToTransaction(nodeId);
			// Load the File from GCN
			File file = getFile(id, update, ObjectPermission.view);

			// if the object with this id is actually an image, throw an exception since only files should be returned
			if (file.isImage()) {
				throw new EntityNotFoundException();
			}

			Collection<Reference> refs = new ArrayList<>();
			refs.add(Reference.TAGS);
			refs.add(Reference.OBJECT_TAGS_VISIBLE);
			if (construct) {
				refs.add(Reference.TAG_EDIT_DATA);
			}
			com.gentics.contentnode.rest.model.File restFile = ModelBuilder.getFile(file, refs);

			FileLoadResponse response = new FileLoadResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded file " + id), restFile);
			response.setStagingStatus(StagingUtil.checkStagingStatus(file, stagingPackageName));
			return response;
		} catch (EntityNotFoundException e) {
			return new FileLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FileLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (NodeException e) {
			logger.error("Error while loading file " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FileLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while loading file " + id + ": " + e.getLocalizedMessage()), null);
		} finally {
			if (isChannelIdSet) {
				transaction.resetChannel();
			}
		}
	}

	@Override
	@POST
	@Path("/load")
	public MultiFileLoadResponse load(MultiObjectLoadRequest request, @QueryParam("fillWithNulls") @DefaultValue("false") boolean fillWithNulls) {
		Transaction t = getTransaction();
		Set<Reference> references = new HashSet<>();

		references.add(Reference.TAGS);
		references.add(Reference.OBJECT_TAGS_VISIBLE);

		try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
			boolean forUpdate = ObjectTransformer.getBoolean(request.isForUpdate(), false);
			List<File> allFiles = t.getObjects(File.class, request.getIds());

			List<com.gentics.contentnode.rest.model.File> returnedFiles = getItemList(request.getIds(), allFiles, file -> {
				Set<Integer> ids = new HashSet<>();
				ids.add(file.getId());
				ids.addAll(file.getChannelSet().values());
				return ids;
			}, file -> {
				if (forUpdate) {
					file = t.getObject(file, true);
				}
				return ModelBuilder.getFile(file, references);
			}, file -> {
				return ObjectPermission.view.checkObject(file)
						&& (!forUpdate || ObjectPermission.edit.checkObject(file));
			}, fillWithNulls);

			MultiFileLoadResponse response = new MultiFileLoadResponse(returnedFiles);
			response.setStagingStatus(StagingUtil.checkStagingStatus(allFiles, request.getPackage(), o -> o.getGlobalId().toString()));
			return response;
		} catch (NodeException e) {
			return new MultiFileLoadResponse(
				new Message(Type.CRITICAL, e.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.FAILURE, "Could not load files"));
		}
	}
	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#createSimpleMultiPartFallback(com.sun.jersey.multipart.MultiPart, jakarta.servlet.http.HttpServletRequest, java.lang.String, java.lang.String, java.lang.String)
	 */
	@POST
	@Path("/createSimple")
	@Consumes("multipart/form-data")
	@Produces(MediaType.APPLICATION_JSON)
	public FileUploadResponse createSimpleMultiPartFallback(MultiPart multiPart, @Context HttpServletRequest request,
			@QueryParam(FileUploadMetaData.META_DATA_FOLDERID_KEY) String folderId,
			@QueryParam(FileUploadMetaData.META_DATA_NODE_ID_KEY) String nodeId,
			@QueryParam(FileUploadMetaData.META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME) String customBodyPartName,
			@QueryParam(AuthenticatedContentNodeResource.QQFILE_FILENAME_PARAMETER_NAME) String qqFileUploaderFileName,
			@QueryParam(FileUploadMetaData.META_DATA_DESCRIPTION_KEY) String description,
			@QueryParam(FileUploadMetaData.META_DATA_OVERWRITE_KEY) @DefaultValue("false") boolean overwrite) {
		try {
			if (multiPart.getBodyParts().isEmpty()) {
				I18nString message = new CNI18nString("rest.file.upload.request_invalid");

				throw new NodeException(message.toString());
			}

			// Load the metadata by examining the multipart payload
			FileUploadMetaData metaData = getMetaData(multiPart, customBodyPartName);

			// Since we previously omitted the authentication for multipart requests we have to do it now
			// This also calls initialize()
			authenticate(metaData);

			Folder folder = null;
			Node owningNode = null;

			// Set the folderId from the query parameters
			if (!StringUtils.isEmpty(folderId)) {
				folder = MiscUtils.load(Folder.class, folderId);
				metaData.setFolderId(Integer.toString(folder.getId()));
			} else if (!metaData.hasProperty(FileUploadMetaData.META_DATA_FOLDERID_KEY)) {
				throw new NodeException(
						"Needed parameter is missing. Folderid parameter {" + FileUploadMetaData.META_DATA_FOLDERID_KEY + "} is {" + folderId + "}");
			} else {
				folder = MiscUtils.load(Folder.class, metaData.getProperty(FileUploadMetaData.META_DATA_FOLDERID_KEY));
			}

			owningNode = folder.getOwningNode();

			if (!StringUtils.isEmpty(nodeId)) {
				metaData.setNodeId(nodeId);
			}

			// Set the filename from the query parameters
			if (!StringUtils.isEmpty(qqFileUploaderFileName)) {
				metaData.setFilename(qqFileUploaderFileName);
			} else if (StringUtils.isEmpty(metaData.getFilename())) {
				throw new NodeException(
						"Needed parameter is missing. Filename parameter {" + FileUploadMetaData.META_DATA_FILE_NAME_KEY + "} is {" + qqFileUploaderFileName + "}");
			}

			String mimeType = FileUtil.getMimeTypeByExtension(metaData.getFilename());
			boolean isImage = mimeType != null && mimeType.startsWith("image/");

			metaData.setFilename(adjustFilename(isImage, metaData.getFilename(), owningNode));
			metaData.setDescription(ObjectTransformer.getString(description, ""));
			metaData.setOverwrite(overwrite ? "true" : "false");

			String filename = metaData.getFilename();

			// Create a transaction lock on the filename
			// This lock doesn't handle all cases of filename collisions because the FUM
			// can change the filename to anything else, but we ignore this case here.
			String lockKey = FileFactory.sanitizeName(filename);

			return (FileUploadResponse) executeLocked(fileNameLock, lockKey, () -> handleMultiPartRequest(multiPart, metaData, 0));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FileUploadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), false);
		} catch (EntityNotFoundException e) {
			return new FileUploadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), false);
		} catch (Exception e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while creating file.", e);

			Message message = new Message(Message.Type.CRITICAL, e.getMessage());
			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getMessage());

			if (e.getCause() != null) {
				responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getCause().getMessage());
			}

			return new FileUploadResponse(message, responseInfo, false);
		} finally {
			if (multiPart != null) {
				multiPart.cleanup();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#createSimple(jakarta.servlet.http.HttpServletRequest, int, int, java.lang.String, java.lang.String)
	 */
	@POST
	@Path("/createSimple")
	@Produces(MediaType.APPLICATION_JSON)
	public FileUploadResponse createSimple(@Context HttpServletRequest request, @QueryParam(FileUploadMetaData.META_DATA_FOLDERID_KEY) int folderId,
			@QueryParam(FileUploadMetaData.META_DATA_NODE_ID_KEY) @DefaultValue("0") int nodeId,
			@QueryParam(FileUploadMetaData.META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME) String customBodyPartKeyName,
			@QueryParam(AuthenticatedContentNodeResource.QQFILE_FILENAME_PARAMETER_NAME) String fileName,
			@QueryParam(FileUploadMetaData.META_DATA_DESCRIPTION_KEY) String description,
			@QueryParam(FileUploadMetaData.META_DATA_OVERWRITE_KEY) @DefaultValue("false") boolean overwrite) {
		Transaction t = getTransaction();

		try {
			if (fileName == null) {
				I18nString message = new CNI18nString("rest.file.upload.filename_not_specified");

				throw new NodeException(message.toString());
			}

			String mimeType = FileUtil.getMimeTypeByExtension(fileName);
			boolean isImage = mimeType != null && mimeType.startsWith("image/");

			try (ChannelTrx trx = new ChannelTrx(nodeId)) {
				Node node = t.getObject(Node.class, nodeId);

				fileName = adjustFilename(isImage, fileName, node);
				// The mime type is used later on, so it is updated here in case the filename was changed by the WEBP_CONVERSION feature.
				mimeType = FileUtil.getMimeTypeByExtension(fileName);

				String sanitizedFilename = FileFactory.sanitizeName(fileName);
				Integer fileId = null;
				if (overwrite) {
					// If overwrite is enabled and a file with the same filename exists in the given folder,
					// overwrite it
					File file = findFileByName(folderId, sanitizedFilename);
					if (file != null && !file.isInherited()) {
						fileId = file.getId();
					}
				}

				Integer finalFileId = fileId;
				AtomicReference<String> mediaType = new AtomicReference<>(mimeType);
				InputStream inputStream = getFileInputStream(isImage, request.getInputStream(), mediaType, node);

				return (FileUploadResponse) executeLocked(fileNameLock, sanitizedFilename, () -> {
					if (finalFileId == null) {
						// Create a new file
						return createFile(inputStream, folderId, nodeId, sanitizedFilename, mediaType.get(), description, null, Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap());
					} else {
						// Save data to an existing file
						return saveFile(inputStream, finalFileId, sanitizedFilename, mediaType.get(), description, null, Collections.emptySet(), Collections.emptyMap());
					}
				});
			}
		} catch (Exception e) {
			// If we encounter an error we just rollback
			try {
				t.rollback();
			} catch (TransactionException e1) {
				NodeLogger.getNodeLogger(getClass()).error("Error while rollback.", e1);
				throw new WebApplicationException(new Exception("Error while saving file - Error while rollback of transaction.", e1));
			}

			NodeLogger.getNodeLogger(getClass()).error("Error while creating file.", e);

			Message message = new Message(Message.Type.CRITICAL, e.getMessage());
			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getMessage());

			if (e.getCause() != null) {
				responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getCause().getMessage());
			}

			return new FileUploadResponse(message, responseInfo, false);
		}
	}

	/**
	 * Returns the filename of the file
	 *
	 * @param fileName
	 * @return filename
	 */
	private String stripFilepath(String fileName) throws NodeException {
		if (fileName != null && !StringUtils.isEmpty(fileName)) {
			int i = fileName.lastIndexOf("\\") + 1;

			return fileName.substring(i);
		} else {
			throw new NodeException("Could not strip fileName from path because fileName was not set");
		}
	}

	/**
	 * Handles the multipart request. The given metaData properties will be used if given.
	 *
	 * @param multiPart
	 * @param metaData
	 * @param fileId
	 * @return Returns the {@link FileUploadResponse}
	 */
	private FileUploadResponse handleMultiPartRequest(MultiPart multiPart, FileUploadMetaData metaData, int fileId) {
		Transaction t = null;
		// Will be closed in save
		InputStream fileDataInputStream = null;

		// If no initial metadata was given we create a empty properties object
		if (metaData == null) {
			metaData = new FileUploadMetaData();
		}

		try {
			BodyPart fileDataBodyPart = getFileDataBodyPart(multiPart, metaData.getProperty(FileUploadMetaData.META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME));

			if (fileDataBodyPart == null) {
				throw new WebApplicationException(new Exception("Could not find valid bodypart to work with"));
			}

			// Check for mandatory fields
			if (fileId == 0 && StringUtils.isBlank(metaData.getProperty(FileUploadMetaData.META_DATA_FOLDERID_KEY))) {
				throw new WebApplicationException(
						new Exception("Could not find value for folderId parameter {" + FileUploadMetaData.META_DATA_FOLDERID_KEY + "}"));
			}

			t = getTransaction();

			if (t == null) {
				throw new WebApplicationException(new Exception("Could not get valid transaction."));
			}

			NodeLogger.getNodeLogger(getClass()).debug("Post Data: " + metaData);
			// Extract values from the fileDataBodyPart
			String partMediaType = fileDataBodyPart.getMediaType().toString();
			String mainMediaType = fileDataBodyPart.getMediaType().getType().toString();
			boolean isImage =  mainMediaType.equals("image");

			if (ObjectTransformer.isEmpty(metaData.get(FileUploadMetaData.META_DATA_FILE_NAME_KEY))) {
				String partFilename = fileDataBodyPart.getContentDisposition().getFileName();

				// Fix for IE filename field
				partFilename = stripFilepath(partFilename);
				metaData.put(FileUploadMetaData.META_DATA_FILE_NAME_KEY, partFilename);
			}

			// Load needed information from metaData
			Integer nodeId = metaData.getNodeId();

			String description = metaData.getDescription();
			Object fileEntity = fileDataBodyPart.getEntity();

			if (fileEntity instanceof BodyPartEntity) {
				fileDataInputStream = ((BodyPartEntity)fileEntity).getInputStream();
			} else if (fileEntity instanceof String) {
				fileDataInputStream = new ByteArrayInputStream(((String)fileEntity).getBytes(StandardCharsets.UTF_8));
			} else {
				throw new WebApplicationException(new Exception("Unsupported file body part type"));
			}

			boolean overwriteExisting = metaData.getOverwrite();

			try (ChannelTrx trx = new ChannelTrx(nodeId)) {
				Folder folder = null;
				Node owningNode = null;

				if (fileId == 0) {
					folder = MiscUtils.load(Folder.class, metaData.getProperty(FileUploadMetaData.META_DATA_FOLDERID_KEY));
				} else {
					File file = MiscUtils.load(File.class, Integer.toString(fileId));
					folder = file.getFolder();
				}
				owningNode = folder.getOwningNode();

				String filename = adjustFilename(isImage, metaData.getFilename(), owningNode);
				metaData.put(FileUploadMetaData.META_DATA_FILE_NAME_KEY, filename);

				if (overwriteExisting && fileId == 0) {
					// If overwrite is enabled and a file with the same filename exists in the given folder,
					// overwrite it
					File file = findFileByName(folder.getId(), FileFactory.sanitizeName(filename));

					// Only overwrite local or localized files
					if (file != null && !file.isInherited()) {
						fileId = file.getId();
					}
				}

				// If webp conversion is enabled and this is an image, the input stream has to be converted to webp.
				AtomicReference<String> mediaType = new AtomicReference<>(partMediaType);

				try (InputStream in = getFileInputStream(isImage, fileDataInputStream, mediaType, folder.getNode())) {
					if (fileId == 0) {
						// Create a new file
						return createFile(in, folder.getId(), nodeId, filename, mediaType.get(), description, null, Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap());
					} else {
						// Save data to an existing file
						return saveFile(in, fileId, filename, mediaType.get(), description, null, Collections.emptySet(), Collections.emptyMap());
					}
				}
			}
		} catch (Exception e) {
			// If we encounter an error we just rollback
			if (t != null) {
				try {
					t.rollback(false);
				} catch (TransactionException e1) {
					NodeLogger.getNodeLogger(getClass()).error("Error while rollback.", e1);
					throw new WebApplicationException(new Exception("Error while saving file - Error while rollback of transaction.", e1));
				}
			} else {
				NodeLogger.getNodeLogger(getClass()).warn("Transaction for rollback not available.", e);
			}

			NodeLogger.getNodeLogger(getClass()).error("Error while creating file.", e);

			Message message = new Message(Message.Type.CRITICAL, e.getMessage());
			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getMessage());

			if (e.getCause() != null) {
				responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getCause().getMessage());
			}

			return new FileUploadResponse(message, responseInfo, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#create(com.sun.jersey.multipart.MultiPart)
	 */
	@POST
	@Path("/create")
	@Consumes("multipart/form-data")
	@Produces(MediaType.APPLICATION_JSON)
	public FileUploadResponse create(MultiPart multiPart) {
		String sentFilename = null;
		FileUploadResponse fileUploadResponse = null;

		try {
			// Load meta data without handling the custombodypartname
			FileUploadMetaData metaData = getMetaData(multiPart, null);
			sentFilename = metaData.getFilename();

			// Since we previously omitted the authentication for multipart requests we have to do it now
			// This also calls initialize()
			authenticate(metaData);

			String mimeType = FileUtil.getMimeTypeByExtension(sentFilename);
			boolean isImage = mimeType != null && mimeType.startsWith("image/");

			// Load needed information from metaData
			Integer nodeId = metaData.getNodeId();
			Folder folder = null;
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				folder = MiscUtils.load(Folder.class, metaData.getProperty(META_DATA_FOLDERID_KEY));
			}
			Node owningNode = folder.getOwningNode();

			sentFilename = adjustFilename(isImage, sentFilename, owningNode);
			metaData.setFilename(sentFilename);

			// Create a transaction lock on the filename
			// This lock doesn't handle all cases of filename collisions because the FUM
			// can change the filename to anything else, but we ignore this case here.
			String lockKey = FileFactory.sanitizeName(sentFilename);

			fileUploadResponse = (FileUploadResponse) executeLocked(fileNameLock, lockKey, () -> handleMultiPartRequest(multiPart, metaData, 0));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FileUploadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), false);
		} catch (EntityNotFoundException e) {
			return new FileUploadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), false);
		} catch (Exception e) {
			logger.error("Error while creating file " + sentFilename, e);
			I18nString message = new CNI18nString("rest.general.error");
			fileUploadResponse = new FileUploadResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while creating file " + sentFilename + ": " + e.getLocalizedMessage()), false);
		} finally {
			if (multiPart != null) {
				multiPart.cleanup();
			}
		}

		return fileUploadResponse;
	}

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public FileUploadResponse create(FileCreateRequest request) {
		int nodeId = request.getNodeId();
		try (ChannelTrx trx = new ChannelTrx(nodeId)) {
			int fileId = getOverwriteFileId(request);

			// get source image from URL
			HttpClient httpClient = new HttpClient();
			GetMethod getMethod = new GetMethod(request.getSourceURL());
			int responseStatus = httpClient.executeMethod(getMethod);

			if (responseStatus != 200) {
				throw new NodeException(String.format("Request to %s returned response code %d", request.getSourceURL(), responseStatus));
			}

			String detectedMimeType = FileUtil.getMimeTypeByContent(new ByteArrayInputStream(getMethod.getResponseBody()), request.getName());
			boolean isImage = detectedMimeType != null && detectedMimeType.startsWith("image/");

			Folder folder = TransactionManager.getCurrentTransaction().getObject(Folder.class, request.getFolderId());

			if (folder == null) {
				throw new EntityNotFoundException("No folder with ID `" + request.getFolderId() + "'");
			}

			if (updateRequestName(isImage, request, getMethod, folder.getNode())) {
				// The filename was changed from the one in the original request, so the file ID needs to be updated.
				fileId = getOverwriteFileId(request);
			}

			String lockKey = FileFactory.sanitizeName(request.getName());
			final int finalFileId = fileId;

			return (FileUploadResponse) executeLocked(fileNameLock, lockKey, () -> {
				try (ChannelTrx ctrx = new ChannelTrx(nodeId)) {
					AtomicReference<String> mediaType = new AtomicReference<>(null);

					try (InputStream fileDataInputStream = getFileInputStream(isImage, getMethod.getResponseBodyAsStream(), mediaType, folder.getNode())) {
						if (finalFileId == 0) {
							// Create a new file
							return createFile(fileDataInputStream, request.getFolderId(), request.getNodeId(), request.getName(),
								mediaType.get(), request.getDescription(), request.getNiceURL(), request.getAlternateURLs(), request.getProperties(), Collections.emptyMap());
						} else {
							// Save data to an existing file
							return saveFile(fileDataInputStream, finalFileId, request.getName(), mediaType.get(), request.getDescription(), request.getNiceURL(), request.getAlternateURLs(), request.getProperties());
						}
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}
			});
		} catch (NodeException | IOException e) {
			NodeLogger.getNodeLogger(getClass()).error(String.format("Error while creating file from URL %s", request.getSourceURL()), e);
			Message message = new Message(Message.Type.CRITICAL, e.getMessage());
			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, e.getMessage());
			return new FileUploadResponse(message, responseInfo, false);
		}
	}

	/**
	 * Get the file input stream and optionally convert to webp.
	 *
	 * <p>
	 *     When the input stream is an image and the feature
	 *     {@link Feature#WEBP_CONVERSION} is enabled for this node, the input
	 *     stream will be converted to webp.
	 * </p>
	 *
	 * @param isImage Whether the file is an image.
	 * @param inputStream The input stream to convert.
	 * @return The possibly converted input stream.
	 */
	private InputStream getFileInputStream(boolean isImage, InputStream inputStream, AtomicReference<String> mediaType, Node node) throws IOException {
		if (!isImage || !NodeConfigRuntimeConfiguration.isFeature(Feature.WEBP_CONVERSION, node)) {
			return inputStream;
		}

		ImmutableImage orig = ImmutableImage.loader().fromStream(inputStream);
		mediaType.set("image/webp");
		return orig.forWriter(WebpWriter.DEFAULT).stream();
	}

	/**
	 * Get the ID for the file to overwrite by this request.
	 * @param request The request to check.
	 * @return The file ID of the file to overwrite, or 0 if no such file is found or {@link FileCreateRequest#isOverwriteExisting()} is false.
	 */
	private int getOverwriteFileId(FileCreateRequest request) throws NodeException {
		int fileId = 0;

		if (request.isOverwriteExisting()) {
			// If overwrite is enabled and a file with the same filename exists in the given folder, overwrite it.
			File file = findFileByName(request.getFolderId(), FileFactory.sanitizeName(request.getName()));

			// Only overwrite local or localized files
			if (file != null && !file.isInherited()) {
				fileId = file.getId();
			}
		}

		return fileId;
	}

	/**
	 * Update the filename in the given request, if it is empty or the extension needs to be replaced with ".webp".
	 *
	 * <p>
	 * When the extension is changed, the extensions of nice and alternate URLs are also changed.
	 * </p>
	 *
	 * @param isImage Whether the file is an image.
	 * @param request The request to update.
	 * @param getMethod The response of loading the binary.
	 * @param node The node of the target file.
	 * @return Whether the {@link FileCreateRequest#getName() name} field in the request was changed.
	 */
	private boolean updateRequestName(boolean isImage, FileCreateRequest request, GetMethod getMethod, Node node) throws IOException {
		String origName = request.getName();

		if (ObjectTransformer.isEmpty(request.getName())) {
			request.setName("new_file");
		}

		if (isImage && NodeConfigRuntimeConfiguration.isFeature(Feature.WEBP_CONVERSION, node)) {
			request.setName(adjustImageFilename(request.getName()));

			request.setNiceURL(adjustImageFilename(request.getNiceURL()));

			Set<String> adaptedAltUrls = new HashSet<>();

			for (String altUrl : request.getAlternateURLs()) {
				adaptedAltUrls.add(adjustImageFilename(altUrl));
			}

			request.setAlternateURLs(adaptedAltUrls);
		} else if (request.getName().indexOf('.') < 0) {
			String detectedMimeType = FileUtil.getMimeTypeByContent(new ByteArrayInputStream(getMethod.getResponseBody()), request.getName());

			if (!"application/octet-stream".equals(detectedMimeType) && detectedMimeType.lastIndexOf('/') > 0) {
				String subtype = detectedMimeType.substring(detectedMimeType.lastIndexOf('/') + 1);

				request.setName(request.getName() + "." + subtype);
			}
		}

		return !Objects.equals(origName, request.getName());
	}

	/**
	 * Set the extension of the given filename to ".webp" if it is an image and
	 * {@link Feature#WEBP_CONVERSION} is enabled for the node.
	 *
	 * @param isImage Whether the file is an image.
	 * @param filename The filename to adjust.
	 * @param node The node of the target file.
	 * @return The filename with the extension replaced by ".webp" if it is an
	 * 		image and webp conversion is enabled, and the original
	 * 		filename otherwise.
	 */
	private String adjustFilename(boolean isImage, String filename, Node node) {
		return isImage && NodeConfigRuntimeConfiguration.isFeature(Feature.WEBP_CONVERSION, node)
			? adjustImageFilename(filename)
			: filename;
	}

	/**
	 * Replace the filenames extension with ".webp".
	 *
	 * <p>
	 *     NOTE that this method must only be called, when it was verified
	 *     beforehand, that the file is actually an image and
	 *     {@link Feature#WEBP_CONVERSION} is active for the files node.
	 * </p>
	 *
	 * @param filename The filename to adjust.
	 * @return The filename with the extension replaced with ".webp".
	 */
	private String adjustImageFilename(String filename) {
		return FilenameUtils.removeExtension(filename) + ".webp";
	}

	/**
	 * Stores the given file into the database
	 *
	 * @param input The files contents
	 * @param folderId The ID of the parent folder
	 * @param nodeId id of the node (channel) for which the file shall be created (for multichannelling)
	 * @param fileName The filename.
	 * @param mediaType The files media type.
	 * @param description The files description.
	 * @param niceUrl The files nice URL.
	 * @param alternateUrls The files alternate URLs.
	 * @param properties Additional properties to be saved to the files object properties.
	 * @param objectTags object tags to copy into the new file
	 * @return
	 * @throws NodeException
	 */
	private FileUploadResponse createFile(InputStream input, int folderId, int nodeId, String fileName,
			String mediaType, String description, String niceUrl, Set<String> alternateUrls,
			Map<String, String> properties, Map<String, ObjectTag> objectTags) throws NodeException {

		Transaction t = getTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// Check permissions for creating the file
		com.gentics.contentnode.object.Folder folder = (com.gentics.contentnode.object.Folder) t.getObject(com.gentics.contentnode.object.Folder.class, folderId);

		if (null == folder) {
			throw new EntityNotFoundException("No folder with ID `" + folderId + "'");
		}

		boolean hasPermission = t.getPermHandler().canCreate(folder, File.class, null);

		// Handle permissions
		if (!hasPermission) {
			String msg = "You don't have permission to create files in the folder with id " + folderId + ".";
			I18nString i18nMessage = new CNI18nString("rest.file.upload.missing_perm_folder");

			i18nMessage.setParameter("0", folderId);

			throw new NodeException(i18nMessage.toString(), new Exception(msg));
		}

		File file = (File) t.createObject(File.class);

		// if multichannelling is active and a nodeId was set, we will create the file for a channel
		if (prefs.isFeature(Feature.MULTICHANNELLING) && nodeId != 0) {
			// check whether the nodeId belongs to a channel
			Node channel = t.getObject(Node.class, nodeId);

			if (channel != null && channel.isChannel()) {
				file.setChannelInfo(nodeId, file.getChannelSetId());
			}
		}

		// Set attributes
		file.setFiletype(mediaType);
		file.setFolderId(folderId);

		return saveFileData(file, input, fileName, mediaType, description, niceUrl, alternateUrls, properties, objectTags);
	}

	/**
	 * Saves a file under a given fileId from the given InputStream and stores it into the database
	 *
	 * @param input The file contents.
	 * @param fileId The files ID.
	 * @param fileName The filename.
	 * @param mediaType The files media type.
	 * @param description The files description.
	 * @param niceUrl The files nice URL.
	 * @param alternateUrls The files alternate URLs.
	 * @param properties Additional values to save to the files object properties.
	 * @return A {@code FileUploadResponse} corresponding to the saved file.
	 * @throws NodeException
	 */
	private FileUploadResponse saveFile(InputStream input, int fileId, String fileName, String mediaType, String description, String niceUrl, Set<String> alternateUrls, Map<String, String> properties) throws NodeException {
		Transaction t = getTransaction();

		File file = (ContentFile) t.getObject(File.class, fileId, true);

		if (file == null) {
			I18nString message = new CNI18nString("file.notfound");

			message.setParameter("0", fileId);
			throw new EntityNotFoundException(message.toString());
		}

		// Check permissions for modifying the file
		boolean hasPermission = t.getPermHandler().canEdit(file);

		// Handle permissions
		if (!hasPermission) {
			Object folderId = file.getFolder().getId();
			String msg = "You don't have permission to edit the file with id " + fileId + " in the folder with id " + folderId + ".";
			I18nString i18nMessage = new CNI18nString("rest.file.upload.missing_perm_edit");

			i18nMessage.setParameter("0", fileId);
			i18nMessage.setParameter("1", folderId);
			throw new NodeException(msg, new Exception(i18nMessage.toString()));
		}

		return saveFileData(file, input, fileName, mediaType, description, niceUrl, alternateUrls, properties, Collections.emptyMap());
	}

	/**
	 * Stores the given file into the database
	 *
	 * @param file The file to save.
	 * @param input The file contents.
	 * @param fileName The filename.
	 * @param mediaType The files media type.
	 * @param description The files description.
	 * @param properties Additional values to save to the files object properties.
	 * @return A {@code FileUploadResponse} corresponding to the saved file.
	 * @throws NodeException
	 */
	private FileUploadResponse saveFileData(File file, InputStream input, String fileName, String mediaType, String description, 
			String niceUrl, Set<String> alternateUrls, 
			Map<String, String> properties, Map<String, ObjectTag> objectTags) throws NodeException {
		Transaction t = getTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// if no fileDescription property was set we use an empty one
		description = (description == null) ? "" : description;

		// if no contenttype was set use the default one
		mediaType = (mediaType == null) ? ContentFile.DEFAULT_FILETYPE : mediaType;

		if (file == null) {
			I18nString i18nMessage = new CNI18nString("rest.file.upload.generic_error");

			throw new WebApplicationException(new Exception(i18nMessage.toString()),
					Response.status(Status.INTERNAL_SERVER_ERROR).entity(i18nMessage.toString()).build());
		}

		Node node = file.getFolder().getNode();

		validateStrict(new MimeTypeInputChannel(node), mediaType, "validation.invalid.mimetype");
		validateStrict(new FileNameInputChannel(node), fileName, "validation.invalid.filename");
		validateStrict(new FileDescriptionInputChannel(node), description, "validation.invalid.filedescription");

		// Set attributes
		file.setName(fileName);
		file.setDescription(description);
		file.setAlternateUrls(alternateUrls);
		file.setNiceUrl(niceUrl);

		Message message = FileUploadManipulatorFileSave.handleFileUploadManipulator(t, prefs, file, input);

		t.commit(false);

		if (message == null) {
			// There has been no Message from the FileUploadManipulator so we have to create the default response message.
			I18nString i18nMessage = new CNI18nString("rest.file.upload.success");

			i18nMessage.setParameter("0", file.getId());
			message = new Message(Message.Type.SUCCESS, i18nMessage.toString());
		}

		ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK, "saved file with id: " + file.getId());

		// Load the file again
		File loadedFile = getFile(file.getId().toString(), true);

		copyObjectTags(loadedFile, objectTags);
		setProperties(loadedFile, properties);
		loadedFile.save();
		t.commit(false);
		loadedFile = getFile(file.getId().toString(), true);

		List<Reference> fillRefs = new ArrayList<>();

		fillRefs.add(Reference.TAGS);

		// When the file properties should be automatically opened after the
		// upload the response must contain the tag data.
		boolean addTagEditData = loadedFile.isImage()
			? prefs.isFeature(Feature.UPLOAD_IMAGE_PROPERTIES, node)
			: prefs.isFeature(Feature.UPLOAD_FILE_PROPERTIES, node);

		if (addTagEditData) {
			fillRefs.add(Reference.TAG_EDIT_DATA);
		}

		com.gentics.contentnode.rest.model.File responseFile = loadedFile.isImage() && loadedFile instanceof ImageFile
			? ModelBuilder.getImage((ImageFile) loadedFile, fillRefs)
			: ModelBuilder.getFile(loadedFile, fillRefs);

		return new FileUploadResponse(message, responseInfo, true, responseFile);

	}

	/**
	 * Set the specified properties in the object tags of the given file.
	 *
	 * <p>
	 *     When the key of a property matches the keyname of an object property,
	 *     its value will be written to the first editable text part of the
	 *     object property.
	 * </p>
	 *
	 * @param file The file to set additional properties for.
	 * @param properties The properties to set in the files object properties.
	 */
	private void setProperties(File file, Map<String, String> properties) throws NodeException {
		if (properties == null) {
			return;
		}

		for (Map.Entry<String, String> property : properties.entrySet()) {
			ObjectTag objTag = file.getObjectTag(property.getKey());

			if (objTag == null) {
				continue;
			}

			for (Value value : objTag.getValues()) {
				PartType partType = value.getPartType();

				if (value.getPart().isEditable() && partType instanceof TextPartType) {
					((TextPartType) partType).setText(property.getValue());
					objTag.setEnabled(true);

					break;
				}
			}
		}
	}
	/**
	 * Copy specified object tags into the object tags of the given file.
	 *
	 * @param file The file to copy the tags into.
	 * @param tags The tags to copy into the files object properties.
	 */
	private void copyObjectTags(File file, Map<String, ObjectTag> tags) throws NodeException {
		if (tags == null) {
			return;
		}
		for (Map.Entry<String, ObjectTag> property : tags.entrySet()) {
			file.getObjectTags().put(property.getKey(), (ObjectTag) property.getValue().copy());
		}
	}

	/**
	 * Performs strict validation on the given text.
	 *
	 * @param channel
	 * 		The channel specifying text context for the validation.
	 * @param text
	 * 		The text to validate.
	 * @param i18nError
	 * 		The i18n error message that will be output on error.
	 * @throws WebApplicationException
	 * 		If validation fails, returns to the client a HTTP BAD_REQUEST response.
	 */
	private void validateStrict(InputChannel channel, String text, String i18nError) throws TransactionException {
		try {
			ValidationResult result = ValidationUtils.validateStrict(channel, text);

			if (result.hasErrors()) {
				I18nString i18nMessage = new CNI18nString(i18nError);

				throw new WebApplicationException(new Exception(i18nMessage.toString()),
						Response.status(Status.BAD_REQUEST).entity(i18nMessage.toString()).build());
			}
		} catch (ValidationException e) {
			throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR).build());
		}
	}

	/**
	 * Handles the FileCopyRequest.
	 *
	 * @param request
	 * @throws NodeException
	 */
	private FileUploadResponse handleFileCopyRequest(FileCopyRequest request) throws NodeException {
		Transaction t = getTransaction();
		Integer srcFileId = request.getFile().getId();
		File file;

		if (request.getNodeId() != null) {
			try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
				file = MiscUtils.load(File.class, srcFileId.toString());
			}
		} else {
			file = MiscUtils.load(File.class, srcFileId.toString());
		}

		Folder targetFolder;
		Integer targetNodeId;
		boolean sameFolder;

		if (request.getTargetFolder() != null) {
			targetNodeId = request.getTargetFolder().getChannelId();

			try (ChannelTrx trx = new ChannelTrx(targetNodeId)) {
				targetFolder = MiscUtils.load(Folder.class, request.getTargetFolder().getId().toString());
				sameFolder = false;
			}
		} else {
			targetFolder = file.getFolder();
			targetNodeId = 0;
			sameFolder = true;
		}

		try (ChannelTrx trx = new ChannelTrx(targetNodeId)) {
			if (!t.canCreate(targetFolder, file.getClass(), null)) {
				I18nString message = new CNI18nString("rest.file.upload.missing_perm_folder");

				message.setParameter("0", targetFolder.getId());

				throw new InsufficientPrivilegesException(message.toString(), file, PermType.create);
			}
		}

		String newFilename = request.getNewFilename();
		FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);

		if (!sameFolder) {
			return createFile(
				file.getFileStream(),
				request.getTargetFolder().getId(),
				request.getTargetFolder().getChannelId(),
				StringUtils.isEmpty(newFilename) ? file.getFilename() : newFilename,
				file.getFiletype(),
				file.getDescription(),
				null,
				Collections.emptySet(),
				Collections.emptyMap(),
				file.getObjectTags());
		}

		File newFile;

		try (ChannelTrx trx = new ChannelTrx(file.getNode())) {
			if (!t.canCreate(file.getFolder(), file.getClass(), null)) {
				I18nString message = new CNI18nString("rest.file.upload.missing_perm_folder");

				message.setParameter("0", file.getFolder().getId().toString());

				throw new InsufficientPrivilegesException(message.toString(), file, PermType.create);
			}
		}

		// Copy the file with the new filename if a name was set
		if (newFilename == null) {
			newFile = fileFactory.copyFile(file);
		} else {
			newFile = fileFactory.copyFile(file, newFilename);
		}

		boolean multichannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);

		// when copying a localized file, the new file is also created in that channel
		if (multichannelling && file.getChannel() != null) {
			newFile.setChannelInfo(file.getChannel().getId(), newFile.getChannelSetId());
		}

		newFile.save();

		if (multichannelling) {
			File masterFile = file.getMaster();
			newFile.changeMultichannellingRestrictions(masterFile.isExcluded(), masterFile.getDisinheritedChannels(), false);
			newFile.setDisinheritDefault(masterFile.isDisinheritDefault(), false);
		}

		ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK, "copied file with id: " + file.getId());

		I18nString i18nMessage = new CNI18nString("rest.file.copy.success");

		i18nMessage.setParameter("0", ObjectTransformer.getString(file.getId(), null));
		i18nMessage.setParameter("1", ObjectTransformer.getString(newFile.getId(), null));
		Message message = new Message(Message.Type.SUCCESS, i18nMessage.toString());

		return new FileUploadResponse(message, responseInfo, true, ModelBuilder.getFile(newFile, Arrays.asList(Reference.TAGS)));

	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#copyFile(com.gentics.contentnode.rest.model.request.FileCopyRequest)
	 */
	@POST
	@Path("/copy")
	public FileUploadResponse copyFile(FileCopyRequest request) {
		try {
			String lockKey = request.getNewFilename() != null
				? FileFactory.sanitizeName(request.getNewFilename())
				: FileFactory.sanitizeName(request.getFile().getName());

			return (FileUploadResponse) executeLocked(fileNameLock, lockKey, () -> handleFileCopyRequest(request));
		} catch (NodeException e) {
			ResponseCode code = e instanceof InsufficientPrivilegesException
				? ResponseCode.PERMISSION
				: ResponseCode.NOTFOUND;

			return new FileUploadResponse(
				new Message(Type.CRITICAL, e.getLocalizedMessage()),
				new ResponseInfo(code, e.getMessage()),
				false);
		}

	}

	/**
	 * Mpve the given file to another folder
	 * @param id file id
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move/{id}")
	public GenericResponse move(@PathParam("id") String id, ObjectMoveRequest request) {
		MultiObjectMoveRequest multiRequest = new MultiObjectMoveRequest();
		multiRequest.setFolderId(request.getFolderId());
		multiRequest.setNodeId(request.getNodeId());
		multiRequest.setIds(Arrays.asList(id));
		return move(multiRequest);
	}

	/**
	 * Move multiple files to another folder
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move")
	public GenericResponse move(MultiObjectMoveRequest request) {
		try (AutoCommit trx = new AutoCommit()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder target = t.getObject(Folder.class, request.getFolderId());

			if (ObjectTransformer.isEmpty(request.getIds())) {
				throw new NodeException("No file ids provided");
			}
			for (String id : request.getIds()) {
				File toMove = getFile(id, false);
				String lockKey = FileFactory.sanitizeName(toMove.getName());
				GenericResponse response = executeLocked(fileNameLock, lockKey, () -> {
					OpResult result = toMove.move(target, ObjectTransformer.getInt(request.getNodeId(), 0));

					if (result.getStatus() == OpResult.Status.OK) {
						// Nothing to do, return a dummy response to signal success.

						return new GenericResponse(null, ResponseInfo.ok("success"));
					}

					GenericResponse failureResponse = new GenericResponse();

					for (NodeMessage msg : result.getMessages()) {
						failureResponse.addMessage(ModelBuilder.getMessage(msg));
					}

					failureResponse.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Error"));

					return failureResponse;
				});

				if (response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
					return response;
				}
			}
			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully moved files"));
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while moving files: " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#save(java.lang.Integer, com.gentics.contentnode.rest.model.request.FileSaveRequest)
	 */
	@POST
	@Path("/save/{id}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public GenericResponse save(@PathParam("id") Integer id, FileSaveRequest request) {
			// Get the file
			com.gentics.contentnode.rest.model.File restFile = request.getFile();

			if (restFile == null) {
				I18nString message = new CNI18nString("file.notfound");
				message.setParameter("0", id);
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.NOTFOUND, ""));
			}

		Supplier<GenericResponse> save = () -> {
			Transaction t = getTransaction();

			// The image ID is probably not in the request, so we set it here
			restFile.setId(id);

			// load file (checking for existence and permissions)
			getFile(Integer.toString(id), true, ObjectPermission.edit);

			File file = ModelBuilder.getFile(restFile);

			Map<String, ObjectTag> objectTags = file.getObjectTags();
			Map<String, Tag> restTags = restFile.getTags();
			if (restTags != null && objectTags != null) {
				// Throw an error if the user doesn't have permission
				// to update all the object properties
				MiscUtils.checkObjectTagEditPermissions(restTags, objectTags, true);
			}

			if (ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false) && !ObjectTransformer.isEmpty(request.getFile().getName())) {
				// check whether the name shall be changed
				File origFile = t.getObject(File.class, restFile.getId());

				if (!Objects.equals(origFile.getName(), file.getName())) {
					ChannelTreeSegment targetSegment = new ChannelTreeSegment(file, false);
					Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(file.getFolder(), targetSegment);
					if (!DisinheritUtils.isFilenameAvailable(file, pcf)) {
						I18nString message = new CNI18nString("a_file_with_this_name");
						return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.INVALIDDATA,
								"Error while saving file " + id + ": " + message.toString(), "name"));
					}
				}
			}

			if (StringUtils.isEmpty(file.getName())) {
				I18nString message = new CNI18nString("invalid_filename");
				return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while saving file " + id + ": " + message.toString()));
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!StringUtils.isEmpty(file.getNiceUrl())) {
					NodeObject conflictingObject = FileFactory.isNiceUrlAvailable(file, file.getNiceUrl());
					if (conflictingObject != null) {
						I18nString message = getUrlDuplicationMessage(file.getNiceUrl(), conflictingObject);
						return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.INVALIDDATA,
								"Error while saving file: " + message.toString(), "niceUrl"));
					}
				}
				if (!file.getAlternateUrls().isEmpty()) {
					List<Message> messages = new ArrayList<>();
					for (String url : file.getAlternateUrls()) {
						NodeObject conflictingObject = FileFactory.isNiceUrlAvailable(file, url);
						if (conflictingObject != null) {
							I18nString message = getUrlDuplicationMessage(url, conflictingObject);
							messages.add(new Message(Message.Type.CRITICAL, message.toString()));
						}
					}
					if (!messages.isEmpty()) {
						GenericResponse response = new GenericResponse();
						response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "Error while saving file.",
								"alternateUrls"));
						response.setMessages(messages);
						return response;
					}
				}
			}

			// save the file
			file.save();

			t.commit(false);

			I18nString message = new CNI18nString("file.save.success");

			return new GenericResponse(new Message(Message.Type.SUCCESS, message.toString()),
					new ResponseInfo(ResponseCode.OK, "saved file with id: " + file.getId()));
		};

		try {
			if (!ObjectTransformer.isEmpty(restFile.getName())) {
				String lockKey = FileFactory.sanitizeName(restFile.getName());
				return executeLocked(fileNameLock, lockKey, save);
			} else {
				return save.supply();
			}
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while saving file " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while saving file " + id + ": " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#saveContent(com.sun.jersey.multipart.MultiPart)
	 */
	@POST
	@Path("/save/{id}")
	@Consumes("multipart/form-data")
	public GenericResponse save(@PathParam("id") Integer id, MultiPart multiPart) {
		// Load meta data without handling the custombodypartname
		FileUploadMetaData metaData = getMetaData(multiPart, null);

		// Since we previously omitted the authentication for multipart requests we have to do it now
		// This also calls initialize()
		authenticate(metaData);

		String sentFilename = metaData.getFilename();

		if (!ObjectTransformer.isEmpty(sentFilename)) {
			try {
				String mimeType = FileUtil.getMimeTypeByExtension(sentFilename);
				boolean isImage = mimeType != null && mimeType.startsWith("image/");

				File file = MiscUtils.load(File.class, Integer.toString(id));
				Node owningNode = file.getOwningNode();
				sentFilename = adjustFilename(isImage, sentFilename, owningNode);
				metaData.setFilename(sentFilename);

				String lockKey = FileFactory.sanitizeName(sentFilename);
				return executeLocked(fileNameLock, lockKey, () -> handleMultiPartRequest(multiPart, metaData, id));
			} catch (InsufficientPrivilegesException e) {
				InsufficientPrivilegesMapper.log(e);
				return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
			} catch (EntityNotFoundException e) {
				return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
			} catch (NodeException e) {
				logger.error("Error while saving file " + id, e);
				I18nString message = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while saving file " + id + ": " + e.getLocalizedMessage()));
			}
		} else {
			return handleMultiPartRequest(multiPart, metaData, id);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FileResource#delete(java.lang.String, java.lang.Integer)
	 */
	@POST
	@Path("/delete/{id}")
	public GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("noSync") Boolean noCrSync) {
		boolean syncCr = Optional.ofNullable(noCrSync).map(BooleanUtils::negate).orElse(true);
		try (ChannelTrx trx = new ChannelTrx(nodeId); InstantPublishingTrx ip = new InstantPublishingTrx(syncCr)) {
			// get the file and check for permission to view and delete it
			File file = getFile(id, false, ObjectPermission.view, ObjectPermission.delete);

			Node node = file.getChannel();

			int nodeIdOfObject = -1;

			if (node != null) {
				nodeIdOfObject = ObjectTransformer.getInteger(node.getId(), -1);
			}

			if (nodeId == null) {
				nodeId = 0;
			}

			if (file.isInherited()) {
				throw new NodeException("Can't delete an inherated file, the file has to be deleted in the master node.");
			}

			if (nodeId > 0 && nodeIdOfObject > 0 && nodeIdOfObject != nodeId) {
				throw new EntityNotFoundException("The specified file exists, but is not part of the node you specified.");
			}

			int channelSetId = ObjectTransformer.getInteger(file.getChannelSetId(), 0);

			if (channelSetId > 0 && !file.isMaster()) {
				throw new NodeException("Deletion of localized files is currently not implemented, you maybe want to unlocalize it instead.");
			}

			final int fileId = file.getId();
			return Operator.executeLocked(new CNI18nString("file.delete.job").toString(), 0, Operator.lock(LockType.channelSet, channelSetId),
					new Callable<GenericResponse>() {
						@Override
						public GenericResponse call() throws Exception {
							File file = getFile(String.valueOf(fileId), false);

							// now delete the file
							file.delete();

							I18nString message = new CNI18nString("file.delete.success");
							return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
						}
					});
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while deleting file " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		}
	}

	@Override
	@POST
	@Path("/wastebin/delete/{id}")
	public GenericResponse deleteFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		return deleteFromWastebin(new IdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/delete")
	public GenericResponse deleteFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		List<String> ids = request.getIds();
		if (ObjectTransformer.isEmpty(ids)) {
			I18nString message = new CNI18nString("rest.general.insufficientdata");
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
		}

		I18nString description = null;
		if (ids.size() == 1) {
			description = new CNI18nString("file.delete.wastebin");
			description.setParameter("0", ids.iterator().next());
		} else {
			description = new CNI18nString("files.delete.wastebin");
			description.setParameter("0", ids.size());
		}

		return Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
			@Override
			public GenericResponse call() throws Exception {
				try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
					List<File> files = new ArrayList<File>();

					for (String id : ids) {
						File file = getFile(id, false);

						try (ChannelTrx cTrx = new ChannelTrx(file.getChannel())) {
							file = getFile(id, false, ObjectPermission.view, ObjectPermission.wastebin);
						}

						if (!file.isDeleted()) {
							I18nString message = new CNI18nString("file.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						}

						files.add(file);
					}

					String filePaths = I18NHelper.getPaths(files, 5);
					for (File file : files) {
						file.delete(true);
					}

					trx.success();
					// generate the response
					I18nString message = new CNI18nString(ids.size() == 1 ? "file.delete.wastebin.success" : "files.delete.wastebin.success");
					message.setParameter("0", filePaths);
					return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			}
		});
	}

	@Override
	@POST
	@Path("/wastebin/restore/{id}")
	public GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		return restoreFromWastebin(new IdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/restore")
	public GenericResponse restoreFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		List<String> ids = request.getIds();
		if (ObjectTransformer.isEmpty(ids)) {
			I18nString message = new CNI18nString("rest.general.insufficientdata");
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
		}

		I18nString description = null;
		if (ids.size() == 1) {
			description = new CNI18nString("file.restore.wastebin");
			description.setParameter("0", ids.iterator().next());
		} else {
			description = new CNI18nString("files.restore.wastebin");
			description.setParameter("0", ids.size());
		}

		return Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
			@Override
			public GenericResponse call() throws Exception {
				try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
					List<File> files = new ArrayList<File>();

					for (String id : ids) {
						File file = getFile(id, false);

						try (ChannelTrx cTrx = new ChannelTrx(file.getChannel())) {
							file = getFile(id, false, ObjectPermission.view, ObjectPermission.wastebin);
						}

						if (!file.isDeleted()) {
							I18nString message = new CNI18nString("file.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						}

						checkImplicitRestorePermissions(file);
						files.add(file);
					}

					String filePaths = I18NHelper.getPaths(files, 5);
					for (File file : files) {
						file.restore();
					}

					trx.success();
					// generate the response
					I18nString message = new CNI18nString(ids.size() == 1 ? "file.restore.wastebin.success" : "files.restore.wastebin.success");
					message.setParameter("0", filePaths);
					return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#getPrivileges(java.lang.Integer)
	 */
	@GET
	@Path("/privileges/{id}")
	public PrivilegesResponse getPrivileges(@PathParam("id") Integer id) {
		throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
	}

	@GET
	@Path("/usage/total")
	@Override
	public TotalUsageResponse getTotalUsageInfo(@QueryParam("id") List<Integer> fileId, @QueryParam("nodeId") Integer nodeId) {
		if (ObjectTransformer.isEmpty(fileId)) {
			return new TotalUsageResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched folders using 0 files"));
		}

		try {
			Map<Integer, Integer> masterMap = mapMasterFileIds(fileId);
			return getTotalUsageInfo(masterMap, File.TYPE_FILE, nodeId);
		} catch (Exception e) {
			logger.error("Error while getting total usage info for " + fileId.size() + " files", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new TotalUsageResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting total usage info for " + fileId.size() + " files" + e.getLocalizedMessage()));
		}
	}

	/**
	 * For every file in the list, get the id of the master file (or the file itself, if it is a master page or multichannelling is not
	 * activated)
	 *
	 * @param fileId
	 *            list of file ids
	 * @return map of master file ids to original ids
	 * @throws NodeException
	 */
	protected Map<Integer, Integer> mapMasterFileIds(List<Integer> fileId) throws NodeException {
		Transaction t = getTransaction();

		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			return fileId.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
		}
		List<File> files = t.getObjects(File.class, fileId);
		Map<Integer, Integer> masterMap = new HashMap<>(fileId.size());

		for (File file : files) {
			Integer id = ObjectTransformer.getInteger(file.getMaster().getId(), null);

			if (id != null && !masterMap.containsKey(id)) {
				masterMap.put(id, file.getId());
			}
		}
		return masterMap;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#getFolderUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean)
	 */
	@GET
	@Path("/usage/folder")
	public FolderUsageListResponse getFolderUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("folders") @DefaultValue("true") boolean returnFolders) {
		if (ObjectTransformer.isEmpty(fileId)) {
			return new FolderUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched folders using 0 files"), null, 0, 0);
		}

		try {
			fileId = getMasterFileIds(fileId);
			return getFolderUsage(skipCount, maxItems, sortBy, sortOrder, File.TYPE_FILE, fileId, nodeId, returnFolders);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + fileId.size() + " files", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FolderUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting usage info for " + fileId.size() + " files" + e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#getPageUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean, boolean, boolean, boolean)
	 */
	@GET
	@Path("/usage/page")
	public PageUsageListResponse getPageUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) {
		if (ObjectTransformer.isEmpty(fileId)) {
			return new PageUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched pages using 0 files"), null, 0, 0);
		}

		try {
			fileId = getMasterFileIds(fileId);
			return MiscUtils.getPageUsage(skipCount, maxItems, sortBy, sortOrder, File.TYPE_FILE, fileId, PageUsage.GENERAL, nodeId, returnPages, pageModel);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + fileId.size() + " files", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new PageUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting usage info for " + fileId.size() + " files" + e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#getTemplateUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean)
	 */
	@GET
	@Path("/usage/template")
	public TemplateUsageListResponse getTemplateUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("templates") @DefaultValue("true") boolean returnTemplates) {
		if (ObjectTransformer.isEmpty(fileId)) {
			return new TemplateUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 files"), null, 0, 0);
		}

		try {
			fileId = getMasterFileIds(fileId);
			return MiscUtils.getTemplateUsage(skipCount, maxItems, sortBy, sortOrder, File.TYPE_FILE, fileId, nodeId, returnTemplates);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + fileId.size() + " files", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new TemplateUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting usage info for " + fileId.size() + " files" + e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#getImageUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean)
	 */
	@GET
	@Path("/usage/image")
	public FileUsageListResponse getImageUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("files") @DefaultValue("true") boolean returnImages) {
		if (ObjectTransformer.isEmpty(fileId)) {
			return new FileUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 files"), null, 0, 0);
		}

		try {
			fileId = getMasterFileIds(fileId);
			return getFileUsage(skipCount, maxItems, sortBy, sortOrder, File.TYPE_FILE, fileId, nodeId, returnImages, true);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + fileId.size() + " files", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FileUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting usage info for " + fileId.size() + " files" + e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FileResource#getFileUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean)
	 */
	@GET
	@Path("/usage/file")
	public FileUsageListResponse getFileUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems, @QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> fileId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("files") @DefaultValue("true") boolean returnFiles) {
		if (ObjectTransformer.isEmpty(fileId)) {
			return new FileUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 files"), null, 0, 0);
		}

		try {
			fileId = getMasterFileIds(fileId);
			return getFileUsage(skipCount, maxItems, sortBy, sortOrder, File.TYPE_FILE, fileId, nodeId, returnFiles, false);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + fileId.size() + " files", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FileUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting usage info for " + fileId.size() + " files" + e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FileResource#getSyncableObjects(java.util.List, java.lang.Integer, java.lang.Integer)
	 */

	/**
	 * For every file in the list, get the id of the master file (or the file itself, if it is a master page or multichannelling is not
	 * activated)
	 *
	 * @param fileId
	 *            list of file ids
	 * @return list of master file ids
	 * @throws NodeException
	 */
	protected List<Integer> getMasterFileIds(List<Integer> fileId) throws NodeException {
		Transaction t = getTransaction();

		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			return fileId;
		}
		List<File> files = t.getObjects(File.class, fileId);
		List<Integer> newFileId = new Vector<Integer>(fileId.size());

		for (File file : files) {
			Integer id = ObjectTransformer.getInteger(file.getMaster().getId(), null);

			if (id != null && !newFileId.contains(id)) {
				newFileId.add(id);
			}
		}
		return newFileId;
	}

	/**
	 * Get the file with given id, check whether the file exists (if not, throw a EntityNotFoundException). Also check the permission to
	 * view the file and throw a InsufficientPrivilegesException, in case of insufficient permission.
	 *
	 * @param id
	 *            id of the file
	 * @param forUpdate
	 *            true if the file shall be fetched for update, false if not
	 * @param perms
	 *            additional permissions to check
	 * @return file
	 * @throws NodeException
	 *             when loading the file fails due to underlying error
	 * @throws EntityNotFoundException
	 *             when the file was not found
	 * @throws InsufficientPrivilegesException
	 *             when the user has no permission on the file
	 */
	@Deprecated
	protected File getFile(Integer id, boolean forUpdate, PermHandler.ObjectPermission... perms) throws EntityNotFoundException,
				InsufficientPrivilegesException, NodeException {
		return getFile(Integer.toString(id), forUpdate, perms);
	}

	/**
	 * Get the file with given id, check whether the file exists (if not, throw a EntityNotFoundException).
	 *
	 * Also checks for given permissions for the current user.
	 *
	 * @param id
	 *            id of the file
	 * @param forUpdate
	 *            true if the file shall be fetched for update, false if not
	 * @param perms
	 *            additional permissions to check
	 * @return file
	 * @throws NodeException
	 *             when loading the file fails due to underlying error
	 * @throws EntityNotFoundException
	 *             when the file was not found
	 * @throws InsufficientPrivilegesException
	 *             when the user has no permission on the file
	 */
	protected File getFile(String id, boolean forUpdate, PermHandler.ObjectPermission... perms) throws EntityNotFoundException,
				InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		File file = t.getObject(File.class, id, forUpdate);

		if (file == null) {
			I18nString message = new CNI18nString("file.notfound");

			message.setParameter("0", id.toString());
			throw new EntityNotFoundException(message.toString());
		}

		// if the file is an image the correct class has to be applied. this is important as
		// saving will decide the ttype depending on the class by using the .getTType() method
		// of the current transaction, which will result in 10008 for images without this fix.
		if (file.isImage()) {
			// This does nothing since ContentFile implements ImageFile anyways.
			file = t.getObject(ImageFile.class, id, forUpdate);
		}

		// check additional permissions
		for (PermHandler.ObjectPermission p : perms) {
			if (!p.checkObject(file)) {
				I18nString message = new CNI18nString("file.nopermission");

				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), file, p.getPermType());
			}

			// delete permissions for master files must be checked for all channels containing localized copies
			if (file.isMaster() && p == ObjectPermission.delete) {
				for (int channelSetNodeId : file.getChannelSet().keySet()) {
					if (channelSetNodeId == 0) {
						continue;
					}
					Node channel = t.getObject(Node.class, channelSetNodeId);
					if (!ObjectPermission.delete.checkObject(file, channel)) {
						I18nString message = new CNI18nString("file.nopermission");
						message.setParameter("0", id.toString());
						throw new InsufficientPrivilegesException(message.toString(), file, PermType.delete);
					}
				}
			}
		}

		return file;
	}

	/**
	 * Finds a file in the given folder by its name
	 * @param folderId The ID of the folder
	 * @param filename The file name to look for
	 * @return An instance of File or null
	 * @throws NodeException
	 */
	File findFileByName(Integer folderId, String filename) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, folderId);
		if (folder != null) {
			List<File> files = folder.getFilesAndImages();
			for (File file : files) {
				if (file.getName().equals(filename)) {
					return file;
				}
			}
		}

		return null;
	}
}
