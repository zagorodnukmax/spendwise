import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { TokenResponse, User } from './models';

const STORAGE_KEY = 'spendwise.session';

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(readSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly token = computed(() => this.session()?.token ?? null);

  isAuthenticated(): boolean {
    const s = this.session();
    return !!s && new Date(s.expiresAt).getTime() > Date.now();
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/auth/login', { email, password })
      .pipe(tap((res) => this.store(res)));
  }

  register(email: string, password: string, displayName: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/auth/register', { email, password, displayName })
      .pipe(tap((res) => this.store(res)));
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    this.router.navigateByUrl('/login');
  }

  private store(res: TokenResponse): void {
    const s: Session = { token: res.accessToken, expiresAt: res.expiresAt, user: res.user };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
    this.session.set(s);
  }
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const session = raw ? (JSON.parse(raw) as Session) : null;
    // An expired token is not a session: drop it so the UI never shows a "logged in" header on /login.
    if (session && !(new Date(session.expiresAt).getTime() > Date.now())) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}
