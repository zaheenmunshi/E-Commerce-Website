// Small DOM helpers: typed lookups that fail loudly, and HTML escaping for template strings.

export function qs<T extends Element = HTMLElement>(selector: string, root: ParentNode = document): T {
  const element = root.querySelector<T>(selector);
  if (!element) {
    throw new Error(`missing element: ${selector}`);
  }
  return element;
}

export function qsa<T extends Element = HTMLElement>(selector: string, root: ParentNode = document): T[] {
  return [...root.querySelectorAll<T>(selector)];
}

export function inputValue(selector: string): string {
  return qs<HTMLInputElement | HTMLTextAreaElement>(selector).value.trim();
}

export function isChecked(selector: string): boolean {
  return qs<HTMLInputElement>(selector).checked;
}

const HTML_ESCAPES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

/** Escape every value interpolated into innerHTML. */
export function escapeHtml(value: unknown): string {
  return String(value ?? '').replace(/[&<>"']/g, (char) => HTML_ESCAPES[char] ?? char);
}

/** Only same-origin absolute paths (our uploads and bundled images) are ever used as image sources. */
export function safeImageUrl(url: string | null | undefined): string {
  return url && url.startsWith('/') && !url.startsWith('//') ? url : '/img/no-image.png';
}

export function onEnter(root: ParentNode, action: () => void): void {
  root.addEventListener('keydown', (event) => {
    if (event instanceof KeyboardEvent && event.key === 'Enter' && !(event.target instanceof HTMLTextAreaElement)) {
      action();
    }
  });
}
