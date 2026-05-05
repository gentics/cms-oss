import '@expo/metro-runtime';
import { View, Text, Linking } from 'react-native';
import theme from '@andp/core/src/styles/theme';
import FormgenScreen from '@andp/feature-formgen/src/screens/FormgenScreen';
import { useEffect, useState } from 'react';
import * as jsonConfig from './formgen.config.json';
import Constants from 'expo-constants';
import { IFormFlowData, ISummaryDetailsUser } from '@andp/feature-formgen/src/types/interfaces';
import { Platform } from 'react-native';
import { fontFamilies as commonFontFamilies } from '@andp/core/src/styles/common/font-config';
import { fontFamilies as customFontFamilies } from '@andp/core/src/styles/custom/font-config';
import { useFonts } from 'expo-font';

export default function App() {
  const [formgenConfig, setFormgenConfig] = useState<IFormFlowData>(jsonConfig);
  const isDevMode = Constants.manifest.extra.devMode;
  const isMockForm = Constants.manifest.extra.mockForm;
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
      if (document.getElementById('root') != undefined && document.getElementById('root') != null) {
        const rootElement = document.getElementById('root');
        const lang = document.getElementById('data-culture')?.getAttribute('data-culture');
        const formid = rootElement?.getAttribute('data-formid');
        const isloggedin = document.getElementById("egov-template--user-data--email") != null ? true : false;
        const emailElement = document.getElementById("egov-template--user-data--email");
        const nameElement = document.getElementById("egov-template--user-data--displayname");
        const userNameElement = document.getElementById("egov-template--user-data--username");
        const translationUrlFromId = rootElement?.getAttribute('data-translation-url');

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

  //console.log(initialInit, language, Endpoint);

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
          culture={language}
          endpoint={Endpoint}
          businessObjectId={businessObjectId}
          prefillContent={prefillContent}
          successContent={successContent}
          userData={userData}
          translationUrl={translationUrl}
        />
      ) : (
        <Text>Fehler</Text>
      )}
    </View>
  );
}