import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { pageOf, Pagination, providePaginatorTranslations } from './pagination';

@Component({
  imports: [Pagination],
  template: `<app-pagination [length]="42" [(pageIndex)]="pageIndex" [(pageSize)]="pageSize" />`,
})
class HostPage {
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
}

describe('pageOf', () => {
  const items = Array.from({ length: 23 }, (_, index) => index + 1);

  it.each([
    { pageIndex: 0, pageSize: 10, page: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] },
    { pageIndex: 2, pageSize: 10, page: [21, 22, 23] },
    { pageIndex: 5, pageSize: 10, page: [] },
  ])(
    'given page $pageIndex of $pageSize, when cutting 23 items, then it keeps $page',
    ({ pageIndex, pageSize, page }) => {
      const cut = pageOf(items, pageIndex, pageSize);

      expect(cut).toEqual(page);
    },
  );
});

describe('Pagination', () => {
  let host: HostPage;
  let loader: HarnessLoader;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HostPage],
      providers: [
        provideTestTranslations(),
        providePaginatorTranslations(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(HostPage);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  it('given 42 items, when going to the next page, then the page index follows and the range is translated', async () => {
    const paginator = await loader.getHarness(MatPaginatorHarness);

    await paginator.goToNextPage();

    expect(host.pageIndex()).toBe(1);
    expect(await paginator.getRangeLabel()).toBe('11 – 20 of 42');
  });

  it('given 42 items, when showing 25 per page, then the page size follows', async () => {
    const paginator = await loader.getHarness(MatPaginatorHarness);

    await paginator.setPageSize(25);

    expect(host.pageSize()).toBe(25);
    expect(await paginator.getRangeLabel()).toBe('1 – 25 of 42');
  });
});
