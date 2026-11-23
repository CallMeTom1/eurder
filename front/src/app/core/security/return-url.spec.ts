import { safeReturnUrl } from './return-url';

describe('safeReturnUrl', () => {
  it.each([
    { url: '/admin/customers', expected: '/admin/customers' },
    { url: '/my-orders?page=2', expected: '/my-orders?page=2' },
  ])(
    'given the address $url of the app, when checking it, then it is kept',
    ({ url, expected }) => {
      const safe = safeReturnUrl(url);

      expect(safe).toBe(expected);
    },
  );

  it.each([
    { situation: 'another site', url: 'https://evil.example/login' },
    { situation: 'a protocol-relative address', url: '//evil.example' },
    { situation: 'a backslash trick', url: '/\\evil.example' },
    { situation: 'a javascript address', url: 'javascript:alert(1)' },
    { situation: 'no address', url: undefined },
    { situation: 'an empty address', url: '' },
  ])('given $situation, when checking it, then it is refused', ({ url }) => {
    const safe = safeReturnUrl(url);

    expect(safe).toBeNull();
  });
});
