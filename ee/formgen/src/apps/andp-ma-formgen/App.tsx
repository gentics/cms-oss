import '@expo/metro-runtime';
import { View, Text } from 'react-native';
import theme from '@andp/core/src/styles/theme';
import FormgenScreen from '@andp/feature-formgen/src/screens/FormgenScreen';
import { useEffect, useReducer, useState } from 'react';
import * as jsonConfig from './formgen.config.json';
import Constants from 'expo-constants';
import { IFeatures, IFormFlowData, ISummaryDetailsUser } from '@andp/feature-formgen/src/types/interfaces';
import { Platform } from 'react-native';
import { fontFamilies as commonFontFamilies } from '@andp/core/src/styles/common/font-config';
import { fontFamilies as customFontFamilies } from '@andp/core/src/styles/custom/font-config';
import { useFonts } from 'expo-font';

export type FormgridPreviewData = {
  formId?: string;
  schema?: any;
  uiSchema?: any;
  features?: IFeatures;
  language?: string;
  prefillContent?: string;
  currentPage?: number;
  selectedElementId?: string;
};

interface ExtendedWindow extends Window {
  // Form Data that is assigned in the page directly.
  formPreviewData: FormgridPreviewData;
}

const PREVIEW_DATA_EVENT_NAME = 'preview-data-change';

interface PreviewDataChangeEvent {
  eventType: typeof PREVIEW_DATA_EVENT_NAME;
  data: Partial<FormgridPreviewData>;
}

export default function App() {
  let initialPreviewData: FormgridPreviewData | undefined;
  let dynamicPreview = false;

  if (Platform.OS === 'web') {
    console.log('is web')
    const queryParams = new URLSearchParams(window.location.search);
    console.log('params', queryParams, window.location.search);
    dynamicPreview = queryParams.get('preview') === 'true';

    // Load initial/static preview data from 
    if ((window as any as ExtendedWindow).formPreviewData != null) {
      initialPreviewData = (window as any as ExtendedWindow).formPreviewData;
    }
  }

  console.log('is dynamic preview', dynamicPreview, initialPreviewData);

  const [previewData, dispatch] = useReducer((state, action: Partial<FormgridPreviewData>) => {
    return {
      ...state,
      ...action,
    };
  }, initialPreviewData);

  const [formgenConfig, setFormgenConfig] = useState<IFormFlowData>(jsonConfig);
  const isFormgridPreview = initialPreviewData != null || dynamicPreview;
  const isDevMode = Constants.manifest.extra.devMode;
  const isMockForm = Constants.manifest.extra.mockForm || isFormgridPreview;
  const [language, setLanguage] = useState<string>();
  const [Endpoint, setEndpoint] = useState<string>();
  const [formId, setFormId] = useState<string>();
  const [businessObjectId, setBusinessObjectId] = useState<string>('');
  const [initialInit, setInit] = useState<boolean>(false);
  const [isLoggedIn, setLoggedIn] = useState<boolean>(false);
  const [successContent, setSuccessContent] = useState<string>('');
  const [prefillContent, setPrefillContent] = useState<string>('');
  const [userData, setUserData] = useState<ISummaryDetailsUser>();
  const [translationUrl, setTranslationUrl] = useState<string>('');
  const [pluginName, setPluginName] = useState<string>('');

  if (dynamicPreview) {
    window.addEventListener('message', (event) => {
      console.log('received message from parent');
      // TODO: Verify origin?

      const mapped = event.data as PreviewDataChangeEvent;
      if (mapped != null && mapped.eventType === PREVIEW_DATA_EVENT_NAME && mapped.data != null) {
        console.log('is change data', mapped.data);
        dispatch(mapped.data);

        setFormId(mapped.data.formId);
        setLanguage(mapped.data.language);
        setPrefillContent(mapped.data.prefillContent || '');
      }
    });

    console.log('sending connect message!');
    window.postMessage({
      eventType: 'preview-connect',
    }, '*');
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
    if (typeof document !== 'undefined') {
      if (isFormgridPreview) {
        setLoggedIn(false);
      }
      else if (document.getElementById('root') != undefined && document.getElementById('root') != null && !isFormgridPreview) {
        const rootElement = document.getElementById('root');
        const lang = document.getElementById('data-culture')?.getAttribute('data-culture');
        //formid is the cms form uuid
        //externalId - a possible externalFormID, e.g. a actanova form-id
        //internally used formid is now externalId if available
        const externalId = rootElement?.getAttribute('data-externalId');
        const formid = externalId || rootElement?.getAttribute('data-formid');
        const isloggedin = document.getElementById("egov-template--user-data--email") != null ? true : false;
        const emailElement = document.getElementById("egov-template--user-data--email");
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
        setPrefillContent(prefillContent);
        setTranslationUrl(translationUrlFromId ?? '');
        setPluginName(pluginNameAttr ?? '');
        
        if (formid) {
          setFormId(formid);
        }
      }
      const searchString = window.location.search;
      if (searchString) {
        const params = new URLSearchParams(searchString);
        const boId = params.get("businessObjectId");
        if (boId) {
          setBusinessObjectId(boId);
        }
      }
    } else {
      setLanguage('de');
      setLoggedIn(true);
    }
    setInit(true);
  }, []);
  
  return (
    <View style={theme.container}>
      {initialInit && language && Endpoint ? (
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
          features={previewData?.features}
          previewSchema={previewData?.schema}
          previewUiSchema={previewData?.uiSchema}
          currentPage={previewData?.currentPage ?? 0}
          selectedElementId={previewData?.selectedElementId ?? ''}
        />
      ) : (
        <Text>Fehler</Text>
      )}
    </View>
  );
}