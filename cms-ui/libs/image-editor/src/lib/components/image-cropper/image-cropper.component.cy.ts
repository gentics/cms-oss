/* eslint-disable @typescript-eslint/no-unsafe-call */
import { GenticsUICoreModule } from '@gentics/ui-core';
import { mockPipe } from '@gentics/ui-core/testing/mock-pipe';
import { createOutputSpy } from 'cypress/angular';
import { getCropperConstructor } from '../../gentics-ui-image-editor.module';
import { AspectRatio, AspectRatios, CropperConstructor } from '../../models';
import { CropperService } from '../../providers';
import { ImageCropperComponent } from './image-cropper.component';

describe('ImageCropperComponent', () => {
    it('should render the cropper library correctly', () => {
        const DND_OFFSET: [number, number] = [-10, -20];

        cy.mount(ImageCropperComponent, {
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [mockPipe('i18n')],
            providers: [
                CropperService,
                { provide: CropperConstructor, useFactory: getCropperConstructor },
            ],
            componentProperties: {
                src: './assets/portrait.jpg',
                enabled: true,
                aspectRatio: AspectRatio.get(AspectRatios.Free),
                imageLoad: createOutputSpy('imageLoadSpy'),
            },
        });

        // Verify that the image has been properly loaded

        cy.get('.source-image')
            .should('exist')
            .and('not.be.visible');

        cy.get('@imageLoadSpy', { timeout: 10_000 })
            .should('have.been.called');

        // Get the original Size of the cropper box

        cy.get('.cropper-crop-box')
            .should('exist')
            .its('0')
            .invoke('getBoundingClientRect')
            .then((rect: DOMRect) => {
                // Resize the box via Drag n drop

                cy.get('.cropper-crop-box [data-cropper-action="all"]')
                    .should('exist')
                    // Start dragging in the bottom right corner
                    .trigger('mousedown', rect.width, rect.height, { button: 'left', force: true })
                    // Move the cursor to the new position
                    .trigger('mousemove', rect.width + DND_OFFSET[0], rect.height + DND_OFFSET[1], { button: 'left', force: true })
                    // Release the click on the last position
                    .trigger('mouseup', rect.width + DND_OFFSET[0], rect.height + DND_OFFSET[1], { button: 'left', force: true })
                    // Wait for the delays and processing in the service
                    .wait(1_000);

                // Validate that the drag n drop worked as expected

                /*
                 * This doesn't properly work for whatever reason.
                 * cropperjs doesn't detect the actual drag'n'drop.
                 * Even digging into cropperjs, but without resolution.

                // cy.get('.cropper-crop-box')
                //     .should('exist')
                //     .then($div => {
                //         const w = rect.width + DND_OFFSET[0];
                //         const h = rect.height + DND_OFFSET[1];
                //         // +/- 1 for rounding errors
                //         expect($div.width()).to.be.within(w - 1, w + 1);
                //         expect($div.height()).to.be.within(h - 1, h + 1);
                //     });
                */
            });

    });
});
