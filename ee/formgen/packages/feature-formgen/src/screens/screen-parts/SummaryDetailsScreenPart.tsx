import { TFunction } from 'i18next';
import React, { useMemo } from 'react';
import { View, StyleSheet, Platform } from 'react-native';
import { FormState } from '../../types/interfaces';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { summaryStyles } from '../../styles/common/common-style';
import { summaryStyles as customSummaryStyles} from '../../styles/custom/custom-style';

interface SummaryDetailsProps {
  t: TFunction;
  formState: FormState;
  displayname?: string;
  email?: string;
  username?: string;
  userAgent?: string;
}

export default function SummaryDetailsScreenPart(props: SummaryDetailsProps) {
  const styles = useMemo(() => mergeStyles(summaryStyles, customSummaryStyles), []);;
  const t = props.t;
  const zone =
    new Date()
      .toLocaleTimeString('en-us', { timeZoneName: 'short' })
      .split(' ')[2] || '';

  // Werte werden nun aus props entnommen
  const { displayname, email, username, userAgent, formState } = props;

  return (
    <View style={styles.summaryDetailSection}>
      {/* User Info */}
      {displayname || email || username ? (
        <View style={styles.subSection}>
          <AndpText style={styles.subSectionHeader}>{t('form_print__user_header')}</AndpText>
          <View style={styles.detailDefinitionList}>
            {displayname && displayname.length > 0 ?
              <View style={styles.detailDefinitionRow}>
                <AndpText style={styles.detailDefinitionLabel}>
                  {t('form_print__user_display_name')}
                </AndpText>
                <AndpText style={styles.detailDefinitionValue}>{displayname}</AndpText>
              </View> : null}
            {email && email.length > 0 ?
              <View style={styles.detailDefinitionRow}>
                <AndpText style={styles.detailDefinitionLabel}>
                  {t('form_print__user_email')}
                </AndpText>
                <AndpText style={styles.detailDefinitionValue}>{email}</AndpText>
              </View>
              : null}
            {username && username.length > 0 ?
              <View style={styles.detailDefinitionRow}>
                <AndpText style={styles.detailDefinitionLabel}>
                  {t('form_print__user_name')}
                </AndpText>
                <AndpText style={styles.detailDefinitionValue}>{username}</AndpText>
              </View>
              : null}
          </View>
        </View>)
        : null}

      {/* Client Info */}
      <View style={styles.subSection}>
        <AndpText style={styles.subSectionHeader}>
          {t('form_print__client_info_heading')}
        </AndpText>
        <View style={styles.detailDefinitionList}>
          <View style={styles.detailDefinitionRow}>
            <AndpText style={styles.detailDefinitionLabel}>
              {t('form_print__client_info_timestamp')}
            </AndpText>
            <AndpText style={styles.detailDefinitionValue}>
              {new Date().toLocaleString()} {zone}
            </AndpText>
          </View>
          <View style={styles.detailDefinitionRow}>
            <AndpText style={styles.detailDefinitionLabel}>Client</AndpText>
            <AndpText style={styles.detailDefinitionValue}>
              {userAgent ?? Platform.OS}
            </AndpText>
          </View>
        </View>
      </View>

      {/* Form Info */}
      <View style={styles.subSection}>
        <AndpText style={styles.subSectionHeader}>
          {t('form_print__form_info_heading')}
        </AndpText>
        <View style={styles.definitionList}>
          <View style={styles.detailDefinitionRow}>
            <AndpText style={styles.detailDefinitionLabel}>
              {t('form_print__form_info_formid')}
            </AndpText>
            <AndpText style={styles.detailDefinitionValue}>{formState.data.formID}</AndpText>
          </View>
        </View>
      </View>
    </View>
  );
}