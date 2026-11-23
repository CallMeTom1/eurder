import { Component, computed, inject, input, signal } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';
import { MatError, MatFormField, MatHint, MatPrefix } from '@angular/material/form-field';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import { imageUrlOf } from '@core/api/api-routes';
import { firstValueFrom } from 'rxjs';
import { AdminItemService } from '../admin-item.service';
import { TranslatePipe } from '@ngx-translate/core';
import { FieldControl } from '@shared/forms/form-field/field-control';
import { FieldErrors } from '@shared/forms/form-field/field-errors';
import { FormFieldComponent } from '@shared/forms/form-field/form-field';
import { CharCounter } from '@shared/ui/char-counter/char-counter';
import { ItemForm } from '../item-form';

const MAX_PICTURE_SIZE = 2 * 1024 * 1024;

@Component({
  imports: [
    FieldControl,
    FieldErrors,
    FormFieldComponent,
    CharCounter,
    FormField,
    MatError,
    MatFormField,
    MatHint,
    MatButton,
    MatIcon,
    MatInput,
    MatPrefix,
    TranslatePipe,
  ],
  selector: 'app-item-form-fields',
  templateUrl: './item-form-fields.html',
  host: { class: 'grid gap-x-4 gap-y-3 sm:grid-cols-[12rem_14rem_1fr]' },
})
export class ItemFormFields {
  readonly form = input.required<FieldTree<ItemForm>>();

  private readonly adminItems = inject(AdminItemService);

  protected readonly uploading = signal(false);
  protected readonly pictureProblem = signal<Pick<ApiProblem, 'code' | 'params'> | null>(null);
  protected readonly pictureUrl = computed(() => imageUrlOf(this.form().image().value()));

  protected async choose(input: HTMLInputElement): Promise<void> {
    const picture = input.files?.[0];
    input.value = '';
    if (!picture) {
      return;
    }
    this.pictureProblem.set(null);
    if (picture.size > MAX_PICTURE_SIZE) {
      this.pictureProblem.set({ code: 'error.common.file-too-large', params: {} });
      return;
    }
    this.uploading.set(true);
    try {
      const path = await firstValueFrom(this.adminItems.uploadImage(picture));
      this.form().image().value.set(path);
    } catch (error) {
      if (isApiProblem(error) && !isGlobalProblem(error)) {
        this.pictureProblem.set(error);
      }
    } finally {
      this.uploading.set(false);
    }
  }

  protected remove(): void {
    this.pictureProblem.set(null);
    this.form().image().value.set(null);
  }
}
