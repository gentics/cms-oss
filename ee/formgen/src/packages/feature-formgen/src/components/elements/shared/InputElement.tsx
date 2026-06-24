import React, { useEffect, useMemo, useRef, useState } from 'react';
import { TextInput } from 'react-native';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  SummaryOptions,
} from '../../../types/interfaces';
import { inputElementStyles } from '../../../styles/common/common-style';
import { inputElementStyles as customInputElementStyles } from '../../../styles/custom/custom-style';
import { SummaryElement } from '../helper/SummaryElement';
import * as utils from '../../../utils/Utils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import * as validationUtils from '../../../utils/ValidationUtils';
import { StatesToUpdateEnum } from '../../../constants/enums/StateToUpdateEnum';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import SharedInputComponent, {
  sanitizeLiveNumberInput,
  sanitizeNumberInput,
  useTextareaHeight,
} from './components/InputElementComponent';

/**
 * InputElement renders a text-based form input for CMS/shared forms in three modes:
 * 1. **Textarea** — when `formGridOptions.numberOfLines > 1` and the field is not a number.
 *    Supports dynamic height adjustment via `useTextareaHeight`.
 * 2. **Number** — when the schema type is `"number"`.
 *    Filters live input via `sanitizeLiveNumberInput`; normalizes on blur via `sanitizeNumberInput`.
 * 3. **String** — for all other text fields (type `"string"`, `"textarea"`, `"property"`).
 *
 * Domain/project-specific behavior:
 * - Intentionally lean: no precision formatting, no step buttons, no overlay dialogs,
 *   and no FormMirrorStore — unlike `ANInputElement` which carries all of that AN-specific logic.
 * - Uses `SharedInputComponent` (from `InputElementComponent`) for all three render blocks
 *   so that the visual output stays in sync with `ANInputElement`.
 * - Placeholder label animates via `useInputAnimation` (Reanimated v2 spring animation).
 * - `triggerHandleChange` writes directly into `formState` and calls `updateState` only when
 *   the validation message changes, to avoid unnecessary re-renders.
 * - `handleBlur` triggers `formEventsUtils.handleChange` for server-side validation,
 *   but skips it when focus moves to the sticky footer buttons.
 * - `validate` and `getSummaryValue` are static methods required by the element type system.
 */
function InputElement(props: IFormComponentAttributes) {
  const { formState, uiState, t, restService } = props;

  const isInputAllowed =
    props.type === 'string' ||
    props.type === 'number' ||
    props.type === 'textarea' ||
    props.type === 'property';

  const [localValue, setLocalValue] = useState(props.value);
  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  const [isFocused, setIsFocused] = useState(false);
  const [lastHandleChangeValue, setLastHandleChangeValue] = useState(props.value);
  const [isFilled, setIsFilled] = useState(props.value !== undefined && props.value !== null && props.value !== '');
  const [unitBadgeWidth, setUnitBadgeWidth] = useState(0);

  let labeltext =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : '');

  const schema = utils.getSchemaForElement(props.formState.schema, props.id);
  const isNumber = schema.type === 'number';
  const elementRef = useRef<TextInput>(null);
  const styles = useMemo(() => mergeStyles(inputElementStyles, customInputElementStyles), []);
  const lang = restService?.getFullCulture?.() || 'de-AT';

  const numberOfLines =
    props.formGridOptions?.numberOfLines != null
      ? Number(props.formGridOptions.numberOfLines)
      : 1;
  const { height, updateHeight } = useTextareaHeight(numberOfLines);

  useEffect(() => {
    setLocalValue(props.value);
  }, [props.value]);

  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  const triggerHandleChange = (valueToUpdate: any) => {
    setLocalValue(valueToUpdate);

    if (valueToUpdate != '') {
      setIsFilled(true);
    } else {
      setIsFilled(false);
    }

    const localValidation = validationUtils.validateField(
      formState,
      props.jsonpath,
      props.id,
      true,
      props.formGridOptions,
      valueToUpdate,
    );

    const elementState = utils.getElementByJsonPath(formState.data, props.jsonpath, props.id);

    if (elementState != null) {
      if (Array.isArray(valueToUpdate)) {
        elementState.values = valueToUpdate;
      } else {
        elementState.value = valueToUpdate;
      }
      elementState.validationMessage = localValidation.errorMessage;
      utils.setStateElementByJsonpath(formState.data, props.jsonpath, elementState);
      if (localValidation.errorMessage !== localErrMsg) {
        props.updateState(StatesToUpdateEnum.ALL);
      }
    }
  };

  const handleFocus = () => {
    setIsFocused(true);
  };

  const handleBlur = async (e?: any) => {
    const relatedTarget = e?.nativeEvent?.relatedTarget;
    const isStickyFooterTarget =
      !!(
        relatedTarget &&
        (relatedTarget.id?.startsWith('sticky-bar-') ||
          relatedTarget.closest?.('[id^="sticky-bar-"]'))
      );
    const shouldValidate = !isStickyFooterTarget;

    setIsFocused(false);
    let valueToUpdate = localValue;
    if (isNumber) {
      valueToUpdate = sanitizeNumberInput(valueToUpdate, lang);
      if (valueToUpdate !== '' && valueToUpdate !== undefined) {
        const parsed = parseFloat(valueToUpdate);
        if (!isNaN(parsed)) {
          valueToUpdate = parsed;
        }
      }
      setLocalValue(valueToUpdate);
    }
    if (shouldValidate) {
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
        props.editable,
      );
    }
    setLastHandleChangeValue(valueToUpdate);
  };

  if (props.summary > 0) {
    return (
      <SummaryElement
        t={t}
        formState={props.formState}
        uiState={props.uiState}
        summaryOption={SummaryOptions.SummaryText}
        label={props.label}
        value={props.value}
        initUrl={props.restService.initUrl}
      />
    );
  }

  const isTextarea =
    !!(props.formGridOptions?.numberOfLines && props.formGridOptions?.numberOfLines > 1 && !isNumber);
  const alignment = isNumber && props.formGridOptions?.alignment;
  const textAlign: 'left' | 'center' | 'right' =
    alignment === 'center' || alignment === 'right' ? alignment : 'left';

  const unitText: string =
    isNumber && schema.unit
      ? utils.getI18nText(props.formState, schema.unit) ?? ''
      : '';
  const hasUnit = unitText.length > 0;
  const alignmentUnit = props.formGridOptions?.alignmentUnit ?? 'left';
  const isUnitLeft = localValue != null && localValue !== '' && hasUnit && alignmentUnit === 'left';
  const isUnitRight = localValue != null && localValue !== '' && hasUnit && alignmentUnit === 'right';

  return (
    <SharedInputComponent
      formState={formState}
      uiState={uiState}
      formComponentAttributes={props}
      localErrorMsg={localErrMsg}
      showFilledIcon={isFilled}
      isInputAllowed={isInputAllowed}
      value={localValue}
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
        const sepCount = (sanitized.match(/[.,]/g) || []).length;
        if (sepCount <= 1) {
          triggerHandleChange(sanitized);
        }
      }}
      onFocus={handleFocus}
      onBlur={handleBlur}
      onContentSizeChange={(event) => updateHeight(event.nativeEvent.contentSize.height)}
      elementRef={elementRef}
      styles={styles}
      t={t}
      unitText={unitText}
      unitBadgeWidth={unitBadgeWidth}
      isUnitLeft={isUnitLeft}
      isUnitRight={isUnitRight}
      onUnitLayout={setUnitBadgeWidth}
    />
  );
}

InputElement.validate = function (
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

InputElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
) {
  return utils.getValue(state, jsonpath, id);
};

export default InputElement;
