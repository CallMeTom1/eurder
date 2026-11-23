import { Routes } from '@angular/router';
import { providePaginatorTranslations } from '@core/i18n/paginator-intl';
import { NotFound } from '@features/not-found/not-found';
import { BackOfficeLayout } from '@layout/back-office-layout/back-office-layout';

export const adminRoutes: Routes = [
  {
    path: '',
    component: BackOfficeLayout,
    providers: [providePaginatorTranslations()],
    children: [
      { path: '', redirectTo: 'items', pathMatch: 'full' },
      {
        path: 'items',
        loadComponent: () =>
          import('./items/admin-items-page/admin-items-page').then((m) => m.AdminItemsPage),
        title: 'admin.nav.items',
      },
      {
        path: 'items/new',
        loadComponent: () =>
          import('./items/item-create-page/item-create-page').then((m) => m.ItemCreatePage),
        title: 'admin.items.create',
      },
      {
        path: 'items/:id',
        loadComponent: () =>
          import('@features/catalog/catalog-item-page/catalog-item-page').then(
            (m) => m.CatalogItemPage,
          ),
        data: { back: '/admin/items' },
        title: 'catalog.item',
      },
      {
        path: 'items/:id/edit',
        loadComponent: () =>
          import('./items/item-edit-page/item-edit-page').then((m) => m.ItemEditPage),
        title: 'admin.items.editTitle',
      },
      {
        path: 'customers',
        loadComponent: () =>
          import('./customers/customers-page/customers-page').then((m) => m.CustomersPage),
        title: 'admin.nav.customers',
      },
      {
        path: 'customers/new',
        loadComponent: () =>
          import('./customers/customer-create-page/customer-create-page').then(
            (m) => m.CustomerCreatePage,
          ),
        title: 'admin.customers.new',
      },
      {
        path: 'customers/:id',
        loadComponent: () =>
          import('./customers/customer-detail-page/customer-detail-page').then(
            (m) => m.CustomerDetailPage,
          ),
        title: 'admin.customers.title',
      },
      {
        path: 'customers/:id/edit',
        loadComponent: () =>
          import('./customers/customer-edit-page/customer-edit-page').then(
            (m) => m.CustomerEditPage,
          ),
        title: 'admin.customers.editTitle',
      },
      {
        path: 'shipping',
        loadComponent: () => import('./shipping-page/shipping-page').then((m) => m.ShippingPage),
        title: 'admin.nav.shipping',
      },
      { path: '**', component: NotFound, title: 'page.notFound' },
    ],
  },
];
