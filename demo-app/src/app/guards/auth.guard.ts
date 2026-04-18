import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { LoginStore } from '../features/login/login.store';

export const authGuard: CanActivateFn = () => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  return loginStore.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  if (!loginStore.isAuthenticated()) return true;
  return loginStore.role() === 'ADMIN'
    ? router.createUrlTree(['/people'])
    : router.createUrlTree(['/student']);
};

export const adminGuard: CanActivateFn = () => {
  const loginStore = inject(LoginStore);
  const router = inject(Router);

  if (!loginStore.isAuthenticated()) return router.createUrlTree(['/login']);
  return loginStore.role() === 'ADMIN' ? true : router.createUrlTree(['/student']);
};
