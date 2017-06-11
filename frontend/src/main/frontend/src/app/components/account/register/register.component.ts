import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {AccountService} from "../../../services/account.service";

@Component({
  templateUrl: './register.component.html',
  styleUrls: ['../account.component.css']
})

export class RegisterComponent implements OnInit {

  model: any = {};
  loading = false;
  errMsg: string;

  constructor(private router: Router,
              private accountService: AccountService) {
  }

  ngOnInit(): void {
    this.initBackGround();
  }

  initBackGround() {
    //https://stackoverflow.com/questions/34636661/how-do-i-change-the-body-class-via-a-typescript-class-angular2
    //any better idea to set bg to element body?
    let body = document.getElementsByTagName('body')[0];
    body.classList.remove('reg_bg');
    body.classList.remove('log_bg');
    body.classList.add('reg_bg');
  }

  register() {
    this.loading = true;
    console.log(this.model);
    this.accountService.create(this.model)
      .subscribe(
        data => {
          console.log(data);
          this.router.navigate(['/account/login']);
        },
        error => {
          this.loading = false;
          this.errMsg = error._body ? JSON.parse(error._body).errMsg : "注册失败";
          console.log(error)
        }
      );
  }
}
