import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoanRecord, LoanRecordCreateDto, LoanRecordUpdateDto } from '../models/loan-record.model';

const API_URL = 'http://localhost:8080/loan';

@Injectable({ providedIn: 'root' })
export class LoanRecordService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<LoanRecord[]> {
    return this.http.get<LoanRecord[]>(API_URL);
  }

  create(dto: LoanRecordCreateDto): Observable<LoanRecord> {
    return this.http.post<LoanRecord>(API_URL, dto);
  }

  update(id: string, dto: LoanRecordUpdateDto): Observable<LoanRecord> {
    return this.http.put<LoanRecord>(`${API_URL}/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${id}`);
  }
}
