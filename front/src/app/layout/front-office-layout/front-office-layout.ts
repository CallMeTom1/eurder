import { Component } from '@angular/core';
import { MatAnchor, MatButton } from '@angular/material/button';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSwitcher } from '@shared/ui/language-switcher/language-switcher';
import { Logo } from '@shared/ui/logo/logo';
import { ThemeToggle } from '@shared/ui/theme-toggle/theme-toggle';

interface NavigationLink {
  path: string;
  labelKey: string;
}

@Component({
  selector: 'app-front-office-layout',
  imports: [
    LanguageSwitcher,
    Logo,
    MatAnchor,
    MatButton,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    ThemeToggle,
    TranslatePipe,
  ],
  templateUrl: './front-office-layout.html',
  host: { class: 'flex min-h-full flex-col' },
})
export class FrontOfficeLayout {
  protected readonly year = new Date().getFullYear();

  protected readonly links: NavigationLink[] = [
    { path: '/catalog', labelKey: 'nav.catalog' },
    { path: '/basket', labelKey: 'nav.basket' },
  ];
}
