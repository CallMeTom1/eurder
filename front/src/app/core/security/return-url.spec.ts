import { safeReturnUrl } from './return-url';

describe('safeReturnUrl', () => {
  it.each([
    ['/admin/customers', '/admin/customers'],
    ['/my-orders?page=2', '/my-orders?page=2'],
  ])('given the address %s of the app, when checking it, then it is kept', (url, expected) => {
    const safe = safeReturnUrl(url);

    expect(safe).toBe(expected);
  });

  it.each([
    ['another site', 'https://evil.example/login'],
    ['a protocol-relative address', '//evil.example'],
    ['a backslash trick', '/\\evil.example'],
    ['a javascript address', 'javascript:alert(1)'],
    ['no address', undefined],
    ['an empty address', ''],
  ])('given %s, when checking it, then it is refused', (_, url) => {
    const safe = safeReturnUrl(url);

    expect(safe).toBeNull();
  });
});
