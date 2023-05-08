interface BaseTab {
    id: string;
    label: string;
}

export interface Tab extends BaseTab {
    isGroup: false;
    icon?: string;
}

export interface TabGroup extends BaseTab {
    isGroup: true;
    expanded?: boolean;
    tabs: Tab[];
}
