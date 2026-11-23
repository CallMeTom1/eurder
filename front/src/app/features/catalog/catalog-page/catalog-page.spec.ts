import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ApiProblem } from '@core/api/api-problem';
import { provideTestTranslations } from '@core/i18n/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { CatalogItem } from '../catalog-item';
import { CatalogService } from '../catalog.service';
import { CatalogPage } from './catalog-page';

describe('CatalogPage', () => {
  let fixture: ComponentFixture<CatalogPage>;
  let page: HTMLElement;
  let findAll: ReturnType<typeof vi.fn>;

  const chair: CatalogItem = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    priceInCents: 4999,
  };
  const lamp: CatalogItem = {
    id: 'i2',
    name: 'Lamp',
    description: 'Desk lamp',
    priceInCents: 2900,
  };
  const armchair: CatalogItem = {
    id: 'i3',
    name: 'armchair',
    description: 'Leather',
    priceInCents: 15000,
  };

  async function open(...answers: Observable<CatalogItem[]>[]): Promise<void> {
    create(answers);
    await fixture.whenStable();
  }

  function openWhileLoading(): void {
    create([new Subject<CatalogItem[]>()]);
    fixture.detectChanges();
  }

  function create(answers: Observable<CatalogItem[]>[]): void {
    localStorage.clear();
    findAll = vi.fn();
    answers.forEach((answer) => findAll.mockReturnValueOnce(answer));
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        { provide: CatalogService, useValue: { findAll } },
      ],
    });
    fixture = TestBed.createComponent(CatalogPage);
    page = fixture.nativeElement as HTMLElement;
  }

  function names(): string[] {
    return [...page.querySelectorAll('[data-testid="catalog-item"] h2')].map((name) =>
      name.textContent!.trim(),
    );
  }

  async function search(query: string): Promise<void> {
    const input = page.querySelector<HTMLInputElement>('input[type="search"]')!;
    input.value = query;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('given the catalog is loading, when the page opens, then it says so', async () => {
    openWhileLoading();

    const status = page.querySelector('[role="status"]');

    expect(status?.textContent).toContain('Loading…');
  });

  it('given items for sale, when the page opens, then each one shows its name, its price and a link to its detail', async () => {
    await open(of([chair, lamp]));

    const firstCard = page.querySelector('[data-testid="catalog-item"]');

    expect(names()).toEqual(['Chair', 'Lamp']);
    expect(firstCard?.textContent).toContain('€49.99');
    expect(firstCard?.querySelector('a')?.getAttribute('href')).toBe('/catalog/i1');
  });

  it('given items for sale, when searching, then only the names starting with the search stay, whatever the case', async () => {
    await open(of([chair, lamp, armchair]));

    await search('  AR ');

    expect(names()).toEqual(['armchair']);
  });

  it('given items for sale, when searching a word in the middle of a name, then nothing matches', async () => {
    await open(of([chair, lamp, armchair]));

    await search('chair');

    expect(names()).toEqual(['Chair']);
  });

  it('given a search without match, when filtering, then the page says no item starts with it', async () => {
    await open(of([chair, lamp]));

    await search('sofa');

    expect(page.textContent).toContain('No item starts with “sofa”.');
  });

  it('given an empty catalog, when the page opens, then it says nothing is for sale yet', async () => {
    await open(of([]));

    const text = page.textContent;

    expect(text).toContain('No item for sale yet.');
  });

  it('given the API fails, when trying again, then the catalog loads', async () => {
    const down: ApiProblem = {
      status: 404,
      code: 'error.common.route-not-found',
      params: {},
      errors: [],
    };
    await open(
      throwError(() => down),
      of([chair]),
    );
    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'This page does not exist.',
    );

    page.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    await fixture.whenStable();

    expect(names()).toEqual(['Chair']);
  });
});
