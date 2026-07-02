import {
  IData,
  ISchema,
  IFeatures,
  ISchemaFields,
  IDataFields,
  IElementType,
  IFeaturesConfig,
  IFormElement,
  ITranslationSet,
  PossibleValueSet,
  FormState,
  IDataFieldProperties,
  ISchemaFieldProperties,
  UIState,
  IPossibleValue,
  IValidation,
  IFormPage,
  IErrorMessage,
  IDataAggregationListInitialEntry,
  IDataAggregationListInitialEntryValue,
  IForm,
  IFirstErrorElementInfo,
  IFormGridOptions,
  CaptchaConfig,
  RawCaptchaConfig,
  FriendlyCaptchaMode,
  reCaptchaMode,
  ICmsCondition,
} from '../types/interfaces';
import JsonPath from 'jsonpath';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { RestService } from '../services/RestService';
import { updateOnChange, afterChange } from './FormEventsUtils';
import * as overlayUtils from './OverlayUtils';
import { FormGridElementTypes } from '../custom/ElementTypes';
import { Platform } from 'react-native';
import 'react-native-get-random-values';
import { v4 as uuidv4 } from 'uuid';
import { PluginNameEnum } from '../constants/enums/PluginNameEnum';

export function isFeatureActive(
  featureName: string,
  stateFeatures: IFeatures,
): boolean {
  return Boolean(stateFeatures?.hasOwnProperty(featureName));
}

export function getFeatureActive(
  featureName: string,
  stateFeatures: IFeatures,
): IFeaturesConfig {
  return stateFeatures?.[featureName] ?? {};
}

export function getCorrectANCulture(lng: string, features: IFeatures): string {
    // If full culture is already provided (e.g. "de-AT"), just return it
    if (lng && lng.length > 2) return lng;

    // Otherwise, resolve the best matching locale from feature_languages
    let locales: string[] = [];
    const lngFeature = getFeatureActive('feature_languages', features);

    if (lngFeature?.languages && Array.isArray(lngFeature.languages)) {
      locales = lngFeature.languages as string[];
    } else {
      // sensible defaults
      locales = ['en-AT', 'de-AT'];
    }

    let localeLng: string | null = null;
    for (const locale of locales) {
      if (locale.startsWith(lng)) {
        localeLng = locale;
      }
    }

    if (localeLng == null) {
      console.warn(
        'There is no locale in the list of available languages (' +
          locales.toString() +
          ') for the current language (' +
          lng +
          ')',
      );
      localeLng = lng; // fallback to short language
    }

    return localeLng;
  }

export function getI18nTextStr(
  culture: string,
  elem?: ITranslationSet,
): string {
  if (!elem) return '';
  if (!culture) culture = 'de-CH';
  if (elem[culture]) return elem[culture];
  if (culture.length > 2) culture = culture.substring(0, 2);
  if (elem[culture]) return elem[culture];
  // Fallback: find first key that starts with the base language (e.g. 'de' → 'de-AT')
  const fallbackKey = Object.keys(elem).find((k) => k.startsWith(culture + '-'));
  return (fallbackKey ? elem[fallbackKey] : '') || '';
}

export function getI18nText(state: FormState, elem: ITranslationSet): string {
  return getI18nTextStr(state.culture, elem);
}

export function isFormGridType(ElemTyp: IElementType): boolean {
  return Object.values(FormGridElementTypes).includes(ElemTyp);
}

/*
 * return the number of pages a form has
 * return -1 if a form is of type report (Suchformulare)
 * progress for Suchformulare is not displayed because they are based on one page
 */
export function getFormPagesLengthForDisplay(formState: FormState): number {
  if (formState.schema.formType === 'report') return -1;
  return formState.form.pages?.length ?? 0;
}

/**
 * Get an entry in form data
 *
 * @param data formdata
 * @param jsonpath jsonpath for element to be retrieved
 * @param id property id for element to be retrieved - preferable this is used as it is faster
 * @returns
 */
export function getElementByJsonPath(
  data: IData,
  jsonpath: string,
  id?: string,
): IDataFieldProperties | null {
  // optimize performance by first trying to get element by id
  if (id && jsonpath.indexOf('.values') === -1) {
    const elementFromDataById = data.properties?.[id];
    if (elementFromDataById) {
      return elementFromDataById;
    }
  }
  if (jsonpath != null && jsonpath !== '') {
    let res = JsonPath.value(data, jsonpath);
    if (res) {
      return res;
    }
  }
  return null;
}

/**
 * get the Element from data by id. - this is quick
 * if unsuccessful, use jsonpath - this is slow
 *
 */
export function getElementByIdOrJsonPath(
  data: IData,
  jsonpath: string,
  id?: string,
) {
  if (id && jsonpath && jsonpath.indexOf('.value') === -1) {
    return data.properties?.[id];
  }
  return getElementByJsonPath(data, jsonpath);
}

export function getVisible(formState: FormState, jsonpath: string) {
  const elementState = getElementByJsonPath(formState.data, jsonpath);
  if (elementState != null && typeof elementState.visible !== 'undefined') {
    return elementState.visible;
  }
  //@TODO check default visibility
  return true;
}

/**
 * Evaluates a CMS field/container condition against the current form data.
 * A condition is met when the referenced control's value equals the expected value.
 * Shared by ElementRenderer, validation and the visibility computation for the request payload.
 */
export function evaluateCmsCondition(
  formState: FormState,
  condition: ICmsCondition,
): boolean {
  const referencedValue =
    formState.data.properties?.[condition.source.control]?.value;

  // Katalog-/Auswahlfelder speichern den Wert als IPossibleValue
  // ({ identifier, displayName }). condition.equals ist aber immer ein einfacher
  // Identifier (String/Zahl). Deshalb vergleichen wir in diesem Fall gegen den
  // identifier des Objekts.
  const isPossibleValue = (value: unknown): value is IPossibleValue =>
    value !== null &&
    typeof value === 'object' &&
    'identifier' in value &&
    'displayName' in value;

  const comparableValue = isPossibleValue(referencedValue)
    ? referencedValue.identifier
    : referencedValue;

  return comparableValue === condition.equals;
}

/**
 * Recursively walks the uiSchema elements and writes the effective visibility state of each
 * CMS field into data.properties[id].visible. A field is visible only when its own condition is
 * met (and it isn't hidden) AND every ancestor container is visible.
 * Container elements are not part of the data structure, so no flag is written for them.
 */
function applyCmsVisibilityToElements(
  elements: IFormElement[] | undefined,
  parentVisible: boolean,
  data: IData,
  formState: FormState,
): void {
  if (!elements) return;
  for (const obj of elements) {
    // A container ("container"/"conditionalcontainer") is part of the uiSchema but NOT of
    // data.properties, so it gets no flag of its own. Instead it gates its children: if the
    // container is hidden by its condition, every field inside it must also count as invisible.
    // We pass the combined visibility (parent AND container condition) down and recurse, so
    // nested containers are handled correctly.
    if (['container', 'conditionalcontainer'].includes(obj.type)) {
      const containerVisible =
        parentVisible &&
        (obj.formGridOptions?.condition
          ? evaluateCmsCondition(formState, obj.formGridOptions.condition)
          : true);
      applyCmsVisibilityToElements(
        obj.elements,
        containerVisible,
        data,
        formState,
      );
      continue;
    }

    // Only real input fields have a value in data.properties. Layout-only elements
    // (text/spacer) and elements without a formGrid type are skipped - they are never validated.
    const type = obj.formGridOptions?.type;
    if (!type || type === 'formgridText' || type === 'formgridSpacer') {
      continue;
    }

    // Determine the field's own visibility:
    // - hidden === true   -> always invisible
    // - has a condition   -> visible only when the condition is met
    // - otherwise         -> visible by default
    const fieldVisible =
      obj.formGridOptions?.hidden === true
        ? false
        : obj.formGridOptions?.condition
        ? evaluateCmsCondition(formState, obj.formGridOptions.condition)
        : true;
    // Effective visibility = field is visible AND all of its ancestor containers are visible.
    const effectiveVisible = parentVisible && fieldVisible;

    // Write the flag back explicitly (true AND false). The plugin skips validation for
    // visible === false; visible fields must keep visible === true so they are not accidentally
    // suppressed. Preserve any existing property data (value, ...) and only set/overwrite visible.
    if (data.properties == null) {
      data.properties = {};
    }
    const existing = data.properties[obj.id];
    data.properties[obj.id] = {
      ...(existing ?? { value: '' }),
      visible: effectiveVisible,
    };
  }
}

/**
 * Sets the effective `visible` flag on every CMS field in `data.properties`, so the Forms plugin
 * can skip validating fields the user cannot see. No-op for non-CMS forms (ActaNova sets `visible`
 * itself). Operates on the passed-in `data` object - callers must pass a clone, not the React state.
 */
export function applyCmsVisibility(
  data: IData,
  form: IForm,
  pluginName: PluginNameEnum,
  formState: FormState,
): void {
  if (pluginName !== PluginNameEnum.CMS_FORMS || !form.pages) return;
  for (const page of form.pages) {
    applyCmsVisibilityToElements(page.elements, true, data, formState);
  }
}

export function getCreatable(formState: FormState, jsonpath: string) {
  const elementState = getElementByJsonPath(formState.data, jsonpath);
  if (elementState != null && typeof elementState.createable !== 'undefined') {
    return elementState.createable;
  }
  return true;
}

export function hashCode(s: string) {
  var h = 0,
    l = s.length,
    i = 0;
  if (l > 0) while (i < l) h = ((h << 5) - h + s.charCodeAt(i++)) | 0;
  return h;
}

//Aggregat Delete
export function listDeleteElement(
  formState: FormState,
  parentJsonPath: string,
  index: number,
  updateState: (state: StatesToUpdateEnum) => void,
) {
  console.log('listDeleteElement');
  let values: any[] = getValue(formState, parentJsonPath);

  if (!Array.isArray(values)) {
    console.warn(
      `listDeleteElement: Erwartetes Array, aber erhalten: ${typeof values}`,
    );
    return;
  }

  values.splice(index, 1);
  setValue(formState, parentJsonPath, values);
  updateState(StatesToUpdateEnum.ALL);
}

export function listAddElement(
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  parentId: string,
  jsonpath: string,
  createAfterIndex?: number,
) {
  console.log('listAddElement');

  // Aggregations-Daten und Schema abrufen
  const aggregationState = getElementByJsonPath(formState.data, jsonpath);
  const aggregationSchema = getSchemaForElement(formState.schema, parentId);

  if (!aggregationState || !aggregationSchema.properties) {
    console.warn('Aggregation oder Schema nicht gefunden:', parentId, jsonpath);
    return;
  }

  let values: any[] = getValue(formState, jsonpath) || [];
  if (!Array.isArray(values)) {
    values = [];
  }
  
  const standardEntry: IDataAggregationListInitialEntry = {
    deletable: true,
    editable: true,
    id: uuidv4(),
    name: "New Aggregation Element",
    sortIndex: 0,
    visible: true,
    value: {}
  };
  
  let newValueEntry: IDataAggregationListInitialEntry;
  
  const insertionIndex = typeof createAfterIndex === 'number' ? createAfterIndex + 1 : values.length;

  if (values.length > 0) {
    const firstEntry = values[0];
    newValueEntry = {
      deletable: firstEntry.deletable !== undefined ? firstEntry.deletable : standardEntry.deletable,
      id: uuidv4(),
      visible: firstEntry.visible !== undefined ? firstEntry.visible : standardEntry.visible,
      editable: firstEntry.editable !== undefined ? firstEntry.editable : standardEntry.editable,
      sortIndex: insertionIndex,
      name: firstEntry.name ? firstEntry.name : standardEntry.name,
      value: {}
    };
    
    values.forEach((item) => {
      if (item.sortIndex !== undefined && item.sortIndex >= insertionIndex) {
        item.sortIndex = item.sortIndex + 1;
      }
    });
  } else {
    newValueEntry = standardEntry;
  }

  Object.keys(aggregationSchema.properties).forEach((schemaFieldID) => {
    const schemaField: ISchemaFieldProperties =
      aggregationSchema.properties![schemaFieldID];
    const isVisible =
      aggregationState?.possibleAggregateValues?.[schemaFieldID]?.visible ??
      true;
    const newValueField: IDataAggregationListInitialEntryValue = {
      visible: isVisible,
      mandatory: schemaField.mandatory,
      name: schemaField.name,
      type: schemaField.type,
      updateOnChange: schemaField.updateOnChange,
      validation: schemaField.validation,
    };
    // Falls `possibleAggregateValues` existieren, Standardwerte übernehmen
    const possibleAggregationValue =
      aggregationState.possibleAggregateValues?.[schemaFieldID];
    if (possibleAggregationValue) {
      if (possibleAggregationValue.defaultValue) {
        newValueField.value = possibleAggregationValue.defaultValue;
      }
      if (possibleAggregationValue.possibleValues) {
        newValueField.possibleValues = possibleAggregationValue.possibleValues;
      }
      if (possibleAggregationValue.possibleValueSetID) {
        newValueField.possibleValueSetID =
          possibleAggregationValue.possibleValueSetID;
      }
      if (possibleAggregationValue.renderingType) {
        newValueField.renderingType = possibleAggregationValue.renderingType;
      }
    }
    newValueEntry.value[schemaFieldID] = newValueField;
  });

  values.splice(insertionIndex, 0, newValueEntry);
// der letzte Parameter ist essentiellel, das löst das Problem in folgender situation: 
// Wenn man vom Hauptformular zurück zum Hauptformular navigiert, dann kann sein,
//  dass der Value innerhalb vom formstate nicht geändert wird, obwohl jegliche Änderung auf newValueEntry Auswirkung auf formstate haben soll. Reusltat: Values wird aktualisiert aber value nicht!
// Die Lösung ist, dass man da explizit sagt, dass value auch geändert werden soll, selbst wen value ein typ von array ist.
  setValue(formState, jsonpath, values, true);
  let customData = JSON.parse(JSON.stringify(formState.data));
  updateOnChange(
    formState,
    uiState,
    restService,
    jsonpath,
    true,
    customData,
    updateState,
  );

  updateState(StatesToUpdateEnum.FormState);
}

export function getValue(
  formState: FormState,
  jsonpath: string,
  id?: string,
  elemState?: ReturnType<typeof getElementByIdOrJsonPath>,
) {
  // Allow callers to pass an already resolved elemState to avoid a redundant
  // getElementByIdOrJsonPath lookup; fall back to resolving it when not provided.
  elemState =
    elemState !== undefined
      ? elemState
      : getElementByIdOrJsonPath(formState.data, jsonpath, id);
  if (elemState) {
    if (elemState.values) {
      return elemState.values;  
    }
    // Do not use if(elemState.value) because 0 and false are falsy values 
    if (elemState.value !== undefined && elemState.value !== null) {
      return elemState.value;
    }
  }

  //@TODO check default Value
  return '';
}

export function setNormalizedSchema(
  formState: FormState,
  normSchema: ISchema,
  updateState: (state: StatesToUpdateEnum) => void,
) {
  const normalizedSchemas = flattenProperties(normSchema.properties);
  formState.normalizedSchema = normalizedSchemas;
  updateState(StatesToUpdateEnum.FormState);
}

function flattenProperties(schemas: ISchemaFields | undefined): ISchemaFields {
  if (!schemas) {
    return {};
  } else {
    return Object.keys(schemas)
      .map((key) => schemas[key])
      .reduce(
        (prev, subSchema) => ({
          ...prev,
          ...flattenProperties(subSchema.properties),
        }),
        schemas,
      );
  }
}

function findSchemaElement(
  object: ISchemaFields | undefined,
  key: string,
): ISchemaFieldProperties | undefined {
  let value;
  if (typeof object !== 'undefined') {
    if (object[key]) {
      return object[key];
    }
    Object.keys(object).some(function (k) {
      if (k === key) {
        value = object[k];
        return true;
      }
      if (object[k].properties) {
        value = findSchemaElement(object[k].properties, key);
        return value !== undefined;
      }
      return false;
    });
  }
  return value;
}

export function getSchemaForElement(
  schema: ISchema,
  id: string,
): ISchemaFieldProperties {
  if (schema.properties != null) {
    const value = findSchemaElement(schema.properties, id);
    if (typeof value !== 'undefined') {
      return value;
    }
  }
  //@TODO does it make sense to return a empty element if no state is available - maybe fix this so a state is always there for all elements!
  /*state.properties[id] = {value: "", visible: false, updateOnChange: false, type: "property"};
  return state.properties[id];
  */
  return { type: 'property' };
}

export function isEditable(
  formState: FormState,
  uiState: UIState,
  jsonpath: string,
  id?: string,
  parentId?: string,
): boolean {
  //While displaying summary, all fields are not editable
  if (uiState.currentPage === uiState.lastPage) return false;
  if (formState.anRoundTrip) return false;
  //if a parentId is specified (when subelements are rendered for an aggregate), we check the parentElements editable attribute.
  //if this is false, the child element should also be not editable - the childs editable attribute is ignored in that case.
  if (parentId) {
    let parentElementState = getElementByIdOrJsonPath(
      formState.data,
      parentId,
      parentId,
    );
    if (parentElementState && parentElementState.editable === false) {
      return false;
    }
  }
  const elementState = getElementByIdOrJsonPath(formState.data, jsonpath, id);
  if (elementState && typeof elementState.editable !== 'undefined') {
    return elementState.editable;
  }
  //@TODO check default Value
  return true;
}

export function isRequired(state: FormState, id: string): boolean {
  const elementSchema = getSchemaForElement(state.schema, id);
  if (elementSchema?.mandatory) {
    return true;
  }
  return false;
}

export function getError(formState: FormState, jsonpath: string, id?: string) {
  const elementState = getElementByIdOrJsonPath(formState.data, jsonpath, id);
  if (
    elementState?.validationMessage &&
    elementState.validationMessage !== ''
  ) {
    return elementState.validationMessage;
  }
  return '';
}

export function isListElement(schema: ISchema, id: string): boolean {
  const elementSchema = getSchemaForElement(schema, id);
  if (
    elementSchema != null &&
    typeof elementSchema.isList !== 'undefined' &&
    elementSchema.isList === true
  ) {
    return true;
  }
  return false;
}

export function getPossibleValues(
  data: IData,
  jsonpath: string,
  pluginName: PluginNameEnum,
  id?: string,
): IPossibleValue[] {
  const elementState = getElementByIdOrJsonPath(data, jsonpath, id);
  // CMS: possibleValues direkt am Feld
  if (pluginName == PluginNameEnum.CMS_FORMS && elementState?.possibleValues && elementState.possibleValues.length > 0) {
    return elementState.possibleValues;
  }
  // AN: possibleValueSetID → possibleValueSets
  if (
    pluginName == PluginNameEnum.ACTA_NOVA &&
    data.possibleValueSets != null &&
    elementState?.possibleValueSetID != null
  ) {
    const set = data.possibleValueSets.find(
      (i) => i.possibleValueSetID === elementState?.possibleValueSetID,
    );
    if (set) {
      return set.possibleValues;
    }
  }
  return [];
}

/**
 * 
 * Reads the catalog `customOption` flag from the per-element data object, i.e. the
 * same structure `getPossibleValues` reads `possibleValues` from
 * (`data.properties[id]`). Returns true only when the backend explicitly set it.
 */
export function getCMSCustomOption(
  data: IData,
  jsonpath: string,
  id?: string,
): boolean {

  const elementState = getElementByIdOrJsonPath(data, jsonpath, id);
  return elementState?.customOption === true;
}

export function getLoading(formState: FormState, jsonpath: string): boolean {
  const elementState = getElementByJsonPath(formState.data, jsonpath);
  if (elementState?.loading) {
    return true;
  }
  return false;
}

export function getANFieldLoadError(
  formState: FormState,
  jsonpath: string,
): boolean {
  const elementState = getElementByJsonPath(formState.data, jsonpath);
  if (elementState?.ANFieldLoadError) {
    return true;
  }
  return false;
}

export function setStateElementByJsonpath(
  data: IData,
  jsonpath: string,
  element: IDataFieldProperties,
) {
  if (jsonpath && jsonpath !== '') {
    JsonPath.apply(data, jsonpath, function (value: any) {
      return element;
    });
  }
}

// der letzte Parameter ist essentiellel, das löst das Problem in folgender situation: 
// Wenn man vom Hauptformular zurück zum Hauptformular navigiert, dann kann sein,
//  dass der Value innerhalb vom formstate nicht geändert wird, obwohl jegliche Änderung auf newValueEntry Auswirkung auf formstate haben soll. Reusltat: Values wird aktualisiert aber value nicht!
// Die Lösung ist, dass man da explizit sagt, dass value auch geändert werden soll, selbst wen value ein typ von array ist.
export function setValue(state: FormState, jsonpath: string, value: any, updateValueAndValues?: boolean) {
  const elementState = getElementByJsonPath(state.data, jsonpath);
  if (elementState != null) {
    if (Array.isArray(value)) {
      elementState.values = value;
      if(updateValueAndValues){
        elementState.value = value;
      }
    } else {
      elementState.value = value;
    }
    // valueIsChanged will trigger save background
    (global as any).valueIsChanged = true;
    setStateElementByJsonpath(state.data, jsonpath, elementState);
  }
}


/**
 * iniitally the value will be an empty string
 * once reset, the value is an empty object
 * Acta Nova can not handle the empty string value
 *
 * @param {(string|object)} value
 * @returns {{boolean}}
//  */
export function isFilled(value: string | object) {
  if (value == '') {
    return false;
  }
  return !isEmptyObject(value);
}

/**
 * checks if the given `obj` os an empty object
 *
 * @param {object} obj
 * @returns {{boolean}}
 */
export function isEmptyObject(obj: object | string) {
  return Object.keys(obj).length === 0 && obj.constructor === Object;
}

export function getValidation(schema: ISchema, id: string): IValidation {
  const elementSchema = getSchemaForElement(schema, id);

  if (elementSchema != null && typeof elementSchema.validation != 'undefined') {
    return elementSchema.validation;
  }
  return {};
}

export function getBinaryURL(formid: string, binaryValue: string, businessObjectID?: string) {
  // TODO anBasePath
  const anBasePath = "";
  let url = anBasePath + "form/" + encodeURIComponent(formid);
  if (businessObjectID !== null && businessObjectID !== "") {
    if (typeof businessObjectID === "undefined") {
      businessObjectID = "";
    }
    url = url + `/businessObject/${encodeURIComponent(businessObjectID)}`;
  }
  url = url + "/binary/" + encodeURIComponent(binaryValue);
  return url;
}

/**
 * retrieves and returnes the stored value of a given formGridnOption. If storedValue is
 * empty it returns the default Value 'X'
 *
 * @param {IFormGridOptions} formgridOptions
 * @returns {string} storedValue
 */
export function getBooleanStoredValue(formgridOptions?: IFormGridOptions): string {
  if (!formgridOptions?.storedValue) return 'X'
  return formgridOptions.storedValue;
}

export function removeValue(formState: FormState, jsonpath: string) {
  const elementState = getElementByJsonPath(formState.data, jsonpath);
  if (elementState != null) {
    delete elementState.values;
    delete elementState.value;
    (global as any).valueIsChanged = true;
    setStateElementByJsonpath(formState.data, jsonpath, elementState);
  }
}

/**
 * Source https://stackoverflow.com/a/31711034
 * @param val
 * @param step
 * @returns
 */
export function floatSafeModulo(val: string, step: number) {
  var valDecCount = (val.toString().split('.')[1] || '').length;
  var stepDecCount = (step.toString().split('.')[1] || '').length;
  var decCount = valDecCount > stepDecCount ? valDecCount : stepDecCount;
  var valInt = parseInt(Number(val).toFixed(decCount).replace('.', ''));
  var stepInt = parseInt(Number(step).toFixed(decCount).replace('.', ''));
  return (valInt % stepInt) / Math.pow(10, decCount);
}

export function setANFieldLoadError(
  formState: FormState,
  jsonpath: string,
  error: boolean,
) {
  const elementState = getElementByJsonPath(formState.data, jsonpath);
  if (elementState != null) {
    elementState.ANFieldLoadError = error;
  }
}
export function getErrorMessages(
  properties: IDataFields,
  pages: IFormPage[] = [],
  pluginName: PluginNameEnum,
  pageNumberParent?: number,
  aggregateID?: string,
  subAggregateID?: string,
  subAggregateSortIndex?: number,
): IErrorMessage[] {
  let errorMessages: IErrorMessage[] = [];
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  for (const [key, value] of Object.entries(properties)) {    
    // is there an element
    if (value) {
      let pageNumber = 0;
      if (pageNumberParent) {
        pageNumber = pageNumberParent;
      } else {
        pageNumber = getPageNumberFromElementId(key, pages);
      }
      // is there an error on the current element (only relevant if element is visible for forms plugin)
      const isVisible = pluginName === PluginNameEnum.ACTA_NOVA ? value.visible : true;
      if (value.validationMessage && isVisible) {
        errorMessages.push({
          validationMessage: value.validationMessage,
          pageNumber,
          elementInfo: {elementID: key, aggregateID, subAggregateID, subAggregateSortIndex
          }
        });
      }
      // check if there are subelements (aggregate) (only relevant if element is visible for forms plugin)
      const isSubVisible = pluginName === PluginNameEnum.ACTA_NOVA ? value.visible : true;
      if ((value.properties || value.value) && isSubVisible) {
        // Keep the aggregate ID stable: if we already know the parent aggregate (e.g. 70C581CE-...), use it;
        // otherwise fall back to the current key.
        const nextAggregateID = aggregateID ?? key;

        // If the current node is a subaggregate entry, it typically has its own `id`
        // and a `value` object (NOT an array). In that case, we want to persist that
        // subaggregate row ID for all nested elements.
        const looksLikeSubAggregateEntry =
          typeof (value as any).id === 'string' &&
          !!(value as any).value &&
          !Array.isArray((value as any).value);

        const nextSubAggregateID = looksLikeSubAggregateEntry
          ? ((value as any).id as string)
          : subAggregateID;

        const nextSubAggregateSortIndex = looksLikeSubAggregateEntry
          ? (typeof (value as any).sortIndex === 'number'
              ? (value as any).sortIndex
              : undefined)
          : subAggregateSortIndex;

        if (value.properties) {
          errorMessages = errorMessages.concat(
            getErrorMessages(
              value.properties,
              pages,
              pluginName,
              pageNumber,
              nextAggregateID,
              nextSubAggregateID,
              nextSubAggregateSortIndex,
            ),
          );
        } else {
          errorMessages = errorMessages.concat(
            getErrorMessages(
              value.value,
              pages,
              pluginName,
              pageNumber,
              nextAggregateID,
              nextSubAggregateID,
              nextSubAggregateSortIndex,
            ),
          );
        }
      }
      // check dynamic aggregate subelements
      if (value.possibleAggregateValues && value.values) {
        // Determine the aggregate ID for dynamic aggregates
        const nextAggregateID = aggregateID ?? key;
        const setsOfAggregatFields = value.values;
        for (let index = 0; index < setsOfAggregatFields.length; index++) {
          const subAggregat = setsOfAggregatFields[index];
          const rowId = subAggregat?.id as string | undefined;
          const rowSortIndex = typeof subAggregat?.sortIndex === 'number' ? subAggregat.sortIndex : index;
          errorMessages = errorMessages.concat(
            getErrorMessages(subAggregat, pages, pluginName, pageNumber, nextAggregateID, rowId, rowSortIndex),
          );
        }
      }
    }
  }
  return errorMessages;
}

  /**
 * Findet das erste Fehlerelement in visueller Reihenfolge.
 */
export const findVisuallyFirstErrorElement = (
  errors: IErrorMessage[],
  form: IForm,
): IFirstErrorElementInfo | null => {
  if (!errors.length) return null;
  
  // Map für schnellen Zugriff auf Fehler nach Element-ID
  const errorMap = new Map<string, IErrorMessage>();
  errors.forEach(err => errorMap.set(err.elementInfo.elementID, err));
  
  // Hilfsfunktion für rekursives Traversieren von Elementen
  function traverseElements(elements: IFormElement[]): IFirstErrorElementInfo | null {
    for (const element of elements || []) {
      // Prüfe dieses Element auf Fehler
      const error = errorMap.get(element.id);
      if (error) {
        return error.elementInfo;
      }
      
      // Prüfe Sub-Elemente falls vorhanden
      if (element.elements?.length) {
        const subError = traverseElements(element.elements);
        if (subError) return subError;
      }
      
      // Spezialfall: Fehler in Aggregaten 
      if (element.type === 'aggregate') {
        const aggregateErrors = errors.filter(err => 
          err.elementInfo.aggregateID === element.id
        );
        if (aggregateErrors.length) {
          // Sortiere nach SubAggregateSortIndex falls vorhanden
          aggregateErrors.sort((a, b) => 
            (a.elementInfo.subAggregateSortIndex ?? 0) - 
            (b.elementInfo.subAggregateSortIndex ?? 0)
          );
          return aggregateErrors[0].elementInfo;
        }
      }
    }
    return null;
  }
  
  // Durchlaufe jede Seite in Reihenfolge
  for (const page of form.pages || []) {
    const errorInfo = traverseElements(page.elements);
    if (errorInfo) return errorInfo;
  }
  
  // Fallback: Erstes Element aus Fehlerliste
  return errors[0].elementInfo;
}



/**
 * returns the page number of the given element within given pages array or 0
 */
function getPageNumberFromElementId(key: string, pages: IFormPage[]) {
  for (let pageIndex = 0; pageIndex < pages.length; pageIndex++) {
    const elements = pages[pageIndex].elements;
    for (let elementIndex = 0; elementIndex < elements.length; elementIndex++) {
      const element = elements[elementIndex];
      if (element.id === key) {
        return pageIndex;
      }
    }
  }
  return 0;
}
export const startANRequest = (setOverlayCallBack?: () => void, disableThreshold?: boolean, updateOnChangeThreshold?: boolean) => {
  let thresholdMilliSeconds: number = 300;
  const anRoundTripThresholdMS = disableThreshold != null && disableThreshold == true ? 0 : updateOnChangeThreshold != null && updateOnChangeThreshold == true ? thresholdMilliSeconds / 2 : thresholdMilliSeconds;
  (global as any).anRequestRunning = true;
  setTimeout(() => {
    if ((global as any).anRequestRunning == true) {
      setOverlayCallBack && setOverlayCallBack();
    }
  }, anRoundTripThresholdMS);
};

export const endANRequest = (
  uiState: UIState,
  updateState: (statea: StatesToUpdateEnum) => void,
  callBack?: () => void,
) => {
  (global as any).anRequestRunning = false;
  overlayUtils.hideOverlay(uiState, updateState);
  if (callBack) callBack();
};

export const prepareDataForSave = (formState: FormState, uploadBinaries: boolean): FormData => {
  var data = new FormData();
  // For CMS forms, send the effective visibility state per field (clone to avoid mutating state).
  const dataToSave: IData = JSON.parse(JSON.stringify(formState.data));
  applyCmsVisibility(dataToSave, formState.form, formState.pluginName, formState);
  data.append('formdata', JSON.stringify(dataToSave));
  if (formState.fileUploads != null && uploadBinaries) {
    for (let [schemaID, jsonpaths] of Object.entries(formState.fileUploads)) {
      let jsonpathsObject: object = {};
      if (typeof jsonpaths === 'object' && jsonpaths != null) {
        jsonpathsObject = jsonpaths;
      }
      for (let [jsonpath, fileArray] of Object.entries(jsonpathsObject)) {
        if (fileArray != null && Array.isArray(fileArray)) {
          for (let [index, file] of Object.entries(fileArray)) {
            let tmpjsonpath = jsonpath;
            if (isListElement(formState.schema, schemaID)) {
              tmpjsonpath = tmpjsonpath + '.values[' + index + ']';
            } else {
              tmpjsonpath = tmpjsonpath + '.value';
            }
            if (typeof file.name === 'string') {
              data.append(encodeURIComponent(tmpjsonpath), file);
            }
          }
        }
      }
    }
  }
  return data;
};

export function setLoading(
  state: FormState,
  jsonpath: string,
  loading: boolean
) {
  const elementState = getElementByJsonPath(state.data, jsonpath);
  if (elementState != null) {
    elementState.loading = loading;
  }
}

export function redirectFormRedirect(data: IData) {
  let url = data.formControls.button.targetUrl;
  //redirect to targetUrl
  if (data.formControls?.button?.action === "openWindow") {
    window.open(url, "_blank")?.focus();
    if (data.formControls.button.actionAfterCalling === "historyBack") {
      window.history.back();
    }
  } else {
    window.open(url, "_self")?.focus();
  }
}

export const isTouchOnlyDevice = (): boolean => {
  // Nur im Web relevant
  if (Platform.OS !== 'web') {
    return false;
  }

  // Prüft, ob das primäre Eingabegerät "grob" ist (Finger) UND nicht hovern kann (keine Maus)
  const isTouchPrimary = window.matchMedia('(hover: none) and (pointer: coarse)').matches;

  // Alternativ/Zusätzlich: Prüfen ob Touch-Punkte unterstützt werden
  const hasTouchPoints = navigator.maxTouchPoints > 0;

  return isTouchPrimary && hasTouchPoints;
};

/**
 * Liefert den passenden `pointerEvents`-Wert für eine View je nach Preview-Modus.
 * Funktion existiert um mehrfachen identischen code zu reduzieren.
 *
 * Im Formgrid-Preview (Form ist im Angular-Editor eingebettet) sollen Form-Inputs
 * nicht auf Klicks/Tastatur reagieren — sie sind reine Vorschau.
 *
 * @param preview `true` im Formgrid-Preview-Modus, sonst `false`/undefined
 * @returns `'none'` (blockiert Interaktion) oder `'auto'` (normales Verhalten)
 */
export const previewPointerEvents = (preview?: boolean): 'none' | 'auto' =>
  preview ? 'none' : 'auto';

/**
 * The single mapping point: backend JSON (`RawCaptchaConfig`) -> internal
 * `CaptchaConfig`.
 *
 * Decouples the backend response shape from the internal model. Returns
 * `undefined` when no captcha config is present.
 */
export function mapCaptchaConfig(
  raw: RawCaptchaConfig | null | undefined,
): CaptchaConfig | undefined {
  if (!raw) {
    return undefined;
  }
  return {
    provider: raw.provider,
    siteKey: raw.sitekey,
    mode: raw.mode as FriendlyCaptchaMode | reCaptchaMode,
    baseUrl: raw.baseUrl,
    language: raw.language,
  };
}