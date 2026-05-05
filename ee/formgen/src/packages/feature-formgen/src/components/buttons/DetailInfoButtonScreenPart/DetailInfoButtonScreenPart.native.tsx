import React, { useMemo, useState } from 'react';
import { Platform, View, Pressable, TouchableOpacity } from 'react-native';
import { buttonStyles } from '../../../styles/common/common-style.native';
import { buttonStyles as customButtonStyles } from '../../../styles/custom/custom-style.native';
import { FormState, IFormFlowButtonDefinition, UIState } from '../../../types/interfaces';
import { TFunction } from 'i18next';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { getFeatureActive, getI18nTextStr, isFeatureActive } from '../../../utils/Utils';
import { handleBackgroundFormSave } from '../../../utils/FormEventsUtils';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import theme from '@andp/core/src/styles/theme.native';

interface DetailInfoButtonProps {
    formState: FormState;
    uiState: UIState;
    restService: any;
    updateState: (state: any) => void;
    mockForm?: boolean;
    t: TFunction;
    screenType: string;
    translateKey: string;
    buttonDefinition: IFormFlowButtonDefinition;
    isLoggedIn?: boolean;
}

const DetailInfoButtonScreenPart: React.FC<DetailInfoButtonProps> = ({
    formState,
    uiState,
    restService,
    updateState,
    mockForm,
    t,
    translateKey,
    buttonDefinition,
    isLoggedIn
}) => {
    const styles = useMemo(() => mergeStyles(buttonStyles, customButtonStyles), [])
    const isMock = mockForm ? mockForm : uiState.isMockForm;
    // const svcActive = isFeatureActive("feature_servicecatalog", formState.features);
    // if (!svcActive && !mockForm) return null;
    /**
    * feature_servicecatalog
    *   detailPage: true/false
    */
    let hasDetail = formState.hasDetail === true;
    let detailsUrl = formState.detailUrl || "";

    if (!hasDetail && !isMock) return null;

    if (isMock) {
        detailsUrl = "http://portal.localhost.gentics.com"; // MockUrl, to test with MockForm
    }

    const [hover, setHover] = useState(false);
    const breakPoints = useBreakpoints();

    function redirectTo(url: string) {
        if (uiState.linkCallback) {
            uiState.linkCallback(url);
        }
    }



    const detailUrlOnClick = function () {
        if (!detailsUrl || detailsUrl === "") {
            detailsUrl = "/";
        }
        handleBackgroundFormSave(
            formState,
            uiState,
            updateState,
            restService,
            () => redirectTo(detailsUrl),
        );
    };

    return (
        <TouchableOpacity
            style={{
                width: 32,
                height: 32,
                backgroundColor: theme.colors.backgroundAccent,
                borderRadius: 10,
                justifyContent: 'center',
                alignItems: 'center'
            }}
            activeOpacity={0.7}
            onPress={() => detailUrlOnClick()}

        >
            <AndpIcon name='info' color={theme.colors.colorAccent}></AndpIcon>
        </TouchableOpacity>
    );
};

export default DetailInfoButtonScreenPart;