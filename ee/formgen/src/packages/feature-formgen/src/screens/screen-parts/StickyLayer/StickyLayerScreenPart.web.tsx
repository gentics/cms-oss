import React, { useMemo } from 'react';
import { Platform, View } from 'react-native';
import { stickyLayerStyles } from '../../../styles/common/common-style';
import { stickyLayerStyles as customStickyLayerStyles } from '../../../styles/custom/custom-style';
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
  const footerButtons = buttons;


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
                  : () => {}
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
                <CancelButton {...props} translateKey={'form_cancel'} />
                {formState.schema.excelResultSupported && (
                  <ExpertExcelDownloadButton
                    t={t}
                    onPress={props.handleExpertExcelDownload!}
                    disabled={props.uiState.loading}
                    uiState={props.uiState}
                  />
                )}
                <ExpertSearchButton
                  t={t}
                  onPress={() =>
                    props.handleExpertSearch
                      ? props.handleExpertSearch()
                      : () => {}
                  }
                  disabled={props.uiState.loading}
                />
              </>
            ) : isDialogFormSubmit ? (
              <>
                <CancelButton {...props} translateKey={'form_cancel'} />
                <SubmitButton {...props} />
              </>
            ) : (
              renderButtons(footerButtons, { ...props })
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
