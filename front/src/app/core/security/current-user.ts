import type { CurrentUserDto } from '@core/api/eurder-api';

export type Role = CurrentUserDto['role'];

export interface CurrentUser {
  id: string;
  role: Role;
  email: string;
  displayName: string;
}

export function toCurrentUser(dto: CurrentUserDto): CurrentUser {
  const fullName = [dto.firstName, dto.lastName].filter(Boolean).join(' ');
  return {
    id: dto.id,
    role: dto.role,
    email: dto.email,
    displayName: fullName || dto.email,
  };
}
