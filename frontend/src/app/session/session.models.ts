// Miroir des DTO du module backend /api/sessions (voir backend/README.md).

export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABORTED';

export interface ExerciseInfo {
  id: number;
  name: string;
  bodyPart: string | null;
  target: string | null;
  equipment: string | null;
  imageUrl: string | null;
  animationUrl: string | null;
}

export interface SessionSet {
  id: number;
  setNumber: number;
  targetReps: number;
  targetWeightKg: number | null;
  actualReps: number | null;
  actualWeightKg: number | null;
  completed: boolean;
  completedAt: string | null;
}

export interface SessionExercise {
  id: number;
  position: number;
  exercise: ExerciseInfo;
  restSeconds: number;
  coachTip: string | null;
  completed: boolean;
  sets: SessionSet[];
}

export interface Session {
  id: number;
  workoutName: string;
  status: SessionStatus;
  startedAt: string;
  endedAt: string | null;
  totalSets: number;
  completedSets: number;
  exercises: SessionExercise[];
}

export interface SessionSummary {
  id: number;
  workoutName: string;
  status: SessionStatus;
  startedAt: string;
  endedAt: string;
  durationSeconds: number;
  totalSets: number;
  completedSets: number;
}

export interface NextWorkout {
  routineId: number;
  routineName: string;
  routineWorkoutId: number;
  workoutName: string;
  position: number;
  workoutCount: number;
  exerciseCount: number;
  inProgressSessionId: number | null;
}

export interface UpdateSetRequest {
  actualReps: number | null;
  actualWeightKg: number | null;
  completed: boolean;
}

/** Corps RFC 9457 renvoyé par le backend, avec son champ `code` stable. */
export interface ApiProblem {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
}
