import { Routes } from '@angular/router';
import { ComingSoon } from '@features/coming-soon/coming-soon';
import { NotFound } from '@features/not-found/not-found';
import { BackOfficeLayout } from '@layout/back-office-layout/back-office-layout';

export const adminRoutes: Routes = [
  {
    path: '',
    component: BackOfficeLayout,
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
      { path: 'shipping', component: ComingSoon, title: 'admin.nav.shipping' },
      { path: '**', component: NotFound, title: 'page.notFound' },
    ],
  },
];
