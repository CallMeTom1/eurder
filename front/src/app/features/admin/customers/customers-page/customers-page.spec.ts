import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { CustomerDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { providePaginatorTranslations } from '@shared/ui/pagination/pagination';
import { CustomersPage } from './customers-page';

describe('CustomersPage', () => {
  let fixture: ComponentFixture<CustomersPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  function customer(id: string, firstName: string, lastName: string): CustomerDto {
    return {
      id,
      firstName,
      lastName,
      email: `${firstName.toLowerCase()}@doe.be`,
      phoneNumber: '0470123456',
      address: {
        street: 'Rue Haute',
        houseNumber: '1',
        postalCode: '1000',
        city: 'Bruxelles',
        country: 'BELGIUM',
      },
    };
  }

  const jane = customer('c1', 'Jane', 'Doe');
  const john = customer('c2', 'John', 'Smith');
  const dora = customer('c3', 'Dora', 'dupont');

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CustomersPage],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        providePaginatorTranslations(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(CustomersPage);
    page = fixture.nativeElement as HTMLElement;
    backend = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  async function answer(customers: CustomerDto[]): Promise<void> {
    backend.expectOne({ method: 'GET', url: '/api/customers' }).flush(customers);
    await fixture.whenStable();
  }

  function rows(): string[][] {
    return [...page.querySelectorAll('[data-testid="customer"]')].map((row) =>
      [...row.querySelectorAll('th, td')].slice(0, 3).map((cell) => cell.textContent!.trim()),
    );
  }

  async function search(query: string): Promise<void> {
    const input = page.querySelector<HTMLInputElement>('input[type="search"]')!;
    input.value = query;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('given customers, when the page opens, then each row shows the id, the first name and the last name', async () => {
    await answer([jane, john]);

    const table = rows();

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Customers');
    expect(table).toEqual([
      ['c1', 'Jane', 'Doe'],
      ['c2', 'John', 'Smith'],
    ]);
  });

  it('given a customer row, when reading it, then the id and View lead to the detail and Edit to the edit page', async () => {
    await answer([jane]);

    const row = page.querySelector('[data-testid="customer"]')!;

    expect(row.querySelector('th a')?.getAttribute('href')).toBe('/admin/customers/c1');
    expect(row.querySelector('a[aria-label="View Jane Doe"]')?.getAttribute('href')).toBe(
      '/admin/customers/c1',
    );
    expect(row.querySelector('a[aria-label="Edit Jane Doe"]')?.getAttribute('href')).toBe(
      '/admin/customers/c1/edit',
    );
  });

  it('given customers, when filtering by last name, then only the last names starting with it stay, whatever the case', async () => {
    await answer([jane, john, dora]);

    await search('D');

    expect(rows().map(([, , lastName]) => lastName)).toEqual(['Doe', 'dupont']);
  });

  it('given a filter without match, when filtering, then the page says so', async () => {
    await answer([jane]);

    await search('Z');

    expect(page.textContent).toContain('No last name starts with “Z”.');
  });

  it('given the page, when it opens, then it offers to create a new customer', async () => {
    await answer([]);

    const link = [...page.querySelectorAll('a')].find((candidate) =>
      candidate.textContent?.includes('New customer'),
    );

    expect(link?.getAttribute('href')).toBe('/admin/customers/new');
    expect(page.textContent).toContain('No customer yet.');
  });

  it('given more than ten customers, when the page opens, then it shows the first ten and paginates the rest', async () => {
    await answer(
      Array.from({ length: 11 }, (_, index) => customer(`c${index}`, 'Jane', `Doe${index}`)),
    );

    const range = page.querySelector('.mat-mdc-paginator-range-label')?.textContent?.trim();

    expect(rows()).toHaveLength(10);
    expect(range).toBe('1 – 10 of 11');
  });
});
