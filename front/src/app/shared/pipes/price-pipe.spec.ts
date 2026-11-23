import { TestBed } from '@angular/core/testing';
import { Language, LanguageService } from '@core/i18n/language.service';
import { provideTestTranslations } from '@core/i18n/testing';
import { PricePipe } from './price-pipe';

describe('PricePipe', () => {
  function pipeIn(language: Language): PricePipe {
    localStorage.setItem('eurder.lang', language);
    TestBed.configureTestingModule({ providers: [provideTestTranslations(), PricePipe] });
    return TestBed.inject(PricePipe);
  }

  function withPlainSpaces(text: string): string {
    return text.replace(/\s/g, ' ');
  }

  beforeEach(() => localStorage.clear());

  it.each([
    { language: 'fr', expected: '49,99 €' },
    { language: 'nl', expected: '€ 49,99' },
    { language: 'en', expected: '€49.99' },
  ] as const)(
    'given a $language page, when formatting 4999 cents, then shows $expected',
    ({ language, expected }) => {
      const pipe = pipeIn(language);

      const price = pipe.transform(4999);

      expect(withPlainSpaces(price)).toBe(expected);
    },
  );

  it('given a French page, when formatting a large amount, then groups the thousands', () => {
    const pipe = pipeIn('fr');

    const price = pipe.transform(123456789);

    expect(withPlainSpaces(price)).toBe('1 234 567,89 €');
  });

  it('given a French page, when the language changes to Dutch, then the same pipe formats in Dutch', () => {
    const pipe = pipeIn('fr');

    TestBed.inject(LanguageService).choose('nl');

    expect(withPlainSpaces(pipe.transform(500))).toBe('€ 5,00');
  });
});
