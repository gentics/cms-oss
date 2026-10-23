import { RouterProvider } from '@tanstack/react-router';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
import { LoginGate } from '@/components/LoginGate/LoginGate';
import { router } from '@/router';

import './i18n';

function App() {
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
