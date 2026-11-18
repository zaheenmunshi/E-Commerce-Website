import { initAuthPage } from '../components/auth-page.js';
import { login, nextPath } from '../lib/auth.js';
import { inputValue, onEnter, qs } from '../lib/dom.js';
import { showAlert, withLoader } from '../lib/ui.js';

initAuthPage();

async function submit(): Promise<void> {
  const email = inputValue('#email');
  const password = qs<HTMLInputElement>('#password').value;
  if (!email || !password) {
    showAlert('fill all the inputs');
    return;
  }
  if (await withLoader(() => login(email, password))) {
    location.replace(nextPath());
  }
}

qs('.submit-btn').addEventListener('click', () => void submit());
onEnter(qs('.container'), () => void submit());
