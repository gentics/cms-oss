import React, { useMemo, useState } from 'react';
import { Platform, View, Pressable } from 'react-native';
import { buttonStyles } from '../../../styles/common/common-style';
import { buttonStyles as customButtonStyles } from '../../../styles/custom/custom-style';
import { FormState, IFormFlowButtonDefinition, UIState } from '../../../types/interfaces';
import { TFunction } from 'i18next';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { getFeatureActive, getI18nTextStr, isFeatureActive } from '../../../utils/Utils';
import { handleBackgroundFormSave } from '../../../utils/FormEventsUtils';

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
    if (Platform.OS !== 'web' && buttonDefinition.mobileDisabled) return null;

    const svcActive = isFeatureActive("feature_servicecatalog", formState.features);
    if (!svcActive && !mockForm) return null;
    /**
    * feature_servicecatalog
    *   detailPage: true/false
    */
    let hasDetail = false;
    let detailsUrl = "";

    if (svcActive) {
        const servicecatalog = getFeatureActive("feature_servicecatalog", formState.features);
        const detailPageEnabled =
            servicecatalog?.hasOwnProperty("detailPage") &&
            ((servicecatalog as any).detailPage === true || (servicecatalog as any).detailPage === "true");

        if (detailPageEnabled && Platform.OS === "web") {
            const aside = document.getElementById("egov-template--aside");
            hasDetail = aside?.dataset.hasdetail === "true";
            detailsUrl = aside?.dataset.detailurl ?? "";
        }
    }

    if (!hasDetail && !mockForm) return null;

    if(mockForm){
        detailsUrl = "http://portal.localhost.gentics.com"; // MockUrl, to test with MockForm
    }

    const [hover, setHover] = useState(false);
    const breakPoints = useBreakpoints();

    function redirectTo(url: string) {
        window.location.href = url;
    }

    const detailUrlOnClick = function () {
        if (!detailsUrl || detailsUrl === "") {
            detailsUrl = "/";
        }
        if (isLoggedIn) {
            handleBackgroundFormSave(
                formState,
                uiState,
                updateState,
                restService,
                () => redirectTo(detailsUrl),
            );
        } else {
            redirectTo(detailsUrl);
        }
    };

    return (
        <View
            style={[
                styles.btnWrapper,
                breakPoints.maximumSm && styles.btnWrapperScreenMaxSmall,
            ]}
        >
            <Pressable
                id={"sticky-bar-detail-info-btn"}
                disabled={uiState.loading}
                onPress={() => detailUrlOnClick()}
                onHoverIn={() => setHover(true)}
                onHoverOut={() => setHover(false)}
                style={[
                    styles.btn,
                    styles.btnSecondary,
                    breakPoints.maximumXl && styles.smallBtn,
                    hover && styles.btnSecondaryHover,
                ]}
            >
                <AndpText
                    selectable={false}
                    numberOfLines={2}
                    ellipsizeMode="tail"
                    style={[
                        styles.btnText,
                        styles.btnSecondaryText,
                        breakPoints.maximumMd && styles.smallText,
                        hover && styles.btnSecondaryTextHover,
                    ]}
                >
                    {t(translateKey)}
                </AndpText>
            </Pressable>
        </View>
    );
};

export default DetailInfoButtonScreenPart;