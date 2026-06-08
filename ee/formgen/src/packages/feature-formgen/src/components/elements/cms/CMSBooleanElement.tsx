import React, { useEffect, useState, useRef } from 'react';
import { View } from 'react-native';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
} from '../../../types/interfaces';
import { TFunction } from 'i18next';
import * as utils from '../../../utils/Utils';
import { useTranslation } from 'react-i18next';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import * as validationUtils from '../../../utils/ValidationUtils';
import { SummaryElement } from '../helper/SummaryElement';
import AndpRadioButton from '@andp/core/src/components/AndpRadioButton';
import * as formEventsUtils from '../../../utils/FormEventsUtils';

/**
 * CMS-specific boolean element.
 *
 * Checked state = value equals formGridOptions.storedValue (a plain string
 * configured in the CMS form definition). When checked the storedValue is
 * written to form state; when unchecked an empty string is written, so the
 * Mesh field is cleared instead of carrying the storedValue as a phantom
 * default.
 */
function CMSBooleanElement(props: IFormComponentAttributes) {
  const { t } = useTranslation();
  const labeltext =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : ''); 

  const elementRef = useRef<View>(null);
  const [localErrMsg, setLocalErrMsg] = useState(props.error);

  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const storedValue = props.formGridOptions?.storedValue ?? '';

  const isChecked = () =>
    storedValue !== '' && props.value === storedValue;

  const handleInputChange = () => {
    const nextValue = isChecked() ? '' : storedValue;
    formEventsUtils.handleChange(
      props.formState,
      props.uiState,
      props.restService,
      props.jsonpath,
      props.id,
      false,
      props.updateState,
      nextValue,
      props.formGridOptions,
      props.editable,
    );
  };

  if (props.summary > 0) {
    return (
      <SummaryElement
        initUrl={props.restService.initUrl}
        formState={props.formState}
        uiState={props.uiState}
        summaryOption={props.summary}
        label={props.label}
        t={props.t}
        value={CMSBooleanElement.getSummaryValue(
          props.formState,
          props.jsonpath,
          props.id,
          t,
          props.formGridOptions,
        )}
      />
    );
  }

  return (
    <FormElementWrapper
      formState={props.formState}
      uiState={props.uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      description={props.description}
      formComponentAttributes={props}
      hideLabel={true}
      hideLoading={true}
      localErrorMsg={localErrMsg}
      isSmallerWarning={true}
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      <View ref={elementRef}>
        <AndpRadioButton
          selected={isChecked()}
          label={labeltext}
          onPress={handleInputChange}
          disabled={!props.editable}
          error={!!props.error}
          type="checkbox"
          accessibilityLabel={labeltext}
          accessibilityHint={
            props.error
              ? t('form_accessibility_boolean_checkbox_error_hint', { error: localErrMsg })
              : t('form_accessibility_boolean_checkbox_hint')
          }
        />
      </View>
    </FormElementWrapper>
  );
}

CMSBooleanElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  return validationUtils.commonValidate(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    formGridOptions,
    localValue,
  );
};

CMSBooleanElement.getSummaryValue = function (
  formState: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
): string {
  const value = utils.getValue(formState, jsonpath, id);
  const storedValue = formGridOptions?.storedValue ?? '';
  return storedValue !== '' && value === storedValue ? storedValue : '';
};

export default CMSBooleanElement;
