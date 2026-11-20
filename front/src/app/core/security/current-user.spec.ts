import type { CurrentUserDto } from '@core/api/eurder-api';
import { toCurrentUser } from './current-user';

describe('toCurrentUser', () => {
  it('given a customer, when mapping, then the display name is their full name', () => {
    const dto: CurrentUserDto = {
      id: '3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90',
      role: 'CUSTOMER',
      email: 'jane@doe.be',
      firstName: 'Jane',
      lastName: 'Doe',
    };

    const user = toCurrentUser(dto);

    expect(user).toEqual({
      id: '3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90',
      role: 'CUSTOMER',
      email: 'jane@doe.be',
      displayName: 'Jane Doe',
    });
  });

  it('given an admin without a name, when mapping, then the display name is the email', () => {
    const dto: CurrentUserDto = {
      id: '9d92d67e-6ef3-450d-85ac-11180c8826d2',
      role: 'ADMIN',
      email: 'admin@eurder.be',
      firstName: null,
      lastName: null,
    };

    const user = toCurrentUser(dto);

    expect(user.displayName).toBe('admin@eurder.be');
    expect(user.role).toBe('ADMIN');
  });
});
