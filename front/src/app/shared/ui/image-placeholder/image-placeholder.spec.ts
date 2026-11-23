import { TestBed } from '@angular/core/testing';
import { ImagePlaceholder } from './image-placeholder';

describe('ImagePlaceholder', () => {
  it('given no image yet, when rendered, then it draws a crossed box hidden from screen readers', async () => {
    await TestBed.configureTestingModule({ imports: [ImagePlaceholder] }).compileComponents();
    const fixture = TestBed.createComponent(ImagePlaceholder);

    await fixture.whenStable();

    const drawing = (fixture.nativeElement as HTMLElement).querySelector('svg');
    expect(drawing?.getAttribute('aria-hidden')).toBe('true');
    expect(drawing?.querySelectorAll('line')).toHaveLength(2);
  });
});
