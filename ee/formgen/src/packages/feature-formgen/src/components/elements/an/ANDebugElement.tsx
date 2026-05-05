import React from 'react';
import { View, TextInput } from 'react-native';
import { TFunction } from 'i18next';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,

} from '../../../types/interfaces';
import * as utils from '../../../utils/Utils';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import AndpText from '@andp/core/src/components/AndpText';

/**
 * DebugElement
 */
function ANDebugElement(props: IFormComponentAttributes) {
  const itemId = props.id;
  const type = props.type;
  const formState = props.formState;
  const isInputAllowed = props.type === 'string' || props.type === 'number';

  return (
    <FormElementWrapper
      formState={formState}
      uiState={props.uiState}
      formComponentAttributes={props}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      localErrorMsg={props.error}
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      <View>
        <AndpText style={{ fontSize: 18, fontWeight: 'bold' }}>Type: {type}</AndpText>
        <AndpText>ID: {itemId}</AndpText>
        <AndpText>Name: {utils.getI18nText(formState, props.label)}</AndpText>
        {isInputAllowed ? (
          <TextInput
            value={props.value}
            placeholder={`Enter a ${type}`}
            style={{
              borderColor: 'gray',
              borderWidth: 1,
              padding: 8,
              marginTop: 10,
            }}
            keyboardType={props.type === 'number' ? 'numeric' : 'default'}
          />
        ) : (
          <></>
        )}
      </View>
    </FormElementWrapper>
  );
}

ANDebugElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  return {isValid: true};
};

ANDebugElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string, 
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  return 'Debug';
};

export default ANDebugElement;
