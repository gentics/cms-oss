import React, { useMemo, useState } from 'react';
import { TouchableOpacity, Platform, View } from 'react-native';
import { buttonStyles } from '../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../styles/custom/custom-style';
import { FormState, UIState } from '../../types/interfaces';
import { TFunction } from 'i18next';
import * as uiUtils from '../../utils/UiUtils';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { useButtons } from '../../screens/essentials/ButtonProvider';

interface PreviousPageButtonProps {
  formState: FormState;
  uiState: UIState;
  restService: any;
  updateState: (state: any) => void;
  mockForm?: boolean;
  t: TFunction;
  screenType: string;
  translateKey: string;
}

/**
 * PreviousPageButtonScreenPart
 * 
 * Diese Komponente zeigt den Button zum Navigieren zur vorherigen Seite an.
 * Die Anzeige erfolgt nur, wenn bestimmte Bedingungen erfüllt sind, die an den alten React‑Code angelehnt sind.
 */
const PreviousPageButtonScreenPart: React.FC<PreviousPageButtonProps> = ({
  formState,
  uiState,
  restService,
  updateState,
  mockForm,
  t,
  screenType,
  translateKey,
}) => {
  const { getButtonTranslationKey } = useButtons();
  const shouldShow =
    (uiState.flowPageIndex !== 0 && uiState.currentPage >= 0) ||
    uiState.currentPage !== 0;
  const [hover, setHover] = useState<boolean>(false);
  const breakPoints = useBreakpoints();
  const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), []);

  // PreHook und PostHook – können hier erweitert werden, z. B. für Analytics oder weitere Logik
  const preHook = () => {
    console.log('Button-preHook executed');
  };

  const postHook = () => {
    console.log('Button-postHook executed');
  };


  /**
   * handlePreviousPageButtonClick
   * 
   * Prüft zunächst Payment-Status:
   * - Bei PaymentAuthorized wird (analog zum Web) versucht, den "payed"-Parameter zu entfernen und neu zu laden.
   *   Da in React Native kein window-Objekt existiert, erfolgt hier momentan nur ein Log.
   * - Bei PaymentFailed werden transaktionsbezogene Daten aus dem formState entfernt.
   * Anschließend wird über die übergebenen Hooks die vorherige Seite asynchron gewechselt.
   */
  const handlePreviousPageButtonClick = async () => {
    preHook();
    // if (formState.paymentState === PaymentStateTypes.PaymentAuthorized) {
    //   // Im Web wurde hier entweder der URL-Parameter "payed" entfernt oder die Seite neu geladen.
    //   // In React Native gibt es kein window. Hier kann z. B. ein Reload über ein natives Modul erfolgen.
    //   console.log('Payment authorized – bitte App neu laden oder Navigation zurücksetzen.');
    //   postHook();
    //   return;
    // } else {
    //   if (formState.paymentState === PaymentStateTypes.PaymentFailed) {
    //     delete formState.transactionId;
    //     delete formState.paymentState;
    //   }
    if(!uiState.loading){
      if (screenType === 'FormScreen' && uiState.currentPage !== 0) {
        await uiUtils.handleHooksChangePreviousForm(formState, uiState, restService, updateState, mockForm);
      } else {
        await uiUtils.handleHooksChangePreviousFormFlow(uiState, updateState);
      }
    }
    // }
    postHook();
  };



  if (!shouldShow) {
    return null;
  }

  return (
    <View style={[styles.btnWrapper,
    breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
    ]}>
      <TouchableOpacity
        id={"sticky-bar-previous-page-btn"}
        onPress={handlePreviousPageButtonClick}
        disabled={uiState.loading}
        style={
          !uiState.loading ? 
          [
            styles.btn,
            styles.btnSecondary,
            hover && styles.btnSecondaryHover,
            breakPoints.maximumXl && styles.smallBtn,
          ]
          : 
          [
            styles.btn,
            styles.btnSecondaryDisabled,
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
          style={[styles.btnText,
          styles.btnSecondaryText,
          hover && styles.btnSecondaryTextHover,
          breakPoints.maximumMd && styles.smallText
          ]}
        >{t(getButtonTranslationKey(translateKey))}</AndpText>
      </TouchableOpacity>
    </View>
  );
};

export default PreviousPageButtonScreenPart;