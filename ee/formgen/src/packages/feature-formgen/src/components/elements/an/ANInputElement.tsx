import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, TextInput, TouchableOpacity, Platform } from 'react-native';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  SummaryOptions,
  IOverlayInfoExtraProps,
} from '../../../types/interfaces';
import { inputElementStyles } from '../../../styles/common/common-style';
import { inputElementStyles as customInputElementStyles } from '../../../styles/custom/custom-style';
import { SummaryElement } from '../helper/SummaryElement';
import * as utils from '../../../utils/Utils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import * as validationUtils from '../../../utils/ValidationUtils';
import { StatesToUpdateEnum } from '../../../constants/enums/StateToUpdateEnum';
import * as overlayUtils from '../../../utils/OverlayUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { inputElementTheme } from '@andp/core/src/styles/theme';
import { RestService } from '../../../services/RestService';
import { useFormMirrorStore } from '../../../state/useFormMirrorState';
import SharedInputComponent, {
  sanitizeLiveNumberInput,
  sanitizeNumberInput,
  useTextareaHeight,
} from '../shared/components/InputElementComponent';

function ANInputElement(props: IFormComponentAttributes) {
  const { formState, uiState, t, restService } = props;
  const isInputAllowed =
    props.type === 'string' ||
    props.type === 'number' ||
    props.type === 'property';

  const [localValue, setLocalValue] = useState(props.value);
  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  const [isFocused, setIsFocused] = useState(false);
  const [lastHandleChangeValue, setLastHandleChangeValue] = useState(props.value);
  const [isFilled, setIsFilled] = useState(!!props.value);
  let labeltext =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : '');

  const schema = utils.getSchemaForElement(props.formState.schema, props.id);
  const isNumber = schema.type === 'number';
  const elementRef = useRef<TextInput>(null);
  const styles = useMemo(() => mergeStyles(inputElementStyles, customInputElementStyles), []);
  const lang = restService?.getFullCulture?.() || "de-AT";
  const setMirrorFormState = useFormMirrorStore((state) => state.setFormState);
  const setMirrorUIState = useFormMirrorStore((state) => state.setUIState);

  // if step is undefined it will not create an attribute on the input element thus allowing all number inputs.
  //    an undefined step value defaults to 0, which is an invalid step value thus allowing all number inputs
  // if precision is 0 step and formGridOptions.numberStep is less than 1, step will be 1
  // if precision is not 0 step and formGridOptions.numberStep is less than 1, step will be formGridOptions.numberStep
  // if precision is not 0 step and formGridOptions.numberStep is equal or greater than 1, step will be formGridOptions.numberStep
  const precision = schema.precision;
  let rawStep = props.formGridOptions?.numberStep;
  let step =
    rawStep == null
      ? undefined
      : typeof rawStep === 'string'
        ? parseFloat(rawStep)
        : rawStep;

  if (step && step < 1 && (precision === 0 || !precision)) {
    step = 1;
  }

  const initialFormatted =
    schema.type === 'number'
      ? getPrecisionAppliedValue(String(props.value ?? ''), lang, precision)
      : props.value;

  const [formattedValue, setFormattedValue] = useState(initialFormatted);

  const numberOfLines =
    props.formGridOptions?.numberOfLines != null
      ? Number(props.formGridOptions.numberOfLines)
      : 1;
  const { height, updateHeight } = useTextareaHeight(numberOfLines);

  useEffect(() => {
    // There was a problem after updateOnChange on other elements, which doesnt updated the value of the disabled string inputElement.
    if (schema.type != 'number' && props.type != 'number') {
      setLocalValue(props.value);
    } else if (
      props.value !== localValue &&
      props.value !== formattedValue
    ) {
      setLocalValue(props.value);
      const formVal = getPrecisionAppliedValue(String(props.value), lang, precision);
      setFormattedValue(formVal);
      if (props.value === '') {
        setIsFocused(false);
      } else {
        setIsFilled(true);
      }
    }
  }, [props.value]);

  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const triggerHandleChange = (valueToUpdate: any, newFormatted?: boolean) => {
    if (valueToUpdate != '') {
      setIsFilled(true);
    } else {
      setIsFilled(false);
    }

    let correctedValue;
    if (schema.type === 'number') {
      // Apply precision for display/typing, but keep the value in state/mirror sanitized
      // (same normalization as in handleBlur).
      const precisionApplied = getPrecisionAppliedValue(
        String(valueToUpdate ?? ''),
        lang,
        precision,
        false,
      );
      correctedValue = sanitizeNumberInput(String(precisionApplied), lang);
    } else {
      correctedValue = valueToUpdate;
    }

    setLocalValue(correctedValue);
    if (!newFormatted) {
      setFormattedValue(valueToUpdate);
    }

    const localValidation = validationUtils.validateField(
      formState,
      props.jsonpath,
      props.id,
      true,
      props.formGridOptions,
      correctedValue,
    );

    const elementState = utils.getElementByJsonPath(formState.data, props.jsonpath);

    if (elementState != null) {
      if (Array.isArray(correctedValue)) {
        elementState.values = correctedValue;
      } else {
        elementState.value = correctedValue;
      }
      elementState.validationMessage = localValidation.errorMessage;
      utils.setStateElementByJsonpath(formState.data, props.jsonpath, elementState);
      // Keep the existing behaviour: only trigger React re-render / AN update
      // when the validation message actually changed.
      if (localValidation.errorMessage !== localErrMsg) {
        props.updateState(StatesToUpdateEnum.ALL);
      }
    }

    // NEW: always push the latest form/ui into the global mirror
    // so the debug diff logs every onChange, not just onBlur.
    setMirrorFormState(formState);
    setMirrorUIState(uiState);
  };

  const handleFocus = () => {
    setIsFocused(true);
  };

  // Increment- und Decrement-Handler
  const handleIncrement = () => {
    if (step) adjustNumericValue(step);
  };

  const handleDecrement = () => {
    if (step) adjustNumericValue(-step);
  };

  const adjustNumericValue = (delta: number) => {
    if (!isFocused) handleFocus();
    const normalized = String(localValue ?? '')
      .trim()
      .replace(/'/g, '')
      .replace(/\s+/g, '')
      .replace(',', '.');
    const current = parseFloat(normalized) || 0;
    const newNumericValue = current + delta;
    const prec = schema.precision ?? 0;
    const fixed = newNumericValue.toFixed(prec);
    const newVal = getPrecisionAppliedValue(String(newNumericValue), lang, prec);
    setFormattedValue(newVal);
    triggerHandleChange(fixed, true);
  };

  const preventNextBlurRef = useRef(false);

  const handleBlur = async (e?: any) => {
    if (preventNextBlurRef.current) {
      preventNextBlurRef.current = false;
      if (Platform.OS === 'web') {
        requestAnimationFrame(() => elementRef.current?.focus?.());
      } else {
        elementRef.current?.focus?.();
      }
      return; // KEINE Animation, KEIN State-Update -> Fokus bleibt wenn step btn geklickt wird
    }

    // On web: if blur happened because focus moved into the sticky footer,
    // do not validate on blur – only persist the current value.
    const relatedTarget = e?.nativeEvent?.relatedTarget;
    const isStickyFooterTarget =
      Platform.OS === 'web' &&
      !!(
        relatedTarget &&
        (relatedTarget.id?.startsWith('sticky-bar-') ||
          relatedTarget.closest?.('[id^="sticky-bar-"]'))
      );
    const shouldValidate = !isStickyFooterTarget;

    setIsFocused(false);
    let valueToUpdate = isNumber ? formattedValue : localValue;

    const overlayInfoOptions = overlayUtils.getOverlayInfoOptions(
      props.formGridOptions?.overlayOptions,
      valueToUpdate,
    );
    const shouldOverlay =
      props.formGridOptions?.overlayOptions?.isShowOverlay &&
      overlayInfoOptions.isShowOverlay;

    if (shouldOverlay && lastHandleChangeValue !== '' && lastHandleChangeValue !== valueToUpdate) {
      const handleCancelOverlayInfo = () => {
        formEventsUtils.handleChange(
          formState, uiState, props.restService, props.jsonpath, props.id,
          true, props.updateState, lastHandleChangeValue, props.formGridOptions, props.editable,
        );
        setLastHandleChangeValue(lastHandleChangeValue);
      };
      overlayUtils.setOverlayInfo(
        uiState,
        { overlayInfoOptions, handleCancel: handleCancelOverlayInfo } as IOverlayInfoExtraProps,
        props.updateState,
      );
    }

    if (isNumber && valueToUpdate && valueToUpdate !== '') {
      // Number precision and step logic
      let v = valueToUpdate.replace(/(?!^)-|[^0-9.,-]/g, '');
      if (precision === 0) {
        v = v.replace(/[.,].*$/, '');
      }
      v = getPrecisionAppliedValue(v, lang, precision);
      // Wenn step definiert ist und precision 0 ist, runde auf das nächste Vielfache von step
      if (!isNaN(v) && step && step > 0 && precision === 0) {
        v = String(Math.round(v / step) * step);
      }
      setFormattedValue(v);
      const cleanedForValidation = sanitizeNumberInput(v, lang);
      valueToUpdate = cleanedForValidation;
      setLocalValue(cleanedForValidation);
    }

    if (shouldValidate) {
      formEventsUtils.handleChange(
        formState, uiState, props.restService, props.jsonpath, props.id,
        true, props.updateState, valueToUpdate, props.formGridOptions, props.editable,
      );
    }
    setLastHandleChangeValue(valueToUpdate);
  };

  if (props.summary > 0) {
    return renderSummary(props);
  }

  const needSpace = typeof step === 'number' && step > 0;
  const alignment = isNumber && props.formGridOptions?.alignment;
  const textAlign: 'left' | 'center' | 'right' =
    alignment === 'center' || alignment === 'right' ? alignment : 'left';
  const isTextarea =
    !!(props.formGridOptions?.numberOfLines && props.formGridOptions?.numberOfLines > 1 && !isNumber);

  const unitText = schema.unitText && utils.getI18nText(props.formState, schema.unitText)

  return (
    <SharedInputComponent
      formState={formState}
      uiState={uiState}
      formComponentAttributes={props}
      localErrorMsg={localErrMsg}
      showFilledIcon={isFilled}
      isInputAllowed={isInputAllowed}
      value={isNumber ? formattedValue : localValue}
      editable={props.editable}
      isNumber={isNumber}
      isTextarea={isTextarea}
      textAlign={textAlign}
      numberOfLines={numberOfLines}
      height={height}
      isFocused={isFocused}
      labeltext={labeltext}
      onChangeText={triggerHandleChange}
      onNumberChangeText={(text) => {
        const sanitized = sanitizeLiveNumberInput(text);
        // Nicht mehr als 1 , oder . zulassen
        const sepCount = (sanitized.match(/[.,]/g) || []).length;
        if (sepCount <= 1) {
          triggerHandleChange(sanitized);
        }
      }}
      onFocus={handleFocus}
      onBlur={handleBlur}
      onContentSizeChange={(event) => updateHeight(event.nativeEvent.contentSize.height)}
      onNumberKeyPress={({ nativeEvent }) => {
        if (nativeEvent.key === 'ArrowUp' && step && step > 0) {
          handleIncrement();
        } else if (nativeEvent.key === 'ArrowDown' && step && step > 0) {
          handleDecrement();
        }
      }}
      renderNumberAddon={needSpace ? () => (
        <View
          onStartShouldSetResponderCapture={() => true}
          onResponderGrant={(e) => {
            // @ts-ignore
            e.preventDefault?.();
            preventNextBlurRef.current = true;
            elementRef.current?.focus?.();
          }}
          style={styles.numberBtns}
        >
          <TouchableOpacity
            onPress={handleIncrement}
            style={styles.numberBtnUp}
            accessibilityRole="button"
            accessibilityLabel={t('form_accessibility_textInput_number_increment_label', { label: labeltext })}
            accessibilityHint={t('form_accessibility_textInput_number_increment_hint', { label: labeltext })}
          >
            <AndpIcon
              name={'arrowUp'}
              size={inputElementTheme.numberButtons.iconSize}
              color={inputElementTheme.numberButtons.iconColor}
            />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={handleDecrement}
            style={styles.numberBtnDown}
            accessibilityRole="button"
            accessibilityLabel={t('form_accessibility_textInput_number_decrement_label', { label: labeltext })}
            accessibilityHint={t('form_accessibility_textInput_number_decrement_hint', { label: labeltext })}
          >
            <AndpIcon
              name={'arrowDown'}
              size={inputElementTheme.numberButtons.iconSize}
              color={inputElementTheme.numberButtons.iconColor}
            />
          </TouchableOpacity>
        </View>
      ) : undefined}
      needSpace={needSpace}
      elementRef={elementRef}
      styles={styles}
      t={t}
      unitText={unitText}
      unitPosition={schema.unitPosition}
    />
  );
}

ANInputElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: any,
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

/**
 * Locale‐aware Formatter für numerische Eingabetexte.
 */
function getPrecisionAppliedValue(
  value: string,
  lang?: string,
  precision?: number,
  useGrouping?: boolean,
) {
  if (value === '') return value;
  const locale = lang;
  let normalized = value.replace(/'/g, '');
  normalized = normalized.replace(/,/g, '.');

  const numberValue = Number(normalized);
  if (Number.isNaN(numberValue)) return value;

  const opts = (precision == null)
    ? { minimumFractionDigits: 0, maximumFractionDigits: 20 }
    : { minimumFractionDigits: precision, maximumFractionDigits: precision };

  return new Intl.NumberFormat(locale, { ...opts, useGrouping: useGrouping ?? true }).format(numberValue);
}

ANInputElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  restService: RestService,
) {
  let ret = utils.getValue(state, jsonpath, id);
  const elementSchema = utils.getSchemaForElement(state.schema, id);
  const lang = restService?.getFullCulture?.() || "de-AT";
  if (elementSchema.type === 'number') {
    ret = getPrecisionAppliedValue(ret, lang, elementSchema.precision);
  }
  return ret;
};

function renderSummary(props: IFormComponentAttributes) {
  return (
    <SummaryElement
      t={props.t}
      formState={props.formState}
      uiState={props.uiState}
      summaryOption={SummaryOptions.SummaryText}
      label={props.label}
      value={props.value}
      initUrl={props.restService.initUrl}
    />
  );
}

export default ANInputElement;
