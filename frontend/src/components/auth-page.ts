// Shared behaviour of the login and signup pages.
import { nextPath, providers, session } from '../lib/auth.js';
import { showAlert } from '../lib/ui.js';

const OAUTH_ERRORS: Record<string, string> = {
  oauth: 'google sign-in failed, please try again',
  unverified: 'your google email is not verified',
};

/** Already logged in? Go straight on. */
export async function redirectIfLoggedIn(): Promise<void> {
  if (await session()) {
    location.replace(nextPath());
  }
}

/** Shows "continue with google" only when the backend has Google configured. */
export async function showOAuthProviders(): Promise<void> {
  const google = document.querySelector<HTMLAnchorElement>('.oauth-link[data-provider="google"]');
  if (google && (await providers()).includes('google')) {
    google.hidden = false;
  }
}

export function showOAuthError(): void {
  const error = new URLSearchParams(location.search).get('error');
  if (error) {
    showAlert(OAUTH_ERRORS[error] ?? OAUTH_ERRORS['oauth'] ?? 'sign-in failed');
  }
}

export function initAuthPage(): void {
  void redirectIfLoggedIn();
  void showOAuthProviders();
  showOAuthError();
}
