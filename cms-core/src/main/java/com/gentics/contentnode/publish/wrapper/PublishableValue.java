package com.gentics.contentnode.publish.wrapper;

import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.lib.etc.StringUtils;

/**
 * Wrapper for the REST Model of a property.
 * Instances of this class will be used when versioned publishing is active and multithreaded publishing is used.
 * See {@link PublishablePage} for details.
 */
public class PublishableValue extends Value {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6246591429841501678L;

	/**
	 * Key of the 2nd level cache for parts
	 */
	protected final static String PART = "part";

	/**
	 * Container of the value
	 */
	protected ValueContainer container;

	/**
	 * Wrapped property
	 */
	protected com.gentics.contentnode.rest.model.Property property;

	/**
	 * Create an instance
	 * @param container container
	 * @param property wrapped property
	 */
	protected PublishableValue(ValueContainer container, com.gentics.contentnode.rest.model.Property property) {
		super(property.getId(), null);
		this.container = container;
		this.property = property;
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		if (info == null) {
			info = container.getObjectInfo().getSubInfo(Value.class);
		}
		return info;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copy()
	 */
	public NodeObject copy() throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public int getInfo() {
		switch (property.getType()) {
		case PAGETAG:
			return ObjectTransformer.getInt(property.getPageId(), 0);
		case TEMPLATETAG:
			return ObjectTransformer.getInt(property.getTemplateId(), 0);
		case PAGE:
			return (ObjectTransformer.getInt(property.getPageId(), 0) > 0) ? 1 : 0;
		case FILE:
			return (ObjectTransformer.getInt(property.getFileId(), 0) > 0) ? 1 : 0;
		case IMAGE:
			return (ObjectTransformer.getInt(property.getImageId(), 0) > 0) ? 1 : 0;
		case FOLDER:
			return (ObjectTransformer.getInt(property.getFolderId(), 0) > 0) ? 1 : 0;
		case LIST:
			return ObjectTransformer.getBoolean(property.getBooleanValue(), false) ? 1 : 0;
		case CMSFORM:
			return (ObjectTransformer.getInt(property.getFormId(), 0) > 0) ? 1 : 0;
		default:
			return 0;
		}
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public String getValueText() {
		switch (property.getType()) {
		case PAGETAG:
			if (ObjectTransformer.getInt(property.getContentTagId(), 0) > 0) {
				return "p";
			} else if (ObjectTransformer.getInt(property.getTemplateTagId(), 0) > 0) {
				return "t";
			} else {
				return property.getStringValue();
			}
		case BOOLEAN:
			return property.getBooleanValue() ? "1" : "0";
		case OVERVIEW:
			Overview overview = property.getOverview();
			return overview != null ? overview.getSource() : "";
		case SELECT:
		{
			List<SelectOption> selectedOptions = property.getSelectedOptions();
			
			if (!ObjectTransformer.isEmpty(selectedOptions)) {
				return selectedOptions.get(0).getId().toString();
			} else {
				return "";
			}
		}
		case MULTISELECT:
		{
			List<SelectOption> selectedOptions = property.getSelectedOptions();
			StringBuilder valueText = new StringBuilder();

			if (selectedOptions != null) {
				for (SelectOption option : selectedOptions) {
					if (valueText.length() > 0) {
						valueText.append("|-|");
					}

					if (option.getId() == -1) {
						// Siteminder datasource options are saved by value, not by ID
						valueText.append(option.getValue());
					} else {
						valueText.append(option.getId());
					}
				}
			}
			return valueText.toString();
		}
		case LIST:
		case ORDEREDLIST:
		case UNORDEREDLIST:
			List<String> values = property.getStringValues();
			return StringUtils.merge((String[]) values.toArray(new String[values.size()]), "\n");
		case PAGE:
			if (property.getPageId() != null) {
				return property.getNodeId() != null ? Integer.toString(property.getNodeId()) : "";
			} else {
				return property.getStringValue();
			}
		case FILE:
			if (property.getFileId() != null) {
				return property.getNodeId() != null ? Integer.toString(property.getNodeId()) : "";
			} else {
				return property.getStringValue();
			}
		case IMAGE:
			if (property.getImageId() != null) {
				return property.getNodeId() != null ? Integer.toString(property.getNodeId()) : "";
			} else {
				return property.getStringValue();
			}
		case FOLDER:
			if (property.getFolderId() != null) {
				return property.getNodeId() != null ? Integer.toString(property.getNodeId()) : "";
			} else {
				return property.getStringValue();
			}
		case CMSFORM:
			if (property.getFormId() != null) {
				return "";
			} else {
				return property.getStringValue();
			}
		default:
			return property.getStringValue();
		}
	}

	@Override
	public int getValueRef() {
		switch (property.getType()) {
		case PAGETAG:
			int contentTagId = ObjectTransformer.getInt(property.getContentTagId(), 0);
			int templateTagId = ObjectTransformer.getInt(property.getTemplateTagId(), 0);
			if (contentTagId > 0) {
				return contentTagId;
			} else if (templateTagId > 0) {
				return templateTagId;
			} else {
				return 0;
			}
		case TEMPLATETAG:
			return ObjectTransformer.getInt(property.getTemplateTagId(), 0);
		case PAGE:
			return ObjectTransformer.getInt(property.getPageId(), 0);
		case FILE:
			return ObjectTransformer.getInt(property.getFileId(), 0);
		case IMAGE:
			return ObjectTransformer.getInt(property.getImageId(), 0);
		case FOLDER:
			return ObjectTransformer.getInt(property.getFolderId(), 0);
		case NODE:
			return ObjectTransformer.getInt(property.getNodeId(), 0);
		case SELECT:
		case MULTISELECT:
		case DATASOURCE:
			return null == property.getDatasourceId() ? 0 : property.getDatasourceId();
		case CMSFORM:
			return ObjectTransformer.getInt(property.getFormId(), 0);
		default:
			return 0;
		}
	}

	@Override
	public Part getPart(boolean checkForNull) throws NodeException {
		// use level2 cache here
		Transaction t = TransactionManager.getCurrentTransaction();
		Part part = (Part) t.getFromLevel2Cache(this, PART);

		if (part == null) {
			Integer partId = getPartId();

			// if the part is loaded without checking for null values, warning messages
			// concerning missing parts will not be sent to the logs, as these may cause
			// massive log flooding if tag parts have been removed from constructs
			part = TransactionManager.getCurrentTransaction().getObject(Part.class, partId, false, true, checkForNull);
			if (checkForNull) {
				// check for data consistency
				assertNodeObjectNotNull(part, partId, "Part");
			}
			t.putIntoLevel2Cache(this, PART, part);
		}
		return part;            
	}

	@Override
	public Integer getPartId() {
		return property.getPartId();
	}

	@Override
	public ValueContainer getContainer() throws NodeException {
		return container;
	}

	@Override
	protected void performDelete() throws NodeException {
		failReadOnly();
	}
}
