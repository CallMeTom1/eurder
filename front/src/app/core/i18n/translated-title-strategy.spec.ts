import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, Router, TitleStrategy } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { LanguageService } from './language.service';
import { provideTestTranslations } from './testing';
import { TranslatedTitleStrategy } from './translated-title-strategy';

@Component({ template: '' })
class EmptyPage {}

describe('TranslatedTitleStrategy', () => {
  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('eurder.lang', 'fr');
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([
          { path: 'catalog', component: EmptyPage, title: 'nav.catalog' },
          { path: 'untitled', component: EmptyPage },
        ]),
        { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
      ],
    });
    await firstValueFrom(TestBed.inject(LanguageService).start());
  });

  it('given a route with a title key, when navigating to it, then the tab shows its translation and the name of the shop', async () => {
    await TestBed.inject(Router).navigateByUrl('/catalog');

    const title = TestBed.inject(Title).getTitle();

    expect(title).toBe('Catalogue · Eurder');
  });

  it('given a route without a title, when navigating to it, then the tab shows the name of the shop', async () => {
    await TestBed.inject(Router).navigateByUrl('/untitled');

    const title = TestBed.inject(Title).getTitle();

    expect(title).toBe('Eurder');
  });

  it('given the catalog, when the language changes, then the tab title follows without navigating', async () => {
    await TestBed.inject(Router).navigateByUrl('/catalog');

    TestBed.inject(LanguageService).choose('nl');

    await vi.waitFor(() => expect(TestBed.inject(Title).getTitle()).toBe('Catalogus · Eurder'));
  });
});
