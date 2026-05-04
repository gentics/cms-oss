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


export function getI18nTextStr(
  culture: string,
  elem?: ITranslationSet,
): string {
  if (!elem) return '';
  if (!culture) culture = 'de-CH';
  if (elem[culture]) return elem[culture];
  if (culture.length > 2) culture = culture.substring(0, 2);
  return elem[culture] || '';
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

export function getValue(formState: FormState, jsonpath: string, id?: string) {
  const elemState = getElementByIdOrJsonPath(formState.data, jsonpath, id);
  if (elemState) {
    if (elemState.values) {
      return elemState.values;
    }
    if (elemState.value) {
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
  id?: string,
): IPossibleValue[] {
  const elementState = getElementByIdOrJsonPath(data, jsonpath, id);
  if (
    data.possibleValueSets != null &&
    elementState?.possibleValueSetID != null
  ) {
    let possibleValues = data.possibleValueSets.find(
      (i) => i.possibleValueSetID === elementState?.possibleValueSetID,
    );
    if (possibleValues) {
      return possibleValues.possibleValues;
    }
  }
  return [];
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
      // is there an error on the current element (only relevant if element is visible)
      if (value.validationMessage && value.visible) {
        errorMessages.push({
          validationMessage: value.validationMessage,
          pageNumber,
          elementInfo: {elementID: key, aggregateID, subAggregateID, subAggregateSortIndex
          }
        });
      }
      // check if there are subelements (aggregate) (only relevant if element is visible)
      if ((value.properties || value.value) && value.visible) {
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
              pageNumber,
              nextAggregateID,
              nextSubAggregateID,
              nextSubAggregateSortIndex
            ),
          );
        } else {
          errorMessages = errorMessages.concat(
            getErrorMessages(
              value.value,
              pages,
              pageNumber,
              nextAggregateID,
              nextSubAggregateID,
              nextSubAggregateSortIndex
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
            getErrorMessages(subAggregat, pages, pageNumber, nextAggregateID, rowId, rowSortIndex),
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
  data.append('formdata', JSON.stringify(formState.data));
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