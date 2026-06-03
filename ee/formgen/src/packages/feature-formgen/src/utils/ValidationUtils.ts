import {
  ApiResponse,
  CommonValidation,
  FormState,
  IData,
  IDataAggregationListEntry,
  IErrorMessage,
  IFormElement,
  IFormGridOptions,
  UIState,
} from '../types/interfaces';
import { getElementTypeByPlugin } from '../custom/ElementTypes';
import i18n from '@andp/core/src/services/i18n';
import * as utils from '../utils/Utils';
import * as overlayUtils from '../utils/OverlayUtils';
import * as mockFormularUtils from '../utils/MockFormularUtils';
import JsonPath from 'jsonpath';
import memoize from 'memoizee';
import { RestService } from '../services/RestService';
import { CaptchaService } from '../services/CaptchaService';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { OverlayTypeEnum } from '../custom/OverlayTypes';
import { IOverlayFullScreenTextEnum } from '../constants/enums/IOverlayFullScreenTextEnum';
import { postLoadValidation, preLoadValidation } from '../custom/HooksUtils';
import { InvalidValidationResponse } from '../exceptions/InvalidValidationResponse';

export function commonValidate(
  formState: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  const elementState = utils.getElementByIdOrJsonPath(
    formState.data,
    jsonpath,
    id,
  );
  const schemaState = utils.getSchemaForElement(formState.schema, id);
  const validations = utils.getValidation(formState.schema, id);

  /**
   * Konvertiert einen lokalisierten Zahlenstring (z.B. "1.234,56" oder "1,234.56") in eine JS-Zahl.
   * Ist `value` bereits eine `number`, wird sie direkt zurückgegeben.
   * Leerer String → `NaN`. Entfernt Tausendertrennzeichen und normalisiert das Dezimalkomma zu `.`.
   */
  function parseNumber(
    value: string | number = '',
    locale = navigator.language,
  ): number {
    if (typeof value === 'number') return value;
    // skip on empty value
    if (value === '') {
      return NaN;
    }
    
    const cleaned = value.replace(/[^-+0-9.,]/g, '');
    const noGrouping = cleaned.replace(/(?<=\d)[.,](?=\d{3}(\D|$))/g, ''); // Tausender raus
    const normalized = noGrouping.replace(/,/g, '.');

    return parseFloat(normalized);
  }

  if (elementState == null) {
    //@TODO check what to do in this case?
    return { isValid: false };
  }

  const isDefaultCheckbox =
    !formGridOptions?.dropdown &&
    !formGridOptions?.button &&
    !formGridOptions?.radio;

  // Determine the value to validate: use localValue if provided; otherwise, get the value from formState
  // => for client side validation
  const value =
    localValue == null || localValue == undefined
      ? utils.getValue(formState, jsonpath, id)
      : localValue;

  // Validation: Required fields
  if (utils.isRequired(formState, id)) {
    const isBooleanType = schemaState.type === 'boolean';

    if (isBooleanType) {
      // Default-Checkbox: mandatory only true/'True' valid
      if (isDefaultCheckbox) {
        if (value !== true && value !== 'True' && value !== 'true') {
          const errorMsg = i18n.t('form_notFilledError');
          if (updateErrorMessages) {
            if (localValue == null) {
              elementState.validationMessage = errorMsg;
              utils.setStateElementByJsonpath(
                formState.data,
                jsonpath,
                elementState,
              );
            } else {
              return { isValid: false, errorMessage: errorMsg };
            }
          }
          return { isValid: false, errorMessage: errorMsg };
        }
      }
      // Dropdown / Radio:  mandatory false/'False' is valid but undefined / '' / '---' not
      else {
        if (
          typeof value === 'undefined' ||
          value === '---' ||
          (!Array.isArray(value) && value === '') ||
          (typeof value === 'string' && value.trim() === '')
        ) {
          const errorMsg = i18n.t('form_notFilledError');
          if (updateErrorMessages) {
            if (localValue == null) {
              elementState.validationMessage = errorMsg;
              utils.setStateElementByJsonpath(
                formState.data,
                jsonpath,
                elementState,
              );
            } else {
              return { isValid: false, errorMessage: errorMsg };
            }
          }
          return { isValid: false, errorMessage: errorMsg };
        }
      }
    } else {
      if (
        typeof value === 'undefined' ||
        (!Array.isArray(value) && value === '') ||
        (typeof value === 'string' && value.trim() === '') ||
        (Array.isArray(value) && value.length === 0) ||
        (value.constructor === Object && Object.keys(value).length === 0)
      ) {
        const errorMsg = i18n.t('form_notFilledError');
        if (updateErrorMessages) {
          if (localValue == null) {
            elementState.validationMessage = errorMsg;
            utils.setStateElementByJsonpath(
              formState.data,
              jsonpath,
              elementState,
            );
          } else {
            return { isValid: false, errorMessage: errorMsg };
          }
        }
        return { isValid: false, errorMessage: errorMsg };
      }
    }
  }

  const hasNumberValue = value !== null && value !== undefined && value !== '';
  if (schemaState.type === 'number' && hasNumberValue) {
    if (hasNumberValue) {
      // check that the value is a multiple of the number step
      if (formGridOptions?.numberStep && formGridOptions?.numberStep > 0) {
        // the number field should only accept numbers and period
        let stepAsNumber = formGridOptions.numberStep;
        // Ganzzahl-Logik
        if (schemaState.precision === 0 && stepAsNumber < 1) {
          stepAsNumber = 1;
        }

        const normalizedValue = value
          .toString()
          // Apostroph und alle Tausender Trenner entfernen  
          .replace(/(?:(?<=\d)[\s'’´`\u00A0\u202F.,](?=\d{3}))/g, '')
          // Dezimalkomma → Punkt
          .replace(',', '.');

        // um modulo fehler bei zu großer zahl zu vermeiden ->
        // zahl hinten in größe von 2 Stellen mehr als stepAsNumber abschneiden und testen
        const modulo = utils.floatSafeModulo(normalizedValue, stepAsNumber);

        if (stepAsNumber > 0 && modulo !== 0) {
          const errorMsg = i18n.t('form_notValidStepNumberError', {
            step: formGridOptions.numberStep,
          });
          if (updateErrorMessages) {
            if (localValue == undefined || localValue == null) {
              elementState.validationMessage = errorMsg;
              utils.setStateElementByJsonpath(
                formState.data,
                jsonpath,
                elementState,
              );
            } else {
              return { isValid: false, errorMessage: errorMsg };
            }
          }
          return { isValid: false, errorMessage: errorMsg };
        }
      }
      if(isNaN(value)) {
        //elementState.value = "";
        let errorMsg = i18n.t('form_notANumberError');
        elementState.validationMessage = errorMsg;
        utils.setStateElementByJsonpath(
          formState.data,
          jsonpath,
          elementState,
        );
        return { isValid: false, errorMessage: errorMsg };
      }
    }
  }
  // Validation: Additional validations (maxLength, minValue, maxValue, regexValidation, etc.)
  //@TODO updateErrorMessage to own method with translation
  if (validations && Object.keys(validations).length > 0) {
    for (const validationKey in validations) {
      if (!Object.prototype.hasOwnProperty.call(validations, validationKey))
        continue;
      const validationInfo = validations[validationKey];
      let parsedNr: number;
      switch (validationKey) {
        case 'maxLength':
          if (
            value != null &&
            value.length != null &&
            value.length > validationInfo
          ) {
            const errorMsg = i18n.t('form_maxLengthError', {
              maxCount: validationInfo,
            });
            if (updateErrorMessages) {
              if (localValue == undefined || localValue == null) {
                elementState.validationMessage = errorMsg;
                utils.setStateElementByJsonpath(
                  formState.data,
                  jsonpath,
                  elementState,
                );
              } else {
                return { isValid: false, errorMessage: errorMsg };
              }
            }
            return { isValid: false, errorMessage: errorMsg };
          }
          break;
        case 'minValue':
          parsedNr = parseNumber(value);
          if (
            value != null &&
            value.length > 0 &&
            !isNaN(parsedNr) &&
            parsedNr < validationInfo
          ) {
            const errorMsg = i18n.t('form_minError', {
              minVal: validationInfo,
            });
            if (updateErrorMessages) {
              if (localValue == undefined || localValue == null) {
                elementState.validationMessage = errorMsg;
                utils.setStateElementByJsonpath(
                  formState.data,
                  jsonpath,
                  elementState,
                );
              } else {
                return { isValid: false, errorMessage: errorMsg };
              }
            }
            return { isValid: false, errorMessage: errorMsg };
          }
          break;
        case 'maxValue':
          parsedNr = parseNumber(value);
          if (
            value != null &&
            value.length > 0 &&
            !isNaN(parsedNr) &&
            parsedNr > validationInfo
          ) {
            const errorMsg = i18n.t('form_maxError', {
              maxVal: validationInfo,
            });
            if (updateErrorMessages) {
              if (localValue == undefined || localValue == null) {
                elementState.validationMessage = errorMsg;
                utils.setStateElementByJsonpath(
                  formState.data,
                  jsonpath,
                  elementState,
                );
              } else {
                return { isValid: false, errorMessage: errorMsg };
              }
            }
            return { isValid: false, errorMessage: errorMsg };
          }
          break;
        case 'regexValidation':
          if (value != null && value.length > 0 && !!validationInfo?.regex) {
            const regex = new RegExp(validationInfo.regex);
            if (!regex.test(value)) {
              const errorMessage = utils.getI18nText(
                formState,
                validationInfo?.errorMessage,
              );
              if (updateErrorMessages) {
                if (localValue == undefined || localValue == null) {
                  elementState.validationMessage = errorMessage;
                  utils.setStateElementByJsonpath(
                    formState.data,
                    jsonpath,
                    elementState,
                  );
                } else {
                  return { isValid: false, errorMessage };
                }
              }

              return { isValid: false, errorMessage };
            }
          }
          break;
        case 'minLength':
          //only used for autocomplete min length
          //https://jira.rubicon.eu/browse/EGOV-1995
          //so do nothing for validation
          break;
        default:
          console.log(validationKey + ' not handled yet');
          break;
      }
    }
  }
  return { isValid: true, errorMessage: '' };
}

export function validateField(
  formState: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  // For Custom-FormGrid-Elements, which are not in state.data.properties & state.schema.properties
  if (
    formGridOptions &&
    formGridOptions.type &&
    formGridOptions.type !== 'formgridText' &&
    formGridOptions.type !== 'formgridSpacer'
  ) {
    return getElementTypeByPlugin(formGridOptions.type.toLowerCase(), formState.pluginName).validate(
      formState,
      jsonpath,
      id,
      updateErrorMessages,
      i18n.t,
      formGridOptions,
      localValue,
    );
  }

  const elemState = utils.getElementByIdOrJsonPath(
    formState.data,
    jsonpath,
    id,
  );
  const schemaState = utils.getSchemaForElement(formState.schema, id);

  if (!updateErrorMessages && elemState && elemState.validationMessage) {
    return { isValid: false, errorMessage: elemState.validationMessage };
  }

  // do not validate invisible fields and readonly fields
  if (!elemState || elemState.visible === false || elemState.editable === false) {
    return { isValid: true, errorMessage: '' };
  }

  if (
    updateErrorMessages &&
    elemState !== null &&
    elemState.validationMessage &&
    elemState.validationMessage !== ''
  ) {
    elemState.validationMessage = '';
    utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
  }

  const elementType = schemaState.type.toLowerCase();
  let result: CommonValidation = { isValid: true, errorMessage: '' };
  if (getElementTypeByPlugin(elementType, formState.pluginName)) {
    result = getElementTypeByPlugin(schemaState.type.toLowerCase(), formState.pluginName).validate(
      formState,
      jsonpath,
      id,
      updateErrorMessages,
      i18n.t,
      formGridOptions,
      localValue,
    );
  }
  return result;
}

export const isPageValid = (
  formState: FormState,
  updateErrorMessages: boolean,
  pageNumber: number,
  mockForm?: boolean,
): boolean => {
  if (typeof formState.form.pages != 'undefined') {
    for (const [key, value] of Object.entries(formState.form.pages)) {
      if (parseInt(key) === pageNumber) {
        const areFieldsValid = areSubElementsValid(
          formState,
          value.elements,
          updateErrorMessages,
          mockForm,
        );
        const captchaValid = isCaptchaValid();
        return areFieldsValid && captchaValid;
      }
    }
  }
  return false;
};

// Create a memoized version of JsonPath.nodes using memoizee.
// By default, memoizee uses strict (reference) equality for arguments,
// which is equivalent to using isEqual: (a, b) => a === b.
const memNodes = memoize(JsonPath.nodes.bind(JsonPath), {
  // You can add additional options here if needed,
  // e.g., a custom normalizer if your arguments are complex objects:
  // normalizer: (args) => JSON.stringify(args),
});

export function areSubElementsValid(
  formState: FormState,
  object: IFormElement[] | undefined,
  updateErrorMessages: boolean,
  mockForm?: boolean,
): boolean {
  let valid = true;
  if (object) {
    for (let key in object) {
      const obj = object[key];
      //@TODO check jsonpath
      let res = memNodes(formState.data.properties, '$..["' + obj.id + '"]');

      // if element is FormGridElement
      if ((!res || res.length === 0) && obj.formGridOptions?.type) {
        // Check if it is not Formgrid-Text or Spacer
        if (
          obj.formGridOptions.type !== 'formgridText' &&
          obj.formGridOptions.type !== 'formgridSpacer'
        ) {
          if (
            !isFieldValid(
              formState,
              '$..["' + obj.id + '"]',
              obj.id,
              updateErrorMessages,
              obj?.formGridOptions,
            )
          ) {
            valid = false;
          }
        }
      }

      // eslint-disable-next-line
      res.forEach((foundElem: any) => {
        // do not check items or its children which are not visible
        if (foundElem?.value?.visible === false) {
          return true;
        }
        // TODO add more abort scenarios
        let pathLength = foundElem.path.length - 2;
        if (pathLength >= 0) {
          if (foundElem.path[pathLength] === 'properties') return;
          if (foundElem.path[pathLength] === 'possibleAggregateValues') return;
        }

        if (mockForm) {
          //ID for Ort & Srasse
          if (
            // Mockvalidation Ids
            obj.id === '9094441D-13E8-4848-A88E-2F8A399FE8E8' ||
            obj.id === '4491D2D9-7837-4A91-9B6F-EB3A13AB09CB' ||
            obj.id === '89D6E122-2CC7-4940-A202-F1D2C51898AD' ||
            obj.id === '78B7F122-2CC7-4940-A202-F1D2C51898AD' ||
            obj.id === '51789F86-C11F-4D1E-8BC9-077A06327B02' ||
            obj.id === 'ED042909-E0D0-444F-AA1B-A352787EDD4C' ||
            obj.id === 'G46F9670-8B2A-4731-8A01-9592C0DB6A13' ||
            obj.id === 'G46F9670-8B2A-4731-8A01-9592C0DB6A15' // Validation for dropdown radio & dropdon checkbox
          ) {
            if (
              !isFieldValid(
                formState,
                '$..["' + obj.id + '"]',
                obj.id,
                updateErrorMessages,
                obj?.formGridOptions,
              )
            ) {
              valid = false;
            }
          }
        } else {
          if (
            !isFieldValid(
              formState,
              '$..["' + obj.id + '"]',
              obj.id,
              updateErrorMessages,
              obj?.formGridOptions,
            )
          ) {
            valid = false;
          }
          // only run for aggregate
          if (obj.type === 'aggregate') {
            const dataField = formState.data.properties?.[obj.id];
            // on aggregate lists there will be no value but a values property (plural)
            if (dataField?.value && Array.isArray(dataField?.value)) {
              dataField.value.forEach(
                (value: IDataAggregationListEntry, index: number) => {
                  //TODO v8.2 aggregate
                  for (let key in value.value) {
                    if (
                      !isFieldValid(
                        formState,
                        `$..["${obj.id}"].value[${index}].value["${key}"]`,
                        key,
                        updateErrorMessages,
                      )
                    ) {
                      valid = false;
                    }
                  }
                },
              );
            }
            // on fixed aggregat fields there is no values but value (singular)
            if (dataField?.value) {
              for (let key in dataField.value) {
                if (
                  !isFieldValid(
                    formState,
                    `$..["${obj.id}"].value["${key}"]`,
                    key,
                    updateErrorMessages,
                  )
                ) {
                  valid = false;
                }
              }
            }
          }
        }
      });
    }
  }
  return valid;
}

export const isFieldValid = (
  formState: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  formGridOptions?: IFormGridOptions,
) => {
  return validateField(
    formState,
    jsonpath,
    id,
    updateErrorMessages,
    formGridOptions,
  ).isValid;
};

export const loadValidationWithHooks = async (
  restService: RestService,
  formState: FormState,
  uiState: UIState,
  updateState: (stateToUpdate: StatesToUpdateEnum) => void,
  afterValidation: (valid?: boolean, data?: IData) => void,
  jsonPath?: string,
  mockForm?: boolean,
  updateOnChange?: boolean,
) => {
  await preLoadValidation(
    restService,
    formState,
    uiState,
    updateState,
    afterValidation,
    jsonPath,
    mockForm,
    updateOnChange,
  );
  await loadValidation(
    restService,
    formState,
    uiState,
    updateState,
    afterValidation,
    jsonPath,
    mockForm,
    updateOnChange,
  );
  await postLoadValidation(
    restService,
    formState,
    uiState,
    updateState,
    afterValidation,
    jsonPath,
    mockForm,
    updateOnChange,
  );
};

const loadValidation = async (
  restService: RestService,
  formState: FormState,
  uiState: UIState,
  updateState: (stateToUpdate: StatesToUpdateEnum) => void,
  afterValidation: (valid?: boolean, data?: IData) => void,
  jsonPath?: string,
  mockForm?: boolean,
  updateOnChange?: boolean,
) => {
  console.log('loadValidation');
  (global as any).updateOnChangeWillRun = Date.now();

  const errorFunc = (errorResponse: any) => {
    (global as any).updateOnChangeWillRun = undefined;
    console.error(errorResponse);
    if (jsonPath) {
      utils.setANFieldLoadError(formState, jsonPath, true);
      updateState(StatesToUpdateEnum.UIState);
    } else {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.LOAD_VALIDATION,
        updateState,
        { response: errorResponse },
      );
      updateState(StatesToUpdateEnum.UIState);
    }
    afterValidation(false);
  };

  utils.startANRequest(
    () =>
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        updateState,
        { loadingText: IOverlayFullScreenTextEnum.Validate },
      ),
    false,
    true,
  );
  updateState(StatesToUpdateEnum.UIState);
  let apiResponse: ApiResponse | undefined = undefined;
  if (mockForm) {
    console.log('Loading mock validation response');
    if (!updateOnChange) {
      let useMockRedirectFormData = false; // true => loading validationData for redirectForm -> ExternalButton -- false => validationData is formState.data
      const data = useMockRedirectFormData ? await new Promise((resolve) =>
        mockFormularUtils.fetchValidationRedirectFormData(resolve),
      ) : formState.data;
      apiResponse = { success: true, responseData: data };
    } else {
      const updateOnChangeData = await new Promise((resolve) =>
        mockFormularUtils.fetchUpdateOnChangeTriggerPreviousPageError(resolve),
      );
      apiResponse = { success: true, responseData: updateOnChangeData };
    }
    // Pause for 1 second (simulate network latency)
    await new Promise((resolve) => setTimeout(resolve, 1000));
  } else {
    apiResponse = await restService.validationRequest(formState.data);
  }
  utils.endANRequest(uiState, updateState);

  if (!apiResponse.success) {
    //Handling the situation when http code is > 400 ( = other than 200)
    // TODO v8.2 Overlay  oder das einzelne Feld auf loadError true setzen
    (global as any).updateOnChangeWillRun = undefined;
    errorFunc(apiResponse);
  } else {
    try {
      (global as any).updateOnChangeWillRun = undefined;
      await handleValidation(
        apiResponse.responseData,
        formState,
        uiState,
        updateState,
        jsonPath,
        undefined,
        mockForm,
        updateOnChange,
      );
      afterValidation(!formState.data.hasValidationErrors, apiResponse.responseData); // formState.data.hasValidationErrors - identifies whether there is a real validation error or not
    } catch (err) {
      if (err instanceof InvalidValidationResponse) {
        (global as any).updateOnChangeWillRun = undefined;
      }
      errorFunc(err);
    }
  }
};
/**
 * Unified validation handler for both normal and update‑on‑change validations.
 *
 * Behaviour:
 *  • Always validates the **entire** form, regardless of where the request originated.
 *  • For **empty** fields that carry a validationMessage coming from the backend,
 *    the message is removed. If the same field already had a local (client‑side)
 *    validationMessage, that local message is preserved.
 *  • state.data.hasValidationErrors is set
 *      – to true when the **current page** still contains errors,
 *      – except on the page right before the summary
 *        (currentPage === state.lastPage ‑ 1), where the flag reflects
 *        whether **any** validation errors exist in the whole form.
 */
export const handleValidation = (
  responseFormData: IData, //new data from AN
  formState: FormState,
  uiState: UIState,
  updateState: (stateToUpdate: StatesToUpdateEnum) => void,
  jsonpath?: string,
  pageIndex?: number,
  mockForm?: boolean,
  updateOnChange?: boolean,
) => {
  {
    // ---------- 1. Sanity check ----------
    if (!responseFormData?.formID) {
      throw new InvalidValidationResponse(
        'Invalid response body of validation',
      );
    }
    if (jsonpath) {
      utils.setANFieldLoadError(formState, jsonpath, false);
    }

    const previousData = formState.data;
    formState.data = responseFormData;

    // ---------- 4. Helpers ----------
    const isEmpty = (val: any): boolean =>
      val === undefined ||
      val === null ||
      (typeof val === 'string' && val.trim() === '') ||
      (Array.isArray(val) && val.length === 0);

    /**
     * Clear backend‑originating validationMessage on *empty* value,
     * but keep a local one if it already existed before.
     */
    const maybeClearValidationMessage = (
      fieldState: any,
      prevFieldState: any,
      isUpdateOnChange: boolean,
    ) => {
      if (!isUpdateOnChange) {
        delete fieldState.validationMessage;
      }
      if (
        fieldState &&
        isEmpty(fieldState.value) &&
        fieldState.validationMessage
      ) {
        if (
          prevFieldState?.validationMessage &&
          !isEmpty(prevFieldState.validationMessage)
        ) {
          fieldState.validationMessage = prevFieldState.validationMessage;
        } else {
          delete fieldState.validationMessage;
        }
      }
    };

    /**
     * Recursively walk through aggregates / containers and apply the clearing rule.
     */
    const clearValidationForField = (
      field: any,
      isUpdateOnChange: boolean,
    ): void => {
      const fieldState = formState.data?.properties?.[field.id];      
      const prevFieldState = previousData?.properties?.[field.id];
      if (!fieldState) return;

      // Simple field
      if (!field.elements) {
        maybeClearValidationMessage(
          fieldState,
          prevFieldState,
          isUpdateOnChange,
        );
        return;
      }

      // Aggregate / container
      const walkEntries = (current: any, previous: any) => {
        if (Array.isArray(current)) {
          current.forEach((entry, idx) =>
            walkEntries(
              entry,
              Array.isArray(previous) ? previous[idx] : undefined,
            ),
          );
        } else if (current && typeof current === 'object') {
          if (current.value && typeof current.value === 'object') {
            Object.keys(current.value).forEach((k) =>
              walkEntries(current.value[k], previous?.value?.[k]),
            );
          } else {
            maybeClearValidationMessage(current, previous, isUpdateOnChange);
          }
        }
      };

      walkEntries(fieldState, prevFieldState);
    };

    // ---------- 5. Page‑aware clearing ----------
    const isUpdateOnChange =
      Boolean(jsonpath) || (updateOnChange ? updateOnChange : false); // update‑on‑change validations pass a jsonpath
    if (formState.form?.pages && typeof uiState.currentPage === 'number') {
      formState.form.pages.forEach((page: any, idx: number) => {
        const shouldClear = isUpdateOnChange
          ? idx >= uiState.currentPage // löscht ab der aktuellen Seite ( >= ) einschließlich dieser Seite.
          : idx > uiState.currentPage; // löscht erst ab der nächsten Seite ( > ), die aktuelle bleibt unberührt.
        if (shouldClear) {
          page.elements?.forEach((field: any) => 
            clearValidationForField(field, isUpdateOnChange),
          );
        }
      });
    }

    // ---------- 6. Recompute error flags ----------
    // Fehlerliste immer ermitteln und über pageNumber auswerten
    let errorMessages: IErrorMessage[] = [];
    if (formState.data.properties) {
      errorMessages = utils.getErrorMessages(
        formState.data.properties,
        formState.form.pages,
      ) as IErrorMessage[];
    }

    // Gibt es Fehler auf einer der Seiten 0..currentPage? (anhand pageNumber)
    const hasErrorsUpToCurrentPage =
      typeof uiState.currentPage === 'number'
        ? errorMessages.some((e) =>
            typeof e.pageNumber === 'number'
              ? e.pageNumber <= uiState.currentPage
              : true // fehlende Seiteninfo als Fehler werten
          )
        : false;

    // Auf der letzten Seite: Fehler irgendwo im Formular? Sonst: Fehler bis zur aktuellen Seite?
    formState.data.hasValidationErrors =
      uiState.currentPage === uiState.lastPage
        ? errorMessages.length > 0
        : hasErrorsUpToCurrentPage;

    uiState.loading = false;
    updateState(StatesToUpdateEnum.ALL);
  }
};

// Needed for Autocomplete
/**
 * returns `false` for elements that have data-hasblur set
 * returns `true` for elements with element schema has updateOnChange set
 * @returns
 */
export function needsANValidation(
  state: FormState,
  id: string,
  jsonpath: string,
): boolean {
  const elementSchema = utils.getSchemaForElement(state.schema, id);
  if (elementSchema.updateOnChange === true) {
    return true;
  }
  return false;
}

export function isCaptchaValid(): boolean {
  if (!CaptchaService.isActive()) {
    CaptchaService.setError(false);
    return true;
  }
  // Invisible mode (reCAPTCHA): token is produced by `execute()` right before
  // the request is sent — don't gate form-level validation on its presence here.
  if (CaptchaService.hasExecute()) {
    CaptchaService.setError(false);
    return true;
  }
  const solved = CaptchaService.getToken() != null;
  CaptchaService.setError(!solved);
  return solved;
}
