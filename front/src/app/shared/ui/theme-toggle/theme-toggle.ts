import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { ThemePreference, ThemeService } from '@core/theme/theme.service';
import { TranslatePipe } from '@ngx-translate/core';

interface ThemeOption {
  preference: ThemePreference;
  labelKey: string;
  icon: string;
}

@Component({
  imports: [MatButtonModule, MatIconModule, MatMenuModule, TranslatePipe],
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);

  protected readonly options: ThemeOption[] = [
    { preference: 'light', labelKey: 'common.theme.light', icon: 'light_mode' },
    { preference: 'dark', labelKey: 'common.theme.dark', icon: 'dark_mode' },
    { preference: 'system', labelKey: 'common.theme.system', icon: 'contrast' },
  ];

  protected readonly currentIcon = computed(
    () => this.options.find((option) => option.preference === this.theme.preference())?.icon,
  );
}
