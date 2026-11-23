import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ApiProblem } from '@core/api/api-problem';
import { provideTestTranslations } from '@core/i18n/testing';
import { Observable, of, throwError } from 'rxjs';
import { CatalogItem } from '../catalog-item';
import { CatalogService } from '../catalog.service';
import { CatalogItemPage } from './catalog-item-page';

describe('CatalogItemPage', () => {
  let fixture: ComponentFixture<CatalogItemPage>;
  let page: HTMLElement;
  let getItem: ReturnType<typeof vi.fn>;

  async function open(id: string, answer: Observable<CatalogItem>): Promise<void> {
    localStorage.clear();
    getItem = vi.fn().mockReturnValue(answer);
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        { provide: CatalogService, useValue: { getItem } },
      ],
    });
    fixture = TestBed.createComponent(CatalogItemPage);
    fixture.componentRef.setInput('id', id);
    await fixture.whenStable();
    page = fixture.nativeElement as HTMLElement;
  }

  it('given an item, when opening its page, then it shows its name, description and price and leads back to the catalog', async () => {
    await open(
      'i1',
      of({ id: 'i1', name: 'Chair', description: 'Wooden chair', priceInCents: 4999 }),
    );

    const heading = page.querySelector('h1');

    expect(getItem).toHaveBeenCalledWith('i1');
    expect(heading?.textContent?.trim()).toBe('Chair');
    expect(page.textContent).toContain('Wooden chair');
    expect(page.textContent).toContain('€49.99');
    expect(page.querySelector('a[href="/catalog"]')?.textContent?.trim()).toBe(
      'Back to the catalog',
    );
  });

  it('given an unknown item, when opening its page, then it says the item does not exist and leads back', async () => {
    const missing: ApiProblem = {
      status: 404,
      code: 'error.item.not-found',
      params: { id: 'nope' },
      errors: [],
    };
    await open(
      'nope',
      throwError(() => missing),
    );

    const alert = page.querySelector('[role="alert"]');

    expect(alert?.textContent).toContain('This item does not exist.');
    expect(page.querySelector('a[href="/catalog"]')).not.toBeNull();
  });
});
