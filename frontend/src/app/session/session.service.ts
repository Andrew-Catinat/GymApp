import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable, InjectionToken } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  NextWorkout,
  Session,
  SessionSet,
  SessionSummary,
  UpdateSetRequest,
} from './session.models';

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  factory: () => '/api',
});

/**
 * Provisoire : le backend lit l'utilisateur courant dans l'en-tête X-User-Id
 * en attendant le JWT. À supprimer quand l'intercepteur d'authentification existera.
 */
export const DEV_USER_ID = new InjectionToken<string | null>('DEV_USER_ID', {
  factory: () => '1',
});

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/sessions`;
  private readonly devUserId = inject(DEV_USER_ID);

  next(): Observable<NextWorkout> {
    return this.http.get<NextWorkout>(`${this.baseUrl}/next`, this.options());
  }

  /** Sans `routineWorkoutId`, le backend lance la prochaine séance de la rotation. */
  start(routineWorkoutId?: number): Observable<Session> {
    const body = routineWorkoutId != null ? { routineWorkoutId } : {};
    return this.http.post<Session>(`${this.baseUrl}/start`, body, this.options());
  }

  /** Séance en cours, ou `null` si le backend répond 204. */
  current(): Observable<Session | null> {
    return this.http
      .get<Session>(`${this.baseUrl}/current`, { ...this.options(), observe: 'response' })
      .pipe(map((response) => (response.status === 204 ? null : response.body)));
  }

  get(sessionId: number): Observable<Session> {
    return this.http.get<Session>(`${this.baseUrl}/${sessionId}`, this.options());
  }

  updateSet(sessionId: number, setId: number, request: UpdateSetRequest): Observable<SessionSet> {
    return this.http.put<SessionSet>(
      `${this.baseUrl}/${sessionId}/sets/${setId}`,
      request,
      this.options(),
    );
  }

  complete(sessionId: number): Observable<SessionSummary> {
    return this.http.post<SessionSummary>(`${this.baseUrl}/${sessionId}/complete`, null, this.options());
  }

  private options(): { headers?: HttpHeaders } {
    return this.devUserId ? { headers: new HttpHeaders({ 'X-User-Id': this.devUserId }) } : {};
  }
}
