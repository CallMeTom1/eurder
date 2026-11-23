import { HttpErrorResponse } from '@angular/common/http';
import { isApiProblem, problemOf, toApiProblem } from './api-problem';

describe('toApiProblem', () => {
  it('given a ProblemDetail of the API, when converting, then keeps its status, code and params', () => {
    const response = new HttpErrorResponse({
      status: 404,
      error: {
        status: 404,
        title: 'Not Found',
        detail: 'No item found for id 3f2a',
        instance: '/api/catalog/3f2a',
        code: 'error.item.not-found',
        params: { id: '3f2a' },
      },
    });

    const problem = toApiProblem(response);

    expect(problem).toEqual({
      status: 404,
      code: 'error.item.not-found',
      params: { id: '3f2a' },
      errors: [],
    });
  });

  it('given a validation error, when converting, then keeps the error of each field', () => {
    const response = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        code: 'error.common.validation-failed',
        params: {},
        errors: [{ field: 'name', code: 'validation.required', params: {} }],
      },
    });

    const problem = toApiProblem(response);

    expect(problem.errors).toEqual([{ field: 'name', code: 'validation.required', params: {} }]);
  });

  it.each([
    { situation: 'the network is down', status: 0, body: null },
    {
      situation: 'a proxy answers with an HTML page',
      status: 502,
      body: '<html>Bad Gateway</html>',
    },
  ])(
    'given $situation, when converting, then the problem is unexpected and keeps the status',
    ({ status, body }) => {
      const response = new HttpErrorResponse({ status, error: body });

      const problem = toApiProblem(response);

      expect(problem).toEqual({ status, code: 'error.common.unexpected', params: {}, errors: [] });
    },
  );
});

describe('isApiProblem', () => {
  it('given a converted problem or anything else, when checking, then only the problem is recognised', () => {
    const problem = toApiProblem(new HttpErrorResponse({ status: 0 }));

    const answers = [isApiProblem(problem), isApiProblem(new Error('boom')), isApiProblem(null)];

    expect(answers).toEqual([true, false, false]);
  });
});

describe('problemOf', () => {
  const notFound = toApiProblem(
    new HttpErrorResponse({
      status: 404,
      error: { status: 404, code: 'error.item.not-found', params: { id: 'i1' } },
    }),
  );

  it.each([
    { situation: 'the problem itself', error: notFound },
    {
      situation: 'a problem wrapped by a resource',
      error: new Error('Resource failed', { cause: notFound }),
    },
  ])('given $situation, when reading it, then it is the problem of the API', ({ error }) => {
    const problem = problemOf(error);

    expect(problem).toBe(notFound);
  });

  it('given anything else, when reading it, then the problem is unexpected', () => {
    const problem = problemOf(new TypeError('boom'));

    expect(problem).toEqual({ status: 0, code: 'error.common.unexpected', params: {}, errors: [] });
  });
});
