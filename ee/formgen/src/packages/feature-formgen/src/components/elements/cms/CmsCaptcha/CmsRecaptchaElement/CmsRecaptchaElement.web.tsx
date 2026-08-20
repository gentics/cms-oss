import React, { useEffect, useMemo, useRef, useState } from 'react';
import ReactDOM from 'react-dom';
import ReCAPTCHA from 'react-google-recaptcha';
import { useTranslation } from 'react-i18next';
import { Text, View } from 'react-native';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { theme as coreTheme } from '@andp/core/src/styles/theme.native';
import { recaptchaElementStyles } from '../../../../../styles/common/common-style';
import { recaptchaElementStyles as customRecaptchaElementStyles } from '../../../../../styles/custom/custom-style';
import { formElementWrapperStyles } from '../../../../../styles/common/common-style';
import { formElementWrapperStyles as customFormElementWrapperStyles } from '../../../../../styles/custom/custom-style';
import { CaptchaService } from '../../../../../services/CaptchaService';
import { CaptchaConfig } from '../../../../../types/interfaces';

/**
 * Global reCAPTCHA v2 widget (web). Rendered once per form by `CMSFormCaptcha`.
 *
 * Modes:
 *  - 'checkbox' (default): visible "I'm not a robot" widget, user solves actively.
 *  - 'invisible': no UI; registers an `execute` bridge so RestService can trigger
 *    the challenge programmatically right before submit/save.
 */
function CmsRecaptchaElement(props: CaptchaConfig) {
  const { siteKey, mode = 'checkbox', language } = props;
  const { t } = useTranslation();

  const recaptchaRef = useRef<ReCAPTCHA>(null);
  const wrapperRef = useRef<any>(null);
  const resolveTokenRef = useRef<((token: string | null) => void) | null>(null);
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
    if (!siteKey) {
      console.error(
        '[CmsRecaptchaElement] siteKey is missing. Please configure "siteKey" in CaptchaConfig.',
      );
      return;
    }

    CaptchaService.activate('recaptcha');

    if (mode === 'invisible') {
      CaptchaService.registerExecute(
        () =>
          new Promise<string | null>((resolve) => {
            resolveTokenRef.current = resolve;
            recaptchaRef.current?.execute();
          }),
      );
    }

    return () => {
      CaptchaService.unregisterExecute();
      CaptchaService.deactivate();
    };
  }, [siteKey, mode]);

  const handleVerify = (token: string | null) => {
    CaptchaService.setToken(token);
    if (resolveTokenRef.current) {
      resolveTokenRef.current(token);
      resolveTokenRef.current = null;
    }
  };

  const handleExpired = () => {
    CaptchaService.setToken(null);
  };

  const handleError = () => {
    CaptchaService.setToken(null);
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

  if (mode === 'invisible') {
    return ReactDOM.createPortal(
      <div
        ref={wrapperRef as any}
        style={{ position: 'fixed', bottom: 0, right: 0, zIndex: 9999 }}
      >
        <ReCAPTCHA
          ref={recaptchaRef}
          sitekey={siteKey}
          size="invisible"
          hl={language ?? 'de'}
          onChange={handleVerify}
          onExpired={handleExpired}
          onErrored={handleError}
        />
      </div>,
      document.body,
    );
  }

  return (
    <View ref={wrapperRef} style={styles.container}>
      <ReCAPTCHA
        ref={recaptchaRef}
        sitekey={siteKey}
        size="normal"
        hl={language ?? 'de'}
        onChange={handleVerify}
        onExpired={handleExpired}
        onErrored={handleError}
      />
      {hasError && (
        <View
          role="alert"
          style={[wrapperStyles.errorText, { marginTop: coreTheme.space.regular }]}
        >
          {t('form_captchaNotSolvedError')}
        </View>
      )}
    </View>
  );
}

export default CmsRecaptchaElement;
