import React, {
  forwardRef,
  ReactElement,
  useCallback,
  useMemo,
  useRef,
  useState,
} from 'react';
import { TFunction } from 'i18next';
import {
  Pressable,
  View,
  Animated,
  LayoutAnimation,
  ActivityIndicator,
  Text,
  Platform,
  useWindowDimensions,
} from 'react-native';
import {
  BottomSheet as IosBottomSheet,
  Group,
  Host,
  Text as IosText,
  List as IosList,
  VStack,
  Spacer,
  Button,
  HStack,
  Image as IosImage,
  Picker,
} from '@expo/ui/swift-ui';
import {
  font,
  foregroundColor,
  listStyle,
  multilineTextAlignment,
  padding,
  pickerStyle,
  presentationDetents,
  tag,
  tint,
} from '@expo/ui/swift-ui/modifiers';
import { SafeAreaView } from 'react-native-safe-area-context';

import AndpInputElement from '@andp/core/src/components/AndpInputElement';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { isFilled } from '../../../../../utils/Utils';
import theme, {
  dropdownInputTheme,
  formElementWrapperTheme,
} from '@andp/core/src/styles/theme';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import {
  baseDropDownBottomSheet,
  baseDropDownInputElement,
} from '../../../../../styles/common/common-style';
import {
  customBaseDropDownBottomSheet,
  baseDropDownInputElement as customBaseDropDownInputElement,
} from '../../../../../styles/custom/custom-style';
import {
  BottomSheetModal,
  BottomSheetFlatList,
  BottomSheetTextInput,
  BottomSheetBackdrop,
} from '@gorhom/bottom-sheet';

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

    const { width, height } = useWindowDimensions();

    const arrowToggleRef = useRef<View>(null);
    const inputRef = useRef<any>(null);
    const bottomSheetInputRef = useRef<any>(null);
    const containerRef = useRef<any>(null);
    const outFocusRef = useRef<any>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [dropdownItemFocused, setDropdownItemFocused] = useState(false);
    const [isNativeSheetOpen, setIsNativeSheetOpen] = useState(false);

    const handleInputFocus = useCallback(() => {
      if (disabled) return;
      setIsFocused(true);
      if (onFocus) {
        onFocus();
      }
    }, [disabled, onFocus]);

    const handleInputBlur = useCallback(() => {
      setIsFocused(false);
      if (onBlur) {
        onBlur();
      }
    }, [onBlur]);
    const styles = useMemo(
      () =>
        mergeStyles(baseDropDownInputElement, customBaseDropDownInputElement),
      [],
    );

    const stylesBottom = useMemo(
      () => mergeStyles(baseDropDownBottomSheet, customBaseDropDownBottomSheet),
      [],
    );

    const bottomSheetModalRef = useRef<BottomSheetModal>(null);
    const snapPoints = useMemo(() => ['40%'], [onlyDropdown]);
    const isBooleanValue = typeof value === 'boolean';
    const hasNonBooleanValue =
      value !== undefined && value !== null && !isBooleanValue;
    const valueDisplay =
      hasNonBooleanValue && typeof value === 'object' && 'displayName' in value
        ? (value as any).displayName
        : hasNonBooleanValue
          ? String(value)
          : '';

    const hasEffectiveValue =
      !isList &&
      (!!(valueDisplay && valueDisplay.length > 0) ||
        !!(inputValue && inputValue.length > 0));

    const effectiveInputValue =
      valueDisplay && valueDisplay.length > 0 ? valueDisplay : inputValue;

    const displayedText = hasEffectiveValue ? effectiveInputValue : labelText;

    const pickerSelectedIndex = useMemo(() => {
      if (!dropdownData || dropdownData.length === 0) return 0;
      let currentVal = value;
      if (typeof currentVal === 'string') {
        try {
          if (currentVal.trim().startsWith('{')) {
            currentVal = JSON.parse(currentVal);
          }
        } catch (e) { }
      }
      if (currentVal && typeof currentVal === 'object' && currentVal.identifier) {
        const idx = dropdownData.findIndex((d) => d.identifier === currentVal.identifier);
        return idx >= 0 ? idx : 0;
      }
      return 0;
    }, [value, dropdownData]);

    // callbacks
    const handlePresentModalPress = () => {
      if (disabled) return;

      if (Platform.OS === 'ios') {
        setIsNativeSheetOpen(true);
        setTimeout(() => {
          !onlyDropdown && bottomSheetInputRef.current?.focus?.();
        }, 500);
        return;
      }

      // Present the bottom sheet at the configured snap point
      bottomSheetModalRef.current?.present();

      // Focus the input inside the bottom sheet after the animation starts
      setTimeout(() => {
        !onlyDropdown && bottomSheetInputRef.current?.focus?.();
      }, 250);
    };
    const handleSheetChanges = useCallback((index: number) => {
      if (index == -1) {
        !onlyDropdown && bottomSheetInputRef.current.blur();
        bottomSheetModalRef.current?.close();
        onBlur && onBlur();
      } else if (index == 0) {
        !onlyDropdown && bottomSheetInputRef.current.focus();
      }
    }, []);

    const renderBackdrop = useCallback(
      (props: any) => (
        <BottomSheetBackdrop
          {...props}
          opacity={0.2}
          disappearsOnIndex={-1}
          appearsOnIndex={0}
          pressBehavior={'close'}
        />
      ),
      [],
    );

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
      const handlePress = () => {
        dropdownOnSelect && dropdownOnSelect(item);

        bottomSheetModalRef.current?.close();
        !onlyDropdown && bottomSheetInputRef.current.blur();
        LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);

        onBlur && onBlur();
      };
      const additionalProps = {
        onBlur: () => setDropdownItemFocused(false),
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
          style={[styles.dropdownOption, error && styles.dropdownOptionError]}
        >
          {childChildren}
        </Pressable>
      );
    };

    const dropdownChipList = () => {
      return (
        <View style={[styles.chipsWrapper, customStyles.chipsWrapper]}>
          {selectedValues?.map((item: any) => (
            <View
              key={item.identifier}
              style={[
                styles.chip,
                customStyles.chip,
                error && styles.errorChip,
              ]}
            >
              <Text
                style={[
                  styles.chipText,
                  customStyles.chipText,
                  error && styles.errorText,
                ]}
                accessibilityLabel={item.displayName}
                ellipsizeMode="tail"
                numberOfLines={1}
                selectable={false}
              >
                {item.displayName}
              </Text>
              <Pressable
                style={styles.chipRemoveButton}
                hitSlop={{ top: 8, right: 8, bottom: 8, left: 8 }}
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
                <View style={styles.chipRemoveIconWrapper}>
                  <AndpIcon
                    name="close"
                    size={formElementWrapperTheme.deleteContentBtnIcon.size}
                    color={formElementWrapperTheme.deleteContentBtnIcon.color}
                  />
                </View>
              </Pressable>
            </View>
          ))}
        </View>
      );
    };

    return (
      <View>
        <Pressable
          accessibilityLabel={t('form_accessibility_dropdown_open')}
          tabIndex={-1}
          onPress={handlePresentModalPress}
        >
          <View
            ref={(node) => {
              containerRef.current = node;
              if (typeof ref === 'function') {
                ref(node);
              } else if (ref) {
                ref.current = node;
              }
              if (node) {
                (node as any).focus = handlePresentModalPress;
              }
            }}
            style={[
              styles.dropdown,
              customStyles.combinedDropdown,
              disabled && styles.disabledDropDown,
            ]}
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
              {hasEffectiveValue && (
                <Animated.Text
                  style={[
                    styles.dropdownPlaceholderLabel,
                    error && !props.disabled && styles.placeholderLabelError,
                    { top: 4, fontSize: 12 }
                  ]}
                  numberOfLines={1}
                  ellipsizeMode="tail"
                >
                  {labelText}
                </Animated.Text>
              )}
              <View style={styles.inputElement}>
                <Text
                  ref={inputRef}
                  style={[
                    styles.inputField,
                    !effectiveInputValue && {
                      color: dropdownInputTheme.input.labelFontColor,
                    },
                    (!effectiveInputValue || isList) && { marginTop: 0 },
                    disabled && styles.disabledInput,
                    error && { color: dropdownInputTheme.errorFontColor },
                    {pointerEvents: 'none'}
                  ]}
                  numberOfLines={1}
                  accessibilityRole="menubar"
                  accessibilityHint={t(
                    'form_accessibility_andp_dropdown_input_hint',
                  )}
                  ellipsizeMode="tail"
                >
                  {displayedText}
                </Text>

                <View style={styles.iconsContainer}>
                  {loading && !disabled ? (
                    <View
                      key="loading"
                      accessible
                      accessibilityRole="image"
                      accessibilityLabel={t(
                        'form_accessibility_dropdown_loading',
                      )}
                      style={styles.iconView}
                    >
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
                    </View>
                  ) : null}

                  {ANFieldLoadError ? (
                    <View
                      key="warning"
                      accessible
                      accessibilityRole="image"
                      accessibilityLabel={t(
                        'form_accessibility_dropdown_warning',
                      )}
                      style={styles.iconView}
                    >
                      <AndpIcon
                        name="warning"
                        size={formElementWrapperTheme.anFieldLoadErrorIcon.size}
                        color={
                          formElementWrapperTheme.anFieldLoadErrorIcon.color
                        }
                      />
                    </View>
                  ) : null}

                  {clearInput &&
                    !disabled &&
                    (inputValue || isFilled(props.value)) ? (
                    <Pressable
                      key="clear"
                      onPress={clearInput}
                      accessibilityRole="button"
                      accessibilityLabel={t('form_accessibility_clear')}
                      accessibilityHint={t('form_accessibility_clear_hint')}
                      hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                      style={styles.iconView}
                    >
                      <AndpIcon
                        name="close"
                        size={formElementWrapperTheme.deleteContentBtnIcon.size}
                        color={
                          formElementWrapperTheme.deleteContentBtnIcon.color
                        }
                      />
                    </Pressable>
                  ) : null}

                  {!hideDropdownArrow && !disabled ? (
                    <Pressable
                      key="arrow"
                      accessible
                      accessibilityRole="button"
                      accessibilityLabel={t(
                        'form_accessibility_dropdown_toggle',
                      )}
                      onPress={handlePresentModalPress}
                      //@ts-ignore
                      style={styles.iconView}
                    >
                      <AndpIcon
                        name={'arrowDown'}
                        size={formElementWrapperTheme.dropdownIcon.size}
                        color={
                          props.error
                            ? formElementWrapperTheme.dropdownIcon.colorError
                            : formElementWrapperTheme.dropdownIcon.color
                        }
                      />
                    </Pressable>
                  ) : null}
                </View>
              </View>
              {isList &&
                selectedValues &&
                selectedValues.length > 0 &&
                dropdownChipList()}
            </View>

            <Pressable ref={outFocusRef}></Pressable>
          </View>
        </Pressable>
        {Platform.OS === 'ios' ? (
          <View>
            <Host style={{
              position: 'absolute', width: width, pointerEvents: 'none'
            }}>
              <IosBottomSheet
                isPresented={isNativeSheetOpen}
                onIsPresentedChange={(e) => {
                  setIsNativeSheetOpen(e);
                }}
              >
                {onlyDropdown ? (
                  <Group modifiers={[presentationDetents([{ fraction: 0.35 }])]}>
                    <Host style={{ width: width, height: 250 }}>
                      <VStack>
                        <HStack modifiers={[padding({ horizontal: 20, top: 20, bottom: 10 })]}>
                          <Spacer />
                          <Button modifiers={[tint(theme.colors.colorAccent)]} label="Bestätigen" onPress={() => setIsNativeSheetOpen(false)} />
                        </HStack>
                        <Picker
                          selection={pickerSelectedIndex}
                          onSelectionChange={(selection) => {
                            const index = Number(selection);
                            if (dropdownData && dropdownData.length > 0) {
                              dropdownOnSelect && dropdownOnSelect(dropdownData[index]);
                            }
                          }}
                          modifiers={[pickerStyle('wheel')]}
                        >
                          {(dropdownData || []).map((item, index) => (
                            <IosText key={index} modifiers={[tag(index)]}>
                              {item.displayName}
                            </IosText>
                          ))}
                        </Picker>
                      </VStack>
                    </Host>
                  </Group>
                ) : (<>
                  <HStack modifiers={[padding({ horizontal: 20, top: 20, bottom: 10 })]}>
                    <IosText modifiers={[font({ weight: 'bold' })]}>{labelText != null && labelText.length && hideDropdownArrow ? labelText : 'Auswahl'}</IosText>
                    <Spacer />
                    {hideDropdownArrow ?
                      <Button modifiers={[tint(theme.colors.colorAccent)]} label="Bestätigen" onPress={() => setIsNativeSheetOpen(false)} /> :
                      <Button modifiers={[tint(theme.colors.colorDeleteRed)]} label="Cancel" onPress={() => {
                        setIsNativeSheetOpen(false);
                        clearInput && !value && clearInput();
                      }} />
                    }
                  </HStack>
                  <SafeAreaView style={{ height: height }}>
                    <View style={{ flex: 1, width: width }}>
                      <View style={{ paddingBottom: 18, paddingHorizontal: 15, borderBottomWidth: 0.5, borderBottomColor: '#dededeff' }}>
                        <AndpInputElement
                          ref={bottomSheetInputRef}
                          placeholder={
                            hideDropdownArrow
                              ? t('app_form_dropdown_autocomplete_placeholder')
                              : t('app_form_dropdown_search_placeholder')
                          }
                          value={inputValue}
                          onChangeText={onlyDropdown ? () => { } : onChangeInputValue}
                          onFocus={handleInputFocus}
                          style={{
                            height: 36,
                          }}
                          icon={hideDropdownArrow ? undefined : <AndpIcon name="search" size={20} color="#d2d2d2ff" />}
                        />
                      </View>
                      <Host style={{ flex: 1, width: '100%' }}>
                        <VStack modifiers={[padding({ all: 0 })]}>
                          {loading ? (
                            <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: 50 }}>
                              <ActivityIndicator
                                size={formElementWrapperTheme.loaderIcon.size}
                              />
                            </View>
                          ) : dropdownData && dropdownData.length > 0 ? (
                            <IosList modifiers={[listStyle('plain')]}>
                              {dropdownData.map((item, index) => {
                                const handlePress = () => {
                                  dropdownOnSelect && dropdownOnSelect(item);
                                  setIsNativeSheetOpen(false);
                                  !onlyDropdown && bottomSheetInputRef.current?.blur?.();
                                  LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
                                  onBlur && onBlur();
                                };
                                const isSelected = selectedValues && selectedValues.some((selectedItem) => selectedItem.identifier === item.identifier);

                                return (
                                  <Button key={index} onPress={handlePress}>
                                    <HStack spacing={20}>
                                      <IosText>{item.displayName}</IosText>
                                      {isSelected && <IosImage size={16} systemName={'checkmark'} />}
                                    </HStack>
                                  </Button>
                                );
                              })}
                            </IosList>
                          ) : <IosText modifiers={[foregroundColor(theme.colors.textMuted), padding({ vertical: 15 }), font({ size: 14 }), multilineTextAlignment('center')]}>Tippe mindestens 5 Zeichen um die Suche zu starten</IosText>}
                          <Spacer></Spacer>
                        </VStack>
                      </Host>
                    </View>
                  </SafeAreaView>
                  <Spacer /></>)}
              </IosBottomSheet>
            </Host>
          </View>
        ) :
          <BottomSheetModal
            snapPoints={snapPoints}
            enableOverDrag={false}
            onChange={handleSheetChanges}
            ref={bottomSheetModalRef}
            backgroundStyle={stylesBottom.bottomSheetBackground}
            enableDynamicSizing={false}
            backdropComponent={renderBackdrop}
          >
            <Text
              style={stylesBottom.headerLabel}
              numberOfLines={1}
              ellipsizeMode="tail"
              onPress={() => {
                !onlyDropdown && bottomSheetInputRef.current?.focus?.();
              }}
              selectable={false}
            >
              {labelText != null && labelText.length > 0 ? labelText : 'Auswahl'}
            </Text>

            {!onlyDropdown ? (
              <BottomSheetTextInput
                ref={bottomSheetInputRef}
                placeholder={
                  hideDropdownArrow
                    ? t('app_form_dropdown_autocomplete_placeholder')
                    : t('app_form_dropdown_search_placeholder')
                }
                value={inputValue}
                onChangeText={onlyDropdown ? () => { } : onChangeInputValue}
                onFocus={handleInputFocus}
                style={stylesBottom.bottomSheetSearchInput}
              />
            ) : null}
            {dropdownData && dropdownData.length > 0 && (
              <BottomSheetFlatList
                data={dropdownData}
                renderItem={customRenderItem}
                keyExtractor={(_: any, index: any) => index.toString()}
                scrollEnabled
                showsVerticalScrollIndicator={
                  dropdownInputTheme.dropdownList.showScrollbar
                }
                keyboardShouldPersistTaps="handled"
                keyboardDismissMode="on-drag"
                ItemSeparatorComponent={() => (
                  <View style={styles.dropdownItemsSeparator} />
                )}
                ListEmptyComponent={
                  loading ? (
                    <View style={stylesBottom.loadingState}>
                      <ActivityIndicator
                        size={formElementWrapperTheme.loaderIcon.size}
                        color={formElementWrapperTheme.loaderIcon.color}
                        accessibilityRole="image"
                        accessibilityLabel={t(
                          'form_accessibility_dropdown_loading',
                        )}
                      />
                    </View>
                  ) : (
                    <View style={stylesBottom.emptyState}>
                      <AndpText
                        style={stylesBottom.emptyStateText}
                        selectable={false}
                      >
                        {t('form_dropdown_no_results', 'No results')}
                      </AndpText>
                    </View>
                  )
                }
              />
            )}
          </BottomSheetModal>}
      </View>
    );
  },
);



export default BaseDropDownInputElement;
