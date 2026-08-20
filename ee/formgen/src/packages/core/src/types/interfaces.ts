import { DimensionValue, InteractionManager, TextStyle, ViewStyle } from 'react-native';

export interface IDropDownSelectOption {
  label: string;
  value: any;
}

export interface IRequestResult {
  success: boolean;
  data?: any; // Daten im Erfolgsfall
  error?: any; // Fehler im Fehlerfall
}

export interface IRenderHTMLTheme {
  error: {
    color: any;
  };
}

export interface ITheme {
  colors: {
    textPrimary: any;
    textMuted: any;
    textAccent: any;
    textOnGray: any;
    textOnColor: any;
    backgroundPrimary: any;
    backgroundAccent: any;
    backgroundGray: any;
    colorAccent: any;
    colorDeleteRed: any;
    lightRed: any;
    warningColor: any;
    [key: string]: any;
  };
  fontFamily: {
    thin?: any;
    thinItalic?: any;
    extraLight?: any;
    extraLightItalic?: any;
    light?: any;
    lightItalic?: any;
    regular: any;
    italic?: any;
    medium?: any;
    mediumItalic?: any;
    semiBold?: any;
    semiBoldItalic?: any;
    bold?: any;
    boldItalic?: any;
    extraBold?: any;
    extraBoldItalic?: any;
    black?: any;
    blackItalic?: any;
  };
  fontWeight: {
    light: any;
    regular: any;
    // medium: any,
    // semiBold: any,
    bold: any;
    black: any;
  };
  fontSize: {
    extraSmall: number;
    small: number;
    regular: number;
    large: number;
    extraLarge: number;
    huge?: number;
    extraHuge?: number;
  };
  border: {
    width: {
      regular: number;
    };
    color: {
      gray: any;
      darkGray: string;
    };
    borderRadius: {
      small: number;
      regular: number;
      large: number;
      mediumLarge?: number;
      extraLarge: number;
    };
  };
  space: {
    extraSmall: number;
    small: number;
    regular: number;
    large: number;
    extraLarge: number;
    huge?: number;
    extraHuge?: number;
  };
  shadow: {
    color: {
      primary: string;
    };
    opacity: {
      regular: number;
    };
    radius: {
      regular: number;
      small: number;
    };
  };
  screenTitle: any;
  container: ViewStyle;
}

export interface IStandardElement {
  fontSize: number;
  fontFamily: string;
  fontColor: string;
  labelFontSize: number;
  labelFontColor: string;
  labelFontFamily: string;
  borderColor: string;
  borderWidth: number;
  borderRadius: number
  backgroundColor: string;
  standardElementHeight: number;
  paddingLeft: number;
  paddingRight: number;
  paddingBottom: number;
  verticalElementGap: number;
  validationError: {
    color: any,
    fontSize: any,
    fontFamily: any
  },
  actaNovaError: {
    color: any,
    fontSize: any,
    fontFamily: any
  },
  elementLoader: {
    color: any,
    size: any
  }
}

export interface IErrorBoxTheme {
  marginBottom: any,
  descriptionContainer: {
    backgroundColor: string;
    fontColor: string;
    fontFamily: string;
    fontSize: number;
    paddingVertical: any,
  },
  listContainer: {
    backgroundColor: string;
    fontColor: string;
    fontFamily: string;
    fontSize: number;
    iconSize: number;
    paddingVertical: any;
    gap: number;
    textIconSpacing: any
  }
}
export interface IOverlayTheme {
  backdrop: {
    color: string;
  }
  closeButton: {
    margin: number;
    size: number;
    iconSize: number;
  }
  header: {
    iconColor: string;
    iconSize: number;
    titleFontSize: number;
    titleFontWeight: any;
  }
  modal: {
    backgroundColor: string;
    borderRadius: number;
    padding: number;
    maxHeight: DimensionValue;
    itemsSpacing: number;
    textAlign: any;
  }
  buttonRow: {
    spacing: number;
  }
  footer: {
    borderWidth: number;
    borderColor: string;
    padding: number;
    fontSize: number;
    color: string;
  }
  breakpoints: {
    modalWidth: {
      default: DimensionValue;
      maximumSm: DimensionValue;
      maximumMd: DimensionValue;
      maximumLg: DimensionValue;
    }
    modalHeight: {
      default: DimensionValue,
      maximumSm: DimensionValue,
      maximumMd: DimensionValue;
      maximumLg: DimensionValue;
    }
  }
}

export interface IPaginationTheme {
  pageItems: {
    size: any,
    backgroundColor: {
      default: string | undefined,
      current: string | undefined,
      disabled: string | undefined,
      hasError: string | undefined
    },
    borderRadius: number,
    borderWidth: number | undefined,
    borderColor: {
      default: string | undefined,
      current: string | undefined,
      disabled: string | undefined,
      hasError: string | undefined
    }
    contentColor: {
      default: string | undefined,
      current: string | undefined,
      disabled: string | undefined,
      hasError: string | undefined
    },
    contentFontFamily: {
      default: string | undefined,
      current: string | undefined,
      disabled: string | undefined,
      hasError: string | undefined
    },
  },
  animation?: {
    duration: {
      default: number;
      introBase: number;
      introStep: number;
      introMax: number;
      scale: number;
      entrance: number;
      textEntrance: number;
      exit: number;
    },
    delay: {
      default: number;
      stagger: number;
      text: number;
    },
    steps: {
      mass: number;
      damping: number;
      stiffness: number;
    }
  }
}

export interface IAggregateElementTheme {
  generalSpacing: number;
  tableView: {
    header: {
      borderColor: any;
      borderWidth: any;
      horizontalSpacing: any;
      fontFamily: any;
      color: any;
      fontSize: any;
      textAlign: any;
      alignItems: any;
    },
    actionButtons: {
      fontFamily: any,
      fontSize: any,
      actionColor: any,
      actionBackgroundColor: any,
      deleteColor: any,
      deleteBackgroundColor: any,
      iconTextGap: any,
      borderRadius: any,
      paddingVertical: any
    },
    cellOnHover: {
      backgroundColor: any,
    },
    smallView: {
      cardsGap: number,
      card: {
        backgroundColor: any,
        borderRadius: any,
        padding: any,
        borderWidth: any,
        borderColor: any,
        dividerWidth: any,
        dividerColor: any,
        fontFamily: any,
        fontSize: any,
        color: any,
      }
    }
  }
  noEntry: {
    fontSize: number;
    fontColor: string;
  };
  functionButtons: {
    gap: number;
    padding: number;
    paddingSmallView: number;
    add: {
      fontColor: any;
      fontSize: any;
      fontFamily: any;
      backgroundColor: any;
      borderWidth: any;
      borderColor: any;
      borderRadius: any;
      onHover: {
        fontColor: any;
        fontSize: any;
        fontFamily: any;
        backgroundColor: any;
        borderWidth: any;
        borderColor: any;
        borderRadius: any;
      };
      smallView: {
        fontColor: any;
        fontSize: any;
        fontFamily: any;
        backgroundColor: any;
        borderWidth: any;
        borderColor: any;
        borderRadius: any;
        size: any;
      };
    };
    delete: {
      fontColor: any;
      fontSize: any;
      fontFamily: any;
      backgroundColor: any;
      borderWidth: any;
      borderColor: any;
      borderRadius: any;
      onHover: {
        fontColor: any;
        fontSize: any;
        fontFamily: any;
        backgroundColor: any;
        borderWidth: any;
        borderColor: any;
        borderRadius: any;
      };
      smallView: {
        fontColor: any;
        fontSize: any;
        fontFamily: any;
        backgroundColor: any;
        borderWidth: any;
        borderColor: any;
        borderRadius: any;
      }
    };
    toggle: {
      fontColor: string;
      fontSize: number;
      fontFamily: string;
      iconTextGap: number;
      textDecoration: "none" | "underline" | "line-through";
      backgroundColor: string;
      borderWidth: number;
      borderColor: string;
      borderRadius: number;
      onHover: {
        fontColor?: any;
        fontSize?: any;
        fontFamily?: any;
        iconTextGap?: any;
        textDecoration?: "none" | "underline" | "line-through";
        backgroundColor: any;
        borderWidth?: any;
        borderColor?: any;
        borderRadius?: any;
      };
      onActive: {
        fontColor: any;
        fontSize: any;
        fontFamily: any;
        iconTextGap: any;
        textDecoration: any;
        backgroundColor: any;
        borderWidth: any;
        borderColor: any;
        borderRadius: any;
      };
      smallView: {
        fontColor: any,
        fontSize: any,
        fontFamily: any,
        textDecoration: any,
        backgroundColor: any,
        borderWidth: any,
        borderColor: any,
        borderRadius: any,
      }
    };
    uploadButton: {
      backgroundColor: string;
      fontSize: number;
      fontFamily: string;
      fontColor: string;
      borderColor: string;
      borderWidth: number;
      borderRadius: number;
      paddingHorizontal: number;
      paddingVertical: number;
      onHover: {
        backgroundColor: string;
        fontColor: string;
        fontSize: number;
        fontFamily: string;
        borderColor: string;
        transitionDuration: number;
      };
    };
  };
  filter: {
    filterSettingsAlignment: any;
    filterSettingsFlexDirection: any;
    appliedFilter: {
      alignment: any;
      backgroundColor: string;
      borderRadius: number;
      borderWidth: number;
      borderColor: string;
      padding: number;
      iconTextGap: number;
      fontSize: number;
      fontColor: string;
      fontFamily: string;
      labelFontFamily: string;
      iconColor: string;
      iconSize: number;
    };
    filterInput: {
      placeholderTopPosition: number;
      placeholderFontSize: number;
      focused: {
        placeholderTopPosition: number;
        borderColor: string;
        borderWidth: number;
        borderStyle: "solid" | "dotted" | "dashed" | undefined;
        placeholderFontSize: number;
      };
    };
    divider: {
      color: string;
      width: number;
    };
    addFilterButton: {
      backgroundColor: string;
      borderRadius: number;
      iconColor: string;
      iconSize: number;
      active: {
        backgroundColor: string;
        iconColor: string;
      };
      onHover: {
        backgroundColor: string;
        iconColor: string;
        transitionDuration: number;
      };
    };
  };
  pagination: {
    gap: number;
    marginVertical: number;
    paddingHorizontal: number;
    paddingVertical: number;
    borderWidth: number;
    borderRadius: number;
    fontSize: number;
    defaultButton: {
      backgroundColor: string;
      borderColor: string;
      fontFamily: string;
      fontColor: string;
    };
    activeButton: {
      backgroundColor: string;
      borderColor: string;
      fontFamily: string;
      fontColor: string;
    };
    disabledButton: {
      backgroundColor: string;
      borderColor: string;
      fontFamily: string;
      fontColor: string;
    };
  };
}

export interface IDropdownInputTheme {
  elementHeight: number;
  input: {
    backgroundColor: string;
    backgroundColorDisabled: string;
    borderColor: string;
    borderWidth: number;
    borderRadius: number;
    labelFontColor: string;
    fontColorDisabled: string;
    fontSize: number;
    fontColor: any;
    fontFamily: string;
    labelFontSize: number;
    labelTopPosition: number;
    activeLabelFontSize: number;
    activeLabelTopPosition: number;
  }
  dropdownList: {
    showScrollbar: boolean
    heightLimit: number;
    backgroundColor: string;
    borderColor: string;
    borderWidth: number;
    borderRadius: number;
    shadowColor: string;
    shadowOffset: any;
    shadowOpacity: number;
    shadowRadius: number;
    separatorColor: string;
    separatorWidth: number;
    checkMarkIconSize: number;
    checkMarkIconColor: string;
    checkMarkIconColorError: string;
    item: {
      padding: number;
      backgroundOnHover: string;
      backgroundErrorOnHover: string;
      fontSize: number;
      fontColor: string;
    }
    multiSelectItem: {
      backgroundColor: string;
      borderRadius: number;
      paddingVertical: number;
      paddingHorizontal: number;
      fontSize: number;
      fontColor: string;
      backgroundColorError: string;
      gap: number;
      iconSize: number;
      iconColor: string;
    }
    activityText: {
      padding: number;
      fontSize: number;
      fontColor: string;
      fontFamily: string;
    }
  }
  errorFontColor: string;
  errorBackgroundColor: string;
}

export interface IInputElementTheme {
  height: number;
  backgroundColor: string;
  borderColor: string;
  borderWidth: number;
  borderRadius: number;
  lineHeight: number;
  fontSize: number;
  fontColor: string;
  fontFamily: string;
  paddingLeft: number;
  paddingTop: number;
  paddingRight: number;
  paddingBottom: number;
  placeholderColor: string;
  placeholderTopPosition: number;
  placeholderFontSize: number;
  placeholderFontFamily: string;
  error: {
    backgroundColor: string;
    borderColor: string;
    fontColor: string;
  }
  focused: {
    borderColor: string;
    borderWidth: number;
    borderStyle: "solid" | "dotted" | "dashed" | undefined;
    placeholderTopPosition: number;
    placeholderFontSize: number;
  }
  disabled: {
    backgroundColor: string;
    fontColor: string;
  }
  numberButtons: {
    width: number;
    verticalPadding: number;
    dividerWidth: number;
    dividerColor: string;
    iconColor: string;
    iconSize: number;
  }
  unitBadge: {
    gap: number;
    paddingHorizontal: number;
  }
  isFilledIcon: {
    width: number;
  }
}

export interface IBinaryInputTheme {
  height: number;
  backgroundColor: string;
  backgroundColorDragOver: string;
  backgroundColorDisabled: string;
  backgroundColorError: string;
  borderColor: string;
  borderColorError: string;
  borderWidth: number;
  borderRadius: number;
  borderStyle: "solid" | "dotted" | "dashed" | undefined;

  fileListSpacing: number;
  fileListItemLinkFontColor: string;
  fileListItemTextFontColor: string;
  fileListItemFontSize: number;
  fileListItemTextDecoration: "none" | "underline" | "line-through" | undefined;
  fileListItemsSpacing: number;
  padding: number;

  fileIconColor: string;
  fileIconSize: number;
  deleteFileIconColor: string;
  deleteFileIconSize: number;

  labelFontSize: number,
  labelFontColor: any,
  labelFontFamily: any,
  errorLabelFontColor: any,
}

export interface IBooleanElementTheme {
  size: number;
  selectedColor: string;
  borderWidth: number;
  borderColor: string;
  disabledBooleanElementOpacity: number;
  iconLabelSpacing: number;
  labelFontSize: number;
  labelFontColor: string;
  labelFontFamily: string;
  labelPadding: number;
  selectedLabelFontFamily: string;
  selectedLabelFontColor: string;
  checkbox: {
    borderRadius: number;
    iconColor: string;
    iconFontSize: number;
  }
  radioButton: {
    dotSize: number;
    dotColor: string;
    groupLabelSpacing: number;
    groupLabelFontSize: number;
    groupLabelFontColor: string;
    optionsSpacing: number;
    groupMarginVertical: number;
  }
  button: {
    borderRadius: number;
    fontSize: number;
    disabledBackgroundColor: string;
    disabledBorderColor: string;
    disabledFontColor: string;
  }
  error: {
    labelFontColor: string;
    accentColor: string;
    lightColor: string;
  }
}

export interface IBaseStylesTheme {
  formContainerWidth: any
  formElementHeight: number
  formElementCheckboxHeight: number
  formElementHorizontalPadding: any
}

export interface IFormElementWrapperTheme {
  tooltip: {
    tooltipIconSpacing: number;
    tooltipContentFontColor: string;
    tooltipContentFontSize: number;
    tooltipContentMaxWidth: number;
    tooltipContentBackgroundColor: string;
    tooltipContentBorderRadius: number;
    tooltipContentVerticalPadding: number;
    tooltipContentHorizontalPadding: number;
    tooltipContentShadow: {
      color: string;
      offset: any;
      opacity: number;
      radius: number;
    };
    tooltipIconSize: number;
    tooltipIconColor: string;
  },
  toolTipCaret: {
    size: number;
  },
  bottomMessageSpacing: number;
  bottomMessageRightSpacing: number;
  rightIconsSpacing: number;
  validationInputIcon: {
    size: number;
    color: string;
  }
  deleteContentBtnIcon: {
    size: number;
    color: string;
  }
  anFieldLoadErrorIcon: {
    size: number;
    color: string;
  }
  loaderIcon: {
    size: number;
    color: string;
  }
  dropdownIcon: {
    size: number;
    color: string;
    colorError: string;
  }
  focusedBorder: {
    borderColor: string;
    borderWidth: number;
    borderStyle: "solid" | "dotted" | "dashed" | undefined;
  }
}

export interface IButtonsTheme {
  width: any;
  height: {
    normalView: number;
    smallView: number;
  };
  paddingHorizontal: number;
  marginHorizontal: number;
  borderRadius: number;

  fontWeight: any;
  fontSize: {
    normalView: number;
    smallView: number;
  };
  textAlign: "auto" | "left" | "right" | "center" | "justify" | undefined;

  primary: {
    backgroundColor: string;
    backgroundColorOnHover: string;
    fontColor: string;
    fontColorOnHover: string;
  };
  secondary: {
    backgroundColor: string;
    backgroundColorOnHover: string;
    fontColor: string;
    fontColorOnHover: string;
  }
  primaryDisabled: {
    backgroundColor: string;
    backgroundColorOnHover: string;
    fontColor: string;
    fontColorOnHover: string;
  };
  secondaryDisabled: {
    backgroundColor: string;
    backgroundColorOnHover: string;
    fontColor: string;
    fontColorOnHover: string;
  };
}

export interface IDateTimeElementTheme {
  input: {
    fontSize: number;
    fontFamily: string;
    fontColor: string;
    labelFontSize: number;
    labelFontFamily: string;
    labelFontColor: string;
    paddingTop: number;
    labelPaddingTop: number;
    paddingBottom: number;
    paddingLeft: number;
    paddingRight: number;
    borderWidth: number;
    borderColor: string;
    borderRadius: number;
    backgroundColor: string;
  }
  focused: {
    borderWidth: number;
    borderColor: string;
    borderStyle: "solid" | "dotted" | "dashed" | undefined;
    labelFontSize: number;
    labelPaddingTop: number;
  }
  error: {
    labelFontColor: string;
    borderColor: string;
    backgroundColor: string;
  }
  disabled: {
    fontColor: string;
    backgroundColor: string;
  }
}

export interface IExpertSearchTableTheme {
  noResultText: {
    fontSize: number;
    fontFamily: string;
    fontColor: string;
    textAlign: "auto" | "center" | "left" | "right" | "justify" | undefined;
    marginVertical: number;
  }
  table: {
    scrollable: boolean,
    headerRow: {
      borderColor: string;
      borderBottomWidth: number;
    };
    headerCell: {
      padding: number;
      gap: number;
      justifyContent: "center" | "flex-start" | "flex-end" | "center" | undefined;
      borderColor: string;
      borderWidth: number;
      backgroundColor: string;
      backgroundColorOnHover: string;
      fontFamily: string;
      fontSize: number;
      fontColor: string;
      iconSize: number;
      iconColor: string;
      hoverTransitionDuration: number; // in milliseconds
      checkboxCell: {
        checkedColor: string;
        uncheckedColor: string;
      }
    };
    row: {
      backgroundColor: string;
      backgroundColorOnHover: string;
      hoverTransitionDuration: number; // in milliseconds
    };
    cell: {
      justifyContent: "center" | "flex-start" | "flex-end" | "center" | undefined;
      padding: number;
      borderColor: string;
      borderWidth: number;
      fontFamily: string;
      fontSize: number;
      fontColor: string;
    };
    buttonCell: {
      textDecoration: "none" | "underline" | "line-through" | undefined;
      fontColor: string;
      fontFamily: string;
      hover: {
        textDecoration: "none" | "underline" | "line-through" | undefined;
        transitionDuration: number; // in milliseconds
      }
    }
  };
  cardView: {
    gapBetweenCards: number;
    viewHeader: {
      flexDirection: "row" | "column" | "row-reverse" | "column-reverse" | undefined;
      alignItems: "center" | "flex-start" | "flex-end" | undefined;
      justifyContent: any;
      checkboxLabelGap: number;
      checkboxLabelFontSize: number,
      checkboxLabelFontFamily: string,
      checkboxLabelFontColor: string,
      titleFontSize: number;
      titleFontFamily: string;
      titleFontColor: string;
    }
    card: {
      backgroundColor: string;
      padding: number;
      borderRadius: number;
      borderColor: string;
      borderWidth: number;
      fontColor: string;
      fontFamily: string;
      fontSize: number;
      row: {
        padding: number;
      }
      header: {
        fontSize: number;
        fontFamily: string;
        dividerColor: string;
        dividerWidth: number;
      }
      button: {
        backgroundColor: string;
        fontColor: string;
        fontSize: number;
        fontFamily: string;
        padding: number;
        borderRadius: number;
        hover: {
          backgroundColor: string;
          transitionDuration: number;
        }
      }
    }
  }
}

export interface ISummaryTheme {
  elements: {
    label: {
      fontSize: any,
      fontColor: any,
      fontFamily: any,
    }
    value: {
      fontSize: any,
      fontColor: any,
      fontFamily: any
    }
  }
}

export interface IFormGridImageElementTheme {
  caption: {
    marginTop: number;
    fontSize: number;
    fontFamily: string;
    color: string;
    opacity: number;
  };
}

export interface ICustomComponentsTheme {
  modal?: {
    backdropColor: string;
    backdropColorSmallView: string;
  };
  input: {
    backgroundColor: string;
    borderRadius: number;
    padding: number;
    heightForDesktop: number;
    horizontalPaddingForDesktop: number;
    iconTextInputGap: number;
    fontSize: number;
    fontColor: string;
    placeholderTextColor: string;
  };
  dropdown: {
    textColor: string;
    backgroundColor: string;
    shadowColor: string;
    shadowOffset: any;
    shadowOpacity: number;
    shadowRadius: number;
    elevation: number; // Android
    menuPadding: number;
    optionPadding: number;
    borderRadius: number;
  };
}
