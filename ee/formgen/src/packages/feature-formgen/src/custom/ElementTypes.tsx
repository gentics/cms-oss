import { IElementTypes, IElementType } from '../types/interfaces';
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
export const elementTypes: IElementTypes = {
    ...FormGridElementTypes,
    boolean: Elements.BooleanElement as unknown as IElementType,
    string: Elements.InputElement as unknown as IElementType,
    number: Elements.InputElement as unknown as IElementType,
    property: Elements.InputElement as unknown as IElementType,
    catalog: Elements.DropDownCatalogElement as unknown as IElementType,
    datetime: Elements.DateTimeElement as unknown as IElementType,
    reference: Elements.DropDownReferenceElement as unknown as IElementType,
    time: Elements.DateTimeElement as unknown as IElementType,
    binary: Elements.BinaryElement as unknown as IElementType,
    aggregate: Elements.AggregateElement as unknown as IElementType,
    autocomplete: Elements.AutoCompleteElement as unknown as IElementType,
    lazycatalog: Elements.LazyCatalogElement as unknown as IElementType,
    label: Elements.LabelElement as unknown as IElementType, //TODO ?
    
};