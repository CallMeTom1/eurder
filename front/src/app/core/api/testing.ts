import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EnvironmentProviders, Provider } from '@angular/core';
import { apiErrorInterceptor } from './api-error.interceptor';

export function provideApiTesting(): (Provider | EnvironmentProviders)[] {
  return [provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()];
}
