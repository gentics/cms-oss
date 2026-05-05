import { Linking, Platform, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { ICommonOverlayProps, IOverlayExtraCommonProps } from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import { overlayPreConditionStyles } from '../../../styles/common/common-style';
import { overlayPreConditionStyles as customOverlayPreConditionStyles } from '../../../styles/custom/custom-style';
import AndpText from '@andp/core/src/components/AndpText';
import { useMemo } from 'react';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';

const PreconditionOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const styles = useMemo(() => mergeStyles(overlayPreConditionStyles, customOverlayPreConditionStyles), []);
  const t = props.formState.t;
  const { width } = useWindowDimensions();
  const preconditions = props.formState.data.preconditions;
  const additionalProps: IOverlayExtraCommonProps = props.uiState.overlayState
          .additionalProps as IOverlayExtraCommonProps;
  let preconditionListContentElement: React.ReactNode = null;
  if (preconditions && preconditions.length > 0) {
    preconditionListContentElement = (
      <View style={styles.bodyAlign}>
        {/* <AndpText style={styles.preconditionFirstLineText}>{t('form_preconditions_overlay__first_line')}</AndpText> */}
        <CustomRenderHTML baseStyle={styles.preconditionFirstLineText} basePath={props.restService.initUrl} contentWidth={width} source={{ html: t('form_preconditions_overlay__first_line') }} linkCallback={Platform.OS != "web" ? props.uiState.linkCallback : null}/>
        {preconditions.map((precondition, index) => {
          return (
            <View
              key={`preconditionMsg-${index}`}
              style={styles.preconditionItem}
            >
              {precondition.formID ? (
                <TouchableOpacity
                  onPress={() => {
                    if (Platform.OS == 'web') {
                      Linking.openURL(
                        '/form/' + encodeURI(precondition.formID!),
                      );
                    } else {
                      props.initializeForm(props.uiState, props.formState, precondition.formID);
                    }
                  }}
                >
                  <AndpText style={styles.preconditionItemLink}>
                    {precondition.message}
                  </AndpText>
                </TouchableOpacity>
              ) : (
                 <AndpText>{precondition.message}</AndpText>
              )}
            </View>
          );
        })}
        {/* <AndpText style={styles.preconditionSecondLineText}>{t('form_preconditions_overlay__second_line')}</AndpText> */}
        <CustomRenderHTML baseStyle={styles.preconditionFirstLineText} basePath={props.restService.initUrl} contentWidth={width} source={{ html: t('form_preconditions_overlay__second_line') }} linkCallback={Platform.OS != "web" ? props.uiState.linkCallback : null}/>
      </View>
    );

    return (
      <DefaultOverlay
        {...props}
        title={t('form_preconditionsNotMet')}
        contentElementNodes={preconditionListContentElement}
        buttonList={[
          {
            key: 'precondition-overlay-leave-btn',
            title: t('form_preconditions_overlay__leave_button'),
            onPress: async () =>
              await props.handleFormCancel?.(formEventsUtils.cancelRedirect),
          },
        ]}
        //footerMessages={[]}
        debugInfo={additionalProps ? additionalProps.response : undefined}
      />
    );
  }
  return <></>;
};

export default PreconditionOverlay;
