import { HttpErrorResponse } from '@angular/common/http';
import type { ProblemDetail } from './eurder-api';

export type ErrorCode = ProblemDetail['code'];
export type ApiFieldError = NonNullable<ProblemDetail['errors']>[number];

export interface ApiProblem {
  status: number;
  code: ErrorCode;
  params: Record<string, unknown>;
  errors: ApiFieldError[];
}

const UNEXPECTED: ErrorCode = 'error.common.unexpected';

export function toApiProblem(response: HttpErrorResponse): ApiProblem {
  const body: unknown = response.error;
  if (!isProblemDetail(body)) {
    return { status: response.status, code: UNEXPECTED, params: {}, errors: [] };
  }
  return {
    status: response.status,
    code: body.code,
    params: body.params ?? {},
    errors: body.errors ?? [],
  };
}

export function isApiProblem(value: unknown): value is ApiProblem {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ApiProblem).status === 'number' &&
    typeof (value as ApiProblem).code === 'string' &&
    Array.isArray((value as ApiProblem).errors)
  );
}

export function problemOf(error: unknown): ApiProblem {
  if (isApiProblem(error)) {
    return error;
  }
  const cause = error instanceof Error ? error.cause : undefined;
  return isApiProblem(cause) ? cause : { status: 0, code: UNEXPECTED, params: {}, errors: [] };
}

function isProblemDetail(body: unknown): body is ProblemDetail {
  return (
    typeof body === 'object' && body !== null && typeof (body as ProblemDetail).code === 'string'
  );
}
