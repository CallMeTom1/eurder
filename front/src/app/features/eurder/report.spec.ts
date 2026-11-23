import { toReport } from './report';

describe('toReport', () => {
  it('given a report of the API, when mapping it, then every price is in cents', () => {
    const report = toReport({
      eurders: [
        {
          id: 'e1',
          itemGroups: [{ itemName: 'Lamp', itemState: 'PRISTINE', amount: 3, subtotal: 0.3 }],
          totalPrice: 0.3,
        },
      ],
      totalPrice: 0.3,
    });

    expect(report).toEqual({
      eurders: [
        {
          id: 'e1',
          lines: [{ itemName: 'Lamp', itemState: 'PRISTINE', amount: 3, subtotalInCents: 30 }],
          totalInCents: 30,
        },
      ],
      totalInCents: 30,
    });
  });
});
