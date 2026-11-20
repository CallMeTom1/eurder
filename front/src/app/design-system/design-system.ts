import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ThemeToggle } from '@shared/ui/theme-toggle/theme-toggle';

interface Swatch {
  name: string;
  colors: string;
}

@Component({
  selector: 'app-design-system',
  imports: [MatButtonModule, ThemeToggle],
  templateUrl: './design-system.html',
})
export class DesignSystem {
  protected readonly swatches: Swatch[] = [
    { name: 'primary', colors: 'bg-primary text-on-primary' },
    { name: 'primary-container', colors: 'bg-primary-container text-on-primary-container' },
    { name: 'secondary', colors: 'bg-secondary text-on-secondary' },
    { name: 'secondary-container', colors: 'bg-secondary-container text-on-secondary-container' },
    { name: 'tertiary', colors: 'bg-tertiary text-on-tertiary' },
    { name: 'error', colors: 'bg-error text-on-error' },
    { name: 'surface', colors: 'bg-surface text-on-surface border border-outline-variant' },
    { name: 'surface-container', colors: 'bg-surface-container text-on-surface' },
  ];
}
