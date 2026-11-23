import { booleanAttribute, Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import { CurrentUser } from '@core/security/current-user';
import { safeReturnUrl } from '@core/security/return-url';
import { SecurityService } from '@core/security/security.service';
import { TranslatePipe } from '@ngx-translate/core';
import { applyApiErrors } from '@shared/forms/api-errors';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { finalize } from 'rxjs';

@Component({
  imports: [
    ErrorState,
    FieldErrorPipe,
    MatButton,
    MatError,
    MatFormField,
    MatInput,
    MatLabel,
    PageHeader,
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-login',
  templateUrl: './login.html',
})
export class Login {
  readonly returnUrl = input<string>();
  readonly registered = input(false, { transform: booleanAttribute });

  private readonly security = inject(SecurityService);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });
  protected readonly submitting = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.problem.set(null);
    this.security
      .login(email, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (user) => this.router.navigateByUrl(this.landingOf(user)),
        error: (error: unknown) => this.show(error),
      });
  }

  private landingOf(user: CurrentUser): string {
    return safeReturnUrl(this.returnUrl()) ?? (user.role === 'ADMIN' ? '/admin' : '/catalog');
  }

  private show(error: unknown): void {
    if (!isApiProblem(error) || isGlobalProblem(error)) {
      return;
    }
    const unmatched = applyApiErrors(this.form, error);
    if (error.errors.length === 0 || unmatched.length > 0) {
      this.problem.set(error);
    }
  }
}
