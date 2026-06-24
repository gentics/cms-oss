import { View } from 'react-native';
import { useBreakpoints, widthFromColumNumber } from '@andp/core/src/AndpUtils';
import { CaptchaConfig, FormState } from '../../../../types/interfaces';
import CmsFriendlyCaptchaElement from './CmsFriendlyCaptchaElement';
import CmsRecaptchaElement from './CmsRecaptchaElement';

interface Props {
  formState: FormState;
}

export default function CMSFormCaptcha({ formState }: Props) {
  const config = formState.captchaConfig;
  if (!config) return null;

  const breakPoints = useBreakpoints();
  const widthStyle = widthFromColumNumber(
    formState.form.formwidth || 8,
    breakPoints,
  );

  const renderElement = () => {
    switch (config.provider) {
      case 'friendly':
        return (
          <CmsFriendlyCaptchaElement
            provider={config.provider}
            siteKey={config.siteKey}
            mode={config.mode}
            language={config.language}
          />
        );
      case 'recaptcha':
        return (
          <CmsRecaptchaElement
            provider={config.provider}
            siteKey={config.siteKey}
            mode={config.mode}
            language={config.language}
            baseUrl={'baseUrl' in config ? config.baseUrl : undefined}
          />
        );
      default:
        return null;
    }
  };

  const element = renderElement();
  if (!element) return null;

  return (
    <View style={{ alignItems: 'center' }}>
      <View style={[widthStyle, {alignItems: 'flex-end'}]}>{element}</View>
    </View>
  );
}
