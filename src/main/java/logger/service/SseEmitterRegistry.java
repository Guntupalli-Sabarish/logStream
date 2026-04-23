package logger.service;

import logger.pojo.Log;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    /** Creates, registers, and returns a new SSE emitter for a connected client. */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e       -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * Broadcasts a new log to all connected clients.
     * Dead emitters (IOException) are removed immediately.
     * Called on the producer thread — kept fast (no blocking I/O).
     */
    public void broadcast(Log log) {
        if (emitters.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(log));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    public int activeConnections() {
        return emitters.size();
    }

}
