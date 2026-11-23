import { Component, computed, input } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  imports: [TranslatePipe],
  selector: 'app-char-counter',
  template: `
    <span [class.text-error]="exceeded()">
      {{ 'common.charactersLeft' | translate: { count: remaining() } }}
    </span>
  `,
})
export class CharCounter {
  readonly field = input.required<FieldTree<string>>();

  private readonly length = computed(() => this.field()().value().length);
  private readonly max = computed(() => this.field()().maxLength?.() ?? Infinity);

  protected readonly remaining = computed(() => Math.max(0, this.max() - this.length()));
  protected readonly exceeded = computed(() => this.length() > this.max());
}
