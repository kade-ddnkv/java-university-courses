package root.integrat;

public class AccountManagerResponse {
  public static int 
      SUCCEED = 0,
      ALREADY_LOGGED = 1, 
      NOT_LOGGED = 2,
      NO_USER_INCORRECT_PASSWORD = 3,
      INCORRECT_RESPONSE = 4,
      UNDEFINED_ERROR = 5, 
      INCORRECT_SESSION = 6,
      NO_MONEY = 7;
  
  public static final AccountManagerResponse ACCOUNT_MANAGER_RESPONSE =
      new AccountManagerResponse(ALREADY_LOGGED, null);
  public static final AccountManagerResponse NO_USER_INCORRECT_PASSWORD_RESPONSE =
      new AccountManagerResponse(NO_USER_INCORRECT_PASSWORD, null);
  public static final AccountManagerResponse UNDEFINED_ERROR_RESPONSE =
      new AccountManagerResponse(UNDEFINED_ERROR, null);
  public static final AccountManagerResponse NOT_LOGGED_RESPONSE =
      new AccountManagerResponse(NOT_LOGGED, null);
  public static final AccountManagerResponse INCORRECT_SESSION_RESPONSE =
      new AccountManagerResponse(INCORRECT_SESSION, null);
  public static final AccountManagerResponse SUCCEED_RESPONSE =
      new AccountManagerResponse(SUCCEED, null);
  public static final AccountManagerResponse NO_MONEY_RESPONSE =
      new AccountManagerResponse(NO_MONEY, null);
  
  public int code;
  public Object response;
  public AccountManagerResponse(int code, Object obj) {
    this.code = code;
    this.response = obj;
  }
}
