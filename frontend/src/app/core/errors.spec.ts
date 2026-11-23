import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './errors';

describe('errorMessage', () => {
  it('explains that the backend is down on status 0', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('not reachable');
  });

  it('uses ProblemDetail.detail', () => {
    const err = new HttpErrorResponse({ status: 409, error: { status: 409, detail: 'Email is already registered' } });
    expect(errorMessage(err)).toBe('Email is already registered');
  });

  it('joins validation field errors', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: { status: 400, detail: 'Validation failed', errors: { email: 'must be valid', password: 'too short' } },
    });
    expect(errorMessage(err)).toBe('email: must be valid; password: too short');
  });

  it('falls back for non-HTTP errors', () => {
    expect(errorMessage(new Error('boom'))).toBe('Unexpected error');
  });
});
