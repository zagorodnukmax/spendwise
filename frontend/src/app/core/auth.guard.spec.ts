import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard, guestGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('route guards', () => {
  let loggedIn = false;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { isAuthenticated: () => loggedIn } }],
    });
  });

  const run = (guard: typeof authGuard) =>
    TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  const url = (result: unknown) => TestBed.inject(Router).serializeUrl(result as UrlTree);

  it('authGuard lets an authenticated user in', () => {
    loggedIn = true;
    expect(run(authGuard)).toBe(true);
  });

  it('authGuard redirects a guest to /login', () => {
    loggedIn = false;
    expect(url(run(authGuard))).toBe('/login');
  });

  it('guestGuard redirects a logged-in user away from /login', () => {
    loggedIn = true;
    expect(url(run(guestGuard))).toBe('/');
  });

  it('guestGuard lets a guest open /login', () => {
    loggedIn = false;
    expect(run(guestGuard)).toBe(true);
  });
});
