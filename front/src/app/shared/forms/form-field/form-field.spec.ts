import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { form, FormField, required } from '@angular/forms/signals';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatError, MatFormField } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { provideTestTranslations } from '@core/i18n/testing';
import { FieldErrors } from './field-errors';
import { FieldControl } from './field-control';
import { FormFieldComponent } from './form-field';

@Component({
  imports: [
    FieldControl,
    FieldErrors,
    FormField,
    FormFieldComponent,
    MatError,
    MatFormField,
    MatInput,
    MatOption,
    MatSelect,
  ],
  template: `
    <app-form-field label="Email" [field]="person.email">
      <mat-form-field>
        <input matInput appFieldControl [formField]="person.email" />
        <mat-error><app-field-errors [field]="person.email" /></mat-error>
      </mat-form-field>
    </app-form-field>
    <app-form-field label="Nickname" [field]="person.nickname">
      <mat-form-field>
        <input matInput appFieldControl [formField]="person.nickname" />
      </mat-form-field>
    </app-form-field>
    <app-form-field label="Country" [field]="person.country">
      <mat-form-field>
        <mat-select appFieldControl [formField]="person.country">
          <mat-option value="BELGIUM">Belgium</mat-option>
        </mat-select>
      </mat-form-field>
    </app-form-field>
    <app-form-field label="Picture" [field]="person.picture">
      <input type="file" appFieldControl />
    </app-form-field>
  `,
})
class HostForm {
  readonly model = signal({ email: '', nickname: '', country: 'BELGIUM', picture: '' });
  readonly person = form(this.model, (path) => {
    required(path.email);
    required(path.country);
  });
}

describe('FormFieldComponent', () => {
  let fixture: ComponentFixture<HostForm>;
  let page: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HostForm],
      providers: [
        provideTestTranslations(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(HostForm);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function labelOf(text: string): HTMLLabelElement {
    return [...page.querySelectorAll('label')].find((label) => label.textContent?.trim() === text)!;
  }

  it('given a field, when rendered, then its label is above it and linked to it', () => {
    const label = labelOf('Email');

    const input = page.querySelector<HTMLInputElement>(`#${label.htmlFor}`);

    expect(label.htmlFor).not.toBe('');
    expect(input?.tagName).toBe('INPUT');
    expect(label.nextElementSibling?.contains(input!)).toBe(true);
  });

  it('given two fields, when rendered, then each one has its own id', () => {
    const ids = ['Email', 'Nickname', 'Picture'].map((text) => labelOf(text).htmlFor);

    const unique = new Set(ids);

    expect(unique.size).toBe(3);
  });

  it('given the schema, when rendered, then only the required fields show a star', () => {
    const email = labelOf('Email');

    const nickname = labelOf('Nickname');

    expect(email.classList).toContain('required');
    expect(nickname.classList).not.toContain('required');
  });

  it('given a select, which is not a native field, when rendered, then it is labelled by the label', () => {
    const label = labelOf('Country');

    const select = page.querySelector('mat-select');

    expect(label.id).not.toBe('');
    expect(select?.getAttribute('aria-labelledby')).toContain(label.id);
  });

  it('given a plain input, when rendered, then it also gets the id of its label', () => {
    const label = labelOf('Picture');

    const file = page.querySelector<HTMLInputElement>('input[type="file"]');

    expect(file?.id).toBe(label.htmlFor);
  });
});

describe('FieldErrors', () => {
  it('given an invalid touched field, when rendered, then its first error is shown translated', async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HostForm],
      providers: [
        provideTestTranslations(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(HostForm);
    await fixture.whenStable();

    fixture.componentInstance.person.email().markAsTouched();
    await fixture.whenStable();

    const error = (fixture.nativeElement as HTMLElement).querySelector('app-field-errors');
    expect(error?.textContent?.trim()).toBe('This field is required.');
  });
});
