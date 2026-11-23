import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '@core/i18n/testing';
import { BasketService } from '../basket.service';
import { BasketPage } from './basket-page';

describe('BasketPage', () => {
  let fixture: ComponentFixture<BasketPage>;
  let page: HTMLElement;
  let basket: BasketService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [BasketPage],
      providers: [provideTestTranslations(), provideRouter([])],
    }).compileComponents();
    basket = TestBed.inject(BasketService);
  });

  async function open(): Promise<void> {
    fixture = TestBed.createComponent(BasketPage);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function fill(): void {
    basket.add({ id: 'i1', name: 'Chair', priceInCents: 4999 });
    basket.add({ id: 'i2', name: 'Lamp', priceInCents: 2900 });
    basket.changeAmount('i2', 3);
  }

  function rows(): string[][] {
    return [...page.querySelectorAll('tbody tr')].map((row) =>
      [...row.querySelectorAll('th, td')]
        .slice(0, 3)
        .map((cell) => cell.querySelector('input')?.value ?? cell.textContent!.trim()),
    );
  }

  function total(): string | undefined {
    return page.querySelector('[data-testid="basket-total"]')?.textContent?.trim();
  }

  function amountOf(name: string): HTMLInputElement {
    return page.querySelector<HTMLInputElement>(`input[aria-label="Amount of ${name}"]`)!;
  }

  async function changeAmount(name: string, value: string): Promise<void> {
    const input = amountOf(name);
    input.value = value;
    input.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  it('given an empty basket, when opening it, then it says so and leads to the catalog', async () => {
    await open();

    const link = page.querySelector('a[href="/catalog"]');

    expect(page.textContent).toContain('Your basket is empty.');
    expect(link?.textContent?.trim()).toBe('Browse the catalog');
    expect(page.querySelector('table')).toBeNull();
  });

  it('given a filled basket, when opening it, then each line shows its product, its amount and its price, and the total', async () => {
    fill();

    await open();

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Your shopping basket');
    expect(
      [...page.querySelectorAll('thead th')].map((header) => header.textContent?.trim()),
    ).toEqual(['Product', 'Amount', 'Price']);
    expect(rows()).toEqual([
      ['Chair', '1', '€49.99'],
      ['Lamp', '3', '€87.00'],
    ]);
    expect(total()).toContain('€136.99');
    expect(page.querySelector('tbody a')?.getAttribute('href')).toBe('/catalog/i1');
  });

  it('given a line, when raising its amount, then its price and the total follow', async () => {
    fill();
    await open();

    await changeAmount('Chair', '2');

    expect(rows()[0]).toEqual(['Chair', '2', '€99.98']);
    expect(total()).toContain('€186.98');
  });

  it.each([{ value: '0' }, { value: '-1' }, { value: '' }, { value: '1.5' }])(
    'given a line, when typing the amount $value, then the amount goes back to the one of the basket',
    async ({ value }) => {
      fill();
      await open();

      await changeAmount('Lamp', value);

      expect(amountOf('Lamp').value).toBe('3');
      expect(basket.lines()[1].amount).toBe(3);
    },
  );

  it('given a line, when removing it, then it leaves the basket and the total', async () => {
    fill();
    await open();

    page.querySelector<HTMLButtonElement>('button[aria-label="Remove Chair"]')!.click();
    await fixture.whenStable();

    expect(rows().map(([name]) => name)).toEqual(['Lamp']);
    expect(total()).toContain('€87.00');
  });

  it('given the last line, when removing it, then the basket says it is empty', async () => {
    basket.add({ id: 'i1', name: 'Chair', priceInCents: 4999 });
    await open();

    page.querySelector<HTMLButtonElement>('button[aria-label="Remove Chair"]')!.click();
    await fixture.whenStable();

    expect(page.textContent).toContain('Your basket is empty.');
  });

  it('given a filled basket, when opening it, then it offers to place the order', async () => {
    fill();

    await open();

    expect(page.querySelector('[data-testid="place-order"]')?.textContent?.trim()).toBe(
      'Place order',
    );
  });
});
