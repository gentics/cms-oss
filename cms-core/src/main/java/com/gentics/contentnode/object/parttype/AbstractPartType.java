/*
 * @author norbert
 * @date 03.01.2007
 * @version $Id: AbstractPartType.java,v 1.8 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.ClassHelper;

/**
 * Abstract implementation of a parttype.
 */
public abstract class AbstractPartType implements PartType, Resolvable, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6722670603860929626L;

	/**
	 * Prefix for the aloha editable ids
	 */
	public static final String ALOHA_EDITABLE_ID_PREFIX = "GENTICS_EDITABLE_";
    
	/**
	 * value of the part
	 */
	private Value value;

	/**
	 * Annotation class name
	 */
	private String annotationClass;

	/**
	 * logger
	 */
	protected transient NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Create an instance
	 * @param value value
	 * @throws NodeException
	 */
	public AbstractPartType(Value value) throws NodeException {
		setValue(value);
	}

	@Override
	public Set<String> getResolvableKeys() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#setValue(com.gentics.contentnode.object.Value)
	 */
	public void setValue(Value value) throws NodeException {
		this.value = value;
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#getValue()
	 */
	public Value getValueObject() {
		return value;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#dirtCache()
	 */
	public void dirtCache() throws NodeException {// by default do nothing
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isLiveEditorCapable()
	 */
	public boolean isLiveEditorCapable() throws NodeException {
		// per default not liveeditor capable
		return false;
	}

	/**
	 * Returns true if the parttype is required
	 * @return true if the parttype is required
	 */
	public boolean isRequired() throws NodeException {
		return getValueObject().getPart().isRequired();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#getAlohaId()
	 */
	public String getAlohaid() {
		return ALOHA_EDITABLE_ID_PREFIX + this.getValueObject().getId();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#copyFrom(com.gentics.contentnode.object.parttype.PartType)
	 */
	public <T extends PartType> void copyFrom(T original) throws ReadOnlyException, NodeException {
		if (value == null) {
			throw new NodeException("Cannot copy from over this parttype, value is null");
		}
		NodeObjectInfo info = value.getObjectInfo();

		if (info == null || !info.isEditable()) {
			throw new ReadOnlyException("Object instance {" + value + "} is readonly and cannot be modified");
		}

		// given object must be of appropriate class
		if (!getClass().isAssignableFrom(original.getClass())) {
			throw new NodeException("Cannot copy {" + original + "} over {" + this + "}");
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// default implementation returns -1
		return -1;
	}

	public void setAnnotationClass(String annotationClass) {
		this.annotationClass = annotationClass;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#getAnnotationClass()
	 */
	public String getAnnotationClass() {
		return annotationClass;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#validate()
	 */
	public boolean validate() throws NodeException {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#validate()
	 */
	public boolean validate(Map<?, ?> regexConfig) throws NodeException {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#preSave()
	 */
	public boolean preSave() throws NodeException {
		// default implementation does nothing
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#postSave()
	 */
	public boolean postSave() throws NodeException {
		// default implementation does nothing
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#delete()
	 */
	public void delete() throws NodeException {
		// default implementation does nothing
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(com.gentics.contentnode.render.RenderResult renderResult, String template) throws NodeException {
		// if an editable value instance is set, we set it again, which will refresh data stored in the
		// PartType implementation from the value data (which may change at any time)
		if (value != null && value.getObjectInfo().isEditable()) {
			setValue(value);
		}
		return null;
	}


	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		// simply call the getter on the object
		try {
			return ClassHelper.invokeGetter(this, key);
		} catch (Exception e) {
			logger.error("Error while getting " + key + " from " + this, e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	@Override
	final public Property toProperty() throws NodeException {
		if (value == null) {
			throw new NodeException("Cannot transform null-value into property");
		}
		Property property = new Property();
		property.setId(value.getId());
		if (value.getGlobalId() != null) {
			property.setGlobalId(value.getGlobalId().toString());
		}
		property.setPartId(value.getPartId());
		property.setType(getPropertyType());

		fillProperty(property);
		return property;
	}

	/**
	 * PartType specific implementation of filling value data into the property
	 * @param property property to change
	 * @throws NodeException
	 */
	protected abstract void fillProperty(Property property) throws NodeException;
}
