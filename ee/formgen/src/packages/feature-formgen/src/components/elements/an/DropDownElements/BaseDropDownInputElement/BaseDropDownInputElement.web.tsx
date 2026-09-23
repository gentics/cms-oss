import {
  forwardRef,
  ReactElement,
  useEffect,
  useImperativeHandle,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { TFunction } from 'i18next';
import {
  FlatList,
  Platform,
  Pressable,
  View,
  Animated,
  Easing,
  NativeSyntheticEvent,
  TextInputKeyPressEventData,
  LayoutAnimation,
  ActivityIndicator,
  TouchableOpacity,
  TextInput,
} from 'react-native';
import { Portal } from 'react-native-portalize';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import AndpInput from '@andp/core/src/components/AndpInputElement';
import { isFilled } from '../../../../../utils/Utils';
import theme, {
  dropdownInputTheme,
  formElementWrapperTheme,
} from '@andp/core/src/styles/theme';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { baseDropDownInputElement } from '../../../../../styles/common/common-style';
import { baseDropDownInputElement as customBaseDropDownInputElement } from '../../../../../styles/custom/custom-style';

/**
 * Used for dynamically calculated style Values.
 * Guarantees that we never pass `null`, `undefined` or `NaN` into a style prop
 * that expects a number. React Native throws “Invariant Violation: … is not
 * usable as a native method argument” when it encounters those values.
 */
const safeNumber = (val: number | null | undefined): number =>
  typeof val === 'number' && !Number.isNaN(val) ? val : 0;

export interface BaseDropDownInputElementProps {
  inputValue: string;
  onChangeInputValue: (text: string) => void;
  onFocus?: () => void;
  onBlur?: () => void;
  clearInput?: () => void;
  isList?: boolean;
  selectedValues?: any[];
  labelText: string;
  placeholderText: string;
  dropdownOpen: boolean;
  children?: React.ReactNode;
  rightElements?: React.ReactNode[];
  onRemoveChip?: (chip: any) => void;
  customStyles?: {
    combinedInputContainer?: object;
    errorContainer?: object;
    floatingLabelContainer?: object;
    floatingLabel?: object;
    inputRow?: object;
    inputContainer?: object;
    inputField?: object;
    chipsWrapper?: object;
    chip?: object;
    chipText?: object;
    dropdownContainer?: object;
    errorDropdown?: object;
    combinedDropdown?: object;
    dropdownListContent?: object;
    inputFieldWithPlaceholder?: object;
    inputFieldWithValue?: object;
  };
  error?: string;
  value?: any;
  dropdownData?: any[];
  dropdownRenderItem?: (info: { item: any; index: number }) => ReactElement;
  dropdownOnSelect?: (item: any) => void;
  disabled?: boolean;
  onlyDropdown?: boolean;
  t: TFunction;
  hideDropdownArrow?: boolean;
  loading?: boolean;
  ANFieldLoadError?: boolean;
  jsonpath?: string;
}

const BaseDropDownInputElement = forwardRef<any, BaseDropDownInputElementProps>(
  (props, ref) => {
    const {
      inputValue,
      onChangeInputValue,
      onFocus,
      onBlur,
      isList,
      selectedValues,
      labelText,
      placeholderText,
      dropdownOpen,
      children,
      rightElements,
      onRemoveChip,
      customStyles = {},
      error,
      value,
      dropdownData,
      dropdownRenderItem,
      dropdownOnSelect,
      disabled,
      onlyDropdown,
      t,
      hideDropdownArrow = false,
      loading = false,
      ANFieldLoadError = false,
      clearInput,
      jsonpath,
    } = props;

    const arrowToggleRef = useRef<View>(null);
    const inputRef = useRef<any>(null);
    const containerRef = useRef<any>(null);
    const outFocusRef = useRef<any>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [focusedIndex, setFocusedIndex] = useState<number>(-1);
    const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
    const [dropdownItemFocused, setDropdownItemFocused] = useState(false);
    const [anchorPosition, setAnchorPosition] = useState({
      x: 0,
      y: 0,
      width: 0,
      height: 0,
    });
    const styles = useMemo(
      () =>
        mergeStyles(baseDropDownInputElement, customBaseDropDownInputElement),
      [],
    );

    // --- Focus management for dropdown items
    const itemRefs = useRef<Array<any>>([]);
    useEffect(() => {
      if (focusedIndex >= 0 && itemRefs.current[focusedIndex]) {
        itemRefs.current[focusedIndex].focus();
      }
    }, [focusedIndex]);
    const measureAnchor = () => {
      if (dropdownOpen && containerRef.current) {
        containerRef.current.measureInWindow(
          (x: number, y: number, width: number, height: number) => {
            if (Platform.OS === 'web') {
              const node: any = containerRef.current;
              const rect = node.getBoundingClientRect();
              const root = document.getElementById('root');
              const offsetY = root
                ? root.getBoundingClientRect().top + window.pageYOffset
                : 0;
              const offsetX = root
                ? root.getBoundingClientRect().left + window.pageXOffset
                : 0;

              const newX = rect.left + window.pageXOffset - offsetX;
              const newY =
                rect.top - offsetY + rect.height + window.pageYOffset;

              if (
                newX !== anchorPosition.x ||
                newY !== anchorPosition.y ||
                rect.width !== anchorPosition.width ||
                rect.height !== anchorPosition.height
              ) {
                setAnchorPosition({
                  x: newX,
                  y: newY,
                  width: rect.width,
                  height: rect.height,
                });
              }
            } else {
              y = y + height;
              if (
                x !== anchorPosition.x ||
                y !== anchorPosition.y ||
                width !== anchorPosition.width ||
                height !== anchorPosition.height
              ) {
                setAnchorPosition({ x, y: y, width, height });
              }
            }
          },
        );
      }
    };

    // Measure anchor only when dropdown visibility toggles or on initial mount
    useLayoutEffect(() => {
      if (dropdownOpen) {
        measureAnchor();
      }
    }, [dropdownOpen]);

    useImperativeHandle(ref, () => ({
      focus: () => {
        if (inputRef.current && typeof inputRef.current.focus === 'function') {
          inputRef.current.focus();
        }
      },
      measureLayout: (relativeTo: any, onSuccess: any, onFail: any) => {
        if (containerRef.current) {
          containerRef.current.measureLayout(relativeTo, onSuccess, onFail);
        }
      }
    }));

    const dropdownOpacityAnimation = useRef(new Animated.Value(0)).current;
    const dropdownTransformAnimation = useRef(new Animated.Value(0)).current;
    const isDropdownVisible =
      dropdownOpen && (isFocused || dropdownItemFocused);
    const [dropdownInDOM, setDropdownInDOM] = useState<boolean>(false);

    useEffect(() => {
      Animated.timing(dropdownOpacityAnimation, {
        toValue: isDropdownVisible ? 1 : 0,
        duration: 200,
        easing: Easing.bezier(0.133, 0.652, 0.415, 1.002),
        useNativeDriver: Platform.OS !== 'web',
      }).start();
      Animated.timing(dropdownTransformAnimation, {
        toValue: isDropdownVisible ? 0 : -30,
        duration: 300,
        easing: Easing.bezier(0.133, 0.652, 0.415, 1),
        useNativeDriver: Platform.OS !== 'web',
      }).start();
      if (!isDropdownVisible) {
        const unmountTimeout = setTimeout(() => {
          setDropdownInDOM(false);
        }, 320);
        return () => clearTimeout(unmountTimeout);
      }
      setDropdownInDOM(true);
    }, [
      dropdownTransformAnimation,
      dropdownOpacityAnimation,
      isDropdownVisible,
    ]);

    const handleContainerPress = () => {
      if (disabled) return;
      if (
        inputRef.current &&
        typeof inputRef.current.focus === 'function' &&
        !isFocused
      ) {
        handleInputFocus();
        inputRef.current.focus();
      }
    };

    const handleArrowToggle = (e: any) => {
      if (disabled) return;
      if (isFocused && dropdownOpen) {
        // Dropdown ist offen -> schließen
        setIsFocused(false);
        handleLabelAnimationForBlur();
        if (inputRef.current && typeof inputRef.current.blur === 'function') {
          inputRef.current.blur();
        }
        onBlur && onBlur();
      } else {
        // Dropdown ist geschlossen -> öffnen und fokussieren
        setIsFocused(true);
        handleLabelAnimationForFocus();
        if (inputRef.current && typeof inputRef.current.focus === 'function') {
          inputRef.current.focus();
        }
        onFocus && onFocus();
      }
    };

    const handleInputFocus = () => {
      if (disabled) return;
      setIsFocused(true);
      handleLabelAnimationForFocus();
      if (onFocus) onFocus();
    };

    const handleInputBlur = (e: any) => {
      setTimeout(() => {
        // On web, if focus moved into one of our dropdown items, keep open
        if (Platform.OS === 'web') {
          if (e.relatedTarge && e.relatedTarget === arrowToggleRef.current)
            return;
          const active = document.activeElement;
          if (itemRefs.current.some((ref) => ref && ref === active)) {
            return;
          }
        }
        setIsFocused(false);
        if (!activeLabel) handleLabelAnimationForBlur();
        if (onBlur) onBlur();
      }, 1);
    };

    const handleInputKeyDownNative = (
      e: NativeSyntheticEvent<TextInputKeyPressEventData>,
    ) => {
      if (!dropdownData || dropdownData.length === 0) return;
      const key = e.nativeEvent.key;

      if (key === 'Tab') {
        // If Shift + Tab is pressed, allow default behavior (focus jumps back)
        if ((e as any).shiftKey) return;

        // If the dropdown is closed, allow default behavior (focus moves to next element)
        if (!dropdownOpen) {
          return;
        }

        // If the dropdown is open, prevent default behavior (focus moving away)
        // so we can manually navigate through the list.
        e.preventDefault();
        e.stopPropagation();
        handleKeyDown(e);
      } else if (key === 'Enter') {
        e.preventDefault();
        e.stopPropagation();
        if (!dropdownOpen) {
          props.dropdownOpen === false && onChangeInputValue(inputValue);
        }
        handleKeyDown(e);
      } else if (key === 'ArrowDown' || key === 'ArrowUp') {
        e.preventDefault();
        e.stopPropagation();
        handleKeyDown(e);
      }
    };

    const handleKeyDown = (
      e: NativeSyntheticEvent<TextInputKeyPressEventData>,
    ) => {
      if (!dropdownData || dropdownData.length === 0) return;
      const key = e.nativeEvent.key;

      if (key === 'Tab' && !dropdownOpen) {
        return;
      }

      if (key === 'ArrowDown') {
        e.preventDefault();
        setFocusedIndex((prev) =>
          prev < (dropdownData?.length || 1) - 1 ? prev + 1 : 0,
        );
        return;
      }
      if (key === 'ArrowUp') {
        e.preventDefault();
        setFocusedIndex((prev) =>
          prev > 0 ? prev - 1 : (dropdownData?.length || 1) - 1,
        );
        return;
      }
      if (key === 'Tab') {
        // Tab inside open dropdown: Cycle through items (next item or back to start)
        e.preventDefault();
        setFocusedIndex((prev) =>
          prev < dropdownData.length - 1 ? prev + 1 : 0,
        );
      } else if (key === 'Enter') {
        if (
          focusedIndex >= 0 &&
          focusedIndex < dropdownData.length &&
          dropdownOnSelect
        ) {
          dropdownOnSelect(dropdownData[focusedIndex]);
          setIsFocused(false);
          if (inputRef.current && typeof inputRef.current.blur === 'function') {
            inputRef.current.blur();
          }
          onBlur && onBlur();

          // For onlyDropdown, shift focus to container to prevent reopening the dropdown
          if (
            !onlyDropdown &&
            isList &&
            containerRef.current &&
            typeof containerRef.current.focus === 'function'
          ) {
            containerRef.current.focus();
          } else if (
            outFocusRef.current &&
            typeof outFocusRef.current.focus === 'function'
          ) {
            outFocusRef.current.focus();
          }
        }
      }
    };

    const customRenderItem = ({
      item,
      index,
    }: {
      item: any;
      index: number;
    }): React.ReactElement => {
      const renderedItem = dropdownRenderItem ? (
        dropdownRenderItem({ item, index })
      ) : (
        <></>
      );
      const isActive =
        hoveredIndex !== null ? hoveredIndex === index : focusedIndex === index;
      const handlePress = () => {
        LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
        dropdownOnSelect && dropdownOnSelect(item);
        setHoveredIndex(null);
        onBlur && onBlur();

        if (
          !onlyDropdown &&
          isList &&
          containerRef.current &&
          typeof containerRef.current.focus === 'function'
        ) {
          containerRef.current.focus();
        } else if (
          outFocusRef.current &&
          typeof outFocusRef.current.focus === 'function'
        ) {
          outFocusRef.current.focus();
        }
      };
      const additionalProps = {
        ref: (el: any) => {
          itemRefs.current[index] = el;
        },
        onMouseEnter: () => setHoveredIndex(index),
        onMouseLeave: () => setHoveredIndex(null),
        onFocus: () => setDropdownItemFocused(true),
        onBlur: () => setDropdownItemFocused(false),
        onKeyDown: (e: any) => {
          const key = e.nativeEvent.key;
          const count = dropdownData?.length || 0;
          if (key === 'Tab') {
            if (e.shiftKey) {
              e.preventDefault();
              if (index === 0) {
                if (
                  inputRef.current &&
                  typeof inputRef.current.focus === 'function'
                ) {
                  inputRef.current.focus();
                }
                setFocusedIndex(-1);
              } else {
                const prev = (index - 1 + count) % count;
                setFocusedIndex(prev);
              }
            } else {
              if (index < count - 1) {
                e.preventDefault();
                const next = (index + 1) % count;
                setFocusedIndex(next);
              } else {
                if (outFocusRef.current) {
                  outFocusRef.current.focus();
                }
                setIsFocused(false);
                onBlur && onBlur();
              }
            }
          } else if (key === 'ArrowDown') {
            e.preventDefault();
            const next = (index + 1) % count;
            setFocusedIndex(next);
          } else if (key === 'ArrowUp') {
            e.preventDefault();
            const prev = (index - 1 + count) % count;
            setFocusedIndex(prev);
          } else if (key === 'Enter') {
            e.preventDefault();
            handlePress();
          }
        },
        onPress: handlePress,
        accessibilityLabel: t('form_accessibility_dropdown_item', {
          index: index + 1,
          label: item.displayName,
        }),
      };
      const {
        style: childStyle,
        children: childChildren,
        id: childId,
        ...childRest
      } = (renderedItem as any).props;
      return (
        <Pressable
          id={childId}
          {...additionalProps}
          style={[
            styles.dropdownOption,
            isActive && styles.dropdownOptionHover,
            error && styles.dropdownOptionError,
            isActive && error && styles.dropdownOptionErrorHover,
          ]}
        >
          {childChildren}
        </Pressable>
      );
    };

    const animatedDropdownStyle = {
      opacity: dropdownOpacityAnimation,
      transform: [
        {
          translateY: dropdownTransformAnimation,
        },
      ],
    };

    const effectiveInputValue = onlyDropdown
      ? value && value.displayName
        ? value.displayName
        : placeholderText
      : inputValue;

    // Initialisiere die Placeholder-Animation
    const animatedPlaceholderTop = useRef(
      new Animated.Value(
        props.value ? styles.dropdownLabelActive.top : styles.dropdownLabel.top,
      ),
    ).current;
    const animatedPlaceholderFontSize = useRef(
      new Animated.Value(
        props.value
          ? styles.dropdownLabelActive.fontSize
          : styles.dropdownLabel.fontSize,
      ),
    ).current;

    const handleLabelAnimationForFocus = () => {
      Animated.timing(animatedPlaceholderTop, {
        toValue: styles.dropdownLabelActive.top,
        duration: 300,
        easing: Easing.out(Easing.quad),
        useNativeDriver: false,
      }).start();
      Animated.timing(animatedPlaceholderFontSize, {
        toValue: styles.dropdownLabelActive.fontSize,
        duration: 300,
        easing: Easing.out(Easing.quad),
        useNativeDriver: false,
      }).start();
    };

    const handleLabelAnimationForBlur = () => {
      Animated.timing(animatedPlaceholderTop, {
        toValue: styles.dropdownLabel.top,
        duration: 300,
        easing: Easing.bezier(0.133, 0.652, 0.415, 1),
        useNativeDriver: false,
      }).start();
      Animated.timing(animatedPlaceholderFontSize, {
        toValue: styles.dropdownLabel.fontSize,
        duration: 300,
        easing: Easing.bezier(0.133, 0.652, 0.415, 1),
        useNativeDriver: false,
      }).start();
    };

    let activeLabel = !(
      !value ||
      (value as string).length == 0 ||
      (isList && selectedValues?.length === 0)
    );

    useEffect(() => {
      activeLabel
        ? handleLabelAnimationForFocus()
        : handleLabelAnimationForBlur();
    }, [activeLabel, value]);

    return (
      <Pressable
        onPress={handleContainerPress}
        accessibilityLabel={t('form_accessibility_dropdown_open')}
        tabIndex={-1}
      >
        <View
          ref={containerRef}
          onLayout={measureAnchor}
          style={[
            styles.dropdown,
            customStyles.combinedDropdown,
            disabled && styles.disabledDropDown,
          ]}
          {...(Platform.OS === 'web' && { onKeyDown: handleKeyDown })}
        >
          <View
            style={[
              styles.dropdownInputContainer,
              customStyles.combinedInputContainer,
              error && styles.errorContainer,
              error && customStyles.errorContainer,
              disabled && styles.disabledDropDown,
            ]}
          >
            <Animated.Text
              style={[
                styles.dropdownPlaceholderLabel,
                error && !props.disabled && styles.placeholderLabelError,
                {
                  top: animatedPlaceholderTop,
                  fontSize: animatedPlaceholderFontSize,
                },
              ]}
              numberOfLines={1}
              ellipsizeMode="tail"
            >
              {labelText}
            </Animated.Text>
            <AndpInput
              ref={inputRef}
              placeholder={value && value.length !== 0 && placeholderText}
              // useCustomPlaceholderWrapper={true}
              value={effectiveInputValue}
              onChangeText={onlyDropdown ? () => {} : onChangeInputValue}
              onFocus={handleInputFocus}
              onBlur={handleInputBlur}
              onKeyPress={(e) => handleInputKeyDownNative(e)}
              {...(Platform.OS === 'web' ? { onKeyDown: handleKeyDown } : {})}
              disableHeightForDesktop={true}
              placeholderTextColor={
                error ? theme.colors.colorDeleteRed : theme.colors.textPrimary
              }
              style={[styles.inputElement, customStyles.inputContainer]}
              inputStyle={[
                styles.inputField,
                customStyles.inputField,
                disabled && styles.disabledInput,
              ]}
              rightElement={[
                ...(rightElements || []),
                loading && (
                  <ActivityIndicator
                    key="loading"
                    accessible
                    accessibilityRole="image"
                    accessibilityLabel={t(
                      'form_accessibility_dropdown_loading',
                    )}
                    size={formElementWrapperTheme.loaderIcon.size}
                    color={formElementWrapperTheme.loaderIcon.color}
                  />
                ),
                clearInput &&
                  !disabled &&
                  (inputValue || isFilled(props.value)) && (
                    <TouchableOpacity
                      id={`${jsonpath}-delete-content-btn`}
                      key="clear"
                      onPress={clearInput}
                      accessibilityRole="button"
                      accessibilityLabel={t('form_accessibility_clear')}
                      accessibilityHint={t('form_accessibility_clear_hint')}
                      hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                    >
                      <AndpIcon
                        name="close"
                        size={formElementWrapperTheme.deleteContentBtnIcon.size}
                        color={
                          formElementWrapperTheme.deleteContentBtnIcon.color
                        }
                      />
                    </TouchableOpacity>
                  ),
                ANFieldLoadError && (
                  <View
                    key="warning"
                    accessible
                    accessibilityRole="image"
                    accessibilityLabel={t(
                      'form_accessibility_dropdown_warning',
                    )}
                  >
                    <AndpIcon
                      name="warning"
                      size={formElementWrapperTheme.anFieldLoadErrorIcon.size}
                      color={formElementWrapperTheme.anFieldLoadErrorIcon.color}
                    />
                  </View>
                ),
                !hideDropdownArrow && (
                  <Pressable
                    ref={arrowToggleRef}
                    key="arrow"
                    accessible
                    accessibilityRole="button"
                    accessibilityLabel={t('form_accessibility_dropdown_toggle')}
                    onPress={handleArrowToggle}
                    disabled={disabled}
                    //@ts-ignore
                    style={
                      Platform.OS === 'web' && {
                        cursor: disabled ? 'not-allowed' : 'pointer',
                      }
                    }
                  >
                    <AndpIcon
                      name={isFocused && dropdownOpen ? 'arrowUp' : 'arrowDown'}
                      size={formElementWrapperTheme.dropdownIcon.size}
                      color={
                        props.error
                          ? formElementWrapperTheme.dropdownIcon.colorError
                          : formElementWrapperTheme.dropdownIcon.color
                      }
                    />
                  </Pressable>
                ),
              ]}
              customPlaceholderWrapperStyle={[
                styles.inputField,
                customStyles.inputField,
                onlyDropdown ? { caretColor: 'transparent' } : undefined,
                disabled && styles.disabledInput,
              ]}
              editable={!onlyDropdown && !props.disabled}
              caretHidden={onlyDropdown}
              showSoftInputOnFocus={!onlyDropdown}
              readOnly={onlyDropdown}
              accessibilityRole="menubar"
              accessibilityLabel={`${t(
                'form_accessibility_andp_dropdown_input',
              )} ${
                effectiveInputValue && effectiveInputValue.length > 0
                  ? effectiveInputValue
                  : placeholderText
              }`}
              accessibilityHint={t(
                'form_accessibility_andp_dropdown_input_hint',
              )}
              accessibilityValue={{ text: effectiveInputValue }}
            />
            {isList && selectedValues && selectedValues.length > 0 && (
              <View style={[styles.chipsWrapper, customStyles.chipsWrapper]}>
                {selectedValues.map((item: any) => (
                  <View
                    key={item.identifier}
                    style={[
                      styles.chip,
                      customStyles.chip,
                      error && styles.errorChip,
                    ]}
                  >
                    <AndpText
                      style={[
                        styles.chipText,
                        customStyles.chipText,
                        error && styles.errorText,
                      ]}
                      accessibilityLabel={item.displayName}
                      ellipsizeMode="tail"
                      numberOfLines={1}
                    >
                      {item.displayName}
                    </AndpText>
                    <Pressable
                      onPress={() => {
                        LayoutAnimation.configureNext(
                          LayoutAnimation.Presets.easeInEaseOut,
                        );
                        onRemoveChip && !disabled && onRemoveChip(item);
                      }}
                      accessible={true}
                      accessibilityRole="button"
                      accessibilityLabel={t(
                        'form_accessibility_andp_dropdown_remove_selected',
                        { label: item.displayName },
                      )}
                    >
                      <View
                        style={
                          Platform.OS === 'web' ? { cursor: 'pointer' } : {}
                        }
                      >
                        <AndpIcon
                          name="close"
                          size={
                            dropdownInputTheme.dropdownList.multiSelectItem
                              .iconSize
                          }
                          color={
                            dropdownInputTheme.dropdownList.multiSelectItem
                              .iconColor
                          }
                        />
                      </View>
                    </Pressable>
                  </View>
                ))}
              </View>
            )}
          </View>
          <TextInput
            ref={outFocusRef}
            style={{ width: 0, height: 0, opacity: 0 }}
          />
          {dropdownInDOM &&
            ((dropdownData && dropdownData.length > 0) || children != null) && (
              <Portal>
                <Animated.View
                  id="dropdown-list"
                  style={[
                    styles.dropdownContainer,
                    customStyles.dropdownContainer,
                    {
                      // Ensure we never propagate null / NaN values on any platform
                      left: safeNumber(anchorPosition.x),
                      top: safeNumber(anchorPosition.y),
                      width: safeNumber(anchorPosition.width),
                    },
                    error && styles.errorDropdown,
                    error && customStyles.errorDropdown,
                    animatedDropdownStyle,
                    disabled && styles.disabledDropDown,
                    dropdownData &&
                      dropdownData.length > 3 && {
                        overflow: Platform.OS === 'web' ? 'auto' : 'scroll',
                      },
                  ]}
                  accessibilityRole="menu"
                  accessibilityLabel={t(
                    'form_accessibility_andp_dropdown_list',
                  )}
                  accessibilityHint={t(
                    'form_accessibility_andp_dropdown_list_hint',
                  )}
                >
                  {dropdownData &&
                  dropdownRenderItem &&
                  dropdownData.length > 0 ? (
                    <FlatList
                      data={dropdownData}
                      renderItem={customRenderItem}
                      keyExtractor={(_, index) => index.toString()}
                      contentContainerStyle={customStyles.dropdownListContent}
                      scrollEnabled={onlyDropdown ? false : true}
                      showsVerticalScrollIndicator={
                        dropdownInputTheme.dropdownList.showScrollbar
                      }
                      keyboardShouldPersistTaps="always"
                      ItemSeparatorComponent={() => (
                        <View style={styles.dropdownItemsSeparator} />
                      )}
                    />
                  ) : (
                    <View>{children}</View>
                  )}
                </Animated.View>
              </Portal>
            )}
          <Pressable ref={outFocusRef}></Pressable>
        </View>
      </Pressable>
    );
  },
);

export default BaseDropDownInputElement;
