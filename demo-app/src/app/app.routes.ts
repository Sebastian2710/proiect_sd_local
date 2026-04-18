import { Routes } from '@angular/router';
import { authGuard, guestGuard, adminGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'people',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/person-list/person-list-page.component').then(
        (m) => m.PersonListPageComponent,
      ),
  },
  {
    path: 'equipment',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/equipment/equipment-page.component').then(
        // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-return
        (m) => m.EquipmentPageComponent,
      ),
  },
  {
    path: 'loans',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/loan-record/loan-record-page.component').then(
        // eslint-disable-next-line @typescript-eslint/no-unsafe-return,@typescript-eslint/no-unsafe-member-access
        (m) => m.LoanRecordPageComponent,
      ),
  },
  {
    path: 'student',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/student-dashboard/student-dashboard-page.component').then(
        // eslint-disable-next-line @typescript-eslint/no-unsafe-return,@typescript-eslint/no-unsafe-member-access
        (m) => m.StudentDashboardPageComponent,
      ),
  },
  {
    path: 'error',
    loadComponent: () =>
      import('./features/not-found/not-found-page.component').then(
        (m) => m.NotFoundPageComponent,
      ),
  },
  {
    path: '**',
    redirectTo: 'error',
  },
];
