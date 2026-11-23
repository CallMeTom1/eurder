import { Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDivider } from '@angular/material/divider';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { Router, RouterLink } from '@angular/router';
import { SecurityService } from '@core/security/security.service';
import { BasketService } from '@features/basket/basket.service';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

@Component({
  imports: [
    MatButton,
    MatDivider,
    MatIcon,
    MatMenu,
    MatMenuItem,
    MatMenuTrigger,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-user-menu',
  templateUrl: './user-menu.html',
})
export class UserMenu {
  protected readonly security = inject(SecurityService);
  private readonly router = inject(Router);
  private readonly basket = inject(BasketService);

  protected async logout(): Promise<void> {
    await firstValueFrom(this.security.logout());
    this.basket.clear();
    await this.router.navigateByUrl('/');
  }
}
