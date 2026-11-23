import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { CustomerDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { CustomerDetailPage } from './customer-detail-page';

describe('CustomerDetailPage', () => {
  let fixture: ComponentFixture<CustomerDetailPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  const jane: CustomerDto = {
    id: 'c1',
    firstName: 'Jane',
    lastName: 'Doe',
    email: 'jane@doe.be',
    phoneNumber: '0470 12 34 56',
    address: {
      street: 'Rue Haute',
      houseNumber: '16A',
      postalCode: '1000',
      city: 'Bruxelles',
      country: 'FRANCE',
    },
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CustomerDetailPage],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CustomerDetailPage);
    fixture.componentRef.setInput('id', 'c1');
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  async function answer(): Promise<void> {
    backend.expectOne({ method: 'GET', url: '/api/customers/c1' }).flush(jane);
    backend.expectOne({ method: 'GET', url: '/api/eurders/reports/c1' }).flush({
      eurders: [
        {
          id: 'e1',
          itemGroups: [{ itemName: 'Chair', itemState: 'PRISTINE', amount: 2, subtotal: 99.98 }],
          totalPrice: 99.98,
        },
      ],
      totalPrice: 99.98,
    });
    await fixture.whenStable();
  }

  function detail(label: string): string | undefined {
    const term = [...page.querySelectorAll('dt')].find(
      (candidate) => candidate.textContent?.trim() === label,
    );
    return term?.nextElementSibling?.textContent?.replace(/\s+/g, ' ').trim();
  }

  function link(name: string): string | null | undefined {
    return [...page.querySelectorAll('a')]
      .find((candidate) => candidate.textContent?.trim().endsWith(name))
      ?.getAttribute('href');
  }

  it('given a customer, when the page opens, then its id heads the page and its details are listed', async () => {
    await answer();

    const header = page.querySelector('app-page-header');

    expect(header?.querySelector('h1')?.textContent?.trim()).toBe('Customer');
    expect(header?.textContent).toContain('c1');
    expect(detail('First name')).toBe('Jane');
    expect(detail('Last name')).toBe('Doe');
    expect(detail('Email')).toBe('jane@doe.be');
    expect(detail('Phone number')).toBe('0470 12 34 56');
    expect(detail('Address')).toBe('Rue Haute 16A, 1000 Bruxelles, France');
  });

  it('given a customer, when the page opens, then it leads to its edit page and back to the customers', async () => {
    await answer();

    const edit = link('Edit');

    expect(edit).toBe('/admin/customers/c1/edit');
    expect(link('Back')).toBe('/admin/customers');
  });

  it('given a customer with orders, when the page opens, then their report follows, without ordering again', async () => {
    await answer();

    const report = page.querySelector('app-eurder-report');

    expect(report?.querySelectorAll('[data-testid="eurder"]')).toHaveLength(1);
    expect(report?.textContent).toContain('Chair');
    expect(report?.querySelector('button')).toBeNull();
  });

  it('given an unknown customer, when the page opens, then it says so', async () => {
    backend
      .expectOne('/api/customers/c1')
      .flush(
        { status: 404, code: 'error.customer.not-found', params: { id: 'c1' } },
        { status: 404, statusText: 'Not Found' },
      );
    backend
      .expectOne('/api/eurders/reports/c1')
      .flush(
        { status: 404, code: 'error.customer.not-found', params: { id: 'c1' } },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();

    const alert = page.querySelector('[role="alert"]');

    expect(alert?.textContent).toContain('This customer does not exist.');
  });
});
