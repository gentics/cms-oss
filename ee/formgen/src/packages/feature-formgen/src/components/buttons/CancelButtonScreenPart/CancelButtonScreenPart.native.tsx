import React, { useMemo, useState } from 'react';
import { View, TouchableOpacity, Platform, Text } from 'react-native';
import { buttonStyles } from '../../../styles/common/common-style.native';
import { buttonStyles as customButtonStyles } from '../../../styles/custom/custom-style.native';

import { IFormPageComponent, IOverlayLeaveExtraProps } from '../../../types/interfaces';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { merge } from 'lodash';
import * as overlayUtils from '../../../utils/OverlayUtils';
import { OverlayTypeEnum } from '../../../custom/OverlayTypes';
import theme from '@andp/core/src/styles/theme.native';

const CancelButtonScreenPart: React.FC<IFormPageComponent> = (props) => {
  const uiState = props.uiState;
  const formState = props.formState;
  const breakPoints = useBreakpoints();
  const [hover, setHover] = useState<boolean>(false);
  const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), [])

  const handleCancelButtonClick = async () => {

    const overlayLeaveExtraProps: IOverlayLeaveExtraProps = { link: '' };
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.ON_LEAVE,
      props.updateState,
      overlayLeaveExtraProps,
    );


    // if (state.paymentActive && state.paymentState === 'PaymentFailed') {
    //   delete state.transactionId;
    //   delete state.paymentState;
    // } TODO NEW STATE
  };

  return (
    <TouchableOpacity
      onPress={handleCancelButtonClick}
      style={{
        width: 32,
        height: 32,
        backgroundColor: theme.colors.lightRed,
        borderRadius: 10,
        justifyContent: 'center',
        alignItems: 'center'
      }}
      activeOpacity={0.7}
    // disabled={
    //   (state.paymentActive &&
    //     state.paymentState === 'PaymentAuthorized') ||
    //   state.loading
    // } TODO NEW STATE 
    >
      <AndpIcon name='close' color={theme.colors.colorDeleteRed}></AndpIcon>
    </TouchableOpacity>

  );
};

export default CancelButtonScreenPart;
