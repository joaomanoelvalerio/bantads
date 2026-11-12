import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

export interface ZipCodeAddress {
  street: string;
  city: string;
  state: string;
}

interface ViaCepResponse {
  logradouro?: string;
  localidade?: string;
  uf?: string;
  erro?: boolean | string;
}

const VIACEP_URL = 'https://viacep.com.br/ws';

@Injectable({ providedIn: 'root' })
export class ZipCodeService {
  private readonly http = inject(HttpClient);

  lookup(zipCode: string): Observable<ZipCodeAddress | null> {
    return this.http
      .get<ViaCepResponse>(`${VIACEP_URL}/${zipCode}/json/`)
      .pipe(map(toAddress));
  }
}

function toAddress(response: ViaCepResponse): ZipCodeAddress | null {
  if (response.erro) {
    return null;
  }

  return {
    street: response.logradouro?.trim() ?? '',
    city: response.localidade?.trim() ?? '',
    state: response.uf?.trim().toUpperCase() ?? '',
  };
}
