export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: User;
}

export interface Category {
  id: number;
  name: string;
  color: string;
  keywords: string;
}

export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  description: string;
  date: string; // yyyy-MM-dd
  category: Pick<Category, 'id' | 'name' | 'color'> | null;
}

export interface TransactionRequest {
  amount: number;
  type: TransactionType;
  description: string;
  date: string;
  categoryId: number | null;
}

export interface CategoryTotal {
  categoryId: number | null;
  name: string;
  color: string;
  total: number;
}

export interface MonthlySummary {
  month: string;
  income: number;
  expenses: number;
  balance: number;
  expensesByCategory: CategoryTotal[];
}

/** Spring's ProblemDetail error body. */
export interface ApiProblem {
  status: number;
  detail?: string;
  errors?: Record<string, string>;
}
