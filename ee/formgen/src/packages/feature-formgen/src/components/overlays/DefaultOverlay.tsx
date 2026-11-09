import React, { useRef, useEffect, useMemo } from 'react';
import {
  ScrollView,
  Text,
  View,
  Modal,
  Animated,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { IDefaultOverlayProps, OverlayButtonDefinition } from '../../types/interfaces';
import OverlayHeader from './OverlayHeader';
import OverlayFooter from './OverlayFooter';
import OverlayButtonRow from './OverlayButtonRow';
import { defaultOverlayStyles } from '../../styles/common/common-style';
import { defaultOverlayStyles as customDefaultOverlayStyles } from '../../styles/custom/custom-style';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import * as overlayUtils from '../../utils/OverlayUtils';
import { DebugSection } from './DebugSection';

const DefaultOverlay: React.FC<IDefaultOverlayProps> = (
  props: IDefaultOverlayProps,
) => {
  let title = props.title;
  let closeable = props.closeAble || false;
  let contentMsgs: string[] | undefined = props.contentMessages;
  let buttonList: OverlayButtonDefinition[] | undefined =
    props.buttonList;

  const styles = useMemo(
    () => mergeStyles(defaultOverlayStyles, customDefaultOverlayStyles),
    [],
  );

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

  
  // Kein DebugInfo => Keine sections
  const debugData = props.debugInfo
    ? overlayUtils.showDebugSections(
      props.uiState,
      props.formState,
      props.debugInfo,
    )
    : null;

  const debugSections: React.ReactNode = debugData ? (
    <View style={styles.debugArea}>
      {debugData.map((section, index) => (
        <DebugSection key={index} {...section} />
      ))}
    </View>
  ) : null;

  const Container = Platform.OS === 'web' ? View : SafeAreaView;

  return (
    <Modal animationType="fade" transparent={true}>
      <Container style={{ flex: 1, backgroundColor: "transparent" }}>
        <View style={styles.modal}>
          <View
            style={[
              styles.modalContent,
              !props.footerMessages && styles.modalContentWithoutFooter,
              breakPoints.maximumLg && styles.modalContentScreenMaxLarge,
              breakPoints.maximumMd && styles.modalContentScreenMaxMedium,
              breakPoints.maximumSm && styles.modalContentScreenMaxSmall,
            ]}
          >
            <OverlayHeader
              uiState={props.uiState}
              updateState={props.updateState}
              closeAble={closeable}
              title={title}
              onClose={props.onClose}
            />
            <View style={styles.body}>
              <ScrollView style={styles.bodyScrollView}>
                {contentMsgs &&
                  contentMsgs.map((text, index) => (
                    <AndpText
                      style={styles.bodyText}
                      key={`overlayContentMsgs-${index}`}
                    >
                      {text}
                    </AndpText>
                  ))}
                {props.contentElementNodes && props.contentElementNodes}
                {debugSections}
              </ScrollView>
              <OverlayButtonRow
                buttons={buttonList}
                buttonObjects={props.buttonObjects}
              />
            </View>
            <OverlayFooter footerMessages={props.footerMessages} />
          </View>
        </View>
      </Container>
    </Modal>
  );
};

export default DefaultOverlay;
