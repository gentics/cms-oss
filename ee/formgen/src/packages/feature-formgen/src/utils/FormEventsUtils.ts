import {
  ApiResponse,
  CommonValidation,
  FormState,
  IData,
  IFormGridOptions,
  IOverlayInfoExtraProps,
  IOverlayInfoOptions,
  ISchemaFieldProperties,
  IValidation,
  UIState,
  IBinary,
  IFormComponentAttributes,
  IErrorMessage,
} from '../types/interfaces';
import * as utils from './Utils';
import * as validationUtils from './ValidationUtils';
import * as mockFormularUtils from './MockFormularUtils';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { RestService } from '../services/RestService';
import { CaptchaService } from '../services/CaptchaService';
import * as overlayUtils from './OverlayUtils';
import { OverlayTypeEnum } from '../custom/OverlayTypes';
import { IOverlayFullScreenTextEnum } from '../constants/enums/IOverlayFullScreenTextEnum';
import { FileUploadErrorTypeEnum } from '../constants/enums/FileUploadErrorTypeEnum';
import * as hookUtils from '../custom/HooksUtils';
import * as DocumentPicker from 'expo-document-picker';
import 'react-native-get-random-values';
import { v4 as uuidv4 } from 'uuid';
import { Platform } from 'react-native';
import i18n from '@andp/core/src/services/i18n';

/**
 * Detects whether a failed API response is the backend's "captcha rejected"
 * error. Backend signals it with HTTP 400 and a JSON body containing
 * `error_code: 'CAPTCHA_ERROR'`. The body may arrive parsed (object) or as a
 * raw JSON string — both shapes are accepted.
 */
function isCaptchaError(res: ApiResponse): boolean {
  if (res.responseCode !== 400) return false;
  const body = res.responseData;
  if (body == null) return false;
  if (typeof body === 'object' && (body as any).error_code === 'CAPTCHA_ERROR') {
    return true;
  }
  if (typeof body === 'string') {
    try {
      const parsed = JSON.parse(body);
      return parsed?.error_code === 'CAPTCHA_ERROR';
    } catch {
      return false;
    }
  }
  return false;
}


export const submitCSVUpload = async (
  jsonpath: string,
  id: string,
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
): Promise<void> => {
  try {
    const res = await DocumentPicker.getDocumentAsync({
      type: 'text/csv',
      multiple: false,
      copyToCacheDirectory: false,
    });

    if (res.canceled) {
      console.log('User hat den CSV-Upload abgebrochen');
      return;
    }

    const cleanUp = () => {
      const fileUploadElement = document.getElementById(
        jsonpath,
      ) as HTMLInputElement;
      if (fileUploadElement) {
        fileUploadElement.value = '';
      }
    };

    const file = res.assets[0];
    const data = new FormData();

    const stateDataWithCSVUploadFlag = formState.data;
    if (stateDataWithCSVUploadFlag.properties?.[id]) {
      stateDataWithCSVUploadFlag.properties[id]!.csvUpload = true;
    }
    data.append('formdata', JSON.stringify(stateDataWithCSVUploadFlag));
    const uploadFile = (file as any).file as File;
    data.append(uploadFile.name, uploadFile);

    const handleSuccess = (response: IData) => {
      if (!uiState.showErrorBlockOnInvalid) {
        uiState.showErrorBlockOnInvalid = true;
      }
      validationUtils.handleValidation(
        response,
        formState,
        uiState,
        updateState,
        jsonpath,
      );
    };
    const handleError = (error: any) => {
      console.error('CSV-Upload fehlgeschlagen:', error);
      cleanUp();
    };

    //this information is set to avoid autosave (or additional autosave) while a save request is running
    utils.startANRequest(() => {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        updateState,
      );
    });
    let apiResponse: ApiResponse = await restService.ANCsvUploadRequest(
      formState,
      data,
    );
    utils.endANRequest(uiState, updateState);

    if (!apiResponse.success) {
      const errorPayload =
        apiResponse.responseData ??   
        apiResponse.exception ??
        `HTTP ${apiResponse.responseCode ?? 'unknown'} during CSV upload`;

      handleError(errorPayload);
    } else {
      handleSuccess(apiResponse.responseData);
    }
  } catch (err) {
    console.error('Fehler beim CSV-Upload:', err);
  }
};

export const afterChange = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  jsonpath: string,
  id: string,
  instant: boolean,
  updateState: (state: StatesToUpdateEnum) => void,
  formGridOptions?: IFormGridOptions,
  editable?: boolean, 
) => {
  //do AN Validation only if clientside validation was correct;
  const elementFromSchema = utils.getSchemaForElement(formState.schema, id);
  //cancel pending updateOnChange Requests
  cancelUpdateOnChange();
  const validateFieldResult: CommonValidation = validationUtils.validateField(
    formState,
    jsonpath,
    id,
    true,
    formGridOptions,
  );
  if (editable && validateFieldResult.isValid && elementFromSchema.updateOnChange) {
    await updateOnChange(
      formState,
      uiState,
      restService,
      jsonpath,
      instant,
      elementFromSchema,
      updateState,
    );
  }
};

async function _updateOnChange(
  formState: FormState,
  uiState: UIState,
  jsonpath: string,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  // customData?: IData,
  mockForm?: boolean,
) {
  let localData: IData;
  // if (customData == null) {
  //     //WHY ? TODO v8.2
  //     localData = JSON.parse(JSON.stringify(formState.data));
  // } else {
  //     localData = JSON.parse(JSON.stringify(customData));
  // }
  //isUpdateOnChange info is used by AN, so not everything is validated on a simple field change
  formState.data.isUpdateOnChange = true;
  const elementState = utils.getElementByJsonPath(formState.data, jsonpath);
  if (elementState != null) {
    elementState.isUpdateOnChange = true;
  }
  if (!uiState.showErrorBlockOnInvalid) {
    uiState.showErrorBlockOnInvalid = true;
  }
  uiState.loading = true;
  await validationUtils.loadValidationWithHooks(
    restService,
    formState,
    uiState,
    updateState,
    () => {},
    jsonpath,
    mockForm,
    true
  );
}

/**
 * Run update on change Request to ActaNova
 * This will not be executed immediately
 */
export const updateOnChange = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  jsonpath: string,
  instant = false,
  elementSchema: ISchemaFieldProperties | null,
  updateState: (state: StatesToUpdateEnum) => void,
) => {
  (global as any).updateOnChangeWillRun = Date.now();
  // clear current waiting debounce and run id directly
  if (instant) {
    cancelUpdateOnChange();
    await _updateOnChange(
      formState,
      uiState,
      jsonpath,
      restService,
      updateState,
      uiState.isMockForm,
    );
  } else {
    //check if minLength is reached, otherwise no update on change is triggered
    if (
      elementSchema &&
      elementSchema.validation != null &&
      typeof elementSchema.validation['minLength'] !== 'undefined'
    ) {
      let checkValue = utils.getValue(formState, jsonpath);
      if (
        (typeof checkValue !== 'string' && !(checkValue instanceof String)) ||
        checkValue.length < elementSchema.validation['minLength']
      ) {
        return;
      }
    }

    let local = Math.random();
    (global as any).debounce = local;

    setTimeout(async function () {
      //check if debounce is still valid, before running _updateOnChange with old data
      if ((global as any).debounce) {
        if (local === (global as any).debounce) {
          cancelUpdateOnChange();
          await _updateOnChange(
            formState,
            uiState,
            jsonpath,
            restService,
            updateState,
            uiState.isMockForm,
          );
        }
      }
    }, 800);

    // reset of disabled elements after 6 seconds - just in case something does not work correctly
    setTimeout(function () {
      if (
        (global as any).updateOnChangeWillRun &&
        (global as any).updateOnChangeWillRun > 0
      ) {
        if ((global as any).updateOnChangeWillRun + 5000 < Date.now()) {
          (global as any).updateOnChangeWillRun = undefined;
        }
      }
    }, 6000);
  }
};

export const handleChange = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  jsonpath: string,
  id: string,
  instant: boolean = true,
  updateState: (statesToUpdate: StatesToUpdateEnum) => void,
  value?: any,
  formGridOptions?: IFormGridOptions,
  editable?: boolean,
) => {
  utils.setValue(formState, jsonpath, value);
  await afterChange(
    formState,
    uiState,
    restService,
    jsonpath,
    id,
    instant,
    updateState,
    formGridOptions,
    editable,
  );
  updateState(StatesToUpdateEnum.ALL);

  /*
  const overlayInfoOptions: IOverlayInfoOptions =
  overlayUtils.getOverlayInfoOptions(formGridOptions?.overlayOptions, value);
// UVEK-879 check if feature overlay is enabled
if (
  false
) {
  // display overlay
  const handleCancelOverlayInfo = async () => {
    utils.setValue(formState, jsonpath, null);
    await afterChange(
      formState,
      uiState,
      restService,
      jsonpath,
      id,
      instant,
      updateState,
      formGridOptions,
    );
  };
  overlayUtils.setOverlayInfo(
    uiState,
    {
      overlayInfoOptions: overlayInfoOptions,
      handleCancel: handleCancelOverlayInfo,
    } as IOverlayInfoExtraProps,
    updateState,
  );
}
  */
  // UVEK-879 check if feature overlay is enabled
  //TODO v8.2 overlay
  // const overlayInfoOptions = utils.getOverlayInfoOptions(
  //   formGridOptions?.overlayOptions,
  //   eventValue,
  // );
  // if (
  //   props.features?.feature_form_elements_with_editing_overlay &&
  //   overlayInfoOptions?.isShowOverlay
  // ) {
  //   // display overlay
  //   const handleCancelOverlayInfo = () => {
  //     utils.setValue(state, jsonpath, null);
  //     afterChange(jsonpath, id, instant);
  //   };
  //   setOverlayFormInfo(overlayInfoOptions, handleCancelOverlayInfo);
  // }
  // }

  // updateState(StatesToUpdateEnum.ALL);
};

//cancel pending updateOnChange requests
export const cancelUpdateOnChange = () => {
  if ((global as any).debounce) {
    (global as any).debounce = undefined;
  }
};


/**
 * Verarbeitet Validierungsfehler beim Dateiupload (HTTP 422). submit und save
 * Ermittelt anhand des `errorType` die passende Fehlermeldung und weist diese
 * dem entsprechenden Formular-Element zu.
 *
 * @param responseData Die Fehlerantwort vom Backend, die `filename`, `jsonPath` und `errorType` enthalten kann.
 * @param formState Der aktuelle Zustand des Formulars (FormState), der aktualisiert wird.
 * @param jsonPath Ein optionaler Fallback-JSON-Pfad, falls dieser in `responseData` fehlt. wenn nicht gesetzt -> submit (anonym user)
 * @returns Die übersetzte Validierungs-Fehlermeldung.
 */
export const handleFileUploadValidationError = ( 
  responseData: any,
  formState: FormState,
  jsonPath?: string,
): string => {
  const filename = responseData?.filename as string | undefined;
  const jsonPathFinal = responseData?.jsonPath ? responseData.jsonPath : jsonPath;
  const errorType = responseData?.errorType as FileUploadErrorTypeEnum | undefined;
  
  let i18nKey = 'form_overlay_errorOnSubmit_fileupload_failed';
  
  if (errorType) {
    switch(errorType) {
      case FileUploadErrorTypeEnum.FILE_TOO_LARGE:
        i18nKey = 'form_overlay_errorOnSubmit_fileupload_failed_file_too_large';
        break;
      case FileUploadErrorTypeEnum.MIME_TYPE_NOT_ALLOWED:
        i18nKey = 'form_overlay_errorOnSubmit_fileupload_failed_mime_type_not_allowed'; 
        break;
      case FileUploadErrorTypeEnum.MIME_TYPE_UNKNOWN:
        i18nKey = 'form_overlay_errorOnSubmit_fileupload_failed_mime_type_unknown'; 
        break;
      case FileUploadErrorTypeEnum.FILE_UPLOAD_FAILED:
        i18nKey = 'form_overlay_errorOnSubmit_fileupload_failed';
        break;
      default:
        i18nKey = 'form_overlay_errorOnSubmit_fileupload_failed';
        break;
    }
  }
  const fileUploadError = i18n.t(i18nKey);
  const validationMessage = filename ? `${fileUploadError} (${filename})` : fileUploadError;
  // Entfernt ".value", ".values", ".value[index]" oder ".values[index]" am Ende des Pfades, um das Eltern-Element zu erhalten
  const jsonPathBinary = jsonPathFinal ? jsonPathFinal.replace(/\.values?(\[\d+\])?$/, '') : jsonPathFinal;
  let datafieldUploadElement = utils.getElementByJsonPath(formState.data, jsonPathBinary);

  if (datafieldUploadElement) {
      datafieldUploadElement.validationMessage = validationMessage;
      formState.data.hasValidationErrors = true;
      utils.setStateElementByJsonpath(formState.data, jsonPathBinary, datafieldUploadElement);
  }

  return validationMessage;
};

//if a binary element changes, it leads to a possible binary upload if the user is signed in
//and all uploads are (client side) valid
export async function possibleBinaryUpload(
  formState: FormState,
  uiState: UIState,
  jsonpath: string,
  id: string,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  isLoggedIn?: boolean,
  formGridOptions?: IFormGridOptions,
  editable?: boolean,
) {

  if (formState.submitted || (global as any).submitted) return;

  const onBinaryChangeFinished = () => {
    uiState.loading = false;
    updateState(StatesToUpdateEnum.ALL);
  };

  if (
      formState.fileUploads &&
      formState.fileUploads[id] &&
      formState.fileUploads[id][jsonpath]
  ){
    //validate Field (client side) - if ok AND loggedin, we trigger a (fullscreen) save request and upload the files
    if (
      validationUtils.validateField(
        formState,
        jsonpath,
        id,
        true,
        formGridOptions,
      ).isValid
      && isLoggedIn
    ) {
      const actionAfterSuc = async () => {
        onBinaryChangeFinished();
        await afterChange(formState, uiState, restService, jsonpath, id, true, updateState, formGridOptions, editable);
      };

      const actionAfterError = (res?: ApiResponse) => {
        onBinaryChangeFinished();

        const responseData = res?.responseData;
        
        if (res?.responseCode === 422 && responseData) {
          handleFileUploadValidationError(responseData, formState, jsonpath);
        } else {
          // something went wrong (most probably backend file-check), we set a generic error message
          const binaryElem = utils.getElementByJsonPath(formState.data, jsonpath);
          if (binaryElem) {
            binaryElem.validationMessage = i18n.t('form_binary_upload_failed');
          }
        }        
        updateState(StatesToUpdateEnum.ALL);
      };
      try {      
        onBinaryChangeFinished();

        console.log('handleBinaryFormSave');
        await saveFormBinary(
          formState,
          uiState,
          updateState,
          restService,
          actionAfterSuc,
          actionAfterError,
        );
      } catch (error) {
        console.error('Error during binary upload: ', error);
        onBinaryChangeFinished();
      }
    }
    else{
      onBinaryChangeFinished();
    }
  }
  else{
    onBinaryChangeFinished();
  }
}


export async function handleBinaryAdd(
  formState: FormState,
  uiState: UIState,
  jsonpath: string,
  id: string,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  filesToAdd: File[],
  isLoggedIn?: boolean,
  formGridOptions?: IFormGridOptions,
  editable?: boolean  
) {
 
  let fileArray: IBinary[] = [];
  let isList = false;
  const savedVal = JSON.parse(
    JSON.stringify(utils.getValue(formState, jsonpath, id)),
  );

  if (utils.isListElement(formState.schema, id)) {
    const oldValue = utils.getValue(formState, jsonpath, id);
    if (oldValue != null && Array.isArray(oldValue)) {
      fileArray = oldValue;
    }
    isList = true;
  }
  //init formStateFileuploads
  if (!formState.fileUploads) formState.fileUploads = {};
  if (!formState.fileUploads[id]) formState.fileUploads[id] = {};
  if (typeof formState.fileUploads[id][jsonpath] === 'undefined' || !isList) {
    formState.fileUploads[id][jsonpath] = [];
  }

  if(!isList && filesToAdd.length > 1){
    filesToAdd = filesToAdd.slice(0,1);
  }

  //not loggedin, special data entry and files added to fileUploads, loggedint- empty data entry, will be filled after saving
  filesToAdd.forEach((file, index) => {
    const binaryMetaInfo: IBinary = {
      fileName: file.name,
      size: file.size,
      value: 'mesh(not uploaded yet): ' + uuidv4(),
    };
    fileArray.push(binaryMetaInfo);
    formState.fileUploads[id][jsonpath][fileArray.length - 1] = file;
  });
  

  if (fileArray.length === 0) {
    utils.removeValue(formState, jsonpath);
  } else {
    if (utils.isListElement(formState.schema, id)) {
      utils.setValue(formState, jsonpath, fileArray);
    } else {
      utils.setValue(formState, jsonpath, fileArray[0]);
    }
  }

  
  //if logged in and everything is ok, files will be uploaded
  //at least afterChanged will be called for updateOnChange events
  await possibleBinaryUpload (
    formState,
    uiState,
    jsonpath,
    id,
    updateState,
    restService,
    isLoggedIn,
    formGridOptions,
    editable  
  )
}

export async function handleBinaryDelete(
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  jsonpath: string,
  binaryFieldID: string,
  index: number,
  toDelete: string,
  isLoggedIn?: boolean,
  formGridOptions?: IFormGridOptions,
  editable?: boolean,
) {
  console.log("delete binary");
  let value = utils.getValue(formState, jsonpath);
  if (value != null) {
    if (Array.isArray(value)) {
      // Remove exactly the entry at the given index from the values array
      if (index >= 0 && index < value.length) {
        value.splice(index, 1);
      }
      if (value.length === 0) {
        utils.removeValue(formState, jsonpath);
      } else {
        utils.setValue(formState, jsonpath, value);
      }
    } else {
      // Single-file field: clear the value entirely
      utils.removeValue(formState, jsonpath);
      if (formState.fileUploads && formState.fileUploads[jsonpath]) {
        formState.fileUploads[jsonpath] = [];
      }
    }
    // Keep fileUploads in sync: remove the same index from the uploads array
    if (
      formState.fileUploads &&
      formState.fileUploads[binaryFieldID] &&
      formState.fileUploads[binaryFieldID][jsonpath]
    ) {
      formState.fileUploads[binaryFieldID][jsonpath].splice(index, 1);
    }    
  }


  //even after a file-delete it could be that a binary field is now valid, so it can be uploaded
  await possibleBinaryUpload (
    formState,
    uiState,
    jsonpath,
    binaryFieldID,
    updateState,
    restService,
    isLoggedIn,
    formGridOptions,
    editable  
  )

}



//TODO v8.2 overlay
/**
 * Redirect user to configured cancel url or otherwise call andpHistoryackCallback
 */
export const cancelRedirect = (uiState?: UIState) => {
  if(Platform.OS == "web"){
    var url = document.getElementById('egov-template--aside')?.dataset.cancelurl;
    if (url && url !== '') {
      window.location.href = url;
    } else {
      andpHistoryBackCallback();
    }
  }else{
    if(uiState) uiState?.andpNavigation.previous();
  }
};

/**
 * Redirect user to previous page if set in session storage
 * else redirect user to start page /
 */
export function andpHistoryBackCallback() {
  const url = sessionStorage.getItem('ANDP.previousPage') ?? '/';
  console.log(`redirecting to previous page ${url}`);
  window.location.href = url;
}

/**
 *
 * @param uiState
 * @param formState
 * @param updateState
 * @param restService
 * @param actionAfterSave for example for await loadDialogForm()
 * @param actionAfterError if nothing is defined and "showError" is true -> the default behavior will be triggered
 * @param showError
 * @returns
 */
export const saveForm = async (
  uiState: UIState,
  formState: FormState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  savePageIndex: boolean,
  uploadBinaries: boolean,
  actionAfterSave?: () => Promise<void>,
  actionAfterError?: (response: ApiResponse) => void,
  showError?: boolean,
  overlayLabel?: IOverlayFullScreenTextEnum,
) => {
  console.log('handle saveForm');
  if (savePageIndex) {
    // savePageIndex nur auf FormScreen beziehen
    if (uiState.flowList[uiState.flowPageIndex].reactClass == 'FormScreen') {
      formState.data.savedPageIndex = uiState.currentPage;
    }
  }
  // send the keep alive request before attempting the save
  // this will refresh the authentication if needed
  await restService.keepAlive();

  let fileUploadHash = 0;
  if (formState.fileUploads) {
    fileUploadHash = utils.hashCode(JSON.stringify(formState.fileUploads));
  }

  //this information is set to avoid autosave (or additional autosave) while a save request is running
  utils.startANRequest(() => {
    (global as any).saveRunning = true;
    if(showError){
      overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.FULL_SCREEN_LOADING,
      updateState,
      { loadingText:  overlayLabel},
    );
    }
  }, true);
  if (CaptchaService.hasExecute()) {
    await CaptchaService.execute();
  }
  let data = utils.prepareDataForSave(formState, uploadBinaries);
  let apiResponse: ApiResponse = await restService.loadSaveFormRequest(
    data,
    formState,
  );
  utils.endANRequest(uiState, updateState, () => {
    (global as any).saveRunning = false;
    if ((global as any).valueIsChanged) {
      (global as any).valueIsChanged = undefined;
    }
  });

  const errorFunc = function (res: ApiResponse) {
    if (showError) {
      if (isCaptchaError(res)) {
        overlayUtils.setOverlay(
          uiState,
          formState,
          OverlayTypeEnum.LOAD_CAPTCHA_ERROR,
          updateState,
          { response: res },
        );
        return;
      }
      if (!actionAfterError) {
        overlayUtils.setOverlay(uiState, formState, OverlayTypeEnum.LOAD_SAVE, updateState, {response: res});
      } else {
        actionAfterError(res);
      }
    }
  };

  if (!apiResponse.success) {
    if (uiState.loading != false) {
      uiState.loading = false;
      updateState(StatesToUpdateEnum.ALL);
    }
    
    errorFunc(apiResponse);
    
    return;
  } else {
    //reset fileUploads if hash is still the same - so binaries are not uploaded again
    if (formState.fileUploads) {
      let currentHash = utils.hashCode(JSON.stringify(formState.fileUploads));
      if (fileUploadHash === currentHash) {
        // The Mesh UUIDs have been injected into the form data object,
        // so we need to update the state.
        const saveResponse = apiResponse.responseData;
        // set updated values to fileupload fields
        for (let fileUpload in formState.fileUploads) {
          const jsonPathToUploadField = Object.keys(
            formState.fileUploads[fileUpload],
          )?.[0];
          if (jsonPathToUploadField) {
            const elementStateSaved = utils.getElementByJsonPath(
              saveResponse,
              jsonPathToUploadField,
            );
            const elementState = utils.getElementByJsonPath(
              formState.data,
              jsonPathToUploadField,
            );
            if (
              elementState &&
              (elementStateSaved?.value || elementStateSaved?.values)
            ) {
              if (Array.isArray(elementStateSaved?.values)) {
                elementState.values = elementStateSaved.values;
              } else {
                elementState.value = elementStateSaved.value;
              }
              utils.setStateElementByJsonpath(
                formState.data,
                jsonPathToUploadField,
                elementState,
              );
            }
          }
        }

        formState.fileUploads = {};
        updateState(StatesToUpdateEnum.FormState);
      }
    }
    // Weitere Aktionen was man nach save aufruft
    if (actionAfterSave) {
      await actionAfterSave();
    }
  }
  if (savePageIndex) {
    delete formState.data.savedPageIndex;
  }
};
/**
 * //TODO v8.2 Überprüfung
 * normal save-request, but a fullscreen-Overlay is set before running the request
 * @param formState
 * @param uiState
 * @param updateState
 * @param restService
 * @param actionAfterSuc
 * @param actionAfterError
 */
export const saveFormBinary = async (
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  actionAfterSuc: any,
  actionAfterError: any,
) => {
  const callbackFunc = async () => {
    overlayUtils.hideOverlay(uiState, updateState);
    if (actionAfterSuc) {
      await actionAfterSuc();
    }
  };

  const errorFunc = async (res?: ApiResponse) => {
    overlayUtils.hideOverlay(uiState, updateState);
    if (actionAfterError) {
      await actionAfterError(res);
    }
  };

  await saveForm(
    uiState,
    formState,
    updateState,
    restService,
    false,
    true,
    callbackFunc,
    errorFunc,
    formState.isLoggedIn,
    IOverlayFullScreenTextEnum.FileUpload

  );
};
/**
 *
 *
 * @param formState
 * @param uiState
 * @param updateState
 * @param restService
 * @param actionAfterSuc method after save is successfuls
 * @param showError
 * @returns
 */

export const handleBackgroundFormSave = async (
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  actionAfterSuc: any,
) => {
  await saveForm(
    uiState,
    formState,
    updateState,
    restService,
    false, //Don't save current page index?
    false, 
    actionAfterSuc,
    undefined, //function () {}, => here it should be undefined, because () {} is a function... // No error function after it failed
    false,
  );
};

export const loadDialogForm = async (
  uiState: UIState,
  formState: FormState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  initForm: (uiState: UIState, formState: FormState, data?: any, newFormId?: string) => void,

  refreshPropertyID: string,
  scrollReference: string,

  editModeDialogFormID: string,
  parentDialogPortalFormHostObjectID: string,
  aggregatePropertyID?: string,
  rowId?: string,
  duplicateRowId?: string,
  createAfterIndex?: number,

  mockForm?: boolean,
) => {
  const successLoadDialog = (data: any) => {
    console.log('Dialog loaded successfully');
    // Klone den aktuellen FormState:
    uiState.dialogForm.refreshPropertyID = refreshPropertyID;
    uiState.dialogForm.scrollReference = scrollReference;
    uiState.dialogForm.mainFormState = {
      uiState: JSON.parse(JSON.stringify(uiState)),
      formState: JSON.parse(JSON.stringify(formState)),
    };
    uiState.dialogForm.isDialogFormOpen = true;
    uiState.currentPage = 0;
    uiState.loading = false;
    formState.formid = data.data.formID;
    overlayUtils.hideOverlay(uiState);
    initForm(uiState, formState, data);
  };

  let loadMockForm = mockForm;
  if (loadMockForm) {
    mockFormularUtils.fetchSubFormular(successLoadDialog);
    return;
  }

  utils.startANRequest(() =>
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.FULL_SCREEN_LOADING,
      updateState,
      { loadingText: IOverlayFullScreenTextEnum.DialogForm },
    ),
  );
  const apiResponse: ApiResponse = await restService.ANLoadDialogForm(
    formState,
    editModeDialogFormID,
    parentDialogPortalFormHostObjectID,
    aggregatePropertyID,
    rowId,
    duplicateRowId,
    createAfterIndex,
  );
  utils.endANRequest(uiState, updateState);

  if (!apiResponse.success) {
    const errorLoadDialog = () => {
      uiState.loading = false;
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        undefined,
        { loadingText: IOverlayFullScreenTextEnum.DialogForm },
      );
      updateState(StatesToUpdateEnum.ALL);
      console.error('Error loading dialog form');
    };

    errorLoadDialog();
  } else {
    successLoadDialog(apiResponse.responseData);
  }
};



export const handleExternalSubmit = async (
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  isMock: boolean = false,
  handleFormCancel: any
) => {
  if (validationUtils.isPageValid(formState, true, uiState.currentPage, isMock)) {
    //clone state
    const formData: IData = JSON.parse(
      JSON.stringify(formState.data)
    );

    //redirect after successful validation(and set redirect url)
    const afterValidation = (
      valid?: boolean,
      data?: IData
    ) => {
      if (
        valid &&
        data &&
        data?.formControls?.button?.targetUrl !== ""
      ) {
        //delete saved data before redirect to avoid
        //form not callable anymore because of auto-redirect on start
        handleFormCancel(function () {
          utils.redirectFormRedirect(data);
        }, false, true);
      }
    };

    validationUtils.loadValidationWithHooks(
      restService,
      formState,
      uiState,
      updateState,
      afterValidation,
      undefined,
      isMock
    );
  } else {
    //update state to show error messages
    updateState(StatesToUpdateEnum.ALL);
  }

}


export const handleSubmitForm = async (
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  isMock: boolean = false,
  afterSuccSubmit?: () => void,
) => {
  if (isMock == true) {
    mockFormularUtils.fetchDefaultFormular(afterSuccSubmit); // TODO v8.2
    return;
  }

  const afterValidation = async (valid?: boolean) => {
    if (!valid) {
      uiState.loading = false;
      updateState(StatesToUpdateEnum.UIState);
      setTimeout(function () {
        formState.handleFocusError();
      }, 500);
      return;
    }
    uiState.loading = true;
    updateState(StatesToUpdateEnum.UIState);
    await submitForm(
      formState,
      uiState,
      updateState,
      restService,
      afterSuccSubmit,
    );
  };
  // Submit-Button should validate, if we are on the last formpage
  if (
    uiState.flowList[uiState.flowPageIndex].reactClass == 'FormScreen' &&
    uiState.currentPage == uiState.lastPage - 1
  ) {
    let clientSideValid = validationUtils.isPageValid(
      formState,
      true,
      uiState.currentPage,
      isMock,
    );
    if (!uiState.showErrorBlockOnInvalid) {
      uiState.showErrorBlockOnInvalid = true;
    }
    if (!clientSideValid) {
      formState.data.hasValidationErrors = true;
      uiState.loading = false;
      updateState(StatesToUpdateEnum.ALL);
      setTimeout(function () {
        formState.handleFocusError();
      }, 650);
      return;
    }

    formState.data.hasValidationErrors = false;

    uiState.loading = true;
    updateState(StatesToUpdateEnum.ALL);

    
    await validationUtils.loadValidationWithHooks(
      restService,
      formState,
      uiState,
      updateState,
      afterValidation,
      undefined,
      isMock,
    );
  } else {
    afterValidation(true);
  }
};

export const submitForm = async (
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  afterSuccSubmit?: () => void,
) => {
  const dataToSend: FormData = utils.prepareDataForSave(formState, true);
  const handleSuccess = (res: ApiResponse) => {
    CaptchaService.reset();
    formState.submitted = true;
    (global as any).submitted = true;
    try {
      const responseBody: IValidation = JSON.parse(res.responseData);
      if (responseBody.preconditions) {
        formState.data.preconditions = responseBody.preconditions;
        uiState.loading = false;
        formState.submitted = false;
        (global as any).submitted = false;
        updateState(StatesToUpdateEnum.ALL);
        return;
      }
    } catch (e) {
      // ignore failed json parsing
    }
    //reset touched, so no beforeUnload-Message is shown
    if (typeof (global as any).valueIsChanged != 'undefined') {
      (global as any).valueIsChanged = undefined;
    }
    uiState.loading = false;
    updateState(StatesToUpdateEnum.ALL);
    if (afterSuccSubmit) {
      afterSuccSubmit();
      return;
    }
  };

  const handleError = (res: ApiResponse) => {
    uiState.loading = false;
    const responseData = res.responseData;

     if (isCaptchaError(res)) {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.LOAD_CAPTCHA_ERROR,
        updateState,
        { response: res },
      );
      return;
    }

     if (res.responseCode === 422 && responseData) {
      const validationMessage = handleFileUploadValidationError(responseData, formState);
      const errorMsg: IErrorMessage = {
          validationMessage,
          pageNumber: uiState.currentPage,
          elementInfo: { elementID: "random-dump" },
        };
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.SUBMIT_USER_INPUT_FAILURE,
        undefined,
        { errorMsgs: [errorMsg], response: res },
      );
      updateState(StatesToUpdateEnum.ALL);
      return;
    }

    // Kombiniert: Exception/kein JSON ODER HTTP 400 -> LOAD_SUBMIT Overlay
    const showLoadSubmit =
      res.exception || !responseData || res.responseCode === 400;

    if (showLoadSubmit) {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.LOAD_SUBMIT,
        updateState,
        { response: res },
      );

      // Bei 400 (File Upload fehler) optional den zurückgelieferten Form-State übernehmen
      if (
        res.responseCode === 400 &&
        responseData &&
        typeof responseData.formID !== 'undefined'
      ) {
        formState.data = responseData;
        updateState(StatesToUpdateEnum.FormState);
      }
      return;
    }

    // Übrige validierte Fehler
    if (responseData?.hasValidationErrors === true) {
      const errorMessages = utils.getErrorMessages(
        responseData.properties,
        formState.form.pages,
      );
      formState.data = responseData;
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.SUBMIT_USER_INPUT_FAILURE,
        undefined,
        { errorMsgs: errorMessages, response: res },
      );
      updateState(StatesToUpdateEnum.ALL);
      return;
    }
  };
  await hookUtils.preSubmitForm(
    formState.formid,
    dataToSend,
    formState,
    uiState,
    updateState,
    restService,
  );
  utils.startANRequest(() =>
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.FULL_SCREEN_LOADING,
      updateState,
      { loadingText: IOverlayFullScreenTextEnum.Submit },
    ),
  );
  const apiResponse: ApiResponse = await restService.submitRequest(
    dataToSend,
    formState,
  );
  utils.endANRequest(uiState, updateState);
  if (!apiResponse.success) {
    handleError(apiResponse);
  } else {
    handleSuccess(apiResponse);
  }
  await hookUtils.postSubmitForm(
    formState.formid,
    dataToSend,
    formState,
    uiState,
    updateState,
    restService,
  );
};
// aka handleRefreshForm
export const revertToMainFormFromDialogForm = async (
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
  initForm: (uiState: UIState, formState: FormState, data?: any, newFormId?: string) => void,
) => {
  const successRefresh = (data: any) => {
    console.log('refreshForm successful', data);
    const scrollReference = uiState.dialogForm.scrollReference;
    // reset dialogForm info
    if (!uiState.dialogForm.mainFormState?.uiState.currentPage) {
      uiState.currentPage = 0;
    } else {
      uiState.currentPage =
        uiState.dialogForm.mainFormState?.uiState.currentPage;
    }
    uiState.lastPage = uiState.dialogForm.mainFormState?.uiState.lastPage || 1;
    uiState.dialogForm = {};
    formState.submitted = false;
    uiState.loading = false;
    overlayUtils.hideOverlay(uiState);
    (global as any).valueIsChanged = true;
    initForm(uiState, formState, data);
    // uiState.fullScreenOverlay = false; // TODO v8.2 overlay
    // TODO v8.2
    // if (scrollReference) {
    //   document.getElementById(scrollReference)?.scrollIntoView();
    // }
  };
  const errorRefresh = () => {
    uiState.loading = false;
    overlayUtils.hideOverlay(uiState, updateState);
    console.log('refreshForm Error');
  };

  if (uiState.isMockForm) {
    mockFormularUtils.fetchDefaultFormular(successRefresh);
    return;
  }

  const mainState = uiState.dialogForm.mainFormState;
  if (!mainState) {
    console.error('Unusual State!');
    return;
  }
  utils.startANRequest(() =>
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.FULL_SCREEN_LOADING,
      updateState,
    ),
  );
  const apiResponse: ApiResponse = await restService.ANRefreshFormRequest(
    formState,
    mainState.formState.formid,
    mainState.formState.data.DialogPortalFormHostObjectID!,
    mainState.uiState.dialogForm.refreshPropertyID!,
    mainState.formState.businessObjectID,
  );
  utils.endANRequest(uiState, updateState);
  if (!apiResponse.success) {
    errorRefresh();
  } else {
    successRefresh(apiResponse.responseData);
  }
};