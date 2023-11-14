import { ActivatedRoute, PRIMARY_OUTLET } from '@angular/router';
import { ROUTE_DETAIL_OUTLET } from '@admin-ui/common';

export function getFullPrimaryPath(route: ActivatedRoute): string {
    const fullPath = [];

    for (const segment of route.pathFromRoot) {
        const snapshot = segment.snapshot;

        // If we have reached another outlet, we don't want to include it anymore.
        // This is because segments other than the main segment are stored in the primary outlet again.
        // i.E. "/example/(dummy:detail/1/foobar)", here the "detail" would be correctly set to "dummy",
        // but the "1/foobar" is then assigned to primary again for some reason, which isn't what we need.
        if (snapshot.outlet !== PRIMARY_OUTLET) {
            break;
        }
        snapshot.url.forEach((part) => fullPath.push(part));
    }

    const urlSegments = fullPath
        .map((part) => part.path)
        .filter((str) => str != null && str.length > 0);
    const fullUrl = `/${urlSegments.join('/')}`;

    return fullUrl;
}
