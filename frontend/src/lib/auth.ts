// OAuth 2.0 style bearer-token session.
// - The access token (a short-lived JWT) lives only in this module's memory, never in web storage.
// - The refresh token is an HttpOnly cookie the page cannot read; POST /api/auth/refresh trades it
//   for a new access token, so every page load starts with one refresh call.
import { jsonInit, requireJson } from './http.js';
import type { AuthResponse, SignupRequest, User } from './types.js';

/** The server only accepts auth calls that carry this header (blocks cross-site form posts). */
const AUTH_HEADERS: Record<string, string> = { 'X-Requested-With': 'fetch' };
const REFRESH_MARGIN_MS = 30_000;
const CONCURRENT_REFRESH_RETRY_MS = 300;

let accessToken: string | null = null;
let expiresAt = 0;
let sessionPromise: Promise<User | null> | null = null;
let refreshPromise: Promise<User | null> | null = null;

function remember(auth: AuthResponse): User {
  accessToken = auth.accessToken;
  expiresAt = Date.parse(auth.expiresAt);
  sessionPromise = Promise.resolve(auth.user);
  return auth.user;
}

function forget(): void {
  accessToken = null;
  expiresAt = 0;
  sessionPromise = Promise.resolve(null);
}

function postAuth(path: string, body?: unknown): Promise<Response> {
  const init = jsonInit('POST', body);
  const headers = { ...(init.headers as Record<string, string> | undefined), ...AUTH_HEADERS };
  return fetch(path, { ...init, headers, credentials: 'same-origin' });
}

const delay = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

async function refreshOnce(): Promise<User | null> {
  let response = await postAuth('/api/auth/refresh');
  if (response.status === 409) {
    // another tab rotated the refresh cookie a moment ago; the browser now has the new one
    await delay(CONCURRENT_REFRESH_RETRY_MS);
    response = await postAuth('/api/auth/refresh');
  }
  if (response.status === 204 || response.status === 401 || response.status === 409) {
    forget();
    return null;
  }
  return remember(await requireJson<AuthResponse>(response));
}

/** Trades the refresh cookie for a new access token. Concurrent callers share one request. */
export function refresh(): Promise<User | null> {
  refreshPromise ??= refreshOnce().finally(() => {
    refreshPromise = null;
  });
  return refreshPromise;
}

/** The logged-in user or null; the first call per page load performs the refresh. */
export function session(): Promise<User | null> {
  sessionPromise ??= refresh().catch(() => null);
  return sessionPromise;
}

export async function login(email: string, password: string): Promise<User> {
  return remember(await requireJson<AuthResponse>(await postAuth('/api/auth/login', { email, password })));
}

export async function signup(request: SignupRequest): Promise<User> {
  return remember(await requireJson<AuthResponse>(await postAuth('/api/auth/signup', request)));
}

export async function logout(): Promise<void> {
  try {
    await postAuth('/api/auth/logout');
  } finally {
    forget();
  }
}

/** Use a token response from another endpoint (e.g. becoming a seller returns a new token). */
export function acceptAuthResponse(auth: AuthResponse): User {
  return remember(auth);
}

function withToken(init: RequestInit): RequestInit {
  const headers = new Headers(init.headers);
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  return { ...init, headers, credentials: 'same-origin' };
}

/** fetch with the bearer token: refreshes it shortly before expiry, and once more on a 401. */
export async function authFetch(input: string, init: RequestInit = {}): Promise<Response> {
  await session();
  if (accessToken && expiresAt - Date.now() < REFRESH_MARGIN_MS) {
    await refresh();
  }
  const response = await fetch(input, withToken(init));
  if (response.status !== 401 || !accessToken) {
    return response;
  }
  await refresh();
  return accessToken ? fetch(input, withToken(init)) : response;
}

/** Login methods besides email + password that the backend has configured, e.g. ["google"]. */
export async function providers(): Promise<string[]> {
  try {
    const response = await fetch('/api/auth/providers');
    const list: unknown = response.ok ? await response.json() : [];
    return Array.isArray(list) ? list.filter((item): item is string => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

/** Where to go after logging in: the ?next= path if it is a local path, else the home page. */
export function nextPath(): string {
  const next = new URLSearchParams(location.search).get('next');
  return next && next.startsWith('/') && !next.startsWith('//') && !next.startsWith('/\\') ? next : '/';
}

/** For protected pages: resolves with the user, or sends the browser to the login page. */
export async function requireUser(): Promise<User> {
  const user = await session();
  if (user) {
    return user;
  }
  location.replace(`/login?next=${encodeURIComponent(location.pathname + location.search)}`);
  return new Promise<User>(() => {
    // never resolves: the page is navigating away
  });
}
