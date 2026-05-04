import React, { useState, useEffect, useRef, useMemo } from 'react';
import { View, TouchableOpacity, Dimensions } from 'react-native';
import { TFunction } from 'i18next';
import * as validationUtils from '../../utils/ValidationUtils';
import {
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  CommonValidation,
} from '../../types/interfaces';
import { FormElementWrapper } from './helper/FormElementWrapper';
import * as utils from '../../utils/Utils';
import * as formEventsUtils from '../../utils/FormEventsUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { dropdownInputTheme } from '@andp/core/src/styles/theme';
import {
  booleanElementStyles,
  dropdownCatalogElementStyles,
} from '../../styles/common/common-style';
import {
  booleanElementStyles as customBooleanElementStyles,
  dropdownCatalogElementStyles as customDropdownCatalogElementStyles,
} from '../../styles/custom/custom-style';
import AndpRadioButton from '@andp/core/src/components/AndpRadioButton';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import BaseDropDownInputElement from './DropDownElements/BaseDropDownInputElement';

function isOptionSelected(option: any, selectedValues: any[]): boolean {
  return selectedValues.some((val) => val.identifier === option.identifier);
}

function DropDownCatalogElement(props: IFormComponentAttributes) {
  const { formState, uiState, t } = props;

  const elementRef = useRef<any>(null);
  const styles = useMemo(
    () =>
      mergeStyles(
        dropdownCatalogElementStyles,
        customDropdownCatalogElementStyles,
      ),
    [],
  );
  const booleanStyles = useMemo(
    () => mergeStyles(booleanElementStyles, customBooleanElementStyles),
    [],
  );

  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  const handleSelectionChange = (value: any) => {
    formEventsUtils.handleChange(
      formState,
      uiState,
      props.restService,
      props.jsonpath,
      props.id,
      false,
      props.updateState,
      value,
      props.formGridOptions,
      props.editable,
    );
  };

  // Summary-Fall: Unverändert
  // if (props.summary && props.summary > 0) {
  //   const summaryValue = DropDownCatalogElement.getSummaryValue(
  //     formState,
  //     props.jsonpath,
  //     props.id,
  //     t,
  //     props.formGridOptions,
  //   );
  //   return (
  //     <View style={styles.summaryContainer}>
  //       <AndpText style={styles.summaryLabel}>
  //         {utils.getI18nText(formState, props.label)}
  //       </AndpText>
  //       <AndpText style={styles.summaryValue}>{summaryValue}</AndpText>
  //     </View>
  //   );
  // }

  if (props.formGridOptions?.radio) {
    const type = props.isList ? 'checkbox' : 'radio';
    const currentSelectedValues: any[] = props.value
      ? Array.isArray(props.value)
        ? [...props.value]
        : [props.value]
      : [];
    const handleChangeRadio = (selectedOption: any) => {
      if (type === 'radio') {
        handleSelectionChange(selectedOption);
      } else {
        let updatedValues = [...currentSelectedValues];
        if (isOptionSelected(selectedOption, updatedValues)) {
          updatedValues = updatedValues.filter(
            (elem) => elem.identifier !== selectedOption.identifier,
          );
        } else {
          updatedValues.push(selectedOption);
        }
        handleSelectionChange(updatedValues);
      }
    };

    // Errechne die gewünschte Anzahl an Spalten
    const screenWidth = Dimensions.get('window').width;
    let columnCount: number = 1;
    const rawColumns = props.formGridOptions?.radioColumns as string | undefined;
    const parsedColumns = rawColumns ? Number(rawColumns) : undefined;
    if (
      typeof parsedColumns === 'number' &&
      !Number.isNaN(parsedColumns) &&
      parsedColumns > 1 &&
      screenWidth >= 770
    ) {
      columnCount = Math.max(1, Math.floor(parsedColumns));
    }

    const labelText =
      utils.getI18nText(formState, props.label) + (props.required ? ' *' : '');

    // Single Column Rendering
    if (columnCount === 1) {
      return (
        <FormElementWrapper
          formState={formState}
          uiState={uiState}
          numberOfColumns={props.formGridOptions?.numberOfColumns}
          description={props.description}
          formComponentAttributes={props}
          labelId={'label-' + props.jsonpath}
          handleDeleteEntry={() =>
            handleSelectionChange(type === 'checkbox' ? [] : null)
          }
          showDeleteContentBtn={false}
          needFloatingLabel={false}
          hideLabel={true}
          hideLoading={true}
          hideAnFieldLoadErrorIcon={true}
          localErrorMsg={props.error ? props.error : ""}
          jsonPath={props.jsonpath}
          objectId={props.id}
        >
          <View ref={elementRef} style={booleanStyles.radioButtonGroupContainer}>
            {/* Wir verstecken das Label, wenn ein Formgrid-Element von diesem Element abhängt */}
            {(!props.formState.formgridDependsOn ||
              props.formState.formgridDependsOn.indexOf(props.id) === -1) && (
              <AndpText
                accessibilityLabel={t(
                  'form_accessibility_dropdownCatalog_radio_label',
                  { label: labelText },
                )}
                style={[
                  booleanStyles.radioButtonGroupLabel,
                  props.error &&
                    props.editable &&
                    booleanStyles.radioButtonGroupLabelError,
                ]}
              >
                {labelText}
              </AndpText>
            )}
            {props.possibleValues?.map((option, idx) => (
              <AndpRadioButton
                key={option.identifier || idx}
                id={`${props.jsonpath}-${type}-${idx}`}
                selected={isOptionSelected(option, currentSelectedValues)}
                label={option.displayName}
                onPress={() => handleChangeRadio(option)}
                disabled={!props.editable}
                error={
                  props.error && props.error != null && props.error.length > 0
                    ? true
                    : false
                }
                type={type}
                accessibilityLabel={option.displayName}
              />
            ))}
          </View>
        </FormElementWrapper>
      );
    } else {
      // Multi-Column Rendering: Aufteilung in Zeilen
      const options = props.possibleValues || [];
      const rows: any[][] = [];
      for (let i = 0; i < options.length; i += columnCount) {
        rows.push(options.slice(i, i + columnCount));
      }

      return (
        <FormElementWrapper
          formState={formState}
          uiState={uiState}
          numberOfColumns={props.formGridOptions?.numberOfColumns}
          description={props.description}
          formComponentAttributes={props}
          labelId={'label-' + props.jsonpath}
          handleDeleteEntry={() =>
            handleSelectionChange(type === 'checkbox' ? [] : null)
          }
          showDeleteContentBtn={false}
          needFloatingLabel={false}
          hideLabel={true}
          hideLoading={true}
          hideAnFieldLoadErrorIcon={true}
          localErrorMsg={props.error ? props.error : ""}
          jsonPath={props.jsonpath}
          objectId={props.id}
        >
          <View
            ref={elementRef}
            style={booleanStyles.radioButtonGroupContainer}
          >
            {/* Wir verstecken das Label, wenn ein Formgrid-Element von diesem Element abhängt */}
            {(!props.formState.formgridDependsOn ||
              props.formState.formgridDependsOn.indexOf(props.id) === -1) && (
              <AndpText
                style={[
                  booleanStyles.radioButtonGroupLabel,
                  props.error &&
                    props.editable &&
                    booleanStyles.radioButtonGroupLabelError,
                ]}
                accessibilityLabel={t(
                  'form_accessibility_dropdownCatalog_radio_label',
                  { label: labelText },
                )}
              >
                {labelText}
              </AndpText>
            )}
            <View style={booleanStyles.radioButtonGroup}>
              {rows.map((row, rowIndex) => (
                <View
                  key={`row_${rowIndex}`}
                  style={booleanStyles.radioButtonRow}
                >
                  {row.map((option, colIndex) => {
                    return (
                      <View
                        key={option.identifier || colIndex}
                        style={{ width: `${100 / columnCount}%` }}
                      >
                        <AndpRadioButton
                          id={`${props.jsonpath}-${type}-${rowIndex}-${colIndex}`}
                          selected={isOptionSelected(
                            option,
                            currentSelectedValues,
                          )}
                          label={option.displayName}
                          onPress={() => handleChangeRadio(option)}
                          disabled={!props.editable}
                          error={!!props.error}
                          type={type}
                          accessibilityLabel={option.displayName}
                        />
                      </View>
                    );
                  })}
                </View>
              ))}
            </View>
          </View>
        </FormElementWrapper>
      );
    }
  }

  // Nicht-Radio-Variante: Unverändert – hier wird BaseDropDownInputElement verwendet
  const [inputValue, setInputValue] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const selectedValues: any[] = props.isList
    ? Array.isArray(props.value)
      ? props.value
      : props.value
      ? [props.value]
      : []
    : props.value
    ? [props.value]
    : [];

  const handleFocus = () => {
    setDropdownOpen(true);
  };

  const handleBlur = () => {
    setTimeout(() => {
      setInputValue('');
      setDropdownOpen(false);
    }, 0);
  };

  const clearInput = () => {
    setInputValue('');
    handleSelectionChange(props.isList ? [] : null);
  };

  const onSelectOption = (option: any) => {
    if (props.isList) {
      const exists = selectedValues.find(
        (v) => v.identifier === option.identifier,
      );
      const newVal = exists
        ? selectedValues.filter((v) => v.identifier !== option.identifier)
        : [...selectedValues, option];
      handleSelectionChange(newVal);
      setInputValue('');
    } else {
      handleSelectionChange(option);
      setInputValue('');
      setDropdownOpen(false);
    }
  };

  const filteredOptions = useMemo(() => {
    const searchFiltered = (props.possibleValues || []).filter((option) =>
      option.displayName.toLowerCase().includes(inputValue.toLowerCase()),
    );
    if (props.isList) {
      return searchFiltered.filter(
        (option) =>
          !selectedValues.some((v) => v.identifier === option.identifier),
      );
    }

    return searchFiltered;
  }, [inputValue, props.possibleValues, props.isList, selectedValues]);

  const placeholderText =
    !props.isList && utils.isFilled(props.value)
      ? props.value.displayName
      : props.isList && selectedValues.length > 0
      ? ''
      : utils.getI18nText(formState, props.label) +
        (props.required ? ' *' : '');

  const labelText =
    utils.getI18nText(formState, props.label) + (props.required ? ' *' : '');

  const dropdownData = filteredOptions.length > 0 ? filteredOptions : undefined;

  const renderOptionCatalog = ({ item }: { item: any }) => (
    <TouchableOpacity
      id={`${props.jsonpath}-${item.identifier}`}
      onPress={() => onSelectOption(item)}
      accessibilityRole="button"
      accessibilityLabel={t('form_accessibility_dropdownCatalog_listItem', {
        option: item.displayName,
      })}
    >
      <AndpText
        style={[
          styles.dropdownListItemText,
          props.error && styles.dropdownListItemTextError,
        ]}
      >
        {item.displayName}
      </AndpText>
      {selectedValues.some((v) => v.identifier === item.identifier) && (
        <AndpIcon
          name="checkMark"
          size={dropdownInputTheme.dropdownList.checkMarkIconSize}
          color={
            props.error
              ? dropdownInputTheme.dropdownList.checkMarkIconColorError
              : dropdownInputTheme.dropdownList.checkMarkIconColor
          }
        />
      )}
    </TouchableOpacity>
  );

  return (
    <FormElementWrapper
      formState={formState}
      uiState={uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      description={props.description}
      formComponentAttributes={props}
      labelId={`label-${props.jsonpath}`}
      showDeleteContentBtn={false}
      needFloatingLabel={false}
      hideLabel={true}
      hideLoading={true}
      hideAnFieldLoadErrorIcon={true}
      localErrorMsg={props.error ? props.error : ''}
      jsonPath={props.jsonpath}
      objectId={props.id}
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
          ANFieldLoadError={props.ANFieldLoadError}
          error={props.error}
          value={props.value}
          onRemoveChip={(chip: any) => {
            const newValues = selectedValues.filter(
              (v) => v.identifier !== chip.identifier,
            );
            handleSelectionChange(newValues);
          }}
          dropdownData={dropdownData}
          dropdownRenderItem={dropdownData ? renderOptionCatalog : undefined}
          dropdownOnSelect={onSelectOption}
          disabled={!props.editable}
        >
          {filteredOptions.length === 0 && (
            <AndpText
              style={[
                styles.activityText,
                props.error && styles.activityTextError,
              ]}
            >
              {t('noResults')}
            </AndpText>
          )}
        </BaseDropDownInputElement>
      </View>
    </FormElementWrapper>
  );
}

DropDownCatalogElement.validate = function (
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

DropDownCatalogElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  const value = utils.getValue(state, jsonpath, id);
  if (!value) return '';
  if (Array.isArray(value)) {
    return value.map((v: any) => v.displayName).join(', ');
  }
  return value.displayName || '';
};

export default DropDownCatalogElement;
