import React, { useMemo, useEffect, useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  LayoutAnimation,
  Animated,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { pagingStyles } from '../../../styles/common/common-style.native';
import { pagingStyles as customPagingStyles } from '../../../styles/custom/custom-style.native';
import {
  FormState,
  IErrorMessage,
  UIState,
  IFormFlowClass,
} from '../../../types/interfaces';
import * as utils from '../../../utils/Utils';
import { cancelRedirect } from '../../../utils/FormEventsUtils';
import { RestService } from '../../../services/RestService';
import { StatesToUpdateEnum } from '../../../constants/enums/StateToUpdateEnum';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { widthFromColumNumber } from '@andp/core/src/AndpUtils';
import AndpButton from '@andp/core/src/components/AndpButton';
import AndpText from '@andp/core/src/components/AndpText';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import theme from '@andp/core/src/styles/theme.native';
import ProgressBar from './NativeHeaderParts/ProgressBar.native';
import FormHeader from './NativeHeaderParts/FormHeader.native';

const MobileErrorList: React.FC<{
  errorMsgs: IErrorMessage[];
}> = ({ errorMsgs }) => {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState<boolean>(false);
  const [fadeAnim] = useState(new Animated.Value(0));

  useEffect(() => {
    if (!errorMsgs || errorMsgs.length === 0) return;

    Animated.timing(fadeAnim, {
      toValue: 1,
      duration: 250,
      useNativeDriver: true,
    }).start();
  }, [errorMsgs, fadeAnim]);

  if (!errorMsgs || errorMsgs.length === 0) {
    return null;
  }

  const handleToggle = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setExpanded((prev) => !prev);
  };

  return (
    <Animated.View style={[localStyles.errorContainer, { opacity: fadeAnim }]}>
      <TouchableOpacity
        activeOpacity={0.9}
        onPress={handleToggle}
        style={localStyles.errorHeaderRow}
      >
        <View style={localStyles.errorHeaderLeft}>
          <AndpIcon
            name="warning"
            size={18}
            color="#DC2626"
          />
          <AndpText style={localStyles.errorHeaderText}>
            {t('form_errorBlock_title')}
          </AndpText>
        </View>
        <View style={localStyles.errorHeaderRight}>
          <View style={localStyles.errorCountPill}>
            <AndpText style={localStyles.errorCountText}>
              {errorMsgs.length}
            </AndpText>
          </View>
          <AndpText style={localStyles.toggleIcon}>
            {expanded ? <AndpIcon name="arrowUp" color={localStyles.toggleIcon.color} /> : <AndpIcon name="arrowDown" color={localStyles.toggleIcon.color} />}
          </AndpText>
        </View>
      </TouchableOpacity>

      {expanded && (
        <View style={localStyles.errorList}>
          {errorMsgs.map((error, idx) => {
            const lines = error.validationMessage
              ? error.validationMessage.split('\r\n')
              : [''];

            return (
              <View key={idx} style={localStyles.errorItem}>
                <AndpIcon
                  name="circleWarning"
                  size={14}
                  color="#DC2626"
                  style={localStyles.errorItemIcon}
                />
                <View style={localStyles.errorItemTextWrapper}>
                  {lines.map((line, lineIdx) => (
                    <AndpText
                      key={`${idx}-${lineIdx}`}
                      style={localStyles.errorText}
                      numberOfLines={2}
                      ellipsizeMode="tail"
                    >
                      {line}
                    </AndpText>
                  ))}
                </View>
              </View>
            );
          })}
        </View>
      )}
    </Animated.View>
  );
};

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
    const isExpertSearch = uiState.isExpertSearch;
    const formPagesLength = utils.getFormPagesLengthForDisplay(formState);
    const isDialogFormSubmit = uiState.dialogForm.isDialogFormOpen;
    const breakPoints = useBreakpoints();
    const styles = useMemo(() => mergeStyles(pagingStyles, customPagingStyles), []);

    type ProgressItem = {
      kind: 'form' | 'flow';
      index: number;
      label: string;
      pageNumber?: number;
    };

    if (isDialogFormSubmit) {
      return (
        <View
          style={[
            styles.formPagingContainer,
            widthFromColumNumber(8, breakPoints),
            { position: 'sticky', top: 0, zIndex: 1000, backgroundColor: 'white' },
          ]}
        >
          <AndpButton
            onPress={() => handleFormCancel?.(cancelRedirect, true)}
            type={'secondary'}
            title={t('form_dialog_back_to_main_form_label')}
          />
        </View>
      );
    } else if (isExpertSearch || uiState.isReadOnlyForm) {
      const formTitle =
        formState.form.formname ? utils.getI18nText(formState, formState.form.formname) : '';

      return (
        <View
          style={[
            styles.formPagingContainer,
            widthFromColumNumber(formState.form.formwidth || 8, breakPoints),
            { paddingBottom: 10, borderBottomColor: theme.border.color.darkGray, borderBottomWidth: theme.border.width.regular },
            { position: 'sticky', top: 0, zIndex: 1000, backgroundColor: 'white' },
          ]}
        >
          <FormHeader
            formTitle={formTitle}
            styles={styles}
            t={t}
            uiState={uiState}
            formState={formState}
            restService={restService}
            updateState={updateState}
            handleFormCancel={handleFormCancel}
          />
          <MobileErrorList errorMsgs={errorMsgs} />
        </View>
      );
    } else if (formPagesLength !== -1) {
      // ---------- Kopfzeile: Cancel (links), Formname (mitte), DetailInfo (rechts) ----------

      // ---------- Kombinierte Progress-Liste: Form-Seiten + Flow-Schritte (ohne FormScreen) ----------
      const pages = formState.form.pages || [];
      const flowList: IFormFlowClass[] = uiState.flowList || [];
      const extraFlowSteps = flowList.filter(
        (s) => s.reactClass && s.reactClass !== 'FormScreen'
      );

      const formItems: ProgressItem[] = pages.map((p, i) => ({
        kind: 'form',
        index: i,
        label: utils.getI18nText(formState, p.pagename) || '',
        pageNumber: i,
      }));

      const flowItems: ProgressItem[] = extraFlowSteps.map((s, i) => ({
        kind: 'flow',
        index: pages.length + i,
        label: s.nameTranslationKey ? t(s.nameTranslationKey) : '',
      }));

      const items: ProgressItem[] = [...formItems, ...flowItems];

      // Aktuelle Position: entweder innerhalb der Form-Seiten (FormScreen) oder auf einem Flow-Schritt (z. B. Summary/Success)
      const currentFlow = flowList[uiState.flowPageIndex];
      const currentCombinedIndex =
        currentFlow && currentFlow.reactClass !== 'FormScreen'
          ? pages.length +
          Math.max(
            0,
            extraFlowSteps.findIndex(
              (s) => s.reactClass === currentFlow.reactClass
            )
          )
          : currentPage;


      const formTitle =
        formState.form.formname ? utils.getI18nText(formState, formState.form.formname) : '';

      return (
        <View
          style={styles.formPagingContainer}
        >
          <FormHeader
            formTitle={formTitle}
            styles={styles}
            t={t}
            uiState={uiState}
            formState={formState}
            restService={restService}
            updateState={updateState}
            handleFormCancel={handleFormCancel}
          />
          <ProgressBar
            items={items}
            currentCombinedIndex={currentCombinedIndex}
            errorMsgs={errorMsgs}
            handlePageChange={handlePageChange}
          />
          <MobileErrorList errorMsgs={errorMsgs} />
        </View>
      );
    } else {
      return <></>;
    }
  };


const localStyles = StyleSheet.create({
  errorContainer: {
    marginTop: 8,
    marginBottom: 4,
    marginHorizontal: 5,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 10,
    backgroundColor: '#FEF2F2',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#FCA5A5',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  errorHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  errorHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 1,
    paddingRight: 8,
  },
  errorHeaderText: {
    marginLeft: 8,
    fontWeight: '600',
    fontSize: 14,
    color: '#B91C1C',
  },
  errorHeaderRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  errorCountPill: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
    backgroundColor: 'rgba(248,113,113,0.18)',
    marginRight: 6,
  },
  errorCountText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#B91C1C',
  },
  toggleIcon: {
    fontSize: 12,
    color: '#EF4444',
  },
  errorList: {
    marginTop: 8,
  },
  errorItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingVertical: 4,
  },
  errorItemIcon: {
    marginRight: 6,
    marginTop: 2,
  },
  errorItemTextWrapper: {
    flex: 1,
  },
  errorText: {
    fontSize: 13,
    lineHeight: 18,
    color: '#991B1B',
  },
});

export default FormProgressScreenPart;