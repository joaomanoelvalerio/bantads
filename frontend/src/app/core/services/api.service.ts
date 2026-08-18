import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  get<T>(caminho: string): Observable<T> {
    return this.http.get<T>(this.enderecoDe(caminho));
  }

  post<T>(caminho: string, corpo: unknown): Observable<T> {
    return this.http.post<T>(this.enderecoDe(caminho), corpo);
  }

  put<T>(caminho: string, corpo: unknown): Observable<T> {
    return this.http.put<T>(this.enderecoDe(caminho), corpo);
  }

  delete<T>(caminho: string): Observable<T> {
    return this.http.delete<T>(this.enderecoDe(caminho));
  }

  private enderecoDe(caminho: string): string {
    return `${environment.apiUrl}${caminho}`;
  }
}
