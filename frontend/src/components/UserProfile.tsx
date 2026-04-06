import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthProvider';
import { getRealmUserCount } from '../services/adminService';

export const UserProfile = () => {
  const { user, accessToken, isAuthenticated, isLoading, error, logout } = useAuth();
  const [userCount, setUserCount] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    if (!isAuthenticated || !user || !accessToken) {
      setUserCount(null);
      return;
    }

    const loadUserCount = async () => {
      const result = await getRealmUserCount(accessToken);
      if (cancelled) return;

      if (result.kind === 'ok') {
        setUserCount(result.count);
        return;
      }
      setUserCount(null);
    };

    loadUserCount();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, user, accessToken]);

  if (isLoading) {
    return <div>Chargement...</div>;
  }

  if (error) {
    return <div style={{ color: 'red' }}>Erreur: {error}</div>;
  }

  if (!isAuthenticated || !user) {
    return <div>Non authentifié</div>;
  }

  return (
    <div style={{ padding: '1rem', border: '1px solid #ccc', borderRadius: '4px' }}>
      <h2>Profil utilisateur</h2>
      <p><strong>Username:</strong> {user.username}</p>
      <p><strong>Email:</strong> {user.email}</p>
      <p><strong>Prénom:</strong> {user.firstName}</p>
      <p><strong>Nom:</strong> {user.lastName}</p>
      <p><strong>Authentifié:</strong> {isAuthenticated ? 'Oui' : 'Non'}</p>
      <p><strong>User count (realm):</strong> {userCount ?? '—'}</p>
      <button onClick={logout}>Se déconnecter</button>
    </div>
  );
};
