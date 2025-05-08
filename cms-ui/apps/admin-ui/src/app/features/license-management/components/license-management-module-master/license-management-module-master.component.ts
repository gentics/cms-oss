import { ChangeDetectionStrategy, Component } from '@angular/core';
import { License } from '@gentics/cms-models';

export enum LicenseManagementModuleTabs {
    OVERVIEW = 'overview',
    CONTENT_REPOSITORIES = 'content-repositories',
}

@Component({
    selector: 'gtx-license-management-module-master',
    templateUrl: './license-management-module-master.component.html',
    styleUrls: ['./license-management-module-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LicenseManagementModuleMasterComponent {

    readonly LicenseModuleTabs = LicenseManagementModuleTabs;

    public activeTab: LicenseManagementModuleTabs = LicenseManagementModuleTabs.OVERVIEW;
    public currentLicense: License | null = null;

    constructor() {}

    setTabAsActive(tabId: LicenseManagementModuleTabs): void {
        this.activeTab = tabId;
    }

    setCurrentLicense(license: License | null): void {
        this.currentLicense = license;
    }
}
