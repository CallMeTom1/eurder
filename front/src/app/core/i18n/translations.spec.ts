import { problemDetailCodeValues } from '@core/api/eurder-api';
import en from '../../../../public/i18n/en.json';
import fr from '../../../../public/i18n/fr.json';
import nl from '../../../../public/i18n/nl.json';

function keysOf(translations: object, prefix = ''): string[] {
  return Object.entries(translations).flatMap(([key, value]) =>
    typeof value === 'object' ? keysOf(value, `${prefix}${key}.`) : [`${prefix}${key}`],
  );
}

describe('Translations', () => {
  it.each([
    { language: 'fr', other: fr },
    { language: 'nl', other: nl },
  ])(
    'given the English texts, when comparing with $language, then both have exactly the same keys',
    ({ other }) => {
      const english = keysOf(en).sort();

      const translated = keysOf(other).sort();

      expect(translated).toEqual(english);
    },
  );

  it.each([
    { language: 'en', translations: en },
    { language: 'fr', translations: fr },
    { language: 'nl', translations: nl },
  ])('given the $language texts, when reading them, then no text is empty', ({ translations }) => {
    const texts = keysOf(translations).map((key) =>
      key
        .split('.')
        .reduce<unknown>((node, part) => (node as Record<string, unknown>)[part], translations),
    );

    expect(texts.filter((text) => typeof text !== 'string' || text.trim() === '')).toEqual([]);
  });
});

describe('Translations of the API error codes', () => {
  it('given every error code the API can send, when looking for its English text, then each one has a text', () => {
    const english = keysOf(en);

    const untranslated = problemDetailCodeValues.filter((code) => !english.includes(code));

    expect(untranslated).toEqual([]);
  });
});
