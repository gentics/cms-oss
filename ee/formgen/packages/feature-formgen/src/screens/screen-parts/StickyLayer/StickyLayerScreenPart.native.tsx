import React, { useMemo, useState } from 'react';
import { Text, TouchableOpacity, View, StyleProp, ViewStyle } from 'react-native';
import Animated, { Easing, useSharedValue, useAnimatedStyle, withTiming } from 'react-native-reanimated';
import { stickyLayerStyles } from '../../../styles/common/common-style.native';
import { stickyLayerStyles as customStickyLayerStyles } from '../../../styles/custom/custom-style.native';
import { IFormFlowButtonDefinition, IFormPageComponent } from '../../../types/interfaces';
import { ButtonComponents } from '../../../custom/ButtonComponentsTypes';
import {
  CancelButton,
  SubmitButton,
} from '../../../custom/ButtonComponentDefinitions';
import ExpertExcelDownloadButton from '../../../components/buttons/ExpertExcelDownloadButton';
import ExpertSearchButton from '../../../components/buttons/ExpertSearchButton';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import ExpertSearchMultipleButtons from '../../../components/buttons/ExpertSearchMultipleDetailsButtons';
import { widthFromColumNumber } from '@andp/core/src/AndpUtils';
import { useButtons } from '../../essentials/ButtonProvider';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import theme from '@andp/core/src/styles/theme.native';

interface IContextMenuMultiButtonProps {
  triggerLabel?: string;
  triggerIcon?: React.ReactNode;
  children: React.ReactNode;
  style?: StyleProp<ViewStyle>;
}

const ContextMenuMultiButton: React.FC<IContextMenuMultiButtonProps> = ({ triggerLabel, triggerIcon, children, style }) => {
  const [menuVisible, setMenuVisible] = useState(false);
  const translateX = useSharedValue(-200);
  const opacity = useSharedValue(0);
  const scale = useSharedValue(0.8);

  // Only render trigger/menu if children is not empty
  const hasChildren = React.Children.count(children) > 0;
  if (!hasChildren) return null;

  const toggleMenu = () => {
    if (menuVisible) {
      // Animate out
      opacity.value = withTiming(0, { duration: 200, easing: Easing.in(Easing.exp) });
      translateX.value = withTiming(-200, {
        duration: 300,
        easing: Easing.in(Easing.exp),
      });
      scale.value = withTiming(0.8, { duration: 300, easing: Easing.in(Easing.exp) });
      setTimeout(() => setMenuVisible(false), 100); // Hide after fade out
    } else {
      setMenuVisible(true);
      translateX.value = withTiming(0, {
        duration: 300,
        easing: Easing.out(Easing.exp),
      });
      opacity.value = withTiming(1, { duration: 200, easing: Easing.out(Easing.exp) });
      scale.value = withTiming(1, { duration: 300, easing: Easing.out(Easing.exp) });
    }
  };

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: translateX.value }, { scale: scale.value }],
    opacity: opacity.value,
  }));

  return (
    <>
      <TouchableOpacity onPress={toggleMenu} style={{ padding: 10, backgroundColor: theme.colors.backgroundAccent, alignContent: "center", justifyContent: "center", borderRadius: 8, }}>
        {triggerIcon ? triggerIcon : <Text>{triggerLabel}</Text>}
      </TouchableOpacity>
      {menuVisible && (
        <Animated.View
          style={[
            {
              position: 'absolute',
              top: -75,
              left: 5,
              backgroundColor: '#fff',
              padding: 10,
              borderTopLeftRadius: 12,
              borderTopRightRadius: 12,
              borderBottomLeftRadius: 12,
              borderBottomRightRadius: 12,
              borderWidth: 1,
              borderColor: '#ddd',
            },
            style,
            animatedStyle,
          ]}
        >
          {children}
        </Animated.View>
      )}
    </>
  );
};

interface IStickyLayerProps extends IFormPageComponent {
  handleExpertSearch?: (callback?: any) => void;
  handleExpertExcelDownload?: () => void;
  handleExpertMultiple?: (buttonID: string) => void;
  handleFormCancel?: (
    callback: any,
    isOnlyCloseDialog?: boolean,
  ) => Promise<void>;
  handleSubmitDialogForm: () => void;
  disabled?: boolean;
  printRef: any;
}

const StickyLayerScreenPart: React.FC<IStickyLayerProps> = (props) => {
  const { t, uiState, formState } = props;
  const isExpertSearch = uiState.isExpertSearch;
  const isDialogFormSubmit = props.uiState.dialogForm.isDialogFormOpen;
  const breakPoints = useBreakpoints();
  const styles = useMemo(() => mergeStyles(stickyLayerStyles, customStickyLayerStyles), []);
  const { buttons } = useButtons();
  const hasPages = !!formState.form.pages?.length;

  // On Mobile, exclude form_cancel and detail_info

  const footerButtons = buttons.filter(
    b => !["form_cancel", "form_detailinfo"].includes(b.translateKey)
  );

  // Determine arrangement for normal forms
  const hasManyFooterButtons = footerButtons.length > 2;

  // detect Prev/Next/Submit by component name or translateKey
  const prevDef = footerButtons.find(
    (b) =>
      /prev/i.test(b.btnComponent || '') || /prev/i.test(b.translateKey || '')
  );
  const nextDef = footerButtons.find(
    (b) =>
      /next/i.test(b.btnComponent || '') || /next/i.test(b.translateKey || '')
  );
  const submitDef = footerButtons.find(
    (b) =>
      /submit/i.test(b.btnComponent || '') || /submit/i.test(b.translateKey || '')
  );
  const otherFooterButtons = footerButtons.filter(
    (b) => b !== prevDef && b !== nextDef && b !== submitDef && b.mobileDisabled == false
  );

  // Abstracting the ExpertExcelDownloadButton button to avoid duplicate code
  const excelButton = formState.schema.excelResultSupported ? (
    <ExpertExcelDownloadButton
      t={t}
      onPress={props.handleExpertExcelDownload!}
      disabled={props.uiState.loading}
      uiState={props.uiState}
    />
  ) : null;

  return (
    <View style={styles.combinedStickyLayerContainer}>
      {isExpertSearch &&
        formState.expertSearchCheckMarkCount &&
        formState.expertSearchCheckMarkCount > 0 ? (
        <View style={styles.stickylayerContainer}>
          {formState.data.reportResult?.headerColumns.some(
            (c) => c.isMultiSelect == true,
          ) && (
              <ExpertSearchMultipleButtons
                t={t}
                onPress={
                  formState.expertMultipleHandler
                    ? formState.expertMultipleHandler
                    : () => { }
                }
                disabled={props.uiState.loading}
                uiState={props.uiState}
                formState={props.formState}
                isMobile={true}
              />
            )}
        </View>
      ) : null}
      <View style={styles.stickylayerContainer}>
        <View
          style={[
            styles.buttonContainerWrapper,
            widthFromColumNumber(props.formState.form.formwidth || 8, breakPoints)
          ]}
        >
          <View
            style={[
              styles.buttonContainer,
              breakPoints.maximumSm &&
              styles.buttonContainerScreenMaxSmall,
            ]}
          >
            {isExpertSearch ? (
              <>
                {hasPages
                  ? excelButton && (
                      <ContextMenuMultiButton
                        triggerIcon={
                          <AndpIcon
                            size="md"
                            name="bars"
                            color={theme.colors.colorAccent}
                          />
                        }
                      >
                        {excelButton}
                      </ContextMenuMultiButton>
                    )
                  : excelButton}
                {hasPages && (
                  <ExpertSearchButton
                    t={t}
                    onPress={() => props.handleExpertSearch?.()}
                    disabled={props.uiState.loading}
                  />
                )}
              </>
            ) : isDialogFormSubmit ? (
              <>
                {/* Only the submit button for dialog form submit */}
                <SubmitButton {...props} />
              </>
            ) : (
              hasManyFooterButtons ? (
                <>
                  {otherFooterButtons.length > 0 ? (
                    <ContextMenuMultiButton triggerIcon={<AndpIcon size="md" name="bars" color={theme.colors.colorAccent} />}>
                      {renderButtons(otherFooterButtons, { ...props })}
                    </ContextMenuMultiButton>
                  ) : null}

                  {/* Then Prev and Next (if present) */}
                  {prevDef ? renderButtons([prevDef], { ...props }) : null}
                  {nextDef ? renderButtons([nextDef], { ...props }) : null}
                  {submitDef ? renderButtons([submitDef], { ...props }) : null}
                </>
              ) : (
                renderButtons(footerButtons, { ...props })
              )
            )}
          </View>
        </View>
      </View>
    </View>
  );
};

function renderButtons(buttonDefinition: IFormFlowButtonDefinition[], additionalProps: any = {}) {
  return buttonDefinition.map((buttonDef, index) => {
    const ButtonComponent = ButtonComponents[buttonDef.btnComponent];
    if (!ButtonComponent) {
      console.warn(`Unbekannte Button-Komponente: ${buttonDef.btnComponent}`);
      return null;
    }
    return (
      <ButtonComponent
        key={index}
        translateKey={buttonDef.translateKey}
        buttonDefinition={buttonDef}
        {...additionalProps}
      />
    );
  });
}

export default StickyLayerScreenPart;
