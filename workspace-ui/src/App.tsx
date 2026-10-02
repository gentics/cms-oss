import { RouterProvider } from '@tanstack/react-router';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
// import { useCmsToken } from '@/hooks/useCmsToken';
import { router } from '@/router';

import './i18n';

function App() {
    // Session start: check for a CMS token, or create one, as soon as the app loads.
    // Disabled for now.
    // useCmsToken();

    return (
        <>
            <RouterProvider router={router} />

            <ErrorNotifications />
        </>
    );
}

export default App;
