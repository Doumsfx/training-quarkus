import { BACKEND_URL } from './config';
import type { UserInfo } from '../types/auth';

export type SessionInfo = {
  user: UserInfo;
  accessToken: string | null;
};

export async function getSessionInfo(): Promise<SessionInfo | null> {
  const response = await fetch(`${BACKEND_URL}/auth/check`, {
    credentials: 'include',
  });

  if (!response.ok) {
    return null;
  }

  const user: UserInfo = await response.json();

  try {
    const tokenResp = await fetch(`${BACKEND_URL}/auth/token`, {
      credentials: 'include',
    });

    if (!tokenResp.ok) {
      return { user, accessToken: null };
    }

    const tokenData: { accessToken: string } = await tokenResp.json();
    return { user, accessToken: tokenData?.accessToken || null };
  } catch {
    return { user, accessToken: null };
  }
}
