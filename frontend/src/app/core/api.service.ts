import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Category, MonthlySummary, Transaction, TransactionRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Categories
  categories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }
  createCategory(body: Omit<Category, 'id'>): Observable<Category> {
    return this.http.post<Category>('/api/categories', body);
  }
  updateCategory(id: number, body: Omit<Category, 'id'>): Observable<Category> {
    return this.http.put<Category>(`/api/categories/${id}`, body);
  }
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`/api/categories/${id}`);
  }
  suggestCategory(description: string): Observable<Category | null> {
    return this.http
      .get<Category>('/api/categories/suggest', { params: { description }, observe: 'response' })
      .pipe(map((res: HttpResponse<Category>) => (res.status === 200 ? res.body : null)));
  }

  // Transactions
  transactions(month: string): Observable<Transaction[]> {
    return this.http.get<Transaction[]>('/api/transactions', { params: { month } });
  }
  summary(month: string): Observable<MonthlySummary> {
    return this.http.get<MonthlySummary>('/api/transactions/summary', { params: { month } });
  }
  createTransaction(body: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>('/api/transactions', body);
  }
  updateTransaction(id: number, body: TransactionRequest): Observable<Transaction> {
    return this.http.put<Transaction>(`/api/transactions/${id}`, body);
  }
  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`/api/transactions/${id}`);
  }
}
