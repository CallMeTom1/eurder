import { booleanAttribute, Component, inject, input, signal } from '@angular/core';
import {
  email,
  form,
  FormField,
  FormRoot,
  required,
  TreeValidationResult,
} from '@angular/forms/signals';
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
import { apiErrorsOf } from '@shared/forms/api-errors';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';

@Component({
  imports: [
    ErrorState,
    FieldErrorPipe,
    FormField,
    FormRoot,
    MatButton,
    MatError,
    MatFormField,
    MatInput,
    MatLabel,
    PageHeader,
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

  protected readonly credentials = signal({ email: '', password: '' });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly loginForm = form(
    this.credentials,
    (path) => {
      required(path.email);
      email(path.email);
      required(path.password);
    },
    { submission: { action: () => this.logIn() } },
  );

  private async logIn(): Promise<TreeValidationResult> {
    this.problem.set(null);
    const { email, password } = this.credentials();
    try {
      const user = await firstValueFrom(this.security.login(email, password));
      await this.router.navigateByUrl(this.landingOf(user));
      return undefined;
    } catch (error) {
      return this.errorsOf(error);
    }
  }

  private landingOf(user: CurrentUser): string {
    return safeReturnUrl(this.returnUrl()) ?? (user.role === 'ADMIN' ? '/admin' : '/catalog');
  }

  private errorsOf(error: unknown): TreeValidationResult {
    if (!isApiProblem(error) || isGlobalProblem(error)) {
      return undefined;
    }
    const { onFields, unmatched } = apiErrorsOf(this.loginForm, error);
    if (error.errors.length === 0 || unmatched.length > 0) {
      this.problem.set(error);
    }
    return onFields;
  }
}
