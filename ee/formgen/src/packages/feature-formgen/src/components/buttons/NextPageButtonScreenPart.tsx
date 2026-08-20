import React, { useState, useEffect, useMemo } from 'react';
import { TouchableOpacity, ActivityIndicator, View, Platform } from 'react-native';
import { IFormPageComponent } from '../../types/interfaces';
import * as uiUtils from '../../utils/UiUtils';
import AndpText from '@andp/core/src/components/AndpText';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { buttonsTheme } from '@andp/core/src/styles/theme';
import { useButtons } from '../../screens/essentials/ButtonProvider';
import { useFormMirrorStore } from '../../state/useFormMirrorState';

const NextPageButtonScreenPart: React.FC<IFormPageComponent> = (
  props: IFormPageComponent,
) => {
  
  const { getButtonTranslationKey } = useButtons();
  const [btnNextVisible, setBtnNextVisible] = useState(false);
  const t = props.t;
  const screenType = props.screenType;
  const currentPage = props.uiState.currentPage;
  const lastPage = props.uiState.lastPage;
  const formState = props.formState;
  const uiState = props.uiState;
  const breakPoints = useBreakpoints();
  const [hover, setHover] = useState<boolean>(false);
  const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), []);

  useEffect(() => {
    let existingSubmitBtn = false;
    if (props.uiState.flowList.length > 0) {
      let formFlowObj = props.uiState.flowList[props.uiState.flowPageIndex];
      formFlowObj.buttons.map((x) => {
        if (x.reactClass.toString() == 'SubmitButton') {
          existingSubmitBtn = true;
        }
      });
    }

    if (
      (screenType && screenType != 'FormScreen' && !existingSubmitBtn) ||
      (currentPage != lastPage - 1 && screenType == 'FormScreen') ||
      (currentPage == lastPage - 1 &&
        screenType == 'FormScreen' &&
        !existingSubmitBtn)
    ) {
      setBtnNextVisible(true);
    } else {
      setBtnNextVisible(false);
    }
  }, [props.uiState]);


  const handleNextPage = async () => {
    const {
      formState: mirroredFormState,
      uiState: mirroredUIState,
    } = useFormMirrorStore.getState();
    // Prefer global mirror; fall back to props if mirror is not yet initialised
    const effectiveFormState = mirroredFormState ?? formState;
    const effectiveUIState = mirroredUIState ?? uiState;
    let disabled = props.uiState.loading;
    if (!disabled){
      if (screenType && screenType == 'FormScreen') {
        await uiUtils.handleHooksChangeNextForm(
          effectiveFormState,
          effectiveUIState,
          props.restService,
          props.updateState,
          props.mockForm
        );
      } else {
        await uiUtils.handleHooksChangeNextFlowPage(effectiveUIState, props.updateState);
      }
    }
  };

  const CorrectTranslateKey = () => {
    if (props.uiState.flowList.length > 0 && props.uiState.currentPage == props.uiState.lastPage - 1) {
      let formFlowObj = props.uiState.flowList[props.uiState.flowPageIndex + 1];
      if (formFlowObj && formFlowObj != null && formFlowObj != undefined && formFlowObj.reactClass != "SuccessScreen") {
        return t(getButtonTranslationKey(formFlowObj.buttonLabelTranslationKey));
      }
    }
    return t(props.translateKey ? getButtonTranslationKey(props.translateKey) : getButtonTranslationKey('form_nextPage'));
  }

  if (btnNextVisible) {
    let disabled = props.uiState.loading;
    return (
      <View style={[styles.btnWrapper,
      breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
      ]}>
        <TouchableOpacity
          id={"sticky-bar-next-page-btn"}
          onPress={handleNextPage}
          style={
            !disabled ? 
            [
              styles.btn,
              styles.btnPrimary,
              hover && styles.btnPrimaryHover,
              breakPoints.maximumXl && styles.smallBtn,
            ] 
            : 
            [
              styles.btn,
              styles.btnPrimaryDisabled,
              breakPoints.maximumXl && styles.smallBtn,
            ]
          }
          activeOpacity={0.7}
          {...(Platform.OS === "web" && {
            onMouseEnter: () => setHover(true),
            onMouseLeave: () => setHover(false),
          })}
        >
          <AndpText
            selectable={false}
            numberOfLines={2}
            ellipsizeMode='tail'
            style={
              !disabled ? 
              [
                styles.btnText,
                styles.btnPrimaryText,
                hover && styles.btnPrimaryTextHover,
                breakPoints.maximumMd && styles.smallText
              ]
              :
              [
                styles.btnText,
                styles.btnPrimaryDisabledText,
                breakPoints.maximumMd && styles.smallText
              ]
            }
          >
            {!disabled ?
              CorrectTranslateKey() :
              <ActivityIndicator  color={buttonsTheme.primaryDisabled.fontColor}></ActivityIndicator>
            }
          </AndpText>
        </TouchableOpacity>
      </View>
    );
  } else {
    return null;
  }
};

export default NextPageButtonScreenPart;
