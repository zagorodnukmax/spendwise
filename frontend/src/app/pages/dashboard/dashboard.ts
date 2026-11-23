import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, forkJoin, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import {
  Category,
  MonthlySummary,
  Transaction,
  TransactionRequest,
  TransactionType,
} from '../../core/models';

const today = () => new Date().toISOString().slice(0, 10);

@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly api = inject(ApiService);

  protected readonly month = signal(today().slice(0, 7));
  protected readonly summary = signal<MonthlySummary | null>(null);
  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly suggestion = signal<Category | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly maxCategoryTotal = computed(() =>
    Math.max(1, ...(this.summary()?.expensesByCategory.map((c) => c.total) ?? [])),
  );

  protected readonly form = inject(FormBuilder).group({
    type: ['EXPENSE' as TransactionType],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    description: ['', [Validators.required, Validators.maxLength(200)]],
    date: [today(), Validators.required],
    categoryId: [null as number | null],
  });

  constructor() {
    this.api.categories().subscribe((c) => this.categories.set(c));

    // Reload whenever the selected month changes.
    effect(() => this.load(this.month()));

    // Live "smart category" hint while typing a description.
    this.form.controls.description.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((text) =>
          text && text.trim().length > 2
            ? this.api.suggestCategory(text).pipe(catchError(() => of(null)))
            : of(null),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((c) => this.suggestion.set(c));
  }

  protected changeMonth(delta: number): void {
    const [y, m] = this.month().split('-').map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    this.month.set(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: TransactionRequest = {
      type: v.type!,
      amount: Number(v.amount),
      description: v.description!,
      date: v.date!,
      categoryId: v.categoryId ?? null,
    };
    const id = this.editingId();
    this.saving.set(true);
    (id ? this.api.updateTransaction(id, body) : this.api.createTransaction(body)).subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.load(this.month());
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  protected edit(t: Transaction): void {
    this.editingId.set(t.id);
    this.form.setValue({
      type: t.type,
      amount: t.amount,
      description: t.description,
      date: t.date,
      categoryId: t.category?.id ?? null,
    });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.suggestion.set(null);
    this.form.reset({ type: 'EXPENSE', amount: null, description: '', date: today(), categoryId: null });
  }

  protected remove(t: Transaction): void {
    if (!confirm(`Delete "${t.description}"?`)) return;
    this.api.deleteTransaction(t.id).subscribe({
      next: () => this.load(this.month()),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected barWidth(total: number): string {
    return `${(total / this.maxCategoryTotal()) * 100}%`;
  }

  private load(month: string): void {
    this.error.set(null);
    forkJoin({ summary: this.api.summary(month), list: this.api.transactions(month) }).subscribe({
      next: ({ summary, list }) => {
        this.summary.set(summary);
        this.transactions.set(list);
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
