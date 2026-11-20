import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { ThemePreference, ThemeService } from '@core/theme/theme.service';

interface ThemeOption {
  preference: ThemePreference;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-theme-toggle',
  imports: [MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './theme-toggle.html',
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);

  protected readonly options: ThemeOption[] = [
    { preference: 'light', label: 'Light', icon: 'light_mode' },
    { preference: 'dark', label: 'Dark', icon: 'dark_mode' },
    { preference: 'system', label: 'System', icon: 'contrast' },
  ];

  protected readonly currentIcon = computed(
    () => this.options.find((option) => option.preference === this.theme.preference())?.icon,
  );
}
