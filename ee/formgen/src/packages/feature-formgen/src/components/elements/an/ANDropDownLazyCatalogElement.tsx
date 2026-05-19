import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator
} from 'react-native';
import AndpText from '@andp/core/src/components/AndpText';
import { useTranslation } from 'react-i18next';
// import debounce from 'lodash.debounce';
import {
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  IPossibleValue,
  CommonValidation
} from '../../../types/interfaces';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import * as utils from '../../../utils/Utils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import { dropdownInputTheme, formElementWrapperTheme } from '@andp/core/src/styles/theme';
import { TFunction } from 'i18next';
import * as validationUtils from '../../../utils/ValidationUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { dropdownLazyCatalogElementStyles } from '../../../styles/common/common-style';
import { dropdownLazyCatalogElementStyles as customDropdownLazyCatalogElementStyles, formElementWrapperStyles } from '../../../styles/custom/custom-style';
import BaseDropDownInputElement from './DropDownElements/BaseDropDownInputElement';

function ANDropDownLazyCatalogElement(props: IFormComponentAttributes) {
  const { t } = useTranslation();
  const elementRef = useRef<any>(null);
  const styles = useMemo(() => mergeStyles(dropdownLazyCatalogElementStyles, customDropdownLazyCatalogElementStyles), [])
  useEffect(() => {
    props.updateFirstErrorElementRef && props.updateFirstErrorElementRef(props, elementRef);
  });
  // if (props.summary && props.summary > 0) {
  //   const summaryValue = DropDownLazyCatalogElement.getSummaryValue(
  //     props.formState,
  //     props.jsonpath,
  //     props.id,
  //     t
  //   );
  //   return (
  //     <View style={styles.summaryContainer}>
  //       <AndpText style={styles.summaryLabel}>
  //         {utils.getI18nText(props.formState, props.label)}
  //       </AndpText>
  //       <AndpText style={styles.summaryValue}>{summaryValue.toString()}</AndpText>
  //     </View>
  //   );
  // }
  const [inputValue, setInputValue] = useState('');
  const [options, setOptions] = useState<IPossibleValue[]>(props.possibleValues || []);
  const [loading, setLoading] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const selectedValues: IPossibleValue[] =
    props.isList && Array.isArray(props.value)
      ? props.value
      : props.value
        ? [props.value]
        : [];
  const onValueChange = async (newValue: any) => {
    await formEventsUtils.handleChange(
      props.formState,
      props.uiState,
      props.restService,
      props.jsonpath,
      props.id,
      true,
      props.updateState,
      newValue,
      props.formGridOptions,
      props.editable
    );
  };
  const clearInput = () => {
    setInputValue('');
    setOptions([]);
    onValueChange(props.isList ? [] : null);
  };
  const loadOptions = (text: string) => {
    if (props.handleLazyCatalog && !loading) {
      setLoading(true);
      props.handleLazyCatalog(props.jsonpath, text, (possibleValues: IPossibleValue[] | null) => {
        setLoading(false);
        setOptions(possibleValues || []);
      });
    }
  };

  const loadFilteredOptions = (text: string) => {
    const filtered = (props.possibleValues || []).filter((option) =>
      option.displayName.toLowerCase().includes(text.toLowerCase())
    );
    setOptions(filtered);
  };

  useEffect(() => {
    loadFilteredOptions(inputValue);
  }, [inputValue, props.possibleValues]);
  // const debouncedLoadOptions = useCallback(debounce(loadOptions, 500), []);
  // useEffect(() => {
  //   if (dropdownOpen) {
  //     debouncedLoadOptions(inputValue);
  //   }
  // }, [inputValue]);
  const handleFocus = () => {
    setDropdownOpen(true);
    if (props.possibleValues.length === 0) {
      loadOptions('*');
    }
  };
  const handleBlur = () => {
    setTimeout(() => {
      setInputValue('');
      setDropdownOpen(false);
    }, 0);
  };
  const labelText = utils.getI18nText(props.formState, props.label) + (props.required ? " *" : "");
  const placeholderText = !props.isList && utils.isFilled(props.value)
    ? props.value.displayName
    : (props.isList && selectedValues.length > 0
      ? ''
      : utils.getI18nText(props.formState, props.label) + (props.required ? " *" : "")
    );
  const filteredOptions = props.isList
    ? options.filter(option => !selectedValues.some(v => v.identifier === option.identifier))
    : options;
  const onSelectOption = (option: IPossibleValue) => {
    if (props.isList) {
      const exists = selectedValues.find(v => v.identifier === option.identifier);
      const newVal = exists
        ? selectedValues.filter(v => v.identifier !== option.identifier)
        : [...selectedValues, option];
      onValueChange(newVal);
      setInputValue('');
    } else {
      onValueChange(option);
      setInputValue('');
      setDropdownOpen(false);
    }
  };
  const removeChip = (chip: IPossibleValue) => {
    const newValues = selectedValues.filter(v => v.identifier !== chip.identifier);
    onValueChange(newValues);
  };
  const dropdownData = !loading && filteredOptions.length > 0 ? filteredOptions : undefined;
  const renderOption = ({ item }: { item: IPossibleValue }) => (
    <TouchableOpacity
      id={`${props.jsonpath}-${item.identifier}`}
      style={styles.optionItem}
      onPress={() => onSelectOption(item)}
      accessibilityRole="button"
      accessibilityLabel={t('form_accessibility_dropdownCatalog_listItem', { option: item.displayName })}
    >
      <AndpText style={[styles.dropdownListItemText,
          props.error && styles.dropdownListItemTextError]}
       >
        {item.displayName}
      </AndpText>
      {selectedValues.some(v => v.identifier === item.identifier) && (
        <AndpIcon name="checkMark" 
          size={dropdownInputTheme.dropdownList.checkMarkIconSize} 
          color={props.error ? 
            dropdownInputTheme.dropdownList.checkMarkIconColorError : 
            dropdownInputTheme.dropdownList.checkMarkIconColor} />
      )}
    </TouchableOpacity>
  );
  return (
    <FormElementWrapper
      formState={props.formState}
      uiState={props.uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      description={props.description}
      formComponentAttributes={props}
      labelId={`label-${props.jsonpath}`}
      showDeleteContentBtn={false}
      needFloatingLabel={false}
      hideLabel={true}
      hideLoading={true}
      hideAnFieldLoadErrorIcon={true}
      localErrorMsg={props.error || ""}
      jsonPath={props.jsonpath}
      objectId={props.id}
      selectedElementId={props.selectedElementId}
    >
      <View style={{ zIndex: 10 }}>
        <BaseDropDownInputElement
          ref={elementRef}
          jsonpath={props.jsonpath}
          t={props.t}
          inputValue={inputValue}
          onChangeInputValue={setInputValue}
          onFocus={handleFocus}
          onBlur={handleBlur}
          clearInput={clearInput}
          isList={props.isList}
          selectedValues={selectedValues}
          labelText={labelText}
          placeholderText={placeholderText}
          dropdownOpen={dropdownOpen}
          loading={loading}
          ANFieldLoadError={props.ANFieldLoadError}
          error={props.error}
          value={props.value}
          onRemoveChip={removeChip}
          dropdownData={dropdownData}
          dropdownRenderItem={dropdownData ? renderOption : undefined}
          dropdownOnSelect={onSelectOption}
          disabled={!props.editable}
        >
          {loading ? (
            <AndpText style={[styles.activityText, props.error && styles.activityTextError]}>
              {t('form_loading')}
            </AndpText>
          ) : inputValue.trim().length === 0 && options.length === 0 ? (
            <AndpText style={[styles.activityText, props.error && styles.activityTextError]}>
              {t('form_startTyping')}
            </AndpText>
          ) : filteredOptions.length === 0 ? (
            <AndpText style={[styles.activityText, props.error && styles.activityTextError]}>
              {t('noResults')}
            </AndpText>
          ) : null} 
        </BaseDropDownInputElement>
      </View>
    </FormElementWrapper>
  );
}

ANDropDownLazyCatalogElement.validate = function (
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

ANDropDownLazyCatalogElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: any
) {
  const schemaState = utils.getSchemaForElement(state.schema, id);
  const value = utils.getValue(state, jsonpath, id);
  let currentValue: IPossibleValue | null = null;
  if (value && value.displayName) {
    currentValue = value;
  }
  let summaryValue = currentValue !== null ? currentValue.displayName : '';
  if (schemaState.isList && Array.isArray(value)) {
    value.forEach((element: IPossibleValue) => {
      if (summaryValue !== '') summaryValue = summaryValue + ', ';
      summaryValue = summaryValue + element.displayName;
    });
  }

  return summaryValue;
};

export default ANDropDownLazyCatalogElement;