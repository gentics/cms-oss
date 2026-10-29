import React, { useMemo } from 'react';
import { View, ActivityIndicator } from 'react-native';
import { useTranslation } from 'react-i18next';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { formElementWrapperStyles as customFormElementWrapperStyles } from '../../../styles/custom/custom-style';
import { formElementWrapperStyles } from '../../../styles/common/common-style';
import { standardElement } from '@andp/core/src/styles/theme';

export function FormElementError(props: { error: string; id: string }) {
  const styles = useMemo(
    () => mergeStyles(formElementWrapperStyles, customFormElementWrapperStyles),
    [],
  );
  if (props.error) {
    return (
      <View style={styles.formFeedbackContainer}>
        <AndpIcon name="circleWarning" style={styles.errorIcon} />
        <AndpText style={styles.errorText}>{props.error}</AndpText>
      </View>
    );
  }
}

// Warning message
export function FormElementAnFieldLoadError({
  anFieldLoadError,
}: {
  anFieldLoadError: boolean;
}) {
  const { t } = useTranslation();
  const styles = useMemo(
    () => mergeStyles(formElementWrapperStyles, customFormElementWrapperStyles),
    [],
  );
  if (anFieldLoadError) {
    return (
      <View style={styles.formFeedbackContainer}>
        <AndpIcon name="warning" style={styles.actaNovaErrorIcon} />
        <AndpText style={styles.actaNovaErrorText}>
          {t('form_errorBackOfficeCall')}
        </AndpText>
      </View>
    );
  }
}

// Loading Circle
export function FormElementLoader({
  loading,
  color,
  size,
}: {
  loading: boolean;
  color?: string;
  size?: number;
}) {
  const styles = useMemo(
    () => mergeStyles(formElementWrapperStyles, customFormElementWrapperStyles),
    [],
  );
  if (loading) {
    return (
      <ActivityIndicator
        style={styles.elementLoader}
        color={standardElement.elementLoader.color}
        size={standardElement.elementLoader.size}
      />
    );
  }
}
