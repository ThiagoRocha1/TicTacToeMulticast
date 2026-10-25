import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MatchService extends Remote {
    String getOponent(String requestJson) throws RemoteException;
}
