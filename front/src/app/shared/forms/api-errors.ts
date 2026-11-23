import { AbstractControl } from '@angular/forms';
import { ApiFieldError, ApiProblem } from '@core/api/api-problem';

export function applyApiErrors(form: AbstractControl, problem: ApiProblem): ApiFieldError[] {
  return problem.errors.filter((error) => {
    const control = form.get(controlPath(error.field));
    if (!control) {
      return true;
    }
    control.setErrors({ api: { code: error.code, params: error.params }, ...control.errors });
    control.markAsTouched();
    return false;
  });
}

function controlPath(field: string): string {
  return field.replace(/\[(\d+)\]/g, '.$1');
}
