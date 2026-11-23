import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { ApiRoutes } from '@core/api/api-routes';
import type {
  AddUnitsDto,
  CreateItemDto,
  ItemDto,
  ItemOverviewDto,
  UpdateItemDto,
  UploadedImageDto,
} from '@core/api/eurder-api';
import { map, Observable } from 'rxjs';
import { AdminItem, ItemDetails, NewItem, toAdminItem } from './admin-item';

@Service()
export class AdminItemService {
  private readonly http = inject(HttpClient);

  getItems(): Observable<AdminItem[]> {
    return this.http
      .get<ItemOverviewDto[]>(ApiRoutes.items.all)
      .pipe(map((items) => items.map(toAdminItem)));
  }

  addItem({ stock, ...details }: NewItem): Observable<string> {
    const item: CreateItemDto = { ...details, units: { PRISTINE: stock } };
    return this.http.post<ItemDto>(ApiRoutes.items.all, item).pipe(map((created) => created.id));
  }

  updateItem(id: string, details: ItemDetails): Observable<void> {
    const item: UpdateItemDto = details;
    return this.http.put<ItemDto>(ApiRoutes.items.one(id), item).pipe(map(() => undefined));
  }

  uploadImage(picture: File): Observable<string> {
    const form = new FormData();
    form.append('file', picture);
    return this.http
      .post<UploadedImageDto>(ApiRoutes.images.all, form)
      .pipe(map((uploaded) => uploaded.path));
  }

  addUnits(id: string, amount: number): Observable<void> {
    const units: AddUnitsDto = { amount, state: 'PRISTINE' };
    return this.http.post<ItemDto>(ApiRoutes.items.units(id), units).pipe(map(() => undefined));
  }
}
