import { convertStyleToCssBlock, mergeStyles } from '@andp/core/src/AndpUtils';
import {
  LoadingScreen,
} from '@andp/core/src/components/LoadingComponents';
import { initI18n } from '@andp/core/src/services/i18n';
import theme from '@andp/core/src/styles/theme';
import { TFunction } from 'i18next';
import React, { Suspense, useEffect, useMemo, useState } from 'react';
import { Platform } from 'react-native';
import FormgenScreenErrorOverlay from '../components/overlays/childOverlays/FormgenScreenErrorOverlay';
import { PluginNameEnum } from '../constants/enums/PluginNameEnum';
import { RestService } from '../services/RestService';
import { baseStyles } from '../styles/common/common-style';
import { baseStyles as customBaseStyles } from '../styles/custom/custom-style';
import { IDataFields, IFeatures, IForm, IFormFlowData, ISchema, ISummaryDetailsUser } from '../types/interfaces';
import { fetchMockDefaultFeatures } from '../utils/MockFormularUtils';
import RendererScreen from './RendererScreen';

const FormgenScreen: React.FC<{
  formId: string;
  config: IFormFlowData;
  isLoggedIn: boolean;
  apiToken: string;
  language: string;
  businessObjectId: string;
  devMode: boolean;
  mockForm: boolean;
  endpoint: string;
  translationUrl?: string; // for loading translations inside webroot of mesh plugin (for example boxed solution)
  successContent?: string;
  prefillContent?: IDataFields;
  userData?: ISummaryDetailsUser;
  features?: IFeatures;
  t?: TFunction;
  detailUrl?: string;
  hasDetail?: boolean;
  pluginName: string;
  isFormgridPreview?: boolean;
  previewSchema?: ISchema;
  previewUiSchema?: IForm;
  currentPage?: number;
  selectedElementId?: string;
}> = (props) => {
  const [features, setFeatures] = useState<IFeatures>();
  const [loadFeatureError, setLoadFeatureError] = useState<boolean>(false);
  const isWeb = Platform.OS === 'web';
  const [t, setT] = useState<any>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pluginName = (props.pluginName) as PluginNameEnum;

  const restService = new RestService(props.endpoint, pluginName, props.apiToken);

  useEffect(() => {
    if(pluginName == PluginNameEnum.ACTA_NOVA){
      const fetchFeatures = async () => {
        const response = await restService.ANLoadFeaturesAsync();
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
        if (__DEV__ || props.isFormgridPreview) {
          const response = await fetchMockDefaultFeatures();
          const features = response;
          if (features != null) {
            setFeatures(features);
          } else {
            setLoadFeatureError(true);
            setError('Server not responding: Features not available');
          }
        }
      };

      if (props.features != null) {
        setFeatures(props.features);
      } else if (props.mockForm || props.isFormgridPreview) {
        fetchMockFeatures();
      } else if (!props.isFormgridPreview) {
        fetchFeatures();
      }
    }
  }, []);

  useEffect(() => {
    if(props.t == null){
      let mounted = true;
      (async () => {
        try {
          const i18nInstance = await initI18n(
            props.endpoint,
            props.formId,
            props.pluginName,
            props.translationUrl,
            false,
            props.language
          );
          await i18nInstance.changeLanguage(props.language);
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
    if (!loading && isWeb && !props.isFormgridPreview) {
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
  const showRenderScreen = () => {
    if(pluginName == PluginNameEnum.ACTA_NOVA && features && props.config && t){
      return true; 
    }
    if(pluginName == PluginNameEnum.CMS_FORMS && props.config && t){
        return true;
    }
    if (props.isFormgridPreview) {
      return props.previewUiSchema != null && props.previewSchema != null;
    }
    return false;
  }
  
  if (showRenderScreen()) {
    return (
      <Suspense fallback={<LoadingScreen />}>
        {t && <RendererScreen
          features={features as IFeatures}
          formId={props.formId}
          businessObjectId={props.businessObjectId}
          config={props.config}
          language={props.language}
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
          pluginName={pluginName}
          successContent={props.successContent}
          isFormgridPreview={props.isFormgridPreview}
          previewSchema={props.previewSchema}
          previewUiSchema={props.previewUiSchema}
          currentPage={props.currentPage}
          selectedElementId={props.selectedElementId}
        />}
        {Platform.OS === "web" && <span dangerouslySetInnerHTML={{ __html: styleHtml }} />}
      </Suspense>
    );
  }

  return <LoadingScreen />;
};

export default FormgenScreen;

