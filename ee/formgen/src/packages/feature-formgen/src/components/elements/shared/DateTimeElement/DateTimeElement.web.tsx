import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, TouchableOpacity, Platform, Pressable } from 'react-native';
import moment, { Moment } from 'moment';
import { TFunction } from 'i18next';
import { Portal } from 'react-native-portalize';
import Animated, { useAnimatedStyle } from 'react-native-reanimated';
import {
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
} from '../../../../types/interfaces';
import { FormElementWrapper } from '../../helper/FormElementWrapper';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpInput from '@andp/core/src/components/AndpInputElement';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { dateTimeElementStyles } from '../../../../styles/common/common-style';
import { dateTimeElementStyles as customDateTimeElementStyles } from '../../../../styles/custom/custom-style';
import { formElementWrapperTheme } from '@andp/core/src/styles/theme';
import theme from '@andp/core/src/styles/theme';
import * as formEventsUtils from '../../../../utils/FormEventsUtils';
import {
  validate as dateTimeValidate,
  getSummaryValue as dateTimeGetSummaryValue,
} from './DateTimeValidate';

import 'moment/locale/de';
import 'moment/locale/fr';
import 'moment/locale/it';
const Datetime = require('react-datetime').default;
require('../../../../styles/common/common-datepicker.css');
require('../../../../styles/custom/custom-datepicker.css');
require('react-datetime/css/react-datetime.css');

import { useDateTimeState, handleDateChange } from './UseDateTimeState';

type DateTimeElementType = React.FC<IFormComponentAttributes> & {
  validate: (
    state: FormState,
    jsonpath: string,
    id: string,
    updateErrorMessages: boolean,
    t: TFunction,
    formGridOptions?: IFormGridOptions,
    localValue?: any,
  ) => any;
  getSummaryValue: (
    formState: FormState,
    jsonpath: string,
    id: string,
    t: TFunction,
    formGridOptions?: IFormGridOptions,
  ) => string;
};

function DateTimeElement(props: IFormComponentAttributes) {
  const { t } = props as { t: TFunction };
  const styles = useMemo(
    () => mergeStyles(dateTimeElementStyles, customDateTimeElementStyles),
    [],
  );

  const {
    schema,
    isTimeField,
    labelText,
    localErrMsg,
    dateValue,
    setDateValue,
    formattedPropsValue,
    placeholderTop,
    placeholderFontSize,
    handleAnimationForBlur,
    handleAnimationForFocus,
  } = useDateTimeState(props);

  const [isFocused, setIsFocused] = useState(false);
  const refInput = useRef<any>(null);
  const arrowToggleRef = useRef<View>(null);
  const inputContainerRef = useRef<any>(null);
  const [anchor, setAnchor] = useState({ x: 0, y: 0, width: 0, height: 0 });
  const [open, setOpen] = useState(false);
  const portalRef = useRef<HTMLDivElement>(null);

  const formattedPropsDisplay = formattedPropsValue;
  const [inputValue, setInputValue] = useState(formattedPropsDisplay);

  useEffect(() => {
    // Monkey-patch measureLayout for Web scrolling support
    // Scroll-Fix Web:
    // Monkey-patch measureLayout for Web scrolling support.
    // The RendererScreen on web expects measureLayout to be present to calculate scroll position.
    // We delegate this to the inner inputContainerRef which corresponds to the actual DOM element.
    if (refInput.current && !refInput.current.measureLayout) {
        refInput.current.measureLayout = (relativeTo: any, onSuccess: any, onFail: any) => {
            if (inputContainerRef.current && inputContainerRef.current.measureLayout) {
                inputContainerRef.current.measureLayout(relativeTo, onSuccess, onFail); 
            }
        };
    }
    // IMPORTANT: No dependency array [] here!
    // We need to re-register the ref on every render so the parent knows about the current error state.
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, refInput);
  });

  const wasFocusedRef = useRef(false);
  useEffect(() => {
    wasFocusedRef.current = isFocused;
  }, [isFocused]);
  //parse acta nova value
  useEffect(() => {
    if (wasFocusedRef.current) {
      return;
    }
    let datevalue;
    if (props.value === null || props.value === undefined) {
      datevalue = '';
    } else {
      datevalue = props.value;
    }
    const SERVER_FMT = 'YYYY-MM-DD[T]HH:mm:ss';

    const mServer = moment(datevalue, SERVER_FMT, true);
    const mIso = moment(datevalue, moment.ISO_8601, true);
    const mTime = moment(datevalue, 'HH:mm', true);
    const mDate = moment(datevalue, 'DD.MM.YYYY', true);

    let m: Moment | null = mServer.isValid()
      ? mServer
      : mIso.isValid()
      ? mIso
      : mTime.isValid()
      ? mTime
      : mDate.isValid()
      ? mDate
      : null;

    if (m) {
      setDateValue(m);
      const display = isTimeField
        ? m.format('HH:mm')
        : schema.dateOnly
        ? m.format('DD.MM.YYYY')
        : m.format('DD.MM.YYYY HH:mm');
      setInputValue(display);
      if (datevalue !== '') {
        handleAnimationForFocus();
      }
    } else {
      setDateValue(datevalue);
      setInputValue(String(datevalue));
      if (datevalue === '') {
        handleAnimationForBlur();
      }
    }
  }, [props.value, isTimeField, schema.dateOnly]);

  const placeholderAnimatedStyle = useAnimatedStyle(() => ({
    top: placeholderTop.value,
  }));
  const placeholderTextAnimatedStyle = useAnimatedStyle(() => ({
    fontSize: placeholderFontSize.value,
  }));

  const measureAnchor = () => {
    const node = inputContainerRef.current;
    if (!node) return;
    const rect = node.getBoundingClientRect();
    const root = document.getElementById('root');

    const offsetY = root
      ? root.getBoundingClientRect().top + window.pageYOffset
      : 0;
    const offsetX = root
      ? root.getBoundingClientRect().left + window.pageXOffset
      : 0;

    const nx = rect.left + window.pageXOffset - offsetX;
    const ny = rect.top - offsetY + window.pageYOffset;

    if (
      nx !== anchor.x ||
      ny !== anchor.y ||
      rect.width !== anchor.width ||
      rect.height !== anchor.height
    ) {
      setAnchor({ x: nx, y: ny, width: rect.width, height: rect.height });
    }
  };

  useEffect(() => {
    measureAnchor();
  });

  /* -------------------------------------------------------------------- */
  /* outside click handling for portal                                    */
  /* -------------------------------------------------------------------- */
  useEffect(() => {
    // updateOnChange beim ersten Mal, außer readOnly
    if (!open && props.editable) {
      handleBlur();
    }

    function handleDocClick(e: MouseEvent | TouchEvent) {
      const target = e.target as Node;
      const inPortal = portalRef.current?.contains(target);
      const inInput = inputContainerRef.current?.contains(target);

      if (open && !inPortal && !inInput) {
        setOpen(false);
      }
    }

    document.addEventListener('mousedown', handleDocClick);
    document.addEventListener('touchstart', handleDocClick);
    return () => {
      document.removeEventListener('mousedown', handleDocClick);
      document.removeEventListener('touchstart', handleDocClick);
    };
  }, [open]);

  /* -------------------------------------------------------------------- */
  /* handlers                                                             */
  /* -------------------------------------------------------------------- */
  const handleArrowToggle = (e: any) => {
      if (!props.editable) return;
      if (isFocused && open) {
        // Dropdown ist offen -> schließen
        setIsFocused(false);
        handleAnimationForBlur();
        if (refInput.current && typeof refInput.current.blur === 'function') {
          refInput.current.blur();
        }
        handleBlur();
      } else {
        // Dropdown ist geschlossen -> öffnen und fokussieren
        setIsFocused(true);
        handleAnimationForFocus();
        if (refInput.current && typeof refInput.current.focus === 'function') {
          refInput.current.focus();
        }
        handleFocus();
      }
    };

  const onChangeWeb = (val: string | Moment) => {
    if (!val || (typeof val === 'string' && val.trim() === '')) {
      setDateValue('');
      handleDateChange(props, '', isTimeField);
      return;
    }
    const m = moment.isMoment(val) ? val : moment(val);
    setDateValue(m);
    const formatted = isTimeField
      ? m.format('HH:mm')
      : schema.dateOnly
      ? m.format('DD.MM.YYYY')
      : m.format('DD.MM.YYYY HH:mm');
    setInputValue(formatted);
  };

  /**
   * ## handleDelete
   *
   * Wird ausgelöst, wenn der Nutzer auf das „Löschen“-Icon im Datum-/Zeit-Eingabefeld klickt.
   *
   * Was tut die Methode?
   * 1. Setzt den lokalen Eingabe-Value zurück (inputValue = '').
   * 2. Setzt den internen Datum-Value zurück (dateValue = '').
   * 3. Ruft formEventsUtils.handleChange() mit leerem String als für das value prop, um auch im
   *    übergeordnetem Form State das value zu löschen
   * 4. Öffnet erneut den Picker bzw. setzt den Fokus auf das Eingabefeld (handleFocus()),
   *    damit der Nutzer sofort einen neuen Wert wählen kann.
   */
  const handleDelete = () => {
    setInputValue('');
    setDateValue('');
    formEventsUtils.handleChange(
      props.formState,
      props.uiState,
      props.restService,
      props.jsonpath,
      props.id,
      false,
      props.updateState,
      '',
      props.formGridOptions,
      props.editable,
    );
    handleFocus();
  };

  const onChangeInput = (val: string) => {
    const m = isTimeField
      ? moment(val, 'HH:mm', true)
      : schema.dateOnly
      ? moment(val, 'DD.MM.YYYY', true)
      : moment(val, 'DD.MM.YYYY HH:mm', true);
    setDateValue(m);
    setInputValue(val);
  };

  const handleFocus = () => {
    if (!props.editable) return;
    handleAnimationForFocus();

    if (refInput.current?.focus) {
      setIsFocused(true);
      refInput.current.focus();
    }
    measureAnchor();
    setOpen(true);
  };

  const handleBlur = (onClose?: boolean) => {
    setIsFocused(false);
    if ((inputValue === '' || !dateValue || dateValue === '') && !onClose) {
      handleAnimationForBlur();
    }
    handleDateChange(props, dateValue, isTimeField, inputValue);
  };

  const renderInput = () => (
    <AndpInput
      accessible={true}
      accessibilityRole={isTimeField ? 'adjustable' : 'combobox'}
      accessibilityLabel={t('form_accessibility_datepicker_input', {
        label: labelText,
      })}
      accessibilityHint={
        props.error
          ? t('form_accessibility_datepicker_input_error_hint', {
              error: localErrMsg,
            })
          : t('form_accessibility_datepicker_input_hint', { label: labelText })
      }
      accessibilityState={{
        disabled: !props.editable,
      }}
      accessibilityValue={{ text: inputValue }}
      accessibilityLiveRegion="assertive"
      importantForAccessibility="yes"
      {...{
        'aria-invalid': !!localErrMsg,
        'aria-haspopup': 'dialog',
        'aria-expanded': open,
      }}
      editable={props.editable}
      ref={refInput}
      key={`datetime_${props.id}`}
      value={inputValue}
      onChangeText={onChangeInput}
      onFocus={handleFocus}
      onSubmitEditing={() => {
        setOpen(false);
        handleBlur();
      }}
      onBlur={(e: any) => {
        // Detect where the focus is moving to
        const nextTarget: any =
          e.relatedTarget || (document as any).activeElement;
        // Ignore blur events that go to <body> (e.g. click inside the portal
        // where focus isn't shifted to a concrete element)
        if (
          !nextTarget || // nothing focused
          nextTarget === document.body || // default to body
          (portalRef.current && portalRef.current.contains(nextTarget)) // inside calendar
        ) {
          return;
        }
        // Otherwise treat this as leaving the field
        setOpen(false);
        handleBlur();
      }}
      returnKeyType="done"
      placeholderTextColor={
        props.error
          ? commonColors.error
          : !props.value
          ? commonColors.mutedText
          : theme.colors.textPrimary
      }
      style={[
        styles.datePickerInput,
        props.error && props.editable && styles.errorDatePickerInput,
        isFocused && props.editable && styles.focusedDatePickerInput,
        !props.editable && styles.disabledDatePickerInput,
      ]}
      inputStyle={styles.datePickerInputText}
      rightElement={[
        inputValue !== '' && props.editable ? (
          <TouchableOpacity
            id={`${props.jsonpath}-delete-content-btn`}
            //@ts-ignore
            dataSet={{ class: 'delete-content-btn' }}
            key="clear"
            onPress={handleDelete}
            accessibilityRole="button"
            accessibilityLabel={t('form_accessibility_datepicker_clear')}
            accessibilityHint={t('form_accessibility_datepicker_clear_hint')}
          >
            <AndpIcon
              name="close"
              size={formElementWrapperTheme.deleteContentBtnIcon.size}
              color={formElementWrapperTheme.deleteContentBtnIcon.color}
            />
          </TouchableOpacity>
        ) : null,
        props.ANFieldLoadError ? (
          <AndpIcon
            key="warn"
            name="warning"
            color={formElementWrapperTheme.anFieldLoadErrorIcon.color}
            size={formElementWrapperTheme.anFieldLoadErrorIcon.size}
          />
        ) : null,
        <Pressable
          ref={arrowToggleRef}
          key="arrow"
          accessible
          accessibilityRole="button"
          accessibilityLabel={t('form_accessibility_dropdown_toggle')}
          onPress={handleArrowToggle}
        >
          <AndpIcon
            name={isFocused && open ? 'arrowUp' : 'arrowDown'}
            size={formElementWrapperTheme.dropdownIcon.size}
            color={
              props.error
                ? formElementWrapperTheme.dropdownIcon.colorError
                : formElementWrapperTheme.dropdownIcon.color
            }
          />
        </Pressable>
      ]}
    />
  );

  const lng = props.formState.culture.slice(0, 2);
  moment.locale(lng);

  return (
    <FormElementWrapper
      formState={props.formState}
      uiState={props.uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      description={props.description}
      formComponentAttributes={props}
      showDeleteContentBtn={false}
      hideLabel
      needFloatingLabel={false}
      localErrorMsg={localErrMsg}
      hideLoading={true}
      hideAnFieldLoadErrorIcon
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      <View ref={inputContainerRef}>
        <Animated.Text
          style={[
            styles.datePickerLabel,
            Platform.select({
              web: {
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              },
              default: null,
            }),
            placeholderAnimatedStyle,
            placeholderTextAnimatedStyle,
            props.error && props.editable && styles.errorDatePickerLabel,
          ]}
          // @ts-ignore
          pointerEvents="none"
        >
          {labelText}
        </Animated.Text>

        <Datetime
          renderInput={renderInput}
          renderView={(viewMode: any, renderCalendar: any) => (
            <>
              {open && (
                <Portal>
                  <div
                    ref={portalRef}
                    className="rdt custom-datetime rdtOpen"
                    style={{
                      top: anchor.y + anchor.height,
                      left: anchor.x,
                      width: anchor.width,
                    }}
                  >
                    <div className="rdtPicker">{renderCalendar()}</div>
                  </div>
                </Portal>
              )}
            </>
          )}
          locale={lng}
          value={dateValue}
          dateFormat={
            isTimeField ? false : schema.dateOnly ? 'DD.MM.YYYY' : 'DD.MM.YYYY'
          }
          timeFormat={isTimeField ? 'HH:mm' : schema.dateOnly ? false : 'HH:mm'}
          closeOnSelect={true}
          open={open}
          onOpen={() => setOpen(true)}
          onClose={() => {
            setOpen(false);
            handleBlur();
          }}
          closeOnClickOutside={true}
          onChange={(val: any) => {
            onChangeWeb(val);
            if (schema.dateOnly) {
              setOpen(false);
              handleBlur(true);
            }
          }}
          className="custom-datetime"
        />
      </View>
    </FormElementWrapper>
  );
}

const DateTimeElementWithStatics =
  DateTimeElement as unknown as DateTimeElementType;

DateTimeElementWithStatics.validate = dateTimeValidate;
DateTimeElementWithStatics.getSummaryValue = dateTimeGetSummaryValue;

/* -------------------------------------------------------------------- */
/* constants + styles                                                   */
/* -------------------------------------------------------------------- */
const commonColors = {
  error: '#dc0018',
  errorBackground: '#fcedef',
  darkText: '#333333',
  mutedText: theme.colors.textMuted,
  loadingText: '#666666',
  backgroundPrimary: theme.colors.backgroundPrimary,
  borderColor: theme.border.color.darkGray,
};

export default DateTimeElementWithStatics;
