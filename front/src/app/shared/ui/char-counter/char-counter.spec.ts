import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';
import { atMost } from '@shared/forms/eurder-validators';
import { provideTestTranslations } from '@core/i18n/testing';
import { CharCounter } from './char-counter';

@Component({
  imports: [CharCounter],
  template: `<app-char-counter [field]="item.description" [max]="255" />`,
})
class HostForm {
  readonly model = signal({ description: '' });
  readonly item = form(this.model, (path) => atMost(path.description, 255));
}

describe('CharCounter', () => {
  let fixture: ComponentFixture<HostForm>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HostForm],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(HostForm);
    await fixture.whenStable();
  });

  async function withDescription(text: string): Promise<HTMLElement> {
    fixture.componentInstance.model.set({ description: text });
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).querySelector('app-char-counter span')!;
  }

  it('given an empty field, when reading the counter, then all the characters of the rule are left', async () => {
    const counter = await withDescription('');

    expect(counter.textContent?.trim()).toBe('255 characters left');
  });

  it('given 10 characters, when reading the counter, then 245 are left', async () => {
    const counter = await withDescription('x'.repeat(10));

    expect(counter.textContent?.trim()).toBe('245 characters left');
  });

  it('given exactly the limit, when reading the counter, then none is left and it is not red', async () => {
    const counter = await withDescription('x'.repeat(255));

    expect(counter.textContent?.trim()).toBe('0 characters left');
    expect(counter.classList).not.toContain('text-error');
  });

  it('given more than the limit, when reading the counter, then it stays at 0 in red and the field is invalid', async () => {
    const counter = await withDescription('x'.repeat(256));

    expect(counter.textContent?.trim()).toBe('0 characters left');
    expect(counter.classList).toContain('text-error');
    expect(fixture.componentInstance.item.description().invalid()).toBe(true);
  });
});
