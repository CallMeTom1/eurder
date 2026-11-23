import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '@core/i18n/testing';
import { ItemCard } from './item-card';

@Component({
  imports: [ItemCard],
  template: `
    <app-item-card name="Chair" [priceInCents]="4999" [link]="['/catalog', 'i1']">
      <button type="button">Add</button>
    </app-item-card>
  `,
})
class HostPage {}

describe('ItemCard', () => {
  it('given an item, when rendered, then its image, name and price lead to it, with the actions below', async () => {
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
    expect(link?.querySelector('[data-testid="item-image"]')).not.toBeNull();
    expect(link?.querySelector('h2')?.textContent?.trim()).toBe('Chair');
    expect(link?.textContent).toContain('€49.99');
    expect(link?.querySelector('button')).toBeNull();
    expect(card.querySelector('button')?.textContent).toBe('Add');
  });
  it('given an item with a picture, when rendered, then the picture replaces the placeholder', async () => {
    localStorage.clear();
    TestBed.overrideTemplate(
      HostPage,
      '<app-item-card name="Chair" [priceInCents]="4999" [link]="[\'/catalog\', \'i1\']" imageUrl="/api/images/chair.jpg" />',
    );
    await TestBed.configureTestingModule({
      imports: [HostPage],
      providers: [provideTestTranslations(), provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(HostPage);

    await fixture.whenStable();

    const image = (fixture.nativeElement as HTMLElement).querySelector('img');
    expect(image?.getAttribute('src')).toBe('/api/images/chair.jpg');
    expect(image?.getAttribute('alt')).toBe('');
  });
});
