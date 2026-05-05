import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  useWindowDimensions,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { FormElementWrapper } from './helper/FormElementWrapper';
import {
  IFormComponentAttributes,
  IFormGridOptions,
  IOverlayLeaveExtraProps,
} from '../../types/interfaces';
import { TFunction } from 'i18next';
import { FormState } from '../../types/interfaces';
import { getI18nText } from '../../utils/Utils';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';
import * as overlayUtils from '../../utils/OverlayUtils';
import { OverlayTypeEnum } from '../../custom/OverlayTypes';


function FormGridTextElement(props: IFormComponentAttributes) {
  const formGridOptions = props.formGridOptions;
  const { width } = useWindowDimensions();

  const value: string =
    formGridOptions?.value && props.formState
      ? getI18nText(props.formState, formGridOptions.value) || ''
      : '';

  const linkCallbackHook = (link: string) => {
    const overlayLeaveExtraProps: IOverlayLeaveExtraProps = { link: link };
    overlayUtils.setOverlay(
      props.uiState,
      props.formState,
      OverlayTypeEnum.ON_LEAVE,
      props.updateState,
      overlayLeaveExtraProps,
    );
    //props.uiState.linkCallback(link);
  }

  return (
    <FormElementWrapper
      formState={props.formState}
      numberOfColumns={formGridOptions?.numberOfColumns}
      formComponentAttributes={props}
      uiState={props.uiState}
      hideLabel={true}
      hideLoading={true}
      localErrorMsg=''
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      <CustomRenderHTML
        basePath={props.restService.initUrl}
        contentWidth={width}
        source={{ html: value }}
        defaultTextProps={{
          selectable: true,
        }}
        linkCallback={Platform.OS != "web" ? linkCallbackHook : undefined}
      ></CustomRenderHTML>
    </FormElementWrapper>
  );
}

FormGridTextElement.validate = () => {
  return { isValid: true };
};

FormGridTextElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  if (formGridOptions?.type === 'formgridSpacer') {
    return ' ';
  }
  if (formGridOptions?.valueSummary) {
    let value = getI18nText(state, formGridOptions.valueSummary) || '';
    return value;
  }
};

export default FormGridTextElement;