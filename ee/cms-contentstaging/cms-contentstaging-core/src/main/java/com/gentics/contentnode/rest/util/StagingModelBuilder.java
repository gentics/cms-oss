package com.gentics.contentnode.rest.util;

import com.gentics.contentnode.staging.StagingErrorRecommendation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.StagedReferencedConstruct;
import com.gentics.contentnode.object.StagedReferencedDatasource;
import com.gentics.contentnode.object.StagedReferencedObject;
import com.gentics.contentnode.object.StagedReferencedObjectTagDefinition;
import com.gentics.contentnode.object.StagedReferencedTemplate;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.NodeIdObjectId;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.staging.StagedChannel;
import com.gentics.contentnode.staging.StagedFile;
import com.gentics.contentnode.staging.StagedFolder;
import com.gentics.contentnode.staging.StagedForm;
import com.gentics.contentnode.staging.StagedImage;
import com.gentics.contentnode.staging.StagedLocalizableObject;
import com.gentics.contentnode.staging.StagedNode;
import com.gentics.contentnode.staging.StagedNodeIdObjectId;
import com.gentics.contentnode.staging.StagedObjectTag;
import com.gentics.contentnode.staging.StagedObjectVersion;
import com.gentics.contentnode.staging.StagedObjectVersionType;
import com.gentics.contentnode.staging.StagedOverview;
import com.gentics.contentnode.staging.StagedPage;
import com.gentics.contentnode.staging.StagedProperty;
import com.gentics.contentnode.staging.StagedReference;
import com.gentics.contentnode.staging.StagedTag;
import com.gentics.contentnode.staging.StagingError;
import com.gentics.contentnode.staging.StagingErrorKind;
import com.gentics.contentnode.staging.StoredImage;
import com.gentics.lib.etc.StringUtils;

/**
 * Staging specific model builder
 */
public class StagingModelBuilder {

	/**
	 * Inflate the staging image Model with its node object.
	 *
	 * @param nodeImage
	 *            Node Object
	 * @param restImage staging model to inflate
	 * @param fillRefs references to be filled
	 * @return REST Model of the Image
	 * @throws NodeException
	 */
	public static StagedImage getImage(ImageFile nodeImage, StagedImage restImage) throws NodeException {
		getFile(nodeImage, restImage);

		restImage.setDpiX(nodeImage.getDpiX());
		restImage.setDpiY(nodeImage.getDpiY());
		restImage.setSizeX(nodeImage.getSizeX());
		restImage.setSizeY(nodeImage.getSizeY());
		restImage.setFpX(nodeImage.getFpX());
		restImage.setFpY(nodeImage.getFpY());
		restImage.setGisResizable(nodeImage.isGisResizable());

		return restImage;
	}


	/**
	 * Fill the file data into the staging Model
	 *
	 * @param nodeFile
	 *            Node Object
	 * @param restFile
	 *            staging Model
	 * @param fillRefs references to be filled
	 * @throws NodeException
	 */
	public static StagedFile getFile(File nodeFile,
			StagedFile restFile) throws NodeException {
		restFile.setId(nodeFile.getGlobalId().toString());
		restFile.setName(nodeFile.getName());
		restFile.setFileSize(nodeFile.getFilesize());
		restFile.setFileType(nodeFile.getFiletype());
		restFile.setDescription(nodeFile.getDescription());
		restFile.setCustomCdate(nodeFile.getCustomCDate().getIntTimestamp());
		restFile.setCustomEdate(nodeFile.getCustomEDate().getIntTimestamp());

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			restFile.setNiceUrl(nodeFile.getNiceUrl());
			restFile.setAlternateUrls(new TreeSet<>(nodeFile.getAlternateUrls()));
		}
		fillMultichannellingData(nodeFile, restFile);
		restFile.setForceOnline(nodeFile.isForceOnline());

		// add the objecttags
		Map<String, ObjectTag> objectTagMap = new HashMap<>(nodeFile.getObjectTags());

		// filter out restricted object tags
		new ObjectTagRestrictionFilter(nodeFile.getOwningNode()).filter(objectTagMap.values());

		Map<String, StagedObjectTag> restTags = new HashMap<>(objectTagMap.size());

		for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
			String tagName = i.next();
			ObjectTag objectTag = objectTagMap.get(tagName);

			StagedObjectTag restTag = getObjectTag(objectTag, new StagedObjectTag());
			restTags.put(restTag.getName(), restTag);
		}
		restFile.setObjectTags(restTags);

		return restFile;
	}


	/**
	 * Inflate the given staging folder model from its node oblect.
	 *
	 * @param nodeFolder
	 *            Node Object
	 * @param stagingFolder
	 * 			  staging API object
	 * @param comparator
	 *            Comparator for sorting subfolders
	 * @param fillRefs
	 *            References to be filled in the REST Model
	 * @return REST Model
	 * @throws NodeException
	 */
	public static StagedFolder getFolder(Folder nodeFolder, StagedFolder stagingFolder) throws NodeException {
		stagingFolder.setId(nodeFolder.getGlobalId().toString());
		stagingFolder.setName(nodeFolder.getName());
		stagingFolder.setDescription(nodeFolder.getDescription());
		stagingFolder.setPublishDir(nodeFolder.getPublishDir());

		fillMultichannellingData(nodeFolder, stagingFolder);

		// set i18n data
		stagingFolder.setNameI18n(I18nMap.TRANSFORM2REST.apply(nodeFolder.getNameI18n()));
		stagingFolder.setDescriptionI18n(I18nMap.TRANSFORM2REST.apply(nodeFolder.getDescriptionI18n()));
		stagingFolder.setPublishDirI18n(I18nMap.TRANSFORM2REST.apply(nodeFolder.getPublishDirI18n()));

		// fill the objecttags
		Map<String, ObjectTag> objectTagMap = new HashMap<>(nodeFolder.getObjectTags());

		// filter out object tags, which are not available for the Node
		new ObjectTagRestrictionFilter(nodeFolder.getOwningNode()).filter(objectTagMap.values());

		Map<String, StagedObjectTag> tags = new HashMap<>(objectTagMap.size());

		for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
			String tagName = i.next();
			ObjectTag objectTag = objectTagMap.get(tagName);

			StagedObjectTag restTag = getObjectTag(objectTag, new StagedObjectTag());
			tags.put(restTag.getName(), restTag);
		}
		stagingFolder.setObjectTags(tags);

		stagingFolder.setTemplateIds(nodeFolder.getTemplates().stream().map(t -> t.getGlobalId().toString()).collect(Collectors.toSet()));

		return stagingFolder;
	}

	/**
	 * Transform the given Overview into its REST Model
	 *
	 * @param stagedOverview
	 *            Node Object
	 * @param onReferenceNotFound 
	 * @param mismatches 
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Overview getOverview(Path ownerPath, String ownerId,
			StagedOverview stagedOverview, Set<StagingError> mismatches, Operator onReferenceNotFound) throws NodeException {
		if (stagedOverview == null || org.apache.commons.lang3.StringUtils.isBlank(stagedOverview.getId())) {
			return null;
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		com.gentics.contentnode.rest.model.Overview restOverview = ModelBuilder.getOverview(t.getObject(Overview.class, stagedOverview.getId(), true));

		restOverview.setGlobalId(stagedOverview.getId());

		// set the selection type
		restOverview.setSelectType(stagedOverview.getSelectType());

		// set the type of selected objects
		restOverview.setListType(stagedOverview.getListType());

		// set the type of order
		restOverview.setOrderBy(stagedOverview.getOrderBy());

		// set the order direction
		restOverview.setOrderDirection(stagedOverview.getOrderDirection());

		// set the template's source
		restOverview.setSource(stagedOverview.getSource());

		// set the entries
		if (stagedOverview.getSelectedNodeItemIds() != null && stagedOverview.getSelectedNodeItemIds().size() > 0) {
			// set the type of selected objects
			ListType listType = restOverview.getListType();

			Class<? extends NodeObject> entryClass = null;
			if (listType != null) {
				switch (listType) {
				case FILE:
					entryClass = File.class;
					break;
				case FOLDER:
					entryClass = Folder.class;
					break;
				case IMAGE:
					entryClass = ImageFile.class;
					break;
				case PAGE:
					entryClass = Page.class;
					break;
				case UNDEFINED:
					break;
				}
			}
			List<StagedNodeIdObjectId> overviewEntries = stagedOverview.getSelectedNodeItemIds();
			List<NodeIdObjectId> selectedObjects = new ArrayList<>(overviewEntries.size());
			List<Integer> selectedItemIds = new ArrayList<>(overviewEntries.size());

			for (StagedNodeIdObjectId entry : overviewEntries) {
				Integer localId = t.getLocalId(entryClass, entry.getObjectId(), onReferenceNotFound);
				if (localId != null) {
					if (org.apache.commons.lang3.StringUtils.isNotBlank(entry.getNodeId())) {
						selectedObjects.add(new NodeIdObjectId(t.getLocalId(Node.class, entry.getNodeId()), localId));
					} else {
						selectedItemIds.add(localId);
					}
				}
			}

			// if the sortorder is DESC, we will reverse the order of the entries
			// (entries will always be set in selection order, not in the real sortorder)
			if (stagedOverview.getOrderDirection() == OrderDirection.DESC) {
				Collections.reverse(selectedObjects);
			}
			if (selectedObjects.size() > 0) {
				restOverview.setSelectedNodeItemIds(selectedObjects);
			}
			if (selectedItemIds.size() > 0) {
				restOverview.setSelectedItemIds(selectedItemIds);
			}
		}

		// set the maximum number of listed entries
		restOverview.setMaxItems(stagedOverview.getMaxItems());

		// set the recursive flag
		restOverview.setRecursive(stagedOverview.getRecursive());

		return restOverview;
	}

	/**
	 * Transform the given Overview into its REST Model
	 *
	 * @param nodeOverview
	 *            Node Object
	 * @return REST Model
	 * @throws NodeException
	 */
	public static StagedOverview getOverview(
			Overview nodeOverview, StagedOverview restOverview) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// ID
		restOverview.setId(nodeOverview.getGlobalId().toString());

		// set the selection type
		restOverview.setSelectType(Overview.getSelectType(nodeOverview.getSelectionType()));

		// set the type of selected objects
		restOverview.setListType(Overview.getListType(nodeOverview.getObjectClass()));

		// set the type of order
		restOverview.setOrderBy(Overview.getOrderBy(nodeOverview.getOrderKind()));

		// set the order direction
		restOverview.setOrderDirection(Overview.getOrderDirection(nodeOverview.getOrderWay()));

		Value value = nodeOverview.getValue();

		if (null != value) {
			// set the template's source
			restOverview.setSource(value.getValueText());
		}

		// set the entries
		List<OverviewEntry> overviewEntries = nodeOverview.getOverviewEntries();

		// Fill out global IDs
		{
			List<StagedNodeIdObjectId> selectedObjects = new ArrayList<>(overviewEntries.size());

			for (OverviewEntry entry : overviewEntries) {
				StagedNodeIdObjectId id = new StagedNodeIdObjectId();
				NodeObject object = entry.getObject();
				if (object != null) {
					id.setObjectId(object.getGlobalId().toString());
					id.setNodeId(entry.getNodeId() > 0 ? t.getObject(Node.class, entry.getNodeId()).getGlobalId().toString() : null);
					selectedObjects.add(id);
				}
			}

			// if the sortorder is DESC, we will reverse the order of the entries
			// (entries will always be set in selection order, not in the real
			// sortorder)
			if (nodeOverview.getOrderWay() == com.gentics.contentnode.object.Overview.ORDERWAY_DESC) {
				Collections.reverse(selectedObjects);
			}
			restOverview.setSelectedNodeItemIds(selectedObjects);
		}

		// set the maximum number of listed entries
		restOverview.setMaxItems(nodeOverview.getMaxObjects());

		// set the recursive flag
		restOverview.setRecursive(nodeOverview.doRecursion());

		return restOverview;
	}

	/**
	 * Transform the property into the staged property
	 * @param nodeP property
	 * @param stagedP staged property
	 * @return staged property
	 * @throws NodeException
	 */
	public static StagedProperty getProperty(Property nodeP, StagedProperty stagedP) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		stagedP.setBooleanValue(nodeP.getBooleanValue());		
		stagedP.setContentTagId(t.getGlobalId(ContentTag.class, nodeP.getContentTagId()));
		stagedP.setFileId(t.getGlobalId(File.class, nodeP.getFileId()));
		stagedP.setFolderId(t.getGlobalId(Folder.class, nodeP.getFolderId()));
		stagedP.setFormId(t.getGlobalId(Form.class, nodeP.getFormId()));
		stagedP.setId(nodeP.getGlobalId());
		stagedP.setType(nodeP.getType());
		stagedP.setImageId(t.getGlobalId(ImageFile.class, nodeP.getImageId()));
		stagedP.setNodeId(t.getGlobalId(Node.class, nodeP.getNodeId()));
		stagedP.setOverview(null);
		com.gentics.contentnode.rest.model.Overview overviewModel = nodeP.getOverview();
		if (overviewModel != null && overviewModel.getId() != null) {
			Overview overview = t.getObject(Overview.class, overviewModel.getId());
			if (overview != null) {
				stagedP.setOverview(getOverview(overview, new StagedOverview()));
			}
		}
		stagedP.setPageId(t.getGlobalId(Page.class, nodeP.getPageId()));
		stagedP.setPartId(t.getGlobalId(Part.class, nodeP.getPartId()));
		stagedP.setStringValue(nodeP.getStringValue());
		stagedP.setStringValues(nodeP.getStringValues() != null ? new ArrayList<>(nodeP.getStringValues()): null);
		stagedP.setTemplateTagId(t.getGlobalId(TemplateTag.class, nodeP.getTemplateTagId()));
		// only set datasource reference, when datasource is an entity
		if (nodeP.getType() == Property.Type.DATASOURCE) {
			stagedP.setDatasource(null);
			stagedP.setOptions(nodeP.getOptions() != null ? new ArrayList<>(nodeP.getOptions()): null);
			stagedP.setSelectedOptions(null);
		} else if (nodeP.getDatasourceId() != null) {
			Datasource ds = t.getObject(Datasource.class, nodeP.getDatasourceId());
			if (ds != null) {
				stagedP.setDatasource(new StagedReferencedDatasource(ds).getStagedReference());
			} else {
				stagedP.setDatasource(null);
			}
			stagedP.setOptions(null);
			stagedP.setSelectedOptions(nodeP.getSelectedOptions() != null ? new ArrayList<>(nodeP.getSelectedOptions()): null);
		} else {
			stagedP.setDatasource(null);
			stagedP.setSelectedOptions(null);
			stagedP.setOptions(null);
		}
		stagedP.setTemplateId(t.getGlobalId(Template.class, nodeP.getTemplateId()));
		return stagedP;
	}

	/**
	 * Transform the staged property into the property
	 * @param stagedProperty staged property
	 * @param nodeProperty property
	 * @param mismatches set for collecting mismatching implementation entities
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @return property
	 * @throws NodeException
	 */
	public static Property getProperty(Path ownerPath, String ownerId, StagedProperty stagedProperty, Property nodeProperty, Set<StagingError> mismatches,
			com.gentics.contentnode.etc.Operator onReferenceNotFound) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		nodeProperty.setBooleanValue(stagedProperty.getBooleanValue());
		nodeProperty.setContentTagId(t.getLocalId(ContentTag.class, stagedProperty.getContentTagId(), onReferenceNotFound));
		nodeProperty.setFileId(t.getLocalId(File.class, stagedProperty.getFileId(), onReferenceNotFound));
		nodeProperty.setFolderId(t.getLocalId(Folder.class, stagedProperty.getFolderId(), onReferenceNotFound));
		nodeProperty.setFormId(t.getLocalId(Form.class, stagedProperty.getFormId(), onReferenceNotFound));
		nodeProperty.setGlobalId(stagedProperty.getId());
		nodeProperty.setImageId(t.getLocalId(ImageFile.class, stagedProperty.getImageId(), onReferenceNotFound));
		nodeProperty.setNodeId(t.getLocalId(Node.class, stagedProperty.getNodeId(), onReferenceNotFound));
		nodeProperty.setOptions(stagedProperty.getOptions() != null ? new ArrayList<>(stagedProperty.getOptions()): null);
		nodeProperty.setOverview(getOverview(ownerPath, ownerId, stagedProperty.getOverview(), mismatches, onReferenceNotFound));
		nodeProperty.setPageId(t.getLocalId(Page.class, stagedProperty.getPageId(), onReferenceNotFound));
		nodeProperty.setPartId(t.getLocalId(Part.class, stagedProperty.getPartId(), onReferenceNotFound));
		nodeProperty.setSelectedOptions(stagedProperty.getSelectedOptions() != null ? new ArrayList<>(stagedProperty.getSelectedOptions()): null);
		nodeProperty.setStringValue(stagedProperty.getStringValue());
		nodeProperty.setStringValues(stagedProperty.getStringValues() != null ? new ArrayList<>(stagedProperty.getStringValues()): null);
		nodeProperty.setTemplateTagId(t.getLocalId(TemplateTag.class, stagedProperty.getTemplateTagId(), onReferenceNotFound));
		getReferenced(ownerPath, ownerId, Datasource.class, StagedReferencedDatasource::new, stagedProperty.getDatasource(), mismatches, nodeProperty::setDatasourceId);
		nodeProperty.setTemplateId(t.getLocalId(Template.class, stagedProperty.getTemplateId()));
		return nodeProperty;
	}

	/**
	 * Transform the object tag into the staged object tag
	 * @param tag object tag
	 * @param model staged object tag
	 * @return staged object tag
	 * @throws NodeException
	 */
	public static StagedObjectTag getObjectTag(ObjectTag tag, StagedObjectTag model) throws NodeException {
		getTag(tag, model);
		ObjectTagDefinition definition = tag.getDefinition();
		if (definition != null) {
			model.setDefinition(new StagedReferencedObjectTagDefinition(definition).getStagedReference());
		}
		return model;
	}

	/**
	 * Transform the tag into the staged tag
	 * @param nodeTag tag
	 * @param restTag staged tag
	 * @return staged tag
	 * @throws NodeException
	 */
	public static StagedTag getTag(Tag nodeTag, StagedTag restTag) throws NodeException {
		// Meta Properties
		restTag.setId(nodeTag.getGlobalId().toString());
		restTag.setName(nodeTag.getName());
		restTag.setActive(nodeTag.isEnabled());
		restTag.setConstruct(new StagedReferencedConstruct(nodeTag.getConstruct()).getStagedReference());

		int ttype = ObjectTransformer.getInt(nodeTag.getTType(), 0);

		switch (ttype) {
		case Tag.TYPE_CONTENTTAG:
			restTag.setType(com.gentics.contentnode.rest.model.Tag.Type.CONTENTTAG);
			break;

		case Tag.TYPE_TEMPLATETAG:
			restTag.setType(com.gentics.contentnode.rest.model.Tag.Type.TEMPLATETAG);
			break;

		case Tag.TYPE_OBJECTTAG:
			restTag.setType(com.gentics.contentnode.rest.model.Tag.Type.OBJECTTAG);
			break;
		}

		// values as properties
		ValueList values = nodeTag.getValues();
		Map<String, StagedProperty> properties = new HashMap<>(values.size());

		for (Value value : values) {
			Property property = value.getPartType().toProperty();
			properties.put(value.getPart().getKeyname(), getProperty(property, new StagedProperty()));
		}
		restTag.setProperties(properties);

		return restTag;
	}

	/**
	 * Inflate the page REST Model from its given node object.
	 *
	 * @param nodePage
	 *            Node Object
	 * @param restPage
	 *            REST model to inflate
	 * @return REST Model
	 * @throws NodeException
	 */
	public static StagedPage getPage(Page nodePage, StagedPage restPage) throws NodeException {
		restPage.setId(nodePage.getGlobalId().toString());
		restPage.setName(nodePage.getName());
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			restPage.setNiceUrl(nodePage.getNiceUrl());
			restPage.setAlternateUrls(new TreeSet<>(nodePage.getAlternateUrls()));
		}
		restPage.setFileName(nodePage.getFilename());
		restPage.setDescription(nodePage.getDescription());
		restPage.setPriority(nodePage.getPriority());
		restPage.setTemplate(new StagedReferencedTemplate(nodePage.getTemplate()).getStagedReference());

		restPage.setCustomCdate(nodePage.getCustomCDate().getIntTimestamp());
		restPage.setCustomEdate(nodePage.getCustomEDate().getIntTimestamp());

		fillMultichannellingData(nodePage, restPage);

		// eventually build the language exactly like in GCN
		ContentLanguage language = nodePage.getLanguage();

		if (language != null) {
			restPage.setLanguage(language.getCode());
		}

		GlobalId contentsetId = GlobalId.getGlobalId("contentset", nodePage.getContentsetId());
		if (contentsetId != null) {
			restPage.setContentsetId(contentsetId.toString());
		}

		restPage.setContentId(nodePage.getContent().getGlobalId().toString());

		// add contenttags
		Map<String, ContentTag> contenttagMap = nodePage.getContentTags();
		Map<String, StagedTag> restContentTags = new HashMap<>(contenttagMap.size());

		for (Iterator<String> i = contenttagMap.keySet().iterator(); i.hasNext();) {
			String tagName = i.next();
			ContentTag tag = contenttagMap.get(tagName);

			if (!nodePage.getContent().isPartiallyLocalized() || !tag.isInherited()) {
				restContentTags.put(tagName, getTag(tag, new StagedTag()));
			}
		}
		restPage.setTags(restContentTags);

		// add objecttags
		Map<String, ObjectTag> objectTagMap = new HashMap<>(nodePage.getObjectTags());

		// filter out restricted object tags
		new ObjectTagRestrictionFilter(nodePage.getOwningNode()).filter(objectTagMap.values());

		Map<String, StagedObjectTag> restObjectTags = new HashMap<>(objectTagMap.size());

		for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
			String tagName = i.next();
			ObjectTag objectTag = objectTagMap.get(tagName);

			StagedObjectTag restTag = getObjectTag(objectTag, new StagedObjectTag());
			restObjectTags.put(restTag.getName(), restTag);
		}
		restPage.setObjectTags(restObjectTags);
		return restPage;
	}

	public static final StagedObjectVersion getPublishedVersion(NodeObjectVersion version) {
		StagedObjectVersion stagedVersion = new StagedObjectVersion();
		stagedVersion.setType(StagedObjectVersionType.published);
		stagedVersion.setTimestamp(version.getDate().getIntTimestamp());
		stagedVersion.setVersionNumber(version.getNumber());
		return stagedVersion;
	}

	public static final StagedObjectVersion getCurrentVersion(NodeObjectVersion version) {
		StagedObjectVersion stagedVersion = new StagedObjectVersion();
		stagedVersion.setType(StagedObjectVersionType.current);
		stagedVersion.setTimestamp(version.getDate().getIntTimestamp());
		stagedVersion.setVersionNumber(version.getNumber());
		return stagedVersion;
	}

	public static final StagedObjectVersion getScheduledVersion(NodeObjectVersion version, int publishAt) {
		StagedObjectVersion stagedVersion = new StagedObjectVersion();
		stagedVersion.setType(StagedObjectVersionType.scheduled);
		stagedVersion.setPublishAt(publishAt);
		stagedVersion.setTimestamp(version.getDate().getIntTimestamp());
		stagedVersion.setVersionNumber(version.getNumber());
		return stagedVersion;
	}

	/**
	 * If the reference is not null, try to load the referenced object.
	 * If not found, throw an {@link EntityNotFoundException}, if found but the hash does not match
	 * the hash in the reference, add the referenced object to the set of mismatched objects.
	 * Otherwise call the consumer with the local ID of the referenced object
	 * @param <T> type of the referenced object
	 * @param path current path
	 * @param clazz class of the referenced object
	 * @param wrapper wrapper function
	 * @param reference reference
	 * @param mismatches set of mismatches
	 * @param consumer consumer for the local ID
	 * @throws NodeException
	 */
	public static <T extends NodeObject, U extends StagedReferencedObject<T>> void getReferenced(Path ownerPath, String ownerId, Class<T> clazz, Function<T, U> wrapper, StagedReference reference,
			Set<StagingError> mismatches, Consumer<Integer> consumer) throws NodeException {
		if (reference == null) {
			return;
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		T referenced = t.getObject(clazz, reference.getId());
		String table = t.getTable(clazz);
		if (referenced == null) {
			throw new EntityNotFoundException(String.format("Could not find %s with id %s", table, reference.getId()),
					I18NHelper.get(String.format("%s.notfound", table), reference.getId()));
		} else if (!wrapper.apply(referenced).matches(reference)) {
			var objectDescription = String.format("%s '%s' (%s, %d)", I18NHelper.get(table), I18NHelper.getName(referenced),
					referenced.getGlobalId().toString(), referenced.getId());
			 var errorMessage = I18NHelper.get("content.staging.import.object_error", objectDescription);
			 var stagingError = new StagingError(ownerId, ownerPath.toString(), errorMessage , StagingErrorKind.MISMATCH);
			 stagingError.setRecommendation(StagingErrorRecommendation.MISMATCH.toString());
			 mismatches.add(stagingError);

			 consumer.accept(referenced.getId());
		} else {
			consumer.accept(referenced.getId());
		}
	}

	/**
	 * Get the GCN Page corresponding to the given REST Page
	 *
	 * @param restPage   REST Page object
	 * @param page
	 * @param mismatches
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @param channel optional channel
	 * @return GCN Page object
	 */
	public static Page getPage(Path pagePath, StagedPage restPage, Page page, Set<StagingError> mismatches,
			com.gentics.contentnode.etc.Operator onReferenceNotFound, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// fill the meta data from the rest page into the gcn page object
		getReferenced(pagePath, restPage.getId(), Template.class, StagedReferencedTemplate::new, restPage.getTemplate(), mismatches, page::setTemplateId);
		if (restPage.getDescription() != null) {
			page.setDescription(restPage.getDescription());
		}
		if (!ObjectTransformer.isEmpty(restPage.getFileName())) {
			page.setFilename(restPage.getFileName());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (restPage.getNiceUrl() != null) {
				page.setNiceUrl(restPage.getNiceUrl());
			}
			if (restPage.getAlternateUrls() != null) {
				page.setAlternateUrls(restPage.getAlternateUrls());
			}
		}
		fillMultichannellingData(restPage, page, Page.class, channel);

		if (!ObjectTransformer.isEmpty(restPage.getName())) {
			page.setName(restPage.getName());
		}
		if (restPage.getPriority() != null) {
			page.setPriority(restPage.getPriority());
		}
		if (restPage.getCustomCdate() != null) {
			page.setCustomCDate(restPage.getCustomCdate());
		}
		if (restPage.getCustomEdate() != null) {
			page.setCustomEDate(restPage.getCustomEdate());
		}

		if (restPage.getContentsetId() != null) {
			page.setGlobalContentsetId(new GlobalId(restPage.getContentsetId()));
		}

		// transform the requested language code into a language and set it to the page
		ContentLanguage language = MiscUtils.getRequestedContentLanguage(page, restPage.getLanguage());

		if (language != null) {
			page.setLanguage(language);

			// when importing a page with a language, we need to check for conflicting pages with the same contentsetId and language
			int localContentsetId = ObjectTransformer.getInt(page.getContentsetId(), 0);
			int localChannelsetId = ObjectTransformer.getInt(page.getChannelSetId(), 0);
			if (localContentsetId > 0) {
				Set<Integer> conflictingPageIds = DBUtils.select("SELECT id FROM page WHERE contentset_id = ? AND contentgroup_id = ? AND channelset_id != ?", ps -> {
					ps.setInt(1, localContentsetId);
					ps.setInt(2, language.getId());
					ps.setInt(3, localChannelsetId);
				}, DBUtils.IDS);

				if (!conflictingPageIds.isEmpty()) {
					try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
						List<Page> conflictingPages = t.getObjects(Page.class, conflictingPageIds);
						for (Page conflict : conflictingPages) {
							if (!conflict.equals(page)) {
								conflict.delete(true);
							}
						}
					}
				}
			}
		}

		if (restPage.getContentId() != null) {
			// if the page is new, we check whether the content already exists
			if (page.isNew()) {
				Content existingContent = t.getObject(Content.class, restPage.getContentId(), true);
				if (existingContent != null) {
					// attach the existing content to the page (import a page variant)
					page.setContent(existingContent);
				} else {
					// set the global ID of the content
					page.getContent().setGlobalId(new GlobalId(restPage.getContentId()));
				}
			}
		}

		// set the tags
		Map<String, StagedTag> restTags = restPage.getTags();

		if (restTags != null) {
			// get the contenttags
			Map<String, ContentTag> contentTags = page.getContentTags();

			for (Iterator<StagedTag> i = restTags.values().iterator(); i.hasNext();) {
				StagedTag restTag = i.next();

				// Check whether the tag has a valid name
				if (restTag.getName() == null) {
					ModelBuilder.logger.error("Could not handle tag since it did not contain a valid name {" + restTag.getName() + " }");
					throw new NodeException("A tag that was specified within the request did not have a name.");
				}
				ContentTag contentTag = contentTags.get(restTag.getName());

				if (contentTag == null) {
					// did not find the tag, so create a new one
					contentTag = (ContentTag) t.createObject(ContentTag.class);
					contentTag.setName(restTag.getName());
					getReferenced(pagePath, restPage.getId(), Construct.class, StagedReferencedConstruct::new, restTag.getConstruct(), mismatches, contentTag::setConstructId);

					// and add it to the other tags
					contentTags.put(restTag.getName(), contentTag);
				}
				if (contentTag != null) {
					// now set the data from the REST tag
					getTag(pagePath, restPage.getId(), restTag, contentTag, mismatches, onReferenceNotFound);
				}
			}
		}

		getObjectTags(pagePath, restPage.getId(), restPage.getObjectTags(), page, page.getOwningNode(), mismatches, onReferenceNotFound);

		return page;
	}

	/**
	 * Transforms the given content staging node model into the corresponding node object.
	 *
	 * @param stagedNode
	 *            the model to transform
	 * @return the transformed node object
	 * @throws NodeException
	 */
	public static Node getNode(StagedNode stagedNode, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// for new nodes, we create a new root folder
		if (node.isNew()) {
			Folder rootFolder = t.createObject(Folder.class);
			rootFolder.setGlobalId(new GlobalId(stagedNode.getRootFolderId()));
			rootFolder.setName(stagedNode.getName());
			rootFolder.setPublishDir("/");

			node.setFolder(rootFolder);
			// note: this is deliberately only set for new nodes
			node.setHostname(stagedNode.getHostname());
			node.setPublishDisabled(true);
		}
		node.setHostnameProperty(stagedNode.getHostnameProperty());

		node.setHttps(stagedNode.isHttps());
		node.setPublishImageVariants(stagedNode.isPublishImageVariants());

		node.setMeshPreviewUrlProperty(stagedNode.getMeshPreviewUrlProperty());
		node.setMeshProjectName(stagedNode.getMeshProjectName());

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT)) {
			node.setPubDirSegment(stagedNode.isPubDirSegment());
		}

		if (stagedNode.getLanguages() != null) {
			List<ContentLanguage> languages = node.getLanguages();
			languages.clear();
			for (String code : stagedNode.getLanguages()) {
				ContentLanguage lang = LanguageFactory.get(code);
				if (lang != null) {
					languages.add(lang);
				}
			}
		}

		node.setPublishFilesystem(stagedNode.isPublishFs());
		node.setPublishFilesystemFiles(stagedNode.isPublishFsFiles());
		node.setPublishFilesystemPages(stagedNode.isPublishFsPages());

		if (stagedNode.getContentRepositoryId() != null) {
			Integer localCrId = t.getLocalId(ContentRepository.class, stagedNode.getContentRepositoryId());
			if (localCrId != null) {
				node.setContentrepositoryId(localCrId);
			}
		}
		node.setPublishContentmap(stagedNode.isPublishContentMap());
		node.setPublishContentMapFiles(stagedNode.isPublishContentMapFiles());
		node.setPublishContentMapFolders(stagedNode.isPublishContentMapFolders());
		node.setPublishContentMapPages(stagedNode.isPublishContentMapPages());

		node.setPublishDir(stagedNode.getPublishDir());
		node.setBinaryPublishDir(stagedNode.getBinaryPublishDir());

		node.setOmitPageExtension(stagedNode.isOmitPageExtension());
		node.setPageLanguageCode(stagedNode.getPageLanguageCode());

		node.setUrlRenderWayPages(Optional.ofNullable(stagedNode.getPageUrls()).map(UrlRenderWay::fromName).orElse(UrlRenderWay.AUTO).getValue());
		node.setUrlRenderWayFiles(Optional.ofNullable(stagedNode.getFileUrls()).map(UrlRenderWay::fromName).orElse(UrlRenderWay.AUTO).getValue());

		return node;
	}

	/**
	 * Transform the features from the staged node into the node
	 * @param stagedNode staged node
	 * @param node node
	 * @return node
	 * @throws NodeException
	 */
	public static Node setFeatures(StagedNode stagedNode, Node node) throws NodeException {
		List<Feature> active = node.getFeatures();
		Set<Feature> toSet = stagedNode.getFeatures().stream().map(Feature::getByName).filter(f -> f != null).collect(Collectors.toSet());

		for (Feature f : toSet) {
			if (!active.contains(f)) {
				node.activateFeature(f);
			} else {
				active.remove(f);
			}
		}
		for (Feature f : active) {
			node.deactivateFeature(f);
		}

		return node;
	}

	/**
	 * Transform the constructs from the staged node into the node
	 * @param stagedNode staged node
	 * @param node node
	 * @return node
	 * @throws NodeException
	 */
	public static Node setConstructs(StagedNode stagedNode, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (String constructId : stagedNode.getConstructIds()) {
			Construct construct = t.getObject(Construct.class, constructId);
			if (construct != null) {
				node.addConstruct(construct);
			}
		}

		return node;
	}

	/**
	 * Transform the object properties from the staged node into the node
	 * @param stagedNode staged node
	 * @param node node
	 * @return node
	 * @throws NodeException
	 */
	public static Node setObjectProperties(StagedNode stagedNode, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (String objectPropId : stagedNode.getObjectPropertyIds()) {
			ObjectTagDefinition objectProperty = t.getObject(ObjectTagDefinition.class, objectPropId);
			if (objectProperty != null && objectProperty.isRestricted()) {
				node.addObjectTagDefinition(objectProperty);
			}
		}
		return node;
	}

	/**
	 * Transform the assigned packages from the staged node into the node
	 * @param stagedNode staged node
	 * @param node node
	 * @return node
	 * @throws NodeException
	 */
	public static Node setPackages(StagedNode stagedNode, Node node) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.DEVTOOLS) && Synchronizer.getStatus() == Synchronizer.Status.UP) {
			for (String packageName : stagedNode.getPackages()) {
				try {
					Synchronizer.addPackage(node, packageName);
				} catch (EntityNotFoundException ignored) {
				}
			}
		}
		return node;
	}

	/**
	 * Variant of {@link #getNode(StagedNode, Node)} for channels
	 * @param stagedChannel model
	 * @param node node object
	 * @return node object
	 * @throws NodeException
	 */
	public static Node getChannel(StagedChannel stagedChannel, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		node = getNode(stagedChannel, node);
		if (node.isNew()) {
			Node directMaster = t.getObject(Node.class, stagedChannel.getMasterNodeId());
			if (directMaster == null) {
				// TODO error message
				throw new RestMappedException("").setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			} else {
				node.getFolder().setChannelMaster(directMaster.getFolder());
			}
		}
		return node;
	}

	/**
	 * Inflate the staging node model with ins node object.
	 * @param nodeNode node
	 * @param node staging model
	 * @return staging Model of the node
	 * @throws NodeException
	 */
	public static StagedNode getNode(Node nodeNode, StagedNode node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		node.setId(nodeNode.getGlobalId().toString());
		node.setName(nodeNode.getFolder().getName());
		node.setPublishDir(nodeNode.getPublishDir());
		node.setRootFolderId(nodeNode.getFolder().getGlobalId().toString());
		node.setBinaryPublishDir(nodeNode.getBinaryPublishDir());
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT)) {
			node.setPubDirSegment(nodeNode.isPubDirSegment());
		}
		node.setHttps(nodeNode.isHttps());
		if (!org.apache.commons.lang3.StringUtils.isBlank(nodeNode.getHostnameProperty())) {
			node.setHostnameProperty(nodeNode.getHostnameProperty());
		} else {
			node.setHostname(nodeNode.getHostname());
		}
		node.setPublishFs(nodeNode.doPublishFilesystem());
		node.setPublishFsPages(nodeNode.doPublishFilesystemPages());
		node.setPublishFsFiles(nodeNode.doPublishFilesystemFiles());
		node.setContentRepositoryId(t.getGlobalId(ContentRepository.class, nodeNode.getContentrepositoryId()));
		node.setPublishContentMap(nodeNode.doPublishContentmap());
		node.setPublishContentMapPages(nodeNode.doPublishContentMapPages());
		node.setPublishContentMapFiles(nodeNode.doPublishContentMapFiles());
		node.setPublishContentMapFolders(nodeNode.doPublishContentMapFolders());
		node.setPublishImageVariants(nodeNode.isPublishImageVariants());
		node.setPageUrls(UrlRenderWay.fromValue(nodeNode.getUrlRenderWayPages()).name());
		node.setFileUrls(UrlRenderWay.fromValue(nodeNode.getUrlRenderWayFiles()).name());
		node.setOmitPageExtension(nodeNode.isOmitPageExtension());
		node.setPageLanguageCode(nodeNode.getPageLanguageCode());
		node.setFeatures(nodeNode.getFeatures().stream().map(Feature::getName).collect(Collectors.toSet()));
		node.setLanguages(nodeNode.getLanguages().stream().map(ContentLanguage::getCode).collect(Collectors.toList()));
		if (!nodeNode.isChannel()) {
			node.setConstructIds(nodeNode.getConstructs().stream().map(construct -> construct.getGlobalId().toString()).collect(Collectors.toSet()));
			node.setObjectPropertyIds(nodeNode.getObjectTagDefinitions().stream().map(objProp -> objProp.getGlobalId().toString()).collect(Collectors.toSet()));
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.DEVTOOLS) && Synchronizer.getStatus() == Synchronizer.Status.UP) {
			node.setPackages(Synchronizer.getPackages(nodeNode));
		}

		node.setMeshPreviewUrlProperty(nodeNode.getMeshPreviewUrlProperty());
		node.setMeshProjectName(nodeNode.getMeshProjectName());

		return node;
	}

	/**
	 * Variant of {@link #getNode(Node, StagedNode)} for channels
	 * @param nodeChannel channel
	 * @param channel staging model
	 * @return staging model
	 * @throws NodeException
	 */
	public static StagedChannel getChannel(Node nodeChannel, StagedChannel channel) throws NodeException {
		getNode(nodeChannel, channel);
		List<Node> masterNodes = nodeChannel.getMasterNodes();
		if (!masterNodes.isEmpty()) {
			channel.setMasterNodeId(masterNodes.get(0).getGlobalId().toString());
		}
		return channel;
	}

	/**
	 * Transform the staged object tag into the object tag
	 * @param model staged object tag
	 * @param tag object tag
	 * @param mismatches set collecting mismatching implementation entities
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @return object tag
	 * @throws NodeException
	 */
	public static ObjectTag getObjectTag(Path ownerPath, String ownerId, StagedObjectTag model, ObjectTag tag, Set<StagingError> mismatches, com.gentics.contentnode.etc.Operator onReferenceNotFound) throws NodeException {
		getTag(ownerPath, ownerId, model, tag, mismatches, onReferenceNotFound);
		getReferenced(ownerPath, ownerId, ObjectTagDefinition.class, StagedReferencedObjectTagDefinition::new, model.getDefinition(), mismatches, id -> {});
		return tag;
	}

	/**
	 * Helper method to fill the data from the given restTag into the given tag
	 *
	 * @param restTag
	 *            rest tag holding data
	 * @param tag
	 *            NodeObject tag to fill data into
	 * @param mismatches set for collecting mismatching implementation entities
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @throws NodeException
	 */
	public static Tag getTag(Path ownerPath, String ownerId, StagedTag restTag, Tag tag, Set<StagingError> mismatches, com.gentics.contentnode.etc.Operator onReferenceNotFound) throws NodeException {
		getReferenced(ownerPath, ownerId, Construct.class, StagedReferencedConstruct::new, restTag.getConstruct(), mismatches, tag::setConstructId);

		tag.setEnabled(ObjectTransformer.getBoolean(restTag.getActive(), true));
		Map<String, StagedProperty> properties = restTag.getProperties();

		if (properties != null) {
			for (Value value : tag.getValues()) {
				String keyName = value.getPart().getKeyname();
				StagedProperty property = properties.get(keyName);

				PartType partType = value.getPartType();
				if (property != null && Objects.equals(property.getType(), partType.getPropertyType())) {
					partType.fromProperty(getProperty(ownerPath, ownerId, property, new Property(), mismatches, onReferenceNotFound));
				}
			}
		}

		return tag;
	}


	/**
	 * Set the staged object tags to the container
	 * @param stagedTags staged object tags
	 * @param container object tag container
	 * @param owningNode node owning the container
	 * @param mismatches set of mismatched implementation entities
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @throws NodeException
	 */
	public static void getObjectTags(Path ownerPath, String ownerId, Map<String, StagedObjectTag> stagedTags, ObjectTagContainer container,
			Node owningNode, Set<StagingError> mismatches, com.gentics.contentnode.etc.Operator onReferenceNotFound) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (stagedTags != null) {
			// get the objecttags
			Map<String, ObjectTag> objectTags = container.getObjectTags();

			for (StagedObjectTag restTag : stagedTags.values()) {
				String tagName = restTag.getName();

				if (tagName.startsWith("object.")) {
					String shortName = tagName.substring(7);
					ObjectTag oTag = objectTags.get(shortName);

					if (oTag == null) {
						// check whether we really should create the tag
						int defId = DBUtils.select("SELECT id FROM objtag WHERE obj_type = ? AND obj_id = ? AND name = ?", pst -> {
							pst.setInt(1, container.getTType());
							pst.setInt(2, 0);
							pst.setString(3, tagName);
						}, DBUtils.firstInt("id"));
						if (defId > 0) {
							ObjectTagDefinition definition = t.getObject(ObjectTagDefinition.class, defId);
							if (definition != null && !definition.isVisibleIn(owningNode)) {
								continue;
							}
						}

						// did not find the tag, so create a new one
						oTag = t.createObject(ObjectTag.class);
						oTag.setName(tagName);
						getReferenced(ownerPath, ownerId, Construct.class, StagedReferencedConstruct::new, restTag.getConstruct(), mismatches, oTag::setConstructId);

						// and add it to the other tags
						objectTags.put(shortName, oTag);
					}

					// now set the data from the REST tag
					if (oTag != null) {
						getObjectTag(ownerPath, ownerId, restTag, oTag, mismatches, onReferenceNotFound);
					}
				}

			}
		}
	}

	/**
	 * Get the GCN File corresponding to the given staging File
	 *
	 * @param restFile staging File object
	 * @param file
	 * @param mismatches
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @param channel  optional channel
	 * @return GCN File object
	 */
	public static File getFile(Path filePath, StagedFile restFile, File file, Set<StagingError> mismatches,
			com.gentics.contentnode.etc.Operator onReferenceNotFound, Node channel) throws NodeException {
		// fill in the data
		if (restFile.getName() != null) {
			file.setName(restFile.getName());
		}
		if (restFile.getDescription() != null) {
			file.setDescription(restFile.getDescription());
		}
		if (restFile.getFileType() != null) {
			file.setFiletype(restFile.getFileType());
		}
		if (restFile.getForceOnline() != null) {
			file.setForceOnline(restFile.getForceOnline());
		}
		if (restFile.getCustomCdate() != null) {
			file.setCustomCDate(restFile.getCustomCdate());
		}
		if (restFile.getCustomEdate() != null) {
			file.setCustomEDate(restFile.getCustomEdate());
		}
		fillMultichannellingData(restFile, file, File.class, channel);
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (restFile.getNiceUrl() != null) {
				file.setNiceUrl(restFile.getNiceUrl());
			}
			if (restFile.getAlternateUrls() != null) {
				file.setAlternateUrls(restFile.getAlternateUrls());
			}
		}

		getObjectTags(filePath, restFile.getId(), restFile.getObjectTags(), file, file.getOwningNode(), mismatches, onReferenceNotFound);

		return file;
	}


	/**
	 * Get the GCN Image corresponding to the given staged Image
	 *
	 * @param restImage REST Image object
	 * @param image
	 * @param mismatches
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @param channel optional channel
	 * @return GCN Image object
	 */
	public static ImageFile getImage(Path imagePath, StoredImage restImage, ImageFile image, Set<StagingError> mismatches,
			com.gentics.contentnode.etc.Operator onReferenceNotFound, Node channel) throws NodeException {
		// fill in the data
		if (restImage.getName() != null && !StringUtils.isEmpty(restImage.getName())) {
			image.setName(restImage.getName());
		}
		if (restImage.getDescription() != null) {
			image.setDescription(restImage.getDescription());
		}
		if (restImage.getFileType() != null && !StringUtils.isEmpty(restImage.getFileType())) {
			image.setFiletype(restImage.getFileType());
		}
		if (restImage.getForceOnline() != null) {
			image.setForceOnline(restImage.getForceOnline());
		}
		if (restImage.getCustomCdate() != null) {
			image.setCustomCDate(restImage.getCustomCdate());
		}
		if (restImage.getCustomEdate() != null) {
			image.setCustomEDate(restImage.getCustomEdate());
		}
		fillMultichannellingData(restImage, image, ImageFile.class, channel);
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (restImage.getNiceUrl() != null) {
				image.setNiceUrl(restImage.getNiceUrl());
			}
			if (restImage.getAlternateUrls() != null) {
				image.setAlternateUrls(restImage.getAlternateUrls());
			}
		}

		// Update focal point information
		if (restImage.getFpX() != null) {
			image.setFpX(restImage.getFpX());
		}
		if (restImage.getFpY() != null) {
			image.setFpY(restImage.getFpY());
		}

		getObjectTags(imagePath, restImage.getId(), restImage.getObjectTags(), image, image.getOwningNode(), mismatches, onReferenceNotFound);

		return image;
	}

	/**
	 * Inflate the given form from the staging form model
	 * @param restModel model
	 * @param form node object
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @return inflated object
	 * @throws NodeException
	 */
	public static Form getForm(StagedForm restModel, Form form, com.gentics.contentnode.etc.Operator onReferenceNotFound) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!org.apache.commons.lang3.StringUtils.isBlank(restModel.getName())) {
			form.setName(restModel.getName());
		}
		if (!org.apache.commons.lang3.StringUtils.isBlank(restModel.getDescription())) {
			form.setDescription(restModel.getDescription());
		}
		if (restModel.getSuccessPageId() != null) {
			form.setSuccessPageId(ObjectTransformer.getInt(t.getLocalId(Page.class, restModel.getSuccessPageId(), onReferenceNotFound), 0));
		}
		if (restModel.getSuccessNodeId() != null) {
			form.setSuccessNodeId(ObjectTransformer.getInt(t.getLocalId(Node.class, restModel.getSuccessNodeId(), onReferenceNotFound), 0));
		}
		if (restModel.getLanguages() != null) {
			form.setLanguages(restModel.getLanguages());
		}
		if (restModel.getData() != null) {
			JsonNode copy = restModel.getData().deepCopy();
			ModelBuilder.recursivelyReplaceAttributes(copy, name -> {
				if (name.endsWith(FormFactory.PAGEID_POSTFIX)) {
					return name;
				} else {
					return null;
				}
			}, (o, n) -> o
					.numberNode(TransactionManager.getCurrentTransaction().getLocalId(Page.class, n.asText(), onReferenceNotFound)));
			form.setData(copy);
		}

		return form;
	}

	/**
	 * Inflate the given staging form model from its node object.
	 *
	 * @param form
	 *            Node Object
	 * @param model
	 * 			  staging API object
	 * @return REST Model
	 * @throws NodeException
	 */
	public static StagedForm getForm(Form form, StagedForm model) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		model.setId(form.getGlobalId().toString());
		model.setName(form.getName());
		model.setDescription(form.getDescription());

		model.setSuccessPageId(t.getGlobalId(Page.class, form.getSuccessPageId()));
		model.setSuccessNodeId(t.getGlobalId(Node.class, form.getSuccessNodeId()));

		model.setLanguages(form.getLanguages());
		ObjectNode copy = form.getData().deepCopy();
		ModelBuilder.recursivelyReplaceAttributes(copy, name -> {
			if (name.endsWith(FormFactory.PAGEID_POSTFIX)) {
				return name;
			} else {
				return null;
			}
		}, (o, n) -> {
			return o.textNode(TransactionManager.getCurrentTransaction().getGlobalId(Page.class, n.asInt()));
		});
		model.setData(copy);

		return model;
	}

	/**
	 * Get the GCN Folder corresponding to the given Content Staging Folder
	 *
	 * @param stagedFolder staging Folder object
	 * @param folder
	 * @param mismatches
	 * @param onReferenceNotFound optional operator, which is called when a reference was not found
	 * @param channel optional channel
	 * @return GCN Folder object
	 */
	public static Folder getFolder(Path folderPath, StagedFolder stagedFolder, Folder folder, Set<StagingError> mismatches,
			com.gentics.contentnode.etc.Operator onReferenceNotFound, Node channel) throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();

		if (!ObjectTransformer.isEmpty(stagedFolder.getName())) {
			folder.setName(stagedFolder.getName());
		}
		if (stagedFolder.getDescription() != null) {
			folder.setDescription(stagedFolder.getDescription());
		}
		if (!ObjectTransformer.isEmpty(stagedFolder.getPublishDir())) {
			folder.setPublishDir(stagedFolder.getPublishDir());
		}

		fillMultichannellingData(stagedFolder, folder, Folder.class, channel);

		// transform the i18n data
		if (stagedFolder.getNameI18n() != null) {
			folder.setNameI18n(I18nMap.TRANSFORM2NODE.apply(stagedFolder.getNameI18n()));
		}
		if (stagedFolder.getDescriptionI18n() != null) {
			folder.setDescriptionI18n(I18nMap.TRANSFORM2NODE.apply(stagedFolder.getDescriptionI18n()));
		}
		if (stagedFolder.getPublishDirI18n() != null) {
			folder.setPublishDirI18n(I18nMap.TRANSFORM2NODE.apply(stagedFolder.getPublishDirI18n()));
		}

		getObjectTags(folderPath, stagedFolder.getId(), stagedFolder.getObjectTags(), folder, folder.getOwningNode(), mismatches, onReferenceNotFound);

		if (stagedFolder.getTemplateIds() != null) {
			List<Template> templates = folder.getTemplates();
			for (String id : stagedFolder.getTemplateIds()) {
				Template template = t.getObject(Template.class, id);
				if (template != null) {
					templates.add(template);
				}
			}
		}

		return folder;
	}

	/**
	 * When multichannelling is activated, add the multichannelling information to the staged object
	 * @param object object
	 * @param stagedObject staged object
	 * @throws NodeException
	 */
	protected static void fillMultichannellingData(Disinheritable<?> object, StagedLocalizableObject stagedObject) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
			if (!object.isMaster()) {
				stagedObject.setMasterId(object.getMaster().getGlobalId().toString());
			} else {
				stagedObject.setExcluded(object.isExcluded());
				stagedObject.setDisinheritDefault(object.isDisinheritDefault());
				Set<Node> channels = object.getDisinheritedChannels();
				stagedObject.setDisinheritedChannelIds(channels.stream().map(c -> c.getGlobalId().toString()).collect(Collectors.toSet()));
			}
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.PARTIAL_MULTICHANNELLING)) {
			if (Page.TYPE_PAGE_INTEGER.equals(object.getTType())) {
				((StagedPage) stagedObject).setPartiallyLocalized(((Page) object).getContent().isPartiallyLocalized());
			}
		}
	}

	/**
	 * Fill multichannelling data (channel and master object) from the staged model into the node object
	 * @param <T>
	 * @param stagedObject staged object
	 * @param object node object
	 * @param clazz object class
	 * @param channel optional channel to import into
	 * @throws NodeException
	 */
	protected static <T extends LocalizableNodeObject<?>> void fillMultichannellingData(
			StagedLocalizableObject stagedObject, Disinheritable<? extends T> object, Class<T> clazz, Node channel)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING) && channel != null) {
			if (stagedObject.getMasterId() != null) {
				T master = t.getObject(clazz, stagedObject.getMasterId());
				if (master == null) {
					throw new NodeException(String.format(
							"Error while importing %s %s: Master object with uuid %s not found",
							stagedObject.getTypeName(), stagedObject.getId(), stagedObject.getMasterId()));
					// TODO this is an error, master must already exist
				} else {
					object.setChannelInfo(channel.getId(), master.getChannelSetId());
				}
			} else if (object.isNew()) {
				object.setChannelInfo(channel.getId(), 0);
			}
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.PARTIAL_MULTICHANNELLING) && channel != null) {
			if (clazz.equals(Page.class)) {
				((Page) object).getContent().setPartiallyLocalized(((StagedPage) stagedObject).isPartiallyLocalized());
			}
		}
	}

	/**
	 * Update the exclusion/disinheritance settings from the staged model
	 * @param stagedObject staged model
	 * @param object object
	 * @throws NodeException
	 */
	public static void setMultichannellingRestrictions(StagedLocalizableObject stagedObject, Disinheritable<?> object) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
			if (object.isMaster()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				boolean excluded = ObjectTransformer.getBoolean(stagedObject.getExcluded(), object.isExcluded());
				Set<String> channelIds = Optional.ofNullable(stagedObject.getDisinheritedChannelIds()).orElse(Collections.emptySet());
				Set<Node> channels = new HashSet<>();

				for (String channelId : channelIds) {
					Node channel = t.getObject(Node.class, channelId);
					if (channel != null) {
						channels.add(channel);
					}
				}
				object.changeMultichannellingRestrictions(excluded, channels, false);

				boolean disinheritedByDefault = ObjectTransformer.getBoolean(stagedObject.getDisinheritDefault(), object.isDisinheritDefault());
				object.setDisinheritDefault(disinheritedByDefault, false);
			}
		}
	}
}
