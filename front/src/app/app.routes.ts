import { Routes } from '@angular/router';
import { devOnlyRoutes } from './dev-only.routes';

export const routes: Routes = [...devOnlyRoutes];
