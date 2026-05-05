import React, { useMemo } from 'react';
import { View,  StyleSheet, useWindowDimensions, Platform } from 'react-native';
import { ISummaryAttributes } from '../../../types/interfaces';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';
import { getI18nText } from '../../../utils/Utils';
import AndpText from '@andp/core/src/components/AndpText';
import { summaryStyles as customSummaryStyles } from '../../../styles/custom/custom-style';
import { summaryStyles } from '../../../styles/common/common-style';

import {
  mergeStyles,
  useBreakpoints,
  useHoverStyle,
  widthFromColumNumber,
} from '@andp/core/src/AndpUtils';

export function SummaryElement(props: ISummaryAttributes) {
  const { width } = useWindowDimensions();
  const styles = useMemo(() => mergeStyles(summaryStyles, customSummaryStyles), []);
  const labelI18n = props.label
      ? getI18nText(props.formState, props.label)
      : '';
  const labelI18nWithDoubleColon = labelI18n ? labelI18n + ': ' : '';
  const breakpoints = useBreakpoints();

  if (props.aggregat) {
    if (props.value != null && props.value != undefined && props.value !== '') {
      return (
        <View
          style={styles.summaryContainer}
          key={'summaryElement-agg-wrapper-' + props.label}
        >
          {labelI18nWithDoubleColon !== '' && (
            <AndpText style={[styles.labelText, styles.summaryHeader]}>
              {labelI18nWithDoubleColon}
            </AndpText>
          )}
          <View
            style={styles.aggregateItem}
            key={'summaryElement-agg-item-' + props.label}
          >
            {Array.isArray(props.value) ? (
              props.value.map((val, index) => (
                <View
                  key={index}
                  style={[
                    styles.summaryRow,
                    index % 2 === 0 ? styles.rowEven : styles.rowOdd,
                  ]}
                >
                  <View
                    key={index}
                    style={[
                      styles.summaryCell,
                      widthFromColumNumber(props.numberOfColumns, breakpoints),
                    ]}
                  >
                    {val}
                  </View>
                </View>
              ))
            ) : (
              <AndpText>{props.value ? props.value : '\u00A0'}</AndpText>
            )}
          </View>
        </View>
      );
    }
  } else {
    // Wenn es sich um ein FormGridElement handelt, das einen String enthält
    if (props.formGridElement === true && typeof props.value === 'string') {
      // Ursprünglich: <dd dangerouslySetInnerHTML={{ __html: props.value }}></dd>
      // In RN kein dangerouslySetInnerHTML; hier einfach Text anzeigen
      return (
        <View
          style={styles.elementWrapper}
          key={'summaryElement-wrapper-' + props.label}
        >
          <CustomRenderHTML
            basePath={props.initUrl}
            contentWidth={width}
            source={{ html: props.value }}
            t={props.t}
            linkCallback={
              Platform.OS != 'web' ? props.uiState.linkCallback : null
            }
          />
        </View>
      );
    } else {
      // Wenn dangerouslySetInnerHTML true ist, war ursprünglich HTML im Text
      if (
        props.dangerouslySetInnerHTML &&
        props.value &&
        typeof props.value === 'string'
      ) {
        return (
          <View
            style={styles.elementWrapper}
            key={'summaryElement-wrapper-' + props.label}
          >
            {labelI18n !== '' && (
              <AndpText style={styles.elementLabel}>{labelI18n}</AndpText>
            )}
            <CustomRenderHTML
              basePath={props.initUrl}
              contentWidth={width}
              source={{ html: props.value }}
              t={props.t}
              linkCallback={
                Platform.OS != 'web' ? props.uiState.linkCallback : null
              }
            />
          </View>
        );
      } else if (props.value) {
        return (
          <View
            style={styles.elementWrapper}
            key={'summaryElement-wrapper-' + props.label}
          >
            {labelI18n !== '' && (
              <AndpText style={styles.elementLabel}>{labelI18n}</AndpText>
            )}
            <AndpText style={styles.elementValue}>{props.value}</AndpText>
          </View>
        );
      }
    }
  }
}
