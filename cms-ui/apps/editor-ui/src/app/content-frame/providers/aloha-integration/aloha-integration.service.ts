import { Injectable } from '@angular/core';
import { AlohaRangeObject, AlohaSettings, GCNAlohaPlugin } from '@gentics/aloha-models';
import { BehaviorSubject } from 'rxjs';
import { AlohaGlobal } from '../../components/content-frame/common';

@Injectable()
export class AlohaIntegrationService {

    public reference$ = new BehaviorSubject<AlohaGlobal>(null);
    public settings$ = new BehaviorSubject<AlohaSettings>(null);
    public contextChange$ = new BehaviorSubject<AlohaRangeObject>(null);
    public gcnPlugin$ = new BehaviorSubject<GCNAlohaPlugin>(null);

}
