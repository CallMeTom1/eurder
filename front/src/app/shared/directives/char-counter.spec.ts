import { Component, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CharCounter } from './char-counter';

@Component({
  imports: [ReactiveFormsModule, CharCounter],
  template: `<textarea [formControl]="description" [appCharCounter]="255"></textarea>`,
})
class HostForm {
  readonly description = new FormControl('', { nonNullable: true });
  readonly counter = viewChild.required(CharCounter);
}

describe('CharCounter', () => {
  let fixture: ComponentFixture<HostForm>;
  let form: HostForm;

  beforeEach(async () => {
    fixture = TestBed.createComponent(HostForm);
    form = fixture.componentInstance;
    await fixture.whenStable();
  });

  async function type(text: string): Promise<void> {
    const textarea = (fixture.nativeElement as HTMLElement).querySelector('textarea')!;
    textarea.value = text;
    textarea.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('given an empty field, when nothing is typed, then all 255 characters are left', () => {
    const remaining = form.counter().remaining();

    expect(remaining).toBe(255);
    expect(form.description.valid).toBe(true);
  });

  it('given the field, when typing 10 characters, then 245 are left', async () => {
    await type('x'.repeat(10));

    expect(form.counter().remaining()).toBe(245);
  });

  it('given the field, when typing exactly 255 characters, then none is left and the field stays valid', async () => {
    await type('x'.repeat(255));

    expect(form.counter().remaining()).toBe(0);
    expect(form.counter().exceeded()).toBe(false);
    expect(form.description.valid).toBe(true);
  });

  it('given the field, when typing 256 characters, then the counter stays at 0 and the field is too long', async () => {
    await type('x'.repeat(256));

    expect(form.counter().remaining()).toBe(0);
    expect(form.counter().exceeded()).toBe(true);
    expect(form.description.errors).toEqual({
      maxlength: { requiredLength: 255, actualLength: 256 },
    });
  });

  it('given an edit form, when the description is filled by code, then the counter follows', async () => {
    form.description.setValue('x'.repeat(200));
    await fixture.whenStable();

    expect(form.counter().remaining()).toBe(55);
  });
});
