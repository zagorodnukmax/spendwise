import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/errors';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly mode = signal<'login' | 'register'>('login');
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['demo@spendwise.dev', [Validators.required, Validators.email]],
    password: ['demo12345', [Validators.required, Validators.minLength(8)]],
    displayName: [''],
  });

  protected toggleMode(): void {
    this.mode.update((m) => (m === 'login' ? 'register' : 'login'));
    this.error.set(null);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password, displayName } = this.form.getRawValue();
    if (this.mode() === 'register' && !displayName.trim()) {
      this.error.set('Please enter your name');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const request$ =
      this.mode() === 'login'
        ? this.auth.login(email, password)
        : this.auth.register(email, password, displayName);

    request$.subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
