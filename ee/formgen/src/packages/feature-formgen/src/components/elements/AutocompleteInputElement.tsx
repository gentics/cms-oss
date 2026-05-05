import React, { useState, useRef, useEffect, useMemo } from 'react';
import { View, TouchableOpacity, ActivityIndicator } from 'react-native';
import { getI18nText } from '../../utils/Utils';
import * as utils from '../../utils/Utils';
import { TFunction } from 'i18next';
import {
  IFormComponentAttributes,
  IAutocompleteValue,
  IElementIdentifier,
  FormState,
  IFormGridOptions,
  CommonValidation,
} from '../../types/interfaces';
import { FormElementWrapper } from './helper/FormElementWrapper';
import * as validationUtils from '../../utils/ValidationUtils';
import { SummaryElement } from './helper/SummaryElement';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { formElementWrapperTheme } from '@andp/core/src/styles/theme';
import * as formEventsUtils from '../../utils/FormEventsUtils';
import AndpText from '@andp/core/src/components/AndpText';
import { autocompleteInputElementStyle } from '../../styles/common/common-style';
import { autocompleteInputElementStyle as customAutocompleteInputElementStyle } from '../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import BaseDropDownInputElement from './DropDownElements/BaseDropDownInputElement';

function AutocompleteInputElement(props: IFormComponentAttributes) {
  const t = props.t;
  const [inputValue, setInputValue] = useState(props.value || '');
  const [autocompleteValues, setAutocompleteValues] = useState<IAutocompleteValue[]>([]);
  const [feedback, setFeedback] = useState(
    props.ANFieldLoadError ? t('form_errorBackOfficeCall') : ''
  );
  const [loading, setLoading] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [hasErrors, setError] = useState(props.error);
  const styles = useMemo(() => mergeStyles(autocompleteInputElementStyle, customAutocompleteInputElementStyle), [])

  const stateRef = useRef(inputValue);
  stateRef.current = inputValue;

  const debounceRef = useRef<NodeJS.Timeout | null>(null);
  const blurCommitRef = useRef<NodeJS.Timeout | null>(null);

  const elementSchema = utils.getSchemaForElement(props.formState.schema, props.id);
  const autoCompleteMinLength = elementSchema.autoCompleteMinLength;
  const elementRef = useRef<any>(null);

  if (props.summary && props.summary > 0) return renderSummary(props);

  useEffect(() => {
    props.updateFirstErrorElementRef && props.updateFirstErrorElementRef(props, elementRef);
  });

  useEffect(() => {
    if (hasErrors !== props.error) setError(props.error);
  }, [props.error]);

  useEffect(() => {
    if (props.ANFieldLoadError) setFeedback(t('form_errorBackOfficeCall'));
    if (props.value != inputValue) setInputValue(props.value);
  }, [props.ANFieldLoadError, props.error, props.value]);

  useEffect(() => {
    return () => {
      if (blurCommitRef.current) {
        clearTimeout(blurCommitRef.current);
        blurCommitRef.current = null;
      }
    };
  }, []);

  const updateValue = async (newVal: any) => {
    await formEventsUtils.handleChange(
      props.formState,
      props.uiState,
      props.restService,
      props.jsonpath,
      props.id,
      true,
      props.updateState,
      newVal,
      props.formGridOptions,
      props.editable
    );
  };

  const clearInput = () => {
    setInputValue('');
    setAutocompleteValues([]);
    updateValue(props.isList ? [] : null);
  };

  const resetLoading = (autocompleteDataElement: any) => {
    if (
      (autocompleteDataElement &&
        autocompleteDataElement.autoCompleteSearchValue === stateRef.current) || props.uiState.isMockForm
    ) {
      setAutocompleteValues(autocompleteDataElement.autoCompleteResults);
      setLoading(false);
    }
  };

  const onInputChange = (value: string) => {
    setInputValue(value);
    setLoading(true);

    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
      if (value && (!autoCompleteMinLength || value.length >= autoCompleteMinLength)) {
        props.handleAutocomplete?.(props.jsonpath, value, resetLoading);
      } else {
        setAutocompleteValues([]);
        setLoading(false);
      }
    }, 300);
  };

  const selectAutocompleteValue = (clickedElem: IAutocompleteValue) => {
    if (blurCommitRef.current) {
      clearTimeout(blurCommitRef.current);
      blurCommitRef.current = null;
    }
    const changedElements: IElementIdentifier[] = [];
    if (clickedElem.values) {
      for (const key in clickedElem.values) {
        const prefillValue = clickedElem.values[key];
        let jsonpath = '$..';
        if (props.jsonpathParent) jsonpath = props.jsonpathParent + '..';
        jsonpath = jsonpath + '["' + key + '"]';
        const currentValue = utils.getValue(props.formState, jsonpath, key);
        if (currentValue !== prefillValue) {
          utils.setValue(props.formState, jsonpath, prefillValue);
          changedElements.push({ jsonpath, schemaID: key });
        }
      }
    }
    setAutocompleteValues([]);
    let toChangeValue =
      clickedElem.values ? clickedElem.values[props.id] : clickedElem.displayName;
    if (toChangeValue === '') toChangeValue = clickedElem.displayName;
    utils.setValue(props.formState, props.jsonpath, toChangeValue);
    changedElements.push({ jsonpath: props.jsonpath, schemaID: props.id });
    props.afterChangeMultiple && props.afterChangeMultiple(changedElements);
    setInputValue(toChangeValue);
    setDropdownOpen(false);
  };

  const handleFocus = () => {
    setDropdownOpen(true);
  };

  const handleBlur = () => {
    setTimeout(() => setDropdownOpen(false), 100);

    // Cancel any existing commit timers to avoid race conditions
    if (blurCommitRef.current) {
      clearTimeout(blurCommitRef.current);
      blurCommitRef.current = null;
    }

    // Commit the *latest* value after a delay, using a ref to avoid stale closures - Important because of the state problem -> It needs to be the last value
    blurCommitRef.current = setTimeout(() => {

      let valueState = utils.getValue(props.formState, props.jsonpath);
      if (valueState != stateRef.current) {
        updateValue(stateRef.current);
      }
      blurCommitRef.current = null;
    }, 0);
  };

  const labelText =
    getI18nText(props.formState, props.label) + (props.required ? ' *' : '');

  const renderOption = ({
    item,
    index,
  }: {
    item: IAutocompleteValue;
    index: number;
  }) => (
    <TouchableOpacity
      accessibilityRole="button"
      // style={[styles.optionItem, index === 0 && styles.activeItem]} // TODO
      onPress={() => selectAutocompleteValue(item)}
    >
      <AndpText style={[autocompleteInputElementStyle.dropdownListItemText,
      props.error && autocompleteInputElementStyle.dropdownListItemTextError]}>
        {item.displayName}
      </AndpText>
    </TouchableOpacity>
  );

  return (
    <FormElementWrapper
      formState={props.formState}
      uiState={props.uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      formComponentAttributes={props}
      labelId={props.id}
      showDeleteContentBtn={false}
      needFloatingLabel={false}
      hideLabel={true}
      hideLoading={true}
      hideAnFieldLoadErrorIcon={true}
      localErrorMsg={hasErrors || props.error || ''}
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      <BaseDropDownInputElement
        jsonpath={props.jsonpath}
        t={props.t}
        ref={elementRef}
        inputValue={inputValue}
        onChangeInputValue={onInputChange}
        onFocus={handleFocus}
        onBlur={handleBlur}
        clearInput={clearInput}
        isList={false}
        selectedValues={[]}
        labelText={labelText}
        placeholderText={labelText}
        dropdownOpen={dropdownOpen}
        hideDropdownArrow
        loading={loading}
        ANFieldLoadError={props.ANFieldLoadError}
        error={hasErrors}
        value={inputValue}
        dropdownData={autocompleteValues}
        dropdownRenderItem={renderOption}
        dropdownOnSelect={selectAutocompleteValue}
        disabled={!props.editable}
      />
    </FormElementWrapper>
  );
}

AutocompleteInputElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any
): CommonValidation {
  return validationUtils.commonValidate(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    formGridOptions,
    localValue
  );
};

function renderSummary(props: IFormComponentAttributes) {
  const value: string = props.value;
  return (
    <SummaryElement
      formState={props.formState}
      uiState={props.uiState}
      summaryOption={props.summary}
      label={props.label}
      value={value}
      initUrl={props.restService.initUrl}
      t={props.t}
    />
  );
}

AutocompleteInputElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction
) {
  const schemaState = utils.getSchemaForElement(state.schema, id);
  const value = utils.getValue(state, jsonpath, id);
  if (schemaState.isList) return '';
  return value;
};

export default AutocompleteInputElement;