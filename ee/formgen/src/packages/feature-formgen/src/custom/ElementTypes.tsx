import { IElementTypes, IElementType, IPluginElements } from '../types/interfaces';
import * as Elements from './ElementDefinitions';

/*
* !IMPORTANT! it is case sensitiv, keys should always be lowercase 
*/
export const FormGridElementTypes: IElementTypes = {
    formgridimage: Elements.FormGridImageElement as unknown as IElementType,
    formgridtext: Elements.FormGridTextElement as unknown as IElementType,
    formgridspacer: Elements.FormGridTextElement as unknown as IElementType,
    text: Elements.TextElement as unknown as IElementType,
};

/*
* !IMPORTANT! it is case sensitiv, keys should always be lowercase 
*/
const anElementTypes: IElementTypes = {
    ...FormGridElementTypes,
    boolean: Elements.ANBooleanElement as unknown as IElementType,
    string: Elements.ANInputElement as unknown as IElementType,
    number: Elements.ANInputElement as unknown as IElementType,
    property: Elements.ANInputElement as unknown as IElementType,
    catalog: Elements.ANDropDownCatalogElement as unknown as IElementType,
    reference: Elements.ANDropDownReferenceElement as unknown as IElementType,
    aggregate: Elements.ANAggregateElement as unknown as IElementType,
    autocomplete: Elements.ANAutocompleteInputElement as unknown as IElementType,
    lazycatalog: Elements.ANDropDownLazyCatalogElement as unknown as IElementType,
    label: Elements.LabelElement as unknown as IElementType, //TODO ?  
};

export const cmsInputElementTypes: IElementTypes = {
    string:   Elements.InputElement,
    number:   Elements.InputElement,
    email:    Elements.InputElement,
    textarea: Elements.InputElement,
};

/*
* !IMPORTANT! it is case sensitiv, keys should always be lowercase
*/
const sharedElementTypes: IElementTypes = {
    ...FormGridElementTypes,           // shared
    ...cmsInputElementTypes,
    boolean: Elements.BooleanElement,
    catalog: Elements.CatalogElement,
    datetime: Elements.DateTimeElement as unknown as IElementType,
    time: Elements.DateTimeElement as unknown as IElementType,
    binary: Elements.BinaryElement as unknown as IElementType,
    // text: TextElement,                 // shared
    // label: LabelElement,               // shared
    // No: aggregate, autocomplete, lazycatalog, reference (not in CMS Forms)
};

/*
* !IMPORTANT! it is case sensitiv, keys should always be lowercase 
*/
const cmsElementTypes: IElementTypes = {
    ...FormGridElementTypes,           // shared
    container: Elements.CmsContainerElement,
    conditionalContainer: Elements.CmsContainerElement,
    adminmail: Elements.CatalogElement
};

/*
* !IMPORTANT! it is case sensitiv, keys should always be lowercase 
*/
const pluginElementTypes: IPluginElements = {
    "shared": sharedElementTypes,
    "acta-nova": anElementTypes,
    "forms": cmsElementTypes
}

export const getElementTypeByPlugin = (elementType: string, pluginName: string) => {
    let sharedElementTypes = pluginElementTypes['shared'];
    let specificElementTypes = pluginElementTypes[pluginName] || {};
    // Der specific type wird bevorzugt
    return specificElementTypes[elementType] || sharedElementTypes[elementType];
}