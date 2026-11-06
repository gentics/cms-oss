import React, { useRef, useEffect } from 'react';
import { ScrollView, View, Modal, Animated, Platform } from 'react-native';
import OverlayFooter from '../OverlayFooter';
import { defaultOverlayStyles, overlayHeaderStyles } from '../../../styles/common/common-style';
import AndpText from '@andp/core/src/components/AndpText';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { useBreakpoints } from '@andp/core/src/AndpUtils';
import theme, { overlaysTheme } from '@andp/core/src/styles/theme';

export interface FormgenScreenErrorOverlay {
  title?: string;
  contentMessages?: string[];
  contentElementNodes?: React.ReactNode;
  footerMessages?: string[];
  closeAble?: boolean;
  buttonObjects?: any[];
  onClose?: () => void;
}

const FormgenScreenErrorOverlay: React.FC<FormgenScreenErrorOverlay> = (
  props: FormgenScreenErrorOverlay,
) => {
  let title = props.title;
  let contentMsgs: string[] | undefined = props.contentMessages;
  // Initialize fade animation
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const breakPoints = useBreakpoints();

  useEffect(() => {
    Animated.timing(fadeAnim, {
      toValue: 1,
      duration: 300,
      useNativeDriver: Platform.OS !== 'web' ? true : false,
    }).start();
  }, [fadeAnim]);

  return <Modal animationType="fade" transparent={true}>
    <View style={defaultOverlayStyles.modal}>
      <View style={[defaultOverlayStyles.modalContent,
      !props.footerMessages && defaultOverlayStyles.modalContentWithoutFooter,
      breakPoints.maximumLg && defaultOverlayStyles.modalContentScreenMaxLarge,
      breakPoints.maximumMd && defaultOverlayStyles.modalContentScreenMaxMedium,
      breakPoints.maximumSm && defaultOverlayStyles.modalContentScreenMaxSmall]}>
        <View style={overlayHeaderStyles.headerContent}>
          <AndpIcon
            name='info'
            color={overlaysTheme.header.iconColor}
            size={overlaysTheme.header.iconSize} />
          <AndpText style={overlayHeaderStyles.headerTitle}>{title}</AndpText>
        </View>
        <View style={defaultOverlayStyles.body}>
          <ScrollView style={defaultOverlayStyles.bodyScrollView}>
            {contentMsgs &&
              contentMsgs.map((text, index) => (
                <AndpText
                  style={defaultOverlayStyles.bodyText}
                  key={`overlayContentMsgs-${index}`}
                >
                  {text}
                </AndpText>
              ))}
            {props.contentElementNodes && props.contentElementNodes}
          </ScrollView>
        </View>
        <OverlayFooter footerMessages={props.footerMessages} />
      </View>
    </View>
  </Modal>
};

export default FormgenScreenErrorOverlay;
