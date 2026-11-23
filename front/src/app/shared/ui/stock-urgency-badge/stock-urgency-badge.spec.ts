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
    ['STOCK_HIGH', 'High stock', 'success'],
    ['STOCK_MEDIUM', 'Medium stock', 'warning'],
    ['STOCK_LOW', 'Low stock', 'error'],
  ] as const)(
    'given %s, when rendered, then it says %s in the %s tone',
    async (urgency, text, tone) => {
      const badge = await badgeFor(urgency);

      const label = badge.querySelector('[data-tone]');

      expect(label?.textContent?.trim()).toBe(text);
      expect(label?.getAttribute('data-tone')).toBe(tone);
    },
  );
});
