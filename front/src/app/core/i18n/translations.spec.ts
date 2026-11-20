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
    ['fr', fr],
    ['nl', nl],
  ])(
    'given the English texts, when comparing with %s, then both have exactly the same keys',
    (_, other) => {
      const english = keysOf(en).sort();

      const translated = keysOf(other).sort();

      expect(translated).toEqual(english);
    },
  );

  it.each([
    ['en', en],
    ['fr', fr],
    ['nl', nl],
  ])('given the %s texts, when reading them, then no text is empty', (_, translations) => {
    const texts = keysOf(translations).map((key) =>
      key
        .split('.')
        .reduce<unknown>((node, part) => (node as Record<string, unknown>)[part], translations),
    );

    expect(texts.filter((text) => typeof text !== 'string' || text.trim() === '')).toEqual([]);
  });
});
