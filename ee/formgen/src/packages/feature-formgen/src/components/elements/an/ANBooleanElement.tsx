import React, { useEffect, useState, useRef, useMemo } from 'react';
import {
  Text,
  View,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
} from '../../../types/interfaces';
import { booleanElementStyles } from '../../../styles/common/common-style';
import { booleanElementStyles as customBooleanElementStyles } from '../../../styles/custom/custom-style';
import { TFunction } from 'i18next';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import * as utils from '../../../utils/Utils';
import { useTranslation } from 'react-i18next';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import * as validationUtils from '../../../utils/ValidationUtils';
import { SummaryElement } from '../helper/SummaryElement';
import AndpText from '@andp/core/src/components/AndpText';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { formElementWrapperTheme } from '@andp/core/src/styles/theme';
import AndpRadioButton from '@andp/core/src/components/AndpRadioButton';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import AndpButton from '@andp/core/src/components/AndpButton';
import BaseDropDownInputElement from './DropDownElements/BaseDropDownInputElement';

/**
 * BooleanElement
 */
function ANBooleanElement(props: IFormComponentAttributes) {
  // using strings to avoid react ignoring the value false for an attribute
  // using leading uppercase because this is what we get from the BO
  const TrueString = 'True';
  const FalseString = 'False';
  const { t } = useTranslation();
  const itemId = props.id;
  let labeltext =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : '');

  const formState = props.formState;

  const elementRef = useRef<View>(null);

  useEffect(() => {
    props.updateFirstErrorElementRef && props.updateFirstErrorElementRef(props, elementRef);
  });
  const [isFocused, setIsFocused] = useState(false);
  const styles = useMemo(() => mergeStyles(booleanElementStyles, customBooleanElementStyles), []);

  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const handleButtonPress = () => {
    handleChange(TrueString);
  };

  const handleInputChange = () => {
    const newValue = !isChecked();
    handleChange(newValue ? TrueString : FalseString);
  };

  const handleChange = (newValue: string) => {
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
      props.editable
    );
  };

  const isChecked = () => {
    return props.value === TrueString ? true : false;
  };

  if (props.summary > 0) {
    return renderSummary(props, t);
  }

  // dropdown instead of checkbox
  if (props.formGridOptions?.dropdown) {
    const [isFocused, setIsFocused] = useState(false);
    return (
      <FormElementWrapper
        formState={formState}
        uiState={props.uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        formComponentAttributes={props}
        needFloatingLabel={false}
        labelId={props.jsonpath}
        hideLabel={true}
        hideLoading={true}
        isDropdown={true}
        localErrorMsg={localErrMsg}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View style={{ zIndex: 10 }}>
          <BaseDropDownInputElement
            ref={elementRef}
            t={props.t}
            inputValue={labeltext}
            onChangeInputValue={() => { }}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            isList={false}
            onlyDropdown={true}
            labelText={labeltext}
            placeholderText={props.value ? t('form_Yes') : t('form_No')}
            dropdownOpen={isFocused}
            error={props.error}
            value={
              props.value === TrueString
                ? { displayName: t('form_Yes'), identifier: TrueString }
                : props.value === FalseString
                  ? { displayName: t('form_No'), identifier: FalseString }
                  : { displayName: '---', identifier: '' }
            }
            dropdownData={[
              { displayName: '---', identifier: '' },
              { displayName: t('form_Yes'), identifier: TrueString },
              { displayName: t('form_No'), identifier: FalseString },
            ]}
            dropdownRenderItem={({ item, index }) => (
              <TouchableOpacity
                accessible
                accessibilityLabel={item.displayName}
                accessibilityState={{
                  selected: item.identifier === props.value,
                  disabled: !props.editable,
                }}
                onPress={() =>
                  formEventsUtils.handleChange(
                    formState,
                    props.uiState,
                    props.restService,
                    props.jsonpath,
                    itemId,
                    false,
                    props.updateState,
                    item.identifier,
                    props.formGridOptions,
                    props.editable
                  )
                }
              >
                <AndpText>
                  {item.displayName}
                </AndpText>
                {item.identifier === props.value && (
                  <AndpIcon
                    name="checkMark"
                    size="md"
                    color={props.error ? '#dc0018' : undefined}
                  />
                )}
              </TouchableOpacity>
            )}
            dropdownOnSelect={(item: any) =>
              formEventsUtils.handleChange(
                formState,
                props.uiState,
                props.restService,
                props.jsonpath,
                itemId,
                false,
                props.updateState,
                item.identifier,
                props.formGridOptions,
                props.editable
              )
            }
            disabled={!props.editable}
            loading={props.loading}
            ANFieldLoadError={props.ANFieldLoadError}
          />
        </View>
      </FormElementWrapper>
    );
  }
  // button instead of checkbox
  if (props.formGridOptions?.button) {
    return (
      <FormElementWrapper
        formState={formState}
        uiState={props.uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        formComponentAttributes={props}
        description={props.description}
        hideLabel={true}
        hideLoading={true}
        isDropdown={true}
        localErrorMsg={localErrMsg}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View ref={elementRef}>
          <AndpButton
            title={labeltext}
            disabled={!props.editable}
            onPress={handleButtonPress}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            accessibilityHint={props.error
                ? t('form_accessibility_boolean_button_error_hint', {
                  error: localErrMsg,
                })
                : t('form_accessibility_boolean_button_hint')}
            accessibilityState={{
              disabled: !props.editable,
            }}
            accessibilityLiveRegion="assertive"
            wrapperStyle={{ width: 'unset', padding: 0}}
            buttonStyle={[
              styles.booleanButton,
              !props.editable && styles.booleanButtonDisabled,
              props.error && styles.booleanButtonError,
              isFocused && styles.booleanButtonFocused
            ]}
            textStyle={[
              styles.booleanButtonText,
              !props.editable && styles.booleanButtonTextDisabled,
              props.error && styles.booleanButtonTextError,
            ]}
            type={'primary'}
          />
        </View>
      </FormElementWrapper>
    );
  }
  // radio buttons
  if (props.formGridOptions?.radio) {
    return (
      <FormElementWrapper
        formState={formState}
        uiState={props.uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        formComponentAttributes={props}
        description={props.description}
        hideLabel={true}
        hideLoading={true}
        localErrorMsg={localErrMsg}
        isSmallerWarning={true}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View
          ref={elementRef}
          style={styles.radioButtonGroupContainer}
          pointerEvents={!props.editable ? 'none' : 'auto'}
        >
          <AndpText
            style={[
              styles.radioButtonGroupLabel,
              props.error && props.editable && styles.radioButtonGroupLabelError,
            ]}
          >
            {labeltext}
          </AndpText>
          <View style={styles.radioButtonGroup}>
            <View style={styles.radioButtonRow}>
              <AndpRadioButton
                id={`${props.jsonpath}-radio-btn-true`}
                selected={isChecked()}
                label={t('form_Yes')}
                onPress={() => handleChange(TrueString)}
                disabled={!props.editable}
                error={!!props.error}
                accessibilityLabel={t('form_Yes')}
                accessibilityHint={
                  props.error
                    ? t('form_accessibility_boolean_button_error_hint', {
                      error: localErrMsg,
                    })
                    : t('form_accessibility_boolean_radio_hint')
                }
              />
            </View>
            <View style={styles.radioButtonRow}>  
              <AndpRadioButton
                id={`${props.jsonpath}-radio-btn-false`}
                selected={!isChecked()}
                label={t('form_No')}
                onPress={() => handleChange(FalseString)}
                disabled={!props.editable}
                error={!!props.error}
                accessibilityLabel={t('form_No')}
                accessibilityHint={
                  props.error
                    ? t('form_accessibility_boolean_button_error_hint', {
                      error: localErrMsg,
                    })
                    : t('form_accessibility_boolean_radio_hint')
                }
              />
            </View>
          </View>
        </View>
      </FormElementWrapper>
    );
  }
  // default checkbox
  return (
    <FormElementWrapper
      formState={formState}
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
  )
}

ANBooleanElement.validate = function (
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

ANBooleanElement.getSummaryValue = function (
  formState: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
) {
  const value = utils.getValue(formState, jsonpath, id);
  if (value === 'True') {
    return t('form_Yes');
  } else if (value === 'False') {
    return t('form_No');
  } else {
    return ''; // ?
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
      value={ANBooleanElement.getSummaryValue(
        props.formState,
        props.jsonpath,
        props.id,
        t,
      )}
    />
  );
}

export default ANBooleanElement;
