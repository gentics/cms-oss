import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';
import {
  FriendlyCaptchaSDK,
  StartMode,
  WidgetErrorData,
} from '@friendlycaptcha/sdk';
import { CaptchaService } from '../../../../../services/CaptchaService';
import { CaptchaConfig, FriendlyCaptchaMode } from '../../../../../types/interfaces';
import { formElementWrapperStyles } from '../../../../../styles/common/common-style';
import { formElementWrapperStyles as customFormElementWrapperStyles } from '../../../../../styles/custom/custom-style';
import { theme as coreTheme } from '@andp/core/src/styles/theme.native';
import { mergeStyles } from '@andp/core/src/AndpUtils';

let sdkInstance: FriendlyCaptchaSDK | null = null;
function getSdk(): FriendlyCaptchaSDK {
  if (!sdkInstance) {
    sdkInstance = new FriendlyCaptchaSDK();
  }
  return sdkInstance;
}

const hiddenContainerStyle: React.CSSProperties = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
  border: 0,
};

const foregroundContainerStyle: React.CSSProperties = {
  display: 'block',
  marginTop: 16,
  marginBottom: 8,
  maxWidth: 320,
};

function resolveStartMode(mode: FriendlyCaptchaMode): StartMode {
  switch (mode) {
    case 'auto':
      return 'auto';
    case 'button':
      return 'none';
    case 'background':
      return 'auto';
  }
}

function CmsFriendlyCaptchaElement(props: CaptchaConfig) {
  const { siteKey, mode = 'auto', language } = props;
  const { t } = useTranslation();

  const containerRef = useRef<HTMLDivElement>(null);
  const wrapperRef = useRef<any>(null);
  const [hasError, setHasError] = useState<boolean>(CaptchaService.hasError());
  const styles = useMemo(
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
    if (!siteKey || !containerRef.current) {
      return;
    }

    const effectiveStartMode: StartMode = resolveStartMode(mode as FriendlyCaptchaMode);

    const widget = getSdk().createWidget({
      element: containerRef.current,
      sitekey: siteKey,
      startMode: effectiveStartMode,
      language
    });

    CaptchaService.activate('friendly');

    const handleComplete = (e: CustomEvent<{ response: string }>) => {
      CaptchaService.setToken(e.detail.response);
    };
    const handleExpire = () => {
      CaptchaService.setToken(null);
    };
    const handleError = (e: CustomEvent<{ error: WidgetErrorData }>) => {
      CaptchaService.setToken(null);
      console.error('[FriendlyCaptcha] widget error:', e.detail.error);
    };
    const handleReset = () => {
      CaptchaService.setToken(null);
    };

    widget.addEventListener('frc:widget.complete', handleComplete);
    widget.addEventListener('frc:widget.expire', handleExpire);
    widget.addEventListener('frc:widget.error', handleError);
    widget.addEventListener('frc:widget.reset', handleReset);

    return () => {
      widget.removeEventListener('frc:widget.complete', handleComplete);
      widget.removeEventListener('frc:widget.expire', handleExpire);
      widget.removeEventListener('frc:widget.error', handleError);
      widget.removeEventListener('frc:widget.reset', handleReset);
      CaptchaService.deactivate();
      widget.destroy();
    };
  }, [siteKey, mode, language]);

  if (!siteKey) {
    return null;
  }

  return (
    <View ref={wrapperRef}>
      <div
        ref={containerRef}
        className="frc-captcha"
        style={mode === 'background' ? hiddenContainerStyle : foregroundContainerStyle}
        aria-hidden={mode === 'background' ? true : undefined}
      />
      {hasError && mode !== 'background' && (
        <View
          role="alert"
          style={[styles.errorText, { marginTop: coreTheme.space.regular }]}
        >
          {t('form_captchaNotSolvedError')}
        </View>
      )}
    </View>
  );
}

export default CmsFriendlyCaptchaElement;
