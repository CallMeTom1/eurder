import { WritableSignal } from '@angular/core';
import { FieldTree, ReadonlyFieldTree, ValidationError } from '@angular/forms/signals';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiFieldError, ApiProblem, isApiProblem } from '@core/api/api-problem';

export interface ApiValidationError extends ValidationError.WithOptionalFieldTree {
  readonly kind: 'api';
  readonly code: string;
  readonly params: Record<string, unknown>;
}

export interface ApiErrors {
  onFields: ApiValidationError[];
  unmatched: ApiFieldError[];
}

export function apiErrorsOf(form: FieldTree<unknown>, problem: ApiProblem): ApiErrors {
  const onFields: ApiValidationError[] = [];
  const unmatched: ApiFieldError[] = [];
  for (const error of problem.errors) {
    const fieldTree = fieldAt(form, error.field);
    if (fieldTree) {
      onFields.push({ kind: 'api', code: error.code, params: error.params, fieldTree });
    } else {
      unmatched.push(error);
    }
  }
  return { onFields, unmatched };
}

export function submissionErrorsOf(
  form: FieldTree<unknown>,
  error: unknown,
  shown: WritableSignal<ApiProblem | null>,
): ApiValidationError[] | undefined {
  if (!isApiProblem(error) || isGlobalProblem(error)) {
    return undefined;
  }
  const { onFields, unmatched } = apiErrorsOf(form, error);
  if (error.errors.length === 0 || unmatched.length > 0) {
    shown.set(error);
  }
  return onFields;
}

export function apiErrorOn(
  fieldTree: ReadonlyFieldTree<unknown>,
  problem: ApiProblem,
): ApiValidationError {
  return { kind: 'api', code: problem.code, params: problem.params, fieldTree };
}

function fieldAt(form: FieldTree<unknown>, path: string): ReadonlyFieldTree<unknown> | undefined {
  let node: unknown = form;
  for (const segment of path.replace(/\[(\d+)\]/g, '.$1').split('.')) {
    node =
      typeof node === 'function'
        ? (node as unknown as Record<string, unknown>)[segment]
        : undefined;
  }
  return typeof node === 'function' ? (node as ReadonlyFieldTree<unknown>) : undefined;
}
