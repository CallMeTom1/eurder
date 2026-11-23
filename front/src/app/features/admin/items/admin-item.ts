import { imageUrlOf } from '@core/api/api-routes';
import type { ItemOverviewDto } from '@core/api/eurder-api';

export interface AdminItem {
  id: string;
  name: string;
  description: string;
  priceInCents: number;
  stock: number;
  urgency: ItemOverviewDto['urgency'];
  image: string | null;
  imageUrl: string | null;
}

export interface ItemDetails {
  name: string;
  description: string;
  price: number;
  image: string | null;
}

export interface NewItem extends ItemDetails {
  stock: number;
}

const CENTS_PER_EURO = 100;

export function toAdminItem(dto: ItemOverviewDto): AdminItem {
  return {
    id: dto.id,
    name: dto.name,
    description: dto.description,
    priceInCents: Math.round(dto.price * CENTS_PER_EURO),
    stock: Object.values(dto.availableUnits).reduce((total, units) => total + units, 0),
    urgency: dto.urgency,
    image: dto.image,
    imageUrl: imageUrlOf(dto.image),
  };
}
