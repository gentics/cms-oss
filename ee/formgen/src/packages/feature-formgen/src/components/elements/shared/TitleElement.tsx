import {
  useWindowDimensions,
  Platform,
} from 'react-native';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import {
  IFormComponentAttributes,
  IFormGridOptions,
  IOverlayLeaveExtraProps,
} from '../../../types/interfaces';
import { FormState } from '../../../types/interfaces';
import { getI18nText } from '../../../utils/Utils';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';
import * as overlayUtils from '../../../utils/OverlayUtils';
import { OverlayTypeEnum } from '../../../custom/OverlayTypes';


function TitleElement(props: IFormComponentAttributes) {
  const formGridOptions = props.formGridOptions;
  const { width } = useWindowDimensions();

  const textVal = formGridOptions?.value || formGridOptions?.text;
  const value: string =
    textVal && props.formState
      ? getI18nText(props.formState, textVal) || ''
      : '';

  // Der Wert ist immer reiner Text (kein Rich Content), daher wird er
  // konsequent escaped und in ein <h2> gepackt - nie als Markup interpretiert.
  const escapeHtml = (text: string) =>
    text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  const html: string = value ? `<h2>${escapeHtml(value)}</h2>` : '';

  const linkCallbackHook = (link: string) => {
    const overlayLeaveExtraProps: IOverlayLeaveExtraProps = { link: link };
    overlayUtils.setOverlay(
      props.uiState,
      props.formState,
      OverlayTypeEnum.ON_LEAVE,
      props.updateState,
      overlayLeaveExtraProps,
    );
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
      selectedElementId={props.selectedElementId}
    >
      <CustomRenderHTML
        basePath={props.restService.initUrl}
        contentWidth={width}
        source={{ html }}
        defaultTextProps={{
          selectable: true,
        }}
        linkCallback={Platform.OS !== "web" ? linkCallbackHook : undefined}
        t={(key, fallback) => props.t(key, fallback || '')}
      />
    </FormElementWrapper>
  );
}

TitleElement.validate = () => {
  return { isValid: true };
};

TitleElement.getSummaryValue = function (
  state: FormState,
  formGridOptions?: IFormGridOptions,
) {
  if (formGridOptions?.valueSummary) {
    return getI18nText(state, formGridOptions.valueSummary) || '';
  }
};

export default TitleElement;
