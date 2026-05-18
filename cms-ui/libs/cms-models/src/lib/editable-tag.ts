import { ObjectTag, Tag, TagType } from './tag';

/** Additional information required when editing a `Tag`. */
export interface TagEditInfo {
    /**
     * The TagType, of which this tag is an instance.
     * This property is not part of the REST model and only used internally by the TagEditor.
     *
     * The property `construct` was added to the REST model, after the new TagEditor was created, which required the `tagType` property.
     * Thus since the entire TagEditor is based on the use of the `tagType` property, it remains in use.
     *
     * @deprecated Use the {@link Tag.construct `construct`} property instead.
     * This property will be removed in the next major version.
     */
    tagType: TagType;
}

/** An extension of `Tag` with properties needed by the TagEditor. */
export interface EditableTag extends Tag, TagEditInfo { }

/** An extension of `ObjectTag` with properties needed by the TagEditor. */
export interface EditableObjectTag extends ObjectTag, TagEditInfo { }
