import React, { useEffect, useState, useRef } from 'react';
import {
  View,
} from 'react-native';
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
import { StatesToUpdateEnum } from '../../../constants/enums/StateToUpdateEnum';
import * as formEventsUtils from '../../../utils/FormEventsUtils';


/**
 * BooleanElement
 * Renders a boolean form element as a single checkbox-style radio button.
 *
 * This component stores its value in the form state as the strings `"True"`
 * and `"False"` rather than native booleans. When the user presses the input,
 * it toggles the current value, writes the updated value to the form state via
 * `utils.setValue`, and triggers a form state refresh through
 * `props.updateState(StatesToUpdateEnum.FormState)`.
 *
 * Behavior:
 * - Builds the visible label from the translated field label and appends `*`
 *   when the field is required.
 * - Registers its wrapper view as a possible first error element so validation
 *   can focus or scroll to it.
 * - Mirrors external validation errors into local state so the wrapper can
 *   display the current error message.
 * - Renders in summary mode when `props.summary > 0`, showing a translated
 *   Yes/No value instead of an editable control.
 * - Uses `AndpRadioButton` with `type="checkbox"` to provide the interactive
 *   boolean input, including disabled and accessibility states.
 *
 * Validation is delegated to `validationUtils.commonValidate`, and summary
 * values are translated to `form_Yes` / `form_No` based on the stored string
 * value.
 */
function BooleanElement(props: IFormComponentAttributes) {
  const { t } = useTranslation();
  const itemId = props.id;
  let labeltext =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : '');

  const formState = props.formState;
  const uiState = props.uiState;

  const elementRef = useRef<View>(null);

  useEffect(() => {
    props.updateFirstErrorElementRef && props.updateFirstErrorElementRef(props, elementRef);
  });
  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const handleInputChange = () => {
    const newValue = !isChecked();
    formEventsUtils.handleChange(
      formState,
      props.uiState,
      props.restService,
      props.jsonpath,
      itemId,
      false,
      props.updateState,
      newValue,
      props.formGridOptions,
      props.editable,
    );
  };

  const isChecked = () => {
    return props.value === true;
  };

  if (props.summary > 0) {
    return renderSummary(props, t);
  }

  
  return (
    <FormElementWrapper
      formState={formState}
      uiState={uiState}
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
  )
}

BooleanElement.validate = function (
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

BooleanElement.getSummaryValue = function (
  formState: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
) {
  const value = utils.getValue(formState, jsonpath, id);
  if (value === true) {
    return t('form_Yes');
  } else if (value === false) {
    return t('form_No');
  } else {
    return '';
  }
};

function renderSummary(props: IFormComponentAttributes, t: TFunction) {
  return (
    <SummaryElement
      initUrl={props.restService.initUrl}
      formState={props.formState}
      uiState={props.uiState}
      summaryOption={props.summary}
      label={props.label}
      t={props.t}
      value={BooleanElement.getSummaryValue(
        props.formState,
        props.jsonpath,
        props.id,
        t,
      )}
    />
  );
}

export default BooleanElement;
