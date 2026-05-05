import React, { useEffect, useMemo, useRef, useState } from 'react';
import ReactDOM from 'react-dom';
import ReCAPTCHA from 'react-google-recaptcha';
import { Text, View } from 'react-native';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { recaptchaElementStyles } from '../../../../styles/common/common-style';
import { recaptchaElementStyles as customRecaptchaElementStyles } from '../../../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
} from '../../../../types/interfaces';
import { FormElementWrapper } from '../../helper/FormElementWrapper';
import * as utils from '../../../../utils/Utils';
import { StatesToUpdateEnum } from '../../../../constants/enums/StateToUpdateEnum';
import { RecaptchaService } from '../../../../services/RecaptchaService';

/**
 * CmsRecaptchaElement renders a Google reCAPTCHA v2 widget (web).
 *
 * The `siteKey` is read from `formGridOptions.siteKey` (set in the form's ui-schema).
 * The `mode` is read from `formGridOptions.mode` — 'checkbox' (default) or 'invisible'.
 *
 * Invisible mode:
 * Before save or submit, `(global as any).executeRecaptcha()` is called by the
 * pre-save/pre-submit hooks. This triggers `recaptchaRef.execute()` and returns a
 * Promise that resolves once `onChange` fires with the token.
 *
 * Domain/project-specific behavior:
 * - If no `siteKey` is configured → console.error + inline error message + form-level error.
 * - On successful verification, the token is written into `formState` via
 *   `utils.setValue` AND into `(global as any).recaptchaJWT` so `RestService`
 *   sends it as the `x-recaptcha-token` header on every request.
 * - Token expiry: when the widget calls `onChange(null)`, the stored token is cleared.
 * - `validate` always requires a non-empty token.
 * - `getSummaryValue` returns `''` — the token has no user-facing summary representation.
 */
function CmsRecaptchaElement(props: IFormComponentAttributes) {
  const { formState, uiState, t } = props;
  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  const styles = useMemo(
    () => mergeStyles(recaptchaElementStyles, customRecaptchaElementStyles),
    [],
  );

  const recaptchaRef = useRef<ReCAPTCHA>(null);
  const resolveTokenRef = useRef<((token: string) => void) | null>(null);

  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const siteKey: string | undefined = props.formGridOptions?.siteKey;
  const mode = props.formGridOptions?.mode ?? 'checkbox';
  const size: 'normal' | 'invisible' = mode === 'invisible' ? 'invisible' : 'normal';
  const hl = formState.culture?.split('-')[0] ?? 'de';

  useEffect(() => {
    if (!siteKey) {
      console.error('[CmsRecaptchaElement] siteKey is missing. Please configure "siteKey" in formGridOptions.');
      formState.data.hasErrors = true;
      props.updateState(StatesToUpdateEnum.FormState);
    }
  }, [siteKey]);

  useEffect(() => {
    RecaptchaService.markRequired();
    if (siteKey && size === 'invisible') {
      RecaptchaService.registerExecute(
        () =>
          new Promise((resolve) => {
            resolveTokenRef.current = resolve;
            recaptchaRef.current?.execute();
          }),
      );
    }
    return () => {
      RecaptchaService.unregisterExecute();
    };
  }, [siteKey, size]);

  const handleVerify = (token: string | null) => {
    const val = token ?? '';
    utils.setValue(formState, props.jsonpath, val);
    RecaptchaService.setToken(val || null);
    if (resolveTokenRef.current && val) {
      resolveTokenRef.current(val);
      resolveTokenRef.current = null;
    }
    props.updateState(StatesToUpdateEnum.FormState);
  };

  return (
    <FormElementWrapper
      formState={formState}
      formComponentAttributes={props}
      uiState={uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      hideLabel={true}
      localErrorMsg={localErrMsg}
      description={props.description}
      showDeleteContentBtn={false}
      hideLoading={true}
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      {siteKey ? (
        size === 'invisible' ? ReactDOM.createPortal(
          <div style={{ position: 'fixed', bottom: 0, right: 0, zIndex: 9999 }}>
            <ReCAPTCHA
              ref={recaptchaRef}
              sitekey={siteKey}
              size="invisible"
              hl={hl}
              onChange={handleVerify}
            />
          </div>,
          document.body,
        ) : (
          <View style={styles.container}>
            <ReCAPTCHA
              ref={recaptchaRef}
              sitekey={siteKey}
              size="normal"
              hl={hl}
              onChange={handleVerify}
            />
          </View>
        )
      ) : (
        <View style={styles.missingKeyError}>
          <AndpIcon name="circleWarning" style={styles.missingKeyErrorIcon} />
          <Text style={styles.missingKeyErrorText}>{t('form_recaptcha_no_sitekey')}</Text>
        </View>
      )}
    </FormElementWrapper>
  );
}

CmsRecaptchaElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  _t: any,
  _formGridOptions?: IFormGridOptions,
  _localValue?: any,
): CommonValidation {
  const token = utils.getValue(state, jsonpath, id);
  if (!token || token === '') {
    return {
      isValid: false,
      errorMessage: updateErrorMessages ? _t('form_recaptcha_error') : undefined,
    };
  }
  return { isValid: true, errorMessage: undefined };
};

CmsRecaptchaElement.getSummaryValue = function () {
  return '';
};

export default CmsRecaptchaElement;
