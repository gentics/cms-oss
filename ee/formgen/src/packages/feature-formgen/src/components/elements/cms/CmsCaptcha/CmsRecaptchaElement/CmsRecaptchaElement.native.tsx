import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Text, View } from 'react-native';
import Recaptcha from 'react-native-recaptcha-that-works';
import { useTranslation } from 'react-i18next';
import AndpButton from '@andp/core/src/components/AndpButton';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { theme as coreTheme } from '@andp/core/src/styles/theme.native';
import { recaptchaElementStyles } from '../../../../../styles/common/common-style.native';
import { recaptchaElementStyles as customRecaptchaElementStyles } from '../../../../../styles/custom/custom-style';
import { formElementWrapperStyles } from '../../../../../styles/common/common-style';
import { formElementWrapperStyles as customFormElementWrapperStyles } from '../../../../../styles/custom/custom-style';
import { CaptchaService } from '../../../../../services/CaptchaService';
import { CaptchaConfig, reCaptchaMode } from '../../../../../types/interfaces';

/**
 * Global reCAPTCHA v2 widget (native). Rendered once per form by `CMSFormCaptcha`.
 *
 * Uses `react-native-recaptcha-that-works`, which presents the challenge in a modal.
 * Modes:
 *  - 'checkbox' (default): user taps a trigger button to open the challenge.
 *  - 'invisible': registers an `execute` bridge; the modal is opened
 *    programmatically by RestService right before submit/save.
 */
function CmsRecaptchaElement(props: CaptchaConfig) {
  const { siteKey, mode = 'checkbox', baseUrl } = props;
  const { t } = useTranslation();

  const recaptchaRef = useRef<any>(null);
  const wrapperRef = useRef<any>(null);
  const resolveTokenRef = useRef<((token: string | null) => void) | null>(null);
  const [verified, setVerified] = useState(false);
  const [hasError, setHasError] = useState<boolean>(CaptchaService.hasError());

  const styles = useMemo(
    () => mergeStyles(recaptchaElementStyles, customRecaptchaElementStyles),
    [],
  );
  const wrapperStyles = useMemo(
    () => mergeStyles(formElementWrapperStyles, customFormElementWrapperStyles),
    [],
  );

  useEffect(() => {
    CaptchaService.registerRef(wrapperRef);
    const unsubscribe = CaptchaService.subscribe(() => {
      setHasError(CaptchaService.hasError());
    });
    return () => {
      unsubscribe();
      CaptchaService.unregisterRef();
    };
  }, []);

  useEffect(() => {
    if (!siteKey) return;
    CaptchaService.activate('recaptcha');

    if (mode === 'invisible') {
      CaptchaService.registerExecute(
        () =>
          new Promise<string | null>((resolve) => {
            resolveTokenRef.current = resolve;
            recaptchaRef.current?.open();
          }),
      );
    }

    return () => {
      CaptchaService.unregisterExecute();
      CaptchaService.deactivate();
    };
  }, [siteKey, mode]);

  const handleVerify = (token: string) => {
    CaptchaService.setToken(token || null);
    setVerified(true);
    if (resolveTokenRef.current) {
      resolveTokenRef.current(token || null);
      resolveTokenRef.current = null;
    }
  };

  const handleExpire = () => {
    CaptchaService.setToken(null);
    setVerified(false);
  };

  if (!siteKey) {
    return (
      <View ref={wrapperRef} style={styles.missingKeyError}>
        <AndpIcon name="circleWarning" style={styles.missingKeyErrorIcon} />
        <Text style={styles.missingKeyErrorText}>
          {t('form_recaptcha_no_sitekey')}
        </Text>
      </View>
    );
  }

  return (
    <View ref={wrapperRef}>
      {mode === 'checkbox' && (
        <AndpButton
          title={verified ? t('form_recaptcha_verified') : t('form_recaptcha_open')}
          onPress={() => recaptchaRef.current?.open()}
          buttonStyle={[styles.button, verified && styles.buttonVerified]}
          textStyle={[styles.buttonText, verified && styles.buttonTextVerified]}
          wrapperStyle={{ width: '100%' }}
        />
      )}
      <Recaptcha
        ref={recaptchaRef}
        siteKey={siteKey}
        baseUrl={baseUrl ?? 'https://localhost'}
        onVerify={handleVerify}
        onExpire={handleExpire}
        size={mode == 'checkbox' ? 'normal' : 'invisible'}
      />
      {hasError && (
        <View
          accessibilityRole="alert"
          style={[wrapperStyles.errorText, { marginTop: coreTheme.space.regular }]}
        >
          <Text>{t('form_captchaNotSolvedError')}</Text>
        </View>
      )}
    </View>
  );
}

export default CmsRecaptchaElement;
