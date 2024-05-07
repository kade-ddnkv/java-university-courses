package root.integrat;

public class ServerResponse {
  public static final int SUCCESS = 0;
  public static final int UNDEFINED_ERROR = 1;
  public static final int ALREADY_LOGGED = 2;
  public static final int NOT_LOGGED = 3;
  public static final int NO_USER_INCORRECT_PASSWORD = 4;
  public static final int NO_MONEY = 5;
  public int code;
  public Object response;
  public ServerResponse(int code, Object obj) {
    this.code = code;
    this.response = obj;
  }
}
