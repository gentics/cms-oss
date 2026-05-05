import React, { useEffect, useMemo, useState } from 'react';
import { TouchableOpacity, ActivityIndicator, View, Platform } from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import {
  IFormPageComponent,
} from '../../types/interfaces';
import * as formEventsUtils from '../../utils/FormEventsUtils';
import * as uiUtils from '../../utils/UiUtils';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import AndpText from '@andp/core/src/components/AndpText';
import { useButtons } from '../../screens/essentials/ButtonProvider';

const SubmitButtonScreenPart: React.FC<IFormPageComponent> = (props) => {
  const { getButtonTranslationKey } = useButtons();
  const screenType = props.screenType;
  if (screenType && screenType == 'FormScreen' && props.uiState.currentPage != props.uiState.lastPage - 1) return null;
  const uiState = props.uiState;
  const formState = props.formState;
  const currentPage = props.uiState.currentPage;
  const lastPage = props.uiState.lastPage;
  const breakPoints = useBreakpoints();
  const [hover, setHover] = useState<boolean>(false);
  const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), []);
  //const isDialogFormSubmit = useSelector((state: RootState) => state.form.dialogForm.isDialogFormOpen); && currentPage === lastPage - 1
  const isDialogFormSubmit = props.uiState.dialogForm.isDialogFormOpen;
  const isExternalSubmitButton = formState.schema.formType === "redirectForm" && uiState.currentPage === uiState.lastPage - 1;

  // useEffect(() => {
  //   if (
  //     (screenType && screenType != 'FormScreen') ||
  //     (currentPage == lastPage - 1 && screenType == 'FormScreen')
  //   ) {
  //     setBtnVisible(true);
  //   } else {
  //     setBtnVisible(false);
  //   }
  // }, [uiState]);


  return (
    <View style={[styles.btnWrapper,
    breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
    ]}>
      <TouchableOpacity
        id={"sticky-bar-submit-btn"}
        disabled={uiState.loading}
        onPress={async () => {
          if (isExternalSubmitButton) {
            formEventsUtils.handleExternalSubmit(formState, uiState, props.updateState, props.restService, props.mockForm, props.handleFormCancel);
          } else if (isDialogFormSubmit) {
            await props.handleSubmitDialogForm();
          } else {
            formEventsUtils.handleSubmitForm(
              props.formState,
              props.uiState,
              props.updateState,
              props.restService,
              props.mockForm,
              () => uiUtils.handleHooksChangeNextFlowPage(uiState, props.updateState),
            );
          }
        }}
        style={[styles.btn,
        styles.btnPrimary,
        hover && styles.btnPrimaryHover,
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
          ellipsizeMode='tail'
          style={[styles.btnText,
          styles.btnPrimaryText,
          hover && styles.btnPrimaryTextHover,
          breakPoints.maximumMd && styles.smallText
          ]}
        >
          {!uiState.loading && isExternalSubmitButton ? formState.data.formControls?.button?.name : !uiState.loading ?
            props.t(
              isDialogFormSubmit
                ? getButtonTranslationKey('form_dialog_save_button_label')
                : getButtonTranslationKey('form_submit'),
            ) :
            <ActivityIndicator color={'white'}></ActivityIndicator>
          }
        </AndpText>
      </TouchableOpacity>
    </View>
  );

};

export default SubmitButtonScreenPart;
