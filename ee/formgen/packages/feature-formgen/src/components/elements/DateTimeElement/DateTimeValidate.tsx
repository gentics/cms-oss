import moment from 'moment';
import * as utils from '../../../utils/Utils';
import * as validationUtils from '../../../utils/ValidationUtils';
import { FormState, IFormGridOptions } from '../../../types/interfaces';
import { TFunction } from 'i18next';

export function validate(
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
) {
    
  const commonValidate = validationUtils.commonValidate(
    state, jsonpath, id, updateErrorMessages,
  );

  if (commonValidate.isValid) {
    const value = utils.getValue(state, jsonpath);
    const elemState = utils.getElementByJsonPath(state.data, jsonpath);
 
    if (value != null && value !== '' && elemState != null) {
      const dateValidation = moment(value, moment.ISO_8601, true).isValid();
      const timeValidation = moment(value, 'HH:mm', true).isValid();
 
      if (!timeValidation && !dateValidation) {
        commonValidate.isValid = false;
        if (updateErrorMessages)
          elemState.validationMessage = t('form_notADateTime');
        utils.setStateElementByJsonpath(state.data, jsonpath, elemState);
      }
    }
  }
  return commonValidate;
}


function convertISOToHumanDate(value: string) {
  const date = moment(value);
  return date.isValid() ? date.format('DD.MM.YYYY') : '';
}
 
function convertISOToHumanDateTime(value: string) {
  const date = moment(value);
  return date.isValid() ? date.format('DD.MM.YYYY HH:mm') : '';
}

function convertISOToHumanTime(value: string) {
  const date = moment(value);
  return date.isValid() ? date.format('HH:mm') : '';
}

export function getSummaryValue(
  formState: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  const value = utils.getValue(formState, jsonpath, id);
  if (!value) return '';
  const schemaState = utils.getSchemaForElement(formState.schema, id);
  
  // Time
  if (schemaState?.type === 'time') {
      return convertISOToHumanTime(value);
  }
  // Date
  if (schemaState?.dateOnly) {
    return convertISOToHumanDate(value);
  }
  // Date + Time
  return convertISOToHumanDateTime(value);
}