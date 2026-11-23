import { Component } from '@angular/core';

@Component({
  selector: 'app-image-placeholder',
  template: `
    <svg
      aria-hidden="true"
      viewBox="0 0 200 100"
      preserveAspectRatio="none"
      class="block aspect-[2/1] w-full border border-outline text-outline-variant"
    >
      <line
        x1="0"
        y1="0"
        x2="200"
        y2="100"
        stroke="currentColor"
        vector-effect="non-scaling-stroke"
      />
      <line
        x1="200"
        y1="0"
        x2="0"
        y2="100"
        stroke="currentColor"
        vector-effect="non-scaling-stroke"
      />
    </svg>
  `,
  host: { class: 'block' },
})
export class ImagePlaceholder {}
