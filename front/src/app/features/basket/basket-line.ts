export interface BasketLine {
  itemId: string;
  name: string;
  priceInCents: number;
  amount: number;
}

export function isBasketLine(value: unknown): value is BasketLine {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const line = value as Record<string, unknown>;
  return (
    typeof line['itemId'] === 'string' &&
    typeof line['name'] === 'string' &&
    Number.isInteger(line['priceInCents']) &&
    isAmount(line['amount'])
  );
}

export function isAmount(amount: unknown): amount is number {
  return Number.isInteger(amount) && (amount as number) >= 1;
}
