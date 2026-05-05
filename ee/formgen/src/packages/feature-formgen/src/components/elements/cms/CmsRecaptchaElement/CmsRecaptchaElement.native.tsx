import React, { useEffect, useMemo, useRef, useState } from 'react';
import { TouchableOpacity, Text, StyleSheet, View } from 'react-native';
import Recaptcha from 'react-native-recaptcha-that-works';
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
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { recaptchaElementStyles } from '../../../../styles/common/common-style.native';
import { recaptchaElementStyles as customRecaptchaElementStyles } from '../../../../styles/custom/custom-style';

/**
 * CmsRecaptchaElement renders a Google reCAPTCHA v2 widget on native (iOS/Android)
 * using `react-native-recaptcha-that-works`, which shows the challenge in a modal.
 *
 * The `siteKey` is read from `formGridOptions.siteKey` (set in the form's ui-schema).
 * The `mode` is read from `formGridOptions.mode` — either 'checkbox' or 'invisible'.
 *
 * Domain/project-specific behavior:
 * - If no `siteKey` is configured → form-level error (`formState.data.hasErrors = true`).
 * - Token storage and RestService header injection are identical to the web variant.
 * - On token expiry, the stored token is cleared.
 * - `validate` and `getSummaryValue` are identical to the web variant.
 */
function CmsRecaptchaElement(props: IFormComponentAttributes) {
  const { formState, uiState, t } = props;
  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  const [verified, setVerified] = useState(false);
  const recaptchaRef = useRef<any>(null);
  const styles = useMemo(
    () => mergeStyles(recaptchaElementStyles, customRecaptchaElementStyles),
    [],
  );

  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const siteKey: string | undefined = props.formGridOptions?.siteKey;
  const mode = props.formGridOptions?.mode ?? 'checkbox';
  const size: 'normal' | 'invisible' = mode === 'invisible' ? 'invisible' : 'normal';

  useEffect(() => {
    RecaptchaService.markRequired();
  }, []);

  useEffect(() => {
    if (!siteKey) {
      formState.data.hasErrors = true;
      props.updateState(StatesToUpdateEnum.FormState);
    }
  }, [siteKey]);

  const handleVerify = (token: string) => {
    utils.setValue(formState, props.jsonpath, token);
    RecaptchaService.setToken(token || null);
    setVerified(true);
    props.updateState(StatesToUpdateEnum.FormState);
  };

  const handleExpire = () => {
    utils.setValue(formState, props.jsonpath, '');
    RecaptchaService.setToken(null);
    setVerified(false);
    props.updateState(StatesToUpdateEnum.FormState);
  };

  return (
    <FormElementWrapper
      formState={formState}
      formComponentAttributes={props}
      uiState={uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      hideLabel={true}
      localErrorMsg={siteKey ? localErrMsg : t('form_recaptcha_no_sitekey')}
      description={props.description}
      showDeleteContentBtn={false}
      hideLoading={true}
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      {siteKey ? (
        <View>
          <TouchableOpacity
            style={[styles.button, verified && styles.buttonVerified]}
            onPress={() => recaptchaRef.current?.open()}
            activeOpacity={0.8}
          >
            <Text style={styles.buttonText}>
              {verified ? t('form_recaptcha_verified') : t('form_recaptcha_open')}
            </Text>
          </TouchableOpacity>
          <Recaptcha
            ref={recaptchaRef}
            siteKey={siteKey}
            baseUrl=""
            onVerify={handleVerify}
            onExpire={handleExpire}
            size={size}
          />
        </View>
      ) : null}
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
