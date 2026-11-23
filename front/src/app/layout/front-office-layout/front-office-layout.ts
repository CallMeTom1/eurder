import { Component } from '@angular/core';
import { MatAnchor, MatButton } from '@angular/material/button';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSwitcher } from '@shared/ui/language-switcher/language-switcher';
import { Logo } from '@shared/ui/logo/logo';
import { ThemeToggle } from '@shared/ui/theme-toggle/theme-toggle';
import { UserMenu } from '@layout/user-menu/user-menu';

interface NavigationLink {
  path: string;
  labelKey: string;
}

@Component({
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
    UserMenu,
  ],
  selector: 'app-front-office-layout',
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
