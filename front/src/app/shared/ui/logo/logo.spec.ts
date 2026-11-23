import { TestBed } from '@angular/core/testing';
import { Logo } from './logo';

describe('Logo', () => {
  it('given the logo, when rendered, then it shows the Örder logo of the README with its name for screen readers', async () => {
    await TestBed.configureTestingModule({ imports: [Logo] }).compileComponents();
    const fixture = TestBed.createComponent(Logo);

    await fixture.whenStable();

    const image = (fixture.nativeElement as HTMLElement).querySelector('img');
    expect(image?.getAttribute('src')).toBe('/brand/order.png');
    expect(image?.getAttribute('alt')).toBe('Örder');
  });
});
