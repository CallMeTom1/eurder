import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('given the app, when it starts, then it only hosts the router outlet', async () => {
    const fixture = TestBed.createComponent(App);

    await fixture.whenStable();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('router-outlet')).not.toBeNull();
    expect(root.children).toHaveLength(1);
  });
});
