import { AxiosInstance, AxiosRequestConfig } from 'axios';
import {
  FormState,
  IData,
  ApiResponse,
  UIState,
  IDialogFormInitBody,
} from '../types/interfaces';
import { CaptchaService } from './CaptchaService';
import * as gzip from 'gzip-js';
import createAxiosClient from '@andp/core/src/services/AxiosClient';
import axios from 'axios';
import { PluginNameEnum } from '../constants/enums/PluginNameEnum';
import {
  fetchMockLazyCatalog,
  fetchMockSearch,
  fetchMockAutoComplete
} from '../utils/MockFormularUtils';
import { getFeatureActive, isFeatureActive } from '../utils/Utils';
import { Platform, Linking } from 'react-native';

export class RestService {
  anBasePath: string;
  apiToken?: string;
  axiosClient: AxiosInstance;
  initUrl: string;
  fullCulture: string | null; // something like de-AT, en-CH, etc.
  pluginName: PluginNameEnum;
  languageParam: string | null | undefined;
  FORMS_DATA_COOKIE: string = "forms-data";

  constructor(meshPath: string, pluginName: PluginNameEnum, apiToken?: string, cultureParam?: string, languageParam?: string) {
    this.pluginName = pluginName;
    this.anBasePath = this.initBasePath(meshPath);
    this.apiToken = apiToken;
    this.axiosClient = createAxiosClient(this.anBasePath);
    this.initUrl = meshPath;
    if(cultureParam && cultureParam.length > 2){
      this.fullCulture = cultureParam;
    }else{
      this.fullCulture = null;
    }
    this.languageParam = languageParam;
  }

  getFullCulture() : string | null {
    return this.fullCulture ? this.fullCulture : null;
  }

  setFullCulture(cultureParam: string) {
    if(cultureParam && cultureParam.length > 2){
      this.fullCulture = cultureParam;
    }else{
      console.error("Something went wrong with setCulture!");
    }
  }

  initBasePath(meshHost: string): string {
    return meshHost + '/api/' + this.pluginName + '/';
  }

  private appendLangParam(url: string): string {
    if (this.pluginName === PluginNameEnum.CMS_FORMS && this.languageParam) {
      const separator = url.includes('?') ? '&' : '?';
      return url + separator + 'lang=' + this.languageParam;
    }
    return url;
  }

  /**
   * Liest den Wert eines bestimmten Cookies aus dem Browser (document.cookie).
   * 
   * Der verwendete reguläre Ausdruck `(?:^|; )' + name + '=([^;]*)` stellt Folgendes sicher:
   * - `(?:^|; )`: Matcht entweder den Anfang des Strings oder die Standard-Cookie-Trennzeichen (Semikolon + Leerzeichen), um exakte Namensübereinstimmungen zu garantieren.
   * - `name`: Der exakte Name des gesuchten Cookies.
   * - `=([^;]*)`: Fängt den eigentlichen Wert (alles nach dem '=' bis zum nächsten ';') in die Capturing Group (match[1]) ein.
   *
   * @param name Der Name des auszulesenden Cookies.
   * @returns Der decodierte Wert des Cookies oder null, falls er nicht existiert (oder nicht im Web ausgeführt wird).
   */
  private readCookie(name: string): string | null {
    if (Platform.OS !== 'web' || typeof document === 'undefined') return null;
    const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return match ? decodeURIComponent(match[1]) : null;
  }

  async post(
    formId: string,
    url: string,
    body?: any,
    requestHeader?: any,
    formState?: FormState,
  ): Promise<ApiResponse> {
    url = this.appendLangParam(url);
    //TODO v8.2 - compress logic
    //a few calls are never compressed
    let bodyForRequest: any;
    if (
      url.indexOf('/save') === -1 &&
      url.indexOf('/submit') === -1 &&
      url.indexOf('/triggerForm') === -1 &&
      url.indexOf('/csvupload') === -1 &&
      url.indexOf('/deleteFile') === -1
    ) {
      //cleanup request before it's sent (remove values not needed in BO)
      const bodyClone = JSON.parse(JSON.stringify(body));
      this.cleanUp(bodyClone);
      delete bodyClone.possibleValueSets;
      bodyForRequest = bodyClone;
      if(Platform.OS == "web" && this.pluginName == PluginNameEnum.ACTA_NOVA){
        if (
          url.indexOf('/csvupload') === -1 &&
          formState != null &&
          isFeatureActive('feature_compress', formState.features) &&
          new URLSearchParams(window.location.search).get('nogzip') === null // TODO MOBILE
        ) {
          let feature_compress = getFeatureActive(
            'feature_compress',
            formState.features,
          );
          if (feature_compress.hasOwnProperty('compressThresholdKBytes')) {
            bodyForRequest = JSON.stringify(bodyClone);
            let threshold: number = parseInt(
              (feature_compress as any).compressThresholdKBytes,
              10,
            );
            if (bodyForRequest.length / 1000 > threshold) {
              //gzip first
              let zipped = gzip.zip(bodyForRequest);
              //then base64 encode and add #ZIP# in front of zipped body
              bodyForRequest = '#ZIP#' + Buffer.from(zipped).toString('base64');
            }
          }
        }
      }

      
    } else {
      bodyForRequest = body;
    }
    let headers = { ...requestHeader }; // if requestHeader -> headers with empty object
    if (this.pluginName === PluginNameEnum.ACTA_NOVA) {
      const context = {
        culture: this.fullCulture,
        formID: formId,
      };
      headers['X-User-Context'] = JSON.stringify(context);
    }
    // Check if manual content type is given --> content type application/json
    if (!headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }
    // for debugging with a specific user
    // login at portal
    // copy AUTH cookie content
    // Set as env AUTH_TOKEN;
    if (__DEV__ && this.apiToken != undefined && this.apiToken != '') {
      headers['Authorization'] = 'Bearer ' + this.apiToken;
    }
    Object.assign(headers, CaptchaService.getRequestHeaders());

    let requestConfig: AxiosRequestConfig = {
      baseURL: this.anBasePath,
      headers: headers,
    };
    let response;
    try {
      response = await axios.post(url, bodyForRequest, requestConfig);
      // The following code will only be executed if status code in the 2xx range
      const data = await response.data;
      this.checkResponseBodyIsValidJson(data);
      return {
        success: true,
        responseCode: response.status,
        responseData: data,
      };
    } catch (error: any) {
      if (axios.isAxiosError(error)) {
        if (error.response) {
          // The server responded with a status code outside the 2xx range
          // debug information is saved in responseData
          return {
            success: false,
            responseCode: error.response.status,
            responseData: error.response.data,
          };
        } else {
          //Network error occurs
          return {
            success: false,
            responseCode: 500,
            exception: error,
          };
        }
      } else if (error instanceof SyntaxError) {
        // Json parsing error
        return {
          success: false,
          responseCode: response!.status,
          exception: error,
        };
      } else {
        return {
          success: false,
          responseCode: 500,
          exception: error,
        };
      }
    }
  }

  async get(
    url: string,
    requestHeader?: any,
    formId?: string,
  ): Promise<ApiResponse> {
    url = this.appendLangParam(url);
    let headers = { ...requestHeader }; // if requestHeader -> headers with empty object

    if (formId && this.fullCulture && this.pluginName === PluginNameEnum.ACTA_NOVA) {
      const context = {
        culture: this.fullCulture,
        formID: formId,
      };
      headers = { 'X-User-Context': JSON.stringify(context) };
    }

    // for debugging with a specific user
    // login at portal
    // copy AUTH cookie content
    // Set as env AUTH_TOKEN;

    if (__DEV__ && this.apiToken != undefined && this.apiToken != '') {
      headers['Authorization'] = 'Bearer ' + this.apiToken;
    }


    let requestConfig: AxiosRequestConfig = {
      baseURL: this.anBasePath,
      headers: headers,
    };

    try {
      const response = await axios.get(url, requestConfig);
      const data = response.data;
      this.checkResponseBodyIsValidJson(data);
      return {
        success: true,
        responseCode: response.status,
        responseData: data,
      };
    } catch (error: any) {
      if (axios.isAxiosError(error)) {
        if (error.response) {
          // The server responded with a status code outside the 2xx range TODO v8.2 internalErrorCode only for formgen for error handlign later
          return {
            success: false,
            responseCode: error.response.status,
            responseData: error.response.data,
          };
        } else {
          //Network error occurs
          return {
            success: false,
            responseCode: 500,
          };
        }
      } else if (error instanceof SyntaxError) {
        // Json error
        return {
          success: false,
          responseCode: 500,
          responseData: error.message,
        };
      } else {
        return {
          success: false,
          responseCode: 500,
          responseData: error.message,
        };
      }
    }
  }

  /**
   * checks whether the response body is a valid json object
   * @param responseBody
   * @returns
   */
  private checkResponseBodyIsValidJson(responseBody: any): any {
    try {
      let data =
        typeof responseBody === 'string'
          ? JSON.parse(responseBody)
          : responseBody;
    } catch (exception) {
      return new SyntaxError('Invalid json format of response body');
    }
  }

   /**
   * object which should be cleaned before sent to ActaNova (it's called recursively)
   *
   * @param obj data
   */
  cleanUp(obj: any) {
    var that = this;
    if (Array.isArray(obj)) {
      Object.entries(obj).map(function ([k, v]) {
        that.cleanUp(v);
        // eslint-disable-next-line
        return;
      });
    } else if (obj && typeof obj === 'object') {
      if (obj.hasOwnProperty('editable')) {
        delete obj.editable;
      }
      if (obj.hasOwnProperty('visible')) {
        delete obj.visible;
      }
      if (obj.hasOwnProperty('autoCompleteResults')) {
        delete obj.autoCompleteResults;
      }
      if (obj.hasOwnProperty('possibleValues')) {
        delete obj.possibleValues;
      }
      if (obj.hasOwnProperty('possibleAggregateValues')) {
        delete obj.possibleAggregateValues;
      }
      Object.entries(obj).map(function ([key, value]) {
        that.cleanUp(value);
        // eslint-disable-next-line
        return;
      });
    }
  }

  loadFormAsync = async (
    formId: string,
    businessObjectID?: string,
    initializeData?: string,
  ): Promise<ApiResponse> => {
    let url = `form/${encodeURIComponent(formId)}`;
    let requestData: IData = { formID: formId };
    // Exsiting only in acta-nova plugin
    if (
      typeof businessObjectID !== 'undefined' &&
      businessObjectID !== null &&
      businessObjectID !== ''
    ) {
      url = url + '/businessObject/' + encodeURIComponent(businessObjectID);
      requestData.businessObjectID = businessObjectID;
    }
    url += '/load';
    if (this.pluginName === PluginNameEnum.CMS_FORMS) {
      const uuid = this.readCookie(this.FORMS_DATA_COOKIE);
      if (uuid) url += '?formDataUuid=' + encodeURIComponent(uuid);
    }
    if (
      initializeData &&
      initializeData != null &&
      initializeData != undefined
    ) {
      requestData.initializeData = JSON.parse(initializeData);
    }
    //TODO v8.2 payment
    if(Platform.OS == "web" && this.pluginName == PluginNameEnum.ACTA_NOVA){
      const queryString = window.location.search;
      if (queryString && queryString.indexOf('payed=true') > 0) {
        url += '?payed=true';
      }
    }
    //let requestBody = `{"formID": "${formId}"}`;

    const response = await this.post(formId, url, requestData);
    return response;
  };

  /**
   * Only send a http post validation request, no handlers for handling diverse situation
   * @param formData
   * @param jsonpath
   * @param pageIndex
   * @returns
   */
  async validationRequest(formData: IData): Promise<ApiResponse> {
    let url = `form/${encodeURIComponent(formData.formID)}/validate`;
    const requestBody = JSON.stringify(formData);
    const response = await this.post(
      formData.formID,
      url,
      requestBody,
    );
    return response;
  }

  async loadSaveFormRequest(data: FormData, formState: FormState) {
    if (
      CaptchaService.isActive() &&
      !CaptchaService.hasExecute() &&
      CaptchaService.getToken() == null
    ) {
      return {
        success: false,
        responseCode: 400,
        responseData: {
          error_code: "CAPTCHA_ERROR"
        }
      } as ApiResponse;
    }
    let url = 'form/' + encodeURIComponent(formState.formid);
    if (
      typeof formState.businessObjectID !== 'undefined' &&
      formState.businessObjectID !== null &&
      formState.businessObjectID !== ''
    ) {
      url +=
        '/businessObject/' + encodeURIComponent(formState.businessObjectID);
    }
    url += '/save';
    if (this.pluginName === PluginNameEnum.CMS_FORMS) {
      const uuid = this.readCookie(this.FORMS_DATA_COOKIE);
      if (uuid) url += '?formDataUuid=' + encodeURIComponent(uuid);
    }
    const header = { 'Content-Type': 'multipart/form-data' };
    const response = await this.post(
      formState.formid,
      url,
      data,
      header,
      formState,
    );
    return response;
  }


  async submitRequest(
    dataToSend: FormData,
    formState: FormState,
  ): Promise<ApiResponse> {
    let url = 'form/' + encodeURIComponent(formState.data.formID);
    if (
      typeof formState.businessObjectID !== 'undefined' &&
      formState.businessObjectID !== null &&
      formState.businessObjectID !== ''
    ) {
      url =
        url +
        '/businessObject/' +
        encodeURIComponent(formState.businessObjectID);
    }
    url = url + '/submit';

    if (this.pluginName === PluginNameEnum.CMS_FORMS) {
      const uuid = this.readCookie(this.FORMS_DATA_COOKIE);
      if (uuid) url += '?formDataUuid=' + encodeURIComponent(uuid);
    }
    if (formState.paymentState) {
      url = url + (url.includes('?') ? '&' : '?') + 'payment=true';
    }
    const apiResponse: ApiResponse = await this.post(
      formState.data.formID,
      url,
      dataToSend,
      { 'Content-Type': 'multipart/form-data' },
      formState,
    );

    return apiResponse;
  }

  async keepAlive() {
    let url = this.anBasePath + 'keepalive';
    return await axios.get(url);
  }
  /**
   * gets json response from features endpoint
   * see config in /mesh/plugins/acta-nova-plugin/features.yml
   */
  ANLoadFeaturesAsync = async (): Promise<ApiResponse> => {
    let url = 'features';
    const response: ApiResponse = await this.get(url);
    return response;
  };

    /** @ANOnly */
  async ANLoadDialogForm(
    formState: FormState,
    editModeDialogFormID: string,
    parentDialogPortalFormHostObjectID: string,
    aggregatePropertyID?: string,
    rowId?: string,
    duplicateRowId?: string,
    createAfterIndex?: number,
  ) {
    const requestData: IDialogFormInitBody = {
      FormID: editModeDialogFormID,
      ParentDialogPortalFormHostObjectID: parentDialogPortalFormHostObjectID,
    };
    if (rowId) {
      requestData.RowID = rowId;
    }
    if (aggregatePropertyID) {
      requestData.PropertyReferenceID = aggregatePropertyID;
    }
    if (duplicateRowId) {
      requestData.DuplicateRowID = duplicateRowId;
    }
    if (createAfterIndex !== undefined && createAfterIndex !== null) {
      requestData.CreateAfterIndex = createAfterIndex;
    }

    const path = `initializeDialog`;

    const response: ApiResponse = await this.post(
      formState.formid,
      path,
      JSON.stringify(requestData),
      { 'Content-Type': 'text/plain;charset=UTF-8' },
      formState,
    );

    return response;
  }

 /**
   * Sendet einen Multipart-Form-Upload an den CSV-Endpoint.
   * Liefert das rohe Response-Objekt zurück, ohne direkt zu parsen oder Validierung zu triggern.
   * @ANOnly
   */
  public async ANCsvUploadRequest(
    formState: FormState,
    formData: FormData,
  ): Promise<ApiResponse> {
    const url = this.anBasePath + 'csvupload';
    const header = { 'Content-Type': 'multipart/form-data' };
    const response = await this.post(
      formState.formid,
      url,
      formData,
      header,
      formState,
    );
    return response;
  }

  async deleteFileRequest(formState: FormState, body: IData): Promise<ApiResponse> {
    let url = `form/${encodeURIComponent(formState.formid)}/deleteFile`;
    return this.post(formState.formid, url, body, {}, formState);
  }

  async cancelFormRequest(formState: FormState): Promise<ApiResponse> {
    // 
    let data = formState.data;
    let url = 'form/' + encodeURIComponent(formState.formid);
    if (
      typeof formState.businessObjectID !== 'undefined' &&
      formState.businessObjectID !== null &&
      formState.businessObjectID !== ''
    ) {
      url +=
        '/businessObject/' + encodeURIComponent(formState.businessObjectID);
    }
    url += '/cancel';
    if (this.pluginName === PluginNameEnum.CMS_FORMS) {
      const uuid = this.readCookie(this.FORMS_DATA_COOKIE);
      if (uuid) url += '?formDataUuid=' + encodeURIComponent(uuid);
    }
    const response = await this.post(
      formState.formid,
      url,
      data,
    );
    return response;
  }

  /** @ANOnly */
  ANcallExpertSearch(state: FormState, callback: any, error: any) {
    let callbackFunc = function (res: ApiResponse) {
      try {
        // Falls responseData bereits ein String ist, parsen wir ihn;
        // andernfalls wandeln wir ihn in einen String um.
        const responseText =
          typeof res.responseData === 'string'
            ? res.responseData
            : JSON.stringify(res.responseData);
        const data = JSON.parse(responseText);
        callback(data);
      } catch (err) {
        console.error('failed to parse json', err);
        error(err);
      }
    };

    let localstate = state.data;
    localstate.reportResultType = 'json';
    const requestHeaders: HeadersInit = new Headers();
    requestHeaders.set('Content-Type', 'application/json');

    this.post(
      state.formid,
      'reportResult',
      localstate,
      requestHeaders,
    ).then((response) => {
      callbackFunc(response);
    });
  }

  /** @ANOnly */
  ANCallExpertExcelDownload(state: FormState, callback: any, validationError: any, exceptionError: any) {
    const that = this;
    let callbackFunc = function (res: ApiResponse) {
      try {
        // Wir nehmen an, dass res.responseData bereits den zurückgelieferten Text enthält.
        const responseText =
          typeof res.responseData === 'string'
            ? res.responseData
            : JSON.stringify(res.responseData);
        const data = JSON.parse(responseText);
        const searchQueryUUID = data['searchQueryUUID'];
        console.log("data", data.hasValidationErrors);
        if (data.hasValidationErrors) {
          validationError(data);
          return;
        }
        //console.log('searchQueryUUID', searchQueryUUID);
        const statusUrl = `${that.anBasePath}reportResult/${searchQueryUUID}?status`;
        if (
          searchQueryUUID == null ||
          searchQueryUUID == undefined ||
          searchQueryUUID.length <= 0
        ) {
          // if searchquery is null/undefined/empty, it should already return error
          console.error('Request failed - error');
          exceptionError(res);
          return;
        }
        let sumIntervalMaxLength = 120000; // 2 Minutes until return with error
        let intervalLength = 3000;
        let intervalCount = 0;
        const intervalId = setInterval(() => {
          fetch(statusUrl, { method: 'GET' })
            .then((statusRes) => statusRes.text())
            .then((statusText) => {
              try {
                intervalCount += 1;
                const statusData = JSON.parse(statusText);
                if (statusData.hasErrors === false) {
                  clearInterval(intervalId);
                  const downloadUrl = `${that.anBasePath}reportResult/${searchQueryUUID}`;

                  // Web: Download via Blob + <a> (existing behaviour)
                  if (Platform.OS === 'web') {
                    fetch(downloadUrl, { method: 'GET' })
                      .then((downloadRes) => downloadRes.blob())
                      .then((blob) => {
                        const blobUrl = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = blobUrl;
                        a.download =
                          new Date()
                            .toLocaleDateString(state.culture)
                            .split('.')
                            .join('-')
                            .split('/')
                            .join('-')
                            .split(' ')
                            .join('') +
                          '_' +
                          new Date()
                            .toLocaleTimeString(state.culture)
                            .split(':')
                            .join('-')
                            .split(' ')
                            .join('') +
                          '.xlsx';

                        // Füge das Element in den DOM ein (notwendig in Firefox)
                        if (document.getElementById('model-dialog')) {
                          document.getElementById('model-dialog')?.appendChild(a);
                        } else {
                          document.querySelector('.andp-form')?.appendChild(a);
                        }
                        a.click();
                        a.remove();
                        callback();
                      })
                      .catch((exception) => {
                        console.error(exception);
                        clearInterval(intervalId);
                        exceptionError(exception);
                      });
                    return;
                  }

                  Linking.openURL(downloadUrl)
                    .then(() => {
                      // Callback aufrufen, damit z. B. der Ladeindikator entfernt wird
                      callback();
                    })
                    .catch((exception) => {
                      console.error(exception);
                      exceptionError(exception);
                    });
                } else {
                  clearInterval(intervalId);
                  console.error('excel response hasErrors');
                  exceptionError(res);
                }
              } catch (err) {
                // Falls Excel noch nicht bereit ist, passiert hier nichts und es wird im Intervall neu geprüft.
                if (intervalCount * intervalLength > sumIntervalMaxLength) {
                  clearInterval(intervalId);
                  console.error('Timelimit reached.');
                  exceptionError(err);
                }
              }
            })
            .catch((exception) => {
              clearInterval(intervalId);
              console.error(exception);
              exceptionError(exception);
            });
        }, intervalLength);
      } catch (err) {
        console.error('failed to parse json', err);
        exceptionError(err);
      }
    };

    let localstate = state.data;
    localstate.reportResultType = 'excel';
    const requestHeaders: HeadersInit = new Headers();
    requestHeaders.set('Content-Type', 'application/json');

    this.post(
      state.formid,
      'reportResult',
      localstate,
      requestHeaders,
    ).then((response) => {
      callbackFunc(response);
    });
  }

  /** @ANOnly */
  async ANTriggerForm(
    state: FormState,
    buttonId: string,
    parameters: string[],
    callback: any,
    error: any,
  ) {
    const formData: { [key: string]: any } = {
      formID: state.data.formID,
      version: state.data.version ?? '',
      buttonID: buttonId,
      parameter: parameters,
    };

    const requestHeaders: HeadersInit = new Headers();
    requestHeaders.set('Content-Type', 'application/json');

    await this.post(
      state.formid, // FormID aus dem State
      'triggerForm', // URL-Pfad
      JSON.stringify(formData), // Body als JSON
      requestHeaders,
      state,
    )
      .then((response: ApiResponse) => {
        try {
          const responseData =
            typeof response.responseData === 'string'
              ? JSON.parse(response.responseData)
              : response.responseData;
          if (!response.success) {
            error(response);
          } else {
            callback(responseData);
          }
        } catch (err) {
          console.error('failed to parse json', err);
          error(err);
        }
      })
      .catch((err: Error) => {
        error(err);
      });
  }

  /** @ANOnly */
  async ANRefreshFormRequest(
    formState: FormState,
    formId: string,
    dialogPortalFormHostObjectID: string,
    refreshPropertyID: string,
    businessObjectID?: string,
  ) {
    let path = 'refreshForm';
    //TODO v8.2 derzeit funktionierit nur auf web
    const queryString = Platform.OS == "web" ? window.location.search : "";
    if (queryString && queryString.indexOf('payed=true') > 0) {
      path += '?payed=true';
    }

    const requestData: {
      FormID: string;
      DialogPortalFormHostObjectID: string;
      RefreshPropertyReferenceID: string;
      BusinessObjectID?: string;
    } = {
      FormID: formId,
      DialogPortalFormHostObjectID: dialogPortalFormHostObjectID,
      RefreshPropertyReferenceID: refreshPropertyID,
    };

    if (businessObjectID) {
      requestData.BusinessObjectID = businessObjectID;
    }
    const apiResponse: ApiResponse = await this.post(
      formState.formid,
      path,
      JSON.stringify(requestData),
      { 'Content-Type': 'text/plain;charset=UTF-8' },
    );
    return apiResponse;
  }

  // Dropdowns - ANDP-420
  /** @ANOnly */
  ANLoadSearch(
    searchTerm: string,
    data: IData,
    callback: (result: any) => void,
    errorFunc: (error: any) => void,
    formState: FormState,
    uiState: UIState,
    jsonpath?: string,
  ): void {
    const requestHeaders = { 'Content-Type': 'application/json' };

    if (uiState.isMockForm && __DEV__) {
      // Nur im Dev-Modus importieren
      fetchMockSearch()
        .then((data) => callback(data))
        .catch(errorFunc);
      return;
    }

    this.post(data.formID, 'search', data, requestHeaders)
      .then((apiResponse) => {
        callback(apiResponse.responseData);
      })
      .catch((err) => {
        errorFunc(err);
      });
  }

  /** @ANOnly */
  ANLoadLazyCatalog(
    searchTerm: string,
    data: IData,
    callback: (result: any) => void,
    errorFunc: (error: any) => void,
    formState: FormState,
    uiState: UIState,
    jsonpath?: string,
  ): void {
    const requestHeaders = { 'Content-Type': 'application/json' };
    if (
      uiState.isMockForm &&
      jsonpath &&
      jsonpath.includes('G46F9670-8B2A-4731-8A01-9592C0DB6A31') &&
      __DEV__
    ) {
      const mockData = fetchMockLazyCatalog()
        .then((data) => callback(data))
        .catch(errorFunc);
      return;
    }
    // Sende den Request über die post-Methode
    this.post(data.formID, 'lazycatalog', data, requestHeaders)
      .then((apiResponse) => {
        callback(apiResponse.responseData);
      })
      .catch((err) => {
        errorFunc(err);
      });
  }

  /** @ANOnly */
  ANLoadAutocomplete(
    data: IData,
    callback: (result: any) => void,
    errorFunc: (error: any) => void,
    state: FormState,
    jsonpath?: string,
    isMockForm?: boolean
  ): void {
    const requestHeaders = { 'Content-Type': 'application/json' };
    if(isMockForm){
      // Nur im Dev-Modus importieren
      fetchMockAutoComplete()
        .then((data) => callback(data))
        .catch(errorFunc);
    }else{
      this.post(data.formID, 'autocomplete', data, requestHeaders)
      .then((apiResponse) => {
        callback(apiResponse.responseData);
      })
      .catch((err) => {
        errorFunc(err);
      });
    }
  }

  /** @ANOnly */
  ANGetBinaryURL(formid: string, binaryValue: string, businessObjectID?: string, width?: number, height?: number) {
    let url = this.anBasePath + 'form/' + encodeURIComponent(formid);
    if (businessObjectID !== null && businessObjectID !== '') {
      if (typeof businessObjectID === 'undefined') {
        businessObjectID = '';
      }
      url = url + `/businessObject/${encodeURIComponent(businessObjectID)}`;
    }
    url = url + '/binary/' + encodeURIComponent(binaryValue);
    if(width != null && width > 0 && height != null && height > 0){
      url = url + '?w=' + width + '&h=' +height;
    }
    return url;
  }

}
