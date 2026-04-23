package logger.data;
import logger.pojo.Log;
public interface Datastore {
    void addLog(Log log);
    void clearFile();
    void close();
}
