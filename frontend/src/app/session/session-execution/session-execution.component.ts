import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';

import { ApiProblem, Session, SessionSet, SessionSummary } from '../session.models';
import { SessionService } from '../session.service';

type ViewState = 'loading' | 'empty' | 'ready' | 'done' | 'error';

/**
 * Écran d'exécution d'une séance : un onglet par exercice, le tableau des séries
 * (prévu / réel), un minuteur de repos après chaque série validée et la clôture.
 *
 * Sans `sessionId`, affiche la séance en cours (ou propose d'en lancer une).
 * Avec le routeur, `withComponentInputBinding()` renseigne `sessionId` depuis `:sessionId`.
 */
@Component({
  selector: 'app-session-execution',
  templateUrl: './session-execution.component.html',
  styleUrl: './session-execution.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionExecutionComponent implements OnInit {
  private readonly sessionService = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);

  readonly sessionId = input<number | undefined, unknown>(undefined, {
    transform: (value: unknown) => (value == null || value === '' ? undefined : Number(value)),
  });

  readonly state = signal<ViewState>('loading');
  readonly session = signal<Session | null>(null);
  readonly summary = signal<SessionSummary | null>(null);
  readonly error = signal<string | null>(null);
  readonly activeIndex = signal(0);
  readonly showAnimation = signal(false);
  readonly savingSetIds = signal<ReadonlySet<number>>(new Set());
  readonly completing = signal(false);
  readonly restRemaining = signal<number | null>(null);

  readonly activeExercise = computed(() => this.session()?.exercises[this.activeIndex()] ?? null);
  readonly isLastExercise = computed(() => {
    const session = this.session();
    return !!session && this.activeIndex() === session.exercises.length - 1;
  });
  readonly progressPercent = computed(() => {
    const session = this.session();
    return session && session.totalSets > 0
      ? Math.round((session.completedSets / session.totalSets) * 100)
      : 0;
  });
  readonly allSetsDone = computed(() => {
    const session = this.session();
    return !!session && session.completedSets === session.totalSets;
  });

  private restTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => this.stopRest());
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.error.set(null);
    const id = this.sessionId();
    const request$ = id != null ? this.sessionService.get(id) : this.sessionService.current();
    request$.subscribe({
      next: (session) => (session ? this.showSession(session) : this.state.set('empty')),
      error: (err) => this.fail(err),
    });
  }

  startSession(): void {
    this.state.set('loading');
    this.sessionService.start().subscribe({
      next: (session) => this.showSession(session),
      error: (err: HttpErrorResponse) =>
        this.problemCode(err) === 'SESSION_ALREADY_IN_PROGRESS' ? this.load() : this.fail(err),
    });
  }

  selectExercise(index: number): void {
    this.activeIndex.set(index);
    this.showAnimation.set(false);
  }

  nextExercise(): void {
    const session = this.session();
    if (session && this.activeIndex() < session.exercises.length - 1) {
      this.selectExercise(this.activeIndex() + 1);
    }
  }

  toggleAnimation(): void {
    this.showAnimation.update((shown) => !shown);
  }

  onRepsInput(set: SessionSet, event: Event): void {
    this.patchSet(set.id, { actualReps: this.readNumber(event, true) });
  }

  onWeightInput(set: SessionSet, event: Event): void {
    this.patchSet(set.id, { actualWeightKg: this.readNumber(event, false) });
  }

  /** Une série déjà validée qu'on corrige est réenregistrée en quittant le champ. */
  onFieldBlur(set: SessionSet): void {
    const current = this.findSet(set.id);
    if (current?.completed) {
      this.saveSet(current, current);
    }
  }

  toggleSet(set: SessionSet): void {
    const previous = this.findSet(set.id);
    if (!previous) {
      return;
    }
    const completed = !previous.completed;
    // Valider sans rien saisir = objectif atteint : on reprend les valeurs prévues.
    const next: SessionSet = {
      ...previous,
      completed,
      actualReps: completed ? (previous.actualReps ?? previous.targetReps) : previous.actualReps,
      actualWeightKg: completed
        ? (previous.actualWeightKg ?? previous.targetWeightKg)
        : previous.actualWeightKg,
    };
    this.patchSet(set.id, next);
    this.saveSet(next, previous);
    if (completed) {
      this.startRest(this.activeExercise()?.restSeconds ?? 0);
    }
  }

  finish(): void {
    const session = this.session();
    if (!session || this.completing()) {
      return;
    }
    if (!this.allSetsDone() && !confirm('Certaines séries ne sont pas validées. Terminer quand même ?')) {
      return;
    }
    this.completing.set(true);
    this.stopRest();
    this.sessionService.complete(session.id).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.state.set('done');
        this.completing.set(false);
      },
      error: (err) => {
        this.completing.set(false);
        this.error.set(this.describe(err));
      },
    });
  }

  skipRest(): void {
    this.stopRest();
  }

  isSaving(setId: number): boolean {
    return this.savingSetIds().has(setId);
  }

  formatDuration(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const mmss = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    return hours > 0 ? `${hours}:${mmss}` : mmss;
  }

  private showSession(session: Session): void {
    this.session.set(session);
    this.state.set(session.status === 'IN_PROGRESS' ? 'ready' : 'done');
    // On ouvre le premier exercice qui n'est pas terminé.
    const firstOpen = session.exercises.findIndex((exercise) => !exercise.completed);
    this.activeIndex.set(firstOpen === -1 ? 0 : firstOpen);
  }

  private saveSet(set: SessionSet, rollback: SessionSet): void {
    const session = this.session();
    if (!session) {
      return;
    }
    this.error.set(null);
    this.savingSetIds.update((ids) => new Set(ids).add(set.id));
    this.sessionService
      .updateSet(session.id, set.id, {
        actualReps: set.actualReps,
        actualWeightKg: set.actualWeightKg,
        completed: set.completed,
      })
      .subscribe({
        next: (saved) => {
          this.patchSet(saved.id, saved);
          this.markSaved(set.id);
        },
        error: (err) => {
          this.patchSet(rollback.id, rollback);
          this.markSaved(set.id);
          this.error.set(this.describe(err));
        },
      });
  }

  private markSaved(setId: number): void {
    this.savingSetIds.update((ids) => {
      const copy = new Set(ids);
      copy.delete(setId);
      return copy;
    });
  }

  /** Remplace une série et recalcule localement l'état des exercices et la progression. */
  private patchSet(setId: number, patch: Partial<SessionSet>): void {
    this.session.update((session) => {
      if (!session) {
        return session;
      }
      const exercises = session.exercises.map((exercise) => {
        if (!exercise.sets.some((s) => s.id === setId)) {
          return exercise;
        }
        const sets = exercise.sets.map((s) => (s.id === setId ? { ...s, ...patch } : s));
        return { ...exercise, sets, completed: sets.every((s) => s.completed) };
      });
      const completedSets = exercises.reduce(
        (count, exercise) => count + exercise.sets.filter((s) => s.completed).length,
        0,
      );
      return { ...session, exercises, completedSets };
    });
  }

  private findSet(setId: number): SessionSet | undefined {
    return this.session()
      ?.exercises.flatMap((exercise) => exercise.sets)
      .find((s) => s.id === setId);
  }

  private startRest(seconds: number): void {
    this.stopRest();
    if (seconds <= 0) {
      return;
    }
    this.restRemaining.set(seconds);
    this.restTimer = setInterval(() => {
      const remaining = (this.restRemaining() ?? 0) - 1;
      if (remaining <= 0) {
        this.stopRest();
        navigator.vibrate?.(300);
      } else {
        this.restRemaining.set(remaining);
      }
    }, 1000);
  }

  private stopRest(): void {
    if (this.restTimer) {
      clearInterval(this.restTimer);
      this.restTimer = null;
    }
    this.restRemaining.set(null);
  }

  private readNumber(event: Event, integer: boolean): number | null {
    const raw = (event.target as HTMLInputElement).value.trim().replace(',', '.');
    if (raw === '') {
      return null;
    }
    const value = integer ? Number.parseInt(raw, 10) : Number.parseFloat(raw);
    return Number.isFinite(value) && value >= 0 ? value : null;
  }

  private fail(err: unknown): void {
    this.error.set(this.describe(err));
    this.state.set('error');
  }

  private problemCode(err: unknown): string | undefined {
    return err instanceof HttpErrorResponse ? (err.error as ApiProblem | null)?.code : undefined;
  }

  private describe(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      switch (this.problemCode(err)) {
        case 'NO_ACTIVE_ROUTINE':
          return "Aucun programme actif : ton coach doit t'en assigner un.";
        case 'SESSION_NOT_IN_PROGRESS':
          return 'Cette séance est déjà terminée.';
      }
      if (err.status === 0) {
        return 'Serveur injoignable. Vérifie ta connexion.';
      }
      const problem = err.error as ApiProblem | null;
      return problem?.detail ?? problem?.title ?? `Erreur ${err.status}`;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
