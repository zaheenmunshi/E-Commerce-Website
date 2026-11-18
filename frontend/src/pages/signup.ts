import { initAuthPage } from '../components/auth-page.js';
import { nextPath, signup } from '../lib/auth.js';
import { inputValue, isChecked, onEnter, qs } from '../lib/dom.js';
import type { SignupRequest } from '../lib/types.js';
import { showAlert, withLoader } from '../lib/ui.js';

initAuthPage();

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Same rules as the server (SignupRequest), checked first for instant feedback. */
function validate(request: SignupRequest): string | null {
  if (request.name.length < 3) return 'name must be 3 letters long';
  if (!request.email) return 'enter your email';
  if (!EMAIL_PATTERN.test(request.email)) return 'enter a valid email';
  if (request.password.length < 8) return 'password should be 8 letters long';
  if (!request.phone) return 'enter your phone number';
  if (!/^\d{10,15}$/.test(request.phone)) return 'invalid number, please enter valid one';
  if (!request.termsAccepted) return 'you must agree to our terms and conditions';
  return null;
}

async function submit(): Promise<void> {
  const request: SignupRequest = {
    name: inputValue('#name'),
    email: inputValue('#email'),
    password: qs<HTMLInputElement>('#password').value,
    phone: inputValue('#number'),
    termsAccepted: isChecked('#terms-and-cond'),
    marketingOptIn: isChecked('#notification'),
  };
  const problem = validate(request);
  if (problem) {
    showAlert(problem);
    return;
  }
  if (await withLoader(() => signup(request))) {
    location.replace(nextPath());
  }
}

qs('.submit-btn').addEventListener('click', () => void submit());
onEnter(qs('.container'), () => void submit());
