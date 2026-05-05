import { ICommonOverlayProps, IOverlayExtraCommonProps, OverlayButtonDefinition } from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';

const ExpertSearchTableOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const additionalProps: IOverlayExtraCommonProps = props.uiState.overlayState
        .additionalProps as IOverlayExtraCommonProps;

  let buttonList: OverlayButtonDefinition[] = [
      {
        key: 'expert-search-table-result-limit-overlay-excel-download-btn',
        title: t('form_expertDownloadAsExcel'),
        onPress: () => props.handleExpertExcelDownload && props.handleExpertExcelDownload(),
        loading: props.uiState.loading
      }
    ];

  return (
    <DefaultOverlay
      {...props}
      title={t('form_OverlayExpertSearchResultTooBig_title')}
      contentMessages={[t('form_OverlayExpertSearchResultTooBig_text')]}
      buttonList={buttonList}
      closeAble={true}
      debugInfo={(additionalProps && additionalProps.response) || undefined}
    />
  );
};

export default ExpertSearchTableOverlay;
