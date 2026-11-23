import { imageUrlOf } from '@core/api/api-routes';
import type { CatalogItemDto } from '@core/api/eurder-api';

export interface CatalogItem {
  id: string;
  name: string;
  description: string;
  priceInCents: number;
  stock: number;
  urgency: CatalogItemDto['urgency'];
  image: string | null;
  imageUrl: string | null;
}

const CENTS_PER_EURO = 100;

export function toCatalogItem(dto: CatalogItemDto): CatalogItem {
  return {
    id: dto.id,
    name: dto.name,
    description: dto.description,
    priceInCents: Math.round(dto.price * CENTS_PER_EURO),
    stock: dto.stock,
    urgency: dto.urgency,
    image: dto.image,
    imageUrl: imageUrlOf(dto.image),
  };
}
