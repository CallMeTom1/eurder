import { computed, Service, signal } from '@angular/core';
import { CatalogItem } from '@features/catalog/catalog-item';
import { BasketLine, isAmount, isBasketLine } from './basket-line';

const STORAGE_KEY = 'eurder.basket';

@Service()
export class BasketService {
  private readonly stored = signal<BasketLine[]>(storedLines());

  readonly lines = this.stored.asReadonly();
  readonly count = computed(() => this.lines().reduce((count, line) => count + line.amount, 0));
  readonly totalInCents = computed(() =>
    this.lines().reduce((total, line) => total + line.amount * line.priceInCents, 0),
  );

  add(item: CatalogItem): void {
    const existing = this.lines().find((line) => line.itemId === item.id);
    if (existing) {
      this.changeAmount(item.id, existing.amount + 1);
      return;
    }
    this.save([
      ...this.lines(),
      { itemId: item.id, name: item.name, priceInCents: item.priceInCents, amount: 1 },
    ]);
  }

  changeAmount(itemId: string, amount: number): void {
    if (!isAmount(amount)) {
      return;
    }
    this.save(this.lines().map((line) => (line.itemId === itemId ? { ...line, amount } : line)));
  }

  remove(itemId: string): void {
    this.save(this.lines().filter((line) => line.itemId !== itemId));
  }

  clear(): void {
    this.save([]);
  }

  private save(lines: BasketLine[]): void {
    this.stored.set(lines);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
  }
}

function storedLines(): BasketLine[] {
  try {
    const stored: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    return Array.isArray(stored) && stored.every(isBasketLine) ? stored : [];
  } catch {
    return [];
  }
}
