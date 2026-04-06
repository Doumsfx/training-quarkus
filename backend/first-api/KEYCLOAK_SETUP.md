# Configuration Keycloak pour VSafe-Next

## Clients à créer dans Keycloak

### 1. Client Web-App (vsafe-web-app)
**Pour le Backend Quarkus**

1. Accédez à Keycloak Admin Console (http://localhost:8180)
2. Sélectionnez le realm **vsafe-next**
3. Allez dans **Clients** → **Create client**
4. Configurez:
   - **Client ID:** `vsafe-web-app`
   - **Client type:** OpenID Connect
   - **Name:** VSafe Web App Backend

5. Onglet **Capability config**:
   - ✅ Client authentication: **ON**
   - Authentication flow: Check **Standard flow**, **Direct access grants**

6. Onglet **Access settings** (après création):
   - **Valid redirect URIs:** 
     ```
     http://localhost:8080/*
     http://localhost:8080/auth/callback
     ```
   - **Valid post logout redirect URIs:**
     ```
     http://localhost:8080/logout
     ```
   - **Web origins:** `http://localhost:8080`, `http://localhost:5173`

7. Onglet **Credentials**:
   - Copier le **Client secret** et le mettre dans `application.properties`

### 2. Client SPA (vsafe-frontend)
**Pour le Frontend React/Vue**

1. **Create client**:
   - **Client ID:** `vsafe-frontend`
   - **Client type:** Single-Page App (SPA)
   - **Name:** VSafe Frontend

2. Onglet **Access settings**:
   - **Valid redirect URIs:**
     ```
     http://localhost:5173
     http://localhost:5173/*
     ```
   - **Valid post logout redirect URIs:**
     ```
     http://localhost:5173
     ```
   - **Web origins:** `http://localhost:5173`

3. Onglet **Advanced settings**:
   - **Revoke refresh on logout:** ON

### 3. Client Service Account (vsafe-admin-service)
**Pour les services backend qui ont besoin d'accès admin (optionnel)**

1. **Create client**:
   - **Client ID:** `vsafe-admin-service`
   - **Client type:** OpenID Connect
   - **Client authentication:** ON

2. Onglet **Service account roles**:
   - Ajouter les rôles nécessaires depuis le realm ou les clients

## Configuration Backend (application.properties)

```properties
# Authentification OIDC
quarkus.oidc.auth-server-url=http://localhost:8180/realms/vsafe-next
quarkus.oidc.client-id=vsafe-web-app
quarkus.oidc.credentials.secret=YOUR_CLIENT_SECRET_HERE
quarkus.oidc.application-type=web-app
quarkus.oidc.token.principal-claim=preferred_username

# Session et cookies
quarkus.oidc.authentication.cookie-path=/
quarkus.session.timeout=30m

# CORS pour le frontend
quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:5173,http://localhost:8080
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS
quarkus.http.cors.headers=content-type,authorization

# API Admin Keycloak pour les statistiques
quarkus.rest-client.keycloak-admin.url=http://localhost:8180
```

## Configuration Frontend

### Exemple avec React:
```javascript
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8180',
  realm: 'vsafe-next',
  clientId: 'vsafe-frontend'
});

// Init et redirection login
keycloak.init({ onLoad: 'login-required', checkLoginIframe: false })
  .then(authenticated => {
    if (authenticated) {
      console.log('Utilisateur authentifié:', keycloak.tokenParsed);
    }
  });
```

## Endpoints disponibles

- `GET /auth/login` - Redirection vers Keycloak login
- `GET /auth/callback` - Callback après authentification (géré automatiquement)
- `GET /auth/users/count` - **Compte les utilisateurs** (nécessite authentification)
- `GET /auth/logout` - Déconnexion

## Flux d'authentification

1. L'utilisateur accède au frontend
2. Frontend redirige vers Backend `/auth/login`
3. Backend (Quarkus) redirige vers Keycloak
4. Utilisateur se connecte à Keycloak
5. Keycloak redirige vers `/auth/callback`
6. Backend valide le token et crée une session
7. Frontend peut maintenant appeler `/auth/users/count`

## Test avec curl

```bash
# Obtenir un token
TOKEN=$(curl -X POST http://localhost:8180/realms/vsafe-next/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=vsafe-web-app" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=YOUR_USERNAME" \
  -d "password=YOUR_PASSWORD" | jq -r '.access_token')

# Compter les utilisateurs
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/auth/users/count
```
