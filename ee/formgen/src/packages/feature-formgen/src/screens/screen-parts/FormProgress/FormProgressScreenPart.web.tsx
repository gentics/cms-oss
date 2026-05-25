import React, { useMemo, useState } from 'react';
import {
  View,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { pagingStyles } from '../../../styles/common/common-style';
import { pagingStyles as customPagingStyles } from '../../../styles/custom/custom-style';
import {
  FormState,
  IErrorMessage,
  IFormPage,
  UIState,
} from '../../../types/interfaces';
import * as utils from '../../../utils/Utils';
import { cancelRedirect } from '../../../utils/FormEventsUtils';
import { RestService } from '../../../services/RestService';
import { StatesToUpdateEnum } from '../../../constants/enums/StateToUpdateEnum';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { widthFromColumNumber } from '@andp/core/src/AndpUtils';
import { formElementWrapperStyles } from '../../../styles/common/common-style';
import CustomTooltip from '../../../components/elements/helper/CustomTooltip';
import DialogFormBackButton from '../../../components/buttons/DialogFormBackButton';

const FormProgressScreenPart: React.FC<{
  handlePageChange: (pageNumber: number) => void;
  errorMsgs: IErrorMessage[];
  currentPage: number;
  formState: FormState;
  uiState: UIState;
  restService: RestService;
  updateState: (state: StatesToUpdateEnum) => void;
  handleFormCancel?: (
    callback: any,
    isOnlyCloseDialog?: boolean,
  ) => Promise<void>;
}> = ({
  handlePageChange,
  errorMsgs,
  currentPage,
  formState,
  uiState,
  restService,
  updateState,
  handleFormCancel,
}) => {
  const { t } = useTranslation();
  const formPagesLength = utils.getFormPagesLengthForDisplay(formState);
  const isDialogFormSubmit = uiState.dialogForm.isDialogFormOpen;
  const breakPoints = useBreakpoints();
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const styles = useMemo(() => mergeStyles(pagingStyles, customPagingStyles), []);

  if (isDialogFormSubmit) {
    return (
      <View
        style={[styles.dialogFormHeader,
          widthFromColumNumber(formState.form.formwidth || 8, breakPoints),
        ]}
      >
        <DialogFormBackButton onPress={() => handleFormCancel?.(cancelRedirect, true)} formState={formState} uiState={uiState} updateState={updateState} t={t} ></DialogFormBackButton>
      </View>
    );
  } else if (formPagesLength !== -1) {
    if (breakPoints.maximumMd) {
      const currentPageText = currentPage + 1;
      const maxPageText = formPagesLength;
      const pageName =
        utils.getI18nText(
          formState,
          formState.form?.pages![currentPage].pagename,
        ) || '';

      return (
        <View
          style={styles.formPagingContainerScreenMaximumMedium}
          accessible={true}
          accessibilityRole="header"
          accessibilityLabel={t('progressBanner', {
            currentPage: currentPageText,
            maxPage: maxPageText,
            pageName: pageName,
          })}
        >
          <View style={styles.pageLabelContainerMaximumMedium}>
            <AndpText style={styles.pageLabelScreenMaximumMedium}>
              {currentPageText} / {maxPageText}
            </AndpText>
          </View>
          <AndpText style={styles.pageTitleScreenMaxiumMedium}>
            {pageName}
          </AndpText>
        </View>
      );
    } else {
      // Desktop-Ansicht: Ursprüngliche Darstellung mit ScrollView und Tooltip
      const formProgressElements = [];

      for (let i = 0; i < formPagesLength; i++) {
        if (formState.form.pages?.[i]) {
          const formpage: IFormPage = formState.form.pages[i];
          const pageNumber = i;
          const allowNavigation = formState.isFormgridPreview || currentPage >= pageNumber;

          let formpageName = '';
          let tooltipTitle = '';

          // Bei der Zusammenfassungsseite
          if (i === formPagesLength) {
            formpageName = t('form_summary');
          } else {
            formpageName = utils.getI18nText(formState, formpage.pagename);
          }
          tooltipTitle = `${t('form_step')} ${pageNumber + 1} - ${formpageName}`;

          const hasError = errorMsgs.some(
            (msg) => msg.pageNumber === pageNumber,
          );
          const isCurrentPage = currentPage === pageNumber;
          const isDisabled = currentPage < pageNumber;

          const hoverProps = Platform.OS === 'web'
            ? {
                onMouseEnter: () => setHoveredIndex(i),
                onMouseLeave: () => setHoveredIndex(null),
              }
            : {};

          const pageItemStyle = [
            styles.pageItem,
            isCurrentPage ? styles.currentPageItem : {},
            allowNavigation ? {} : styles.disabledPageItem,
            hasError ? styles.hasErrorPageItem : {},
          ];
          formProgressElements.push(
            <CustomTooltip
              isVisible={hoveredIndex === i}
              content={<AndpText style={formElementWrapperStyles.tooltipText}>{tooltipTitle}</AndpText>}
              placement="bottom"
              onClose={() => setHoveredIndex(null)}
              contentStyle={{
                ...formElementWrapperStyles.toolTipContent,
                minWidth: tooltipTitle.length > 25 ? 230 : 0,
              }} 
              tooltipStyle={formElementWrapperStyles.toolTip}
              caretUpStyle={formElementWrapperStyles.caretUp}
              caretRightStyle={formElementWrapperStyles.caretRight}
              key={"pagination-tooltip-" + i}
            >
              <View {...hoverProps} key={"pagination-element-index-" + i}>
                <TouchableOpacity
                  id={'pagination-element-' + i}
                  key={i}
                  accessible
                  accessibilityRole="button"
                  accessibilityLabel={t('form_accessibility_tooltip_button', {
                    label: 'description',
                  })}
                  accessibilityHint={t(
                    'form_accessibility_tooltip_button_hint',
                  )}
                  accessibilityState={{ expanded: hoveredIndex === i }}
                  onPress={() =>
                    allowNavigation ? handlePageChange(pageNumber) : null
                  }
                  disabled={!allowNavigation}
                  style={pageItemStyle}
                  {...hoverProps}
                >
                  <AndpText
                    style={[
                      styles.number,
                      isCurrentPage ? styles.currentNumber : {},
                      isDisabled ? styles.disabledNumber : {},
                      hasError ? styles.hasErrorNumber : {},
                    ]}
                  >
                    {pageNumber + 1}
                  </AndpText>
                </TouchableOpacity>
              </View>
            </CustomTooltip>,
          );
        }
      }
      return (
        <View
          style={[
            styles.formPagingContainer,
            widthFromColumNumber(formState.form.formwidth || 8, breakPoints),
          ]}
        >
          <View style={styles.pagination} key={"formprogress-pagination"}>
            {formProgressElements}
          </View>
          <View style={styles.pageHeaderContainer}>
            <AndpText style={styles.pageLabel}>
              {t('form_step')} {currentPage + 1}
            </AndpText>
            <AndpText style={styles.pageHeaderHyphenText}>-</AndpText>
            <AndpText style={styles.pageTitle}>
              {utils.getI18nText(
                formState,
                formState.form?.pages![currentPage].pagename,
              ) || ''}
            </AndpText>
          </View>
        </View>
      );
    }
  } else {
    // ExpertSearch => formPagesLength ist -1
    return <></>;
  }
};

export default FormProgressScreenPart;
