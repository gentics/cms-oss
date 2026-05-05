import React, { useEffect, useRef, useMemo } from 'react';
import { TFunction } from 'i18next';
import * as validationUtils from '../../../utils/ValidationUtils';
import {
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  CommonValidation,
} from '../../../types/interfaces';
import * as utils from '../../../utils/Utils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import {
  booleanElementStyles,
  dropdownCatalogElementStyles,
} from '../../../styles/common/common-style';
import {
  booleanElementStyles as customBooleanElementStyles,
  dropdownCatalogElementStyles as customDropdownCatalogElementStyles,
} from '../../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import SharedSelectRenderer from '../helper/SharedSelectRenderer';


/**
 * Wrapper for the shared select UI used by acta nova dropdown catalog elements.
 *
 * Rendering is delegated to `SharedSelectRenderer`. This component only keeps the
 * acta nova specific change handling, i.e. it forwards value updates through
 * `formEventsUtils.handleChange(...)` so the surrounding form logic, REST hooks,
 * dependent fields, and state updates continue to behave exactly as before.
 */
function ANDropDownCatalogElement(props: IFormComponentAttributes) {
  const { formState, uiState } = props;

  const elementRef = useRef<any>(null);
  // Merge default and custom dropdown styles once to keep the renderer call lean.
  const styles = useMemo(
    () =>
      mergeStyles(
        dropdownCatalogElementStyles,
        customDropdownCatalogElementStyles,
      ),
    [],
  );
  // Merge default and custom radio/checkbox styles once for the shared renderer.
  const booleanStyles = useMemo(
    () => mergeStyles(booleanElementStyles, customBooleanElementStyles),
    [],
  );

  // Register the element ref so validation can focus the first invalid field.
  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  const handleSelectionChange = (value: any) => {
    formEventsUtils.handleChange(
      formState,
      uiState,
      props.restService,
      props.jsonpath,
      props.id,
      false,
      props.updateState,
      value,
      props.formGridOptions,
      props.editable,
    );
  };

  return (
    <SharedSelectRenderer
      props={props}
      elementRef={elementRef}
      styles={styles}
      booleanStyles={booleanStyles}
      onSelectionChange={handleSelectionChange}
    />
  );
}

ANDropDownCatalogElement.validate = function (
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

ANDropDownCatalogElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  const value = utils.getValue(state, jsonpath, id);
  if (!value) return '';
  if (Array.isArray(value)) {
    return value.map((v: any) => v.displayName).join(', ');
  }
  return value.displayName || '';
};

export default ANDropDownCatalogElement;
