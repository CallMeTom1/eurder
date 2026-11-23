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
      { path: 'items', component: ComingSoon, title: 'admin.nav.items' },
      { path: 'customers', component: ComingSoon, title: 'admin.nav.customers' },
      { path: 'shipping', component: ComingSoon, title: 'admin.nav.shipping' },
      { path: '**', component: NotFound, title: 'page.notFound' },
    ],
  },
];
