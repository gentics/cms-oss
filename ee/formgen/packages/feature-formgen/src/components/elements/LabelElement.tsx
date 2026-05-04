import React from 'react';
import { Platform, View, useWindowDimensions } from 'react-native';
import { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
  IOverlayLeaveExtraProps
} from '../../types/interfaces';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';
import * as utils from '../../utils/Utils';
import * as overlayUtils from '../../utils/OverlayUtils';
import { OverlayTypeEnum } from '../../custom/OverlayTypes';

/**
 * LabelElement
 */
function LabelElement(props: IFormComponentAttributes) {
  const { width } = useWindowDimensions();
  const lng = props.formState.culture;
  const { t } = useTranslation();
  const htmlValueContent =
    props.formGridOptions?.value && props.formGridOptions.value[lng]
      ? props.formGridOptions.value[lng]
      : t('form_label_element_value_not_available');

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
    <View>
      {/* RenderHTML-Komponente */}
      <CustomRenderHTML basePath={props.restService.initUrl} contentWidth={width} source={{ html: htmlValueContent }} linkCallback={Platform.OS != "web" ? linkCallbackHook : undefined} />
    </View>
  );
}

LabelElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  return { isValid: true};
};

LabelElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  const value = utils.getValue(state, jsonpath, id);
  return value;
};


export default LabelElement;
