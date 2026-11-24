'use strict';
const $ = id => document.getElementById(id);
const money = value => value.toLocaleString('en-IN', { style: 'currency', currency: 'INR' });

// sessionStorage: survives a reload, gone when the tab closes.
let token = sessionStorage.getItem('token');
let accounts = [];
let selected = null;
let registering = false;

function say(text, bad = false) {
  $('message').textContent = text;
  $('message').className = bad ? 'bad' : '';
}

async function api(path, body, method = body ? 'POST' : 'GET') {
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = 'Bearer ' + token;
  let response;
  try {
    response = await fetch('/api' + path, { method, headers, body: body && JSON.stringify(body) });
  } catch {
    throw new Error('Cannot reach the bank. If you were moving money, check the history before retrying.');
  }
  const data = await response.json().catch(() => null);
  if (response.ok) return data;
  if (response.status === 401 && token) {
    logout();
    throw new Error('Your session has expired. Please log in again.');
  }
  throw new Error(data?.message || 'Request failed (' + response.status + '). Please try again in a moment.');
}

/** Runs an action with its button disabled, so a double click cannot move money twice. */
async function busy(button, action) {
  button.disabled = true;
  say('');
  try {
    await action();
  } catch (error) {
    say(error.message, true);
  } finally {
    button.disabled = false;
  }
}

function element(tag, text, className = '') {
  const node = document.createElement(tag);
  node.textContent = text;
  node.className = className;
  return node;
}

function showView() {
  $('auth').hidden = !!token;
  $('bank').hidden = $('who').hidden = !token;
}

function logout() {
  sessionStorage.removeItem('token');
  token = selected = null;
  accounts = [];
  $('username').textContent = '';
  renderAccounts();
  $('history').replaceChildren();
  $('move-form').reset();
  showMoveFields();
  showView();
}

async function refresh() {
  accounts = (await api('/accounts')).sort((a, b) => a.accountId - b.accountId);
  selected ??= accounts[0]?.accountNumber ?? null;
  renderAccounts();
  if (!selected) return;
  const number = selected;
  const transactions = await api('/transactions/account/' + number);
  if (number === selected) renderHistory(transactions);
}

function renderAccounts() {
  $('no-accounts').hidden = accounts.length > 0;
  $('selected').hidden = !selected;
  $('selected-number').textContent = selected ?? '';
  $('total').textContent = accounts.length ? 'Total ' + money(accounts.reduce((sum, a) => sum + a.balance, 0)) : '';
  $('accounts').replaceChildren(...accounts.map(account => {
    const card = element('button', account.accountNumber);
    card.append(element('b', money(account.balance)));
    card.setAttribute('aria-pressed', account.accountNumber === selected);
    card.onclick = () => {
      selected = account.accountNumber;
      $('history').replaceChildren();
      say('');
      refresh().catch(error => say(error.message, true));
    };
    return card;
  }));
  $('own-accounts').replaceChildren(...accounts.filter(a => a.accountNumber !== selected)
    .map(a => Object.assign(document.createElement('option'), { value: a.accountNumber })));
}

function renderHistory(transactions) {
  $('no-history').hidden = transactions.length > 0;
  $('history').replaceChildren(...transactions.map(tx => {
    const out = tx.sourceAccountNumber === selected;
    const details = tx.transactionType.typeName !== 'TRANSFER' ? (out ? 'Withdrawal' : 'Deposit')
      : out ? 'To ' + tx.destinationAccountNumber : 'From ' + tx.sourceAccountNumber;
    const status = element('td', tx.status.replace('_', ' ').toLowerCase(), tx.status === 'COMPLETED' ? '' : 'bad');
    if (tx.failureReason) status.append(element('div', tx.failureReason, 'muted'));
    const row = document.createElement('tr');
    row.append(
      // The containers run in UTC and send timestamps without a zone.
      element('td', new Date(tx.createdAt.slice(0, 23) + 'Z').toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })),
      element('td', details),
      status,
      element('td', (out ? '−' : '+') + money(tx.amount), 'num' + (tx.status === 'COMPLETED' && !out ? ' in' : '')));
    return row;
  }));
}

function showAuthMode() {
  const form = $('auth-form');
  $('auth-title').textContent = $('auth-submit').textContent = registering ? 'Create account' : 'Log in';
  $('auth-switch').textContent = registering ? 'Already registered? Log in' : 'New here? Create an account';
  $('email-field').hidden = form.elements.email.disabled = !registering;
  form.elements.password.minLength = registering ? 8 : 0;
  form.elements.password.autocomplete = registering ? 'new-password' : 'current-password';
}

function showMoveFields() {
  const { action, to } = $('move-form').elements;
  $('to-field').hidden = to.disabled = action.value !== 'transfer';
}

$('auth-switch').onclick = () => {
  registering = !registering;
  say('');
  showAuthMode();
};

$('auth-form').onsubmit = event => {
  event.preventDefault();
  busy($('auth-submit'), async () => {
    const { username, email, password } = Object.fromEntries(new FormData(event.target));
    if (registering) {
      await api('/auth/register', { username, email, password });
      registering = false;
      showAuthMode();
    }
    token = (await api('/auth/login', { username, password })).token;
    sessionStorage.setItem('token', token);
    event.target.reset();
    await start();
  });
};

$('logout').onclick = () => {
  logout();
  say('You are logged out.');
};

$('open-account').onclick = () => busy($('open-account'), async () => {
  selected = (await api('/accounts', null, 'POST')).accountNumber;
  $('history').replaceChildren();
  await refresh();
  say('Account ' + selected + ' is open.');
});

$('move-form').onchange = showMoveFields;

$('move-form').onsubmit = event => {
  event.preventDefault();
  const form = event.target;
  busy(form.querySelector('button'), async () => {
    const { action, to, amount } = Object.fromEntries(new FormData(form));
    const account = selected;
    const value = Number(amount);
    try {
      await api('/transactions/' + action, action === 'transfer'
        ? { sourceAccountNumber: account, destinationAccountNumber: to, amount: value }
        : { accountNumber: account, amount: value });
    } catch (error) {
      // Failed and unknown-outcome attempts belong in the history too.
      if (token) await refresh().catch(() => {});
      throw error;
    }
    form.elements.amount.value = '';
    say({ deposit: 'Deposited ', withdraw: 'Withdrew ', transfer: 'Transferred ' }[action] + money(value) + '.');
    await refresh();
  });
};

async function start() {
  showView();
  if (!token) return;
  $('username').textContent = (await api('/users/me')).username;
  await refresh();
}

showAuthMode();
showMoveFields();
start().catch(error => say(error.message, true));
