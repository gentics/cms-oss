import { Platform, StyleSheet } from 'react-native';
import { Theme } from '../../types/interfaces';
import {
  theme,
  buttonsTheme,
  overlaysTheme,
  baseStylesTheme,
  formElementWrapperTheme,
  paginationTheme,
  errorBoxTheme,
  dropdownInputTheme,
  binaryInputTheme,
  booleanElementTheme,
  inputElementTheme,
  dateTimeElementTheme,
  standardElement,
  formGridImageElementTheme,
} from '@andp/core/src/styles/theme';
import {
  aggregateElementTheme,
  expertSearchTableTheme,
  summaryTheme,
} from '@andp/core/src/styles/theme.web';

//+---------------------------------+
//|---------Base Styling------------|
//+---------------------------------+

const baseTheme: Theme = {
  container: {
    flexGrow: 1,
    padding: 20,
    backgroundColor: '#f4f4f4',
  },
  screenTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
};

const baseStyles = StyleSheet.create({
  formContainer: {
    marginHorizontal: 'auto',
  },
  formContainerScreenMaxMedium: {
    flex: 1,
    width: 'auto',
    marginHorizontal: 15,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  h1: {
    fontFamily: theme.fontFamily.bold,
    fontSize: 28,
    color: theme.colors.textPrimary,
    marginBottom: 12,
  },
  h2: {
    fontFamily: theme.fontFamily.semiBold,
    fontSize: 24,
    color: theme.colors.textPrimary,
    marginBottom: 10,
  },
  h3: {
    fontFamily: theme.fontFamily.medium,
    fontSize: 20,
    color: theme.colors.textPrimary,
    marginBottom: 8,
  },
  h4: {
    fontFamily: theme.fontFamily.medium,
    fontSize: 18,
    color: theme.colors.textPrimary,
    marginBottom: 6,
  },
  h5: {
    fontFamily: theme.fontFamily.regular,
    fontSize: 16,
    color: theme.colors.textPrimary,
    marginBottom: 4,
  },
  h6: {
    fontFamily: theme.fontFamily.regular,
    fontSize: 14,
    color: theme.colors.textPrimary,
    marginBottom: 2,
  },
  p: {
    fontFamily: theme.fontFamily.regular,
    fontSize: theme.fontSize.regular,
    color: theme.colors.textPrimary,
    marginBottom: 10,
  },
  ul: {
    marginBottom: 10,
  },
  ulItem: {},
  ol: {
    marginBottom: 10,
  },
  olItem: {},
  li: {
    fontFamily: theme.fontFamily.regular,
    fontSize: theme.fontSize.regular,
    color: theme.colors.textPrimary,
    marginBottom: 4,
  },
  a: {
    color: theme.colors.colorAccent,
    textDecorationLine: 'underline',
  },
  blockquote: {
    fontStyle: 'italic',
    color: '#666',
    borderLeftWidth: 4,
    borderLeftColor: '#ccc',
    paddingLeft: 10,
    marginBottom: 10,
  },
  code: {
    fontFamily: 'monospace',
    backgroundColor: '#f4f4f4',
    padding: 4,
    borderRadius: 4,
  },
  pre: {
    fontFamily: 'monospace',
    backgroundColor: '#f4f4f4',
    padding: 10,
    marginBottom: 10,
    borderRadius: 6,
  },
  hr: {
    borderBottomWidth: 1,
    borderBottomColor: '#ccc',
    marginVertical: 20,
  },
  b: {
    fontFamily: theme.fontFamily.bold,
  },
  strong: {
    fontFamily: theme.fontFamily.bold,
  },
  i: {
    fontStyle: 'italic',
  },
  em: {
    fontStyle: 'italic',
  },
  u: {
    textDecorationLine: 'underline',
  },
});

//+---------------------------------+
//|---Aggregate Element Styling-----|
//+---------------------------------+

const aggregateElementStyles = StyleSheet.create({
  container: {
    gap: aggregateElementTheme.generalSpacing,
    marginVertical: 20,
  },
  uploadButtonContainer: {
    alignItems: 'flex-start',
  },
  uploadButton: {
    backgroundColor:
      aggregateElementTheme.functionButtons.uploadButton.backgroundColor,
    paddingVertical:
      aggregateElementTheme.functionButtons.uploadButton.paddingVertical,
    paddingHorizontal:
      aggregateElementTheme.functionButtons.uploadButton.paddingHorizontal,
    borderRadius:
      aggregateElementTheme.functionButtons.uploadButton.borderRadius,
    alignItems: 'center',
    borderWidth: aggregateElementTheme.functionButtons.uploadButton.borderWidth,
    borderColor: aggregateElementTheme.functionButtons.uploadButton.borderColor,
  },
  uploadButtonText: {
    color: aggregateElementTheme.functionButtons.uploadButton.fontColor,
    fontFamily: aggregateElementTheme.functionButtons.uploadButton.fontFamily,
    fontSize: aggregateElementTheme.functionButtons.uploadButton.fontSize,
  },
  uploadButtonOnHover: {
    backgroundColor:
      aggregateElementTheme.functionButtons.uploadButton.onHover
        .backgroundColor,
  },
  uploadButtonTextOnHover: {
    color: aggregateElementTheme.functionButtons.uploadButton.onHover.fontColor,
    fontFamily:
      aggregateElementTheme.functionButtons.uploadButton.onHover.fontFamily,
    fontSize:
      aggregateElementTheme.functionButtons.uploadButton.onHover.fontSize,
  },

  expandedAggregateItemIdicator: {
    backgroundColor: theme.colors.textOnGray,
    width: 2,
    marginLeft: -7,
    marginRight: 5,
    marginBottom: 5,
    height: 'auto',
    borderRadius: 10,
  },
  blackBarMobile: {
    left: 5,
    width: 2,
    height: '98%',
  },
  singleAggregateContainer: {
    flex: 1,
  },
  borderLine: {
    height: theme.border.width.regular,
    backgroundColor: theme.border.color.darkGray,
  },

  actionButtons: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: aggregateElementTheme.functionButtons.gap,
    marginVertical: aggregateElementTheme.functionButtons.gap,
  },
  // ---- Toggle Detail Button ----
  aggToggleDetailButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: aggregateElementTheme.functionButtons.toggle.iconTextGap,
    backgroundColor:
      aggregateElementTheme.functionButtons.toggle.backgroundColor,
    borderRadius: aggregateElementTheme.functionButtons.toggle.borderRadius,
    borderWidth: aggregateElementTheme.functionButtons.toggle.borderWidth,
    borderColor: aggregateElementTheme.functionButtons.toggle.borderColor,
    padding: aggregateElementTheme.functionButtons.padding,
  },
  aggToggleDetailButtonOnHover: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: aggregateElementTheme.functionButtons.toggle.iconTextGap,
    backgroundColor:
      aggregateElementTheme.functionButtons.toggle.onHover.backgroundColor,
    borderRadius:
      aggregateElementTheme.functionButtons.toggle.onHover.borderRadius,
    borderWidth:
      aggregateElementTheme.functionButtons.toggle.onHover.borderWidth,
    borderColor:
      aggregateElementTheme.functionButtons.toggle.onHover.borderColor,
  },
  aggToggleDetailButtonActive: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: aggregateElementTheme.functionButtons.toggle.iconTextGap,
    backgroundColor:
      aggregateElementTheme.functionButtons.toggle.onActive.backgroundColor,
    borderRadius:
      aggregateElementTheme.functionButtons.toggle.onActive.borderRadius,
    borderWidth:
      aggregateElementTheme.functionButtons.toggle.onActive.borderWidth,
    borderColor:
      aggregateElementTheme.functionButtons.toggle.onActive.borderColor,
  },
  aggToggleDetailButtonText: {
    fontFamily: aggregateElementTheme.functionButtons.toggle.fontFamily,
    fontSize: aggregateElementTheme.functionButtons.toggle.fontSize,
    color: aggregateElementTheme.functionButtons.toggle.fontColor,
    textDecorationLine:
      aggregateElementTheme.functionButtons.toggle.textDecoration,
  },
  aggToggleDetailButtonTextOnHover: {
    fontFamily: aggregateElementTheme.functionButtons.toggle.onHover.fontFamily,
    fontSize: aggregateElementTheme.functionButtons.toggle.onHover.fontSize,
    color: aggregateElementTheme.functionButtons.toggle.onHover.fontColor,
    textDecorationLine:
      aggregateElementTheme.functionButtons.toggle.onHover.textDecoration,
  },
  aggToggleDetailButtonTextActive: {
    fontFamily:
      aggregateElementTheme.functionButtons.toggle.onActive.fontFamily,
    fontSize: aggregateElementTheme.functionButtons.toggle.onActive.fontSize,
    color: aggregateElementTheme.functionButtons.toggle.onActive.fontColor,
    textDecorationLine:
      aggregateElementTheme.functionButtons.toggle.onActive.textDecoration,
  },
  aggToggleDetailButtonIcon: {
    color: aggregateElementTheme.functionButtons.toggle.fontColor,
  },
  aggToggleDetailButtonIconOnHover: {
    color: aggregateElementTheme.functionButtons.toggle.fontColor,
  },
  aggToggleDetailButtonIconActive: {
    color: aggregateElementTheme.functionButtons.toggle.fontColor,
  },
  aggToggleDetailButtonSmallView: {
    flex: 2,
    alignItems: 'center',
    justifyContent: 'center',
    padding: aggregateElementTheme.functionButtons.paddingSmallView,
    backgroundColor:
      aggregateElementTheme.functionButtons.toggle.smallView.backgroundColor,
    borderRadius:
      aggregateElementTheme.functionButtons.toggle.smallView.borderRadius,
    borderWidth:
      aggregateElementTheme.functionButtons.toggle.smallView.borderWidth,
    borderColor:
      aggregateElementTheme.functionButtons.toggle.smallView.borderColor,
  },
  aggToggleDetailButtonTextSmallView: {
    fontFamily:
      aggregateElementTheme.functionButtons.toggle.smallView.fontFamily,
    fontSize: aggregateElementTheme.functionButtons.toggle.smallView.fontSize,
  },

  // ------ Aggregate Delete Button -----
  aggDeleteButton: {
    backgroundColor:
      aggregateElementTheme.functionButtons.delete.backgroundColor,
    borderRadius: aggregateElementTheme.functionButtons.delete.borderRadius,
    borderWidth: aggregateElementTheme.functionButtons.delete.borderWidth,
    borderColor: aggregateElementTheme.functionButtons.delete.borderColor,
    padding: aggregateElementTheme.functionButtons.padding,
  },
  aggDeleteButtonOnHover: {
    backgroundColor:
      aggregateElementTheme.functionButtons.delete.onHover.backgroundColor,
    borderWidth:
      aggregateElementTheme.functionButtons.delete.onHover.borderWidth,
    borderRadius:
      aggregateElementTheme.functionButtons.delete.onHover.borderRadius,
    borderColor:
      aggregateElementTheme.functionButtons.delete.onHover.borderColor,
  },
  aggDeleteButtonContent: {
    color: aggregateElementTheme.functionButtons.delete.fontColor,
    fontSize: aggregateElementTheme.functionButtons.delete.fontSize,
    fontFamily: aggregateElementTheme.functionButtons.delete.fontFamily,
  },
  aggDeleteButtonContentOnHover: {
    color: aggregateElementTheme.functionButtons.delete.onHover.fontColor,
    fontSize: aggregateElementTheme.functionButtons.delete.onHover.fontSize,
    fontFamily: aggregateElementTheme.functionButtons.delete.onHover.fontFamily,
  },
  aggDeleteButtonMobile: {
    backgroundColor:
      aggregateElementTheme.functionButtons.delete.smallView.backgroundColor,
    borderWidth:
      aggregateElementTheme.functionButtons.delete.smallView.borderWidth,
    borderRadius:
      aggregateElementTheme.functionButtons.delete.smallView.borderRadius,
    borderColor:
      aggregateElementTheme.functionButtons.delete.smallView.borderColor,
    padding: aggregateElementTheme.functionButtons.paddingSmallView,
  },
  aggDeleteButtonMobileContent: {
    color: aggregateElementTheme.functionButtons.delete.smallView.fontColor,
    fontSize: aggregateElementTheme.functionButtons.delete.smallView.fontSize,
    fontFamily:
      aggregateElementTheme.functionButtons.delete.smallView.fontFamily,
  },
  // ------ Aggregate Add Button -----
  aggAddButton: {
    backgroundColor: aggregateElementTheme.functionButtons.add.backgroundColor,
    borderRadius: aggregateElementTheme.functionButtons.add.borderRadius,
    borderWidth: aggregateElementTheme.functionButtons.add.borderWidth,
    borderColor: aggregateElementTheme.functionButtons.add.borderColor,
    padding: aggregateElementTheme.functionButtons.padding,
  },
  aggAddButtonOnHover: {
    backgroundColor:
      aggregateElementTheme.functionButtons.add.onHover.backgroundColor,
    borderRadius:
      aggregateElementTheme.functionButtons.add.onHover.borderRadius,
    borderWidth: aggregateElementTheme.functionButtons.add.onHover.borderWidth,
    borderColor: aggregateElementTheme.functionButtons.add.onHover.borderColor,
  },
  aggAddButtonContent: {
    color: aggregateElementTheme.functionButtons.add.fontColor,
    fontSize: aggregateElementTheme.functionButtons.add.fontSize,
    fontFamily: aggregateElementTheme.functionButtons.add.fontFamily,
  },
  aggAddButtonContentOnHover: {
    color: aggregateElementTheme.functionButtons.add.onHover.fontColor,
    fontSize: aggregateElementTheme.functionButtons.add.onHover.fontSize,
    fontFamily: aggregateElementTheme.functionButtons.add.onHover.fontFamily,
  },
  aggAddButtonMobile: {
    backgroundColor:
      aggregateElementTheme.functionButtons.add.smallView.backgroundColor,
    borderWidth:
      aggregateElementTheme.functionButtons.add.smallView.borderWidth,
    borderRadius:
      aggregateElementTheme.functionButtons.add.smallView.borderRadius,
    borderColor:
      aggregateElementTheme.functionButtons.add.smallView.borderColor,
    padding: aggregateElementTheme.functionButtons.paddingSmallView + 1,
  },
  aggAddButtonMobileContent: {
    color: aggregateElementTheme.functionButtons.add.smallView.fontColor,
    fontSize: aggregateElementTheme.functionButtons.add.smallView.fontSize,
    fontFamily: aggregateElementTheme.functionButtons.add.smallView.fontFamily,
  },
  singleDetailAggregateContainerMobile: {
    paddingLeft: 5,
  },

  //| Filter Section of the Aggregate |
  filter: {
    gap: aggregateElementTheme.generalSpacing,
  },
  filterSettingsContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: aggregateElementTheme.filter.filterSettingsAlignment,
  },
  filterSettings: {
    gap: aggregateElementTheme.generalSpacing,
    flexDirection: aggregateElementTheme.filter.filterSettingsFlexDirection,
  },
  filterSettingsMobile: {
    flexDirection: 'column',
  },
  filterDivider: {
    backgroundColor: aggregateElementTheme.filter.divider.color,
    height: aggregateElementTheme.filter.divider.width,
  },
  filterDropdownContainer: {
    flex: 1,
  },
  filterInputAndBtnContainer: {
    flex: 1,
    flexDirection: 'row',
    gap: aggregateElementTheme.generalSpacing,
  },
  filterInputContainer: {
    flex: 1,
  },
  filterInput: {
    height: inputElementTheme.height,
    backgroundColor: inputElementTheme.backgroundColor,
    fontSize: inputElementTheme.fontSize,
    fontFamily: inputElementTheme.fontFamily,
    color: inputElementTheme.fontColor,
    borderColor: inputElementTheme.borderColor,
    borderWidth: inputElementTheme.borderWidth,
    borderRadius: inputElementTheme.borderRadius,
    paddingLeft: inputElementTheme.paddingLeft - inputElementTheme.borderWidth,
    paddingRight: inputElementTheme.paddingRight,
    paddingTop: inputElementTheme.paddingTop,
    paddingBottom: inputElementTheme.paddingBottom,
    position: 'relative',
  },
  filterInputFocused: {
    borderColor: inputElementTheme.focused.borderColor,
    borderWidth: inputElementTheme.focused.borderWidth,
    borderStyle: inputElementTheme.focused.borderStyle,
    paddingLeft:
      inputElementTheme.paddingLeft - inputElementTheme.focused.borderWidth,
    // @ts-ignore
    boxSizing: 'border-box',
    outlineStyle: undefined,
    outlineWidth: 0,
  },
  filterPlaceholder: {
    zIndex: 10,
    width: '85%',
    overflow: 'hidden',
    position: 'absolute',
    color: inputElementTheme.placeholderColor,
    left: inputElementTheme.paddingLeft,
    top: inputElementTheme.placeholderTopPosition,
    fontFamily: inputElementTheme.placeholderFontFamily,
  },
  appliedFiltersContainer: {
    overflow: 'hidden',
    flexWrap: 'wrap',
    flexDirection: 'row',
    alignItems: aggregateElementTheme.filter.appliedFilter.alignment,
    gap: aggregateElementTheme.generalSpacing,
  },
  appliedFilterCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: aggregateElementTheme.filter.appliedFilter.backgroundColor,
    borderRadius: aggregateElementTheme.filter.appliedFilter.borderRadius,
    padding: aggregateElementTheme.filter.appliedFilter.padding,
    gap: aggregateElementTheme.filter.appliedFilter.iconTextGap,
    borderWidth: aggregateElementTheme.filter.appliedFilter.borderWidth,
    borderColor: aggregateElementTheme.filter.appliedFilter.borderColor,
  },
  appliedFilterCardTypeText: {
    fontSize: aggregateElementTheme.filter.appliedFilter.fontSize,
    fontFamily: aggregateElementTheme.filter.appliedFilter.labelFontFamily,
    color: aggregateElementTheme.filter.appliedFilter.fontColor,
  },
  appliedFilterCardText: {
    fontSize: aggregateElementTheme.filter.appliedFilter.fontSize,
    fontFamily: aggregateElementTheme.filter.appliedFilter.fontFamily,
    color: aggregateElementTheme.filter.appliedFilter.fontColor,
  },
  aggAddFilterButton: {
    height: standardElement.standardElementHeight,
    width: standardElement.standardElementHeight,
    backgroundColor:
      aggregateElementTheme.filter.addFilterButton.backgroundColor,
    borderRadius: aggregateElementTheme.filter.addFilterButton.borderRadius,
    alignItems: 'center',
    justifyContent: 'center',
  },
  aggAddFilterPressable: {
    // This is Style is only used to make the whole Button clickable
    justifyContent: 'center',
    alignItems: 'center',
    height: '100%',
    width: '100%',
  },
  addFilterButtonActive: {
    backgroundColor:
      aggregateElementTheme.filter.addFilterButton.active.backgroundColor,
  },
  addFilterButtonActiveOnHover: {
    backgroundColor:
      aggregateElementTheme.filter.addFilterButton.onHover.backgroundColor,
  },
  noEntriesContainer: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  noEntriesText: {
    fontSize: aggregateElementTheme.noEntry.fontSize,
    color: aggregateElementTheme.noEntry.fontColor,
  },
  //+---------------------------------+
  //| /////////////////////////////// |
  //+---------------------------------+

  //+---------------------------------+
  //| ------ Tableview Styling ------ |
  //+---------------------------------+
  tableHeader: {
    flexDirection: 'row',
    paddingBottom: 10,
    borderBottomWidth: aggregateElementTheme.tableView.header.borderWidth,
    borderColor: aggregateElementTheme.tableView.header.borderColor,
  },
  tableHeaderCell: {
    flex: 1,
    flexDirection: 'row',
    marginHorizontal: aggregateElementTheme.tableView.header.horizontalSpacing,
    gap: aggregateElementTheme.tableView.header.horizontalSpacing,
    alignItems: aggregateElementTheme.tableView.header.alignItems,
    textAlign: aggregateElementTheme.tableView.header.textAlign,
  },
  tableAggHeaderText: {
    fontFamily: aggregateElementTheme.tableView.header.fontFamily,
    fontSize: aggregateElementTheme.tableView.header.fontSize,
    color: aggregateElementTheme.tableView.header.color,
  },
  tableRow: {
    flexDirection: 'row',
    cursor: 'auto',
    borderBottomWidth: theme.border.width.regular,
    borderColor: theme.border.color.darkGray,
  },

  tableCell: {
    flex: 1,
    margin: 10,
    flexDirection: 'column',
    gap: 10,
  },
  tableAggActionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: aggregateElementTheme.tableView.actionButtons.iconTextGap,
  },
  deleteButton: {
    color: aggregateElementTheme.tableView.actionButtons.deleteColor,
    fontFamily: aggregateElementTheme.tableView.actionButtons.fontFamily,
    fontSize: aggregateElementTheme.tableView.actionButtons.fontSize,
  },
  addButton: {
    color: aggregateElementTheme.tableView.actionButtons.actionColor,
    fontFamily: aggregateElementTheme.tableView.actionButtons.fontFamily,
    fontSize: aggregateElementTheme.tableView.actionButtons.fontSize,
  },
  editButton: {
    color: aggregateElementTheme.tableView.actionButtons.actionColor,
    fontFamily: aggregateElementTheme.tableView.actionButtons.fontFamily,
    fontSize: aggregateElementTheme.tableView.actionButtons.fontSize,
  },
  copyButton: {
    color: aggregateElementTheme.tableView.actionButtons.actionColor,
    fontFamily: aggregateElementTheme.tableView.actionButtons.fontFamily,
    fontSize: aggregateElementTheme.tableView.actionButtons.fontSize,
  },
  tableRowHover: {
    backgroundColor:
      aggregateElementTheme.tableView.cellOnHover.backgroundColor,
  },

  // Mobile (Card)Table View
  mobileCardList: {
    gap: aggregateElementTheme.tableView.smallView.cardsGap,
  },
  mobileCard: {
    backgroundColor:
      aggregateElementTheme.tableView.smallView.card.backgroundColor,
    borderRadius: aggregateElementTheme.tableView.smallView.card.borderRadius,
    padding: aggregateElementTheme.tableView.smallView.card.padding,
    borderWidth: aggregateElementTheme.tableView.smallView.card.borderWidth,
    borderColor: aggregateElementTheme.tableView.smallView.card.borderColor,
    cursor: 'auto',
  },
  mobileCardHover: {
    backgroundColor: undefined,
  },
  mobileFieldRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderBottomWidth:
      aggregateElementTheme.tableView.smallView.card.dividerWidth,
    borderBottomColor:
      aggregateElementTheme.tableView.smallView.card.dividerColor,
    paddingBottom: 5,
    marginBottom: 6,
  },
  mobileFieldLabel: {
    fontFamily: theme.fontFamily.bold,
    fontSize: aggregateElementTheme.tableView.smallView.card.fontSize,
    color: aggregateElementTheme.tableView.smallView.card.color,
    textTransform: 'uppercase',
  },
  mobileFieldValue: {
    fontFamily: aggregateElementTheme.tableView.smallView.card.fontFamily,
    fontSize: aggregateElementTheme.tableView.smallView.card.fontSize,
    color: aggregateElementTheme.tableView.smallView.card.color,
    textTransform: undefined,
  },
  mobileValidation: {
    fontFamily: aggregateElementTheme.tableView.smallView.card.fontFamily,
    color: 'red',
    fontSize: aggregateElementTheme.tableView.smallView.card.fontFamily,
    marginBottom: theme.space.extraSmall,
  },
  mobileActionsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
  },
  mobileActionButton: {
    flex: 1,
    backgroundColor:
      aggregateElementTheme.tableView.actionButtons.actionBackgroundColor,
    borderRadius: aggregateElementTheme.tableView.actionButtons.borderRadius,
    paddingVertical:
      aggregateElementTheme.tableView.actionButtons.paddingVertical,
    alignItems: 'center',
    justifyContent: 'center',
  },
  mobileActionButtonContent: {
    color: aggregateElementTheme.tableView.actionButtons.actionColor,
    fontSize: aggregateElementTheme.tableView.actionButtons.fontSize,
  },
  mobileDeleteButton: {
    flex: 1,
    backgroundColor:
      aggregateElementTheme.tableView.actionButtons.deleteBackgroundColor,
    borderRadius: aggregateElementTheme.tableView.actionButtons.borderRadius,
    paddingVertical:
      aggregateElementTheme.tableView.actionButtons.paddingVertical,
    alignItems: 'center',
    justifyContent: 'center',
  },
  mobileDeleteButtonContent: {
    color: aggregateElementTheme.tableView.actionButtons.deleteColor,
    fontSize: aggregateElementTheme.tableView.actionButtons.fontSize,
  },
  //+---------------------------------+
  aggVarAddButtonContainer: {
    alignItems: 'flex-start',
  },
  aggVarAddButtonMobile: {
    width: aggregateElementTheme.functionButtons.add.smallView.size,
    height: aggregateElementTheme.functionButtons.add.smallView.size,
    backgroundColor:
      aggregateElementTheme.functionButtons.add.smallView.backgroundColor,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius:
      aggregateElementTheme.functionButtons.add.smallView.borderRadius,
  },
  aggVarAddButton: {
    justifyContent: 'center',
    alignItems: 'flex-start',
  },

  // ------- Pagination Styles -------
  paginationContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: aggregateElementTheme.pagination.marginVertical,
    gap: aggregateElementTheme.pagination.gap,
  },
  pageItem: {
    paddingHorizontal: aggregateElementTheme.pagination.paddingHorizontal,
    paddingVertical: aggregateElementTheme.pagination.paddingVertical,
    borderColor: aggregateElementTheme.pagination.defaultButton.borderColor,
    borderWidth: aggregateElementTheme.pagination.borderWidth,
    borderRadius: aggregateElementTheme.pagination.borderRadius,
    backgroundColor:
      aggregateElementTheme.pagination.defaultButton.backgroundColor,
  },
  pageItemText: {
    fontSize: aggregateElementTheme.pagination.fontSize,
    color: aggregateElementTheme.pagination.defaultButton.fontColor,
    fontFamily: aggregateElementTheme.pagination.defaultButton.fontFamily,
  },
  activePageItem: {
    borderColor: aggregateElementTheme.pagination.activeButton.borderColor,
    backgroundColor:
      aggregateElementTheme.pagination.activeButton.backgroundColor,
  },
  activePageItemText: {
    fontFamily: aggregateElementTheme.pagination.activeButton.fontFamily,
    color: aggregateElementTheme.pagination.activeButton.fontColor,
  },
  disabledButton: {
    opacity: 0.5,
  },
  disabledButtonText: {},
  breakContainer: {
    paddingHorizontal: aggregateElementTheme.pagination.paddingHorizontal,
  },
  breakText: {
    color: aggregateElementTheme.pagination.defaultButton.fontColor,
  },
});

//+---------------------------------+
//|----DateTime Element Styling-----|
//+---------------------------------+

const dateTimeElementStyles = StyleSheet.create({
  // Styling des Pickers befindet sich unter ./common-datepicker.css
  datePickerInput: {
    padding: 0, // damit das initiale padding überschrieben werden kann (wird in unten mit paddingHorizontal ersetzt)
    position: 'relative',
    backgroundColor: dateTimeElementTheme.input.backgroundColor,
    height: standardElement.standardElementHeight,
    borderColor: dateTimeElementTheme.input.borderColor,
    borderWidth: dateTimeElementTheme.input.borderWidth,
    borderRadius: dateTimeElementTheme.input.borderRadius,
    paddingHorizontal:
      dateTimeElementTheme.input.paddingLeft -
      dateTimeElementTheme.input.borderWidth,
  },
  disabledDatePickerInput: {
    backgroundColor: dateTimeElementTheme.disabled.backgroundColor,
    color: dateTimeElementTheme.disabled.fontColor,
  },
  focusedDatePickerInput: {
    borderColor: dateTimeElementTheme.focused.borderColor,
    borderWidth: dateTimeElementTheme.focused.borderWidth,
    borderStyle: dateTimeElementTheme.focused.borderStyle,
    paddingHorizontal:
      dateTimeElementTheme.input.paddingLeft -
      dateTimeElementTheme.focused.borderWidth,
    // @ts-ignore
    boxSizing: 'border-box',
    outlineStyle: dateTimeElementTheme.focused.borderStyle,
    outlineWidth: dateTimeElementTheme.focused.borderWidth,
    outlineOffset: -dateTimeElementTheme.focused.borderWidth,
    outlineColor: dateTimeElementTheme.focused.borderColor,
  },
  errorDatePickerInput: {
    backgroundColor: dateTimeElementTheme.error.backgroundColor,
    borderColor: dateTimeElementTheme.error.borderColor,
  },
  datePickerInputText: {
    display: 'flex',
    height: '100%',
    paddingTop: dateTimeElementTheme.input.paddingTop,
    paddingBottom: dateTimeElementTheme.input.paddingBottom,
    fontSize: dateTimeElementTheme.input.fontSize,
    fontFamily: dateTimeElementTheme.input.fontFamily,
    color: dateTimeElementTheme.input.fontColor,
  },
  datePickerLabel: {
    zIndex: 10,
    width: '75%',
    overflow: 'hidden',
    //@ts-ignore
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    position: 'absolute',
    color: dateTimeElementTheme.input.labelFontColor,
    fontSize: dateTimeElementTheme.input.labelFontSize,
    fontFamily: dateTimeElementTheme.input.labelFontFamily,
    left: dateTimeElementTheme.input.paddingLeft,
  },
  errorDatePickerLabel: {
    color: dateTimeElementTheme.error.labelFontColor,
  },
});

//+----------------------------------------+
//|---------Binary Element Styling---------|
//+----------------------------------------+

const binaryElementStyles = StyleSheet.create({
  srOnly: {
    position: 'absolute',
    width: 0,
    height: 0,
    margin: 0,
    padding: 0,
    overflow: 'hidden',
  },
  fileChooser: {
    height: binaryInputTheme.height,
    backgroundColor: binaryInputTheme.backgroundColor,
    borderWidth: binaryInputTheme.borderWidth,
    borderColor: binaryInputTheme.borderColor,
    borderRadius: binaryInputTheme.borderRadius,
    borderStyle: binaryInputTheme.borderStyle,
    padding: binaryInputTheme.padding,
    overflow: 'hidden',
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
  },
  fileChooserLabel: {
    fontSize: binaryInputTheme.labelFontSize,
    color: binaryInputTheme.labelFontColor,
    fontFamily: binaryInputTheme.labelFontFamily,
  },
  fileChooserLabelError: {
    color: binaryInputTheme.errorLabelFontColor,
  },
  fileChooserLabelDisabled: {
    color: binaryInputTheme.labelFontColor,
  },
  fileChooserWithFiles: {
    borderBottomLeftRadius: 0,
    borderBottomRightRadius: 0,
  },
  dragOverFileChooser: {
    backgroundColor: binaryInputTheme.backgroundColorDragOver,
  },
  disabledFileChooser: {
    backgroundColor: binaryInputTheme.backgroundColorDisabled,
  },
  errorFileChooser: {
    borderColor: binaryInputTheme.borderColorError,
    backgroundColor: binaryInputTheme.backgroundColorError,
  },
  fileContainer: {
    borderColor: binaryInputTheme.borderColor,
    borderWidth: binaryInputTheme.borderWidth,
    borderRadius: binaryInputTheme.borderRadius,
    borderStyle: binaryInputTheme.borderStyle,
    borderTopWidth: 0,
    borderTopLeftRadius: 0,
    borderTopRightRadius: 0,
  },
  fileList: {
    padding: binaryInputTheme.padding,
    gap: binaryInputTheme.fileListSpacing,
    flexDirection: 'row',
    flexWrap: 'wrap',
    width: '100%',
  },
  fielListItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: binaryInputTheme.fileListItemsSpacing,
  },
  fileListItemLinkText: {
    color: binaryInputTheme.fileListItemLinkFontColor,
    textDecorationLine: binaryInputTheme.fileListItemTextDecoration,
  },
  fileListItemText: {
    flex: 1,
    fontSize: binaryInputTheme.fileListItemFontSize,
    color: binaryInputTheme.fileListItemTextFontColor,
  },
  summaryLinkListRow: {
    rowGap: binaryInputTheme.fileListItemsSpacing,
  },
  // Layout helpers for BinaryElement file list
  fileContainerInner: {
    flexDirection: 'column',
    alignItems: 'flex-start',
  },
});

const binaryFileCardStyles = StyleSheet.create({
  cardSizeXlScreen: {
    width: `calc(25% - ${(binaryInputTheme.fileListSpacing * 3) / 4}px)` as any,
  },
  cardSizeMdScreen: {
    width:
      `calc(33.33% - ${(binaryInputTheme.fileListSpacing * 2) / 3}px)` as any,
  },
  cardSizeSmScreen: {
    width: `calc(50% - ${(binaryInputTheme.fileListSpacing * 1) / 2}px)` as any,
  },
  card: {
    // Bootstrap-like grid: 4 columns (12/3 = 4 items)
    width: `calc(50% - ${(binaryInputTheme.fileListSpacing * 1) / 2}px)` as any,
    height: 210,
    borderRadius: theme.border.borderRadius.large,
    backgroundColor: '#fff',
    overflow: 'hidden',
    borderWidth: theme.border.width.regular,
    borderColor: theme.border.color.darkGray,
  },
  cardShadow: {
    shadowColor: '#717171ff',
    shadowOpacity: 0.12,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 2 },
  },
  cardShadowHover: {
    shadowColor: '#717171ff',
    shadowOpacity: 0.25,
    shadowRadius: 10,
  },
  cardmobile: {
    height: 150,
  },
  header: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: '25%',
    padding: theme.space.regular,
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'flex-end',
    ...Platform.select({
      web: {
        backgroundImage:
          'linear-gradient(180deg, rgba(0,0,0,0.20) 0%, rgba(0,0,0,0.00) 100%)',
      } as any,
    }),
    zIndex: 2,
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.space.regular,
  },
  iconButton: {
    width: 36,
    height: 36,
    borderRadius: theme.border.borderRadius.regular,
    borderWidth: 1,
    borderColor: theme.border.color.gray,
    backgroundColor: theme.colors.backgroundPrimary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteIcon: {
    color: theme.colors.colorDeleteRed,
  },
  downloadIcon: {
    color: theme.colors.colorAccent,
  },
  bodyThumbnailContainer: {
    height: '100%',
    flex: 1,
    backgroundColor: '#E9ECEF',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1,
  },
  bodyIconContainer: {
    flex: 1,
    backgroundColor: '#E9ECEF',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1,
  },
  bodyIcon: {
    color: theme.colors.textMuted,
    fontSize: 60,
  },
  previewLabel: {
    color: '#777',
    fontSize: 12,
  },
  footer: {
    borderTopWidth: theme.border.width.regular,
    borderTopColor: theme.border.color.gray,
    paddingHorizontal: 12,
    paddingVertical: 10,
    justifyContent: 'flex-start',
    gap: 2,
  },
  footerMobile: {
    paddingHorizontal: 7,
    paddingVertical: 5,
  },
  fileName: {
    fontSize: theme.fontSize.regular,
    fontWeight: theme.fontWeight.bold,
    color: theme.colors.textPrimary,
  },
  fileNameMobile: {
    fontSize: theme.fontSize.small,
  },
  fileType: {
    fontSize: theme.fontSize.small,
    color: theme.colors.textMuted,
  },
  fileTypeMobile: {
    fontSize: 10,
    color: theme.colors.textMuted,
  },
});

const pdfPreviewStyles = StyleSheet.create({
  pdfPreview: {
    width: '100%',
    height: '100%',
    overflow: 'hidden',
    alignItems: 'flex-start',
  },
});

//+----------------------------------------+
//|------Dropdown Base Element Styling-----|
//+----------------------------------------+

const baseDropDownInputElement = StyleSheet.create({
  dropdown: {
    ...(Platform.OS === 'web' ? ({ cursor: 'text' } as any) : {}),
  },
  dropdownInputContainer: {
    backgroundColor: dropdownInputTheme.input.backgroundColor,
    borderRadius: dropdownInputTheme.input.borderRadius,
    borderWidth: dropdownInputTheme.input.borderWidth,
    borderColor: dropdownInputTheme.input.borderColor,
    minHeight:
      dropdownInputTheme.elementHeight -
      dropdownInputTheme.input.borderWidth * 2,
  },
  dropdownPlaceholderLabel: {
    width: '85%',
    position: 'absolute',
    overflow: 'hidden',
    left: theme.space.large,
    color: dropdownInputTheme.input.labelFontColor,
  },
  dropdownLabel: {
    fontSize: dropdownInputTheme.input.labelFontSize,
    top:
      dropdownInputTheme.input.labelTopPosition -
      dropdownInputTheme.input.borderWidth,
  },
  dropdownLabelActive: {
    fontSize: dropdownInputTheme.input.activeLabelFontSize,
    top:
      dropdownInputTheme.input.activeLabelTopPosition -
      dropdownInputTheme.input.borderWidth,
  },
  placeholderLabelError: {
    color: dropdownInputTheme.errorFontColor,
  },
  inputElement: {
    backgroundColor: 'transparent',
    minHeight:
      dropdownInputTheme.elementHeight -
      dropdownInputTheme.input.borderWidth * 2,
    flex: 1,
  },
  inputField: {
    marginTop: 8,
    fontSize: dropdownInputTheme.input.fontSize,
    fontFamily: dropdownInputTheme.input.fontFamily,
    color: dropdownInputTheme.input.fontColor,
  },
  errorContainer: {
    backgroundColor: dropdownInputTheme.errorBackgroundColor,
    borderColor: dropdownInputTheme.errorFontColor,
  },
  errorDropdown: {
    backgroundColor: dropdownInputTheme.errorBackgroundColor,
    borderColor: dropdownInputTheme.errorFontColor,
  },
  dropdownContainer: {
    position: 'absolute',
    marginTop: -5,
    zIndex: 10,
    overflow: 'hidden',
    backgroundColor: dropdownInputTheme.dropdownList.backgroundColor,
    borderTopColor: dropdownInputTheme.dropdownList.separatorColor,
    borderWidth: dropdownInputTheme.dropdownList.borderWidth,
    borderColor: dropdownInputTheme.dropdownList.borderColor,
    borderBottomLeftRadius: dropdownInputTheme.dropdownList.borderRadius,
    borderBottomRightRadius: dropdownInputTheme.dropdownList.borderRadius,
    maxHeight: dropdownInputTheme.dropdownList.heightLimit,
    shadowColor: dropdownInputTheme.dropdownList.shadowColor,
    shadowOffset: dropdownInputTheme.dropdownList.shadowOffset,
    shadowOpacity: dropdownInputTheme.dropdownList.shadowOpacity,
    shadowRadius: dropdownInputTheme.dropdownList.shadowRadius,
  },
  dropdownItemsSeparator: {
    height: dropdownInputTheme.dropdownList.separatorWidth,
    backgroundColor: dropdownInputTheme.dropdownList.separatorColor,
  },
  dropdownOption: {
    backgroundColor: dropdownInputTheme.dropdownList.backgroundColor,
    padding: dropdownInputTheme.dropdownList.item.padding,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  dropdownOptionHover: {
    backgroundColor: dropdownInputTheme.dropdownList.item.backgroundOnHover,
  },
  dropdownOptionError: {
    backgroundColor: dropdownInputTheme.errorBackgroundColor,
    color: dropdownInputTheme.errorFontColor,
  },
  dropdownOptionErrorHover: {
    backgroundColor:
      dropdownInputTheme.dropdownList.item.backgroundErrorOnHover,
  },
  chipsWrapper: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: theme.space.regular,
    borderTopWidth: theme.border.width.regular,
    borderTopColor: theme.border.color.gray,
    gap: theme.space.regular,
  },
  chip: {
    flexShrink: 1,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor:
      dropdownInputTheme.dropdownList.multiSelectItem.backgroundColor,
    borderRadius: dropdownInputTheme.dropdownList.multiSelectItem.borderRadius,
    paddingVertical:
      dropdownInputTheme.dropdownList.multiSelectItem.paddingVertical,
    paddingHorizontal:
      dropdownInputTheme.dropdownList.multiSelectItem.paddingHorizontal,
    gap: dropdownInputTheme.dropdownList.multiSelectItem.gap,
  },
  chipText: {
    fontSize: dropdownInputTheme.dropdownList.multiSelectItem.fontSize,
    color: dropdownInputTheme.dropdownList.multiSelectItem.fontColor,
  },

  errorChip: {
    backgroundColor:
      dropdownInputTheme.dropdownList.multiSelectItem.backgroundColorError,
  },
  errorText: {
    color: dropdownInputTheme.errorFontColor,
  },

  disabledInput: {
    ...(Platform.OS === 'web' ? ({ cursor: 'not-allowed' } as any) : {}),
  },
  disabledDropDown: {
    backgroundColor: dropdownInputTheme.input.backgroundColorDisabled,
    color: dropdownInputTheme.input.fontColorDisabled,
    ...(Platform.OS === 'web' && ({ cursor: 'not-allowed' } as any)),
  },
});

//+----------------------------------------+
//|-- Autocomplete Input Element Styling---|
//+----------------------------------------+

const autocompleteInputElementStyle = StyleSheet.create({
  dropdownListItemText: {
    fontSize: dropdownInputTheme.dropdownList.item.fontSize,
    color: dropdownInputTheme.dropdownList.item.fontColor,
  },
  dropdownListItemTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
});

//+-----------------------------------------+
//|--Dropdown Lazy Catalog Element Styling--|
//+-----------------------------------------+

const dropdownLazyCatalogElementStyles = StyleSheet.create({
  dropdownListItemText: {
    fontSize: dropdownInputTheme.dropdownList.item.fontSize,
    color: dropdownInputTheme.dropdownList.item.fontColor,
  },
  activityText: {
    padding: dropdownInputTheme.dropdownList.activityText.padding,
    fontSize: dropdownInputTheme.dropdownList.activityText.fontSize,
    color: dropdownInputTheme.dropdownList.activityText.fontColor,
    fontFamily: dropdownInputTheme.dropdownList.activityText.fontFamily,
    textAlign: 'center',
  },
  activityTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
  dropdownListItemTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
});

//+----------------------------------------+
//|---Dropdown Reference Element Styling---|
//+----------------------------------------+

const dropdownReferenceElementStyles = StyleSheet.create({
  dropdownListItemText: {
    fontSize: dropdownInputTheme.dropdownList.item.fontSize,
    color: dropdownInputTheme.dropdownList.item.fontColor,
  },
  activityText: {
    padding: dropdownInputTheme.dropdownList.activityText.padding,
    fontSize: dropdownInputTheme.dropdownList.activityText.fontSize,
    color: dropdownInputTheme.dropdownList.activityText.fontColor,
    fontFamily: dropdownInputTheme.dropdownList.activityText.fontFamily,
    textAlign: 'center',
  },
  activityTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
  dropdownListItemTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
});

//+----------------------------------------+
//|---Dropdown Catalog Element Styling-----|
//+----------------------------------------+

const dropdownCatalogElementStyles = StyleSheet.create({
  dropdownListItemText: {
    fontSize: dropdownInputTheme.dropdownList.item.fontSize,
    color: dropdownInputTheme.dropdownList.item.fontColor,
  },
  dropdownListItemTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
  activityText: {
    padding: dropdownInputTheme.dropdownList.activityText.padding,
    fontSize: dropdownInputTheme.dropdownList.activityText.fontSize,
    color: dropdownInputTheme.dropdownList.activityText.fontColor,
    fontFamily: dropdownInputTheme.dropdownList.activityText.fontFamily,
    textAlign: 'center',
  },
  activityTextError: {
    color: dropdownInputTheme.errorFontColor,
  },
});

//+---------------------------------+
//|-----Boolean Element Styling-----|
//+---------------------------------+

const booleanElementStyles = StyleSheet.create({
  radioButtonGroupContainer: {
    marginVertical: booleanElementTheme.radioButton.groupMarginVertical,
    gap: booleanElementTheme.radioButton.groupLabelSpacing,
  },
  radioButtonGroupLabel: {
    fontSize: booleanElementTheme.radioButton.groupLabelFontSize,
    color: booleanElementTheme.radioButton.groupLabelFontColor,
  },
  radioButtonGroupLabelError: {
    color: booleanElementTheme.error.labelFontColor,
  },
  radioButtonGroup: {
    gap: booleanElementTheme.radioButton.optionsSpacing,
    flexDirection: 'column',
  },
  radioButtonRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  booleanElementContainer: {
    gap: booleanElementTheme.iconLabelSpacing,
    flexDirection: 'row',
    alignItems: 'center',
  },
  disabledBooleanElementContainer: {
    opacity: booleanElementTheme.disabledBooleanElementOpacity,
  },

  // -------- Radio Button ----------
  radioButtonElement: {
    width: booleanElementTheme.size,
    height: booleanElementTheme.size,
    borderRadius: 99,
    borderWidth: booleanElementTheme.borderWidth,
    borderColor: booleanElementTheme.borderColor,
    justifyContent: 'center',
    alignItems: 'center',
  },
  radioButtonDot: {
    width: booleanElementTheme.radioButton.dotSize,
    height: booleanElementTheme.radioButton.dotSize,
    backgroundColor: booleanElementTheme.radioButton.dotColor,
    borderRadius: 99,
  },
  selectedRadioButton: {
    backgroundColor: booleanElementTheme.selectedColor,
    borderColor: booleanElementTheme.selectedColor,
  },
  errorRadioButton: {
    borderColor: booleanElementTheme.error.accentColor,
    backgroundColor: booleanElementTheme.error.lightColor,
  },
  errorRadioButtonDot: {
    backgroundColor: booleanElementTheme.error.accentColor,
  },
  focusedRadioButton: {
    borderStyle: formElementWrapperTheme.focusedBorder.borderStyle,
    borderRadius: 99,
    borderWidth: formElementWrapperTheme.focusedBorder.borderWidth,
    borderColor: formElementWrapperTheme.focusedBorder.borderColor,
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioButtonLabel: {
    flex: 1,
    overflow: 'hidden',
    color: booleanElementTheme.labelFontColor,
    fontSize: booleanElementTheme.labelFontSize,
    fontFamily: booleanElementTheme.labelFontFamily,
    borderWidth:
      booleanElementTheme.labelPadding +
      formElementWrapperTheme.focusedBorder.borderWidth,
    borderColor: 'transparent',
  },
  selectedRadioButtonLabel: {
    fontFamily: booleanElementTheme.selectedLabelFontFamily,
    color: booleanElementTheme.selectedLabelFontColor,
  },
  errorRadioButtonLabel: {
    color: booleanElementTheme.error.labelFontColor,
  },
  focusedRadioButtonLabel: {
    padding: booleanElementTheme.labelPadding,
    borderColor: formElementWrapperTheme.focusedBorder.borderColor,
    borderWidth: formElementWrapperTheme.focusedBorder.borderWidth,
    borderStyle: formElementWrapperTheme.focusedBorder.borderStyle,
    elevation: 10,
    // @ts-ignore
    boxSizing: 'border-box',
    // @ts-ignore
    outlineStyle: undefined,
    // @ts-ignore
    outlineWidth: 0,
  },

  // -------- Checkbox --------------
  checkboxElement: {
    width: booleanElementTheme.size,
    height: booleanElementTheme.size,
    borderWidth: booleanElementTheme.borderWidth,
    borderColor: booleanElementTheme.borderColor,
    borderRadius: booleanElementTheme.checkbox.borderRadius,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxIcon: {
    color: booleanElementTheme.checkbox.iconColor,
    fontSize: booleanElementTheme.checkbox.iconFontSize,
  },
  disabledCheckboxIcon: {},
  errorCheckboxIcon: {},
  disabledCheckbox: {},
  selectedCheckbox: {
    backgroundColor: booleanElementTheme.selectedColor,
    borderColor: booleanElementTheme.selectedColor,
  },
  selectedDisabledCheckbox: {},
  errorCheckbox: {
    borderColor: booleanElementTheme.error.accentColor,
    backgroundColor: booleanElementTheme.error.lightColor,
  },
  selectedErrorCheckbox: {
    backgroundColor: booleanElementTheme.error.accentColor,
  },
  focusedCheckbox: {
    borderColor: formElementWrapperTheme.focusedBorder.borderColor,
    borderWidth: formElementWrapperTheme.focusedBorder.borderWidth,
    borderStyle: formElementWrapperTheme.focusedBorder.borderStyle,
    elevation: 10,
  },
  checkboxLabel: {
    flex: 1,
    overflow: 'hidden',
    color: booleanElementTheme.labelFontColor,
    fontSize: booleanElementTheme.labelFontSize,
    fontFamily: booleanElementTheme.labelFontFamily,
    borderWidth:
      booleanElementTheme.labelPadding +
      formElementWrapperTheme.focusedBorder.borderWidth,
    borderColor: 'transparent',
  },
  selectedCheckboxLabel: {
    fontFamily: booleanElementTheme.selectedLabelFontFamily,
    color: booleanElementTheme.selectedLabelFontColor,
  },
  errorCheckboxLabel: {
    color: booleanElementTheme.error.labelFontColor,
  },
  focusedCheckboxLabel: {
    padding: booleanElementTheme.labelPadding,
    borderColor: formElementWrapperTheme.focusedBorder.borderColor,
    borderWidth: formElementWrapperTheme.focusedBorder.borderWidth,
    borderStyle: formElementWrapperTheme.focusedBorder.borderStyle,
    elevation: 10,
  },
  disabledCheckboxLabel: {},
  selectedDisabledCheckboxLabel: {},

  // -------- Boolean Button --------
  booleanButton: {
    borderRadius: booleanElementTheme.button.borderRadius,
    height: standardElement.standardElementHeight,
  },
  booleanButtonDisabled: {
    backgroundColor: booleanElementTheme.button.disabledBackgroundColor,
    borderColor: booleanElementTheme.button.disabledBorderColor,
  },
  booleanButtonError: {
    backgroundColor: booleanElementTheme.error.lightColor,
    borderColor: booleanElementTheme.error.accentColor,
  },
  booleanButtonText: {
    fontSize: booleanElementTheme.button.fontSize,
  },
  booleanButtonTextDisabled: {
    color: booleanElementTheme.button.disabledFontColor,
  },
  booleanButtonTextError: {
    color: booleanElementTheme.error.accentColor,
  },
  booleanButtonFocused: {
    borderColor: formElementWrapperTheme.focusedBorder.borderColor,
    borderWidth: formElementWrapperTheme.focusedBorder.borderWidth,
    borderStyle: formElementWrapperTheme.focusedBorder.borderStyle,
    elevation: 10,
    // @ts-ignore
    boxSizing: 'border-box',
    // @ts-ignore
    outlineStyle: undefined,
    // @ts-ignore
    outlineWidth: 0,
  },
});

//+---------------------------------+
//|------Input Element Styling------|
//+---------------------------------+

const inputElementStyles = StyleSheet.create({
  input: {
    position: 'relative',
    height: inputElementTheme.height,
    backgroundColor: inputElementTheme.backgroundColor,
    fontSize: inputElementTheme.fontSize,
    fontFamily: inputElementTheme.fontFamily,
    color: inputElementTheme.fontColor,
    borderColor: inputElementTheme.borderColor,
    borderWidth: inputElementTheme.borderWidth,
    borderRadius: inputElementTheme.borderRadius,
    paddingLeft: inputElementTheme.paddingLeft - inputElementTheme.borderWidth,
    paddingRight: inputElementTheme.paddingRight,
    paddingTop: inputElementTheme.paddingTop,
    paddingBottom: inputElementTheme.paddingBottom,
  },
  inputError: {
    backgroundColor: inputElementTheme.error.backgroundColor,
    borderColor: inputElementTheme.error.borderColor,
  },
  disabledInput: {
    backgroundColor: inputElementTheme.disabled.backgroundColor,
    color: inputElementTheme.disabled.fontColor,
  },
  inputFocused: {
    borderColor: inputElementTheme.focused.borderColor,
    borderWidth: inputElementTheme.focused.borderWidth,
    borderStyle: inputElementTheme.focused.borderStyle,
    paddingLeft:
      inputElementTheme.paddingLeft - inputElementTheme.focused.borderWidth,
    // @ts-ignore
    boxSizing: 'border-box',
    // @ts-ignore
    outlineStyle: undefined,
    // @ts-ignore
    outlineWidth: 0,
  },
  placeholder: {
    zIndex: 10,
    width: '85%',
    overflow: 'hidden',
    position: 'absolute',
    color: inputElementTheme.placeholderColor,
    left: inputElementTheme.paddingLeft,
    top: inputElementTheme.placeholderTopPosition,
    fontFamily: inputElementTheme.placeholderFontFamily,
  },
  placeholderError: {
    color: inputElementTheme.error.fontColor,
  },
  placeholderFocused: {
    top: inputElementTheme.focused.placeholderTopPosition,
  },
  placeholderText: {
    color: inputElementTheme.placeholderColor,
    fontSize: inputElementTheme.placeholderFontSize,
  },
  placeholderTextFocused: {
    fontSize: inputElementTheme.focused.placeholderFontSize,
  },

  inputNumber: {
    position: 'relative',
    height: inputElementTheme.height,
    backgroundColor: inputElementTheme.backgroundColor,
    fontSize: inputElementTheme.fontSize,
    color: inputElementTheme.fontColor,
    borderColor: inputElementTheme.borderColor,
    borderWidth: inputElementTheme.borderWidth,
    borderRadius: inputElementTheme.borderRadius,
    paddingLeft: inputElementTheme.paddingLeft - inputElementTheme.borderWidth,
    paddingRight: inputElementTheme.paddingRight,
    paddingTop: inputElementTheme.paddingTop,
    paddingBottom: inputElementTheme.paddingBottom,
  },
  inputNumberWithSpace: {
    paddingRight:
      inputElementTheme.paddingRight + inputElementTheme.numberButtons.width,
  },
  placeholderWithSpace: {
    paddingRight: inputElementTheme.numberButtons.width,
  },
  numberBtns: {
    position: 'absolute',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    top: 0,
    bottom: 0,
    right: inputElementTheme.paddingLeft,
    width: inputElementTheme.numberButtons.width,
  },
  numberBtnUp: {
    paddingVertical: inputElementTheme.numberButtons.verticalPadding,
    borderBottomWidth: inputElementTheme.numberButtons.dividerWidth,
    borderBottomColor: inputElementTheme.numberButtons.dividerColor,
    width: '100%',
    alignItems: 'center',
  },
  numberBtnDown: {
    paddingVertical: inputElementTheme.numberButtons.verticalPadding,
    width: '100%',
    alignItems: 'center',
  },
  inputAreaFocused: {
    borderColor: 'transparent',
    paddingTop: inputElementTheme.paddingTop - inputElementTheme.borderWidth,
    // @ts-ignore
    boxSizing: 'border-box',
    outlineStyle: inputElementTheme.focused.borderStyle,
    outlineWidth: inputElementTheme.focused.borderWidth,
    outlineOffset: -inputElementTheme.focused.borderWidth,
    outlineColor: inputElementTheme.focused.borderColor,
  },

  textarea: {
    position: 'relative',
    borderColor: inputElementTheme.borderColor,
    borderWidth: inputElementTheme.borderWidth,
    borderRadius: inputElementTheme.borderRadius,
    fontFamily: inputElementTheme.fontFamily,
    fontSize: inputElementTheme.fontSize,

    lineHeight: inputElementTheme.lineHeight,
    paddingBottom: inputElementTheme.paddingBottom,
    paddingTop: inputElementTheme.paddingTop - inputElementTheme.borderWidth,
    paddingRight: inputElementTheme.paddingRight,
    paddingLeft: inputElementTheme.paddingLeft - inputElementTheme.borderWidth,
    minHeight: standardElement.standardElementHeight,
    overflow: 'hidden',
    textAlignVertical: 'top',
  },
});

//+---------------------------------+
//|------Expert Search Styling------|
//+---------------------------------+

const expertSearchStyles = StyleSheet.create({
  tableContainer: {
    marginHorizontal: 'auto',
  },
  headerRow: {
    flexDirection: 'row',
    borderColor: expertSearchTableTheme.table.headerRow.borderColor,
    borderBottomWidth: expertSearchTableTheme.table.headerRow.borderBottomWidth,
  },
  headerCell: {
    borderColor: expertSearchTableTheme.table.headerCell.borderColor,
    backgroundColor: expertSearchTableTheme.table.headerCell.backgroundColor,
  },
  headerCellOnHover: {
    backgroundColor:
      expertSearchTableTheme.table.headerCell.backgroundColorOnHover,
  },
  headerPressable: {
    padding: expertSearchTableTheme.table.headerCell.padding,
    alignItems: 'center',
    flexDirection: 'row',
    gap: expertSearchTableTheme.table.headerCell.gap,
  },
  headerText: {
    flex: 1,
    display: 'flex',
    justifyContent: expertSearchTableTheme.table.headerCell.justifyContent,
    fontFamily: expertSearchTableTheme.table.headerCell.fontFamily,
    fontSize: expertSearchTableTheme.table.headerCell.fontSize,
    color: expertSearchTableTheme.table.headerCell.fontColor,
  },
  resizer: {
    position: 'absolute',
    right: -5,
    top: 0,
    bottom: 0,
    width: 10,
    zIndex: 5,
  },
  headerSelectBoxCell: {
    width: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.backgroundPrimary,
  },
  rowOnHover: {
    backgroundColor: theme.colors.backgroundGray,
  },
  cell: {
    display: 'flex',
    justifyContent: expertSearchTableTheme.table.cell.justifyContent,
    padding: expertSearchTableTheme.table.cell.padding,
    fontSize: expertSearchTableTheme.table.cell.fontSize,
    fontFamily: expertSearchTableTheme.table.cell.fontFamily,
    color: expertSearchTableTheme.table.cell.fontColor,
    borderColor: expertSearchTableTheme.table.cell.borderColor,
  },
  buttonText: {
    display: 'flex',
    justifyContent: expertSearchTableTheme.table.cell.justifyContent,
    textDecorationLine: expertSearchTableTheme.table.buttonCell.textDecoration,
    fontSize: expertSearchTableTheme.table.cell.fontSize,
    fontFamily: expertSearchTableTheme.table.buttonCell.fontFamily,
    color: expertSearchTableTheme.table.buttonCell.fontColor,
  },
  buttonOnHover: {
    textDecorationLine:
      expertSearchTableTheme.table.buttonCell.hover.textDecoration,
  },
  rowSelect: { width: 40, alignItems: 'center' },

  cardView: {
    gap: theme.space.large,
  },
  cardViewHeaderContainer: {
    flexDirection: expertSearchTableTheme.cardView.viewHeader.flexDirection,
    alignItems: expertSearchTableTheme.cardView.viewHeader.alignItems,
    justifyContent: expertSearchTableTheme.cardView.viewHeader.justifyContent,
  },
  cardViewListTitle: {
    justifyContent: 'flex-start',
    fontSize: expertSearchTableTheme.cardView.viewHeader.titleFontSize,
    fontFamily: expertSearchTableTheme.cardView.viewHeader.titleFontFamily,
    color: expertSearchTableTheme.cardView.viewHeader.titleFontColor,
  },
  cardSelectAllHeader: {
    gap: expertSearchTableTheme.cardView.viewHeader.checkboxLabelGap,
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  cardSelectAllHeaderText: {
    fontFamily:
      expertSearchTableTheme.cardView.viewHeader.checkboxLabelFontFamily,
    fontSize: expertSearchTableTheme.cardView.viewHeader.checkboxLabelFontSize,
    color: expertSearchTableTheme.cardView.viewHeader.checkboxLabelFontColor,
  },
  cardSeparator: {
    height: expertSearchTableTheme.cardView.gapBetweenCards,
  },
  card: {
    backgroundColor: expertSearchTableTheme.cardView.card.backgroundColor,
    padding: expertSearchTableTheme.cardView.card.padding,
    borderRadius: expertSearchTableTheme.cardView.card.borderRadius,
  },
  cardContainerHeader: {
    flexDirection: 'row',
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: expertSearchTableTheme.cardView.card.padding,
  },
  cardTitle: {
    fontSize: expertSearchTableTheme.cardView.card.header.fontSize,
    fontFamily: expertSearchTableTheme.cardView.card.header.fontFamily,
    color: expertSearchTableTheme.cardView.card.fontColor,
    flex: 1,
  },
  cardDivider: {
    height: expertSearchTableTheme.cardView.card.header.dividerWidth,
    backgroundColor: expertSearchTableTheme.cardView.card.header.dividerColor,
    marginHorizontal: -expertSearchTableTheme.cardView.card.padding,
  },
  cardButton: {
    backgroundColor:
      expertSearchTableTheme.cardView.card.button.backgroundColor,
    padding: expertSearchTableTheme.cardView.card.button.padding,
    borderRadius: expertSearchTableTheme.cardView.card.button.borderRadius,
    marginTop: expertSearchTableTheme.cardView.card.padding,
    fontSize: expertSearchTableTheme.cardView.card.button.fontSize,
    color: expertSearchTableTheme.cardView.card.button.fontColor,
    fontFamily: expertSearchTableTheme.cardView.card.button.fontFamily,
    textAlign: 'center',
  },
  cardButtonOnHover: {
    backgroundColor:
      expertSearchTableTheme.cardView.card.button.hover.backgroundColor,
  },
  cardButtonText: {
    fontSize: expertSearchTableTheme.cardView.card.button.fontSize,
    color: expertSearchTableTheme.cardView.card.button.fontColor,
    fontFamily: expertSearchTableTheme.cardView.card.button.fontFamily,
    textAlign: 'center',
  },
  cardRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: theme.space.extraLarge,
    paddingVertical: expertSearchTableTheme.cardView.card.row.padding,
    borderBottomWidth: expertSearchTableTheme.cardView.card.borderWidth,
    marginHorizontal: -expertSearchTableTheme.cardView.card.padding,
    borderBottomColor: expertSearchTableTheme.cardView.card.borderColor,
  },
  cardLabel: {
    fontSize: expertSearchTableTheme.cardView.card.fontSize,
    fontFamily: theme.fontFamily.bold,
    color: expertSearchTableTheme.cardView.card.fontColor,
    flex: 1,
  },
  cardValue: {
    fontSize: expertSearchTableTheme.cardView.card.fontSize,
    color: expertSearchTableTheme.cardView.card.fontColor,
  },

  emptyText: {
    fontSize: expertSearchTableTheme.noResultText.fontSize,
    fontFamily: expertSearchTableTheme.noResultText.fontFamily,
    color: expertSearchTableTheme.noResultText.fontColor,
    textAlign: expertSearchTableTheme.noResultText.textAlign,
    marginVertical: expertSearchTableTheme.noResultText.marginVertical,
  },
});

//+---------------------------------+
//|--------Error Box Styling--------|
//+---------------------------------+

const errorBoxStyles = StyleSheet.create({
  errorBoxFormWidthContainer: {
    marginHorizontal: 'auto',
  },
  errorBoxContainer: {
    backgroundColor: errorBoxTheme.listContainer.backgroundColor,
    marginBottom: errorBoxTheme.marginBottom,
    marginHorizontal: -theme.space.large,
  },
  errorBoxDescriptionContainer: {
    backgroundColor: errorBoxTheme.descriptionContainer.backgroundColor,
    paddingVertical: errorBoxTheme.descriptionContainer.paddingVertical,
    paddingHorizontal: theme.space.large,
  },
  errorBoxDescription: {
    fontSize: errorBoxTheme.descriptionContainer.fontSize,
    fontFamily: errorBoxTheme.descriptionContainer.fontFamily,
    color: errorBoxTheme.descriptionContainer.fontColor,
  },

  errorBoxListContainer: {
    paddingHorizontal: theme.space.large,
  },
  errorBoxList: {
    paddingVertical: errorBoxTheme.listContainer.paddingVertical,
    gap: errorBoxTheme.listContainer.gap,
  },
  errorBoxItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  errorText: {
    marginLeft: errorBoxTheme.listContainer.textIconSpacing,
    color: errorBoxTheme.listContainer.fontColor,
    fontFamily: errorBoxTheme.listContainer.fontFamily,
    fontSize: errorBoxTheme.listContainer.fontSize,
  },
});

//+---------------------------------+
//|---------Overlay Styling---------|
//+---------------------------------+

const defaultOverlayStyles = StyleSheet.create({
  modal: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: overlaysTheme.backdrop.color,
  },
  modalContent: {
    minWidth: 320,
    backgroundColor: overlaysTheme.modal.backgroundColor,
    borderRadius: overlaysTheme.modal.borderRadius,
    padding: overlaysTheme.modal.padding,
    gap: overlaysTheme.modal.itemsSpacing,
    maxWidth: overlaysTheme.breakpoints.modalWidth.default,
    maxHeight: overlaysTheme.breakpoints.modalHeight.default,
    paddingBottom: overlaysTheme.footer.padding,
  },
  modalContentWithoutFooter: {
    paddingBottom: overlaysTheme.modal.padding,
  },
  // ---- Equivalent to the media queries ----
  modalContentScreenMaxLarge: {
    maxWidth: overlaysTheme.breakpoints.modalWidth.maximumLg,
    maxHeight: overlaysTheme.breakpoints.modalHeight.maximumLg,
  },
  modalContentScreenMaxMedium: {
    maxWidth: overlaysTheme.breakpoints.modalWidth.maximumMd,
    maxHeight: overlaysTheme.breakpoints.modalHeight.maximumMd,
  },
  modalContentScreenMaxSmall: {
    flex: 1,
    borderRadius: 0,
    maxHeight: overlaysTheme.breakpoints.modalHeight.maximumSm,
    maxWidth: overlaysTheme.breakpoints.modalWidth.maximumSm,
  },
  // -----------------------------------------
  body: {
    flex: 1,
    gap: overlaysTheme.modal.itemsSpacing,
  },
  bodyScrollView: {
    flexGrow: 0,
  },
  bodyText: {
    textAlign: overlaysTheme.modal.textAlign,
  },
  debugArea: {
    marginTop: theme.space.extraLarge,
  },
});

const overlayButtonRowStyles = StyleSheet.create({
  buttonRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  buttonWrapper: {
    paddingHorizontal: overlaysTheme.buttonRow.spacing,
    // ---- simuliert min-width: fit-content ----
    alignSelf: 'flex-start',
    width: 'auto',
    minWidth: 'auto',
    maxWidth: '100%',
    flex: 1,
    // ------------------------------------------
  },
  buttonWrapperScreenMaxSmall: {
    paddingVertical: overlaysTheme.buttonRow.spacing,
  },
});

const overlayFooterStyles = StyleSheet.create({
  footer: {
    borderTopWidth: overlaysTheme.footer.borderWidth,
    borderTopColor: overlaysTheme.footer.borderColor,
    marginHorizontal: -overlaysTheme.modal.padding,
    paddingTop: overlaysTheme.footer.padding,
  },
  footerText: {
    fontSize: overlaysTheme.footer.fontSize,
    color: overlaysTheme.footer.color,
    paddingHorizontal: overlaysTheme.modal.padding,
    textAlign: overlaysTheme.modal.textAlign,
  },
});

const overlayHeaderStyles = StyleSheet.create({
  headerContent: {
    flexDirection: 'column',
    alignItems: overlaysTheme.modal.textAlign,
    gap: overlaysTheme.modal.itemsSpacing,
  },
  headerIconHeight: {
    height: overlaysTheme.header.iconSize,
  },
  headerIconColor: {
    color: overlaysTheme.header.iconColor,
  },
  headerIcon: {
    //native and web unterschiedlich? todo
    color: overlaysTheme.header.iconColor,
    height: overlaysTheme.header.iconSize,
  },
  headerTitle: {
    flexWrap: 'wrap',
    fontSize: overlaysTheme.header.titleFontSize,
    fontWeight: overlaysTheme.header.titleFontWeight,
    textAlign: overlaysTheme.modal.textAlign,
  },
  closeButton: {
    position: 'absolute',
    right: overlaysTheme.closeButton.margin,
    top: overlaysTheme.closeButton.margin,
    padding: overlaysTheme.closeButton.size,
  },
  closeButtonIcon: {
    height: overlaysTheme.closeButton.iconSize,
  },
});

const overlayPreConditionStyles = StyleSheet.create({
  bodyAlign: {
    alignItems: 'center',
  },
  preconditionItem: {
    marginBottom: theme.space.extraSmall,
  },
  preconditionItemLink: {
    color: theme.colors.colorAccent,
    textDecorationLine: 'underline',
  },
  preconditionFirstLineText: {
    fontSize: theme.fontSize.regular,
    color: theme.colors.textPrimary,
  },
  preconditionSecondLineText: {
    fontSize: theme.fontSize.regular,
    color: theme.colors.textPrimary,
  },
});

const fullScreenOverlayStyles = StyleSheet.create({
  loadingOverlay: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 9999,
  },
});

//+---------------------------------+
//|------Form Paging Styling--------|
//+---------------------------------+

const pagingStyles = StyleSheet.create({
  formPagingContainer: {
    marginInline: 'auto',
    marginBlock: theme.space.extraLarge,
    gap: theme.space.regular,
  },
  dialogFormHeader: {
    marginInline: 'auto',
    marginBlock: theme.space.extraLarge,
  },
  formPagingContainerScreenMaximumMedium: {
    marginBlock: theme.space.large,
    flexDirection: 'row',
    gap: theme.space.regular,
    alignItems: 'center',
  },
  pagination: {
    flexDirection: 'row',
    gap: theme.space.regular,
  },
  pageHeaderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.space.regular,
  },
  pageLabel: {
    fontFamily: theme.fontFamily.bold,
  },
  pageLabelContainerMaximumMedium: {
    backgroundColor: paginationTheme.pageItems.backgroundColor.current,
    borderRadius: paginationTheme.pageItems.borderRadius,
    paddingHorizontal: 13,
    paddingVertical: theme.space.small,
  },
  pageLabelScreenMaximumMedium: {
    fontFamily: theme.fontFamily.extraBold,
    fontSize: theme.fontSize.extraLarge,
    color: theme.colors.textOnColor,
  },
  pageTitle: {
    color: paginationTheme.pageItems.contentColor.default,
    fontFamily: paginationTheme.pageItems.contentFontFamily.default,
  },
  pageHeaderHyphenText: {
    color: paginationTheme.pageItems.contentColor.default,
  },
  pageTitleScreenMaxiumMedium: {
    fontFamily: theme.fontFamily.semiBold,
    fontSize: theme.fontSize.extraLarge,
    color: paginationTheme.pageItems.backgroundColor.current,
  },
  pageItem: {
    position: 'relative',
    width: paginationTheme.pageItems.size,
    height: paginationTheme.pageItems.size,
    borderWidth: paginationTheme.pageItems.borderWidth,
    borderRadius: paginationTheme.pageItems.borderRadius,
    backgroundColor: paginationTheme.pageItems.backgroundColor.default,
    borderColor: paginationTheme.pageItems.borderColor.default,
    justifyContent: 'center',
    alignItems: 'center',
  },
  currentPageItem: {
    backgroundColor: paginationTheme.pageItems.backgroundColor.current,
    borderColor: paginationTheme.pageItems.borderColor.current,
  },
  disabledPageItem: {
    backgroundColor: paginationTheme.pageItems.backgroundColor.disabled,
    borderColor: paginationTheme.pageItems.borderColor.disabled,
  },
  hasErrorPageItem: {
    borderColor: paginationTheme.pageItems.borderColor.hasError,
    backgroundColor: paginationTheme.pageItems.backgroundColor.hasError,
  },
  number: {
    textDecorationLine: 'none',
    color: paginationTheme.pageItems.contentColor.default,
    fontFamily: paginationTheme.pageItems.contentFontFamily.default,
  },
  currentNumber: {
    color: paginationTheme.pageItems.contentColor.current,
    fontFamily: paginationTheme.pageItems.contentFontFamily.current,
  },
  disabledNumber: {
    color: paginationTheme.pageItems.contentColor.disabled,
    fontFamily: paginationTheme.pageItems.contentFontFamily.disabled,
    pointerEvents: 'none',
  },
  hasErrorNumber: {
    color: paginationTheme.pageItems.contentColor.hasError,
    fontFamily: paginationTheme.pageItems.contentFontFamily.hasError,
  },
});

//+---------------------------------+
//|----Form StickyLayer Styling-----|
//+---------------------------------+

const stickyLayerStyles = StyleSheet.create({
  combinedStickyLayerContainer: {
    bottom: 0,
    ...(Platform.OS === 'web' && { position: 'sticky' as any }),
  },
  stickylayerContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    bottom: 0,
    left: 0,
    backgroundColor: theme.colors.backgroundPrimary,
    borderTopWidth: theme.border.width.regular,
    borderTopColor: theme.border.color.darkGray,
    paddingHorizontal: theme.space.large,
    ...(Platform.OS === 'web' && { position: 'sticky' as any }),
  },
  buttonContainerWrapper: {
    flexDirection: 'row',
    marginHorizontal: theme.space.large,
  },
  buttonContainer: {
    flex: 1,
    alignSelf: 'center',
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
    alignItems: 'center',
    paddingVertical: theme.space.large,
    marginHorizontal: -5,
  },
  buttonContainerScreenMaxSmall: {
    gap: theme.space.regular,
  },
  expertSearchCheckMarksContainer: {
    borderRightWidth: 2,
    borderRightColor: theme.border.color.darkGray,
    marginRight: 5,
    paddingRight: 10,
    alignItems: 'center',
  },
  expertSearchCheckMarksMultiButtonContainer: {
    flex: 1,
    gap: 5,
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-end',
    maxWidth: buttonsTheme.width.normalView,
    maxHeight: buttonsTheme.height.normalView,
  },
});

//+---------------------------------+
//|------Form Button Styling--------|
//+---------------------------------+

const buttonStyles = StyleSheet.create({
  // +----+ Responsive Button Styles +----+
  smallBtn: {
    height: buttonsTheme.height.smallView,
    paddingHorizontal: 5,
  },
  smallText: {
    fontSize: buttonsTheme.fontSize.smallView,
  },
  btnWrapperScreenMaxSmall: {
    maxWidth: '100%',
  },

  // +----+ Basic Button Styles +----+
  btnWrapper: {
    maxWidth: '25%',
    alignSelf: 'flex-start',
    minWidth: 'auto',
    flex: 1,
    paddingHorizontal: buttonsTheme.marginHorizontal,
  },
  btn: {
    borderRadius: buttonsTheme.borderRadius,
    paddingHorizontal: buttonsTheme.paddingHorizontal,
    height: buttonsTheme.height.normalView,
    width: '100%',
  },
  btnText: {
    textAlign: buttonsTheme.textAlign,
    fontWeight: buttonsTheme.fontWeight,
    fontSize: buttonsTheme.fontSize.normalView,
    marginBlock: 'auto',
  },

  // DialogFormBackButton
  btnDialogFormBack: {
    maxWidth: '50%',
    alignSelf: 'flex-start',
    minWidth: 'auto',
    flex: 1,
  },

  // +----+ Button Variations Styles +----+
  // Primary Button
  btnPrimary: {
    backgroundColor: buttonsTheme.primary.backgroundColor,
  },
  btnPrimaryHover: {
    backgroundColor: buttonsTheme.primary.backgroundColorOnHover,
  },
  btnPrimaryText: {
    color: buttonsTheme.primary.fontColor,
  },
  btnPrimaryTextHover: {
    color: buttonsTheme.primary.fontColorOnHover,
  },
  btnPrimaryDisabled: {
    backgroundColor: buttonsTheme.primaryDisabled.backgroundColor,
  },
  btnPrimaryDisabledText: {
    color: buttonsTheme.primaryDisabled.fontColor,
  },
  // Secondary Button
  btnSecondary: {
    backgroundColor: buttonsTheme.secondary.backgroundColor,
    borderColor: buttonsTheme.secondary.fontColor,
    borderWidth: theme.border.width.regular,
  },
  btnSecondaryText: {
    color: buttonsTheme.secondary.fontColor,
  },
  btnSecondaryHover: {
    backgroundColor: buttonsTheme.secondary.backgroundColorOnHover,
    borderColor: buttonsTheme.secondary.fontColorOnHover,
  },
  btnSecondaryTextHover: {
    color: buttonsTheme.secondary.fontColorOnHover,
  },
  btnSecondaryDisabled: {
    backgroundColor: buttonsTheme.secondaryDisabled.backgroundColor,
    borderColor: buttonsTheme.secondaryDisabled.fontColor,
    borderWidth: theme.border.width.regular,
  },
  btnSecondaryDisabledText: {
    color: buttonsTheme.secondaryDisabled.fontColor,
  },
  // Btn DialogFormBack
  btnDialogFormBackStyle: {
    backgroundColor: buttonsTheme.secondary.backgroundColor,
    borderColor: buttonsTheme.secondary.fontColor,
    borderWidth: theme.border.width.regular,
  },
  btnDialogFormBackText: {
    color: buttonsTheme.secondary.fontColor,
  },
  btnDialogFormBackStyleHover: {
    backgroundColor: buttonsTheme.secondary.backgroundColorOnHover,
    borderColor: buttonsTheme.secondary.fontColorOnHover,
  },
  btnDialogFormBackTextHover: {
    color: buttonsTheme.secondary.fontColorOnHover,
  },
});

//+---------------------------------+
//|------Form Wrapper Styling-------|
//+---------------------------------+

const formElementWrapperStyles = StyleSheet.create({
  wrapper: {
    flexDirection: 'row',
    gap: formElementWrapperTheme.tooltip.tooltipIconSpacing,
  },
  formGroup: {
    flex: 1,
  },
  tooltipButton: {},
  tooltipButtonActive: {},
  tooltipIcon: {
    fontSize: formElementWrapperTheme.tooltip.tooltipIconSize,
    color: formElementWrapperTheme.tooltip.tooltipIconColor,
  },
  tooltipIconActive: {},
  tooltipContainer: {
    height: baseStylesTheme.formElementHeight,
    justifyContent: 'center',
  },
  tooltipText: {
    color: formElementWrapperTheme.tooltip.tooltipContentFontColor,
    fontSize: formElementWrapperTheme.tooltip.tooltipContentFontSize,
    flexWrap: 'wrap',
  },
  toolTipContent: {
    maxWidth: formElementWrapperTheme.tooltip.tooltipContentMaxWidth,
    width: 'auto',
    backgroundColor:
      formElementWrapperTheme.tooltip.tooltipContentBackgroundColor,
    paddingHorizontal:
      formElementWrapperTheme.tooltip.tooltipContentHorizontalPadding,
    paddingVertical:
      formElementWrapperTheme.tooltip.tooltipContentVerticalPadding,
    borderRadius: formElementWrapperTheme.tooltip.tooltipContentBorderRadius,
    shadowColor: formElementWrapperTheme.tooltip.tooltipContentShadow.color,
    shadowOffset: formElementWrapperTheme.tooltip.tooltipContentShadow.offset,
    shadowOpacity: formElementWrapperTheme.tooltip.tooltipContentShadow.opacity,
    shadowRadius: formElementWrapperTheme.tooltip.tooltipContentShadow.radius,
  },
  toolTip: {
    // borderRadius: formElementWrapperTheme.tooltip.tooltipContentBorderRadius,
    shadowOpacity: 0,
  },
  caretUp: {
    width: 0,
    height: 0,
    borderLeftWidth: formElementWrapperTheme.toolTipCaret.size,
    borderRightWidth: formElementWrapperTheme.toolTipCaret.size,
    borderBottomWidth: formElementWrapperTheme.toolTipCaret.size,
    borderLeftColor: 'transparent',
    borderRightColor: 'transparent',
    borderBottomColor:
      formElementWrapperTheme.tooltip.tooltipContentBackgroundColor,
    alignSelf: 'center',
  },
  caretRight: {
    width: 0,
    height: 0,
    right: -formElementWrapperTheme.toolTipCaret.size,
    marginTop: -formElementWrapperTheme.toolTipCaret.size,
    borderTopWidth: formElementWrapperTheme.toolTipCaret.size,
    borderBottomWidth: formElementWrapperTheme.toolTipCaret.size,
    borderLeftWidth: formElementWrapperTheme.toolTipCaret.size,
    borderTopColor: 'transparent',
    borderBottomColor: 'transparent',
    borderLeftColor:
      formElementWrapperTheme.tooltip.tooltipContentBackgroundColor,
    position: 'absolute',
    top: '50%',
  },
  bottomMessages: {
    marginTop: formElementWrapperTheme.bottomMessageSpacing,
    paddingRight: formElementWrapperTheme.bottomMessageRightSpacing,
  },
  needSpacing: {
    height: 5,
    width: inputElementTheme.numberButtons.width,
  },
  inputItemsRight: {
    zIndex: 9999,
    position: 'absolute',
    flexDirection: 'row',
    alignItems: 'center',
    pointerEvents: 'none',
    height: baseStylesTheme.formElementHeight,
    right: baseStylesTheme.formElementHorizontalPadding,
    gap: formElementWrapperTheme.rightIconsSpacing,
  },
  checkboxRightIcons: {
    height: baseStylesTheme.formElementCheckboxHeight,
  },
  validInputIcon: {
    color: formElementWrapperTheme.validationInputIcon.color,
    height: formElementWrapperTheme.validationInputIcon.size,
    width: formElementWrapperTheme.validationInputIcon.size,
  },
  deleteContentBtnIcon: {
    color: formElementWrapperTheme.deleteContentBtnIcon.color,
    height: formElementWrapperTheme.deleteContentBtnIcon.size,
    width: formElementWrapperTheme.deleteContentBtnIcon.size,
  },
  anFieldLoadErrorIcon: {
    height: formElementWrapperTheme.anFieldLoadErrorIcon.size,
    width: formElementWrapperTheme.anFieldLoadErrorIcon.size,
    color: formElementWrapperTheme.anFieldLoadErrorIcon.color,
  },
  formElementWrapperLoader: {
    height: formElementWrapperTheme.loaderIcon.size,
    width: formElementWrapperTheme.loaderIcon.size,
    color: formElementWrapperTheme.loaderIcon.color,
  },

  // Form Element Error
  formFeedbackContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.space.extraSmall,
    marginHorizontal: theme.space.extraSmall,
  },
  errorText: {
    color: standardElement.validationError.color,
    fontSize: standardElement.validationError.fontSize,
    fontFamily: standardElement.validationError.fontFamily,
  },
  errorIcon: {
    color: standardElement.validationError.color,
    fontSize: standardElement.validationError.fontSize,
  },
  actaNovaErrorText: {
    color: theme.colors.textPrimary,
    fontSize: standardElement.actaNovaError.fontSize,
    fontFamily: standardElement.actaNovaError.fontFamily,
  },
  actaNovaErrorIcon: {
    color: standardElement.actaNovaError.color,
    fontSize: standardElement.actaNovaError.fontSize,
  },
  elementLoader: {},
});

const formWrapperLabelStyles = StyleSheet.create({
  labelContainer: {
    zIndex: 1,
    position: 'absolute',
    top: 20,
    left: 22,
    fontSize: 12,
    pointerEvents: 'none',
  },
  labelText: {
    fontSize: theme.fontSize.extraLarge,
    color: '#736e74',
  },
  labelTextError: {
    color: theme.colors.colorDeleteRed,
  },
});

const utilStyles = StyleSheet.create({
  visualyHidden: { display: 'none' },
});

//+---------------------------------+
//|--------Summary Styling----------|
//+---------------------------------+

const summaryStyles = StyleSheet.create({
  summaryContainer: {},
  summaryHeader: {
    paddingRight: theme.space.large,
    paddingTop: theme.space.large,
    paddingBottom: theme.space.large,
  },
  summaryRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: theme.space.large,
    paddingBottom: 0,
    borderRadius: theme.border.borderRadius.large,
    rowGap: theme.space.large,
  },
  rowEven: {
    backgroundColor: theme.colors.backgroundGray,
  },
  rowOdd: {
    backgroundColor: 'transparent',
  },
  summaryCell: {
    flexDirection: 'column',
  },
  labelText: {
    fontFamily: summaryTheme.elements.label.fontFamily,
    color: summaryTheme.elements.label.fontColor,
    fontSize: summaryTheme.elements.label.fontSize,
  },

  sectionHeader: {
    fontFamily: theme.fontFamily.bold,
    fontSize: theme.fontSize.extraLarge,
    color: theme.colors.textPrimary,
    paddingVertical: theme.space.extraLarge,
  },
  summarySection: {
    borderBottomColor: theme.border.color.darkGray,
    borderBottomWidth: theme.border.width.regular,
    paddingVertical: theme.space.large,
  },
  elementWrapper: {
    flexDirection: 'column',
    marginBottom: theme.space.small,
  },
  elementLabel: {
    fontFamily: summaryTheme.elements.label.fontFamily,
    color: summaryTheme.elements.label.fontColor,
    fontSize: summaryTheme.elements.label.fontSize,
  },
  elementValue: {
    fontFamily: summaryTheme.elements.value.fontFamily,
    color: summaryTheme.elements.value.fontColor,
    fontSize: summaryTheme.elements.value.fontSize,
  },

  summaryDetailSection: {
    paddingVertical: theme.space.extraLarge,
    gap: theme.space.extraLarge,
  },
  subSection: {
    gap: theme.space.small,
  },
  subSectionHeader: {
    fontFamily: theme.fontFamily.bold,
    fontSize: 20,
    color: theme.colors.textPrimary,
  },
  detailDefinitionList: {
    gap: theme.space.extraSmall,
  },
  detailDefinitionRow: {
    flexDirection: 'row',
    gap: theme.space.small,
  },
  detailDefinitionLabel: {
    fontFamily: theme.fontFamily.semiBold,
    fontSize: theme.fontSize.regular,
    color: theme.colors.textPrimary,
  },
  detailDefinitionValue: {
    fontFamily: theme.fontFamily.regular,
    fontSize: theme.fontSize.regular,
    color: theme.colors.textOnGray,
  },
});

//+---------------------------------+
//|-----Success Screen Styling------|
//+---------------------------------+

const successStyles = StyleSheet.create({
  successContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    //@ts-ignore
    height: '100vh',
  },
  successText: {
    fontSize: 25,
    textAlign: 'center',
    color: theme.colors.textOnGray,
    fontFamily: theme.fontFamily.semiBold,
    marginBottom: theme.space.large,
  },
  successIconBadge: {
    padding: 16,
    borderRadius: 99,
    backgroundColor: theme.colors.backgroundPrimary,
    elevation: 6,
    shadowColor: theme.shadow.color.primary,
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: theme.shadow.opacity.regular,
    shadowRadius: theme.shadow.radius.small,
  },
  successIcon: {
    fontSize: theme.fontSize.extraLarge,
    color: theme.colors.successGreen,
  },
  containerMaximumLg: {
    paddingVertical: 84,
    paddingHorizontal: 74,
  },
  containerMinimumLg: {
    paddingVertical: 84,
    paddingHorizontal: 106,
  },
  htmlBox: {
    width: '100%',
    textAlign: 'left',
    lineHeight: 1.5,
    marginTop: 32,
  },
});

//+---------------------------------+
//|-----Formgrid Image Styling------|
//+---------------------------------+

const formGridImageElementStyles = StyleSheet.create({
  container: {
    alignSelf: 'stretch',
    width: '100%',
    maxWidth: '100%',
  },
  caption: {
    marginTop: formGridImageElementTheme.caption.marginTop,
    fontSize: formGridImageElementTheme.caption.fontSize,
    fontFamily: formGridImageElementTheme.caption.fontFamily,
    color: formGridImageElementTheme.caption.color,
    opacity: formGridImageElementTheme.caption.opacity,
  },
});

//+---------------------------------+
//|---reCAPTCHA Element Styling-----|
//+---------------------------------+

const recaptchaElementStyles = StyleSheet.create({
  container: {
    alignItems: 'center',
  },
  missingKeyError: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.space.extraSmall,
    marginHorizontal: theme.space.extraSmall,
  },
  missingKeyErrorIcon: {
    color: standardElement.validationError.color,
    fontSize: standardElement.validationError.fontSize,
  },
  missingKeyErrorText: {
    color: standardElement.validationError.color,
    fontSize: standardElement.validationError.fontSize,
    fontFamily: standardElement.validationError.fontFamily,
  },
});

//+---------------------------------+
//|--------Debug Mode Styling-------|
//+---------------------------------+

const debugModeSectionStyles = StyleSheet.create({
  section: {
    marginBottom: 10,
    width: '100%',
  },
  inlineRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sectionLabel: {
    fontWeight: 'bold',
    marginRight: 5,
  },
  contentText: {
    fontSize: 14,
    color: '#333',
    flexShrink: 1,
  },
  scrollableArea: {
    height: 150,
    borderColor: '#ccc',
    borderWidth: 1,
    borderRadius: 4,
    padding: 5,
    width: '100%',
  },
  textarea: {
    flex: 1,
    textAlignVertical: 'top',
  },
  copyButton: {
    padding: 4,
    backgroundColor: '#eee',
    borderRadius: 4,
    marginHorizontal: 8,
  },
  copyButtonText: {
    fontSize: 12,
    color: '#007bff',
  },
});

export default baseTheme;
export {
  baseStyles,
  utilStyles,
  formElementWrapperStyles,
  pagingStyles,
  stickyLayerStyles,
  buttonStyles,
  errorBoxStyles,
  inputElementStyles,
  summaryStyles,
  booleanElementStyles,
  baseDropDownInputElement,
  autocompleteInputElementStyle,
  dropdownLazyCatalogElementStyles,
  dropdownReferenceElementStyles,
  dropdownCatalogElementStyles,
  binaryElementStyles,
  binaryFileCardStyles,
  pdfPreviewStyles,
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
