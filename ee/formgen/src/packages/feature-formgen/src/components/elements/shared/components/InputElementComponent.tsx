import React, { useEffect, useRef, useState } from 'react';
import { View, TextInput, Text, Platform, TextInputInstance } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
} from 'react-native-reanimated';
import { TFunction } from 'i18next';
import { FormState, IFormComponentAttributes, UIState, UnitPosition } from '../../../../types/interfaces';
import { FormElementWrapper } from '../../helper/FormElementWrapper';
import { inputElementTheme } from '@andp/core/src/styles/theme';
import { previewPointerEvents } from '../../../../utils/Utils';

// ─── Utility functions ────────────────────────────────────────────────────────

export function sanitizeLiveNumberInput(raw: string): string {
  if (!raw) return '';
  return raw.normalize('NFKC').replace(/[A-Za-z]/g, '');
}

export function getLocaleSeparators(locale: string): { groupingSep: string | null; decimalSep: string } {
  const parts = new Intl.NumberFormat(locale).formatToParts(1000.1);
  const groupPart = parts.find(p => p.type === 'group');
  const decimalPart = parts.find(p => p.type === 'decimal');
  return {
    groupingSep: groupPart ? groupPart.value : null,
    decimalSep: decimalPart ? decimalPart.value : '.',
  };
}

export function sanitizeNumberInput(raw: string, lang: string): string {
  const { groupingSep, decimalSep } = getLocaleSeparators(lang);
  return raw
    .normalize('NFKC')
    .replace(/['`\u2018\u2019\u201B\u2032\u2035\u02BC\u00B4]/g, '') // Entf alle Apostroph-Varianten
    .replace(/[\s\u00A0\u202F\u2007\u2009\u200A]+/g, '') // Entf alle Whitespaces
    .replace(groupingSep ? new RegExp(`\\${groupingSep}`, 'g') : /\u0000/g, '') // entf alle Gruppierungszeichen
    .replace(decimalSep === '.' ? /\u0000/ : new RegExp(`${decimalSep}`, 'g'), '.') // Dezimaltrennzeichen -> Punkt
    .trim();
}

// ─── Animation Hook ───────────────────────────────────────────────────────────

export function useInputAnimation(initialValue: any, styles: any) {
  const stiffness = 1800;
  const damping = 150;

  const isFilled = initialValue !== undefined && initialValue !== null && initialValue !== '';
  const placeholderTop = useSharedValue(
    isFilled ? styles.placeholderFocused.top : styles.placeholder.top,
  );
  const placeholderFontSize = useSharedValue(
    isFilled ? styles.placeholderTextFocused.fontSize : styles.placeholderText.fontSize,
  );
  const placeholderAnimatedStyle = useAnimatedStyle(() => ({ top: placeholderTop.value }));
  const placeholderTextAnimatedStyle = useAnimatedStyle(() => ({ fontSize: placeholderFontSize.value }));

  const handleAnimationForBlur = () => {
    placeholderTop.value = withSpring(styles.placeholder.top, { stiffness, damping });
    placeholderFontSize.value = withSpring(styles.placeholderText.fontSize, { stiffness, damping });
  };

  const handleAnimationForFocus = () => {
    placeholderTop.value = withSpring(styles.placeholderFocused.top, { stiffness, damping });
    placeholderFontSize.value = withSpring(styles.placeholderTextFocused.fontSize, { stiffness, damping });
  };

  return { placeholderAnimatedStyle, placeholderTextAnimatedStyle, handleAnimationForFocus, handleAnimationForBlur };
}

// ─── Textarea Height Hook ─────────────────────────────────────────────────────

export function useTextareaHeight(numberOfLines: number) {
  const [height, setHeight] = useState(100);
  const debounceTimeout = useRef<any | null>(null);

  const updateHeight = (newHeight: number) => {
    if (debounceTimeout.current) clearTimeout(debounceTimeout.current);
    const lineHeight = 20;
    const minHeight =
      numberOfLines * lineHeight +
      inputElementTheme.paddingBottom +
      inputElementTheme.paddingTop;
    debounceTimeout.current = setTimeout(() => {
      setHeight(Math.max(minHeight, newHeight));
    }, 10);
  };

  return { height, updateHeight };
}

// ─── SharedInputComponent ─────────────────────────────────────────────────────

/**
 * InputElementComponent is the shared render layer for all text-based input elements.
 * It is used by both `InputElement` (CMS/shared) and `ANInputElement` (Acta Nova).
 *
 * This file also exports the utility functions and hooks that both consumers share:
 * - `sanitizeLiveNumberInput` — strips letters and normalizes Unicode during live typing.
 * - `getLocaleSeparators` — resolves grouping and decimal separator for a given locale.
 * - `sanitizeNumberInput` — full cleanup of a number string on blur (removes grouping chars,
 *   normalizes decimal separator to `.`).
 * - `useInputAnimation` — Reanimated v2 spring animation for the floating placeholder label
 *   (moves up/shrinks on focus, returns on blur).
 * - `useTextareaHeight` — tracks and debounces dynamic height for multiline text inputs.
 *
 * The default export renders one of three input variants based on the props:
 * 1. **Textarea** (`isTextarea`) — multiline `TextInput` with dynamic height.
 * 2. **Number** (`isNumber`) — numeric `TextInput` with `keyboardType="numeric"`,
 *    accepts a `renderNumberAddon` render prop for AN-specific increment/decrement buttons,
 *    and `onNumberKeyPress` for ArrowUp/ArrowDown keyboard support.
 * 3. **String** — single-line `TextInput` for all other text fields.
 *
 * Domain/project-specific behavior:
 * - All three variants are wrapped in `FormElementWrapper` for consistent error display,
 *   label, description, and loading state handling.
 * - Accessibility attributes (`accessibilityRole`, `accessibilityLabel`, `aria-*` on web)
 *   are set on every `TextInput`.
 * - Platform differences (`Platform.OS === 'web'`) are handled inline where needed
 *   (cursor style, `aria-*` props, focus refocus via `requestAnimationFrame`).
 */
interface SharedInputComponentProps {
  // FormElementWrapper
  formState: FormState;
  uiState: UIState;
  formComponentAttributes: IFormComponentAttributes;
  localErrorMsg: string;
  showFilledIcon?: boolean;

  // Input-Konfiguration
  isInputAllowed: boolean;
  value: string;
  editable: boolean;
  isNumber: boolean;
  isTextarea: boolean;
  textAlign?: 'left' | 'center' | 'right';
  numberOfLines?: number;
  height?: number;
  isFocused: boolean;

  // Handlers
  labeltext: string;
  onChangeText: (text: string) => void;
  onNumberChangeText: (text: string) => void;
  onFocus: () => void;
  onBlur: (e: any) => void;
  onContentSizeChange?: (event: any) => void;
  onNumberKeyPress?: (e: any) => void;

  // AN-spezifischer Slot für Increment/Decrement-Buttons
  renderNumberAddon?: () => React.ReactNode;
  needSpace?: boolean;

  // Unit-Badge
  unitText?: string;
  unitPosition?: UnitPosition;

  // Refs & Styles
  elementRef: React.RefObject<TextInputInstance | null>;
  styles: any;
  t: any;
}

export default function SharedInputComponent(props: SharedInputComponentProps) {
  const {
    formState, uiState, formComponentAttributes, localErrorMsg, showFilledIcon,
    isInputAllowed, value, editable, isNumber, isTextarea,
    textAlign = 'left', numberOfLines = 1, height, isFocused,
    labeltext, onChangeText, onNumberChangeText, onFocus, onBlur, onContentSizeChange, onNumberKeyPress,
    renderNumberAddon, needSpace = false,
    unitText,
    unitPosition = 'Append',
    elementRef, styles, t,
  } = props;

  const [unitBadgeWidth, setUnitBadgeWidth] = useState(0);
  const isPrepend = unitPosition === 'Prepend';
  const isAppend = unitPosition === 'Append';
  // Show the unit badge once the field has a value or is focused.
  const hasValue = value !== undefined && value !== null && value !== '';
  const showUnit = !!unitText && (hasValue || isFocused);

  const activeInputBorderWidth = isFocused && editable
    ? inputElementTheme.focused.borderWidth
    : inputElementTheme.borderWidth;
  const unitBadgeRightOffset =
    inputElementTheme.paddingLeft
    + (needSpace ? inputElementTheme.numberButtons.width + inputElementTheme.unitBadge.gap : 0)
    + (showFilledIcon ? inputElementTheme.isFilledIcon.width : 0);
  const unitBadgeRightPadding =
    unitBadgeRightOffset + unitBadgeWidth + inputElementTheme.unitBadge.gap - activeInputBorderWidth;

  const { placeholderAnimatedStyle, placeholderTextAnimatedStyle, handleAnimationForFocus, handleAnimationForBlur } =
    useInputAnimation(value, styles);

  useEffect(() => {
    const hasValue = value !== undefined && value !== null && value !== '';
    if (hasValue || isFocused) {
      handleAnimationForFocus();
    } else if (!isFocused) {
      handleAnimationForBlur();
    }
  }, [value, isFocused]);

  const { error } = formComponentAttributes;

  // Textfeld für mehrzeiligen Text
  if (isTextarea) {
    return (
      <FormElementWrapper
        formState={formState}
        formComponentAttributes={formComponentAttributes}
        uiState={uiState}
        numberOfColumns={formComponentAttributes.formGridOptions?.numberOfColumns}
        hideLabel={true}
        localErrorMsg={localErrorMsg}
        showDeleteContentBtn={false}
        description={formComponentAttributes.description}
        hideLoading={true}
        jsonPath={formComponentAttributes.jsonpath}
        objectId={formComponentAttributes.id}
        contextId={formComponentAttributes.contextId}
        selectedElementId={formComponentAttributes.selectedElementId}
        selectedElementContextId={formComponentAttributes.selectedElementContextId}
      >
        <View
          style={[
            Platform.OS === 'web' && (!editable || formState.isFormgridPreview) && ({ cursor: 'not-allowed' } as any),
          ]}
        >
          {isInputAllowed && (
            <View pointerEvents={previewPointerEvents(!editable || formState.isFormgridPreview)}>
              <Animated.View
                pointerEvents="none"
                style={[
                  styles.placeholder,
                  placeholderAnimatedStyle,
                  error && editable && styles.placeholderError,
                ]}
              >
                <Animated.Text
                  style={[
                    styles.placeholderText,
                    placeholderTextAnimatedStyle,
                    error && editable && styles.placeholderError,
                  ]}
                  numberOfLines={1}
                  ellipsizeMode={'tail'}
                >
                  {labeltext}
                </Animated.Text>
              </Animated.View>
              <TextInput
                ref={elementRef}
                editable={editable}
                value={value}
                onChangeText={onChangeText}
                onFocus={onFocus}
                onBlur={onBlur}
                multiline={true}
                numberOfLines={numberOfLines}
                onContentSizeChange={onContentSizeChange}
                style={[
                  styles.textarea,
                  { height },
                  error && editable && styles.inputError,
                  isFocused && editable && styles.inputAreaFocused,
                  !editable && styles.disabledInput,
                ]}
                accessible={true}
                accessibilityRole="text"
                accessibilityLabel={t('form_accessibility_textInput_multiline_label', { label: labeltext })}
                accessibilityHint={
                  error
                    ? t('form_accessibility_textInput_error_hint', { error: localErrorMsg })
                    : t('form_accessibility_textInput_multiline_hint', { label: labeltext })
                }
                accessibilityState={{ disabled: !editable }}
                accessibilityValue={{ text: value }}
                accessibilityLiveRegion="assertive"
                importantForAccessibility="yes"
                {...(Platform.OS === 'web' && {
                  'aria-invalid': !!localErrorMsg,
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
  if (isNumber) {
    return (
      <FormElementWrapper
        formState={formState}
        formComponentAttributes={formComponentAttributes}
        uiState={uiState}
        numberOfColumns={formComponentAttributes.formGridOptions?.numberOfColumns}
        hideLabel={true}
        localErrorMsg={localErrorMsg}
        showFilledIcon={showFilledIcon}
        description={formComponentAttributes.description}
        showDeleteContentBtn={false}
        needSpace={needSpace}
        hideLoading={true}
        jsonPath={formComponentAttributes.jsonpath}
        objectId={formComponentAttributes.id}
        contextId={formComponentAttributes.contextId}
        selectedElementId={formComponentAttributes.selectedElementId}
        selectedElementContextId={formComponentAttributes.selectedElementContextId}
      >
        <View
          style={
            Platform.OS === 'web' && (!editable || formState.isFormgridPreview) && ({ cursor: 'not-allowed' } as any)
          }
        >
          {isInputAllowed && (
            <>
              <Animated.View
                pointerEvents="none"
                style={[
                  styles.placeholder,
                  placeholderAnimatedStyle,
                  error && editable && styles.placeholderError,
                  needSpace && styles.placeholderWithSpace,
                ]}
              >
                <Animated.Text
                  style={[
                    styles.placeholderText,
                    placeholderTextAnimatedStyle,
                    error && editable && styles.placeholderError,
                  ]}
                  numberOfLines={1}
                  ellipsizeMode={'tail'}
                >
                  {labeltext}
                </Animated.Text>
              </Animated.View>
              <TextInput
                ref={elementRef}
                value={value}
                editable={editable}
                onChangeText={onNumberChangeText}
                onFocus={onFocus}
                onBlur={onBlur}
                keyboardType="numeric"
                onKeyPress={onNumberKeyPress}
                style={[
                  styles.inputNumber,
                  error && editable && styles.inputError,
                  isFocused && editable && styles.inputFocused,
                  !editable && styles.disabledInput,
                  needSpace && styles.inputNumberWithSpace,
                  { textAlign },
                  showUnit && isPrepend && unitBadgeWidth > 0 && {
                    paddingLeft:
                      inputElementTheme.paddingLeft
                      + unitBadgeWidth
                      + inputElementTheme.unitBadge.gap
                      - activeInputBorderWidth,
                  },
                  showUnit && isAppend && unitBadgeWidth > 0 && {
                    paddingRight: unitBadgeRightPadding,
                  },
                ]}
                accessible={true}
                accessibilityRole="text"
                accessibilityLabel={t('form_accessibility_textInput_number_label', { label: labeltext })}
                accessibilityHint={
                  error
                    ? t('form_accessibility_textInput_error_hint', { error: localErrorMsg })
                    : t('form_accessibility_textInput_number_hint', { label: labeltext })
                }
                accessibilityState={{ disabled: !editable }}
                accessibilityValue={{ text: value }}
                accessibilityLiveRegion="assertive"
                importantForAccessibility="yes"
                {...(Platform.OS === 'web'
                  ? { 'aria-invalid': !!localErrorMsg, 'aria-haspopup': 'false', 'aria-expanded': isFocused }
                  : {})}
              />
              {showUnit && (
                <View
                  pointerEvents="none"
                  onLayout={(e) => setUnitBadgeWidth(e.nativeEvent.layout.width)}
                  style={[
                    styles.unitBadge,
                    isPrepend ? styles.unitBadgeLeft : { right: unitBadgeRightOffset },
                    error && editable && styles.unitBadgeError,
                  ]}
                >
                  <Text style={[styles.unitBadgeText, error && editable && styles.unitBadgeTextError]}>
                    {unitText}
                  </Text>
                </View>
              )}
              {renderNumberAddon?.()}
            </>
          )}
        </View>
      </FormElementWrapper>
    );
  }

  // Normales String-Feld
  return (
    <FormElementWrapper
      formState={formState}
      formComponentAttributes={formComponentAttributes}
      uiState={uiState}
      numberOfColumns={formComponentAttributes.formGridOptions?.numberOfColumns}
      hideLabel={true}
      localErrorMsg={localErrorMsg}
      showFilledIcon={showFilledIcon}
      description={formComponentAttributes.description}
      showDeleteContentBtn={false}
      hideLoading={true}
      jsonPath={formComponentAttributes.jsonpath}
      objectId={formComponentAttributes.id}
      contextId={formComponentAttributes.contextId}
      selectedElementId={formComponentAttributes.selectedElementId}
      selectedElementContextId={formComponentAttributes.selectedElementContextId}
    >
      <View
        style={[
          Platform.OS === 'web' && (!editable || formState.isFormgridPreview) && ({ cursor: 'not-allowed' } as any),
        ]}
      >
        {isInputAllowed && (
          <>
            <Animated.View
              pointerEvents="none"
              style={[
                styles.placeholder,
                placeholderAnimatedStyle,
                error && editable && styles.placeholderError,
              ]}
            >
              <Animated.Text
                style={[
                  styles.placeholderText,
                  placeholderTextAnimatedStyle,
                  error && editable && styles.placeholderError,
                ]}
                numberOfLines={1}
                ellipsizeMode={'tail'}
              >
                {labeltext}
              </Animated.Text>
            </Animated.View>
            <TextInput
              pointerEvents={previewPointerEvents(!editable || formState.isFormgridPreview)}
              ref={elementRef}
              value={value}
              editable={editable}
              onChangeText={onChangeText}
              onFocus={onFocus}
              onBlur={onBlur}
              style={[
                styles.input,
                error && editable && styles.inputError,
                isFocused && editable && styles.inputFocused,
                !editable && styles.disabledInput,
              ]}
              accessible={true}
              accessibilityRole="text"
              accessibilityLabel={t('form_accessibility_textInput_string_label', { label: labeltext })}
              accessibilityHint={
                error
                  ? t('form_accessibility_textInput_error_hint', { error: localErrorMsg })
                  : t('form_accessibility_textInput_string_hint', { label: labeltext })
              }
              accessibilityState={{ disabled: !editable }}
              accessibilityValue={{ text: value }}
              accessibilityLiveRegion="assertive"
              importantForAccessibility="yes"
              {...(Platform.OS === 'web' && {
                'aria-invalid': !!localErrorMsg,
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
