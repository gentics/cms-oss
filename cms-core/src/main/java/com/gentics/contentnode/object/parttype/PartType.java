/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: PartType.java,v 1.15 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.lib.resolving.ResolvableMapWrappable;

/**
 * The PartType is a special templaterenderer, which can render a {@link Value}.
 * PartTypes are created using the PartTypeFactory.
 *
 * A parttype contains a reference to the value to be rendered. Therefore
 * the parttype implementation can cache depending on the value. As parttypes
 * are instantiated for each value, the implementation should be very
 * memory-efficient.
 *
 * TODO check, if a special get(RenderType,key) method is needed (for .url,.. props)
 */
public interface PartType extends Resolvable, TemplateRenderer, TransformablePartType, ResolvableMapWrappable {

	/**
	 * Set of all Part types that are convertible to each other because their value is of type text
	 */
	public static final Set<Integer> TEXTTYPE_PARTS = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(new Integer[]{ 1, 2, 3, 9, 10, 15, 16, 17, 21})));

	/**
	 * set the value which will be used by the parttype.
	 * @param value the new value object.
	 * TODO maybe remove this from the interface
	 * to implement custom renderable parttypes use @see com.gentics.api.contentnode.parttype.ExtensiblePartType for now.
	 * @throws NodeException
	 */
	void setValue(Value value) throws NodeException;

	/**
	 * get the current value which has been set to the parttype.
	 * @return the current value, or null if not set.
	 */
	Value getValueObject();

	/**
	 * Get the HTML id used for aloha editables
	 * @return the HTML id used for aloha
	 */
	String getAlohaid();

	/**
	 * check, if the value needs a template to be rendered.
	 * @return true, if the parttype requests a template, else false.
	 * @throws NodeException
	 */
	boolean hasTemplate() throws NodeException;

	/**
	 * Dirt the caches of sub objects
	 * @throws NodeException
	 */
	void dirtCache() throws NodeException;

	/**
	 * Return true when values of this parttype can be edited with the live editor
	 * @return true for liveeditor, false if not
	 * @throws NodeException
	 */
	boolean isLiveEditorCapable() throws NodeException;
    
	/**
	 * Returns true when the parttype is mandatory but not filled in.
	 * @return true when the parttype is mandatory but not filled in.
	 * @throws NodeException
	 */
	boolean isMandatoryAndNotFilledIn() throws NodeException;

	/**
	 * Copy the given parttype over this object
	 * @param original given object, must be of the same class
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	<T extends PartType> void copyFrom(T original) throws ReadOnlyException, NodeException;

	/**
	 * Get the effective udate for parttype specific data, or -1 if no parttype specific data exists
	 * @return udate
	 * @throws NodeException
	 */
	int getEffectiveUdate() throws NodeException;

	/**
	 * Set the annotation class
	 * @param annotationClass annotation class
	 */
	void setAnnotationClass(String annotationClass);

	/**
	 * Get the annotation class for annotating editables in aloha editor
	 * @return annotation class for the parttype
	 */
	String getAnnotationClass();

	/**
	 * Validates the value of this PartType.
	 *
	 * If this PartType holds text data, validation will involve testing this
	 * data against the regular expression (if any) that has been configured to
	 * constrain the range of possible values.
	 *
	 * @return True if the value held by this PartType is valid.
	 */
	boolean validate() throws NodeException;

	/**
	 * Validates the value of this PartType.
	 *
	 * @param regexConfig
	 *            Globally configured regular expression with which to validate
	 *            TextPartType. PartTypes other than TextPartType ignore this
	 *            parameter.
	 *
	 * @see com.gentics.contentnode.object.parttype.PartType#validate()
	 */
	boolean validate(Map<?, ?> regexConfig) throws NodeException;

	/**
	 * Do PartType specific saving for a value, that has to be done before the
	 * value is saved. Implementations of this method possibly modify the value
	 * object, and they cannot rely on the value being already stored (and
	 * having an ID)
	 * @param batchUpdater optional batch updater
	 * 
	 * @return true if something was changed, false if not
	 * @throws NodeException
	 */
	boolean preSave(BatchUpdater batchUpdater) throws NodeException;

	/**
	 * Do PartType specific saving for a value, that has to be done after the
	 * value is saved. Implementations of this method should not modify the
	 * value object (because changes would not be persisted), but they can rely
	 * on the value being already stored (and having an ID)
	 * @param batchUpdater optional batch updater
	 * 
	 * @return true if something was changed, false if not
	 * @throws NodeException
	 */
	boolean postSave(BatchUpdater batchUpdater) throws NodeException;

	/**
	 * Do PartType specific deletion
	 * @throws NodeException
	 */
	void delete() throws NodeException;

	/**
	 * Check whether the value has the same content as the value of the other parttype
	 * @param other other parttype
	 * @return true iff the values have the same content.
	 * @throws NodeException
	 */
	boolean hasSameContent(PartType other) throws NodeException;
}
