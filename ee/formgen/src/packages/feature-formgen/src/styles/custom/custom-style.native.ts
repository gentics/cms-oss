import { StyleSheet } from 'react-native';

const baseStyles = StyleSheet.create({
  formContainer: {},
  formContainerScreenMaxMedium: {},
  row: {},
  h1: {},
  h2: {},
  h3: {},
  h4: {},
  h5: {},
  h6: {},
  p: {},
  ul: {},
  ol: {},
  li: {},
  a: {},
  blockquote: {},
  code: {},
  pre: {},
  hr: {},
  b: {},
  strong: {},
  i: {},
  em: {},
  u: {},
});

const utilStyles = StyleSheet.create({
  visualyHidden: {},
});

const summaryStyles = StyleSheet.create({
  sectionHeader: {},
  section: {},
  summaryContainer: {}
});

const successStyles = StyleSheet.create({
  successText: {},
  successIconBadge: {},
  successIcon: {},
  buttonWrapper: {},
  buttonWrapperScreenMaxSmall: {},
})


const formElementWrapperStyles = StyleSheet.create({
  wrapper: {},
  formGroup: {},
  tooltipIcon: {},
  tooltipContainer: {},
  tooltipText: {},
  toolTipContent: {},
  toolTip: {},
  bottomMessages: {},
  needSpacing: {},
  inputItemsRight: {},
  validInputIcon: {},
  deleteContentBtnIcon: {},
  anFieldLoadErrorIcon: {},
  formElementWrapperLoader: {},
});

const aggregateElementStyles = StyleSheet.create({
  mobileCard: {},
  mobileCardHover: {},
  mobileFieldRow: {},
  mobileFieldLabel: {},
  mobileFieldValue: {},
  mobileValidation: {},
  mobileActionsRow: {},
  mobileActionButton: {},
  uploadButton: {},
  uploadButtonText: {},
  tableRowHover: {},
  container: {},
  appliedFiltersContainer: {},
  noEntriesContainer: {},
  noEntriesText: {},
  filterDropdownContainer: {},
  filterInputContainer: {},
  filterInputAndBtnContainer: {},
  filterInputFocused: {},
  filterPlaceholder: {},
  filterInput: {},
  tableHeader: {},
  tableHeaderCell: {},
  tableRow: {},
  tableCell: {},
  borderLine: {},
  tableAggHeaderText: {},
  tableAggActionButton: {},
  deleteButton: {},
  addButton: {},
  editButton: {},
  copyButton: {},
  actionButtons: {},
  aggAddButtonContainer: {},
  uploadButtonContainer: {},
  uploadButtonOnHover: {},
  uploadButtonTextOnHover: {},
  expandedAggregateItemIdicator: {},
  singleAggregateContainer: {},
  aggToggleDetailButton: {},
  aggToggleDetailButtonOnHover: {},
  aggToggleDetailButtonActive: {},
  aggToggleDetailButtonText: {},
  aggToggleDetailButtonTextOnHover: {},
  aggToggleDetailButtonTextActive: {},
  aggToggleDetailButtonIcon: {},
  aggToggleDetailButtonIconOnHover: {},
  aggToggleDetailButtonIconActive: {},
  aggToggleDetailButtonSmallView: {},
  aggToggleDetailButtonTextSmallView: {},
  aggDeleteButton: {},
  aggDeleteButtonOnHover: {},
  aggDeleteButtonContent: {},
  aggDeleteButtonContentOnHover: {},
  aggDeleteButtonMobile: {},
  aggDeleteButtonMobileContent: {},
  aggAddButton: {},
  aggAddButtonOnHover: {},
  aggAddButtonContent: {},
  aggAddButtonContentOnHover: {},
  aggAddButtonMobile: {},
  aggAddButtonMobileContent: {},
  singleDetailAggregateContainerMobile: {},
  filter: {},
  filterSettingsContainer: {},
  filterSettings: {},
  filterSettingsMobile: {},
  filterDivider: {},
  appliedFilterCard: {},
  appliedFilterCardTypeText: {},
  appliedFilterCardText: {},
  aggAddFilterButton: {},
  aggAddFilterPressable: {},
  addFilterButtonActive: {},
  addFilterButtonActiveOnHover: {},
  mobileCardList: {},
  mobileActionButtonContent: {},
  mobileDeleteButton: {},
  mobileDeleteButtonContent: {},
  aggVarAddButtonMobile: {},
  aggVarAddButton: {},
  paginationContainer: {},
  pageItem: {},
  pageItemText: {},
  activePageItem: {},
  activePageItemText: {},
  disabledButton: {},
  disabledButtonText: {},
  breakContainer: {},
  breakText: {},
});

const dateTimeElementStyles = StyleSheet.create({
  // Styling des Pickers befindet sich unter ./custom-datepicker.css
  datePickerInput: {},
  disabledDatePickerInput: {},
  focusedDatePickerInput: {},
  errorDatePickerInput: {},
  datePickerInputText: {},
  datePickerLabel: {},
  errorDatePickerLabel: {},
});

const binaryElementStyles = StyleSheet.create({
  srOnly: {},
  fileChooser: {},
  fileChooserLabel: {},
  fileChooserLabelError: {},
  fileChooserLabelDisabled: {},
  fileChooserWithFiles: {},
  disabledFileChooser: {},
  errorFileChooser: {},
  fileContainer: {},
  fileList: {},
  fielListItem: {},
  fileListItemLinkText: {},
  fileListItemText: {},
});

const binaryFileCardStyles = StyleSheet.create({
  card: {},
  cardShadow: {},
  cardShadowHover: {},
  cardmobile: {},
  cardHovered: {},
  header: {},
  headerActions: {},
  iconButton: {},
  deleteIcon: {},
  downloadIcon: {},
  bodyThumbnailContainer: {},
  bodyIconContainer: {},
  bodyIcon: {},
  previewLabel: {},
  footer: {},
  fileName: {},
  fileType: {},
});

const baseDropDownInputElement = StyleSheet.create({
  dropdown: {},
  dropdownInputContainer: {},
  dropdownPlaceholderLabel: {},
  placeholderLabelError: {},
  inputElement: {},
  inputField: {},
  errorContainer: {},
  dropdownItemsSeparator: {},
  dropdownOption: {},
  dropdownOptionError: {},
  chipsWrapper: {},
  chip: {},
  chipText: {},
  errorChip: {},
  errorText: {},
  disabledInput: {},
  disabledDropDown: {},
  iconsContainer: {},
  iconView: {}
});

const customBaseDropDownBottomSheet = StyleSheet.create({
  bottomSheetBackground: {},
  headerLabel: {},
  bottomSheetSearchInput: {},
  emptyState: {},
  emptyStateText: {},
  loadingState: {},
});


const autocompleteInputElementStyle = StyleSheet.create({
  dropdownListItemText: {},
  dropdownListItemTextError: {}
})

const dropdownLazyCatalogElementStyles = StyleSheet.create({
  dropdownListItemText: {},
  activityText: {},
  activityTextError: {},
  dropdownListItemTextError:{}
})

const dropdownCatalogElementStyles = StyleSheet.create({
  label: {},
  picker: {},
  container: {},
  input: {},
  suggestionsContainer: {},
  suggestionItemContainer: {},
  suggestionItem: {},
  selectedItemsContainer: {},
  chip: {},
  chipText: {},
  chipRemove: {},
});

const booleanElementStyles = StyleSheet.create({
  radioButtonGroupContainer: {},
  radioButtonGroupLabel: {},
  radioButtonGroupLabelError: {},
  radioButtonGroup: {},
  radioButtonRow: {},
  booleanElementContainer: {},
  disabledBooleanElementContainer: {},

  // -------- Radio Button ----------
  radioButtonElement: {},
  radioButtonDot: {},
  selectedRadioButton: {},
  errorRadioButton: {},
  errorRadioButtonDot: {},
  focusedRadioButton: {},
  radioButtonLabel: {},
  selectedRadioButtonLabel: {},
  errorRadioButtonLabel: {},
  focusedRadioButtonLabel: {},

  // -------- Checkbox --------------
  checkboxElement: {},
  selectedCheckbox: {},
  errorCheckbox: {},
  selectedErrorCheckbox: {},
  focusedCheckbox: {},
  checkboxLabel: {},
  selectedCheckboxLabel: {},
  errorCheckboxLabel: {},
  focusedCheckboxLabel: {},

  // -------- Boolean Button --------
  booleanButton: {},
  booleanButtonDisabled: {},
  booleanButtonError: {},
  booleanButtonText: {},
  booleanButtonTextDisabled: {},
  booleanButtonTextError: {},
  booleanButtonFocused: {},
});

const inputElementStyles = StyleSheet.create({
  input: {},
  inputError: {},
  disabledInput: {},
  inputFocused: {},
  placeholder: {},
  placeholderError: {},
  placeholderFocused: {},
  inputNumber: {},
  inputNumberWithSpace: {},
  numberBtns: {},
  numberBtnUp: {},
  numberBtnDown: {},
  unitBadge: {},
  unitBadgeError: {},
  unitBadgeLeft: {},
  unitBadgeText: {},
  unitBadgeTextError: {},
  inputAreaFocused: {},
  textarea: {},
});

const pagingStyles = StyleSheet.create({
  formPagingContainer: {},
  dialogFormHeader: {},
  formPagingContainerScreenMaximumMedium: {},
  pagination: {},
  pageHeaderContainer: {},
  pageLabel: {},
  pageLabelContainerMaximumMedium: {},
  pageLabelScreenMaximumMedium: {},
  pageTitle: {},
  pageHeaderHyphenText: {},
  pageTitleScreenMaxiumMedium: {},
  pageItem: {},
  currentPageItem: {},
  disabledPageItem: {},
  hasErrorPageItem: {},
  number: {},
  currentNumber: {},
  disabledNumber: {},
  hasErrorNumber: {},
  dashBetweenSteps: {},
  doneDashBetweenSteps: {},
  hasErrorDashBetweenSteps: {}
});

const stickyLayerStyles = StyleSheet.create({
  combinedStickyLayerContainer: {},
  stickylayerContainer: {},
  buttonContainerWrapper: {},
  buttonContainer: {},
  buttonContainerScreenMaxSmall: {},
  expertSearchCheckMarksContainer: {},
  expertSearchCheckMarksMultiButtonContainer: {},
});

const buttonStyles = StyleSheet.create({
  smallBtn: {},
  smallText: {},
  btnWrapperScreenMaxSmall: {},
  btnWrapper: {},
  btnDialogFormBack: {},
  btn: {},
  btnText: {},
  btnPrimary: {},
  btnPrimaryHover: {},
  btnPrimaryText: {},
  btnPrimaryTextHover: {},
  btnPrimaryDisabled: {},
  btnSecondary: {},
  btnSecondaryText: {},
  btnSecondaryHover: {},
  btnSecondaryTextHover: {},
  btnSecondaryDisabled: {},
  btnDialogFormBackStyle: {},
  btnDialogFormBackText: {},
  btnDialogFormBackStyleHover: {},
  btnDialogFormBackTextHover: {},
});

//ExpertSearchStyle
const expertSearchStyles = StyleSheet.create({
  container: {},
  cardContainer: {},
  tableContainer: {},
  scroll: {},
  headerRow: {},
  headerSelect: {},
  headerCell: {},
  headerPressable: {},
  headerText: {},
  sortIcon: {},
  resizer: {},
  row: {},
  rowSelect: {},
  cell: {},
  buttonText: {},
  card: {},
  cardContainerHeader: {},
  cardContainerHeaderFirstColumnText: {},
  cardSelectAllHeader: {},
  cardSelectAllHeaderText: {},
  cardSelectAllHeaderIcon: {},
  cardHeader: {},
  cardTitle: {},
  cardDivider: {},
  cardButton: {},
  cardButtonText: {},
  cardRow: {},
  cardLabel: {},
  cardValue: {},
  emptyText: {},
});

const errorBoxStyles = StyleSheet.create({
  errorBoxContainer: {},
  errorBoxDescription: {},
  errorBoxItem: {},
  errorIcon: {},
  errorText: {},
  errorBoxDescriptionContainer: {},
  errorBoxWidthContainer: {},
  errorBoxListWidth: {},
  errorBoxList: {},
});

const defaultOverlayStyles = StyleSheet.create({
  modal: {},
  modalContent: {},
  modalContentWithoutFooter: {},
  modalContentScreenMaxLarge: {},
  modalContentScreenMaxMedium: {},
  modalContentScreenMaxSmall: {},
  body: {},
  bodyScrollView: {},
  bodyText: {},
});

const overlayButtonRowStyles = StyleSheet.create({
  buttonRow: {},
  buttonWrapper: {},
  buttonWrapperScreenMaxSmall: {}
});

const overlayFooterStyles = StyleSheet.create({
  footer: {},
  footerText: {},
});

const overlayHeaderStyles = StyleSheet.create({
  headerContent: {},
  headerIcon: {},
  headerTitle: {},
  closeButton: {},
  closeButtonIcon: {},
});

const overlayPreConditionStyles = StyleSheet.create({
  bodyAlign: {},
  preconditionItem: {},
  preconditionItemLink: {},
  preconditionFirstLineText: {},
  preconditionSecondLineText: {},
});

const debugModeSectionStyles = StyleSheet.create({
  section: {},
  inlineRow: {},
  headerRow: {},
  sectionLabel: {},
  contentText: {},
  scrollableArea: {},
  textarea: {},
  copyButton: {},
  copyButtonText: {},
});

const fullScreenOverlayStyles = StyleSheet.create({
  loadingOverlay: {},
});

const formWrapperLabelStyles = StyleSheet.create({
  labelContainer: {},
  labelText: {},
  labelTextError: {},
});

const formGridImageElementStyles = StyleSheet.create({
  container: {},
  caption: {},
});

const recaptchaElementStyles = StyleSheet.create({
  button: {},
  buttonVerified: {},
  buttonText: {}
});

export {
  summaryStyles,
  baseStyles,
  utilStyles,
  formElementWrapperStyles,
  pagingStyles,
  stickyLayerStyles,
  buttonStyles,
  errorBoxStyles,
  inputElementStyles,
  booleanElementStyles,
  baseDropDownInputElement,
  customBaseDropDownBottomSheet,
  autocompleteInputElementStyle,
  dropdownLazyCatalogElementStyles,
  dropdownCatalogElementStyles,
  binaryElementStyles,
  binaryFileCardStyles,
  dateTimeElementStyles,
  aggregateElementStyles,
  recaptchaElementStyles,
  defaultOverlayStyles,
  overlayButtonRowStyles,
  overlayFooterStyles,
  overlayHeaderStyles,
  overlayPreConditionStyles,
  debugModeSectionStyles,
  fullScreenOverlayStyles,
  expertSearchStyles,
  formWrapperLabelStyles,
  successStyles,
  formGridImageElementStyles,
};
