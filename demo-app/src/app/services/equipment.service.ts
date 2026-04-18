import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateEquipmentDto, Equipment } from '../models/equipment.model';

const API_URL = 'http://localhost:8080/equipment';

@Injectable({ providedIn: 'root' })
export class EquipmentService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(API_URL);
  }

  create(dto: CreateEquipmentDto): Observable<Equipment> {
    return this.http.post<Equipment>(API_URL, dto);
  }

  update(id: string, dto: CreateEquipmentDto): Observable<Equipment> {
    return this.http.put<Equipment>(`${API_URL}/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${id}`);
  }
}
