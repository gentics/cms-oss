import React, { useMemo, useState } from 'react';
import { View, TouchableOpacity, Platform } from 'react-native';
import { buttonStyles } from '../../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../../styles/custom/custom-style';

import { IFormPageComponent, IOverlayLeaveExtraProps } from '../../../types/interfaces';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { merge } from 'lodash';
import * as overlayUtils from '../../../utils/OverlayUtils';
import { OverlayTypeEnum } from '../../../custom/OverlayTypes';

const CancelButtonScreenPart: React.FC<IFormPageComponent> = (props) => {
  const uiState = props.uiState;
  const formState = props.formState;
  const breakPoints = useBreakpoints();
  const [hover, setHover] = useState<boolean>(false);
  const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), [])

  const handleCancelButtonClick = async () => {
    if (Platform.OS == "web") {
      const isOnlyCloseDialog = props.uiState.dialogForm.isDialogFormOpen;
      await props.handleFormCancel?.(() => formEventsUtils.cancelRedirect(uiState), isOnlyCloseDialog);
    } else {
      const overlayLeaveExtraProps: IOverlayLeaveExtraProps = { link: '' };
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.ON_LEAVE,
        props.updateState,
        overlayLeaveExtraProps,
      );
    }

    // if (state.paymentActive && state.paymentState === 'PaymentFailed') {
    //   delete state.transactionId;
    //   delete state.paymentState;
    // } TODO NEW STATE
  };

  return (
    <View style={[styles.btnWrapper,
    breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
    ]}>
      <TouchableOpacity
        id={"sticky-bar-cancel-btn"}
        onPress={handleCancelButtonClick}
        style={[styles.btn,
        styles.btnSecondary,
        hover && styles.btnSecondaryHover,
        breakPoints.maximumXl && styles.smallBtn,
        ]}
        activeOpacity={0.7}
        {...(Platform.OS === "web" && {
          onMouseEnter: () => setHover(true),
          onMouseLeave: () => setHover(false),
        })}
      >
        <AndpText
          selectable={false}
          numberOfLines={1}
          lineBreakMode='head'
          ellipsizeMode='tail'
          style={[styles.btnText,
          styles.btnSecondaryText,
          hover && styles.btnSecondaryTextHover,
          breakPoints.maximumMd && styles.smallText
          ]}
        >
          {props.t(props.translateKey!)}
        </AndpText>
      </TouchableOpacity>
    </View>
  );
};

export default CancelButtonScreenPart;
