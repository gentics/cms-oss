import React from 'react';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
} from '../../../types/interfaces';
import InputElement from '../shared/InputElement';
import * as validationUtils from '../../../utils/ValidationUtils';

/**
 * CmsUserMailElement renders exactly like the shared InputElement, but adds a
 * hardcoded e-mail format check on validation.
 *
 * On validate it first checks the value against a hardcoded e-mail format
 * (validationUtils.validateEmail) and only afterwards runs the regular
 * commonValidate — which includes the schema-configured regexValidation. This
 * guarantees that every usermail field always enforces a syntactically valid
 * e-mail format, independent of the schema configuration.
 */
function CmsUserMailElement(props: IFormComponentAttributes) {
  return <InputElement {...props} />;
}

CmsUserMailElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: any,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  // 1) hardcoded: zuerst auf gültige E-Mail prüfen
  const emailResult = validationUtils.validateEmail(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    localValue,
  );
  if (!emailResult.isValid) return emailResult;

  // 2) danach normale Validierung inkl. regexValidation
  return validationUtils.commonValidate(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    formGridOptions,
    localValue,
  );
};

CmsUserMailElement.getSummaryValue = InputElement.getSummaryValue;

export default CmsUserMailElement;
