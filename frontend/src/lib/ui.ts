// The alert box and loader that several pages share (markup + css/alert.css).

const ALERT_DURATION_MS = 3000;
let alertTimer: number | undefined;

export function showAlert(message: string, type: 'error' | 'success' = 'error'): void {
  const box = document.querySelector<HTMLElement>('.alert-box');
  const text = document.querySelector<HTMLElement>('.alert-msg');
  const image = document.querySelector<HTMLImageElement>('.alert-img');
  if (!box || !text || !image) {
    window.alert(message);
    return;
  }
  text.textContent = message;
  image.src = type === 'success' ? '/img/success.png' : '/img/error.png';
  text.style.color = type === 'success' ? '#0ab50a' : '';
  box.classList.add('show');
  window.clearTimeout(alertTimer);
  alertTimer = window.setTimeout(() => box.classList.remove('show'), ALERT_DURATION_MS);
}

export function setLoading(loading: boolean): void {
  const loader = document.querySelector<HTMLElement>('.loader');
  if (loader) {
    loader.style.display = loading ? 'block' : '';
  }
}

/** Runs an async action with the loader shown, reporting failures in the alert box. */
export async function withLoader<T>(action: () => Promise<T>): Promise<T | undefined> {
  setLoading(true);
  try {
    return await action();
  } catch (error) {
    showAlert(errorMessage(error));
    return undefined;
  } finally {
    setLoading(false);
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof Error && error.message ? error.message : 'something went wrong, please try again';
}

export function formatPrice(amount: number | null | undefined): string {
  const value = amount ?? 0;
  return `$${Number.isInteger(value) ? value : value.toFixed(2)}`;
}
