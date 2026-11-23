import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';
import { ApiProblem } from '@core/api/api-problem';
import { apiErrorsOf, submissionErrorsOf } from './api-errors';

describe('apiErrorsOf', () => {
  function orderForm() {
    return TestBed.runInInjectionContext(() =>
      form(
        signal({
          name: '',
          address: { city: '' },
          itemGroups: [{ amount: 0 }],
        }),
      ),
    );
  }

  const problem: ApiProblem = {
    status: 400,
    code: 'error.common.validation-failed',
    params: {},
    errors: [
      { field: 'name', code: 'validation.required', params: {} },
      { field: 'address.city', code: 'validation.too-long', params: { max: 50 } },
      { field: 'itemGroups[0].amount', code: 'validation.must-be-positive', params: {} },
      { field: 'password', code: 'validation.too-short', params: { min: 8 } },
    ],
  };

  it('given a validation problem, when reading it for a form, then each error points at its field, nested ones included', () => {
    const order = orderForm();

    const { onFields } = apiErrorsOf(order, problem);

    expect(onFields).toEqual([
      { kind: 'api', code: 'validation.required', params: {}, fieldTree: order.name },
      {
        kind: 'api',
        code: 'validation.too-long',
        params: { max: 50 },
        fieldTree: order.address.city,
      },
      {
        kind: 'api',
        code: 'validation.must-be-positive',
        params: {},
        fieldTree: order.itemGroups[0].amount,
      },
    ]);
  });

  it('given an error on a field the form does not have, when reading it, then it is left to show elsewhere', () => {
    const order = orderForm();

    const { unmatched } = apiErrorsOf(order, problem);

    expect(unmatched).toEqual([
      { field: 'password', code: 'validation.too-short', params: { min: 8 } },
    ]);
  });
});

describe('submissionErrorsOf', () => {
  function nameForm() {
    return TestBed.runInInjectionContext(() => form(signal({ name: '' })));
  }

  function problemWith(status: number, errors: ApiProblem['errors']): ApiProblem {
    return { status, code: 'error.common.unexpected', params: {}, errors };
  }

  it.each([
    { what: 'an error that is not from the API', error: new Error('boom') },
    { what: 'a server failure, already shown by the interceptor', error: problemWith(500, []) },
  ])('given $what, when submitting, then nothing more is shown', ({ error }) => {
    const shown = signal<ApiProblem | null>(null);

    const errors = submissionErrorsOf(nameForm(), error, shown);

    expect(errors).toBeUndefined();
    expect(shown()).toBeNull();
  });

  it('given field errors the form knows, when submitting, then they go on their fields only', () => {
    const shown = signal<ApiProblem | null>(null);
    const person = nameForm();

    const errors = submissionErrorsOf(
      person,
      problemWith(400, [{ field: 'name', code: 'validation.required', params: {} }]),
      shown,
    );

    expect(errors).toEqual([
      { kind: 'api', code: 'validation.required', params: {}, fieldTree: person.name },
    ]);
    expect(shown()).toBeNull();
  });

  it.each([
    { what: 'a problem without field errors', problem: problemWith(404, []) },
    {
      what: 'an error on a field the form does not have',
      problem: problemWith(400, [{ field: 'password', code: 'validation.required', params: {} }]),
    },
  ])('given $what, when submitting, then the page shows the problem', ({ problem }) => {
    const shown = signal<ApiProblem | null>(null);

    submissionErrorsOf(nameForm(), problem, shown);

    expect(shown()).toEqual(problem);
  });
});
