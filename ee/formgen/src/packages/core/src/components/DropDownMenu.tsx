import React, { useRef, useEffect, useState, ReactNode } from 'react';
import { View, StyleSheet, Animated, Modal, TouchableOpacity, TouchableWithoutFeedback, Dimensions, Platform, ViewInstance } from 'react-native';
import { customComponentsTheme } from '../styles/theme';
import { useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';
import { IDropDownSelectOption } from '../types/interfaces';
import AndpIcon from './AndpIcon';
import AndpText from './AndpText';

/**
 * Ensures we never pass `undefined`, `null`, or `NaN` to native style props,
 * which would otherwise trigger “Invariant Violation: … is not usable as a
 * native method argument” on Android/iOS.
 */
const safeNumber = (val: number | null | undefined): number =>
  typeof val === 'number' && !Number.isNaN(val) ? val : 0;

interface DropdownMenuProps {
  modalAlignment?: 'left' | 'right';
  closeOnOutsideClick?: boolean;
  options: IDropDownSelectOption[];
  onChange?: (selected: IDropDownSelectOption) => void;
  children?: ReactNode;
}

export const MenuOption = ({
  onSelect,
  children,
}: {
  onSelect: () => void;
  children: ReactNode;
}) => {
  return (
    <TouchableOpacity onPress={onSelect} style={{padding: customComponentsTheme.dropdown.optionPadding,}}>
      {children}
    </TouchableOpacity>
  );
};

const DropDownMenu:  React.FC<DropdownMenuProps> = ({
  children,
  closeOnOutsideClick = true,
  options,
  onChange,
  modalAlignment = 'right'
}) => {
  const [visible, setVisible] = useState(false);
  const [position, setPosition] = useState({x: 0, y: 0, width: 0, height: 0});
  const [selected, setSelected] = useState(options[0]);
  
  const opacity = useSharedValue(0);
  const windowWidth = Dimensions.get('window').width;

  const triggerRef = useRef<ViewInstance>(null);

  useEffect(() => {
    if (triggerRef.current && visible) {
      triggerRef.current.measure((fx, fy, width, height, px, py) => {
        setPosition({
          x: px,
          y: py,
          width: width,
          height: height,
        });
      });
    }
    if (visible) {
        opacity.value = withTiming(1, { duration: 100 });
    } else {
        opacity.value = withTiming(0, { duration: 100 });
    }
  }, [visible]);

  const animatedStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
  }));

  return <View>
    <TouchableOpacity onPress={() => setVisible(true)}>
      <View ref={triggerRef}>{children}</View>
    </TouchableOpacity>
    <Modal
        transparent={true}
        visible={visible}
        animationType="fade"
        onRequestClose={() => setVisible(!closeOnOutsideClick)}
    >
      <TouchableWithoutFeedback onPress={() => setVisible(false)}>
        <View style={styles.modalOverlay}>
          <Animated.View style={[animatedStyle, {width: '100%'}]}>
          <View
                style={[
                  styles.menu,
                  {
                    // On web keep the classic behaviour (+5px below the trigger)
                    // On native (iOS/Android) open a bit further down
                    top:
                      Platform.OS === 'web'
                        ? safeNumber(position.y) + 5
                        : safeNumber(position.y) + 150,
                  },
                  modalAlignment === 'left'  && { left:  safeNumber(position.x) },
                  modalAlignment === 'right' && { right: safeNumber(windowWidth - position.x - position.width) },
                ]}>
                {options.map((option) => 
                    <MenuOption key={option.value} onSelect={() => {
                        setSelected(option)
                        if (onChange) onChange(option);
                        setVisible(false)
                    }}>
                        <View style={{flexDirection: "row", justifyContent: 'space-between', gap: customComponentsTheme.dropdown.menuPadding}}>
                            <AndpText style={{color: customComponentsTheme.dropdown.textColor}}>{option.label}</AndpText>
                            <View style={{width: 20, alignItems: 'flex-end', justifyContent: 'center'}}>
                                {option.value === selected.value ? 
                                <AndpIcon name='checkMark' size='md' color={customComponentsTheme.dropdown.textColor}/> : <></>}
                            </View>                            
                        </View>
                    </MenuOption>)
                }
              </View>
          </Animated.View>
        </View>
      </TouchableWithoutFeedback>
    </Modal>
  </View>;
}

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
    backgroundColor: 'transparent',
  },
  menu: {
    position: 'absolute',
    backgroundColor: customComponentsTheme.dropdown.backgroundColor,
    borderRadius: customComponentsTheme.dropdown.borderRadius,
    padding: customComponentsTheme.dropdown.menuPadding,
    shadowColor: customComponentsTheme.dropdown.shadowColor,
    shadowOffset: customComponentsTheme.dropdown.shadowOffset,
    shadowOpacity: customComponentsTheme.dropdown.shadowOpacity,
    shadowRadius: customComponentsTheme.dropdown.shadowRadius,
    elevation: customComponentsTheme.dropdown.elevation,
  }
});

export default DropDownMenu;