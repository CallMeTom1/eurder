import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { StockUrgency, StockUrgencyBadge } from './stock-urgency-badge';

describe('StockUrgencyBadge', () => {
  async function badgeFor(urgency: StockUrgency): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [StockUrgencyBadge],
      providers: [provideTestTranslations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(StockUrgencyBadge);
    fixture.componentRef.setInput('urgency', urgency);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it.each([
    { urgency: 'STOCK_HIGH', text: 'High stock', tone: 'success' },
    { urgency: 'STOCK_MEDIUM', text: 'Medium stock', tone: 'warning' },
    { urgency: 'STOCK_LOW', text: 'Low stock', tone: 'error' },
  ] as const)(
    'given $urgency, when rendered, then it says $text in the $tone tone',
    async ({ urgency, text, tone }) => {
      const badge = await badgeFor(urgency);

      const label = badge.querySelector('[data-tone]');

      expect(label?.textContent?.trim()).toBe(text);
      expect(label?.getAttribute('data-tone')).toBe(tone);
    },
  );
});
