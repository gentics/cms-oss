import React from 'react';
import { Platform } from 'react-native';
import CustomTooltip from './CustomTooltip';

type Placement = 'bottom' | 'left';

export type CustomTooltipProps = {
  isVisible: boolean;
  placement?: Placement;
  onClose?: () => void;
  content: string;
  contentStyle?: any;
  textStyle?: any;
  tooltipStyle?: any;
  caretUpStyle?: any;
  caretRightStyle?: any;
  children: React.ReactNode;
  closeOnOutsidePress?: boolean;
};
export default function CustomTooltipz(props: CustomTooltipProps) {
  return <CustomTooltip {...props} />;
}
