import { createContext, useEffect, useContext, useRef, useState } from 'react';
import type { UserInfo } from '../types/auth';
import { BACKEND_URL } from '../services/config';
import { getSessionInfo } from '../services/authService';

export type AuthContextType = {
    user: UserInfo | null;
    accessToken: string | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;
    logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    // State pour stocker les infos utilisateur, le statut de chargement et les erreurs
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [accessToken, setAccessToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const hasCheckedRef = useRef(false);
    const redirectingRef = useRef(false);

    const isLoggedOut = () => sessionStorage.getItem('loggedOut') === '1';

    const checkAuth = async () => {
        try {
            const session = await getSessionInfo();

            if (session) {
                setUserInfo(session.user);
                setAccessToken(session.accessToken);
                sessionStorage.removeItem('loggedOut');
                return;
            }

            setUserInfo(null);
            setAccessToken(null);
        } catch (error) {
            console.error('Erreur lors de la vérification de l\'authentification:', error);
            setUserInfo(null);
            setAccessToken(null);
        } finally {
            setIsLoading(false);
        }
    };

    // Vérifie l'authentification au montage du composant
    useEffect(() => {
        // En StrictMode (dev), React peut monter/démonter/remonter -> on évite les doubles appels.
        if (hasCheckedRef.current) return;
        hasCheckedRef.current = true;
        checkAuth();
    }, []);

    useEffect(() => {
        if (!isLoading && !userInfo && !redirectingRef.current && !isLoggedOut()) {
            redirectingRef.current = true;
            sessionStorage.setItem('returnUrl', window.location.pathname + window.location.search);
            window.location.href = `${BACKEND_URL}/auth/login`;
        }
    }, [isLoading, userInfo]);

    const login = () => {
        sessionStorage.removeItem('loggedOut');
        sessionStorage.setItem('returnUrl', window.location.pathname + window.location.search);
        window.location.href = `${BACKEND_URL}/auth/login`;
    }

    const logout = () => {
        setUserInfo(null);
        setAccessToken(null);
        sessionStorage.setItem('loggedOut', '1');
        window.location.href = `${BACKEND_URL}/auth/logout`;
    }

    if (isLoading) {
        return <div>Chargement...</div>;
    }

    if (!userInfo) {
        if (isLoggedOut()) {
            return (
                <div>
                    <div>Déconnecté.</div>
                    <button onClick={login}>Se connecter</button>
                </div>
            );
        }

        return <div>Non authentifié. Redirection vers la page de connexion...</div>;
    }   

    return (
        <AuthContext.Provider value={{ user: userInfo, accessToken, isAuthenticated: !!userInfo, isLoading, error: null, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() { 
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth doit être utilisé à l\'intérieur d\'un AuthProvider');
    }   
    return context;
}