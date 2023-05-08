package com.gentics.contentnode.publish.mesh;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

/**
 * Mesh publisher for publishing overviews as fields of a micronode
 */
public class MeshOverviewPublisher {
	/**
	 * String Array containing the overview list types
	 */
	protected final static String[] OVERVIEW_LIST_TYPES = Arrays.asList(ListType.values()).stream().map(ListType::toString).collect(Collectors.toList())
			.toArray(new String[0]);

	/**
	 * String Array containing the overview select types
	 */
	protected final static String[] OVERVIEW_SELECT_TYPES = Arrays.asList(SelectType.values()).stream().map(SelectType::toString).collect(Collectors.toList())
			.toArray(new String[0]);

	/**
	 * String Array containing the overview order directions
	 */
	protected final static String[] OVERVIEW_ORDER_DIRS = Arrays.asList(OrderDirection.values()).stream().map(OrderDirection::toString)
			.collect(Collectors.toList()).toArray(new String[0]);

	/**
	 * String Array containing the overview order by values
	 */
	protected final static String[] OVERVIEW_ORDER_BYS = Arrays.asList(OrderBy.values()).stream().map(OrderBy::toString).collect(Collectors.toList())
			.toArray(new String[0]);

	/**
	 * Mesh publisher
	 */
	protected MeshPublisher publisher;

	/**
	 * Base field name (will be used as prefix for fields)
	 */
	protected String baseName;

	/**
	 * Create an instance
	 * @param publisher mesh publisher
	 * @param baseName base field name
	 */
	public MeshOverviewPublisher(MeshPublisher publisher, String baseName) {
		this.publisher = publisher;
		this.baseName = baseName;
	}

	/**
	 * Add the field schemas to the given list
	 * @param fieldSchemas list of field schemas, which will be modified
	 */
	public void addFieldSchemas(List<FieldSchema> fieldSchemas) {
		fieldSchemas.add(new StringFieldSchemaImpl().setAllowedValues(OVERVIEW_LIST_TYPES).setName(name(MicronodeField.LISTTYPE)));
		fieldSchemas.add(new StringFieldSchemaImpl().setAllowedValues(OVERVIEW_SELECT_TYPES).setName(name(MicronodeField.SELECTTYPE)));
		fieldSchemas.add(new StringFieldSchemaImpl().setAllowedValues(OVERVIEW_ORDER_DIRS).setName(name(MicronodeField.ORDERDIR)));
		fieldSchemas.add(new StringFieldSchemaImpl().setAllowedValues(OVERVIEW_ORDER_BYS).setName(name(MicronodeField.ORDERBY)));
		fieldSchemas.add(new ListFieldSchemaImpl().setListType(FieldTypes.NODE.toString()).setAllowedSchemas(publisher.getSchemaName(File.TYPE_FILE),
				publisher.getSchemaName(Folder.TYPE_FOLDER), publisher.getSchemaName(Page.TYPE_PAGE)).setName(name(MicronodeField.ITEMS)));
		fieldSchemas.add(new ListFieldSchemaImpl().setListType(FieldTypes.NUMBER.toString()).setName(name(MicronodeField.NODEIDS)));
		fieldSchemas.add(new StringFieldSchemaImpl().setName(name(MicronodeField.SOURCE)));
		fieldSchemas.add(new BooleanFieldSchemaImpl().setName(name(MicronodeField.RECURSIVE)));
		fieldSchemas.add(new NumberFieldSchemaImpl().setName(name(MicronodeField.MAXITEMS)));
	}

	/**
	 * Add the fields to the given list
	 * @param nodeId node ID
	 * @param fieldList list of pairs fieldname/field, which will be modified
	 * @param overview overview to transform
	 * @param postponeHandler postpone handler
	 * @throws NodeException
	 */
	public void addFields(int nodeId, List<Pair<String, Field>> fieldList, Overview overview, Operator postponeHandler) throws NodeException {
		com.gentics.contentnode.rest.model.Overview restOverview = ModelBuilder.getOverview(overview);
		fieldList.add(ImmutablePair.of(name(MicronodeField.LISTTYPE), new StringFieldImpl().setString(restOverview.getListType().toString())));
		fieldList.add(ImmutablePair.of(name(MicronodeField.SELECTTYPE), new StringFieldImpl().setString(restOverview.getSelectType().toString())));
		fieldList.add(ImmutablePair.of(name(MicronodeField.ORDERDIR), new StringFieldImpl().setString(restOverview.getOrderDirection().toString())));
		fieldList.add(ImmutablePair.of(name(MicronodeField.ORDERBY), new StringFieldImpl().setString(restOverview.getOrderBy().toString())));

		NodeFieldListImpl items = new NodeFieldListImpl();
		fieldList.add(ImmutablePair.of(name(MicronodeField.ITEMS), items));

		NumberFieldListImpl nodeIds = new NumberFieldListImpl();
		fieldList.add(ImmutablePair.of(name(MicronodeField.NODEIDS), nodeIds));

		for (OverviewEntry entry : overview.getOverviewEntries()) {
			String entryMeshUuid = MeshPublisher.getMeshUuid(entry.getObject());
			Optional<MeshProject> optionalProject = publisher.getProject(entry.getObject());
			if (optionalProject.isPresent() && publisher.existsInMesh(nodeId, optionalProject.get(), entry.getObject())) {
				items.add(new NodeFieldListItemImpl().setUuid(entryMeshUuid));
				nodeIds.add(entry.getNodeId());
			} else {
				postponeHandler.operate();
			}
		}

		fieldList.add(ImmutablePair.of(name(MicronodeField.SOURCE), new StringFieldImpl().setString(restOverview.getSource())));
		fieldList.add(ImmutablePair.of(name(MicronodeField.RECURSIVE), new BooleanFieldImpl().setValue(restOverview.isRecursive())));
		fieldList.add(ImmutablePair.of(name(MicronodeField.MAXITEMS), new NumberFieldImpl().setNumber(restOverview.getMaxItems())));
	}

	/**
	 * Get the name of the given field
	 * @param field field
	 * @return name
	 */
	protected String name(MicronodeField field) {
		return String.format("%s_%s", baseName, field.suffix);
	}

	/**
	 * Enumeration of micronode fields used for the overview
	 */
	protected static enum MicronodeField {
		LISTTYPE("listType"), SELECTTYPE("selectType"), ORDERDIR("orderDir"), ORDERBY("orderBy"), ITEMS("items"), NODEIDS("nodeIds"), SOURCE(
				"source"), RECURSIVE("recursive"), MAXITEMS("maxItems");

		/**
		 * Field suffix
		 */
		String suffix;

		/**
		 * Create instance
		 * @param suffix field suffix
		 */
		MicronodeField(String suffix) {
			this.suffix = suffix;
		}
	}
}
