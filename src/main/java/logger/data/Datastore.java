package logger.data;
import logger.pojo.Log;
import java.util.concurrent.TimeoutException;
public interface Datastore {
    void addLog(Log log);
    void appendLog() throws TimeoutException;
    void clearFile();
    void close();
}
