package root.integrat;

public interface IServer {
  public ServerResponse login(String userName, String mdPass);
  public ServerResponse logout(long id);
  public ServerResponse withdraw(long id, double balance);
  public ServerResponse deposit(long id, double balance);
  public ServerResponse getBalance(long id);
}
