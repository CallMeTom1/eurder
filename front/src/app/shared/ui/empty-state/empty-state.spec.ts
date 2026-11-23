import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EmptyState } from './empty-state';

@Component({
  imports: [EmptyState],
  template: `
    <app-empty-state message="No item yet">
      <button actions>Add an item</button>
    </app-empty-state>
  `,
})
class HostPage {}

describe('EmptyState', () => {
  it('given nothing to show, when rendered, then it shows the message and the action', async () => {
    const fixture = TestBed.createComponent(HostPage);

    await fixture.whenStable();

    const empty = fixture.nativeElement as HTMLElement;
    expect(empty.querySelector('p')?.textContent?.trim()).toBe('No item yet');
    expect(empty.querySelector('button')?.textContent).toBe('Add an item');
  });
});
