import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environments';
import { map } from 'rxjs/operators';

export interface TokenResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class CallService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl + '/calls';

  getToken(conversationId: string): Observable<string> {
    return this.http.get<TokenResponse>(`${this.baseUrl}/${conversationId}/token`).pipe(
      map(res => res.token)
    );
  }
}
