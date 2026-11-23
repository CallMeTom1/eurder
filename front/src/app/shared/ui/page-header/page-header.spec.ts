import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeader } from './page-header';

@Component({
  imports: [PageHeader],
  template: `
    <app-page-header title="Items" subtitle="Everything we sell">
      <button actions>New item</button>
    </app-page-header>
  `,
})
class HostPage {}

describe('PageHeader', () => {
  it('given a title, a subtitle and an action, when rendered, then the title is the heading of the page and the action sits beside it', async () => {
    const fixture = TestBed.createComponent(HostPage);

    await fixture.whenStable();

    const header = fixture.nativeElement as HTMLElement;
    expect(header.querySelector('h1')?.textContent?.trim()).toBe('Items');
    expect(header.querySelector('p')?.textContent?.trim()).toBe('Everything we sell');
    expect(header.querySelector('button')?.textContent).toBe('New item');
  });

  it('given no subtitle, when rendered, then no empty paragraph is left', async () => {
    const fixture = TestBed.createComponent(PageHeader);
    fixture.componentRef.setInput('title', 'Customers');

    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('p')).toBeNull();
  });
});
