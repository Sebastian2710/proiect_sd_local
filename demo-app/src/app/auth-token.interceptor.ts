import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoginStore } from './features/login/login.store';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const loginStore = inject(LoginStore);
  const token = loginStore.token();

  if (!token || request.url.endsWith('/login')) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
