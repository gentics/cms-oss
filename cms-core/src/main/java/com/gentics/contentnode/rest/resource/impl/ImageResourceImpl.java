package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.checkBody;
import static com.gentics.contentnode.rest.util.MiscUtils.getItemList;
import static com.gentics.contentnode.rest.util.MiscUtils.getMatchingSystemUsers;
import static com.gentics.contentnode.rest.util.MiscUtils.getUrlDuplicationMessage;

import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeDescriptor;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
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
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.ImageCreateRequest;
import com.gentics.contentnode.rest.model.request.ImageResizeRequest;
import com.gentics.contentnode.rest.model.request.ImageRotateRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.FileUsageListResponse;
import com.gentics.contentnode.rest.model.response.FolderUsageListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.MultiImageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.MiscUtils.PageUsage;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.staging.StagingUtil;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.image.CropFilter;
import com.gentics.lib.image.ImageUtils;
import com.gentics.lib.image.ResizeFilter;
import com.gentics.lib.image.SmarterResizeFilter;
import com.gentics.lib.log.NodeLogger;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Resource for loading and manipulating Images in GCN
 * @author norbert
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path("/image")
@Authenticated
public class ImageResourceImpl implements ImageResource {

	public static final String RESIZE_DEFAULT_TARGET_FORMAT = "png";

	public static NodeLogger log = NodeLogger.getNodeLogger(ImageResourceImpl.class);

	/**
	 * Default constructor
	 */
	public ImageResourceImpl() {}

	@Override
	@GET
	public ImageListResponse list(
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam FileListParameterBean fileListParams,
			@BeanParam FilterParameterBean filterParams,
			@BeanParam SortParameterBean sortingParams,
			@BeanParam PagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(fileListParams.nodeId)) {
			FolderResourceImpl folderResource = new FolderResourceImpl();

			boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

			try (WastebinFilter filter = folderResource.getWastebinFilter(includeWastebin, inFolder.folderId)) {
				com.gentics.contentnode.object.Folder folder = folderResource.getFolder(inFolder.folderId, false);
				List<File> images = folderResource.getFilesOrImagesFromFolder(folder, ContentFile.TYPE_IMAGE,
					Folder.FileSearch.create().setSearchString(filterParams.query).setNiceUrlSearch(fileListParams.niceUrl)
						.setEditors(getMatchingSystemUsers(editableParams.editor, editableParams.editorIds))
						.setCreators(getMatchingSystemUsers(editableParams.creator, editableParams.creatorIds))
						.setEditedBefore(editableParams.editedBefore).setEditedSince(editableParams.editedSince)
						.setCreatedBefore(editableParams.createdBefore).setCreatedSince(editableParams.createdSince)
						.setRecursive(inFolder.recursive).setInherited(fileListParams.inherited).setOnline(fileListParams.online).setBroken(fileListParams.broken)
						.setUsed(fileListParams.used).setUsedIn(fileListParams.usedIn).setWastebin(wastebinParams.wastebinSearch == WastebinSearch.only));

				if (wastebinParams.wastebinSearch == WastebinSearch.only) {
					Wastebin.ONLY.filter(images);
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

				ImageListResponse response = ListBuilder.from(images, image -> ModelBuilder.getImage((ImageFile) image, refs))
					.sort(comparator)
					.page(pagingParams)
					.to(new ImageListResponse());
				response.setStagingStatus(StagingUtil.checkStagingStatus(images, inFolder.stagingPackageName, o -> o.getGlobalId().toString()));
				trx.success();
				return response;
			}
		}
	}

	@Override
	@GET
	@Path("/load/{id}")
	public ImageLoadResponse load(
			@PathParam("id") String id,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@DefaultValue("false") @QueryParam("construct") boolean construct,
			@QueryParam("nodeId") Integer nodeId, 
			@QueryParam("package") String stagingPackageName
			) throws NodeException {

		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			// Load the image from GCN
			ImageFile image = getImage(id, update, ObjectPermission.view);

			// if the object with this id is actually a file, throw an exception since only images should be returned
			if (!image.isImage()) {
				throw new EntityNotFoundException();
			}

			Collection<Reference> refs = new ArrayList<>();
			refs.add(Reference.TAGS);
			refs.add(Reference.OBJECT_TAGS_VISIBLE);
			if (construct) {
				refs.add(Reference.TAG_EDIT_DATA);
			}
			Image restImage = ModelBuilder.getImage(image, refs);

			ImageLoadResponse response = new ImageLoadResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded image " + id), restImage);
			response.setStagingStatus(StagingUtil.checkStagingStatus(image, stagingPackageName));
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/load")
	public MultiImageLoadResponse load(MultiObjectLoadRequest request, @QueryParam("fillWithNulls") @DefaultValue("false") boolean fillWithNulls) throws NodeException {
		Set<Reference> references = new HashSet<>();

		references.add(Reference.TAGS);
		references.add(Reference.OBJECT_TAGS_VISIBLE);

		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx ctrx = new ChannelTrx(request.getNodeId())) {
			Transaction t = trx.getTransaction();
			boolean forUpdate = ObjectTransformer.getBoolean(request.isForUpdate(), false);
			List<ImageFile> allImages = t.getObjects(ImageFile.class, request.getIds());

			List<com.gentics.contentnode.rest.model.Image> returnedImages = getItemList(request.getIds(), allImages, image -> {
				Set<Integer> ids = new HashSet<>();
				ids.add(image.getId());
				ids.addAll(image.getChannelSet().values());
				return ids;
			}, image -> {
				if (forUpdate) {
					image = t.getObject(image, true);
				}
				return ModelBuilder.getImage(image, references);
			}, image -> {
				return ObjectPermission.view.checkObject(image)
						&& (!forUpdate || ObjectPermission.edit.checkObject(image));
			}, fillWithNulls);

			MultiImageLoadResponse response = new MultiImageLoadResponse(returnedImages);
			response.setStagingStatus(StagingUtil.checkStagingStatus(allImages, request.getPackage(), o -> o.getGlobalId().toString()));
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/resize/")
	public FileUploadResponse resize(ImageResizeRequest imageResizeRequest) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);

			Message message = new Message(Message.Type.CRITICAL, "Unknown error occured");
			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, "Unknown error occured");

			if (imageResizeRequest.getImage() == null) {
				throw new NodeException("Please specify a image in your request.");
			}

			File file = (ContentFile) t.getObject(File.class, imageResizeRequest.getImage().getId(), true);

			if (!file.isImage()) {
				throw new NodeException("The file with id {" + imageResizeRequest.getImage().getId() + "} is no image.");
			}

			if (imageResizeRequest.isCopyFile()) {
				file = fileFactory.copyFile(file);
				file.save();
			}

			// Update the focal point information if possible
			if (file instanceof ImageFile) {
				ImageFile image = (ImageFile) file;
				if (imageResizeRequest.getFpX() != null) {
					image.setFpX(imageResizeRequest.getFpX());
				}
				if (imageResizeRequest.getFpY() != null) {
					image.setFpY(imageResizeRequest.getFpY());
				}
			}

			// prepare image to be resized
			final File finalFile = file;
			PlanarImage image = ImageUtils.read(() -> {
				try {
					return finalFile.getFileStream();
				} catch (NodeException e) {
					throw new IOException(e);
				}
			});

			// rotate, if requested
			if (imageResizeRequest.getRotate() != null) {
				switch (imageResizeRequest.getRotate()) {
				case ccw:
					image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_270), null);
					break;
				case cw:
					image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_90), null);
					break;
				default:
					break;
				}
			}

			// crop, if requested
			if ("cropandresize".equalsIgnoreCase(imageResizeRequest.getMode())) {
				CropFilter cropFilter = new CropFilter();
				Properties cropProperties = new Properties();

				cropProperties.setProperty(CropFilter.HEIGHT, String.valueOf(imageResizeRequest.getCropHeight()));
				cropProperties.setProperty(CropFilter.WIDTH, String.valueOf(imageResizeRequest.getCropWidth()));
				cropProperties.setProperty(CropFilter.TOPLEFTX, String.valueOf(imageResizeRequest.getCropStartX()));
				cropProperties.setProperty(CropFilter.TOPLEFTY, String.valueOf(imageResizeRequest.getCropStartY()));
				cropFilter.initialize(cropProperties);
				image = cropFilter.filter(image);
			}

			// Resize
			Properties resizeProperties = new Properties();

			if ("force".equalsIgnoreCase(imageResizeRequest.getResizeMode())) {
				resizeProperties.setProperty("MODE", "unproportional");
			}
			resizeProperties.setProperty(SmarterResizeFilter.HEIGHT, String.valueOf(imageResizeRequest.getHeight()));
			resizeProperties.setProperty(SmarterResizeFilter.WIDTH, String.valueOf(imageResizeRequest.getWidth()));

			if ("smart".equalsIgnoreCase(imageResizeRequest.getResizeMode())) {
				SmarterResizeFilter smartResizeFilter = new SmarterResizeFilter();
				smartResizeFilter.initialize(resizeProperties);
				image = smartResizeFilter.filter(image);
			} else {
				ResizeFilter resizeFilter = new ResizeFilter();
				resizeFilter.initialize(resizeProperties);
				image = resizeFilter.filter(image);
			}

			// TODO Use a cycle stream wrapper to speed things up and save memory
			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			String targetFormat = imageResizeRequest.getTargetFormat();

			String validTargetFormat = NodeConfigRuntimeConfiguration.isFeature(Feature.WEBP_CONVERSION,
					file.getOwningNode()) ? "webp" : RESIZE_DEFAULT_TARGET_FORMAT;

			if (targetFormat != null && !"null".equalsIgnoreCase(targetFormat)) {
				validTargetFormat = imageResizeRequest.getTargetFormat();
			}

			boolean writerState = ImageUtils.write(image, validTargetFormat, boas);

			if (!writerState) {
				throw new NodeException("Image could not be encoded. No appropriate writer found.");
			}

			byte[] buffer = boas.toByteArray();

			if (buffer.length == 0) {
				throw new NodeException("Image could not be encoded. Aborting.");
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(buffer);

			file.setFileStream(bais);
			file.save();

			t.commit(false);
			message = new Message(Message.Type.SUCCESS, "Resizing was successful");
			responseInfo = new ResponseInfo(ResponseCode.OK, "Resizing was successful");

			trx.success();
			// Because the file maybe got copied, the requester want's to
			// know the new file ID. That's why we return a FileUploadResponse.
			return new FileUploadResponse(message, responseInfo, true, ModelBuilder.getFile(file, Arrays.asList(Reference.TAGS)));
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	@Override
	@POST
	@Path("/rotate")
	public ImageLoadResponse rotate(ImageRotateRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);

			Message message = new Message(Message.Type.CRITICAL, "Unknown error occured");
			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, "Unknown error occured");
			checkBody(request, r -> Pair.of("image", r.getImage()), r -> Pair.of("image.id", r.getImage().getId()), r -> Pair.of("rotate", r.getRotate()));

			File file = MiscUtils.load(File.class, Integer.toString(request.getImage().getId()), ObjectPermission.edit);

			if (!file.isImage()) {
				throw new NodeException("The file with id {" + request.getImage().getId() + "} is no image.");
			}

			if (request.isCopyFile()) {
				file = fileFactory.copyFile(file);
				file.save();
			} else {
				file = t.getObject(file, true);
			}

			// prepare image to be rotated
			final File finalFile = file;
			PlanarImage image = ImageUtils.read(() -> {
				try {
					return finalFile.getFileStream();
				} catch (NodeException e) {
					throw new IOException(e);
				}
			});

			// rotate
			if (request.getRotate() != null) {
				switch (request.getRotate()) {
				case ccw:
					image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_270), null);
					break;
				case cw:
					image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_90), null);
					break;
				default:
					break;
				}
			}

			// TODO Use a cycle stream wrapper to speed things up and save memory
			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			String targetFormat = request.getTargetFormat();

			String validTargetFormat = NodeConfigRuntimeConfiguration.isFeature(Feature.WEBP_CONVERSION,
					file.getOwningNode()) ? "webp" : RESIZE_DEFAULT_TARGET_FORMAT;

			if (targetFormat != null && !"null".equalsIgnoreCase(targetFormat)) {
				validTargetFormat = request.getTargetFormat();
			}

			boolean writerState = ImageUtils.write(image, validTargetFormat, boas);

			if (!writerState) {
				throw new NodeException("Image could not be encoded. No appropriate writer found.");
			}

			byte[] buffer = boas.toByteArray();

			if (buffer.length == 0) {
				throw new NodeException("Image could not be encoded. Aborting.");
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(buffer);

			file.setFileStream(bais);
			file.save();

			file = file.reload();

			message = new Message(Message.Type.SUCCESS, "Rotating was successful");
			responseInfo = new ResponseInfo(ResponseCode.OK, "Rotating was successful");

			trx.success();
			return new ImageLoadResponse(message, responseInfo,
					ModelBuilder.getImage((ImageFile) file, Arrays.asList(Reference.TAGS)));
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	@Override
	@GET
	@Path("/content/load/{id}")
	@Produces("image/*")
	public Response loadContent(@PathParam("id") Integer id) {
		throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
	}

	@Override
	@POST
	@Path("/create")
	public ImageLoadResponse create(ImageCreateRequest request) {
		throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
	}

	/**
	 * Mpve the given file to another folder
	 * @param id file id
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move/{id}")
	public GenericResponse move(@PathParam("id") String id, ObjectMoveRequest request) throws NodeException {
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
	public GenericResponse move(MultiObjectMoveRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder target = t.getObject(Folder.class, request.getFolderId());

			for (String id : request.getIds()) {
				File toMove = getImage(id, false);
				OpResult result = toMove.move(target, ObjectTransformer.getInt(request.getNodeId(), 0));
				switch (result.getStatus()) {
				case FAILURE:
					GenericResponse response = new GenericResponse();
					for (NodeMessage msg : result.getMessages()) {
						response.addMessage(ModelBuilder.getMessage(msg));
					}
					response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Error"));
					return response;
				case OK:
					// Nothing to do.
				}
			}
			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully moved files"));
		}
	}

	@Override
	@POST
	@Path("/save/{id}")
	public GenericResponse save(@PathParam("id") Integer id, ImageSaveRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			// Get the image
			com.gentics.contentnode.rest.model.Image restFile = request.getImage();

			if (restFile == null) {
				I18nString message = new CNI18nString("file.notfound");

				message.setParameter("0", id);
				throw new EntityNotFoundException(message.toString());
			}

			// The image ID is probably not in the request, so we set it here
			restFile.setId(id);

			// load image (checking for existence and permissions)
			getImage(Integer.toString(id), true, ObjectPermission.edit);

			ImageFile image = ModelBuilder.getImage(restFile);

			// Throw an error if the user doesn't have permission
			// to update all the object properties
			Map<String, Tag> restTags = restFile.getTags();
			Map<String, ObjectTag> objectTags = image.getObjectTags();
			if (restTags != null && objectTags != null) {
				MiscUtils.checkObjectTagEditPermissions(restTags, objectTags, true);
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!StringUtils.isEmpty(image.getNiceUrl())) {
					NodeObject conflictingObject = FileFactory.isNiceUrlAvailable(image, image.getNiceUrl());
					if (conflictingObject != null) {
						I18nString message = getUrlDuplicationMessage(image.getNiceUrl(), conflictingObject);
						return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.INVALIDDATA,
								"Error while saving image: " + message.toString(), "niceUrl"));
					}
				}
				if (!image.getAlternateUrls().isEmpty()) {
					List<Message> messages = new ArrayList<>();
					for (String url : image.getAlternateUrls()) {
						NodeObject conflictingObject = FileFactory.isNiceUrlAvailable(image, url);
						if (conflictingObject != null) {
							I18nString message = getUrlDuplicationMessage(url, conflictingObject);
							messages.add(new Message(Message.Type.CRITICAL, message.toString()));
						}
					}
					if (!messages.isEmpty()) {
						GenericResponse response = new GenericResponse();
						response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "Error while saving image.",
								"alternateUrls"));
						response.setMessages(messages);
						return response;
					}
				}
			}

			// save the file
			image.save();

			trx.success();

			I18nString message = new CNI18nString("image.save.success");

			return new GenericResponse(new Message(Message.Type.SUCCESS, message.toString()),
					new ResponseInfo(ResponseCode.OK, "saved image with id: " + image.getId()));
		}
	}

	@Override
	@POST
	@Path("/content/save/{id}")
	@Consumes("image/*")
	public GenericResponse saveContent(InputStream fileContent) {
		throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
	}

	@Override
	@POST
	@Path("/delete/{id}")
	public GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("noSync") Boolean noCrSync) throws NodeException {
		boolean syncCr = Optional.ofNullable(noCrSync).map(BooleanUtils::negate).orElse(true);
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId); InstantPublishingTrx ip = new InstantPublishingTrx(syncCr)) {
			// get the image and check for permission to view and delete it
			ImageFile image = getImage(id, false, ObjectPermission.view, ObjectPermission.delete);

			Node node = image.getChannel();

			int nodeIdOfObject = -1;

			if (node != null) {
				nodeIdOfObject = ObjectTransformer.getInteger(node.getId(), -1);
			}

			if (nodeId == null) {
				nodeId = 0;
			}

			if (image.isInherited()) {
				throw new NodeException("Can't delete an inherated image, the image has to be deleted in the master node.");
			}

			if (nodeId > 0 && nodeIdOfObject > 0 && nodeIdOfObject != nodeId) {
				throw new EntityNotFoundException("The specified image exists, but is not part of the node you specified.");
			}

			int channelSetId = ObjectTransformer.getInteger(image.getChannelSetId(), 0);

			if (channelSetId > 0 && !image.isMaster()) {
				throw new NodeException("Deletion of localized images is currently not implemented, you maybe want to unlocalize it instead.");
			}

			final int imageId = image.getId();
			GenericResponse response = Operator.executeLocked(new CNI18nString("image.delete.job").toString(), 0, Operator.lock(LockType.channelSet, channelSetId),
					new Callable<GenericResponse>() {
						@Override
						public GenericResponse call() throws Exception {
							ImageFile image = getImage(String.valueOf(imageId), false);

							// now delete the image
							image.delete();

							I18nString message = new CNI18nString("image.delete.success");
							return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
						}
					});
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/wastebin/delete/{id}")
	public GenericResponse deleteFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		return deleteFromWastebin(new IdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/delete")
	public GenericResponse deleteFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<String> ids = request.getIds();
			if (ObjectTransformer.isEmpty(ids)) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");
				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
			}

			I18nString description = null;
			if (ids.size() == 1) {
				description = new CNI18nString("image.delete.wastebin");
				description.setParameter("0", ids.iterator().next());
			} else {
				description = new CNI18nString("images.delete.wastebin");
				description.setParameter("0", ids.size());
			}

			GenericResponse response = Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
				@Override
				public GenericResponse call() throws Exception {
					try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
						List<ImageFile> images = new ArrayList<ImageFile>();
						for (String id : ids) {
							ImageFile image = getImage(id, false);
							
							try (ChannelTrx cTrx = new ChannelTrx(image.getChannel())) {
								image = getImage(id, false, ObjectPermission.view, ObjectPermission.wastebin);
							}
							
							if (!image.isDeleted()) {
								I18nString message = new CNI18nString("image.notfound");
								message.setParameter("0", id.toString());
								throw new EntityNotFoundException(message.toString());
							}
							images.add(image);
						}
						
						String imagePaths = I18NHelper.getPaths(images, 5);
						for (ImageFile image : images) {
							image.delete(true);
						}
						
						trx.success();
						// generate the response
						I18nString message = new CNI18nString(ids.size() == 1 ? "image.delete.wastebin.success" : "images.delete.wastebin.success");
						message.setParameter("0", imagePaths);
						return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
					}
				}
			});
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/wastebin/restore/{id}")
	public GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		return restoreFromWastebin(new IdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/restore")
	public GenericResponse restoreFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<String> ids = request.getIds();
			if (ObjectTransformer.isEmpty(ids)) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");
				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
			}

			I18nString description = null;
			if (ids.size() == 1) {
				description = new CNI18nString("image.restore.wastebin");
				description.setParameter("0", ids.iterator().next());
			} else {
				description = new CNI18nString("images.restore.wastebin");
				description.setParameter("0", ids.size());
			}

			GenericResponse response = Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
				@Override
				public GenericResponse call() throws Exception {
					try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
						List<ImageFile> images = new ArrayList<ImageFile>();
						for (String id : ids) {
							ImageFile image = getImage(id, false);

							try (ChannelTrx cTrx = new ChannelTrx(image.getChannel())) {
								image = getImage(id, false, ObjectPermission.view, ObjectPermission.wastebin);
							}

							if (!image.isDeleted()) {
								I18nString message = new CNI18nString("image.notfound");
								message.setParameter("0", id.toString());
								throw new EntityNotFoundException(message.toString());
							}

							MiscUtils.checkImplicitRestorePermissions(image);
							images.add(image);
						}

						String imagePaths = I18NHelper.getPaths(images, 5);
						for (ImageFile image : images) {
							image.restore();
						}

						trx.success();
						// generate the response
						I18nString message = new CNI18nString(ids.size() == 1 ? "image.restore.wastebin.success" : "images.restore.wastebin.success");
						message.setParameter("0", imagePaths);
						return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
					}
				}
			});
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/privileges/{id}")
	public PrivilegesResponse getPrivileges(@PathParam("id") Integer id) throws NodeException {
		throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
	}

	@GET
	@Path("/usage/total")
	@Override
	public TotalUsageResponse getTotalFileUsageInfo(@QueryParam("id") List<Integer> imageId, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		if (ObjectTransformer.isEmpty(imageId)) {
			return new TotalUsageResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully image usage for 0 images"));
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			Map<Integer, Integer> masterMap = mapMasterImageIds(imageId);
			TotalUsageResponse response = MiscUtils.getTotalUsageInfo(masterMap, ContentFile.TYPE_IMAGE, nodeId);
			trx.success();
			return response;
		}
	}

	/**
	 * For every image in the list, get the id of the master image (or the image
	 * itself, if it is a master page or multichannelling is not activated)
	 *
	 * @param imageId
	 *            list of image ids
	 * @return list of master image ids
	 * @throws NodeException
	 */
	protected Map<Integer, Integer> mapMasterImageIds(List<Integer> imageId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			return imageId.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
		}
		List<ImageFile> images = t.getObjects(ImageFile.class, imageId);
		Map<Integer, Integer> masterMap = new HashMap<>(imageId.size());

		for (ImageFile image : images) {
			Integer id = ObjectTransformer.getInteger(image.getMaster().getId(), null);

			if (id != null && !masterMap.containsKey(id)) {
				masterMap.put(id, image.getId());
			}
		}
		return masterMap;
	}

	@Override
	@GET
	@Path("/usage/folder")
	public FolderUsageListResponse getFolderUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("folders") @DefaultValue("true") boolean returnFolders) throws NodeException {
		if (ObjectTransformer.isEmpty(imageId)) {
			return new FolderUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched folders using 0 images"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			imageId = getMasterImageIds(imageId);
			FolderUsageListResponse response = MiscUtils.getFolderUsage(skipCount, maxItems, sortBy, sortOrder, ContentFile.TYPE_IMAGE, imageId, nodeId, returnFolders);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/usage/page")
	public PageUsageListResponse getPageUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) throws NodeException {
		if (ObjectTransformer.isEmpty(imageId)) {
			return new PageUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched pages using 0 images"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			imageId = getMasterImageIds(imageId);
			PageUsageListResponse response = MiscUtils.getPageUsage(skipCount, maxItems, sortBy, sortOrder, ContentFile.TYPE_IMAGE, imageId, PageUsage.GENERAL, nodeId, returnPages, pageModel);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/usage/template")
	public TemplateUsageListResponse getTemplateUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("templates") @DefaultValue("true") boolean returnTemplates) throws NodeException {
		if (ObjectTransformer.isEmpty(imageId)) {
			return new TemplateUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 images"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			imageId = getMasterImageIds(imageId);
			TemplateUsageListResponse response = MiscUtils.getTemplateUsage(skipCount, maxItems, sortBy, sortOrder, ContentFile.TYPE_IMAGE, imageId, nodeId, returnTemplates);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/usage/image")
	public FileUsageListResponse getImageUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("images") @DefaultValue("true") boolean returnImages) throws NodeException {
		if (ObjectTransformer.isEmpty(imageId)) {
			return new FileUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 images"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			imageId = getMasterImageIds(imageId);
			FileUsageListResponse response = MiscUtils.getFileUsage(skipCount, maxItems, sortBy, sortOrder, ContentFile.TYPE_IMAGE, imageId, nodeId, returnImages, true);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/usage/file")
	public FileUsageListResponse getFileUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> imageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("files") @DefaultValue("true") boolean returnFiles) throws NodeException {
		if (ObjectTransformer.isEmpty(imageId)) {
			return new FileUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 images"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			imageId = getMasterImageIds(imageId);
			FileUsageListResponse response = MiscUtils.getFileUsage(skipCount, maxItems, sortBy, sortOrder, ContentFile.TYPE_IMAGE, imageId, nodeId, returnFiles, false);
			trx.success();
			return response;
		}
	}

	/**
	 * For every image in the list, get the id of the master image (or the image
	 * itself, if it is a master page or multichannelling is not activated)
	 *
	 * @param imageId
	 *            list of image ids
	 * @return list of master image ids
	 * @throws NodeException
	 */
	protected List<Integer> getMasterImageIds(List<Integer> imageId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			return imageId;
		}
		List<ImageFile> images = t.getObjects(ImageFile.class, imageId);
		List<Integer> newImageId = new Vector<Integer>(imageId.size());

		for (ImageFile image : images) {
			Integer id = ObjectTransformer.getInteger(image.getMaster().getId(), null);

			if (id != null && !newImageId.contains(id)) {
				newImageId.add(id);
			}
		}
		return newImageId;
	}

	/**
	 * Get the image with given id, check whether the image exists (if not,
	 * throw a EntityNotFoundException). Also check the permission to view the image and
	 * throw a InsufficientPrivilegesException, in case of insufficient permission.
	 * @param id id of the image
	 * @param forUpdate true if the image shall be fetched for update, false if not
	 * @param perms additional permissions to check
	 * @return image
	 * @throws NodeException when loading the image fails due to underlying
	 *         error
	 * @throws EntityNotFoundException when the image was not found
	 * @throws InsufficientPrivilegesException when the user has no permission
	 *         on the image
	 */
	@Deprecated
	protected ImageFile getImage(Integer id, boolean forUpdate, PermHandler.ObjectPermission... perms) throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		return getImage(Integer.toString(id), forUpdate, perms);
	}

	/**
	 * Get the image with given id, check whether the image exists (if not,
	 * throw a EntityNotFoundException).
	 *
	 * Also checks for given permissions for the current user.
	 *
	 * @param id id of the image
	 * @param forUpdate true if the image shall be fetched for update, false if not
	 * @param perms additional permissions to check
	 * @return image
	 * @throws NodeException when loading the image fails due to underlying
	 *         error
	 * @throws EntityNotFoundException when the image was not found
	 * @throws InsufficientPrivilegesException when the user has no permission
	 *         on the image
	 */
	protected ImageFile getImage(String id, boolean forUpdate, PermHandler.ObjectPermission... perms) throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ImageFile image = t.getObject(ImageFile.class, id, forUpdate);
		if (image == null) {
			I18nString message = new CNI18nString("image.notfound");

			message.setParameter("0", id.toString());
			throw new EntityNotFoundException(message.toString());
		}

		// check additional permission bits
		for (PermHandler.ObjectPermission p : perms) {
			if (!p.checkObject(image)) {
				I18nString message = new CNI18nString("image.nopermission");

				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), image, p.getPermType());
			}

			// delete permissions for master files must be checked for all channels containing localized copies
			if (image.isMaster() && p == ObjectPermission.delete) {
				for (int channelSetNodeId : image.getChannelSet().keySet()) {
					if (channelSetNodeId == 0) {
						continue;
					}
					Node channel = t.getObject(Node.class, channelSetNodeId);
					if (!ObjectPermission.delete.checkObject(image, channel)) {
						I18nString message = new CNI18nString("image.nopermission");
						message.setParameter("0", id.toString());
						throw new InsufficientPrivilegesException(message.toString(), image, PermType.delete);
					}
				}
			}
		}

		return image;
	}
}
