import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, TextInput, TouchableOpacity, Platform } from 'react-native';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  SummaryOptions,
  IOverlayInfoExtraProps,
} from '../../types/interfaces';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
} from 'react-native-reanimated';
import { inputElementStyles } from '../../styles/common/common-style';
import { inputElementStyles as customInputElementStyles } from '../../styles/custom/custom-style';
import { TFunction } from 'i18next';
import { SummaryElement } from './helper/SummaryElement';
import { FormElementWrapper } from './helper/FormElementWrapper';
import * as utils from '../../utils/Utils';
import * as formEventsUtils from '../../utils/FormEventsUtils';
import * as validationUtils from '../../utils/ValidationUtils';
import { StatesToUpdateEnum } from '../../constants/enums/StateToUpdateEnum';
import * as overlayUtils from '../../utils/OverlayUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { inputElementTheme } from '@andp/core/src/styles/theme';
import { RestService } from '../../services/RestService';
import { useFormMirrorStore } from '../../state/useFormMirrorState';

function InputElement(props: IFormComponentAttributes) {
  const { formState, uiState, t, restService } = props;
  const isInputAllowed =
    props.type === 'string' ||
    props.type === 'number' ||
    props.type === 'property';

  // Für Nicht-Nummernfelder: localValue, für Nummernfelder: formattedValue
  const [localValue, setLocalValue] = useState(props.value);
  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  const [isFocused, setIsFocused] = useState(false);
  const [lastHandleChangeValue, setLastHandleChangeValue] = useState(
    props.value,
  );
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

  useEffect(() => {
    // There was a problem after updateOnChange on other elements, which doesnt updated the value of the disabled string inputElement.
    if (schema.type != 'number' && props.type != 'number') {
      setLocalValue(props.value);
      if (props.value && props.value.length > 0) {
        handleAnimationForFocus();
      } else {
        if (!isFocused) {
          handleAnimationForBlur();
        }
      }
    } else if (
      props.value !== localValue &&
      props.value !== formattedValue
    ) {
      setLocalValue(props.value);

      const formVal = getPrecisionAppliedValue(
        String(props.value),
        lang,
        precision,
      );
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
    correctedValue = sanitizeNumberInput(String(precisionApplied));
  } else {
    correctedValue = valueToUpdate;
  }

  setLocalValue(correctedValue);
  if(!newFormatted){
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
  
  const elementState = utils.getElementByJsonPath(
    formState.data,
    props.jsonpath,
  );
  
  if (elementState != null) {
    if (Array.isArray(correctedValue)) {
      elementState.values = correctedValue;
    } else {
      elementState.value = correctedValue;
    }
    elementState.validationMessage = localValidation.errorMessage;

    utils.setStateElementByJsonpath(
      formState.data,
      props.jsonpath,
      elementState,
    );
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

  // Mobile
  const stiffness = 1800;
  const damping = 150;
  

  // Initialisiere die Placeholder-Animation & Animations Handler (Reanimated v2)
  const placeholderTop = useSharedValue(
    props.value ? styles.placeholderFocused.top : styles.placeholder.top,
  );
  const placeholderFontSize = useSharedValue(
    props.value
      ? styles.placeholderTextFocused.fontSize
      : styles.placeholderText.fontSize,
  );
  const placeholderAnimatedStyle = useAnimatedStyle(() => ({
    top: placeholderTop.value,
  }));
  const placeholderTextAnimatedStyle = useAnimatedStyle(() => ({
    fontSize: placeholderFontSize.value,
  }));

  const handleAnimationForBlur = () => {
    placeholderTop.value = withSpring(styles.placeholder.top, {
      stiffness: stiffness,
      damping: damping,
    });
    placeholderFontSize.value = withSpring(styles.placeholderText.fontSize, {
      stiffness: stiffness,
      damping: damping,
    });
  };

  const handleAnimationForFocus = () => {
    placeholderTop.value = withSpring(styles.placeholderFocused.top, {
      stiffness: stiffness,
      damping: damping,
    });
    placeholderFontSize.value = withSpring(
      styles.placeholderTextFocused.fontSize,
      { stiffness: stiffness, damping: damping },
    );
  };
  // -----

  const handleFocus = () => {
    setIsFocused(true);
    handleAnimationForFocus();
  };

  // Increment- und Decrement-Handler
  const handleIncrement = () => {
    const inc = step;
    if (inc) {
      adjustNumericValue(inc);
    }
  };

  const handleDecrement = () => {
    const dec = step;
    if (dec) {
      adjustNumericValue(-dec);
    }
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
    const precision = schema.precision ?? 0;
    const fixed = newNumericValue.toFixed(precision);
    const newVal = getPrecisionAppliedValue(String(newNumericValue), lang, precision);
    setFormattedValue(newVal);
    triggerHandleChange(fixed, true);
  };

  const preventNextBlurRef = useRef(false);

  function sanitizeLiveNumberInput(raw: string) {
    if (!raw) return '';
    return raw.normalize('NFKC').replace(/[A-Za-z]/g, '');
  }

  // Ermittelt Dezimal / Gruppierungszeichen
  function getLocaleSeparators(locale: string) {
    const parts = new Intl.NumberFormat(locale).formatToParts(1000.1);
    const groupPart = parts.find(p => p.type === 'group');
    const decimalPart = parts.find(p => p.type === 'decimal');
    const groupingSep = groupPart ? groupPart.value : null;
    const decimalSep = decimalPart ? decimalPart.value : '.';
    return { groupingSep, decimalSep } as { groupingSep: string | null; decimalSep: string };
  }

  function sanitizeNumberInput(raw: string) {
    const locale = lang;
    const { groupingSep, decimalSep } = getLocaleSeparators(locale);
    const cleaned = raw
      .normalize('NFKC')
      .replace(/['`\u2018\u2019\u201B\u2032\u2035\u02BC\u00B4]/g, '') // Entf alle Apostroph-Varianten
      .replace(/[\s\u00A0\u202F\u2007\u2009\u200A]+/g, '') // Entf alle Whitespaces
      .replace(groupingSep ? new RegExp(`\\${groupingSep}`, 'g') : /\u0000/g, '') // entf alle Gruppierungszeichen
      .replace(decimalSep === '.' ? /\u0000/ : new RegExp(`${decimalSep}`, 'g'), '.') // Dezimaltrennzeichen -> Punkt
      .trim();

    return cleaned;
  }

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
    // Our RN-web sticky footer buttons are divs with ids like:
    //  - sticky-bar-cancel-btn
    //  - sticky-bar-next-page-btn
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

    if (
      shouldOverlay &&
      lastHandleChangeValue !== '' &&
      lastHandleChangeValue !== valueToUpdate
    ) {
      // Abbruch → alten Wert zurücksetzen
      const handleCancelOverlayInfo = () => {
        formEventsUtils.handleChange(
          formState,
          uiState,
          props.restService,
          props.jsonpath,
          props.id,
          true,
          props.updateState,
          lastHandleChangeValue,
          props.formGridOptions,
          props.editable
        );

        setLastHandleChangeValue(lastHandleChangeValue);
      };

      // Overlay anzeigen
      overlayUtils.setOverlayInfo(
        uiState,
        {
          overlayInfoOptions: overlayInfoOptions,
          handleCancel: handleCancelOverlayInfo,
        } as IOverlayInfoExtraProps,
        props.updateState,
      );
    }

    if (!valueToUpdate || valueToUpdate === '') {
      handleAnimationForBlur();
    } else if (isNumber) {
      // Number precision and step logic
      let v = valueToUpdate.replace(/(?!^)-|[^0-9.,-]/g, '');
      if (precision === 0) {
        // entfernt ab dem ersten Punkt ODER Komma alles
        v = v.replace(/[.,].*$/, '');
      }

      v = getPrecisionAppliedValue(v, lang, precision);
      //Wenn step definiert ist und precision 0 ist, runde auf das nächste Vielfache von step
      if (!isNaN(v) && step && step > 0 && precision === 0) {
        v = String(Math.round(v / step) * step);
      }
      setFormattedValue(v);

      const cleanedForValidation = sanitizeNumberInput(v);
      valueToUpdate = cleanedForValidation;
      setLocalValue(cleanedForValidation);

      if (!valueToUpdate || valueToUpdate === '') {
        handleAnimationForBlur();
      }
    }

    if(shouldValidate) {
      formEventsUtils.handleChange(
        formState,
        uiState,
        props.restService,
        props.jsonpath,
        props.id,
        true,
        props.updateState,
        valueToUpdate,
        props.formGridOptions,
        props.editable
      );
    }
    setLastHandleChangeValue(valueToUpdate);
  };

  if (props.summary > 0) {
    return renderSummary(props);
  }

  // Berechne manuell die Höhe des mehrzeiligen Textfelds
  const [height, setHeight] = useState(100);
  const debounceTimeout = useRef<NodeJS.Timeout | null>(null);
  // Normalize numberOfLines to a number so Android doesn’t receive a string
  const numberOfLines =
    props.formGridOptions?.numberOfLines != null
      ? Number(props.formGridOptions.numberOfLines)
      : 1;
  const updateHeight = (newHeight: number) => {
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }
    const lineHeight = 20;
    const minHeight =
      numberOfLines * lineHeight +
      inputElementTheme.paddingBottom +
      inputElementTheme.paddingTop;

    debounceTimeout.current = setTimeout(() => {
      setHeight(Math.max(minHeight, newHeight));
    }, 10);
  };

  const needSpace = typeof step === 'number' && step > 0;
  const alignment = isNumber && props.formGridOptions?.alignment;
  const textAlign: 'left' | 'center' | 'right' =
    alignment === 'center' || alignment === 'right' ? alignment : 'left';

  // Textfeld für mehrzeiligen Text
  if (
    props.formGridOptions?.numberOfLines &&
    props.formGridOptions?.numberOfLines > 1 &&
    !isNumber
  ) {
    return (
      <FormElementWrapper
        formState={formState}
        formComponentAttributes={props}
        uiState={uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        hideLabel={true}
        localErrorMsg={localErrMsg}
        showDeleteContentBtn={false}
        description={props.description}
        hideLoading={true}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View
          style={[
            Platform.OS === 'web' &&
              !props.editable &&
              ({ cursor: 'not-allowed' } as any),
          ]}
        >
          {isInputAllowed && (
            <View pointerEvents={!props.editable ? 'none' : 'auto'}>
              <Animated.View
                pointerEvents="none"
                style={[
                  styles.placeholder,
                  placeholderAnimatedStyle,
                  props.error && props.editable && styles.placeholderError,
                ]}
              >
                <Animated.Text
                  style={[
                    styles.placeholderText,
                    placeholderTextAnimatedStyle,
                    props.error && props.editable && styles.placeholderError,
                  ]}
                  numberOfLines={1}
                  ellipsizeMode={'tail'}
                >
                  {labeltext}
                </Animated.Text>
              </Animated.View>
              <TextInput
                ref={elementRef}
                editable={props.editable}
                value={localValue}
                onChangeText={triggerHandleChange}
                onFocus={handleFocus}
                onBlur={(e) => handleBlur(e)}
                multiline={true}
                numberOfLines={numberOfLines}
                onContentSizeChange={(event) => {
                  const newHeight = event.nativeEvent.contentSize.height;
                  updateHeight(newHeight);
                }}
                style={[
                  styles.textarea,
                  {
                    height: height,
                  },
                  props.error && props.editable && styles.inputError,
                  isFocused && props.editable && styles.inputAreaFocused,
                  !props.editable && styles.disabledInput,
                ]}
                accessible={true}
                accessibilityRole="text"
                accessibilityLabel={t(
                  'form_accessibility_textInput_multiline_label',
                  { label: labeltext },
                )}
                accessibilityHint={
                  props.error
                    ? t('form_accessibility_textInput_error_hint', {
                        error: localErrMsg,
                      })
                    : t('form_accessibility_textInput_multiline_hint', {
                        label: labeltext,
                      })
                }
                accessibilityState={{ disabled: !props.editable }}
                accessibilityValue={{ text: localValue }}
                accessibilityLiveRegion="assertive"
                importantForAccessibility="yes"
                {...(Platform.OS === 'web' && {
                  'aria-invalid': !!localErrMsg,
                  'aria-haspopup': 'false',
                  'aria-expanded': isFocused,
                })}
              />
            </View>
          )}
        </View>
      </FormElementWrapper>
    );
  }
  // Numerisches Feld
  else if (isNumber) {
    return (
      <FormElementWrapper
        formState={formState}
        formComponentAttributes={props}
        uiState={uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        hideLabel={true}
        localErrorMsg={localErrMsg}
        showFilledIcon={isFilled}
        description={props.description}
        showDeleteContentBtn={false}
        needSpace={needSpace}
        hideLoading={true}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View
          style={
            Platform.OS === 'web' &&
            !props.editable &&
            ({ cursor: 'not-allowed' } as any)
          }
        >
          {isInputAllowed && (
            <>
              <Animated.View
                pointerEvents="none"
                style={[
                  styles.placeholder,
                  placeholderAnimatedStyle,
                  props.error && props.editable && styles.placeholderError,
                  needSpace && styles.placeholderWithSpace,
                ]}
              >
                <Animated.Text
                  style={[
                    styles.placeholderText,
                    placeholderTextAnimatedStyle,
                    props.error && props.editable && styles.placeholderError,
                  ]}
                  numberOfLines={1}
                  ellipsizeMode={'tail'}
                >
                  {labeltext}
                </Animated.Text>
              </Animated.View>
              <TextInput
                ref={elementRef}
                value={formattedValue}
                editable={props.editable}
                onChangeText={(text) => {
                  const sanitized = sanitizeLiveNumberInput(text);
                  // Nicht mehr als 1 , oder . zulassen
                  const sepCount = (sanitized.match(/[.,]/g) || []).length;
                  if (sepCount <= 1) {
                    triggerHandleChange(sanitized);
                  }
                }}
                onFocus={handleFocus}
                onBlur={(e) => handleBlur(e)}
                keyboardType="numeric"
                onKeyPress={({ nativeEvent }) => {
                  if (nativeEvent.key === 'ArrowUp') {
                    if (step && step > 0) {
                      handleIncrement();
                    }
                  } else if (nativeEvent.key === 'ArrowDown') {
                    if (step && step > 0) {
                      handleDecrement();
                    }
                  }
                }}
                style={[
                  styles.inputNumber,
                  props.error && props.editable && styles.inputError,
                  isFocused && props.editable && styles.inputFocused,
                  !props.editable && styles.disabledInput,
                  needSpace && styles.inputNumberWithSpace,
                  { textAlign },
                ]}
                accessible={true}
                accessibilityRole="text"
                accessibilityLabel={t(
                  'form_accessibility_textInput_number_label',
                  { label: labeltext },
                )}
                accessibilityHint={
                  props.error
                    ? t('form_accessibility_textInput_error_hint', {
                        error: localErrMsg,
                      })
                    : t('form_accessibility_textInput_number_hint', {
                        label: labeltext,
                      })
                }
                accessibilityState={{ disabled: !props.editable }}
                accessibilityValue={{ text: localValue }}
                accessibilityLiveRegion="assertive"
                importantForAccessibility="yes"
                {...(Platform.OS === 'web'
                  ? {
                      'aria-invalid': !!localErrMsg,
                      'aria-haspopup': 'false',
                      'aria-expanded': isFocused,
                    }
                  : {})}
              />
              {needSpace && (
                <View 
                onStartShouldSetResponderCapture={() => true}
                onResponderGrant={(e) => {
                  // @ts-ignore
                  e.preventDefault?.();
                  preventNextBlurRef.current = true;
                  elementRef.current?.focus?.();
                }}
                style={styles.numberBtns}>
                  <TouchableOpacity
                    onPress={handleIncrement}
                    style={styles.numberBtnUp}
                    accessibilityRole="button"
                    accessibilityLabel={t(
                      'form_accessibility_textInput_number_increment_label',
                      { label: labeltext },
                    )}
                    accessibilityHint={t(
                      'form_accessibility_textInput_number_increment_hint',
                      { label: labeltext },
                    )}
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
                    accessibilityLabel={t(
                      'form_accessibility_textInput_number_decrement_label',
                      { label: labeltext },
                    )}
                    accessibilityHint={t(
                      'form_accessibility_textInput_number_decrement_hint',
                      { label: labeltext },
                    )}
                  >
                    <AndpIcon
                      name={'arrowDown'}
                      size={inputElementTheme.numberButtons.iconSize}
                      color={inputElementTheme.numberButtons.iconColor}
                    />
                  </TouchableOpacity>
                </View>
              )}
            </>
          )}
        </View>
      </FormElementWrapper>
    );
  }
  // Normales Feld
  else {
    return (
      <FormElementWrapper
        formState={formState}
        formComponentAttributes={props}
        uiState={uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        hideLabel={true}
        localErrorMsg={localErrMsg}
        showFilledIcon={isFilled}
        description={props.description}
        showDeleteContentBtn={false}
        hideLoading={true}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View
          style={[
            Platform.OS === 'web' &&
              !props.editable &&
              ({ cursor: 'not-allowed' } as any),
          ]}
        >
          {isInputAllowed && (
            <>
              <Animated.View
                pointerEvents="none"
                style={[
                  styles.placeholder,
                  placeholderAnimatedStyle,
                  props.error && props.editable && styles.placeholderError,
                ]}
              >
                <Animated.Text
                  style={[
                    styles.placeholderText,
                    placeholderTextAnimatedStyle,
                    props.error && props.editable && styles.placeholderError,
                  ]}
                  numberOfLines={1}
                  ellipsizeMode={'tail'}
                >
                  {labeltext}
                </Animated.Text>
              </Animated.View>
              <TextInput
                pointerEvents={!props.editable ? 'none' : 'auto'}
                ref={elementRef}
                value={localValue}
                editable={props.editable}
                onChangeText={triggerHandleChange}
                onFocus={handleFocus}
                onBlur={(e) => handleBlur(e)}
                style={[
                  styles.input,
                  props.error && props.editable && styles.inputError,
                  isFocused && props.editable && styles.inputFocused,
                  !props.editable && styles.disabledInput,
                ]}
                accessible={true}
                accessibilityRole="text"
                accessibilityLabel={t(
                  'form_accessibility_textInput_string_label',
                  { label: labeltext },
                )}
                accessibilityHint={
                  props.error
                    ? t('form_accessibility_textInput_error_hint', {
                        error: localErrMsg,
                      })
                    : t('form_accessibility_textInput_string_hint', {
                        label: labeltext,
                      })
                }
                accessibilityState={{ disabled: !props.editable }}
                accessibilityValue={{ text: localValue }}
                accessibilityLiveRegion="assertive"
                importantForAccessibility="yes"
                {...(Platform.OS === 'web' && {
                  'aria-invalid': !!localErrMsg,
                  'aria-haspopup': 'false',
                  'aria-expanded': isFocused,
                })}
              />
            </>
          )}
        </View>
      </FormElementWrapper>
    );
  }
}

InputElement.validate = function (
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

/**
 * Locale‐aware Formatter für numerische Eingabetexte.
 *
 * - Leere Eingabe → bleibt unverändert.
 * - Ermittelt Locale (navigator.language): ‹,› oder ‹.› als Dezimaltrennzeichen.
 * - Entfernt alle Apostrophe, ersetzt Kommata durch ‹.› und erlaubt nur Ziffern bzw. einen Dezimalpunkt.
 * - Parst das Resultat, wendet Präzision und optionalen Schritt an und rundet entsprechend.
 * - Formatiert schließlich mit Intl.NumberFormat, nutzt Tausender-Grouping (Apostroph) nur für ‹-CH›, sonst ohne.
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

InputElement.getSummaryValue = function (
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
export default InputElement;
