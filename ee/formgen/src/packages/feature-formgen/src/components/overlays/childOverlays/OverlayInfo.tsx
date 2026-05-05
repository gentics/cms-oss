import {
  ICommonOverlayProps,
  IOverlayInfoExtraProps,
  OverlayButtonDefinition,
  UIState,
} from '../../../types/interfaces';
import * as utils from '../../../utils/Utils';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';
import { View } from 'react-native';
import * as overlayUtils from '../../../utils/OverlayUtils';
import DefaultOverlay from '../DefaultOverlay';
import { ReactNode } from 'react';
import AndpText from '@andp/core/src/components/AndpText';

const OverlayInfo: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const uiState: UIState = props.uiState;
  const extraProps: IOverlayInfoExtraProps = uiState.overlayState
    .additionalProps as IOverlayInfoExtraProps;
  const overlayInfoOptions = extraProps.overlayInfoOptions;
  const texts = overlayInfoOptions?.texts;
  const value = overlayInfoOptions?.value;
  const t = props.formState.t;
  const closeAble = false;
  const culture = props.formState.culture;
  // keep global overlay close function and add given function
  const onClose = async () => {
    await extraProps.handleCancel();
    overlayUtils.hideOverlay(uiState, props.updateState);
  };

  let buttonList: OverlayButtonDefinition[] | undefined = [
    {
      key: 'info-overlay-cancel-btn',
      title: t('form_cancel'),
      onPress: async () => await onClose(),
    },
    {
      key: 'info-overlay-submit-btn',
      title: t('form_submit'),
      onPress: () => overlayUtils.hideOverlay(uiState, props.updateState),
    },
  ];

  const contentElementNodes: ReactNode = (
    <>
      <CustomRenderHTML
        basePath={props.restService.initUrl}
        source={{
          html: utils.getI18nTextStr(culture, texts?.textPre),
        }}
      />
      <View>
        <AndpText>{value}</AndpText>
      </View>
      <CustomRenderHTML
        basePath={props.restService.initUrl}
        source={{
          html: utils.getI18nTextStr(culture, texts?.textPost),
        }}
      />
    </>
  );
  return (
    <DefaultOverlay
      {...props}
      title={utils.getI18nTextStr(culture, texts?.title)}
      contentElementNodes={contentElementNodes}
      onClose={onClose}
      closeAble={closeAble}
      buttonList={buttonList}
      debugInfo={extraProps ? extraProps.response : undefined}
    />
  );
};

export default OverlayInfo;
