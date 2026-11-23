import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '@core/i18n/testing';
import { ItemCard } from './item-card';

@Component({
  imports: [ItemCard],
  template: `<app-item-card
    name="Chair"
    [priceInCents]="4999"
    urgency="STOCK_MEDIUM"
    [link]="['/catalog', 'i1']"
  />`,
})
class HostPage {}

describe('ItemCard', () => {
  it('given an item, when rendered, then its name and image lead to it, with its price and its stock urgency', async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HostPage],
      providers: [provideTestTranslations(), provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(HostPage);

    await fixture.whenStable();

    const card = fixture.nativeElement as HTMLElement;
    const link = card.querySelector('a');
    expect(link?.getAttribute('href')).toBe('/catalog/i1');
    expect(link?.querySelector('h2')?.textContent?.trim()).toBe('Chair');
    expect(link?.querySelector('app-image-placeholder')).not.toBeNull();
    expect(card.textContent).toContain('Price: €49.99');
    expect(card.querySelector('app-stock-urgency-badge')?.textContent?.trim()).toBe('Medium stock');
  });
});
