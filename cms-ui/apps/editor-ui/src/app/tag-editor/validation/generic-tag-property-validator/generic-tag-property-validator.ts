import {
    CmsFormTagPartProperty,
    DataSourceTagPartProperty,
    FileTagPartProperty,
    FolderTagPartProperty,
    FormTagPartProperty,
    ImageTagPartProperty,
    ListTagPartProperty,
    NodeTagPartProperty,
    OrderedUnorderedListTagPartProperty,
    PageTagTagPartProperty,
    SelectTagPartProperty,
    TagPart,
    TagPartProperty,
    TagPropertyType,
    TagPropertyValidator,
    TemplateTagTagPartProperty,
    ValidationResult
} from '@gentics/cms-models';

/**
 * Used to store a list of property names of a type.
 */
class PropertyList<T extends TagPartProperty> {
    constructor(public readonly properties: (keyof T)[]) {}
}

const PROPS_TO_VALIDATE = new Map<TagPropertyType, PropertyList<any>>();
PROPS_TO_VALIDATE.set(TagPropertyType.DATASOURCE, new PropertyList<DataSourceTagPartProperty>(['options']));
PROPS_TO_VALIDATE.set(TagPropertyType.FILE, new PropertyList<FileTagPartProperty>(['fileId']));
PROPS_TO_VALIDATE.set(TagPropertyType.FOLDER, new PropertyList<FolderTagPartProperty>(['folderId']));
PROPS_TO_VALIDATE.set(TagPropertyType.IMAGE, new PropertyList<ImageTagPartProperty>(['imageId']));
PROPS_TO_VALIDATE.set(TagPropertyType.LIST, new PropertyList<ListTagPartProperty>(['stringValues']));
PROPS_TO_VALIDATE.set(TagPropertyType.NODE, new PropertyList<NodeTagPartProperty>(['nodeId']));
PROPS_TO_VALIDATE.set(TagPropertyType.ORDEREDLIST, new PropertyList<OrderedUnorderedListTagPartProperty>(['stringValues']));
PROPS_TO_VALIDATE.set(TagPropertyType.UNORDEREDLIST, new PropertyList<OrderedUnorderedListTagPartProperty>(['stringValues']));
PROPS_TO_VALIDATE.set(TagPropertyType.PAGETAG, new PropertyList<PageTagTagPartProperty>(['pageId', 'contentTagId']));
PROPS_TO_VALIDATE.set(TagPropertyType.SELECT, new PropertyList<SelectTagPartProperty>(['selectedOptions']));
PROPS_TO_VALIDATE.set(TagPropertyType.MULTISELECT, new PropertyList<SelectTagPartProperty>(['selectedOptions']));
PROPS_TO_VALIDATE.set(TagPropertyType.TEMPLATETAG, new PropertyList<TemplateTagTagPartProperty>(['templateId', 'templateTagId']));
PROPS_TO_VALIDATE.set(TagPropertyType.FORM, new PropertyList<FormTagPartProperty>(['formId']));
PROPS_TO_VALIDATE.set(TagPropertyType.CMSFORM, new PropertyList<CmsFormTagPartProperty>(['formId']));

/**
 * This is a validator that is used as a fallback, if no specific validator is registered for a TagPart.
 *
 * If the TagPropertyType has some (JavaScript Object) properties configured that need to be checked, the validator will
 * check if all these properties are set and return `isSet: allSet, success: allSet || !tagPart.mandatory}`.
 *
 * If nothing is configured for a TagPropertyType, the validator will return `{isSet: true, success: true}`
 * because it cannot know which (JavaScript Object) property contains the value of the TagProperty.
 */
export class GenericTagPropertyValidator implements TagPropertyValidator<TagPartProperty> {

    validate(editedProperty: TagPartProperty, tagPart: TagPart): ValidationResult {
        const propertiesToValidate = PROPS_TO_VALIDATE.get(editedProperty.type) || null;

        if (propertiesToValidate) {
            const allSet = this.checkIfPropertiesAreSet(propertiesToValidate, editedProperty);
            return {
                isSet: allSet,
                success: allSet || !tagPart.mandatory
            };
        } else {
            return {
                isSet: true,
                success: true
            };
        }
    }

    /**
     * Checks if all the specified properties are set on the tagProperty.
     * A property p is set, if !tagProperty[p] is true and
     * if the property is an array, it has length > 0.
     */
    private checkIfPropertiesAreSet(propertiesToValidate: PropertyList<any>, tagProperty: TagPartProperty): boolean {
        for (let key of propertiesToValidate.properties) {
            const value = tagProperty[key];
            if (!value || (Array.isArray(value) && value.length === 0)) {
                return false;
            }
        }
        return true;
    }

}
