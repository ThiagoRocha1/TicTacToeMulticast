import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MatchService extends Remote {
    byte[] getOponent(byte[] requestJson) throws RemoteException;
}
