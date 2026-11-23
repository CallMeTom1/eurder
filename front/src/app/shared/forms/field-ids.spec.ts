import { uniquePrefix } from './field-ids';

describe('uniquePrefix', () => {
  it('given two forms on a page, when each asks for a prefix, then their field ids cannot collide', () => {
    const first = uniquePrefix();

    const second = uniquePrefix();

    expect(first).not.toBe(second);
    expect(second).toMatch(/^form-\d+-$/);
  });
});
