import React, { ReactNode, useMemo, useState, useRef } from 'react';
import {
  View,
  TouchableOpacity,
  Platform,
  useWindowDimensions,
} from 'react-native';
import {
  FormState,
  IFormComponentAttributes,
  ITranslationSet,
  UIState,
} from '../../../types/interfaces';
import { useTranslation } from 'react-i18next';
import {
  FormElementError,
  FormElementLoader,
  FormElementAnFieldLoadError,
} from './UtilComponents';
import { getI18nText } from '../../../utils/Utils';
import * as mockFormularUtils from '../../../utils/MockFormularUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import {
  formElementWrapperStyles,
  utilStyles,
} from '../../../styles/common/common-style';
import { formElementWrapperStyles as customFormElementWrapperStyles } from '../../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { formElementWrapperTheme } from '@andp/core/src/styles/theme';
import CustomTooltip from './CustomTooltip';
if (Platform.OS === 'web') {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  require('../../../styles/custom/custom-style.css');
  require('../../../styles/common/common-style.css');
}

export enum DeleteEntryType {
  Dropdown,
  DateTimePicker,
}

export interface FormElementWrapperProps {
  formState: FormState;
  uiState: UIState;
  localErrorMsg: string;
  description?: ITranslationSet;
  numberOfColumns?: number;
  additionalWrapperClass?: string;
  additionalClass?: string;
  children: ReactNode;
  formComponentAttributes?: IFormComponentAttributes;
  labelId?: string;
  hideLabel?: boolean;
  hideLoading?: boolean;
  needFloatingLabel?: boolean;
  showFilledIcon?: boolean;
  showDeleteContentBtn?: boolean;
  hideAnFieldLoadErrorIcon?: boolean;
  handleDeleteEntry?: (...args: any[]) => void;
  appMode?: string;
  mockForm?: boolean;
  needSpace?: boolean;
  isDropdown?: boolean;
  isSmallerWarning?: boolean;
  jsonPath: string;
  objectId: string;
}

export function FormElementWrapper(props: FormElementWrapperProps) {
  const { t } = useTranslation();
  const formComponentAttributes = props.formComponentAttributes;
  const description = props.description
    ? getI18nText(props.formState, props.description)
    : null;

  const { width } = useWindowDimensions();
  const [isHovering, setIsHovering] = useState(false);
  const reopenRequiresLeaveRef = useRef(false);

  const styles = useMemo(
    () => mergeStyles(formElementWrapperStyles, customFormElementWrapperStyles),
    [],
  );

  let loading = props.uiState.loading;
  let anFieldLoadError = formComponentAttributes?.ANFieldLoadError;

  const interactionProps = {
    onMouseEnter: () => {
      if (reopenRequiresLeaveRef.current) return;
      setIsHovering(true);
    },
    onMouseLeave: () => {
      reopenRequiresLeaveRef.current = false;
      setIsHovering(false);
    },
  };

  //Dev Mock ELement loading
  if (props.uiState.isMockForm) {
    loading = mockFormularUtils.fakeLoader(
      loading,
      formComponentAttributes?.id,
    );
    anFieldLoadError = mockFormularUtils.fakeANfieldLoadError(
      formComponentAttributes?.id,
    );
  }

  const hasIcons = !!(
    (loading && !props.hideLoading) ||
    props.showFilledIcon ||
    props.showDeleteContentBtn ||
    (!props.hideAnFieldLoadErrorIcon && anFieldLoadError)
  );

  const fieldWithBasicTopComponents = (): React.ReactNode => {
    if (formComponentAttributes !== undefined) {
      return (
        <>
          {formComponentAttributes.required && (
            <View style={utilStyles.visualyHidden}>
              <AndpText>{t('form_screenreader_mandatory_field')}</AndpText>
            </View>
          )}
          {hasIcons && (
            <View
              style={[
                styles.inputItemsRight,
                props.isSmallerWarning && styles.checkboxRightIcons,
              ]}
            >
              {!props.hideLoading && (
                <FormElementLoader
                  loading={loading}
                  color={styles.formElementWrapperLoader.color}
                  size={styles.formElementWrapperLoader.height}
                />
              )}
              {props.showFilledIcon && (
                <AndpIcon
                  name="checkMark"
                  color={formElementWrapperTheme.validationInputIcon.color}
                  style={styles.validInputIcon}
                />
              )}
              {props.showDeleteContentBtn && (
                <TouchableOpacity
                  id={`${props.jsonPath}-delete-content-btn`}
                  onPress={(e) =>
                    props.handleDeleteEntry && props.handleDeleteEntry(e)
                  }
                  accessibilityLabel={
                    formComponentAttributes?.label
                      ? `${getI18nText(
                        props.formState,
                        formComponentAttributes.label,
                      )} - ${t('form_delete_entry')}`
                      : t('form_delete_entry')
                  }
                >
                  <AndpIcon
                    name="close"
                    color={formElementWrapperTheme.deleteContentBtnIcon.color}
                    style={styles.deleteContentBtnIcon}
                  />
                </TouchableOpacity>
              )}
              {!props.hideAnFieldLoadErrorIcon && anFieldLoadError && (
                <AndpIcon
                  name="warning"
                  color={formElementWrapperTheme.anFieldLoadErrorIcon.color}
                  style={styles.anFieldLoadErrorIcon}
                />
              )}
              {props.needSpace && <View style={styles.needSpacing}></View>}
            </View>
          )}
        </>
      );
    }
    return null;
  };

  const hasError = !!(
    anFieldLoadError ||
    (props.localErrorMsg && props.localErrorMsg.length > 0)
  );
  const fieldWithBasicBottomComponents = (): React.ReactNode => {
    if (formComponentAttributes !== undefined && hasError) {
      return (
        <View style={styles.bottomMessages}>
          {anFieldLoadError && (
            <FormElementAnFieldLoadError anFieldLoadError={anFieldLoadError} />
          )}
          {props.localErrorMsg ? (
            <FormElementError
              error={
                props.localErrorMsg != undefined && props.localErrorMsg != null
                  ? props.localErrorMsg
                  : formComponentAttributes.error
              }
              id={`${formComponentAttributes.id}_errorMessage`}
            />
          ) : null}
        </View>
      );
    }
  };

  const showTooltipIcon = (): boolean => {
    if (description) {
      return description.length > 0;
    }
    return false;
  };

  return (
    <View style={styles.wrapper}>
      <View style={styles.formGroup}>
        {fieldWithBasicTopComponents()}
        {Platform.OS === 'web' ? (
          <div
            id={props.jsonPath}
            className={`${props.objectId} ${camelToKebab(
              props.formComponentAttributes?.elemTypeString,
            )}-element ${props.formComponentAttributes?.editable ? 'editable' : 'disabled'}`}
          >
            {props.children}
          </div>
        ) : (
          props.children
        )}
        {fieldWithBasicBottomComponents()}
      </View>
      {showTooltipIcon() && (
        <View style={styles.tooltipContainer}><CustomTooltip
          isVisible={isHovering}
          placement={width < 770 ? 'left' : 'bottom'}
          onClose={() => setIsHovering(false)}
          content={description!}
          contentStyle={{
            ...styles.toolTipContent,
            minWidth: description!.length > 25 ? 250 : 0,
          }}
          tooltipStyle={styles.toolTip}
          caretUpStyle={styles.caretUp}
          caretRightStyle={styles.caretRight}
        >
          {Platform.OS === "web" ?
            <TouchableOpacity
              accessible
              accessibilityRole="button"
              accessibilityLabel={t('form_accessibility_tooltip_button', {
                label: description!,
              })}
              accessibilityHint={t('form_accessibility_tooltip_button_hint')}
              accessibilityState={{ expanded: isHovering }}
              {...interactionProps}
              style={[styles.tooltipButton, isHovering && styles.tooltipButtonActive]}
            >
              <AndpIcon
                name="info"
                style={[styles.tooltipIcon, isHovering && styles.tooltipIconActive]}
              />
            </TouchableOpacity> : <AndpIcon
              name="info"
              style={[styles.tooltipIcon, isHovering && styles.tooltipIconActive]}
            />
          }
        </CustomTooltip></View>
      )}
    </View>
  );
}

function camelToKebab(str: string | undefined): string | undefined {
  return str
    ? str.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()
    : undefined;
}
