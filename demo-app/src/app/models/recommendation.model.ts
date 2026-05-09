import { LoanRecord } from './loan-record.model';
import { PersonSummary, EquipmentSummary } from './loan-record.model';

export type AvailabilityStatus = 'AVAILABLE' | 'INSUFFICIENT_STOCK' | 'OUT_OF_STOCK';
export type RecommendationSessionStatus = 'DRAFT' | 'SUBMITTED' | 'EXPIRED';

export interface RecommendedItem {
  id: string;
  /** Null when the LLM produced a name the validator chain couldn't resolve. */
  equipment: EquipmentSummary | null;
  originalLlmName: string;
  quantity: number;
  reason: string;
  availabilityStatus: AvailabilityStatus | null;
}

export interface RecommendationSession {
  id: string;
  person: PersonSummary;
  originalDescription: string;
  projectPlan: string;
  status: RecommendationSessionStatus;
  createdAt: string;          // ISO instant
  expectedReturnDate: string; // ISO date (yyyy-MM-dd)
  loanRecord: LoanRecord | null; // populated only after submission
  items: RecommendedItem[];
}

export interface RecommendationRequestDto {
  description: string;
  expectedReturnDate: string; // yyyy-MM-dd
}

export interface SubmitItemDto {
  recommendedItemId: string;
  quantity: number;
}

export interface RecommendationSubmitRequestDto {
  items: SubmitItemDto[];
  expectedReturnDate: string | null; // null → server reuses session's date
}
