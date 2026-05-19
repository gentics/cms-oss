import '@expo/metro-runtime';
import { View, Text, Linking } from 'react-native';
import theme from '@andp/core/src/styles/theme';
import FormgenScreen from '@andp/feature-formgen/src/screens/FormgenScreen';
import { useEffect, useMemo, useState } from 'react';
import * as jsonConfig from './formgen.config.json';
import Constants from 'expo-constants';
import { IFeatures, IFormFlowData, ISummaryDetailsUser } from '@andp/feature-formgen/src/types/interfaces';
import { Platform } from 'react-native';
import { fontFamilies as commonFontFamilies } from '@andp/core/src/styles/common/font-config';
import { fontFamilies as customFontFamilies } from '@andp/core/src/styles/custom/font-config';
import { useFonts } from 'expo-font';

export type FormgridPreviewData = {
  formId?: string;
  formJson?: string;
  features?: IFeatures;
  language?: string;
  prefillContent?: string;
  currentPage?: number;
  selectedElementId?: string;
};

export default function App(props: { isFormgridPreview?: boolean; previewData?: FormgridPreviewData }) {
  const [formgenConfig, setFormgenConfig] = useState<IFormFlowData>(jsonConfig);
  const isFormgridPreview = typeof document !== 'undefined' && document.getElementById('formgridForm') != null;
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

  const domPreview = useMemo<FormgridPreviewData>(() => {
    if (!isFormgridPreview || typeof document === 'undefined') return {};

    const formJsonEl = document.getElementById('formJson');
    const featuresEl = document.getElementById('formFeatures');
    const currentPageEl = document.getElementById('formCurrentPage');
    const selectedElementIdEl = document.getElementById('formSelectedElementId');

    const formJsonText = formJsonEl?.innerText ?? '';
    const featuresText = featuresEl?.innerText ?? null;
    const currentPageText = currentPageEl?.innerText ?? '';
    const selectedElementIdText = selectedElementIdEl?.innerText ?? '';

    const parsedCurrentPage = Number.parseInt(currentPageText, 10);

    let parsedFeatures: IFeatures | undefined = undefined;
    if (featuresText) {
      try {
        parsedFeatures = JSON.parse(featuresText) as IFeatures;
      } catch (e) {
        console.warn('Could not parse formFeatures JSON', e);
      }
    }

    return {
      formJson: formJsonText,
      features: parsedFeatures,
      currentPage: Number.isFinite(parsedCurrentPage) ? parsedCurrentPage : 0,
      selectedElementId: selectedElementIdText,
    };
  }, [isFormgridPreview]);

  const effectivePreview: FormgridPreviewData = {
    ...domPreview,
    ...(props.previewData ?? {}),
  };

  if (Platform.OS !== 'web' || isDevMode || isMockForm) {
    if (customFontFamilies && Object.keys(customFontFamilies).length > 0) {
      useFonts(customFontFamilies)
    } else {
      useFonts(commonFontFamilies)
    }
  }

  const formgridPreviewInit = (override?: FormgridPreviewData) => {
    const formidElement = document.getElementById('formId');
    const languageElement = document.getElementById("formLanguage");
    const formid = formidElement && formidElement.innerText != null ? formidElement.innerText : "";
    const language = languageElement && languageElement.innerText != null ? languageElement.innerText : "de";

    const nextFormId = override?.formId ?? formid;
    const nextLanguage = override?.language ?? language;
    const nextPrefill = override?.prefillContent ?? prefillContent;

    setLoggedIn(false);
    setFormId(nextFormId);
    setLanguage(nextLanguage || 'de');
    setPrefillContent(nextPrefill);
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
        formgridPreviewInit(props.previewData)
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

  useEffect(() => {
    if (!isFormgridPreview) return;
    formgridPreviewInit(props.previewData);
  }, [
    isFormgridPreview,
    props.previewData?.formId,
    props.previewData?.language,
    props.previewData?.prefillContent,
  ]);
  
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
          features={effectivePreview.features}
          formJson={effectivePreview.formJson ?? ''}
          currentPage={effectivePreview.currentPage ?? 0}
          selectedElementId={effectivePreview.selectedElementId ?? ''}
        />
      ) : (
        <Text>Fehler</Text>
      )}
    </View>
  );
}