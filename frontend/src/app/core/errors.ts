import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from './models';

export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'Backend is not reachable. Is it running on :8080?';
    const body = err.error as ApiProblem | null;
    if (body?.errors) {
      return Object.entries(body.errors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('; ');
    }
    return body?.detail ?? err.message;
  }
  return 'Unexpected error';
}
