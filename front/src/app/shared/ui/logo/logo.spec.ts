import { TestBed } from '@angular/core/testing';
import { Logo } from './logo';

describe('Logo', () => {
  it('given the logo, when rendered, then it shows the name and hides its drawing from screen readers', async () => {
    const fixture = TestBed.createComponent(Logo);

    await fixture.whenStable();

    const logo = fixture.nativeElement as HTMLElement;
    expect(logo.textContent?.trim()).toBe('Eurder');
    expect(logo.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
  });
});
