// En dev: on passe par le proxy Vite (même origin) pour éviter les soucis de cookies SameSite.
// En prod: on peut pointer vers une URL de backend via VITE_BACKEND_URL.
export const BACKEND_URL = import.meta.env.DEV ? '' : (import.meta.env.VITE_BACKEND_URL || '');
