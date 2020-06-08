import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../model/user';
import {  catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Book } from '../model/book';
import { BookDTO } from '../model/bookDTO';

@Injectable()
export class BooksService{

    private _bookHistoryUrl = 'http://localhost:8080/books/';
    private _wishlistUrl = 'http://localhost:8080/wishlist/';
    private _getBookLoansUrl = 'http://localhost:8080/bookLoan/all/';

    constructor(private _http: HttpClient){ }

    getBookHistory(userId : String) : Observable<BookDTO[]>{
        return this._http.get<BookDTO[]>(this._bookHistoryUrl+ userId).pipe(
                    catchError(this.handleError));
    }

    getWishlist(userId : String) : Observable<BookDTO[]>{
        return this._http.get<BookDTO[]>(this._wishlistUrl+"/"+ userId).pipe(
                    catchError(this.handleError));
    }

    private handleError(err: HttpErrorResponse){
        console.log(err.message);
        return throwError(err.message);
    }
    
    getBookLoans(uId : String) : Observable<any[]>{
        return this._http.get<any[]>(this._getBookLoansUrl + uId).pipe(
                    catchError(this.handleError));
    }
}