import React, { useMemo } from 'react';
import { StyleSheet, View } from 'react-native';
import AndpButton from '@andp/core/src/components/AndpButton';
import { useButtons } from '../../../essentials/ButtonProvider';
import { ButtonComponents } from '../../../../custom/ButtonComponentsTypes';
import {
    FormState,
    UIState,
    IFormFlowButtonDefinition,
} from '../../../../types/interfaces';
import { RestService } from '../../../../services/RestService';
import { StatesToUpdateEnum } from '../../../../constants/enums/StateToUpdateEnum';
import { pagingStyles } from '../../../../styles/common/common-style.native';
import { pagingStyles as customPagingStyles } from '../../../../styles/custom/custom-style.native';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
interface FormHeaderProps {
    formTitle: string;
    styles: any;
    t: any;
    uiState: UIState;
    formState: FormState;
    restService: RestService;
    updateState: (state: StatesToUpdateEnum) => void;
    handleFormCancel?: (cancelDef?: IFormFlowButtonDefinition) => Promise<void>;
}

const FormHeader: React.FC<FormHeaderProps> = (props) => {
    const { formTitle, t, uiState, formState, restService, updateState, handleFormCancel } = props;
    const { buttons } = useButtons();

    // isCancel left or right?
    const cancelButtonLeft = true;

    const cancelDef = buttons.find(btn => btn.translateKey === 'form_cancel');
    const detailInfoDef = buttons.find(btn => btn.translateKey === 'form_detailinfo');
    const styles = useMemo(() => mergeStyles(pagingStyles, customPagingStyles), []);

    const CancelButton = () => {
        return (
            <View>{renderButtons(cancelDef ? [cancelDef] : [], props)}</View>
        );
    }

    const DetailInfoButton = () => {
        return detailInfoDef ? (
            <View style={{ minWidth: 30 }}>
                {renderButtons(detailInfoDef ? [detailInfoDef] : [], props)}
            </View>
        ) : null;
    }
    return (
        <View
            style={{
                flexDirection: 'row',
                alignItems: 'center',
                justifyContent: 'space-between',
            }}
        >
            <View style={{ minWidth: 30 }}>
                {cancelButtonLeft ? CancelButton() : DetailInfoButton()}
            </View>
            <View style={[localStyles.formTitle, { flex: 1 }]}>
                <AndpText size={18} style={{ fontWeight: "bold", textAlign: "center" }}>{formTitle}</AndpText>
            </View>
            <View style={{ minWidth: 30 }}>
                {!cancelButtonLeft ? CancelButton() : DetailInfoButton()}
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
                mockForm={additionalProps.isMock}
                {...additionalProps}
            />
        );
    });
}

const localStyles = StyleSheet.create({
    formTitle: {
        justifyContent: "center",
    }
});


export default FormHeader;
