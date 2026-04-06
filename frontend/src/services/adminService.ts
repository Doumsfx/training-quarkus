import { BACKEND_URL } from './config';

export type RealmUserCountResult =
  | { kind: 'ok'; count: number }
  | { kind: 'forbidden' }
  | { kind: 'error' };

export async function getRealmUserCount(accessToken: string): Promise<RealmUserCountResult> {
  try {
    const response = await fetch(`${BACKEND_URL}/api/admin/users/count`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (response.status === 403) {
      return { kind: 'forbidden' };
    }

    if (!response.ok) {
      return { kind: 'error' };
    }

    const data: { count: number } = await response.json();
    if (typeof data.count !== 'number') {
      return { kind: 'error' };
    }

    return { kind: 'ok', count: data.count };
  } catch {
    return { kind: 'error' };
  }
}
