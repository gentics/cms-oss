import React, { useEffect, useMemo, useState } from 'react';
import { View } from 'react-native';
import { errorBoxStyles } from '../../styles/common/common-style';
import { errorBoxStyles as customErrorBoxStyles } from '../../styles/custom/custom-style';
import { FormState, IErrorMessage, UIState } from '../../types/interfaces';
import { useTranslation } from 'react-i18next';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints, widthFromColumNumber } from '@andp/core/src/AndpUtils';
import { errorBoxTheme } from '@andp/core/src/styles/theme';
import { CaptchaService } from '../../services/CaptchaService';


const ErrorBlockScreenPart: React.FC<{
  formState: FormState;
  uiState: UIState;
  errorMsgs: IErrorMessage[];
}> = (props) => {
  const formState: FormState = props.formState;
  const compareFormNotFilledErrorMessage = 'form_notFilledError';
  const breakpoints = useBreakpoints();
  let addedOnceFormNotFilled = false;
    const { t } = useTranslation();
  let errorMessages: IErrorMessage[] = props.errorMsgs;
  const styles = useMemo(() => mergeStyles(errorBoxStyles, customErrorBoxStyles), []);

  const [captchaHasError, setCaptchaHasError] = useState<boolean>(
    CaptchaService.hasError(),
  );
  useEffect(() => {
    return CaptchaService.subscribe(() => {
      setCaptchaHasError(CaptchaService.hasError());
    });
  }, []);

  if (errorMessages.length > 0 || captchaHasError) {
    // filter for validation messages for the current or previous pages
    // only show one "form not filled" validation message
    const errorMessagesFromPreviousOrCurrentPage = errorMessages.filter(
      (item) => {
        if (item.pageNumber <= props.uiState.currentPage) {
          if (item.validationMessage === compareFormNotFilledErrorMessage) {
            if (!addedOnceFormNotFilled) {
              addedOnceFormNotFilled = true;
              return true;
            } else {
              //if already added, filter entry
              return false;
            }
          }
          return true;
        }
        return false;
      },
    );
    if (errorMessagesFromPreviousOrCurrentPage.length || captchaHasError) {
      return (
        <View style={[styles.errorBoxContainer]}>
          <View
            style={styles.errorBoxDescriptionContainer}
          >
            <View
              style={[styles.errorBoxFormWidthContainer, 
                widthFromColumNumber(formState.form.formwidth || 8, breakpoints) ]}
            >
              <AndpText style={styles.errorBoxDescription}>
                {t("form_errorBlock_title")}
              </AndpText>
            </View>
          </View>
          <View style={styles.errorBoxListContainer}>
            <View
              style={[styles.errorBoxFormWidthContainer, 
                widthFromColumNumber(formState.form.formwidth || 8, breakpoints) ]}
            >
              <View style={styles.errorBoxList}>
                {errorMessagesFromPreviousOrCurrentPage.map((value, index) => {
                  if (value.pageNumber <= props.uiState.currentPage) {
                    // Split error message by line breaks, each message should have its own break
                    let splittedError: string[] =
                      value.validationMessage.split('\r\n');
                    return splittedError.map((splitError, subIndex) => (
                      <View
                        style={styles.errorBoxItem}
                        key={`${index}-${subIndex}`}
                      >
                        <AndpIcon name="circleWarning" 
                          size={errorBoxTheme.listContainer.iconSize} 
                          color={errorBoxTheme.listContainer.fontColor} />
                        <AndpText style={styles.errorText}>
                          {splitError}{' '}
                        </AndpText>
                      </View>
                    ));
                  }
                })}
                {captchaHasError && (
                  <View style={styles.errorBoxItem} key="captcha-error">
                    <AndpIcon name="circleWarning"
                      size={errorBoxTheme.listContainer.iconSize}
                      color={errorBoxTheme.listContainer.fontColor} />
                    <AndpText style={styles.errorText}>
                      {t('form_captchaNotSolvedError')}{' '}
                    </AndpText>
                  </View>
                )}
              </View>
            </View>
          </View>
        </View>
      );
    }
    return null;
  }
};

export default ErrorBlockScreenPart;
