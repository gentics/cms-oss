import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import {
  View,
  Platform,
  StyleSheet,
  LayoutChangeEvent,
  ViewInstance,
} from 'react-native';
import { Portal } from 'react-native-portalize';
import AndpText from '@andp/core/src/components/AndpText';

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
  isVisible,
  placement = 'bottom',
  onClose,
  content,
  contentStyle,
  textStyle,
  tooltipStyle,
  caretUpStyle,
  caretRightStyle,
  children,
  closeOnOutsidePress = true,
}: CustomTooltipProps) {
  const anchorRef = useRef<ViewInstance | null>(null);

  const visible = isVisible;

  const [pos, setPos] = useState<{ top: number; left: number }>({ top: 0, left: 0 });
  const [hasPos, setHasPos] = useState(false);

  const [bubbleSize, setBubbleSize] = useState<{ w: number; h: number }>({ w: 0, h: 0 });
  const hasSize = bubbleSize.w > 0 && bubbleSize.h > 0;

  const onBubbleLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    setBubbleSize({ w: width, h: height });
  };

  const setPosSafe = (top: number, left: number) => {
    if (Number.isFinite(top) && Number.isFinite(left)) {
      setPos({ top: Math.round(top), left: Math.round(left) });
      setHasPos(true);
    }
  };

  // Pos Messen
  useLayoutEffect(() => {
    if (!visible) return;

    const node: any = anchorRef.current;
    const rect = node?.getBoundingClientRect?.();
    if (!rect) return;

    if (placement === 'left') {
      setPosSafe(rect.top + rect.height / 2, rect.left - 15);
    } else {
      setPosSafe(rect.bottom + 4, rect.left + rect.width / 2);
    }
  }, [visible, placement]);

  // Web
  useEffect(() => {
    if (!visible) return;
    const closeAll = () => {
      onClose?.();
    };
    const onKeyDown = (e: KeyboardEvent) => {
      const closeKeys = new Set([' ', 'Spacebar', 'PageDown', 'PageUp', 'ArrowDown', 'ArrowUp', 'Home', 'End', 'Escape']);
      if (closeKeys.has(e.key)) closeAll();
    };
    if (Platform.OS === "web") {
      const opts: AddEventListenerOptions = { capture: true, passive: true };
      window.addEventListener('wheel', closeAll, opts);
      window.addEventListener('touchmove', closeAll, opts);
      window.addEventListener('scroll', closeAll, opts);
      window.addEventListener('resize', closeAll, opts);
      window.addEventListener('keydown', onKeyDown, true);
      return () => {
        window.removeEventListener('wheel', closeAll, true);
        window.removeEventListener('touchmove', closeAll, true);
        window.removeEventListener('scroll', closeAll, true);
        window.removeEventListener('resize', closeAll, true);
        window.removeEventListener('keydown', onKeyDown, true);
      };
    }
  }, [visible, onClose]);

  // Offsets
  const translateX = hasSize ? (placement === 'left' ? -bubbleSize.w : -bubbleSize.w / 2) : 0;
  const translateY = hasSize ? (placement === 'left' ? -bubbleSize.h / 2 : 0) : 0;

  return (
    <View
      ref={anchorRef}
      collapsable={false}
    >
      {children}

      {visible && hasPos && (
        <Portal>
          <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>

            {/* TooltipBubble */}
            <View
              pointerEvents={hasSize ? 'auto' : 'none'}
              style={[
                {
                  alignItems: 'center',
                  zIndex: 9999,
                  opacity: hasSize ? 1 : 0,
                },
                Platform.select({
                  web: { position: 'fixed' as any },
                  default: { position: 'absolute' },
                }),
                {
                  top: pos.top,
                  left: pos.left,
                  transform: [{ translateX }, { translateY }] as any,
                },
                tooltipStyle,
              ]}
            >
              {placement === 'bottom' && <View style={caretUpStyle} />}
              {placement === 'left' && <View style={caretRightStyle} />}
              <View onLayout={onBubbleLayout} style={contentStyle}>
                <View
                  accessible
                  accessibilityRole="text"
                  accessibilityLabel={`Tooltip: ${content}`}
                  accessibilityLiveRegion="polite"
                >
                  <AndpText style={{color: "white"}}>
                    {content}
                  </AndpText>
                </View>
              </View>
            </View>
          </View>
        </Portal>
      )}
    </View>
  );
}
