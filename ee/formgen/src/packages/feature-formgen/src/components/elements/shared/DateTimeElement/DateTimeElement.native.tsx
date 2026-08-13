import React, { useMemo, useState } from 'react';
import { Platform, Pressable, View } from 'react-native';
import moment, { Moment } from 'moment';

import AndpInput from '@andp/core/src/components/AndpInputElement';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import * as utils from '../../../../utils/Utils';
import { TFunction } from 'i18next';
import {
  ISchemaFieldProperties,
  IFormComponentAttributes,
} from '../../../../types/interfaces';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { formElementWrapperStyles as customFormElementWrapperStyles } from '../../../../styles/custom/custom-style.native';
import { formElementWrapperStyles } from '../../../../styles/common/common-style.native';
import { dateTimeElementStyles } from '../../../../styles/common/common-style.native';
import { dateTimeElementStyles as customDateTimeElementStyles } from '../../../../styles/custom/custom-style.native';
import Animated, {
  AnimatedStyle,
  useAnimatedStyle,
  useSharedValue,
  withSpring,
} from 'react-native-reanimated';
import RNDateTimePicker, {
  DateTimePickerAndroid,
} from '@react-native-community/datetimepicker';

import { useDateTimeState, handleDateChange } from './UseDateTimeState';
import { validate, getSummaryValue } from './DateTimeValidate';

type WithStatics<T> = T & {
  validate: typeof validate;
  getSummaryValue: typeof getSummaryValue;
};

type DisplayedComponents = 'date' | 'hourAndMinute' | 'dateAndTime';

const DISPLAY_FORMAT: Record<DisplayedComponents, string> = {
  date: 'DD.MM.YYYY',
  hourAndMinute: 'HH:mm',
  dateAndTime: 'DD.MM.YYYY HH:mm',
};

interface DateTimeElementNativeProps {
  t: TFunction<'translation', undefined>;
  styles?: any;
  animatedStyles?: {
    placeholderAnimatedStyle?: AnimatedStyle<any>;
    placeholderTextAnimatedStyle?: AnimatedStyle<any>;
  };

  formProps?: IFormComponentAttributes;
  props?: IFormComponentAttributes;
  formState?: IFormComponentAttributes['formState'];
  id?: IFormComponentAttributes['id'];
  error?: IFormComponentAttributes['error'];
  editable?: IFormComponentAttributes['editable'];
  label?: IFormComponentAttributes['label'];
  required?: IFormComponentAttributes['required'];
  dateValue: any;
  localErrMsg: any;
  initialDisplayValue: string;
  deleteDate: () => void;
  onChangeHelper: (val: string | Moment) => void;
  handleAnimationForBlur: () => void;
  handleAnimationForFocus: () => void;
}

type FormPropsInput =
  | { formProps?: IFormComponentAttributes; props?: IFormComponentAttributes }
  | (Partial<IFormComponentAttributes> & { [k: string]: any });

function useNormalizedFormProps(p: FormPropsInput): IFormComponentAttributes {
  if ((p as any).formProps?.formState) return (p as any).formProps;
  if ((p as any).props?.formState) return (p as any).props;

  const maybeInline = p as Partial<IFormComponentAttributes>;
  if (maybeInline.formState && typeof maybeInline.id !== 'undefined') {
    const { formState, id, error, editable, label, required, ...rest } =
      maybeInline;
    return {
      formState,
      id: id as any,
      error,
      editable,
      label,
      required,
      ...(rest as any),
    } as IFormComponentAttributes;
  }

  throw new Error(
    'DateTimeElementNative: formProps/formState fehlen. Übergib {formProps}, {props} oder Inline-IFormComponentAttributes.',
  );
}

const DateTimeElementNative: React.FC<DateTimeElementNativeProps> = (
  allProps,
) => {
  const {
    t,
    animatedStyles,
    dateValue,
    localErrMsg,
    initialDisplayValue,
    deleteDate,
    onChangeHelper,
    handleAnimationForBlur,
    handleAnimationForFocus,
  } = allProps;

  const onChange = onChangeHelper ?? (() => {});
  const onDelete = deleteDate ?? (() => {});

  const { placeholderAnimatedStyle = {}, placeholderTextAnimatedStyle = {} } =
    animatedStyles ?? {};

  const openAndroidPickerRef = React.useRef<(() => void) | undefined>(undefined);

  const formProps = useNormalizedFormProps(allProps);
  
  // Ref logic for scrolling and focus
  const elementRef = React.useRef<any>(null);
  const containerRef = React.useRef<any>(null);

  React.useEffect(() => {
    if (formProps.updateFirstErrorElementRef) {
      if (!elementRef.current) {
         elementRef.current = {};
      }
      // Monkey-patch focus to open the picker
      elementRef.current.focus = () => {
         if (Platform.OS === 'android') {
             // For Android, we need to trigger the picker logic
             // We can access the internal function via a ref if we exposed it, 
             // but here we are inside the component closure, so we can just call it?
             // No, we need to expose it to the ref which is passed up.
             // But 'openAndroidPicker' is defined below. 
             // Solution: update the ref.current.focus later or use a mutable ref that delegates.
         } else {
             setOpen(true);
         }
      };
      
      // We'll assign the View to a property or use the containerRef for measurement
      // But findNodeHandle usually expects the ref.current to BE the native handle or component.
      // So we should make elementRef.current point to the View.
      // We will do this in the View ref callback.
      
      // Formgen Scroll-Fix:
      // We register the ref with the parent form component here.
      // IMPORTANT: This useEffect MUST NOT have a dependency array (e.g. []).
      // It needs to run on every render because `formProps` (containing error state) changes,
      // and checking strict equality of function props might fail or the parent might
      // need to be re-notified of the current ref when the error status updates.
      // If we limited this to mount-only, subsequent validation errors would not find this element.
      formProps.updateFirstErrorElementRef(formProps, elementRef);
    }
  });

  // Update focus handler with latest closure if needed, particularly for Android functions defined later
  React.useEffect(() => {
     if (elementRef.current) {
         elementRef.current.focus = () => {
             if (Platform.OS === 'android') {
                // accessing openAndroidPicker variable which involves state
                 // eslint-disable-next-line @typescript-eslint/no-use-before-define
                 openAndroidPickerRef.current?.();
             } else {
                 setOpen(true);
             }
         };
     }
  });

  const styles = useMemo(
    () => mergeStyles(dateTimeElementStyles, customDateTimeElementStyles),
    [],
  );
  const wrapperStyles = useMemo(
    () => mergeStyles(formElementWrapperStyles, customFormElementWrapperStyles),
    [],
  );

  const prevDisplayRef = React.useRef<string>(initialDisplayValue);
  const hadValueOnOpenRef = React.useRef<boolean>(!!initialDisplayValue);

  // Schema / Flags
  const schema: ISchemaFieldProperties = utils.getSchemaForElement(
    formProps.formState.schema,
    formProps.id,
  );
  const hasError = !!formProps.error;
  const isEditable = !!formProps.editable;

  const type: DisplayedComponents =
    schema.type === 'time'
      ? 'hourAndMinute'
      : schema.dateOnly
      ? 'date'
      : 'dateAndTime';

  const convertISOToHumanDisplay = (value: string) => {
    const d = moment(value);
    return d.isValid() ? d.format(DISPLAY_FORMAT[type]) : '';
  };

  // UI-State
  const [open, setOpen] = useState(false);
  const [displayValue, setDisplayValue] = useState<string>(initialDisplayValue);

  const toPickerDate = (v: any): Date => {
    const m = moment.isMoment(v) ? v : v ? moment(v) : null;
    return m && m.isValid() ? m.toDate() : new Date();
  };
  const [pickerDate, setPickerDate] = useState<Date>(() =>
    toPickerDate(dateValue),
  );

  React.useLayoutEffect(() => {
    if (open) setPickerDate(toPickerDate(dateValue));
  }, [open]);

  //Animated Label
  const labelText =
    utils.getI18nText(formProps.formState, formProps.label) +
    (formProps.required ? ' *' : '');

  const hasInitialValue =
    !!initialDisplayValue ||
    (moment.isMoment(dateValue) ? dateValue.isValid() : !!dateValue);

  const stiffness = 1800;
  const damping = 150;
  const placeholderTopSV = useSharedValue(hasInitialValue ? styles.datePickerLabelActive.top : styles.datePickerLabel.top);
  const placeholderFontSV = useSharedValue(hasInitialValue ? styles.datePickerLabelActive.fontSize : styles.datePickerLabel.fontSize);

  const localPlaceholderStyle = useAnimatedStyle(() => ({
    top: placeholderTopSV.value,
  }));
  const localPlaceholderTextStyle = useAnimatedStyle(() => ({
    fontSize: placeholderFontSV.value,
  }));

  React.useEffect(() => {
    if (!displayValue) {
      placeholderTopSV.value = withSpring(styles.datePickerLabel.top, { stiffness, damping });
      placeholderFontSV.value = withSpring(styles.datePickerLabel.fontSize, { stiffness, damping });
    }
  }, [displayValue]);

  const runFocus = () => {
    (handleAnimationForFocus ?? (() => {}))();
    placeholderTopSV.value = withSpring(styles.datePickerLabelActive.top, { stiffness, damping });
    placeholderFontSV.value = withSpring(styles.datePickerLabelActive.fontSize, { stiffness, damping });
  };
  const runBlur = () => {
    (handleAnimationForBlur ?? (() => {}))();
    if (!displayValue) {
      placeholderTopSV.value = withSpring(styles.datePickerLabel.top, { stiffness, damping });
      placeholderFontSV.value = withSpring(styles.datePickerLabel.fontSize, { stiffness, damping });
    }
  };

  // Server/Acta-Nova-Wert -> Anzeige + Label-Animation
  React.useEffect(() => {
    const v = (formProps as any)?.value;
    if (v == null) {
      setDisplayValue('');
      placeholderTopSV.value = withSpring(styles.datePickerLabelActive.top, { stiffness, damping });
      placeholderFontSV.value = withSpring(styles.datePickerLabelActive.fontSize, { stiffness, damping });
      prevDisplayRef.current = '';
      hadValueOnOpenRef.current = false;
      return;
    }

    const SERVER_FMT = 'YYYY-MM-DD[T]HH:mm:ss';

    const mServer = moment(v, SERVER_FMT, true);
    const mIso = moment(v, moment.ISO_8601, true);
    const mTime = moment(v, 'HH:mm', true);
    const mDate = moment(v, 'DD.MM.YYYY', true);

    const m = mServer.isValid()
      ? mServer
      : mIso.isValid()
      ? mIso
      : mTime.isValid()
      ? mTime
      : mDate.isValid()
      ? mDate
      : null;

    if (m) {
      const disp =
        type === 'hourAndMinute'
          ? m.format('HH:mm')
          : type === 'date'
          ? m.format('DD.MM.YYYY')
          : m.format('DD.MM.YYYY HH:mm');

      setDisplayValue(disp);
      prevDisplayRef.current = disp;
      hadValueOnOpenRef.current = !!disp;

      placeholderTopSV.value = withSpring(styles.datePickerLabelActive.top, { stiffness, damping });
      placeholderFontSV.value = withSpring(styles.datePickerLabelActive.fontSize, { stiffness, damping });
    } else {
      const s = String(v);
      setDisplayValue(s);
      prevDisplayRef.current = s;
      hadValueOnOpenRef.current = !!s;

      if (s) {
        placeholderTopSV.value = withSpring(styles.datePickerLabelActive.top, { stiffness, damping });
        placeholderFontSV.value = withSpring(styles.datePickerLabelActive.fontSize, { stiffness, damping });
      } else {
        placeholderTopSV.value = withSpring(styles.datePickerLabel.top, { stiffness, damping });
        placeholderFontSV.value = withSpring(styles.datePickerLabel.fontSize, { stiffness, damping });
      }
    }
  }, [formProps.value, type]);

  // ---------------- ANDROID: Dialog-Picker (imperativ, Date → Time bei dateAndTime) ----------------
  if (Platform.OS === 'android') {
    const [opening, setOpening] = useState(false);

    const getCurrent = () =>
      moment.isMoment(dateValue)
        ? dateValue.toDate()
        : dateValue
        ? new Date(dateValue)
        : new Date();

    const handleDismiss = () => {
      setTimeout(() => setOpening(false), 0);
      setDisplayValue(prevDisplayRef.current ?? '');
      if (!hadValueOnOpenRef.current) runBlur();
    };

    const openTimeAfterDate = (baseDate: Date) => {
      const prev = getCurrent();
      const prefilled = new Date(baseDate);
      prefilled.setHours(prev.getHours(), prev.getMinutes(), 0, 0);

      DateTimePickerAndroid.open({
        value: prefilled,
        mode: 'time',
        is24Hour: true,
        display: 'default',
        onChange: (ev, selectedTime) => {
          if (ev.type !== 'set' || !selectedTime) {
            handleDismiss();
            return;
          }
          setTimeout(() => setOpening(false), 0);

          const merged = new Date(baseDate);
          merged.setHours(
            selectedTime.getHours(),
            selectedTime.getMinutes(),
            0,
            0,
          );

          const m = moment(merged);
          const disp = convertISOToHumanDisplay(m.toISOString());
          setDisplayValue(disp);
          onChange(m);
          handleDateChange(formProps, m, type === 'hourAndMinute', disp);
        },
      });
    };

    const openDateThenMaybeTime = () => {
      const current = getCurrent();
      DateTimePickerAndroid.open({
        value: current,
        mode: 'date',
        display: 'default',
        onChange: (ev, selectedDate) => {
          if (ev.type !== 'set' || !selectedDate) {
            handleDismiss();
            return;
          }
          if (type === 'date') {
            setTimeout(() => setOpening(false), 0);
            const m = moment(selectedDate);
            const disp = convertISOToHumanDisplay(m.toISOString());
            setDisplayValue(disp);
            onChange(m);
            handleDateChange(formProps, m, false, disp);
            return;
          }
          openTimeAfterDate(selectedDate);
        },
      });
    };

    const openOnlyTime = () => {
      const current = getCurrent();
      DateTimePickerAndroid.open({
        value: current,
        mode: 'time',
        is24Hour: true,
        display: 'default',
        onChange: (ev, selected) => {
          if (ev.type !== 'set' || !selected) {
            handleDismiss();
            return;
          }
          setTimeout(() => setOpening(false), 0);

          const merged = new Date(current);
          merged.setHours(selected.getHours(), selected.getMinutes(), 0, 0);

          const m = moment(merged);
          const disp = convertISOToHumanDisplay(m.toISOString());
          setDisplayValue(disp);
          onChange(m);
          handleDateChange(formProps, m, true, disp);
        },
      });
    };

    const openAndroidPicker = () => {
      console.log('openAndroidPicker');
      if (!isEditable || opening) return;

      prevDisplayRef.current = displayValue;
      hadValueOnOpenRef.current = !!displayValue;
      runFocus();
      setOpening(true);

      requestAnimationFrame(() => {
        if (type === 'hourAndMinute') {
          openOnlyTime();
        } else {
          openDateThenMaybeTime();
        }
      });
    };
    
    // Update ref so focus handler can call it
    openAndroidPickerRef.current = openAndroidPicker;

    return (
      <View
        ref={(node) => {
             containerRef.current = node;
             // We need to sync elementRef.current with the node AND keep the focus method
             if (node) {
                 (node as any).focus = () => {
                      if (Platform.OS === 'android') {
                          openAndroidPickerRef.current?.();
                      } else {
                          setOpen(true);
                      }
                 };
                 elementRef.current = node;
             }
        }}
      >
        <Animated.View
          pointerEvents="none"
          style={[styles.datePickerLabelContainer, localPlaceholderStyle]}
        >
          <Animated.Text
            style={[
              styles.datePickerLabel,
              localPlaceholderTextStyle,
              hasError && isEditable && styles.errorDatePickerLabel,
            ]}
          >
            {labelText}
          </Animated.Text>
        </Animated.View>

        <AndpInput
          accessible
          accessibilityRole="combobox"
          accessibilityLabel={t('form_accessibility_datepicker_input', {
            label: labelText,
          })}
          accessibilityHint={
            hasError
              ? t('form_accessibility_datepicker_input_error_hint', {
                  error: localErrMsg,
                })
              : t('form_accessibility_datepicker_input_hint', {
                  label: labelText,
                })
          }
          accessibilityState={{ disabled: !isEditable }}
          accessibilityValue={{ text: displayValue }}
          value={displayValue}
          showSoftInputOnFocus={false}
          caretHidden
          onPress={openAndroidPicker}
          editable={isEditable}
          style={[
            styles.datePickerInput,
            hasError && isEditable && styles.errorDatePickerInput,
          ]}
          inputStyle={[
            styles.datePickerInputText,
            Platform.OS === 'android' && {
              paddingHorizontal: 0,
              paddingLeft: 0,
              marginLeft: 0,
            },
          ]}
          rightElement={[
            displayValue && isEditable ? (
              <Pressable
                key="clear"
                onPress={() => {
                  setDisplayValue('');
                  onDelete();
                }}
              >
                <AndpIcon
                  name="close"
                  style={[
                    { pointerEvents: 'none' },
                    wrapperStyles.deleteContentBtnIcon,
                  ]}
                />
              </Pressable>
            ) : null,
            <AndpIcon
              key="arrow"
              name="arrowDown"
              style={wrapperStyles.dropdownIcon}
            />,
          ]}
        />
      </View>
    );
  }

  // ---------------- iOS: BottomSheet + Save/Cancel (Expo UI) ----------------
  // Dynamischer Import, damit Android die Module nie lädt
  const {
    BottomSheet,
    Button,
    DatePicker,
    Host,
    HStack,
    Spacer,
    VStack,
  } = require('@expo/ui/swift-ui');
  const { padding, datePickerStyle } = require('@expo/ui/swift-ui/modifiers');

  const openPicker = () => {
    prevDisplayRef.current = displayValue;
    hadValueOnOpenRef.current = !!displayValue;
    setOpen(true);
  };
  const closePicker = () => setOpen(false);

  const handleCancel = () => {
    setOpen(false);
    const prev = prevDisplayRef.current ?? '';
    setDisplayValue(prev);
    if (hadValueOnOpenRef.current) {
    } else {
      runBlur();
    }
  };

  const handleChange = (date: any) => {
    runFocus();
    const iso = date?.toISOString?.() ?? new Date(date).toISOString();
    setPickerDate(new Date(iso));
    setDisplayValue(convertISOToHumanDisplay(iso));
  };

  const handleSave = () => {
    closePicker();
    const m = moment(displayValue, DISPLAY_FORMAT[type], true);
    onChange(m);
    handleDateChange(formProps, m, type === 'hourAndMinute', displayValue);
  };

  return (
    <View>
      <Host style={{ position: 'absolute' }}>
        <BottomSheet
          isPresented={open}
          onIsPresentedChange={closePicker}
          fitToContents
        >
          <VStack>
            <HStack
              modifiers={[padding({ leading: 20, trailing: 20, top: 20 })]}
            >
              <Button label="Cancel" onPress={handleCancel} />
              <Spacer />
              <Button label="Save" onPress={handleSave} />
            </HStack>
            <DatePicker
              onDateChange={(date: Date) => handleChange(date)}
              displayedComponents={
                type === 'dateAndTime' ? ['date', 'hourAndMinute'] : [type]
              }
              selection={pickerDate}
              modifiers={[
                datePickerStyle(type === 'dateAndTime' ? 'graphical' : 'wheel'),
              ]}
            />
          </VStack>
        </BottomSheet>
      </Host>

      <View
        ref={(node) => {
             containerRef.current = node;
             if (node) {
                 (node as any).focus = () => {
                      setOpen(true);
                 };
                 elementRef.current = node;
             }
        }}
      >
        <Animated.Text
          style={[
            styles.datePickerLabel,
            localPlaceholderStyle,
            localPlaceholderTextStyle,
            hasError && isEditable && styles.errorDatePickerLabel,
          ]}
          // @ts-ignore
          pointerEvents="none"
        >
          {labelText}
        </Animated.Text>

        <AndpInput
          accessible
          accessibilityRole="combobox"
          accessibilityLabel={t('form_accessibility_datepicker_input', {
            label: labelText,
          })}
          accessibilityHint={
            hasError
              ? t('form_accessibility_datepicker_input_error_hint', {
                  error: localErrMsg,
                })
              : t('form_accessibility_datepicker_input_hint', {
                  label: labelText,
                })
          }
          accessibilityState={{ disabled: !isEditable }}
          accessibilityValue={{ text: displayValue }}
          value={displayValue}
          showSoftInputOnFocus={false}
          caretHidden
          onPress={openPicker}
          editable={isEditable}
          style={[
            styles.datePickerInput,
            hasError && isEditable && styles.errorDatePickerInput,
          ]}
          inputStyle={styles.datePickerInputText}
          rightElement={[
            displayValue && isEditable ? (
              <Pressable
                key="clear"
                onPress={() => {
                  setDisplayValue('');
                  onDelete();
                }}
                accessibilityRole="button"
                accessibilityLabel={t('form_accessibility_datepicker_clear')}
                accessibilityHint={t(
                  'form_accessibility_datepicker_clear_hint',
                )}
              >
                <AndpIcon
                  name="close"
                  style={[
                    { pointerEvents: 'none' },
                    wrapperStyles.deleteContentBtnIcon,
                  ]}
                />
              </Pressable>
            ) : null,
            <AndpIcon
              key="arrow"
              name="arrowDown"
              style={wrapperStyles.dropdownIcon}
            />,
          ]}
        />
      </View>
    </View>
  );
};

let Comp = DateTimeElementNative as unknown as WithStatics<
  typeof DateTimeElementNative
>;

// Falls du einen Wrapper verwendest (memo/forwardRef/etc.)
Comp = React.memo(DateTimeElementNative) as unknown as WithStatics<
  typeof DateTimeElementNative
>;

// Statics anhängen NACH dem Wrap:
Comp.validate = validate;
Comp.getSummaryValue = getSummaryValue;

export default Comp;
