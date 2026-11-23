import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { TokenResponse } from './models';

const response = (expiresInMs: number): TokenResponse => ({
  accessToken: 'jwt-token',
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + expiresInMs).toISOString(),
  user: { id: 1, email: 'anna@example.com', displayName: 'Anna' },
});

describe('AuthService', () => {
  let http: HttpTestingController;

  function setup(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('is not authenticated without a stored session', () => {
    const auth = setup();
    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.user()).toBeNull();
  });

  it('login posts credentials, stores the token and exposes the user', () => {
    const auth = setup();
    auth.login('anna@example.com', 'secret123').subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'anna@example.com', password: 'secret123' });
    req.flush(response(60_000));

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.token()).toBe('jwt-token');
    expect(auth.user()?.displayName).toBe('Anna');
    expect(localStorage.getItem('spendwise.session')).toContain('jwt-token');
  });

  it('register sends display name and logs the user in', () => {
    const auth = setup();
    auth.register('new@example.com', 'secret123', 'New').subscribe();

    const req = http.expectOne('/api/auth/register');
    expect(req.request.body).toEqual({ email: 'new@example.com', password: 'secret123', displayName: 'New' });
    req.flush(response(60_000));

    expect(auth.isAuthenticated()).toBe(true);
  });

  it('restores the session from localStorage on startup', () => {
    localStorage.setItem(
      'spendwise.session',
      JSON.stringify({ token: 'saved', expiresAt: new Date(Date.now() + 60_000).toISOString(), user: { id: 1 } }),
    );
    const auth = setup();
    expect(auth.token()).toBe('saved');
    expect(auth.isAuthenticated()).toBe(true);
  });

  it('treats an expired token as logged out', () => {
    localStorage.setItem(
      'spendwise.session',
      JSON.stringify({ token: 'old', expiresAt: new Date(Date.now() - 1000).toISOString(), user: { id: 1 } }),
    );
    expect(setup().isAuthenticated()).toBe(false);
  });

  it('ignores corrupted localStorage data', () => {
    localStorage.setItem('spendwise.session', '{not json');
    expect(setup().isAuthenticated()).toBe(false);
  });

  it('logout clears the session and navigates to /login', () => {
    const auth = setup();
    auth.login('a@b.c', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush(response(60_000));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    auth.logout();

    expect(auth.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('spendwise.session')).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
