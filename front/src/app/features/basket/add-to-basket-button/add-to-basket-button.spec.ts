import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { provideRouter, Router } from '@angular/router';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { SecurityService } from '@core/security/security.service';
import { CatalogItem } from '@features/catalog/catalog-item';
import { firstValueFrom } from 'rxjs';
import { BasketService } from '../basket.service';
import { AddToBasketButton } from './add-to-basket-button';

describe('AddToBasketButton', () => {
  let fixture: ComponentFixture<AddToBasketButton>;
  let element: HTMLElement;
  let backend: HttpTestingController;
  let documentLoader: HarnessLoader;

  const chair: CatalogItem = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    priceInCents: 4999,
    stock: 12,
    urgency: 'STOCK_HIGH',
  };
  const admin: CurrentUserDto = {
    id: 'a',
    role: 'ADMIN',
    email: 'admin@eurder.be',
    firstName: null,
    lastName: null,
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AddToBasketButton],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AddToBasketButton);
    fixture.componentRef.setInput('item', chair);
    element = fixture.nativeElement as HTMLElement;
    documentLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  afterEach(() => backend.verify());

  async function loggedInAs(user: CurrentUserDto): Promise<void> {
    const loaded = firstValueFrom(TestBed.inject(SecurityService).loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(user);
    await loaded;
  }

  function button(): HTMLButtonElement | null {
    return element.querySelector('button');
  }

  it('given an item, when adding it, then the basket holds one of it', async () => {
    await fixture.whenStable();

    button()!.click();
    await fixture.whenStable();

    expect(button()?.textContent).toContain('Add to basket');
    expect(TestBed.inject(BasketService).lines()).toEqual([
      { itemId: 'i1', name: 'Chair', priceInCents: 4999, amount: 1 },
    ]);
  });

  it('given an item, when adding it, then a message confirms it and leads to the basket', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await fixture.whenStable();

    button()!.click();
    const message = await documentLoader.getHarness(MatSnackBarHarness);

    expect(await message.getMessage()).toBe('Chair added to the basket');
    expect(await message.getActionDescription()).toBe('View basket');
    await message.dismissWithAction();
    expect(navigate).toHaveBeenCalledWith('/basket');
  });

  it('given an admin, when showing an item, then there is no button since an admin cannot order', async () => {
    await loggedInAs(admin);

    await fixture.whenStable();

    expect(button()).toBeNull();
  });
});
