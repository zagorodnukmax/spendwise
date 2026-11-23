import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { Category } from '../../core/models';

@Component({
  selector: 'app-categories',
  imports: [ReactiveFormsModule],
  templateUrl: './categories.html',
  styleUrl: './categories.css',
})
export class Categories {
  private readonly api = inject(ApiService);

  protected readonly categories = signal<Category[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(40)]],
    color: ['#4f46e5', Validators.required],
    keywords: ['', Validators.maxLength(500)],
  });

  constructor() {
    this.load();
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.form.getRawValue();
    const id = this.editingId();
    (id ? this.api.updateCategory(id, body) : this.api.createCategory(body)).subscribe({
      next: () => {
        this.reset();
        this.load();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected edit(c: Category): void {
    this.editingId.set(c.id);
    this.form.setValue({ name: c.name, color: c.color, keywords: c.keywords ?? '' });
  }

  protected remove(c: Category): void {
    if (!confirm(`Delete category "${c.name}"? Its transactions become uncategorized.`)) return;
    this.api.deleteCategory(c.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected reset(): void {
    this.editingId.set(null);
    this.error.set(null);
    this.form.reset();
  }

  protected keywordList(c: Category): string[] {
    return (c.keywords ?? '')
      .split(',')
      .map((k) => k.trim())
      .filter(Boolean);
  }

  private load(): void {
    this.api.categories().subscribe({
      next: (c) => this.categories.set(c),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
