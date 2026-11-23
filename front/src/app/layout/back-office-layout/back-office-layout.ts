import { Component } from '@angular/core';
import { MatDivider } from '@angular/material/divider';
import { MatListItem, MatNavList } from '@angular/material/list';
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
  selector: 'app-back-office-layout',
  imports: [
    LanguageSwitcher,
    Logo,
    MatDivider,
    MatListItem,
    MatNavList,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    ThemeToggle,
    TranslatePipe,
    UserMenu,
  ],
  templateUrl: './back-office-layout.html',
  host: { class: 'flex min-h-full flex-col' },
})
export class BackOfficeLayout {
  protected readonly links: NavigationLink[] = [
    { path: '/admin/items', labelKey: 'admin.nav.items' },
    { path: '/admin/customers', labelKey: 'admin.nav.customers' },
    { path: '/admin/shipping', labelKey: 'admin.nav.shipping' },
  ];
}
