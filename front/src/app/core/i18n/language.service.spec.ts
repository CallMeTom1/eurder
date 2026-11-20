import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { LanguageService } from './language.service';
import { provideTestTranslations } from './testing';

describe('LanguageService', () => {
  const root = document.documentElement;

  beforeEach(() => {
    localStorage.clear();
    root.lang = '';
    TestBed.configureTestingModule({ providers: [provideTestTranslations()] });
  });

  function browserLanguages(...languages: string[]) {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(languages);
  }

  async function startedService(): Promise<LanguageService> {
    const service = TestBed.inject(LanguageService);
    await firstValueFrom(service.start());
    return service;
  }

  it('given a stored language, when the service starts, then it uses that language', async () => {
    localStorage.setItem('eurder.lang', 'nl');
    browserLanguages('fr-BE');

    const service = await startedService();

    expect(service.language()).toBe('nl');
    expect(root.lang).toBe('nl');
    expect(TestBed.inject(TranslateService).getCurrentLang()).toBe('nl');
  });

  it('given no stored language and a Belgian French browser, when the service starts, then it uses French', async () => {
    browserLanguages('fr-BE', 'en');

    const service = await startedService();

    expect(service.language()).toBe('fr');
  });

  it('given no stored language and a German browser, when the service starts, then it falls back to English', async () => {
    browserLanguages('de-DE');

    const service = await startedService();

    expect(service.language()).toBe('en');
  });

  it('given an unknown stored language, when the service starts, then it uses the browser language', async () => {
    localStorage.setItem('eurder.lang', 'xx');
    browserLanguages('nl-BE');

    const service = await startedService();

    expect(service.language()).toBe('nl');
  });

  it('given the service, when choosing French, then the texts, the page language and the stored choice follow', async () => {
    browserLanguages('en');
    const service = await startedService();

    service.choose('fr');

    expect(service.language()).toBe('fr');
    expect(root.lang).toBe('fr');
    expect(localStorage.getItem('eurder.lang')).toBe('fr');
    expect(TestBed.inject(TranslateService).instant('common.theme.dark')).toBe('Sombre');
  });
});
