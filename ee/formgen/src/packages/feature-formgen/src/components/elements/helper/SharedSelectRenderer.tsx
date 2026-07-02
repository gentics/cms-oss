import React, { useMemo, useRef, useState } from 'react';
import { View, TouchableOpacity, Dimensions, TextInput } from 'react-native';
import { FormElementWrapper } from './FormElementWrapper';
import * as utils from '../../../utils/Utils';
import * as overlayUtils from '../../../utils/OverlayUtils';
import { OverlayTypeEnum } from '../../../custom/OverlayTypes';
import { IOverlayCustomOptionExtraProps } from '../../../types/interfaces';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { dropdownInputTheme } from '@andp/core/src/styles/theme';
import AndpRadioButton from '@andp/core/src/components/AndpRadioButton';
import BaseDropDownInputElement from '../an/DropDownElements/BaseDropDownInputElement';
import SharedInputComponent from '../shared/components/InputElementComponent';
import { inputElementStyles } from '../../../styles/common/common-style';
import { inputElementStyles as customInputElementStyles } from '../../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';

/**
 * Sentinel identifier for the synthetic "additional element" option that is shown
 * in the dropdown when `customOption` is enabled. Selecting it opens the input
 * overlay instead of persisting it as a value.
 */
const CUSTOM_OPTION_IDENTIFIER = '__formgen_custom_option__';

/**
 * Returns whether the given option is already part of the current selection.
 */
function isOptionSelected(option: any, selectedValues: any[]): boolean {
  return selectedValues.some((val) => val.identifier === option.identifier);
}

function getMockRadioOptions() {
  return Array.from({ length: 5 }, (_, index) => ({
    identifier: `mock-${index + 1}`,
    displayName: `Wert${index + 1}`,
  }));
}

/**
 * Props for the shared select renderer.
 *
 * The concrete element component passes its own wrapper props and injects the
 * project-specific change handler through `onSelectionChange(...)`.
 */
type SharedSelectRendererProps = {
  props: any;
  elementRef: any;
  styles: any;
  booleanStyles: any;
  onSelectionChange: (value: any) => void;
};

/**
 * Shared renderer for select-like form elements.
 *
 * This component contains the common UI for both wrapper components:
 * - radio / checkbox rendering when `formGridOptions.radio` is enabled
 * - searchable dropdown rendering for single- and multi-select use cases
 *
 * It is intentionally presentation-focused. Value persistence and side effects
 * stay in the outer wrapper via the injected `onSelectionChange(...)` callback.
 */
function SharedSelectRenderer({
  props,
  elementRef,
  styles,
  booleanStyles,
  onSelectionChange,
}: SharedSelectRendererProps) {
  const { formState, uiState, t } = props;

  // Catalog flag: allow the user to enter an additional free-text value.
  // Delivered by the backend on the per-element data object (same source as
  // `possibleValues`) and mapped to this prop in ElementRenderer.
  const customOptionEnabled = props.customOption === true;

  // Shared state/styles for the customOption free-text field rendered via
  // SharedInputComponent. Declared unconditionally (before the radio/dropdown
  // branch returns) to keep hook order stable across renders.
  const [customInputFocused, setCustomInputFocused] = useState(false);
  // Tracks that the user activated the custom option even before entering text,
  // so its indicator shows as selected immediately on click (radio semantics).
  const [customSelected, setCustomSelected] = useState(false);
  const customInputRef = useRef<TextInput>(null);
  const customInputStyles = useMemo(
    () => mergeStyles(inputElementStyles, customInputElementStyles),
    [],
  );

  // Render radio or checkbox options when the field is configured for radio mode.
  if (props.formGridOptions?.radio) {
    const radioOptions = props.formState.isFormgridPreview
      ? getMockRadioOptions()
      : props.possibleValues || [];
    const type = props.isList ? 'checkbox' : 'radio';
    const currentSelectedValues: any[] = props.value
      ? Array.isArray(props.value)
        ? [...props.value]
        : [props.value]
      : [];

    // Toggle the clicked option and delegate the resulting value to the wrapper.
    const handleChangeRadio = (selectedOption: any) => {
      if (type === 'radio') {
        // Selecting a predefined radio deactivates the custom option.
        setCustomSelected(false);
        onSelectionChange(selectedOption);
      } else {
        let updatedValues = [...currentSelectedValues];
        if (isOptionSelected(selectedOption, updatedValues)) {
          updatedValues = updatedValues.filter(
            (elem) => elem.identifier !== selectedOption.identifier,
          );
        } else {
          updatedValues.push(selectedOption);
        }
        onSelectionChange(updatedValues);
      }
    };

    // customOption in radio mode: render a plain text input at the end of the list.
    // The custom value is the current selection whose identifier is not one of the
    // predefined options. Editing it writes the entered text as the field value.
    const radioOptionIds = new Set(
      radioOptions.map((o: any) => o.identifier),
    );
    const customRadioEntry = currentSelectedValues.find(
      (v) => !radioOptionIds.has(v.identifier),
    );
    const customRadioText = customRadioEntry?.displayName ?? '';

    const handleCustomRadioChange = (text: string) => {
      if (type === 'radio') {
        onSelectionChange(
          text.trim().length === 0
            ? null
            : { identifier: text, displayName: text },
        );
      } else {
        // checkbox / list: keep predefined selections, replace the single custom entry.
        const withoutCustom = currentSelectedValues.filter((v) =>
          radioOptionIds.has(v.identifier),
        );
        onSelectionChange(
          text.trim().length === 0
            ? withoutCustom
            : [...withoutCustom, { identifier: text, displayName: text }],
        );
      }
    };

    // The indicator is selected as soon as a custom value exists or the user has
    // activated the custom option by clicking it.
    const customIndicatorSelected = !!customRadioEntry || customSelected;

    // Activate the custom option (mark its indicator). In radio mode the custom
    // option is exclusive, so any predefined selection is cleared. Shared by the
    // indicator press and by focusing the text field.
    const activateCustomOption = () => {
      if (!props.editable) return;
      setCustomSelected(true);
      if (type === 'radio' && !customRadioEntry) {
        onSelectionChange(null);
      }
    };

    // Pressing the indicator activates the custom option and focuses the text
    // field. In checkbox mode, pressing an already set entry clears it.
    const handleCustomIndicatorPress = () => {
      if (!props.editable) return;
      if (type === 'checkbox' && customRadioEntry) {
        handleCustomRadioChange('');
        setCustomSelected(false);
        return;
      }
      activateCustomOption();
      customInputRef.current?.focus();
    };

    const customRadioInput = customOptionEnabled ? (
      <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8 }}>
        <AndpRadioButton
          id={`${props.jsonpath}-${type}-customOption`}
          selected={customIndicatorSelected}
          label=""
          onPress={handleCustomIndicatorPress}
          disabled={!props.editable}
          error={!!props.error}
          type={type}
          accessibilityLabel={t('form_catalog_customOption')}
        />
        <View style={{ flex: 1 }}>
        <SharedInputComponent
          formState={formState}
          uiState={uiState}
          // Distinct ids/jsonpath: the internally rendered FormElementWrapper
          // emits a <div id={jsonpath}> on web — without a suffix it would
          // collide with the radio group's own DOM id and preview selection.
          // description/error are suppressed so the group's tooltip and error
          // message are not duplicated here.
          formComponentAttributes={{
            ...props,
            jsonpath: `${props.jsonpath}-customOption`,
            id: `${props.id}-customOption`,
            description: undefined,
            error: undefined,
            required: false,
            formGridOptions: undefined,
          }}
          localErrorMsg=""
          showFilledIcon={false}
          isInputAllowed={true}
          value={customRadioText}
          editable={props.editable}
          isNumber={false}
          isTextarea={false}
          numberOfLines={1}
          isFocused={customInputFocused}
          labeltext={t('form_catalog_customOption')}
          onChangeText={handleCustomRadioChange}
          onNumberChangeText={() => {}}
          onFocus={() => {
            setCustomInputFocused(true);
            activateCustomOption();
          }}
          onBlur={() => setCustomInputFocused(false)}
          elementRef={customInputRef}
          styles={customInputStyles}
          t={t}
        />
        </View>
      </View>
    ) : null;

    // Respect the configured number of radio columns on larger screens only.
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

    // Default layout: render each option in a single vertical column.
    if (columnCount === 1) {
      return (
        <FormElementWrapper
          formState={formState}
          uiState={uiState}
          numberOfColumns={props.formGridOptions?.numberOfColumns}
          description={props.description}
          formComponentAttributes={props}
          labelId={`label-${props.jsonpath}`}
          handleDeleteEntry={() =>
            onSelectionChange(type === 'checkbox' ? [] : null)
          }
          showDeleteContentBtn={false}
          needFloatingLabel={false}
          hideLabel={true}
          hideLoading={true}
          hideAnFieldLoadErrorIcon={true}
          localErrorMsg={props.error ? props.error : ''}
          jsonPath={props.jsonpath}
          objectId={props.id}
          selectedElementId={props.selectedElementId}
        >
          <View ref={elementRef} style={booleanStyles.radioButtonGroupContainer}>
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

            {radioOptions.map((option: any, idx: any) => (
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
            {customRadioInput}
          </View>
        </FormElementWrapper>
      );
    }

    // Multi-column layout: group options into rows based on the resolved column count.
    const options = radioOptions;
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
        labelId={`label-${props.jsonpath}`}
        handleDeleteEntry={() =>
          onSelectionChange(type === 'checkbox' ? [] : null)
        }
        showDeleteContentBtn={false}
        needFloatingLabel={false}
        hideLabel={true}
        hideLoading={true}
        hideAnFieldLoadErrorIcon={true}
        localErrorMsg={props.error ? props.error : ''}
        jsonPath={props.jsonpath}
        objectId={props.id}
        selectedElementId={props.selectedElementId}
      >
        <View ref={elementRef} style={booleanStyles.radioButtonGroupContainer}>
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

          <View style={booleanStyles.radioButtonGroup}>
            {rows.map((row, rowIndex) => (
              <View key={`row_${rowIndex}`} style={booleanStyles.radioButtonRow}>
                {row.map((option, colIndex) => (
                  <View
                    key={option.identifier || colIndex}
                    style={{ width: `${100 / columnCount}%` }}
                  >
                    <AndpRadioButton
                      id={`${props.jsonpath}-${type}-${rowIndex}-${colIndex}`}
                      selected={isOptionSelected(option, currentSelectedValues)}
                      label={option.displayName}
                      onPress={() => handleChangeRadio(option)}
                      disabled={!props.editable}
                      error={!!props.error}
                      type={type}
                      accessibilityLabel={option.displayName}
                    />
                  </View>
                ))}
              </View>
            ))}
          </View>
          {customRadioInput}
        </View>
      </FormElementWrapper>
    );
  }

  // Dropdown mode: keep local UI state for search input and open/closed state.
  const [inputValue, setInputValue] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);

  // Normalize the current value into an array so single- and multi-select can share logic.
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

  // Clear both the search text and the current selection.
  const clearInput = () => {
    setInputValue('');
    onSelectionChange(props.isList ? [] : null);
  };

  // Open the custom-option overlay. The entered value is added as a new option and
  // selected; cancelling leaves the current selection untouched (the special option
  // itself is never persisted as a value).
  const openCustomOptionOverlay = () => {
    setInputValue('');
    setDropdownOpen(false);
    const extraProps: IOverlayCustomOptionExtraProps = {
      onConfirm: (val: string) => {
        const newOption = { identifier: val, displayName: val };
        if (props.isList) {
          const exists = selectedValues.some(
            (v) => v.identifier === newOption.identifier,
          );
          onSelectionChange(exists ? selectedValues : [...selectedValues, newOption]);
        } else {
          onSelectionChange(newOption);
        }
      },
    };
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.CUSTOM_OPTION_INPUT,
      props.updateState,
      extraProps,
    );
  };

  // Apply the clicked dropdown option and reset the search input afterwards.
  const onSelectOption = (option: any) => {
    if (option.identifier === CUSTOM_OPTION_IDENTIFIER) {
      openCustomOptionOverlay();
      return;
    }
    if (props.isList) {
      const exists = selectedValues.find(
        (v) => v.identifier === option.identifier,
      );
      const newVal = exists
        ? selectedValues.filter((v) => v.identifier !== option.identifier)
        : [...selectedValues, option];

      onSelectionChange(newVal);
      setInputValue('');
    } else {
      onSelectionChange(option);
      setInputValue('');
      setDropdownOpen(false);
    }
  };

  // Filter options by the current search text and hide already selected entries in list mode.
  const filteredOptions = useMemo(() => {
    const searchFiltered = (props.possibleValues || []).filter((option: any) =>
      option.displayName.toLowerCase().includes(inputValue.toLowerCase()),
    );

    if (props.isList) {
      return searchFiltered.filter(
        (option: any) =>
          !selectedValues.some((v) => v.identifier === option.identifier),
      );
    }

    return searchFiltered;
  }, [inputValue, props.possibleValues, props.isList, selectedValues]);

  // Show the current single value as placeholder, otherwise fall back to the label.
  const placeholderText =
    !props.isList && utils.isFilled(props.value)
      ? props.value.displayName
      : props.isList && selectedValues.length > 0
        ? ''
        : utils.getI18nText(formState, props.label) +
        (props.required ? ' *' : '');

  const labelText =
    utils.getI18nText(formState, props.label) + (props.required ? ' *' : '');

  // When customOption is enabled, append the synthetic "additional element" entry
  // at the end of the list so it is always selectable regardless of the search text.
  const displayOptions = customOptionEnabled
    ? [
        ...filteredOptions,
        {
          identifier: CUSTOM_OPTION_IDENTIFIER,
          displayName: t('form_catalog_customOption'),
        },
      ]
    : filteredOptions;

  const dropdownData = displayOptions.length > 0 ? displayOptions : undefined;

  // Render a single dropdown result entry, including the selected-state checkmark.
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

  // Render the shared searchable dropdown variant inside the standard form wrapper.
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
          ANFieldLoadError={props.ANFieldLoadError}
          error={props.error}
          value={props.value}
          onRemoveChip={(chip: any) => {
            const newValues = selectedValues.filter(
              (v) => v.identifier !== chip.identifier,
            );
            onSelectionChange(newValues);
          }}
          dropdownData={dropdownData}
          dropdownRenderItem={dropdownData ? renderOptionCatalog : undefined}
          dropdownOnSelect={onSelectOption}
          disabled={!props.editable}
        >
          {displayOptions.length === 0 && (
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

export default SharedSelectRenderer;