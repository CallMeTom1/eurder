import { startingWith } from './starting-with';

describe('startingWith', () => {
  const names = [{ name: 'Chair' }, { name: 'armchair' }, { name: 'Lamp' }];

  it.each([
    { query: 'ch', kept: ['Chair'] },
    { query: '  AR ', kept: ['armchair'] },
    { query: 'chair', kept: ['Chair'] },
    { query: '', kept: ['Chair', 'armchair', 'Lamp'] },
    { query: 'sofa', kept: [] },
  ])('given the query "$query", when filtering, then $kept stay', ({ query, kept }) => {
    const filtered = startingWith(names, query, (item) => item.name);

    expect(filtered.map((item) => item.name)).toEqual(kept);
  });
});
