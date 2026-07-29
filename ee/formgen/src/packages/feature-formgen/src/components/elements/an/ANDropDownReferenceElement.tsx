import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { View, TouchableOpacity, Platform, ActivityIndicator } from 'react-native';
//@ts-ignore
import debounce from 'lodash.debounce';
import { useTranslation } from 'react-i18next';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { dropdownInputTheme, formElementWrapperTheme } from '@andp/core/src/styles/theme';
import * as utils from '../../../utils/Utils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import * as validationUtils from '../../../utils/ValidationUtils';
import { TFunction } from 'i18next';
import { FormState, IFormComponentAttributes, IFormGridOptions, IPossibleValue, CommonValidation } from '../../../types/interfaces';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import { dropdownReferenceElementStyles } from '../../../styles/common/common-style';
import { dropdownReferenceElementStyles as customDropdownReferenceElementStyles} from '../../../styles/common/common-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import BaseDropDownInputElement from './DropDownElements/BaseDropDownInputElement';

function ANDropDownReferenceElement(props: IFormComponentAttributes) {
  const { t } = useTranslation();
  const elementSchema = utils.getSchemaForElement(props.formState.schema, props.id);
  const searchReferenceValueMinLength = elementSchema.searchReferenceValueMinLength;
  const [hasErrors, setError] = useState(props.error);
  const [inputValue, setInputValue] = useState('');
  const [options, setOptions] = useState<IPossibleValue[]>([]);
  const [loading, setLoading] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const styles = useMemo(() => mergeStyles(dropdownReferenceElementStyles, customDropdownReferenceElementStyles), [])

  const selectedValues: IPossibleValue[] = props.isList && Array.isArray(props.value) ? props.value : props.value ? [props.value] : [];
  const elementRef = useRef<any>(null);
  useEffect(() => {
    props.updateFirstErrorElementRef && props.updateFirstErrorElementRef(props, elementRef);
  });

  const updateValue = async (newValue: any) => {
    await formEventsUtils.handleChange(props.formState, props.uiState, props.restService, props.jsonpath, props.id, true, props.updateState, newValue, props.formGridOptions, props.editable);
  };

  useEffect(() => {
    if (hasErrors !== props.error) setError(props.error);
  }, [props.error]);

  const loadOptions = useCallback(async (text: string) => {
    const trimmed = (text ?? '').trim();
    // If empty, do nothing (no requests, no dropdown data)
    if (trimmed.length === 0) {
      setOptions([]);
      return;
    }
    if (
      searchReferenceValueMinLength &&
      trimmed.length < searchReferenceValueMinLength //<= or <
    ) {
      setOptions([]);
      return;
    }
    if (props.handleSearch && loading !== true) {
      setLoading(true);
      try {
        // Support both callback-style and promise-style handleSearch
        const loaded = await new Promise<IPossibleValue[] | null>((resolve) => {
          const possible = (props.handleSearch as any)(
            props.jsonpath,
            trimmed,
            (res: IPossibleValue[] | null) => resolve(res)
          );
          if (possible && typeof possible.then === 'function') {
            possible.then(resolve);
          }
        });
        setOptions(loaded || []);
      } finally {
        setLoading(false);
      }
    }
  }, [props.jsonpath, props.handleSearch, searchReferenceValueMinLength, loading]);

  // loadOptions must not be captured directly by the debounce instance: useRef keeps
  // only the value of the first render, which would pin the mount-time props (and with
  // them a stale formState/handleSearch/updateState) for the lifetime of the element.
  const loadOptionsRef = useRef(loadOptions);
  useEffect(() => {
    loadOptionsRef.current = loadOptions;
  });

  const debouncedLoadOptions = useRef(
    debounce((text: string) => loadOptionsRef.current(text), 500),
  ).current;

  const handleChangeInputValue = (value: string) => {
    if (loading) return;
    setInputValue(value);
  };

  useEffect(() => {
    return () => {
      debouncedLoadOptions.cancel();
    };
  }, [debouncedLoadOptions]);
  
  useEffect(() => {
    if (dropdownOpen && inputValue.trim().length > 0) {
      debouncedLoadOptions(inputValue);
    }else if(options.length > 0 && inputValue.trim().length == 0){
      setOptions([]);
    }
  }, [inputValue, dropdownOpen]);

  const handleFocus = () => {
    setDropdownOpen(true);
  };
  const handleBlur = () => {
    setTimeout(() => {
      setInputValue('');
      setOptions([]);
      setDropdownOpen(false);

    }, 0);
  };
  const clearInput = () => {
    if (loading) return;
    setInputValue('');
    setOptions([]); 
    updateValue(props.isList ? [] : null);
  };

  const labelText = utils.getI18nText(props.formState, props.label) + (props.required ? ' *' : '');  
  const placeholderText = !props.isList && utils.isFilled(props.value)
    ? props.value.displayName
    : (props.isList && selectedValues.length > 0 ? '' : utils.getI18nText(props.formState, props.label) + (props.required ? ' *' : ''));
  const filteredOptions = props.isList ? options.filter(option => !selectedValues.some(v => v.identifier === option.identifier)) : options;
  const onSelectOption = (option: IPossibleValue) => {
    if (loading) return;
    if (props.isList) {
      const exists = selectedValues.find(v => v.identifier === option.identifier);
      const newVal = exists ? selectedValues.filter(v => v.identifier !== option.identifier) : [...selectedValues, option];
      updateValue(newVal);
      setInputValue('');
      setOptions([]);
    } else {
      updateValue(option);
      setInputValue('');
      //setDropdownOpen(false);
      setOptions([]);
    }
  };
  const removeChip = (chip: IPossibleValue) => {
    const newValues = selectedValues.filter(
      (v) => v.identifier !== chip.identifier,
    );
    updateValue(newValues);
  };

  const renderOption = ({ item }: { item: IPossibleValue }): React.ReactElement => {
    return (
      <TouchableOpacity
        id={`${props.jsonpath}-${item.identifier}`}
        disabled={loading}
        accessible
        accessibilityHint={t('form_accessibility_button')}
        accessibilityLabel={`${t("form_accessibility_dropdown_listItem")} ${item.displayName}`} 
        accessibilityRole="button" 
        onPress={() => onSelectOption(item)}>
        <AndpText style={[styles.dropdownListItemText,
          hasErrors && styles.dropdownListItemTextError]}
        >
          {item.displayName}
        </AndpText>
        {selectedValues.some(v => v.identifier === item.identifier) && (
          <AndpIcon name="checkMark" 
            size={dropdownInputTheme.dropdownList.checkMarkIconSize} 
            color={hasErrors ? 
              dropdownInputTheme.dropdownList.checkMarkIconColorError : 
              dropdownInputTheme.dropdownList.checkMarkIconColor} />
        )}
      </TouchableOpacity>
    );
  };

  const dropdownData = !loading && inputValue.trim().length > 0 && filteredOptions.length > 0 ? filteredOptions : [];


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
      localErrorMsg={hasErrors || ""}
      jsonPath={props.jsonpath}
      objectId={props.id}
      selectedElementId={props.selectedElementId}
    >
      <View ref={containerRef as any} style={{ zIndex: 10 }}>
        <BaseDropDownInputElement
          ref={elementRef}
          jsonpath={props.jsonpath}
          t={props.t}
          inputValue={inputValue}
          onChangeInputValue={handleChangeInputValue}
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
          error={hasErrors}
          value={props.value}
          onRemoveChip={removeChip}
          dropdownData={dropdownData}
          dropdownRenderItem={dropdownData ? renderOption : undefined}
          dropdownOnSelect={onSelectOption}
          disabled={!props.editable || loading}
        >
          {loading ? (
            <AndpText accessible={true} accessibilityHint={t('form_accessibility_dropdown_loading')} accessibilityLabel={t('form_loading')} 
              style={[styles.activityText, hasErrors && styles.activityTextError]}>
              {t('form_loading')}
            </AndpText>
          ) : inputValue.trim().length === 0 && options.length === 0 ? (
            <AndpText style={[styles.activityText, hasErrors && styles.activityTextError]} accessible={true} accessibilityHint={t('form_accessibility_dropdown_startTyping')} accessibilityLabel={t('form_startTyping')}>
              {t('form_startTyping')}
            </AndpText>
          ) : (searchReferenceValueMinLength && searchReferenceValueMinLength > 0 && inputValue.trim().length > 0 && inputValue.trim().length <= searchReferenceValueMinLength) ? (
            <AndpText style={[styles.activityText, hasErrors && styles.activityTextError]} accessible={true} accessibilityHint={t('form_minValue', { count: searchReferenceValueMinLength })} accessibilityLabel={t('form_minValue', { count: searchReferenceValueMinLength })}>
              {t('form_minValue', { count: searchReferenceValueMinLength })}
            </AndpText>
          ) : filteredOptions.length === 0 ? (
            <AndpText style={[styles.activityText, hasErrors && styles.activityTextError]} accessible={true} accessibilityHint={t('form_accessibility_dropdown_noResults')} accessibilityLabel={t('noResults')}>
              {t('noResults')}
            </AndpText>
          ) : null}
        </BaseDropDownInputElement>
      </View>
    </FormElementWrapper>
  );
}

ANDropDownReferenceElement.validate = function (state: FormState, jsonpath: string, id: string, updateErrorMessages: boolean, t: TFunction, formGridOptions?: IFormGridOptions, localValue?: any): CommonValidation {
  return validationUtils.commonValidate(state, jsonpath, id, updateErrorMessages, formGridOptions, localValue);
};

ANDropDownReferenceElement.getSummaryValue = function (state: FormState, jsonpath: string, id: string, t: TFunction, formGridOptions?: IFormGridOptions): string {
  const value = utils.getValue(state, jsonpath, id);
  if (!value) return '';
  return Array.isArray(value) ? value.map((v: any) => v.displayName).join(', ') : value.displayName || '';
};

export default ANDropDownReferenceElement;