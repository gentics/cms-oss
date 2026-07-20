import { ComponentRef, ElementRef, EmbeddedViewRef, Injector, TemplateRef, Type, ViewContainerRef, ViewRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ObservableViewContainerRef, OverlayHostService } from '@gentics/ui-core';

/**
 * Allows to create a spy on a dynamically created component.
 * @param componentTypes The component types on which a spy should be created.
 * @param setUpSpyFn The callback function that will set up the spy.
 */
/*export function spyOnDynamicallyCreatedComponent(
    componentTypes: Type<any>[],
    setUpSpyFn: (componentType: Type<any>, componentInstance: ComponentRef<any>) => void,
): void {
    const componentFactoryResolver: ComponentFactoryResolver = TestBed.inject(ComponentFactoryResolver);
    const origResolveFn = componentFactoryResolver.resolveComponentFactory.bind(componentFactoryResolver);

    spyOn(componentFactoryResolver, 'resolveComponentFactory').and.callFake((componentType: Type<any>) => {
        const componentFactory: ComponentFactory<any> = origResolveFn(componentType);

        // Only spy on component types we are interested in.
        if (componentTypes.findIndex(type => type === componentType) !== -1) {
            const origCreateFn = componentFactory.create.bind(componentFactory);
            spyOn(componentFactory, 'create').and.callFake((...args: any[]) => {
                const component: ComponentRef<any> = origCreateFn(...args);
                setUpSpyFn(componentType, component);
                return component;
            });
        }

        return componentFactory;
    });
}*/



export async function spyOnDynamicallyCreatedComponent(
    componentTypes: Type<any>[],
    setUpSpyFn: (
        componentType: Type<any>,
        componentInstance: ComponentRef<any>
    ) => void
): Promise<void> {
    const overlayService = TestBed.inject(OverlayHostService);
    const hostView = await  overlayService.getHostView();

    console.log(hostView);
    console.log(hostView.constructor.name);

    // Ensure that the overlay service is using the ObservableViewContainerRef wrapper
    if (!(hostView instanceof ObservableViewContainerRef)) {
        throw new Error(
            'OverlayService hostView must be an ObservableViewContainerRef'
        );
    }

    // Register the component types that the test is interested in
    hostView.registerComponentSpy(componentTypes, setUpSpyFn);
}