import { useEffect, useMemo, useState } from 'react';
import moment, { Moment } from 'moment';
import { IFormComponentAttributes, FormState } from '../../../../types/interfaces';
import * as utils from '../../../../utils/Utils';
import * as formEventsUtils from '../../../../utils/FormEventsUtils';
import { StatesToUpdateEnum } from '../../../../constants/enums/StateToUpdateEnum';
import { useSharedValue, withSpring } from 'react-native-reanimated';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { dateTimeElementStyles } from '../../../../styles/common/common-style';
import { dateTimeElementStyles as customDateTimeElementStyles} from '../../../../styles/custom/custom-style';

export type SharedState = ReturnType<typeof useDateTimeState>;

export function useDateTimeState(props: IFormComponentAttributes) {
  const schema = utils.getSchemaForElement(props.formState.schema, props.id);
  const isTimeField: boolean = schema?.type === 'time';
  const formState: FormState = props.formState;
  const styles = useMemo(
    () => mergeStyles(dateTimeElementStyles, customDateTimeElementStyles),
    [],
  );


  const labelText =
    utils.getI18nText(formState, props.label) + (props.required ? ' *' : '');

  const formattedPropsValue =
    props.value &&
    (moment(props.value, moment.ISO_8601, true).isValid() ||
      moment(props.value, 'DD.MM.YYYY', true).isValid() ||
      moment(props.value, 'DD.MM.YYYY HH:mm', true).isValid() ||
      moment(props.value, 'HH:mm', true).isValid())
      ? isTimeField
        // Try multiple formats to robustly handle values that might already be localized (e.g. from dialog return)
        // or strict ISO strings. correct "14.01.2026 13:56" -> "13:56"
        ? moment(props.value, [
            moment.ISO_8601,
            'HH:mm',
            'DD.MM.YYYY HH:mm',
          ]).format('HH:mm')
        : schema?.dateOnly
          ? moment(props.value, [
              moment.ISO_8601,
              'DD.MM.YYYY',
              'DD.MM.YYYY HH:mm',
            ]).format('DD.MM.YYYY')
          : moment(props.value, [
              moment.ISO_8601,
              'DD.MM.YYYY HH:mm',
            ]).format('DD.MM.YYYY HH:mm')
      : '';

  const [localErrMsg, setLocalErrMsg] = useState(props.error);
  const [dateValue, setDateValue] = useState<any>(
    props?.value ? parseDateValue(props.value) : '',
  );

  useEffect(() => {
    if (props.error !== localErrMsg) {
      setLocalErrMsg(props.error);
      props.updateState(StatesToUpdateEnum.ALL);
    }
  }, [props.error]);

  useEffect(() => {
    if (props?.value && props.value !== dateValue?.toString()) {
      setDateValue(parseDateValue(props.value));
    }
  }, [props?.value]);

  // Reanimated Placeholder (plattformunabhängig nutzbar)
  const placeholderTop = useSharedValue(props.value ? styles.datePickerLabelActive.top : styles.datePickerLabel.top);
  const placeholderFontSize = useSharedValue(props.value ? styles.datePickerLabelActive.fontSize : styles.datePickerLabel.fontSize);

  const animCfg = { stiffness: 1800, damping: 150 };

  const handleAnimationForBlur = () => {
    placeholderTop.value = withSpring(styles.datePickerLabel.top, animCfg);
    placeholderFontSize.value = withSpring(styles.datePickerLabel.fontSize, animCfg);
  };

  const handleAnimationForFocus = () => {
    placeholderTop.value = withSpring(styles.datePickerLabelActive.top, animCfg);
    placeholderFontSize.value = withSpring(styles.datePickerLabelActive.fontSize, animCfg);
  };

  const onChangeHelper = (val: string | Moment) => {
    if (!val || (typeof val === 'string' && val.trim() === '')) {
      setDateValue('');
      handleDateChange(props, '', isTimeField);
      return;
    }
    const m = moment.isMoment(val) ? val : moment(val);
    setDateValue(m);
  };

  const deleteValue = () => {
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
    );
    handleAnimationForBlur();
    setDateValue('');
  };

  return {
    schema,
    isTimeField,
    labelText,
    localErrMsg,
    setLocalErrMsg,
    dateValue,
    setDateValue,
    formattedPropsValue,
    placeholderTop,
    placeholderFontSize,
    handleAnimationForBlur,
    handleAnimationForFocus,
    onChangeHelper,
    deleteValue,
  };
}

// web

export function parseDateValue(value: string): moment.Moment | string {
  if (!value) return '';
  const formats = [
    moment.ISO_8601,
    'DD.MM.YYYY',
    'DD.MM.YYYY HH:mm',
    'HH:mm',
  ];
  const parsed = moment(value, formats, true);
  return parsed.isValid() ? parsed : value;
}

export function handleDateChange(
  props: IFormComponentAttributes,
  date: Moment | string,
  isTimeField: boolean,
  inputValue?: string,
) {
  let finalValue: string | Moment = '';
  if (moment.isMoment(date) && date.isValid()) {
    finalValue = isTimeField ? date.format('HH:mm') : date.toISOString();
  } else {
    const m = moment(inputValue, moment.ISO_8601, false);
    finalValue = m;
  }
  if (inputValue !== '') {
    formEventsUtils.handleChange(
      props.formState,
      props.uiState,
      props.restService,
      props.jsonpath,
      props.id,
      false,
      props.updateState,
      finalValue,
      props.formGridOptions,
      props.editable
    );
  }
}