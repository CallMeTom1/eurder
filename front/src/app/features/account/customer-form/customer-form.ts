import {
  email,
  minLength,
  pattern,
  required,
  SchemaPath,
  SchemaPathTree,
} from '@angular/forms/signals';
import { atMost } from '@shared/forms/eurder-validators';
import type { UpdateCustomerDto } from '@core/api/eurder-api';

export type CustomerDetails = UpdateCustomerDto;

const PHONE_NUMBER = /^\+?[0-9 ./-]{6,20}$/;

function requiredText(path: SchemaPath<string>, length: number): void {
  required(path);
  atMost(path, length);
}

export function emptyCustomer(): CustomerDetails {
  return {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    address: { street: '', houseNumber: '', postalCode: '', city: '', country: 'BELGIUM' },
  };
}

export function customerRules(path: SchemaPathTree<CustomerDetails>): void {
  requiredText(path.firstName, 255);
  requiredText(path.lastName, 255);
  requiredText(path.email, 255);
  email(path.email);
  required(path.phoneNumber);
  pattern(path.phoneNumber, PHONE_NUMBER);
  requiredText(path.address.street, 255);
  requiredText(path.address.houseNumber, 20);
  requiredText(path.address.postalCode, 20);
  requiredText(path.address.city, 255);
  required(path.address.country);
}

export function passwordRules(path: SchemaPath<string>): void {
  required(path);
  minLength(path, 8);
  atMost(path, 72);
}
