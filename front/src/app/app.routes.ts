import { Routes } from '@angular/router';
import { adminGuard, customerGuard, guestGuard } from '@core/security/guards';
import { Login } from '@features/account/login/login';
import { ComingSoon } from '@features/coming-soon/coming-soon';
import { NotFound } from '@features/not-found/not-found';
import { FrontOfficeLayout } from '@layout/front-office-layout/front-office-layout';
import { devOnlyRoutes } from './dev-only.routes';

export const routes: Routes = [
  ...devOnlyRoutes,
  {
    path: 'admin',
    canMatch: [adminGuard],
    loadChildren: () => import('@features/admin/admin.routes').then((m) => m.adminRoutes),
  },
  {
    path: '',
    component: FrontOfficeLayout,
    children: [
      { path: '', redirectTo: 'catalog', pathMatch: 'full' },
      { path: 'catalog', component: ComingSoon, title: 'nav.catalog' },
      { path: 'basket', component: ComingSoon, title: 'nav.basket' },
      { path: 'login', canActivate: [guestGuard], component: Login, title: 'nav.login' },
      {
        path: 'register',
        canActivate: [guestGuard],
        component: ComingSoon,
        title: 'nav.register',
      },
      {
        path: 'my-orders',
        canActivate: [customerGuard],
        runGuardsAndResolvers: 'always',
        component: ComingSoon,
        title: 'nav.myOrders',
      },
      { path: '**', component: NotFound, title: 'page.notFound' },
    ],
  },
];
