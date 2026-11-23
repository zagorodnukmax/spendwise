import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';
import { Category } from './models';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('passes the month as a query parameter', () => {
    api.transactions('2026-11').subscribe();
    const req = http.expectOne((r) => r.url === '/api/transactions');
    expect(req.request.params.get('month')).toBe('2026-11');
    req.flush([]);
  });

  it('suggestCategory returns the category on 200', () => {
    const cat: Category = { id: 3, name: 'Transport', color: '#2563eb', keywords: 'uber' };
    let result: Category | null | undefined;
    api.suggestCategory('Uber home').subscribe((c) => (result = c));

    const req = http.expectOne((r) => r.url === '/api/categories/suggest');
    expect(req.request.params.get('description')).toBe('Uber home');
    req.flush(cat);
    expect(result).toEqual(cat);
  });

  it('suggestCategory returns null on 204 No Content', () => {
    let result: Category | null | undefined;
    api.suggestCategory('gift').subscribe((c) => (result = c));
    http.expectOne((r) => r.url === '/api/categories/suggest').flush(null, { status: 204, statusText: 'No Content' });
    expect(result).toBeNull();
  });
});
