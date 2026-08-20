import { fontFamilies as commonFontFamilies } from '@andp/core/src/styles/common/font-config';
import { fontFamilies as customFontFamilies } from '@andp/core/src/styles/custom/font-config';
import theme from '@andp/core/src/styles/theme';
import FormgenScreen from '@andp/feature-formgen/src/screens/FormgenScreen';
import { IDataFields, IFormFlowData, ISummaryDetailsUser } from '@andp/feature-formgen/src/types/interfaces';
import {
  ExtendedWindow,
  FormgridPreviewData,
  PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME,
  PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME,
  PREVIEW_FORM_CHANGE_EVENT_NAME,
  PREVIEW_INTIALIZATION_EVENT_NAME,
  PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME,
  PreviewEvent,
  PreviewEventReceiver,
  PreviewFormData,
  PreviewInformation,
} from '@andp/feature-formgen/src/types/preview';
import '@expo/metro-runtime';
import Constants from 'expo-constants';
import { useFonts } from 'expo-font';
import { useEffect, useState } from 'react';
import { Platform, Text, View } from 'react-native';
import * as jsonConfig from './formgen.config.json';
import { sendPreviewConnectEvent } from '@andp/feature-formgen/src/components/elements/helper/formgridPreviewBridge';

export default function App() {
  let initialPreviewData: FormgridPreviewData | undefined;
  let dynamicPreview = false;

  if (Platform.OS === 'web') {
    const queryParams = new URLSearchParams(window.location.search);
    dynamicPreview = queryParams.get('preview') === 'true';

    // Load initial/static preview data from 
    if ((window as any as ExtendedWindow).formPreviewData != null) {
      initialPreviewData = (window as any as ExtendedWindow).formPreviewData;
    }
  }

  const isFormgridPreview = initialPreviewData != null || dynamicPreview;

  const [previewInitialized, setPreviewInitialized] = useState(false);
  const [previewFormData, setPreviewFormData] = useState<PreviewFormData>();
  const [previewInformation, setPreviewInformation] = useState<PreviewInformation>();
  const [previewSelectedElement, setPreviewSelectedElement] = useState<string>();
  const [previewPageIndex, setPreviewPageIndex] = useState<number>();
  const [previewMessageListener, setPreviewMessageListener] = useState(false);
  const [previewFromDomLoaded, setPreviewFromDomLoaded] = useState(false);

  const [formgenConfig, setFormgenConfig] = useState<IFormFlowData>(jsonConfig);
  const isDevMode = Constants.manifest.extra.devMode;
  const isMockForm = Constants.manifest.extra.mockForm && !isFormgridPreview;
  const [language, setLanguage] = useState<string>();
  const [Endpoint, setEndpoint] = useState<string>();
  const [formId, setFormId] = useState<string>();
  const [businessObjectId, setBusinessObjectId] = useState<string>('');
  const [initialInit, setInit] = useState<boolean>(false);
  const [isLoggedIn, setLoggedIn] = useState<boolean>(false);
  const [successContent, setSuccessContent] = useState<string>('');
  const [prefillContent, setPrefillContent] = useState<IDataFields>();
  const [userData, setUserData] = useState<ISummaryDetailsUser>();
  const [translationUrl, setTranslationUrl] = useState<string>('');
  const [pluginName, setPluginName] = useState<string>('');

  // Load the data from the window variable if available, but only once
  if (initialPreviewData && !previewFromDomLoaded && !previewInitialized) {
    setFormId(initialPreviewData.formId);
    setPreviewInformation({
      dynamic: false,
      features: initialPreviewData.features,
      formType: initialPreviewData.formType,
      pluginName: initialPreviewData.pluginName,
      flowId: initialPreviewData.flowId,
      flows: initialPreviewData.flows,
      availableLanguages: [initialPreviewData.language],
      cmsSid: initialPreviewData.cmsSid,
    });
    setPluginName(initialPreviewData.pluginName);
    setLanguage(initialPreviewData.language);
    setPreviewFormData({
      schema: initialPreviewData.schema,
      uiSchema: initialPreviewData.uiSchema,
    });
    setPrefillContent(initialPreviewData.prefillContent);
    if (initialPreviewData.currentPage) {
      setPreviewPageIndex(initialPreviewData.currentPage);
    }
    if (initialPreviewData.selectedElementId) {
      setPreviewSelectedElement(initialPreviewData.selectedElementId);
    }
    setPreviewFromDomLoaded(true);

    if (!dynamicPreview) {
      setPreviewInitialized(true);
    }
  }

  // Register the dynamic preview message handler
  if (dynamicPreview && !previewMessageListener && !previewInitialized) {
    window.addEventListener('message', (raw) => {
      const event = raw.data as PreviewEvent;
      if (event.receiver !== PreviewEventReceiver.PUPPET) {
        return;
      }

      console.debug('<< FormGen', event.eventType);

      switch (event.eventType) {
        case PREVIEW_INTIALIZATION_EVENT_NAME:
          setFormId(event.formId);
          setPreviewInformation({
            dynamic: true,
            features: event.features,
            formType: event.formType,
            pluginName: event.pluginName,
            flowId: event.flowId,
            flows: event.flows,
            availableLanguages: event.availableLanguages,
            cmsSid: event.cmsSid,
          });
          setPluginName(event.pluginName);
          setPreviewFormData({
            schema: event.schema,
            uiSchema: event.uiSchema,
          });
          setLanguage(event.language);
          setPreviewPageIndex(event.pageIndex);
          setPreviewSelectedElement(event.elementId);
          setPreviewInitialized(true);
          break;

        case PREVIEW_FORM_CHANGE_EVENT_NAME:
          setPreviewFormData({
            schema: event.schema,
            uiSchema: event.uiSchema,
          });
          setPrefillContent(event.prefillContent);
          break;

        case PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME:
          setPreviewPageIndex(event.pageIndex);
          break;

        case PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME:
          setLanguage(event.language);
          break;

        case PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME:
          setPreviewSelectedElement(event.elementId);
          break;
      }
    });
    setPreviewMessageListener(true);

    sendPreviewConnectEvent();
  }

  if (Platform.OS !== 'web' || isDevMode || isMockForm) {
    if (customFontFamilies && Object.keys(customFontFamilies).length > 0) {
      useFonts(customFontFamilies)
    } else {
      useFonts(commonFontFamilies)
    }
  }

  useEffect(() => {
    if (isDevMode)
      console.log('DEV_MODE ist', isDevMode ? 'aktiviert' : 'deaktiviert');
    if (isMockForm)
      console.log('MOCK_FORM ist', isMockForm ? 'aktiviert' : 'deaktiviert');

    let domain = '';
    if (typeof window !== 'undefined' && window.location && window.location.origin) {
      domain = window.location.origin;
    }
    setEndpoint(isDevMode || isMockForm ? formgenConfig.meshEndpoint : domain);

    if (document == null) {
      setLanguage('de');
      setLoggedIn(true);
      setInit(true);
      return;
    }

    if (isFormgridPreview) {
      setLoggedIn(false);
    } else if (document.getElementById('root') != undefined && document.getElementById('root') != null && !isFormgridPreview) {
      initFromDocumentElements();
    }

    const searchString = window.location.search;
    if (searchString) {
      const params = new URLSearchParams(searchString);
      const boId = params.get("businessObjectId");
      if (boId) {
        setBusinessObjectId(boId);
      }
    }

    setInit(true);
  }, []);

  function initFromDocumentElements(): void {
    const rootElement = document.getElementById('root');
    const lang = rootElement?.getAttribute('data-culture');
    //formid is the cms form uuid
    //externalId - a possible externalFormID, e.g. a actanova form-id
    //internally used formid is now externalId if available
    const externalId = rootElement?.getAttribute('data-externalId');
    const formid = externalId || rootElement?.getAttribute('data-formid');
    const emailElement = document.getElementById("egov-template--user-data--email");
    const isloggedin = emailElement != null ? true : false;
    const nameElement = document.getElementById("egov-template--user-data--displayname");
    const userNameElement = document.getElementById("egov-template--user-data--username");
    const translationUrlFromId = rootElement?.getAttribute('data-translation-url');

    // TODO GPU-2306 GPU-2305 später acta-nova entfernen hier. 
    const pluginNameAttr = rootElement?.getAttribute('data-plugin-name') ?? "acta-nova";

    const userEmail = emailElement && emailElement.innerText != null ? emailElement.innerText : "";
    const userName = nameElement && nameElement.innerText != null ? nameElement.innerText : "";
    const userUsername = userNameElement && userNameElement.innerText != null ? userNameElement.innerText : "";

    const userData: ISummaryDetailsUser = {
      name: userName,
      username: userUsername,
      email: userEmail
    }

    const successElement = document.getElementById("egov-template--success-message");
    const prefillElement = document.getElementById("egov-template--prefill");
    const successMessage = successElement && successElement.innerText != null ? successElement.innerText : "";
    const prefillContent = prefillElement && prefillElement.innerText != null ? prefillElement.innerText : "";
    setLoggedIn(isloggedin);
    setUserData(userData);
    setLanguage(lang ? lang : 'de');
    setSuccessContent(successMessage);
    try {
      setPrefillContent(JSON.parse(prefillContent));
    } catch (err) {
      console.warn('Could not load prefill content');
    }
    setTranslationUrl(translationUrlFromId ?? '');
    setPluginName(pluginNameAttr ?? '');
    
    if (formid) {
      setFormId(formid);
    }
  }

  return (
    <View style={theme.container}>
      {initialInit && language && Endpoint ? (
        (!isFormgridPreview || previewInitialized) && <>
          <FormgenScreen
            config={formgenConfig}
            formId={formId ? formId : ''}
            isLoggedIn={isLoggedIn}
            apiToken={''}
            devMode={isDevMode}
            mockForm={isMockForm}
            language={language}
            endpoint={Endpoint}
            businessObjectId={businessObjectId}
            prefillContent={prefillContent}
            successContent={successContent}
            userData={userData}
            translationUrl={translationUrl}
            pluginName={pluginName}
            isFormgridPreview={isFormgridPreview}
            previewFormData={previewFormData}
            previewInformation={previewInformation}
            previewSelectedElement={previewSelectedElement}
            previewPageIndex={previewPageIndex}
          />
        </>
      ) : (
        <Text>Fehler</Text>
      )}
    </View>
  );
}