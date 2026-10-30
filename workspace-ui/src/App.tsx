import { RouterProvider } from '@tanstack/react-router';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
import { LoginGate } from '@/components/LoginGate/LoginGate';
import { useCmsUiLanguage, useCmsUser } from '@/hooks/useCmsAuth';
import { router } from '@/router';

import './i18n';

function App() {
    // Once logged in, the Workspace takes the user's language from the CMS session.
    useCmsUiLanguage(Boolean(useCmsUser({ refetchOnMount: false }).data));

    return (
        <>
            <LoginGate>
                <RouterProvider router={router} />
            </LoginGate>

            <ErrorNotifications />
        </>
    );
}

export default App;
