import { TFunction } from 'i18next';
import React from 'react';
import {
  FormState,
  IFormGridOptions,
  SummaryOptions,
} from '../../../types/interfaces';
import { getI18nText, getValue } from '../../../utils/Utils';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import { SummaryElement } from '../helper/SummaryElement';

/**
 * TextElement
 */
function TextElement(props: any) {
  const summary = props.summary;
  const value = getI18nText(props.formState, props.formGridOptions?.text || props.value) || '';
  const localErrMsg = props.error;

  if (summary === SummaryOptions.SummaryText) {
    return (
      <SummaryElement
        formState={props.formState}
        t={props.t}
        uiState={props.uiState}
        summaryOption={props.summary}
        label={props.label}
        value={TextElement.getSummaryValue(props.formState, props.jsonpath, props.id, props.t)}
        initUrl={props.restService.initUrl}
      />
    );
  } else {
    return (
      <FormElementWrapper
        formState={props.formState}
        uiState={props.uiState}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
        hideLoading={true}
        localErrorMsg={localErrMsg}
        jsonPath={props.jsonpath}
        objectId={props.id}
        selectedElementId={props.selectedElementId}
      >
        {/* TODO v8.2 should not only be html ?  */}
        <div
          className="text"
          tabIndex={0}
          dangerouslySetInnerHTML={{ __html: value ?? '' }}
        />
      </FormElementWrapper>
    );
  }
}

TextElement.validate = (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) => {
  return { isValid: true };
};

TextElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  const value = getValue(state, jsonpath, id);
  return value;
};

export default TextElement;
