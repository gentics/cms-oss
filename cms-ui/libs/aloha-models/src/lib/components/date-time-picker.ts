import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaDateTimePickerComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.DATE_TIME_PICKER;

    value: Date | null;
    label?: string;
    format?: string;
    min?: Date;
    max?: Date;
    allowTime?: boolean;
    inline?: boolean;
    hoursLabel?: string;
    minutesLabel?: string;
    monthNames?: string[12];
    monthShort?: string[12];
    weekdayNames?: string[7];
    weekdayShort?: string[7];
    weekdayMinimal?: string[7];
    weekStart?: number;
}
