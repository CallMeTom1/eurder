import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { provideRouter } from '@angular/router';
import type { ReportDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { MyOrdersPage } from './my-orders-page';

describe('MyOrdersPage', () => {
  let fixture: ComponentFixture<MyOrdersPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let documentLoader: HarnessLoader;

  const report: ReportDto = {
    eurders: [
      {
        id: 'e1',
        itemGroups: [{ itemName: 'Chair', itemState: 'PRISTINE', amount: 2, subtotal: 99.98 }],
        totalPrice: 99.98,
      },
    ],
    totalPrice: 99.98,
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MyOrdersPage],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyOrdersPage);
    page = fixture.nativeElement as HTMLElement;
    documentLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
    backend.expectOne({ method: 'GET', url: '/api/eurders/report' }).flush(report);
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  function reorderButton(): HTMLButtonElement {
    return page.querySelector<HTMLButtonElement>('[data-testid="eurder"] button')!;
  }

  it('given orders, when the page opens, then it shows them with a button to order again', () => {
    const orders = page.querySelectorAll('[data-testid="eurder"]');

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('My orders');
    expect(orders).toHaveLength(1);
    expect(reorderButton().textContent).toContain('Order again');
  });

  it('given a past order, when ordering it again, then the API reorders it, a message confirms it and the report reloads', async () => {
    reorderButton().click();

    backend
      .expectOne({ method: 'POST', url: '/api/eurders/e1/reorder' })
      .flush({ id: 'e2', itemGroups: [], totalPrice: 99.98 });
    await new Promise((resolve) => setTimeout(resolve));
    backend.expectOne('/api/eurders/report').flush(report);
    await settle();
    const message = await documentLoader.getHarness(MatSnackBarHarness);

    expect(await message.getMessage()).toBe('Order placed again.');
  });

  it('given an order with an item that no longer exists, when ordering it again, then the page says so', async () => {
    reorderButton().click();

    backend
      .expectOne('/api/eurders/e1/reorder')
      .flush(
        { status: 404, code: 'error.item.not-found', params: { id: 'i9' } },
        { status: 404, statusText: 'Not Found' },
      );
    await settle();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'This item does not exist.',
    );
  });
});
