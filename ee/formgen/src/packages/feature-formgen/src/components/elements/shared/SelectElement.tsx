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
import {
  booleanElementStyles,
  dropdownCatalogElementStyles,
} from '../../../styles/common/common-style.native';
import {
  booleanElementStyles as customBooleanElementStyles,
  dropdownCatalogElementStyles as customDropdownCatalogElementStyles,
} from '../../../styles/custom/custom-style.native';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { StatesToUpdateEnum } from '../../../constants/enums/StateToUpdateEnum';
import SharedSelectRenderer from '../helper/SharedSelectRenderer';

/**
 * Wrapper for the shared select UI used by the generic select element.
 *
 * Rendering is delegated to `SharedSelectRenderer`. This component only keeps the
 * local state update behavior, i.e. it writes the selected value into `formState`
 * via `utils.setValue(...)` and then triggers `updateState(...)` so the regular
 * form rendering and validation flow continues unchanged.
 */
function SelectElement(props: IFormComponentAttributes) {
  const { formState } = props;

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

  // Keep the original local update flow when the shared renderer emits a new value.
  const handleSelectionChange = (value: any) => {
    utils.setValue(formState, props.jsonpath, value);
    props.updateState(StatesToUpdateEnum.FormState);
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

SelectElement.validate = function (
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

SelectElement.getSummaryValue = function (
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

export default SelectElement;
