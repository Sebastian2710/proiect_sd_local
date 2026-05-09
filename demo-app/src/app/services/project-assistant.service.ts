import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecommendationRequestDto,
  RecommendationSession,
  RecommendationSubmitRequestDto,
} from '../models/recommendation.model';

const API_URL = 'http://localhost:8080/assistant';

@Injectable({ providedIn: 'root' })
export class ProjectAssistantService {
  private readonly http = inject(HttpClient);

  recommend(dto: RecommendationRequestDto): Observable<RecommendationSession> {
    return this.http.post<RecommendationSession>(`${API_URL}/recommend`, dto);
  }

  getSession(sessionId: string): Observable<RecommendationSession> {
    return this.http.get<RecommendationSession>(`${API_URL}/recommend/${sessionId}`);
  }

  submit(
    sessionId: string,
    dto: RecommendationSubmitRequestDto,
  ): Observable<RecommendationSession> {
    return this.http.post<RecommendationSession>(
      `${API_URL}/recommend/${sessionId}/submit`,
      dto,
    );
  }
}
