import React, { useEffect, useState } from 'react';
import {
  View,
  Platform,
  Modal,
  StyleSheet,
  TouchableOpacity,
  Pressable,
  Keyboard,
  Alert,
} from 'react-native';
import AndpText from '@andp/core/src/components/AndpText';
import {
  BottomSheet,
  Button,
  Host,
  HStack,
  Spacer,
  VStack,
  Text,
} from '@expo/ui/swift-ui';
import {
  AlertDialog as AndroidBottomSheet,
  Button as AndroidButton,
  shadow,
} from '@expo/ui/jetpack-compose';
import {
  background,
  cornerRadius,
  fixedSize,
  font,
  foregroundColor,
  frame,
  grayscale,
  multilineTextAlignment,
  onTapGesture,
  opacity,
  padding,
  scaleEffect,
  shapes,
} from '@expo/ui/swift-ui/modifiers';
import theme, {
  formElementWrapperTheme,
} from '@andp/core/src/styles/theme.native';
import { useTranslation } from 'react-i18next';

type Placement = 'bottom' | 'left';

export type CustomTooltipProps = {
  isVisible: boolean;
  placement?: Placement;
  onClose?: () => void;
  content: string;
  contentStyle?: any;
  textStyle?: any;
  tooltipStyle?: any;
  caretUpStyle?: any;
  caretRightStyle?: any;
  children: React.ReactNode;
  closeOnOutsidePress?: boolean;
};

export default function CustomTooltip({
  content,
  children,
}: CustomTooltipProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const closeInfoButtonText = t('messages_modal_btn_close');

  const handleOpen = () => {
    console.log('Opening tooltip');
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  if (Platform.OS === 'ios') {
    return (
      <View>
        <Host style={{ position: 'absolute' }}>
          <BottomSheet isOpened={open} onIsOpenedChange={handleClose}>
            <VStack
              spacing={20}
              modifiers={[
                padding({ all: 25 }),
                background(
                  formElementWrapperTheme.tooltip.tooltipContentBackgroundColor,
                ),
              ]}
            >
              <HStack>
                <Spacer />
                <Button variant="default" onPress={handleClose}>
                  <Text>{closeInfoButtonText}</Text>
                </Button>
              </HStack>

              <Text
                color={formElementWrapperTheme.tooltip.tooltipContentFontColor}
                modifiers={[
                  fixedSize({ vertical: true }),
                  multilineTextAlignment('center'),
                ]}
              >
                {content}
              </Text>
            </VStack>
          </BottomSheet>
        </Host>
        <Pressable onPress={handleOpen}>{children}</Pressable>
      </View>
    );
  }

  return (
    <View>
      {/* Waiting for Expo Library to update and fix Issues */}
      {/* <AndroidButton>Test</AndroidButton>
      <AndroidBottomSheet
        visible={open}
        title="Info"
        text={content}
        onDismissPressed={handleClose}
        dismissButtonText={closeInfoButtonText}
      /> */}
      <Pressable
        onPress={() =>
          Alert.alert('Info', content, [{ text: closeInfoButtonText }])
        }
      >
        {children}
      </Pressable>
    </View>
  );
}
