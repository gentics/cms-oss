import React, { Suspense, useState, useEffect, useMemo } from 'react';
import { Platform } from 'react-native';
import baseTheme, { baseStyles } from '../styles/common/common-style';
import { baseStyles as customBaseStyles } from '../styles/custom/custom-style';
import RendererScreen from './RendererScreen';
import { IFeatures, IFormFlowData, ISummaryDetailsUser } from '../types/interfaces';
import { RestService } from '../services/RestService';
import {
  LoadingScreen,
} from '@andp/core/src/components/LoadingComponents';
import { initI18n } from '@andp/core/src/services/i18n';
import { fetchMockDefaultFeatures } from '../utils/MockFormularUtils';
import FormgenScreenErrorOverlay from '../components/overlays/childOverlays/FormgenScreenErrorOverlay';
import { TFunction } from 'i18next';
import theme from '@andp/core/src/styles/theme';
import { convertStyleToCssBlock, mergeStyles } from '@andp/core/src/AndpUtils';

const FormgenScreen: React.FC<{
  formId: string;
  config: IFormFlowData;
  isLoggedIn: boolean;
  apiToken: string;
  culture: string;
  businessObjectId: string;
  devMode: boolean;
  mockForm: boolean;
  endpoint: string;
  translationUrl?: string; // for loading translations inside webroot of mesh plugin (for example boxed solution)
  successContent?: string;
  prefillContent?: string;
  userData?: ISummaryDetailsUser;
  features?: IFeatures;
  t?: TFunction;
  detailUrl?: string;
  hasDetail?: boolean;
}> = (props) => {
  const [features, setFeatures] = useState<IFeatures>();
  const [loadFeatureError, setLoadFeatureError] = useState<boolean>(false);
  const isWeb = Platform.OS === 'web';
  const [t, setT] = useState<any>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const restService = new RestService(props.endpoint, props.apiToken);


  useEffect(() => {
    const fetchFeatures = async () => {
      const response = await restService.loadFeaturesAsync();
      if (response.success) {
        const features = response.responseData;
        if (features != null) {
          setFeatures(features);
        } else {
          setLoadFeatureError(true);
          setError("Server not responding: Features not available");
        }
      }
    };
    const fetchMockFeatures = async () => {
      if (__DEV__) {
        const response = await fetchMockDefaultFeatures();
        const features = response;
        if (features != null) {
          setFeatures(features);
        } else {
          setLoadFeatureError(true);
          setError("Server not responding: Features not available");
        }
      }
    };

    if(props.features != null){
      setFeatures(props.features);
    }else if (props.mockForm) {
      fetchMockFeatures();
    } else {
      fetchFeatures();
    }
  }, []);

  useEffect(() => {
    if(props.t == null || props.t == undefined){
      let mounted = true;
      (async () => {
        try {
          const i18nInstance = await initI18n(
            props.endpoint,
            props.translationUrl,
            false,
            props.culture,
          );
          await i18nInstance.changeLanguage(props.culture);
          if (mounted) {
            setT(() => i18nInstance.t.bind(i18nInstance));
            setLoading(false);
          }
        } catch (err: any) {
          console.error('i18n init error:', err);
          if (mounted) {
            setError('Translations could not be loaded: ' + err.toString());
            setLoading(false);
          }
        }
      })();
      return () => {
        mounted = false;
      };
    }else{
      setT(() => props.t);
      setLoading(false);
    }
    
  }, [props.endpoint, props.translationUrl]);

  useEffect(() => {
    if (!loading && isWeb) {
      const loader = document.getElementById('bcLoader');
      if (loader) {
        loader.style.opacity = '0';
        setTimeout(() => {
          loader.style.display = 'none';
        }, 500);
      }
    }
  }, [loading, isWeb]);

  const styles = useMemo(() => mergeStyles(baseStyles, customBaseStyles), []);

  const styleHtml = `<style>
    #root { font-family: ${theme.fontFamily.regular} }
    ${convertStyleToCssBlock('#root h1', styles.h1)}
    ${convertStyleToCssBlock('#root h2', styles.h2)}
    ${convertStyleToCssBlock('#root h3', styles.h3)}
    ${convertStyleToCssBlock('#root h4', styles.h4)}
    ${convertStyleToCssBlock('#root h5', styles.h5)}
    ${convertStyleToCssBlock('#root h6', styles.h6)}
    ${convertStyleToCssBlock('#root p', styles.p)}
    ${convertStyleToCssBlock('#root ul', styles.ul)}
    ${convertStyleToCssBlock('#root ul li', styles.ulItem)}
    ${convertStyleToCssBlock('#root ol', styles.ol)}
    ${convertStyleToCssBlock('#root ol li', styles.olItem)}
    ${convertStyleToCssBlock('#root li', styles.li)}
    ${convertStyleToCssBlock('#root a', styles.a)}
    ${convertStyleToCssBlock('#root blockquote', styles.blockquote)}
    ${convertStyleToCssBlock('#root code', styles.code)}
    ${convertStyleToCssBlock('#root pre', styles.pre)}
    ${convertStyleToCssBlock('#root hr', styles.hr)}
    ${convertStyleToCssBlock('#root b', styles.b)}
    ${convertStyleToCssBlock('#root strong', styles.strong)}
    ${convertStyleToCssBlock('#root i', styles.i)}
    ${convertStyleToCssBlock('#root em', styles.em)}
    ${convertStyleToCssBlock('#root u', styles.u)}
  </style>`;

  if (error) {
    return (
      <FormgenScreenErrorOverlay
        title="Error loading resources"
        contentMessages={[error]}
      />
    );
  }
  
  if (features && props.config && t) {
    return (
      <Suspense fallback={<LoadingScreen />}>
        <RendererScreen
          features={features}
          formId={props.formId}
          businessObjectId={props.businessObjectId}
          config={props.config}
          culture={props.culture}
          apiToken={props.apiToken}
          isLoggedIn={props.isLoggedIn}
          devMode={props.devMode}
          mockForm={props.mockForm}
          t={t}
          endpoint={props.endpoint}
          translationUrl={props.translationUrl}
          prefillContent={props.prefillContent}
          userData={props.userData}
          detailUrl={props.detailUrl}
          hasDetail={props.hasDetail}
          successContent={props.successContent}
        />
        {Platform.OS === "web" && <span dangerouslySetInnerHTML={{ __html: styleHtml }} />}
      </Suspense>
    );
  }
};

export default FormgenScreen;

