import { Routes } from '@angular/router';
import { adminGuard, customerGuard, guestGuard } from '@core/security/guards';
import { CatalogPage } from '@features/catalog/catalog-page/catalog-page';
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
      { path: 'catalog', component: CatalogPage, title: 'nav.catalog' },
      {
        path: 'catalog/:id',
        loadComponent: () =>
          import('@features/catalog/catalog-item-page/catalog-item-page').then(
            (m) => m.CatalogItemPage,
          ),
        title: 'catalog.item',
      },
      {
        path: 'basket',
        loadComponent: () =>
          import('@features/basket/basket-page/basket-page').then((m) => m.BasketPage),
        title: 'nav.basket',
      },
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () => import('@features/account/login/login').then((m) => m.Login),
        title: 'nav.login',
      },
      {
        path: 'register',
        canActivate: [guestGuard],
        loadComponent: () => import('@features/account/register/register').then((m) => m.Register),
        title: 'nav.register',
      },
      {
        path: 'my-orders',
        canActivate: [customerGuard],
        runGuardsAndResolvers: 'always',
        loadComponent: () =>
          import('@features/eurder/my-orders-page/my-orders-page').then((m) => m.MyOrdersPage),
        title: 'nav.myOrders',
      },
      {
        path: '**',
        loadComponent: () => import('@features/not-found/not-found').then((m) => m.NotFound),
        title: 'page.notFound',
      },
    ],
  },
];
