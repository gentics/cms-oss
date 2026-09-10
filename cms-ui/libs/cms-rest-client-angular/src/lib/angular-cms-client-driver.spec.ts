import { HttpResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ResponseCode, UserListResponse } from '@gentics/cms-models';
import { of } from 'rxjs';
import { GCMSRestClientModule } from './cms-rest-client.module';
import { GCMSRestClientService } from './cms-rest-client.service';

it('should handle the response correctly', async () => {
    let requestCounter = 0;
    const RESPONSE: UserListResponse = {
        responseInfo: {
            responseCode: ResponseCode.OK,
        },
        messages: [],
        hasMoreItems: false,
        numItems: 0,
        items: [],
    };

    TestBed.configureTestingModule({
        imports: [
            GCMSRestClientModule,
        ],
        providers: [
            provideHttpClient(
                withInterceptors([
                    (req) => {
                        requestCounter++;

                        return of(new HttpResponse({
                            status: 200,
                            statusText: 'OK',
                            url: req.url,
                            body: RESPONSE,
                        }));
                    },
                ]),
            ),
        ],
    });

    const client = TestBed.inject(GCMSRestClientService);
    const request = client.user.list();
    const res = await request.toPromise();

    expect(requestCounter).toEqual(1);
    expect(res).toEqual(RESPONSE);
});
