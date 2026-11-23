import { max, min, required, SchemaPathTree } from '@angular/forms/signals';
import {
  atMost,
  digits,
  notNegative,
  positive,
  wholeNumber,
} from '@shared/forms/eurder-validators';

export interface ItemForm {
  name: string;
  description: string;
  price: number | null;
  stock: number | null;
  image: string | null;
}

export const MAX_TEXT = 255;
const MAX_UNITS = 1000;

export function itemRules(path: SchemaPathTree<ItemForm>, minStock: () => number): void {
  required(path.name);
  atMost(path.name, MAX_TEXT);
  required(path.description);
  atMost(path.description, MAX_TEXT);
  required(path.price);
  positive(path.price);
  digits(path.price, 8, 2);
  required(path.stock);
  notNegative(path.stock);
  wholeNumber(path.stock);
  min(path.stock, minStock);
  max(path.stock, MAX_UNITS);
}
