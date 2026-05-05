import React, { useEffect, useMemo, useState } from 'react';
import {
  ICommonOverlayProps,
  IOverlayFullScreenExtraProps,
  OverlayState,
} from '../../../types/interfaces';
import { Modal } from 'react-native';
import { LoadingScreen } from '@andp/core/src/components/LoadingComponents';
import { fullScreenOverlayStyles } from '../../../styles/common/common-style';
import { fullScreenOverlayStyles as customFullScreenOverlayStyles } from '../../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';

const FullScreenOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const styles = useMemo(() => mergeStyles(fullScreenOverlayStyles, customFullScreenOverlayStyles), []);
  const overlayState: OverlayState = props.uiState.overlayState as OverlayState;
  const additionalProps: IOverlayFullScreenExtraProps =
    overlayState.additionalProps as IOverlayFullScreenExtraProps;
  const t = props.formState.t;
  const text = additionalProps?.loadingText
    ? t(additionalProps.loadingText)
    : '';

  return (
    <Modal
      style={styles.loadingOverlay}>
      <LoadingScreen text={text}/>
    </Modal>
  );
};

export default FullScreenOverlay;
