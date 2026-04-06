# Auth Provider - Guide d'utilisation

## Structure

```
src/
├── contexts/
│   └── AuthContext.tsx          # Définition du contexte
├── providers/
│   └── AuthProvider.tsx         # Provider d'authentification
├── hooks/
│   └── useAuth.ts               # Hook pour accéder au contexte
├── components/
│   └── UserProfile.tsx          # Composant exemple
└── main.tsx                     # Wrappé avec AuthProvider
```

## Utilisation dans tes composants

### 1. Récupérer les infos utilisateur

```tsx
import { useAuth } from '../hooks/useAuth';

export const MyComponent = () => {
  const { user, isAuthenticated, isLoading, error, logout } = useAuth();

  if (isLoading) return <div>Chargement...</div>;
  if (error) return <div>Erreur: {error}</div>;

  return (
    <div>
      {isAuthenticated && (
        <>
          <p>Bienvenue {user?.username}!</p>
          <button onClick={logout}>Logout</button>
        </>
      )}
    </div>
  );
};
```

## Flow d'authentification

1. **Au chargement du frontend**: `AuthProvider` appelle `/auth/login`
   - Si pas authentifié → Quarkus redirige vers Keycloak
   - Si authentifié → Reste sur le frontend

2. **Après authentification**: Le provider récupère les infos via `/auth/userinfo`

3. **Dans tes composants**: Utilise le hook `useAuth()` pour accéder aux données

## Configuration

- **URL Backend**: Défini dans `.env.local`
  ```
  VITE_BACKEND_URL=http://localhost:8080
  ```

## Types disponibles

```tsx
interface UserInfo {
  username: string;
  timestamp: string;
}

interface AuthContextType {
  user: UserInfo | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  logout: () => void;
}
```

## Exemples de composants

### Afficher le username
```tsx
const { user } = useAuth();
return <span>{user?.username}</span>;
```

### Bouton logout
```tsx
const { logout } = useAuth();
return <button onClick={logout}>Déconnexion</button>;
```

### Redirection conditionnelle
```tsx
const { isAuthenticated, isLoading } = useAuth();

if (isLoading) return <LoadingScreen />;
if (!isAuthenticated) return <LoginPage />;
return <Dashboard />;
```
