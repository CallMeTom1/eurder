import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { ApiProblem } from '@core/api/api-problem';
import { applyApiErrors } from './api-errors';

describe('applyApiErrors', () => {
  function orderForm() {
    return new FormGroup({
      name: new FormControl('', Validators.maxLength(100)),
      address: new FormGroup({ city: new FormControl('') }),
      itemGroups: new FormArray([new FormGroup({ amount: new FormControl(0) })]),
    });
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

  it('given a validation problem, when applying it, then each error lands on its field, nested ones included', () => {
    const form = orderForm();

    applyApiErrors(form, problem);

    expect(form.get('name')?.errors).toEqual({ api: { code: 'validation.required', params: {} } });
    expect(form.get('address.city')?.errors).toEqual({
      api: { code: 'validation.too-long', params: { max: 50 } },
    });
    expect(form.get('itemGroups.0.amount')?.errors).toEqual({
      api: { code: 'validation.must-be-positive', params: {} },
    });
    expect(form.get('address.city')?.touched).toBe(true);
  });

  it('given a field that already has an error of its own, when applying, then the error of the API is shown first', () => {
    const form = new FormGroup({ name: new FormControl('', Validators.required) });

    applyApiErrors(form, problem);

    expect(Object.keys(form.get('name')?.errors ?? {})).toEqual(['api', 'required']);
  });

  it('given an error on a field the form does not have, when applying, then it is returned to be shown elsewhere', () => {
    const form = orderForm();

    const unmatched = applyApiErrors(form, problem);

    expect(unmatched).toEqual([
      { field: 'password', code: 'validation.too-short', params: { min: 8 } },
    ]);
  });

  it('given an error of the API on a field, when the user changes the field, then the error goes away', () => {
    const form = orderForm();
    applyApiErrors(form, problem);

    form.get('name')?.setValue('Chair');

    expect(form.get('name')?.errors).toBeNull();
  });
});
