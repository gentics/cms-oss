package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getTemplateResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Assert;

import com.gentics.api.contentnode.parttype.ExtensiblePartType;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.BreadcrumbPartType;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.ExtensiblePartTypeWrapper;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.ListPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.NavigationPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.SelectPartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlet.queue.NodeCopyQueueEntry;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.infrastructure.TestEnvironment;


/**
 * Static helper class for generation of test data
 */
public class ContentNodeTestDataUtils {
	/**
	 * Name of the template part for a velocity construct
	 */
	public static final String TEMPLATE_PARTNAME = "template";

	/**
	 * Name of the startfolder part for a breadcrumb/navigation construct
	 */
	public static final String STARTFOLDER_PARTNAME = "startfolder";

	/**
	 * Id of the System Group
	 */
	public final static int SYSTEM_GROUP_ID = 1;

	/**
	 * Id of the Node Group
	 */
	public final static int NODE_GROUP_ID = 2;


	/**
	 * Create a construct with a single visible editable part
	 * @param node node
	 * @param clazz parttype class
	 * @param constructKeyword keyword of the construct
	 * @param partKeyword keyword of the part
	 * @return id of the construct
	 * @throws NodeException
	 */
	public static <T extends PartType> int createConstruct(Node node, Class<T> clazz, String constructKeyword, String partKeyword) throws NodeException {
		if (clazz.isAssignableFrom(ExtensiblePartTypeWrapper.class)) {
			return createVelocityConstruct(node, constructKeyword, partKeyword);
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		// when creating a construct with a SelectPartType part, we create a Datasource first
		com.gentics.contentnode.object.Datasource ds = null;
		if (SelectPartType.class.isAssignableFrom(clazz)) {
			ds = t.createObject(com.gentics.contentnode.object.Datasource.class);
			ds.setSourceType(SourceType.staticDS);
			ds.setName("Test Datasource for " + constructKeyword);
			List<DatasourceEntry> entries = ds.getEntries();
			int dsId = 0;
			List<String> values = Arrays.asList("one", "two", "three");
			for (String value : values) {
				DatasourceEntry entry = t.createObject(DatasourceEntry.class);
				entry.setDsid(++dsId);
				entry.setKey(value);
				entry.setValue(value);
				entries.add(entry);
			}
			ds.save();
			t.commit(false);
		}

		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setIconName("icon");
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		if (node != null) {
			construct.getNodes().add(node);
		}

		Part part = t.createObject(Part.class);
		part.setEditable(1);
		part.setHidden(false);
		part.setKeyname(partKeyword);
		part.setName(partKeyword, 1);
		part.setPartTypeId(getPartTypeId(clazz));
		part.setDefaultValue(t.createObject(Value.class));

		if (ds != null) {
			part.setInfoInt(ObjectTransformer.getInt(ds.getId(), 0));
		}

		construct.getParts().add(part);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Create a construct with a visible part of given clazz.
	 * This method can be used for {@link VelocityPartType}, {@link BreadcrumbPartType} or {@link NavigationPartType}
	 * @param node node
	 * @param clazz parttype class
	 * @param constructKeyword keyword of the construct
	 * @param partKeyword keyword of the part
	 * @return id of the construct
	 * @throws NodeException
	 */
	public static <T extends ExtensiblePartType> int createExtensibleConstruct(Node node, Class<T> clazz, String constructKeyword, String partKeyword)
			throws NodeException {
		if (clazz.isAssignableFrom(VelocityPartType.class)) {
			return createVelocityConstruct(node, constructKeyword, partKeyword);
		} else if (clazz.isAssignableFrom(BreadcrumbPartType.class)) {
			return createBreadcrumbConstruct(node, constructKeyword, partKeyword);
		} else if (clazz.isAssignableFrom(NavigationPartType.class)) {
			return createNavigationConstruct(node, constructKeyword, partKeyword);
		} else {
			throw new NodeException("Unable to create construct for parttype " + clazz);
		}
	}

	/**
	 * Create a construct containing a velocity part
	 * @param node node
	 * @param constructKeyword construct keyword
	 * @param partKeyword part keyword for the velocity part
	 * @return construct id
	 * @throws NodeException
	 */
	public static int createVelocityConstruct(Node node, String constructKeyword, String partKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setIconName("icon");
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		if (node != null) {
			construct.getNodes().add(node);
		}

		Part vtlPart = t.createObject(Part.class);
		vtlPart.setEditable(0);
		vtlPart.setHidden(false);
		vtlPart.setKeyname(partKeyword);
		vtlPart.setName(partKeyword, 1);
		vtlPart.setPartTypeId(getPartTypeId(VelocityPartType.class));
		construct.getParts().add(vtlPart);

		Part templatePart = t.createObject(Part.class);
		templatePart.setEditable(1);
		templatePart.setHidden(true);
		templatePart.setKeyname(TEMPLATE_PARTNAME);
		templatePart.setName(TEMPLATE_PARTNAME, 1);
		templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		t.createObject(Value.class).setPart(templatePart);
		construct.getParts().add(templatePart);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Create a default breadcrumb construct
	 * @param node node
	 * @param constructKeyword construct keyword
	 * @param partKeyword part keyword
	 * @return construct id
	 * @throws NodeException
	 */
	public static int createBreadcrumbConstruct(Node node, String constructKeyword, String partKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setIconName("icon");
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		construct.getNodes().add(node);

		// breadcrumb part
		Part vtlPart = t.createObject(Part.class);
		vtlPart.setEditable(0);
		vtlPart.setHidden(false);
		vtlPart.setKeyname(partKeyword);
		vtlPart.setName(partKeyword, 1);
		vtlPart.setPartTypeId(getPartTypeId(BreadcrumbPartType.class));
		construct.getParts().add(vtlPart);

		// template part
		Part templatePart = t.createObject(Part.class);
		templatePart.setEditable(1);
		templatePart.setHidden(true);
		templatePart.setKeyname(TEMPLATE_PARTNAME);
		templatePart.setName(TEMPLATE_PARTNAME, 1);
		templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		templatePart.setDefaultValue(t.createObject(Value.class));
		try {
			templatePart.getDefaultValue().setValueText(StringUtils.readStream(ContentNodeTestDataUtils.class.getResourceAsStream("breadcrumb.vm")));
		} catch (IOException e) {
			throw new NodeException("Could not create breadcrumb part", e);
		}
		construct.getParts().add(templatePart);

		// startfolder part
		Part startfolderPart = t.createObject(Part.class);
		startfolderPart.setEditable(1);
		startfolderPart.setHidden(true);
		startfolderPart.setKeyname(STARTFOLDER_PARTNAME);
		startfolderPart.setName(STARTFOLDER_PARTNAME, 1);
		startfolderPart.setPartTypeId(getPartTypeId(FolderURLPartType.class));
		startfolderPart.setDefaultValue(t.createObject(Value.class));
		construct.getParts().add(startfolderPart);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Create a default navigation construct
	 * @param node node
	 * @param constructKeyword construct keyword
	 * @param partKeyword part keyword
	 * @return construct id
	 * @throws NodeException
	 */
	public static int createNavigationConstruct(Node node, String constructKeyword, String partKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setIconName("icon");
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		construct.getNodes().add(node);

		// breadcrumb part
		Part vtlPart = t.createObject(Part.class);
		vtlPart.setEditable(0);
		vtlPart.setHidden(false);
		vtlPart.setKeyname(partKeyword);
		vtlPart.setName(partKeyword, 1);
		vtlPart.setPartTypeId(getPartTypeId(NavigationPartType.class));
		construct.getParts().add(vtlPart);

		// template part
		Part templatePart = t.createObject(Part.class);
		templatePart.setEditable(1);
		templatePart.setHidden(true);
		templatePart.setKeyname(TEMPLATE_PARTNAME);
		templatePart.setName(TEMPLATE_PARTNAME, 1);
		templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		templatePart.setDefaultValue(t.createObject(Value.class));
		try {
			templatePart.getDefaultValue().setValueText(StringUtils.readStream(ContentNodeTestDataUtils.class.getResourceAsStream("navigation.vm")));
		} catch (IOException e) {
			throw new NodeException("Could not create breadcrumb part", e);
		}
		construct.getParts().add(templatePart);

		// startfolder part
		Part startfolderPart = t.createObject(Part.class);
		startfolderPart.setEditable(1);
		startfolderPart.setHidden(true);
		startfolderPart.setKeyname(STARTFOLDER_PARTNAME);
		startfolderPart.setName(STARTFOLDER_PARTNAME, 1);
		startfolderPart.setPartTypeId(getPartTypeId(FolderURLPartType.class));
		startfolderPart.setDefaultValue(t.createObject(Value.class));
		construct.getParts().add(startfolderPart);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);	}

	/**
	 * Get the parttype id for the given parttype class
	 * @param clazz class
	 * @return parttype id
	 * @throws NodeException if the parttype class was not found
	 */
	public static <T> int getPartTypeId(final Class<T> clazz) throws NodeException {
		final int[] id = new int[1];
		DBUtils.executeStatement("SELECT id FROM type WHERE javaclass = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setString(1, clazz.getName());
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					id[0] = rs.getInt("id");
				} else {
					throw new NodeException("Could not find type for " + clazz);
				}
			}
		});

		return id[0];
	}

	/**
	 * Get the parttype implementation of the given part from the container
	 * @param clazz expected parttype implementation class
	 * @param container container
	 * @param partKeyword keyword of the part
	 * @return parttype implementation
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PartType> T getPartType(Class<T> clazz, ValueContainer container, String partKeyword) throws NodeException {
		Value value = container.getValues().getByKeyname(partKeyword);
		if (value == null) {
			throw new NodeException(container + " does not contain part " + partKeyword);
		}
		PartType partType = value.getPartType();
		if (clazz.isAssignableFrom(partType.getClass())) {
			return (T) value.getPartType();
		} else {
			throw new NodeException("Part " + partKeyword + " of " + container + " is of " + partType.getClass() + " and not " + clazz);
		}
	}

	/**
	 * Creates a dummy node with predefined attributes with the given features enabled.
	 *
	 * @return
	 * @throws NodeException
	 */
	public static Node createNode(Feature ...features) throws NodeException {
		return createNode("dummyNode", "dummyHost", "/nowhere", null, false,
				false, false, features);
	}

	/**
	 * Create a new node
	 *
	 * @param name
	 * @param host
	 * @param publishDir
	 * @param binaryPublishDir
	 * @param publishFS
	 * @param https
	 * @return
	 * @throws NodeException
	 */
	public static Node createNode(String name, String host, String publishDir, String binaryPublishDir, boolean publishFS, boolean https) throws NodeException {
		return createNode(name, host, publishDir, binaryPublishDir, publishFS, https, false);
	}

	/**
	 * Fill the given value with data according to its PartType
	 * @param value value to be filled
	 * @param text text for text based PartTypes
	 * @param flag flag for CheckboxPartType or for the ChangeableListPartType
	 * @param listEntries list entries for ListPartTypes
	 * @param contentTag content tag
	 * @param templateTag template tag
	 * @param file file
	 * @param folder folder
	 * @param node node
	 * @param image image
	 * @param page page
	 * @throws NodeException
	 */
	public static void fillValue(Value value, String text, boolean flag, List<String> listEntries, ContentTag contentTag, TemplateTag templateTag, File file,
			Node node, Folder folder, ImageFile image, Page page) throws NodeException {
		PartType partType = value.getPartType();
		if (partType instanceof TextPartType) {
			value.setValueText(text);
		} else if (partType instanceof CheckboxPartType) {
			value.setValueText(flag ? "1" : "0");
		} else if (partType instanceof SelectPartType) {
			com.gentics.contentnode.object.Datasource ds = ((SelectPartType) partType).getDatasource();
			List<DatasourceEntry> entries = ds.getEntries();
			if (!entries.isEmpty()) {
				value.setValueText("" + entries.get(0).getDsid());
			}
		} else if (partType instanceof DatasourcePartType) {
			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.Datasource ds = ((DatasourcePartType) partType).getDatasource();
			for (String entryValue : listEntries) {
				DatasourceEntry entry = t.createObject(DatasourceEntry.class);
				entry.setKey(entryValue);
				entry.setValue(entryValue);
				ds.getEntries().add(entry);
			}
		} else if (partType instanceof ListPartType) {
			if (partType instanceof ChangeableListPartType) {
				value.setInfo(flag ? 1 : 0);
			}
			value.setValueText(StringUtils.merge((String[]) listEntries.toArray(new String[listEntries.size()]), "\n"));
		} else if (partType instanceof OverviewPartType) {
			value.setValueText("<node page.name>");
			Overview overview = ((OverviewPartType) partType).getOverview();
			overview.setObjectClass(Page.class);
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			List<OverviewEntry> entries = overview.getOverviewEntries();
			Transaction t = TransactionManager.getCurrentTransaction();
			OverviewEntry newEntry = t.createObject(OverviewEntry.class);
			newEntry.setObjectId(page.getId());
			entries.add(newEntry);
		} else if (partType instanceof PageTagPartType) {
			List<Page> pages = ((Content)contentTag.getContainer()).getPages();
			((PageTagPartType) partType).setPageTag(pages.get(0), contentTag);
		} else if (partType instanceof TemplateTagPartType) {
			((TemplateTagPartType) partType).setTemplateTag(templateTag.getTemplate(), templateTag);
		} else if (partType instanceof FileURLPartType) {
			((FileURLPartType) partType).setTargetFile(file);
		} else if (partType instanceof NodePartType) {
			((NodePartType) partType).setNode(node);
		} else if (partType instanceof FolderURLPartType) {
			((FolderURLPartType) partType).setTargetFolder(folder);
		} else if (partType instanceof ImageURLPartType) {
			((ImageURLPartType) partType).setTargetImage(image);
		} else if (partType instanceof PageURLPartType) {
			((PageURLPartType) partType).setTargetPage(page);
		}
	}

	/**
	 * Create a new node
	 *
	 * @param name
	 *            name of the node (root folder)
	 * @param host
	 *            hostname
	 * @param publishDir
	 *            publish dir of the node
	 * @param binaryPublishDir
	 *            publish dir for binaries (if not null)
	 * @param publishFS
	 *            true if the node shall publish into the filesystem
	 * @param https
	 *            Whether https should be enabled or not
	 * @param publishStartPage
	 *            Whether this node should publish startpages during instant publishing as well.
	 * @param features
	 *            List of features to enable for the created node.
	 * @return new node (as non editable instance)
	 * @throws NodeException
	 */
	public static Node createNode(String name, String host, String publishDir, String binaryPublishDir, boolean publishFS, boolean https, boolean publishStartPage, Feature ...features) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node node = t.createObject(Node.class);
		Folder root = t.createObject(Folder.class);

		root.setName(name);
		root.setPublishDir("/");
		node.setFolder(root);
		node.setHostname(host);
		node.setPublishDir(publishDir);
		if (binaryPublishDir != null) {
			node.setBinaryPublishDir(binaryPublishDir);
		}
		node.setPublishFilesystem(publishFS);
		node.setHttps(https);

		if (Feature.PUBLISH_FOLDER_STARTPAGE.isActivated()) {
			if (publishStartPage) {
				node.activateFeature(Feature.PUBLISH_FOLDER_STARTPAGE);
			} else {
				node.deactivateFeature(Feature.PUBLISH_FOLDER_STARTPAGE);
			}
		}

		node.save();

		for (Feature feature : features) {
			node.activateFeature(feature);
		}

		t.commit(false);

		return t.getObject(Node.class, node.getId());
	}

	/**
	 * Transform the given list of entries into a (sorted) map, where keys and values are identical
	 * @param entries list of entries
	 * @return sorted map
	 */
	private static Map<String, String> listToMap(List<String> entries) {
		Map<String, String> entryMap = new LinkedHashMap<String, String>(entries.size());
		for (String entry : entries) {
			entryMap.put(entry, entry);
		}

		return entryMap;
	}

	/**
	 * Create a new datasource
	 * @param name name
	 * @param entries datasource entries (will be used as keys and values)
	 * @return datasource
	 * @throws Exception
	 */
	public static Datasource createDatasource(String name, List<String> entries) throws NodeException {
		return createDatasource(name, listToMap(entries));
	}

	/**
	 * Create an new datasource
	 * @param name name
	 * @param entries entry map
	 * @return datasource
	 * @throws Exception
	 */
	public static Datasource createDatasource(String name, Map<String, String> entries) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Datasource datasource = t.createObject(Datasource.class);
		datasource.setSourceType(SourceType.staticDS);
		datasource.setName(name);
		fillDatasource(datasource, entries);
		return t.getObject(Datasource.class, datasource.getId());
	}

	/**
	 * Fill the datasource with the given entries (clear the datasource first)
	 * @param ds datasource
	 * @param entries entries
	 * @throws NodeException
	 */
	public static void fillDatasource(Datasource ds, List<String> entries) throws NodeException {
		fillDatasource(ds, listToMap(entries));
	}

	/**
	 * Fill the datasource with the given entries (clear the datasource first)
	 * @param ds datasource
	 * @param entries entries
	 * @throws NodeException
	 */
	public static void fillDatasource(Datasource ds, Map<String, String> entries) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ds.getEntries().clear();
		int dsId = 1;
		for (Map.Entry<String, String> entry : entries.entrySet()) {
			DatasourceEntry dsEntry = t.createObject(DatasourceEntry.class);
			dsEntry.setKey(entry.getKey());
			dsEntry.setValue(entry.getValue());
			dsEntry.setDsid(dsId++);
			ds.getEntries().add(dsEntry);
		}
		ds.save();
		t.commit(false);
	}

	/**
	 * Create a new datasource
	 * @param name name
	 * @param entries entries
	 * @return datasource
	 * @throws NodeException
	 */
	@SafeVarargs
	public static Datasource createDatasource(String name, Pair<String, String>...entries) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Datasource datasource = t.createObject(Datasource.class);
		datasource.setSourceType(SourceType.staticDS);
		datasource.setName(name);
		fillDatasource(datasource, entries);
		return t.getObject(Datasource.class, datasource.getId());
	}

	/**
	 * Fill the datasource with the given entries (clear the datasource first)
	 * @param ds datasource
	 * @param entries entries
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void fillDatasource(Datasource ds, Pair<String, String>...entries) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ds.getEntries().clear();
		int dsId = 1;
		for (Pair<String, String> entry : entries) {
			DatasourceEntry dsEntry = t.createObject(DatasourceEntry.class);
			dsEntry.setKey(entry.getKey());
			dsEntry.setValue(entry.getValue());
			dsEntry.setDsid(dsId++);
			ds.getEntries().add(dsEntry);
		}
		ds.save();
		t.commit(false);
	}

	/**
	 * Fill an overview part of the given (editable) tag. The changes are not persisted
	 * You probably have to save the passed tag after calling this method.
	 * @param tag tag, which must be editable
	 * @param partKeyword part keyword of the overview part
	 * @param template overview template
	 * @param clazz class of objects listed in the overview
	 * @param selectionType selection type
	 * @param maxObjects max listed objects
	 * @param orderKind order kind
	 * @param orderWay order way
	 * @param recursive true for recursive (if selection is by folder)
	 * @param selected list of selected objects
	 * @throws NodeException
	 */
	public static void fillOverview(Tag tag, String partKeyword, String template, Class<? extends NodeObject> clazz, int selectionType, int maxObjects, int orderKind,
			int orderWay, boolean recursive, List<? extends NodeObject> selected) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		tag.getValues().getByKeyname(partKeyword).setValueText(template);

		OverviewPartType overviewPartType = getPartType(OverviewPartType.class, tag, partKeyword);
		Overview overview = overviewPartType.getOverview();
		overview.setObjectClass(clazz);
		overview.setSelectionType(selectionType);
		overview.setMaxObjects(maxObjects);
		overview.setOrderKind(orderKind);
		overview.setOrderWay(orderWay);
		overview.setRecursion(recursive);

		if (!ObjectTransformer.isEmpty(selected)) {
			List<OverviewEntry> entries = overview.getOverviewEntries();
			for (NodeObject sel : selected) {
				OverviewEntry entry = t.createObject(OverviewEntry.class);
				entry.setObjectId(sel.getId());
				entries.add(entry);
			}
		}
	}

	/**
	 * Create a new simple template
	 *
	 * @param parentFolder
	 * @param source
	 * @param name
	 * @param tags optional list of tags to be added to the template
	 * @return
	 * @throws NodeException
	 */
	public static Template createTemplate(Folder parentFolder, String source, String name, TemplateTag...tags) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);

		template.setName(name);
		template.setMlId(1);
		template.setSource(source);
		template.setFolderId(parentFolder.getId());

		for (TemplateTag tag : tags) {
			template.getTags().put(tag.getName(), tag);
		}

		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a new template tag, which will not be saved (must be added to a template first)
	 * @param constructId construct id
	 * @param name name of the tag
	 * @param editableInPage true if tag shall be editable in page
	 * @param mandatory true if tag shall be mandatory
	 * @return tag
	 * @throws NodeException
	 */
	public static TemplateTag createTemplateTag(int constructId, String name, boolean editableInPage, boolean mandatory) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		TemplateTag tag = t.createObject(TemplateTag.class);

		tag.setConstructId(constructId);
		tag.setName(name);
		tag.setEnabled(true);
		tag.setPublic(editableInPage);
		tag.setMandatory(mandatory);

		return tag;
	}

	/**
	 * Localize the template in the channel
	 * @param template template
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	public static Template localize(Template template, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template copy = (Template)template.copy();
		copy.setChannelInfo(channel.getId(), template.getChannelSetId());
		copy.save();
		t.commit(false);
		return t.getObject(Template.class, copy.getId());
	}

	/**
	 * Create a new objectTag definition
	 *
	 * @param objTagName
	 * @param targetType (e.g.: Page.TYPE_PAGE)
	 * @param constructId
	 * @param nodes optional list of nodes to restrict
	 * @throws NodeException
	 */
	public static ObjectTagDefinition createObjectTagDefinition(String objTagName, int targetType, Integer constructId, Node...nodes) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ObjectTagDefinition objProp = t.createObject(ObjectTagDefinition.class);

		objProp.setTargetType(targetType);
		objProp.setName(objTagName, 1);
		ObjectTag objectTag = objProp.getObjectTag();
		objectTag.setConstructId(constructId);
		objectTag.setEnabled(true);
		objectTag.setName("object." + objTagName);
		objectTag.setObjType(targetType);

		for (Node node : nodes) {
			objProp.getNodes().add(node);
		}

		objProp.save();
		t.commit(false);

		return objProp;
	}

	/**
	 * Creates a velocity construct
	 *
	 * @return
	 * @throws NodeException
	 */
	public static Construct createVelocityConstruct(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("constr");
		construct.setName("Construct (de)", 1);
		construct.setName("Construct (en)", 2);
		construct.setIconName("icon.png");
		construct.getNodes().add(node);

		Part velPart = t.createObject(Part.class);
		velPart.setKeyname("velocity");
		velPart.setHidden(false);
		velPart.setEditable(0);
		velPart.setName("velocity", 1);
		velPart.setName("velocity", 2);
		velPart.setPartOrder(1);
		velPart.setPartTypeId(33);

		Part tplPart = t.createObject(Part.class);
		tplPart.setKeyname("template");
		tplPart.setHidden(true);
		tplPart.setEditable(0);
		tplPart.setName("tpl", 1);
		tplPart.setName("tpl", 2);
		tplPart.setPartOrder(2);
		tplPart.setPartTypeId(21);

		Part textPart = t.createObject(Part.class);
		textPart.setKeyname("text");
		textPart.setHidden(true);
		textPart.setEditable(2);
		textPart.setName("Text", 1);
		textPart.setName("Text", 2);
		textPart.setPartOrder(3);
		textPart.setPartTypeId(1);

		List<Part> parts = construct.getParts();
		parts.add(velPart);
		parts.add(tplPart);
		parts.add(textPart);

		Value vval = t.createObject(Value.class);
		vval.setContainer(construct);
		vval.setPart(tplPart);
		vval.setValueText("");
		tplPart.setDefaultValue(vval);

		construct.save();

		return construct;
	}

	/**
	 * Create a new node.
	 * If the publish target is CONTENTREPOSITORY it will create a cr in memory.
	 *
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param publishTarget
	 *            publish target of the node
	 * @return the created node
	 * @throws NodeException
	 */
	public static Node createNode(String hostName, String name, PublishTarget publishTarget, ContentLanguage... language) throws NodeException {
		return createNode(hostName, name, publishTarget, false, false, language);
	}

	/**
	 * Create a new node.
	 * If the publish target is CONTENTREPOSITORY it will create a cr in memory.
	 *
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param publishTarget
	 *            publish target of the node
	 * @param languages
	 *            languages assigned to the node
	 * @param mccr true to publish into a mccr, false for a normal cr
	 * @param instant True for instant publishing, false otherwise
	 * @return the created node
	 * @throws NodeException
	 */
	public static Node createNode(String hostName, String name, PublishTarget publishTarget,
			boolean mccr, boolean instant, ContentLanguage... language) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/Content.node");
		node.setPublishFilesystem(false);
		Folder rootFolder = t.createObject(Folder.class);

		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/home/");

		node.save();

		if (language != null) {
			int position = 1;
			for (ContentLanguage lang : language) {
				DBUtils.executeInsert("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (?, ?, ?)",
						new Object[] { node.getId(), lang.getId(), position++ });
			}
		}

		t.commit(false);

		if (publishTarget.isPublishCR()) {
			// activate publishing into the contentrepository
			ContentRepository cr = createContentRepositoryWithDatsource(name, mccr, instant, node);
			node.setPublishContentmap(true);
			node.setContentrepositoryId(cr.getId());
		}

		if (publishTarget.isPublishFS()) {
			node.setPublishFilesystem(true);
		}

		node.save();
		t.commit(false);

		return t.getObject(Node.class, node.getId());
	}

	/**
	 * Create a new channel
	 *
	 * @param master
	 *            master node
	 * @param name
	 *            name of the channel (root folder)
	 * @param host
	 *            hostname
	 * @param publishDir
	 *            publish dir of the channel
	 * @return new channel (as non editable instance)
	 * @throws NodeException
	 */
	public static Node createChannel(Node master, String name, String host, String publishDir) throws NodeException {
		return createChannel(master, name, host, publishDir, PublishTarget.NONE, false);
	}

	/**
	 * Create a new channel
	 *
	 * @param master
	 *            master node
	 * @param name
	 *            name of the channel (root folder)
	 * @param host
	 *            hostname
	 * @param publishDir
	 *            publish dir of the channel
	 * @param publishTarget publish target
	 * @return new channel (as non editable instance)
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public static Node createChannel(Node master, String name, String host, String publishDir,
			PublishTarget publishTarget)
					throws NodeException {
		return createChannel(master, name, host, publishDir, publishTarget, false);
	}

	/**
	 * Create a new channel
	 *
	 * @param master
	 *            master node
	 * @param name
	 *            name of the channel (root folder)
	 * @param host
	 *            hostname
	 * @param publishDir
	 *            publish dir of the channel
	 * @param publishTarget publish target
	 * @param instant True for instant publishing, false otherwise
	 * @return new channel (as non editable instance)
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public static Node createChannel(Node master, String name, String host, String publishDir,
			PublishTarget publishTarget, boolean instant)
					throws NodeException {
		MiscUtils.require(Feature.MULTICHANNELLING);
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.createObject(Node.class);
		Folder channelRootFolder = t.createObject(Folder.class);

		channel.setFolder(channelRootFolder);
		channel.setHostname(host);
		channel.setPublishDir(publishDir);
		channelRootFolder.setName(name);
		channelRootFolder.setPublishDir("/");
		channelRootFolder.setChannelMaster(master.getFolder());

		channel.save();
		t.commit(false);

		if (publishTarget.isPublishCR()) {
			// when the master publishes into a mccr cr, the channel will do the same
			ContentRepository masterCR = master.getContentRepository();
			if (masterCR != null && masterCR.isMultichannelling()) {
				channel.setPublishContentmap(true);
				channel.setContentrepositoryId(masterCR.getId());
			} else {
				// activate publishing into the contentrepository
				ContentRepository cr = createContentRepositoryWithDatsource(name, false, instant, channel);
				channel.setPublishContentmap(true);
				channel.setContentrepositoryId(cr.getId());

			}
		}

		if (publishTarget.isPublishFS()) {
			channel.setPublishFilesystem(true);
		}

		channel.save();
		t.commit(false);

		return t.getObject(Node.class, ObjectTransformer.getInteger(channel.getId(), -1));
	}

	/**
	 * Create a new object property definition
	 * @param type type of the object
	 * @param constructId construct id
	 * @param name name
	 * @param keyword keyword
	 * @return object property definition
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException
	 */
	public static ObjectTagDefinition createObjectPropertyDefinition(
			int type, int constructId, String name, String keyword)
					throws InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ObjectTagDefinition objProp = t.createObject(ObjectTagDefinition.class);

		objProp.setTargetType(type);
		objProp.setName(name, 1);
		ObjectTag objectTag = objProp.getObjectTag();

		objectTag.setConstructId(constructId);
		objectTag.setEnabled(true);
		if (!keyword.startsWith("object.")) {
			keyword = "object." + keyword;
		}
		objectTag.setName(keyword);
		objectTag.setObjType(type);
		objProp.save();
		t.commit(false);

		return t.getObject(ObjectTagDefinition.class, objProp.getId());
	}

	/**
	 *
	 * @param type
	 * @param constructId
	 * @param keyword
	 * @param inTag
	 * @param Owning node object
	 * @return
	 * @throws NodeException
	 */
	public static ObjectTag createInTagObjectTag(ObjectTag inTag, int constructId,
			String keyword, NodeObject owner) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ObjectTag objectTag = t.createObject(ObjectTag.class);

		objectTag.setConstructId(constructId);
		objectTag.setEnabled(true);
		if (!keyword.startsWith("object.")) {
			keyword = "object." + keyword;
		}
		objectTag.setName(keyword);
		objectTag.setInTagObject(inTag);
		objectTag.setObjType(inTag.getObjType());
		objectTag.setNodeObject(owner);
		objectTag.save();

		t.commit(false);

		return t.getObject(ObjectTag.class, objectTag.getId());
	}

	/**
	 * Create a form with the given name in the specified folder.
	 *
	 * <p>
	 *     The form will be saved, but not published.
	 * </p>
	 *
	 * @see #create(Class, NodeObjectHandler)
	 *
	 * @param folder The parent folder for the form
	 * @param name The form name
	 * @return The created form
	 * @throws NodeException
	 */
	public static Form createForm(Folder folder, String name) throws NodeException {
		return create(
			Form.class,
			form -> {
				form.setFolderId(folder.getId());
				form.setName(name);
			});
	}

	/**
	 * Create a new folder
	 * @param mother mother folder (must not be null)
	 * @param name folder name
	 * @return new folder
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public static Folder createFolder(Folder mother, String name) throws ReadOnlyException, NodeException{
		return createFolder(mother, name, null);
	}

	/**
	 * Create a new folder
	 * @param mother mother folder (must not be null)
	 * @param name folder name
	 * @param channel channel to create a channel local folder. if null or channel is a master node, the folder will be created in the master node
	 * @return new folder
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public static Folder createFolder(Folder mother, String name, Node channel) throws ReadOnlyException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.createObject(Folder.class);

		folder.setMotherId(mother.getId());
		folder.setName(name);
		if (channel != null && channel.isChannel()) {
			folder.setChannelInfo(channel.getId(), folder.getChannelSetId());
		}
		folder.save();
		t.commit(false);

		return t.getObject(Folder.class, folder.getId());
	}

	/**
	 * Localize the folder in the channel
	 * @param folder folder
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	public static Folder localize(Folder folder, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder copy = (Folder)folder.copy();
		copy.setChannelInfo(channel.getId(), folder.getChannelSetId());
		copy.save();
		t.commit(false);
		return t.getObject(Folder.class, copy.getId());
	}

	/**
	 * Create a template
	 * @param folder
	 * @param name
	 * @return
	 * @throws NodeException
	 */
	public static Template createTemplate(Folder folder, String name) throws NodeException {
		return createTemplate(folder, name, null);
	}

	/**
	 * Create a template in a channel
	 * @param folder
	 * @param name
	 * @param channel
	 * @return
	 * @throws NodeException
	 */
	public static Template createTemplate(Folder folder, String name, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setSource("");
		template.setName(name);
		template.addFolder(folder);
		if (channel != null && channel.isChannel()) {
			template.setChannelInfo(channel.getId(), template.getChannelSetId());
		}
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a page
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @return the page
	 * @throws NodeException
	 */
	public static Page createPage(Folder folder, Template template, String name) throws NodeException {
		return createPage(folder, template, name, null);
	}

	/**
	 * Create a page
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @param channel channel, if the page shall be created in a chanmel, may be null
	 * @param language A language or null
	 * @return the page
	 * @throws NodeException
	 */
	public static Page createPage(Folder folder, Template template, String name, Node channel,
			ContentLanguage language) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);

		page.setFolderId(folder.getId());
		page.setTemplateId(template.getId());
		page.setName(name);
		page.setLanguage(language);
		if (channel != null && channel.isChannel()) {
			page.setChannelInfo(channel.getId(), page.getChannelSetId());
		}
		page.save();
		t.commit(false);

		return t.getObject(Page.class, page.getId());
	}

	/**
	 * Create a page
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @param channel channel, if the page shall be created in a chanmel, may be null
	 * @return the page
	 * @throws NodeException
	 */
	public static Page createPage(Folder folder, Template template, String name, Node channel) throws NodeException {
		return createPage(folder, template, name, channel, null);
	}

	/**
	 * Create a template and a page based on this template
	 * @param folder
	 * @param name
	 * @return
	 * @throws NodeException
	 */
	public static Page createTemplateAndPage(Folder folder, String name) throws NodeException {
		Template template = ContentNodeTestDataUtils.createTemplate(folder, name);
		Page page = ContentNodeTestDataUtils.createPage(folder, template, name);

		return page;
	}

	/**
	 * Localize the page in the channel
	 * @param page page
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	public static Page localize(Page page, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page copy = (Page)page.copy();
		copy.setChannelInfo(channel.getId(), page.getChannelSetId());
		copy.save();
		t.commit(false);
		return t.getObject(Page.class, copy.getId());
	}

	/**
	 * Create a file
	 * @param folder folder
	 * @param name name
	 * @param data binary data
	 * @return the file
	 * @throws NodeException
	 */
	public static File createFile(Folder folder, String name, byte[] data) throws NodeException {
		return createFile(folder, name, data, null);
	}

	/**
	 * Create an image
	 * @param folder folder
	 * @param name name
	 * @param data binary data
	 * @return the image
	 * @throws NodeException
	 */
	public static File createImage(Folder folder, String name, byte[] data) throws NodeException {
		return createImage(folder, name, data, null);
	}

	/**
	 * Create an image
	 * @param folder folder
	 * @param name name
	 * @param inputStream input stream
	 * @return the image
	 * @throws NodeException
	 */
	public static File createImage(Folder folder, String name, InputStream inputStream) throws NodeException {
		return createImage(folder, name, inputStream, null);
	}

	/**
	 * Create a file
	 * @param folder folder
	 * @param name name
	 * @param data binary data
	 * @param channel channel, if the file shall be create in a channel, may be null
	 * @return the file
	 * @throws NodeException
	 */
	public static File createFile(Folder folder, String name, byte[] data, Node channel) throws NodeException {
		return createFile(folder, name, new ByteArrayInputStream(data), channel);
	}

	/**
	 * Create a file
	 * @param folder folder
	 * @param name name
	 * @param data data as input stream
	 * @param channel channel, if the file shall be created in a channel, may be null
	 * @return the file
	 * @throws NodeException
	 */
	public static File createFile(Folder folder, String name, InputStream data, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		File file = t.createObject(File.class);

		file.setFolderId(folder.getId());
		file.setFileStream(data);
		file.setName(name);
		if (channel != null && channel.isChannel()) {
			file.setChannelInfo(channel.getId(), file.getChannelSetId());
		}
		file.save();
		t.commit(false);

		return t.getObject(File.class, file.getId());
	}

	/**
	 * Create an image
	 * @param folder folder
	 * @param name name
	 * @param data binary data
	 * @param channel channel, if the file shall be create in a channel, may be null
	 * @return the image
	 * @throws NodeException
	 */
	public static File createImage(Folder folder, String name, byte[] data, Node channel) throws NodeException {
		return createImage(folder, name, new ByteArrayInputStream(data), channel);
	}

	/**
	 * Create an image
	 * @param folder folder
	 * @param name name
	 * @param inputStream input stream
	 * @param channel channel, if the file shall be create in a channel, may be null
	 * @return the image
	 * @throws NodeException
	 */
	public static File createImage(Folder folder, String name, InputStream inputStream, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ImageFile file = t.createObject(ImageFile.class);

		file.setFolderId(folder.getId());
		file.setFileStream(inputStream);
		file.setName(name);
		if (channel != null && channel.isChannel()) {
			file.setChannelInfo(channel.getId(), file.getChannelSetId());
		}
		file.save();
		t.commit(false);

		return t.getObject(File.class, file.getId());
	}

	/**
	 * Create a node object in the given folder and channel
	 * @param objectType
	 * @param folder
	 * @param name
	 * @return
	 * @throws NodeException
	 * @throws Exception
	 */
	public static NodeObject createNodeObject(int objectType, Folder folder, String name, Node channel, boolean publish) throws NodeException, Exception {
		final NodeObject nodeObject;

		byte[] data= "File contents".getBytes();

		switch (objectType) {
		case Folder.TYPE_FOLDER:
			nodeObject = ContentNodeTestDataUtils.createFolder(folder, name, channel);
			break;
		case Template.TYPE_TEMPLATE:
			nodeObject = ContentNodeTestDataUtils.createTemplate(folder, name, channel);
			break;
		case Page.TYPE_PAGE:
			nodeObject = ContentNodeTestDataUtils.createPage(folder, ContentNodeTestDataUtils.createTemplate(folder, name, channel), name, channel);
			break;
		case File.TYPE_FILE:
			nodeObject = ContentNodeTestDataUtils.createFile(folder, name, data, channel);
			break;
		case ImageFile.TYPE_IMAGE:
			InputStream inputStream = GenericTestUtils.getPictureResource("blume.jpg");
			nodeObject = ContentNodeTestDataUtils.createImage(folder, name, IOUtils.toByteArray(inputStream), channel);
			break;
		default:
			Assert.fail("createNodeObject can't create an NodeObject for type " + objectType);
			return null;
		}

		Assert.assertNotNull("nodeObject should not be null", nodeObject);

		if (publish && objectType == Page.TYPE_PAGE) {
			Trx.operate(() -> {
				Page updateablePage = (Page)TransactionManager.getCurrentTransaction().getObject(nodeObject, true);
				updateablePage.publish();
				updateablePage.unlock();
			});
		}

		return nodeObject;
	}

	/**
	 * Create a node object in the given folder
	 * @param objectType
	 * @param folder
	 * @param name
	 * @return
	 * @throws NodeException
	 * @throws Exception
	 */
	public static NodeObject createNodeObject(int objectType, Folder folder, String name) throws NodeException, Exception {
		return createNodeObject(objectType, folder, name, null, false);
	}

	/**
	 * Localize the file in the channel
	 * @param file file
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	public static File localize(File file, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		File copy = (File)file.copy();
		copy.setChannelInfo(channel.getId(), file.getChannelSetId());
		copy.save();
		t.commit(false);
		return t.getObject(File.class, copy.getId());
	}

	/**
	 * Localize the image in the channel
	 * @param file file
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	public static ImageFile localize(ImageFile file, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ImageFile copy = (ImageFile)file.copy();
		copy.setChannelInfo(channel.getId(), file.getChannelSetId());
		copy.save();
		t.commit(false);
		return t.getObject(ImageFile.class, copy.getId());
	}

	/**
	 * Create a new user group
	 * @return user group
	 * @throws NodeException
	 */
	public static UserGroup createUserGroup(String name, int motherId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a new group
		List<Integer> groupIds = DBUtils.executeInsert("INSERT INTO usergroup (name, mother) VALUES (?, ?)", new Object[] { name, motherId });
		int newGroupId = groupIds.get(0);

		// duplicate the permissions
		DBUtils.executeUpdate("INSERT INTO perm SELECT ?, o_type, o_id, perm FROM perm WHERE usergroup_id = ?", new Object[] { newGroupId, motherId });

		// let the permission store refresh the permissions
		PermissionStore.getInstance().refreshGroup(newGroupId);

		return t.getObject(UserGroup.class, newGroupId);
	}

	/**
	 * Create a new user
	 * @param firstName first name
	 * @param lastName last name
	 * @param email email address
	 * @param login login
	 * @param password password
	 * @param groups groups of the user
	 * @return new user (not editable)
	 * @throws NodeException
	 */
	public static SystemUser createSystemUser(String firstName, String lastName, String email, String login, String password, List<UserGroup> groups) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser user = t.createObject(SystemUser.class);

		user.setActive(true);
		user.setFirstname(firstName);
		user.setLastname(lastName);
		user.setEmail(ObjectTransformer.getString(email, ""));
		user.setLogin(login);

		if (groups != null) {
			user.getUserGroups().addAll(groups);
		}

		user.save();

		int userId = ObjectTransformer.getInt(user.getId(), -1);

		user.setPassword(SystemUserFactory.hashPassword(password, userId));
		user.save();

		t.commit(false);

		return t.getObject(SystemUser.class, user.getId());
	}

	/**
	 * Create a new contentrepository
	 * @param mccr true for multichannelling
	 * @param name cr name
	 * @param url url
	 * @return contentrepository instance
	 * @throws NodeException
	 * @throws Exception
	 */
	public static ContentRepository createContentRepository(
			String name, boolean mccr, Boolean instant, String url) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);
		cr.setDbType("hsql");
		cr.setUrl(url);
		cr.setUsername("sa");
		cr.setPassword("");
		cr.setMultichannelling(mccr);
		cr.setInstantPublishing(instant);
		cr.save();
		t.commit(false);

		// Add some standard reserved tagmap entries
		addTagmapEntryAllTypes(cr, GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id", null, false,
				false, true, -1, null, null);
		addTagmapEntry(cr, Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "folder.name", "name", null, false,
				false, true, -1, null, null);
		addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.name", "name", null, false,
				false, true, -1, null, null);
		addTagmapEntry(cr, ContentFile.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "file.name", "name", null, false,
				false, true, -1, null, null);
		addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_INTEGER, "page.publishtimestamp", "publishtimestamp",
				null, false, false, true, -1, null, null);
		addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, null,
				"content", null, false, false, true, -1, null, null);
		addTagmapEntry(cr, ContentFile.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_BLOB, null,
				"binarycontent", null, false, false, true, -1, null, null);

		return t.getObject(ContentRepository.class, cr.getId());
	}

	/**
	 * Create a new contentrepository with a datsource
	 * @param mccr true for multichannelling
	 * @param name cr name
	 * @param url url
	 * @return contentrepository instance
	 * @throws NodeException
	 * @throws Exception
	 */
	public static ContentRepository createContentRepositoryWithDatsource(
			String name, boolean mccr, Boolean instant, Node node) throws NodeException{
		Transaction t = TransactionManager.getCurrentTransaction();

		Map<String, String> handleProperties = createDatasource(node, mccr);
		ContentRepository cr = createContentRepository(name, mccr, instant, handleProperties.get("url"));

		return cr;
	}

	/**
	 * Add a tagmap entry to the content repository
	 * @param cr content repository
	 * @param objectType type of the object the entry belongs to
	 * @param attributeType attribute type
	 * @param tagName tagname (what is rendered)
	 * @param mapName map name (name of the attribute in the CR)
	 * @param category category (may be null)
	 * @param multivalue true for multivalue
	 * @param filesystem true for filesystem attribute
	 * @param optimized true for optimized attribute
	 * @param targetType type of the link target (for link or foreignlink type)
	 * @param foreignLinkAttribute foreign link attribute (for foreignlink type)
	 * @param foreignLinkAttributeRule rule (for foreignlink type)
	 * @throws NodeException
	 * @throws Exception
	 */
	public static TagmapEntry addTagmapEntry(ContentRepository cr, int objectType, int attributeType,
			String tagName, String mapName, String category, boolean multivalue, boolean filesystem,
			boolean optimized, int targetType, String foreignLinkAttribute, String foreignLinkAttributeRule) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		cr = t.getObject(ContentRepository.class, cr.getId(), true);
		TagmapEntry entry = t.createObject(TagmapEntry.class);
		entry.setObject(objectType);
		entry.setAttributeTypeId(attributeType);
		entry.setTagname(tagName);
		entry.setMapname(mapName);
		entry.setCategory(category);
		entry.setMultivalue(multivalue);
		entry.setFilesystem(filesystem);
		entry.setOptimized(optimized);
		entry.setTargetType(targetType);
		entry.setForeignlinkAttribute(foreignLinkAttribute);
		entry.setForeignlinkAttributeRule(foreignLinkAttributeRule);
		cr.getEntries().add(entry);
		cr.save();
		t.commit(false);

		return t.getObject(TagmapEntry.class, entry.getId());
	}

	/**
	 * Add a tagmap entry to the content repository for all
	 * object types (folders, pages, files)
	 * @param cr content repository
	 * @param attributeType attribute type
	 * @param tagName tagname (what is rendered)
	 * @param mapName map name (name of the attribute in the CR)
	 * @param category category (may be null)
	 * @param multivalue true for multivalue
	 * @param filesystem true for filesystem attribute
	 * @param optimized true for optimized attribute
	 * @param targetType type of the link target (for link or foreignlink type)
	 * @param foreignLinkAttribute foreign link attribute (for foreignlink type)
	 * @param foreignLinkAttributeRule rule (for foreignlink type)
	 * @throws NodeException
	 * @throws Exception
	 */
	public static void addTagmapEntryAllTypes(ContentRepository cr, int attributeType, String tagName,
			String mapName, String category, boolean multivalue, boolean filesystem, boolean optimized,
			int targetType, String foreignLinkAttribute, String foreignLinkAttributeRule)
					throws NodeException {
		for (int objecttype : new int[]{  Folder.TYPE_FOLDER, Page.TYPE_PAGE,  ContentFile.TYPE_FILE}) {
			addTagmapEntry(cr, objecttype, attributeType, tagName, mapName, category,
				 multivalue, filesystem, optimized, targetType, foreignLinkAttribute, foreignLinkAttributeRule);
		}
	}

	/**
	 * Create a datasource for the given node
	 *
	 * @param node
	 *            node
	 * @param mccr true to create a mccr, false for a normal cr
	 * @return handle properties
	 * @throws Exception
	 */
	private static Map<String, String> createDatasource(Node node, boolean mccr) throws NodeException {
		Map<String, String> handleProps = new HashMap<String, String>();

		handleProps.put("type", "jdbc");
		handleProps.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProps.put("url", "jdbc:hsqldb:mem:" + node.getHostname() + TestEnvironment.getRandomHash(10) + ";shutdown=true");
		handleProps.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> dsProps = new HashMap<String, String>();

		dsProps.put("sanitycheck2", "true");
		dsProps.put("autorepair2", "true");

		com.gentics.api.lib.datasource.Datasource datasource = null;
		if (mccr) {
			datasource = PortalConnectorFactory.createMultichannellingDatasource(handleProps, dsProps);
		} else {
			datasource = PortalConnectorFactory.createDatasource(handleProps, dsProps);
		}

		assertNotNull("Check whether datasource was created", datasource);

		return handleProps;
	}

	/**
	 * Get the id of a node user (user in the Node group)
	 * @return id of the user
	 * @throws Exception
	 */
	public static int getNodeUserId() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		UserGroup nodeGroup = t.getObject(UserGroup.class, NODE_GROUP_ID);
		assertNotNull("Node group must exist", nodeGroup);
		List<SystemUser> nodeGroupMembers = nodeGroup.getMembers();
		assertFalse("List of members of node group must not be null", nodeGroupMembers.isEmpty());

		// start a new transaction with a member of the node group
		return ObjectTransformer.getInt(nodeGroupMembers.get(0).getId(), -1);
	}

	/**
	 * Does what the name says
	 * @param nodeObjectId
	 * @param responseCode
	 * @return
	 * @throws Exception
	 */
	public static Map<String, com.gentics.contentnode.rest.model.Tag> loadRestNodeObjectAndCheckIfTagExists(
			int objectType, Integer nodeObjectId, String tagName, boolean shouldExist) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = null;
		switch (objectType) {
		case Folder.TYPE_FOLDER:
			FolderResourceImpl folderResourceImpl = new FolderResourceImpl();
			folderResourceImpl.setTransaction(t);
			FolderLoadResponse folderLoadResponse = folderResourceImpl.load(nodeObjectId.toString(), false, false, false, 0, null);
			ContentNodeTestUtils.assertResponseCode(folderLoadResponse, ResponseCode.OK);
			restTags = folderLoadResponse.getFolder().getTags();
			break;
		case Template.TYPE_TEMPLATE:
			TemplateLoadResponse templateLoadResponse = getTemplateResource().load(nodeObjectId.toString(), 0);
			ContentNodeTestUtils.assertResponseCode(templateLoadResponse, ResponseCode.OK);
			restTags = templateLoadResponse.getTemplate().getObjectTags();
			break;
		case Page.TYPE_PAGE:
			PageResourceImpl pageResourceImpl = new PageResourceImpl();
			pageResourceImpl.setTransaction(t);
			PageLoadResponse pageLoadResponse = pageResourceImpl.load(nodeObjectId.toString(), false, false,
					false, false, false, false, false, false, false, false, 0, null);
			ContentNodeTestUtils.assertResponseCode(pageLoadResponse, ResponseCode.OK);
			restTags = pageLoadResponse.getPage().getTags();
			break;
		case File.TYPE_FILE:
			FileResourceImpl fileResourceImpl = new FileResourceImpl();
			fileResourceImpl.setTransaction(t);
			FileLoadResponse fileLoadResponse = fileResourceImpl.load(nodeObjectId.toString(), false, false, 0, null);
			ContentNodeTestUtils.assertResponseCode(fileLoadResponse, ResponseCode.OK);
			restTags = fileLoadResponse.getFile().getTags();
			break;
		case ImageFile.TYPE_IMAGE:
			ImageResourceImpl imageResourceImpl = new ImageResourceImpl();
			imageResourceImpl.setTransaction(t);
			ImageLoadResponse imageLoadResponse = imageResourceImpl.load(nodeObjectId.toString(), false, false, 0, null);
			ContentNodeTestUtils.assertResponseCode(imageLoadResponse, ResponseCode.OK);
			restTags = imageLoadResponse.getImage().getTags();
			break;
		}

		Assert.assertEquals("Check if the node object type " + objectType + " ID: " + nodeObjectId
				+ " contains the tag " + tagName, shouldExist, restTags.containsKey(tagName));

		return restTags;
	}

	/**
	 * Saves the passed rest tags to the node object
	 * and checks the response code
	 * @param objectType
	 * @param nodeObjectId
	 * @param restTags
	 * @param shouldFail
	 * @param reponseCode
	 * @return
	 * @throws Exception
	 */
	public static GenericResponse saveRestNodeObjectPropertyTagsAndAssert(int objectType, Integer nodeObjectId,
			Map<String, com.gentics.contentnode.rest.model.Tag> restTags, ResponseCode reponseCode) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		GenericResponse genericResponse = null;
		switch (objectType) {
		case Folder.TYPE_FOLDER:
			FolderResourceImpl folderResourceImpl = new FolderResourceImpl();
			folderResourceImpl.setTransaction(t);
			FolderSaveRequest folderSaveRequest = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder restFolder = new com.gentics.contentnode.rest.model.Folder();
			restFolder.setTags(restTags);
			folderSaveRequest.setFolder(restFolder);
			genericResponse = folderResourceImpl.save(nodeObjectId.toString(), folderSaveRequest);
			break;
		case Template.TYPE_TEMPLATE:
			TemplateSaveRequest templateSaveRequest = new TemplateSaveRequest();
			com.gentics.contentnode.rest.model.Template restTemplate = new com.gentics.contentnode.rest.model.Template();
			restTemplate.setObjectTags(restTags);
			templateSaveRequest.setTemplate(restTemplate);
			genericResponse = getTemplateResource().update(nodeObjectId.toString(), templateSaveRequest);
			break;
		case Page.TYPE_PAGE:
			PageResourceImpl pageResourceImpl = new PageResourceImpl();
			pageResourceImpl.setTransaction(t);
			PageSaveRequest pageSaveRequest = new PageSaveRequest();
			com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
			restPage.setTags(restTags);
			pageSaveRequest.setPage(restPage);
			genericResponse = pageResourceImpl.save(nodeObjectId.toString(), pageSaveRequest);
			break;
		case File.TYPE_FILE:
			FileResourceImpl fileResourceImpl = new FileResourceImpl();
			fileResourceImpl.setTransaction(t);
			FileSaveRequest fileSaveRequest = new FileSaveRequest();
			com.gentics.contentnode.rest.model.File restFile = new com.gentics.contentnode.rest.model.File();
			restFile.setTags(restTags);
			fileSaveRequest.setFile(restFile);
			genericResponse = fileResourceImpl.save(nodeObjectId, fileSaveRequest);
			break;
		case ImageFile.TYPE_IMAGE:
			ImageResourceImpl imageResourceImpl = new ImageResourceImpl();
			imageResourceImpl.setTransaction(t);
			ImageSaveRequest imageSaveRequest = new ImageSaveRequest();
			com.gentics.contentnode.rest.model.Image restImage = new com.gentics.contentnode.rest.model.Image();
			restImage.setTags(restTags);
			imageSaveRequest.setImage(restImage);
			genericResponse = imageResourceImpl.save(nodeObjectId, imageSaveRequest);
			break;
		}

		ContentNodeTestUtils.assertResponseCode(genericResponse, reponseCode);

		return genericResponse;
	}

	/**
	 * Possible publish targets
	 */
	public static enum PublishTarget {

		/**
		 * Node is neither published into filesystem, nor into contentrepository (only publish table)
		 */
		NONE(false, false),

		/**
		 * Node is published into filesystem
		 */
		FILESYSTEM(true, false),

		/**
		 * Node is published into contentrepository
		 */
		CONTENTREPOSITORY(false, true),

		/**
		 * Node is published into filesystem and contentrepository
		 */
		BOTH(true, true);

		/**
		 * Publish into filesystem
		 */
		private boolean fs = false;

		/**
		 * Publish into contentrepository
		 */
		private boolean cr = false;

		/**
		 * Create an instance
		 * @param fs filesystem
		 * @param cr contentrepository
		 */
		PublishTarget(boolean fs, boolean cr) {
			this.fs = fs;
			this.cr = cr;
		}

		/**
		 * Return true if the node publishes into the filesystem
		 *
		 * @return true for publishing into filesystem
		 */
		public boolean isPublishFS() {
			return fs;
		}

		/**
		 * Return true if the node publishes into the contentrepository
		 *
		 * @return true for publishing into contentrepository
		 */
		public boolean isPublishCR() {
			return cr;
		}
	}

	/**
	 * Localize page
	 *
	 * @param channel
	 * @param page
	 * @return
	 * @throws Exception
	 */
	public static Page localizePage(Node channel, Page page) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page localizedPage = (Page) page.copy();

		localizedPage.setChannelInfo(channel.getId(), page.getChannelSetId());
		localizedPage.save();
		t.commit(false);

		localizedPage.publish();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedPage;
	}

	/**
	 * Localize image
	 *
	 * @param channel
	 * @param image
	 * @return
	 * @throws Exception
	 */
	public static ImageFile localizeImage(Node channel, ImageFile image) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ImageFile localizedImage = (ImageFile) image.copy();

		localizedImage.setChannelInfo(channel.getId(), image.getChannelSetId());
		localizedImage.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedImage;
	}

	/**
	 * Localize file
	 *
	 * @param channel
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static File localizeFile(Node channel, File file) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		File localizedFile = (File) file.copy();

		localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
		localizedFile.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedFile;
	}

	/**
	 * Localize a folder
	 * @param channel
	 * @param folder inherited folder in the channel. Can be master folder or subchannel folder
	 * @return
	 * @throws Exception
	 */
	public static Folder localizeFolder(Node channel, Folder folder) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder localizedFolder = (Folder) folder.copy();

		localizedFolder.setChannelInfo(channel.getId(), folder.getChannelSetId());
		localizedFolder.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedFolder;
	}

	/**
	 * Assert that the given operation result has a localized message with given message key and (optional) parameters
	 * @param result result to check
	 * @param messageKey message key
	 * @param parameters optional list of parameters
	 * @throws Exception
	 */
	public static void assertResultMessage(OpResult result, String messageKey, String... parameters) throws Exception {
		assertEquals("Check # of messages in the result", 1, result.getMessages().size());
		CNI18nString message = new CNI18nString(messageKey);
		for (String p : parameters) {
			message.addParameter(p);
		}

		assertEquals("Check result message", message.toString(), result.getMessages().get(0).getMessage());
	}

	/**
	 * Creates a new session and a session ID for the given user name
	 * @param username The name of the user
	 * @return A session or null if the session couldn't be created
	 * @throws NodeException
	 */
	public static Session createSession(String username) throws NodeException {
		Session session = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			SystemUser systemUser = ((SystemUserFactory) t.getObjectFactory(SystemUser.class))
					.getSystemUser(username, null, false);
			session = new Session(systemUser, "", "ContentNodeTestDataUtils", "secret", 0);
			trx.success();
		}

		return session;
	}

	/**
	 * Creates a new session and a session ID for the user "system"
	 * @return A session or null if the session couldn't be created
	 * @throws NodeException
	 */
	public static Session createSession() throws NodeException{
		return createSession("system");
	}

	/**
	 * Copy the given node (using NodeCopy) and return the copy. This method assumes a running transaction.
	 * @param node node to copy
	 * @param copyPages true to copy pages
	 * @param copyTemplates true to copy templates
	 * @param copyFiles true to copy files
	 * @return copy of the node
	 * @throws NodeException
	 */
	public static Node copy(Node node, boolean copyPages, boolean copyTemplates, boolean copyFiles) throws NodeException {
		// get node IDs before copy
		Set<Integer> nodeIdsBeforeCopy = DBUtils.select("SELECT id FROM node", DBUtils.IDS);

		// copy the node
		NodeCopyQueueEntry entry = new NodeCopyQueueEntry();
		entry.setIdParameter(node.getId().toString());
		entry.setParameter("copyPages", Boolean.toString(copyPages));
		entry.setParameter("copyTemplates", Boolean.toString(copyTemplates));
		entry.setParameter("copyFiles", Boolean.toString(copyFiles));
		entry.invoke();

		// get new node
		Set<Integer> nodeIdsAfterCopy = DBUtils.select("SELECT id FROM node", DBUtils.IDS);
		nodeIdsAfterCopy.removeAll(nodeIdsBeforeCopy);
		assertThat(nodeIdsAfterCopy).as("New node IDs").hasSize(1);
		return TransactionManager.getCurrentTransaction().getObject(Node.class, nodeIdsAfterCopy.iterator().next());
	}

	/**
	 * Add the given publish handler to the cr
	 * @param cr contentrepository
	 * @param handlerClass publish handler implementation class
	 * @param properties optional properties (key1, value1, key2, value2, ...)
	 * @return id of the DB entry
	 * @throws Exception
	 */
	public static int addPublishHandler(ContentRepository cr, Class<? extends CnMapPublishHandler> handlerClass, String...properties)
			throws Exception {
		List<Integer> ids = new ArrayList<>();
		StringWriter writer = new StringWriter();

		if (!ObjectTransformer.isEmpty(properties)) {
			Properties props = new Properties();
			String key = null;
			for (String property : properties) {
				if (key == null) {
					key = property;
				} else {
					props.setProperty(key, property);
					key = null;
				}
			}
			props.list(new PrintWriter(writer));
		}
		String propsString = writer.toString();
		DBUtils.executeStatement("INSERT INTO cr_publish_handler (name, contentrepository_id, javaclass, properties) VALUES (?, ?, ?, ?)",
				Transaction.INSERT_STATEMENT, stmt -> {
					stmt.setString(1, handlerClass.getSimpleName());
					stmt.setInt(2, cr.getId());
					stmt.setString(3, handlerClass.getName());
					stmt.setString(4, propsString);
				}, null, stmt -> {
					ResultSet keys = stmt.getGeneratedKeys();
					while (keys.next()) {
						ids.add(keys.getInt(1));
					}
				});

		return ids.get(0);
	}

	/**
	 * Udpate the given object. This method can be called with or without a currently running transaction.
	 * If called without transaction a new transaction is created and committed for updating.
	 * If called with a transaction, the transaction is committed, but not closed.
	 * If the given object is a published page, the page will be published after modification
	 * @param object object to update
	 * @param updateHandler update handler
	 * @return updated object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T update(T object, NodeObjectHandler<T> updateHandler) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t == null) {
			try (Trx trx = new Trx()) {
				T editedObject = trx.getTransaction().getObject(object, true);
				updateHandler.handle(editedObject);
				editedObject.save();

				if (editedObject instanceof Page) {
					if (((Page) editedObject).isOnline()) {
						((Page) editedObject).publish();
					}
				}

				object = trx.getTransaction().getObject(object);
				trx.success();
			}
		} else {
			T editedObject = t.getObject(object, true);
			updateHandler.handle(editedObject);
			editedObject.save();

			if (editedObject instanceof Page) {
				if (((Page) editedObject).isOnline()) {
					((Page) editedObject).publish();
				}
			}

			object = t.getObject(object);
			t.commit(false);
		}

		return object;
	}

	/**
	 * Create a new object of given class. This method can be called with or without a currently running transaction.
	 * If called without transaction the object is created in a new (temporary) transaction.
	 * If called with a transaction, the transaction is committed, but not closed.
	 * @param clazz object class
	 * @param createHandler create handler
	 * @return created object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T create(Class<T> clazz, NodeObjectHandler<T> createHandler) throws NodeException {
		return create(clazz, createHandler, true);
	}

	/**
	 * Create a new object of given class. This method can be called with or without a currently running transaction.
	 * If called without transaction the object is created in a new (temporary) transaction.
	 * If called with a transaction, the transaction is committed, but not closed.
	 * @param clazz object class
	 * @param createHandler create handler
	 * @param save true if the object shall be saved, false if not
	 * @return created object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T create(Class<T> clazz, NodeObjectHandler<T> createHandler, boolean save) throws NodeException {
		T createdObject = null;
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t == null) {
			try (Trx trx = new Trx()) {
				createdObject = trx.getTransaction().createObject(clazz);
				createHandler.handle(createdObject);
				if (save) {
					createdObject.save();
					createdObject = trx.getTransaction().getObject(createdObject);
				}
				trx.success();
			}
		} else {
			createdObject = t.createObject(clazz);
			createHandler.handle(createdObject);
			if (save) {
				createdObject.save();
				createdObject = t.getObject(createdObject);
				t.commit(false);
			}
		}

		return createdObject;
	}

	/**
	 * Create a body part for MultiPart
	 * We dont use FormDataBodyPart(String name, String value) in here because
	 * it doesn't seem to set the parameters properties which is used by the Rest APi
	 * @param headers Example: form-data; name="folderId"
	 * @param value The value for this part
	 * @param mediaType A media type
	 * @return A body part
	 * @throws ParseException
	 */
	public static BodyPart createformDataBodyPart(String headers, String value, MediaType mediaType)
			throws ParseException {
		// We set a new content disposition because the FormDataBodyPart constructor
		// doesn't see to set the body part parameters, which the Rest endpoint needs.
		BodyPart bodyPart = new FormDataBodyPart(new FormDataContentDisposition(headers), value);

		if (mediaType != null) {
			bodyPart.setMediaType(mediaType);
		}

		return bodyPart;
	}

	/**
	 * Create a body part for MultiPart
	 * We dont use FormDataBodyPart(String name, String value) in here because
	 * it doesn't seem to set the parameters properties which is used by the Rest APi
	 * @param headers Example: form-data; name="folderId"
	 * @param value The value for this part
	 * @param mediaType A media type
	 * @return A body part
	 * @throws ParseException
	 */
	public static BodyPart createformDataBodyPart(String headers, Object value, MediaType mediaType)
			throws ParseException {
		// We set a new content disposition because the FormDataBodyPart constructor
		// doesn't see to set the body part parameters, which the Rest endpoint needs.
		return new FormDataBodyPart(new FormDataContentDisposition(headers), value, mediaType);
	}

	/**
	 * Creates a MultiPart object for FileResource.create
	 * A session transaction (with sid) must be available.
	 * You must call the cleanup() method on the returned object yourself.
	 * @param name The file name
	 * @param folderId The folder ID
	 * @param nodeId The node ID
	 * @param description A description
	 * @param overwrite Whether to overwrite an existing file in the same folder, can be null for unspecified
	 * @param fileBinaryData The file contents
	 * @return A multi part object for FileResource.create
	 * @throws ParseException
	 * @throws TransactionException
	 */
	public static MultiPart createRestFileUploadMultiPart(
			String name, Integer folderId, Integer nodeId, String description, Boolean overwrite,
			String data) throws ParseException, TransactionException {

		Transaction t = TransactionManager.getCurrentTransaction();

		@SuppressWarnings("resource")
		MultiPart multiPart = new MultiPart()
			.bodyPart(createformDataBodyPart("form-data; name=\"folderId\"", folderId.toString(), null))
			.bodyPart(createformDataBodyPart("form-data; name=\"nodeId\"", nodeId.toString(), null))
			.bodyPart(createformDataBodyPart("form-data; name=\"" + SessionToken.SESSION_ID_QUERY_PARAM_NAME + "\"",
					t.getSessionId(), null))
			.bodyPart(createformDataBodyPart("form-data; name=\"" + SessionToken.SESSION_SECRET_COOKIE_NAME + "\"",
					t.getSession().getSessionSecret(), null))
			.bodyPart(createformDataBodyPart("form-data; name=\"fileName\"", name, null))
			.bodyPart(createformDataBodyPart("form-data; name=\"description\"", description, null))
			.bodyPart(createformDataBodyPart("form-data; name=\"fileBinaryData\"; filename=\"" + name + "\"",
					data, MediaType.APPLICATION_OCTET_STREAM_TYPE));

		if (overwrite != null) {
			multiPart.bodyPart(createformDataBodyPart("form-data; name=\"overwrite\"", (overwrite ? "true" : "false"), null));
		}

		// some dummy value to make the FileResourceImpl happy
		multiPart.getHeaders().add("content-length", "0");

		return multiPart;
	}

	/**
	 * Unlock the given object
	 * @param object object to unlock
	 * @return unlocked object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T unlock(T object) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t == null) {
			try (Trx trx = new Trx()) {
				T editedObject = trx.getTransaction().getObject(object, true);
				editedObject.unlock();

				object = object.reload();
				trx.success();
			}
		} else {
			T editedObject = t.getObject(object, true);
			editedObject.unlock();

			object = object.reload();
			t.commit(false);
		}

		return object;
	}

	/**
	 * Get language with given code (null if not found)
	 * @param code language code
	 * @return language or null
	 * @throws NodeException
	 */
	public static ContentLanguage getLanguage(String code) throws NodeException {
		return Trx.supply(t -> {
			int id = DBUtils.select("SELECT id FROM contentgroup WHERE code = ?", st -> st.setString(1, code), rs -> {
				if (rs.next()) {
					return rs.getInt("id");
				} else {
					return 0;
				}
			});
			return t.getObject(ContentLanguage.class, id);
		});
	}

	/**
	 * Get the IDs of the systemusers in the system and node group
	 * @return set of user IDs
	 * @throws NodeException
	 */
	public static Set<Integer> getSystemUsers() throws NodeException {
		return Trx.supply(() -> {
			return DBUtils.select("SELECT systemuser.id FROM user_group LEFT JOIN systemuser ON user_group.user_id = systemuser.id WHERE usergroup_id IN (?, ?) AND systemuser.id IS NOT NULL", ps -> {
				ps.setInt(1, SYSTEM_GROUP_ID);
				ps.setInt(2, NODE_GROUP_ID);
			}, DBUtils.IDS);
		});
	}

	/**
	 * Delete all but the given system users
	 * @param systemUserIds IDs of users which shall not be removed
	 * @throws NodeException
	 */
	public static void cleanUsers(Set<Integer> systemUserIds) throws NodeException {
		Trx.operate(() -> {
			String qm = StringUtils.repeat("?", systemUserIds.size(), ",");
			Object[] params = (Object[]) systemUserIds.toArray(new Object[systemUserIds.size()]);
			DBUtils.executeUpdate("DELETE FROM user_group WHERE user_id NOT IN (" + qm + ")", params);
			DBUtils.executeUpdate("DELETE FROM systemuser WHERE id NOT IN (" + qm + ")", params);
		});
	}

	/**
	 * Delete all but the system groups
	 * @throws NodeException
	 */
	public static void cleanGroups() throws NodeException {
		// delete all groups (but 1, 2)
		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM user_group WHERE usergroup_id NOT IN (?, ?)", new Object[] { SYSTEM_GROUP_ID, NODE_GROUP_ID });
			DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id NOT IN (?, ?)", new Object[] { SYSTEM_GROUP_ID, NODE_GROUP_ID });
			DBUtils.executeUpdate("DELETE FROM usergroup WHERE id NOT IN (?, ?)", new Object[] { SYSTEM_GROUP_ID, NODE_GROUP_ID });
			PermissionStore.initialize(true);
		});
	}

	/**
	 * Clear the node by deleting all contained folders, pages, images and files.
	 * This method must be called within a transaction
	 * @param node node to clear
	 * @throws NodeException
	 */
	public static void clear(Node node) throws NodeException {
		clear(node.getFolder());
	}

	/**
	 * Clear the folder by deleting all contained folders, pages, images, files and forms.
	 * This method must be called within a transaction
	 * @param folder folder to clear
	 * @throws NodeException
	 */
	public static void clear(Folder folder) throws NodeException {
		try (InstantPublishingTrx iTrx = new InstantPublishingTrx(false)) {
			for (Folder f : folder.getChildFolders()) {
				f.delete(true);
			}
			for (Page p : folder.getPages()) {
				p.delete(true);
			}
			for (File f : folder.getFilesAndImages()) {
				f.delete(true);
			}
			for (Form f : folder.getForms(new Folder.FormSearch().setWastebin(Wastebin.INCLUDE))) {
				f.delete(true);
			}
		}
	}
}
