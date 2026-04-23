import { useState, useMemo } from 'react';
import axios from 'axios';
const fmt = (ts) => {
  if (!ts) return 'N/A';
  const d = new Date(ts);
  return isNaN(d) ? 'N/A' : `${d.toLocaleDateString()} ${d.toLocaleTimeString()}`;
};
const sevClass = (s) => (s ? s.toLowerCase() : 'low');
export default function TraceExplorer({ api, logs }) {
  const [input,   setInput]   = useState('');
  const [traceId, setTraceId] = useState('');
  const [remote,  setRemote]  = useState(null); 
  const [loading, setLoading] = useState(false);
  const search = async () => {
    const tid = input.trim();
    if (!tid) return;
    setTraceId(tid);
    setLoading(true);
    try {
      const r = await axios.get(`${api}/api/logs`, { params: { traceId: tid, limit: 500 } });
      setRemote(r.data);
    } catch { setRemote([]); }
    finally { setLoading(false); }
  };
  const events = useMemo(() => {
    if (remote !== null) return [...remote].sort((a, b) =>
      new Date(a.timestamp) - new Date(b.timestamp));
    if (!traceId) return [];
    return [...logs.filter(l => l.traceId === traceId)]
      .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
  }, [remote, logs, traceId]);
  const knownTraces = useMemo(() =>
    [...new Set(logs.map(l => l.traceId).filter(Boolean))].slice(0, 20),
    [logs]);
  return (
    <>
      <div className="card" style={{ marginBottom: 16 }}>
        <h3 style={{ marginBottom: 12, fontSize: 14, fontWeight: 600, color: 'var(--text-2)' }}>
          Search by Trace ID
        </h3>
        <div className="trace-search">
          <input id="trace-input" className="input-field" placeholder="Enter traceId…"
            value={input} onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && search()} />
          <button id="trace-search-btn" className="btn btn-primary" onClick={search}>
            Search
          </button>
          {traceId && (
            <button className="btn btn-ghost" onClick={() => { setTraceId(''); setInput(''); setRemote(null); }}>
              Clear
            </button>
          )}
        </div>
        {knownTraces.length > 0 && (
          <div style={{ marginTop: 10 }}>
            <span style={{ fontSize: 11, color: 'var(--text-3)' }}>Known traces: </span>
            {knownTraces.map(tid => (
              <button key={tid} className="btn btn-ghost"
                style={{ fontSize: 10, padding: '2px 8px', marginLeft: 4 }}
                onClick={() => { setInput(tid); setTraceId(tid); setRemote(null); }}>
                {tid.slice(0, 12)}…
              </button>
            ))}
          </div>
        )}
      </div>
      {loading && <div className="empty-state">Searching…</div>}
      {!loading && traceId && (
        <div className="card">
          <div className="feed-header">
            <span className="feed-title">
              Trace: <code style={{ fontSize: 12, color: 'var(--cyan)', fontFamily: 'JetBrains Mono,monospace' }}>{traceId}</code>
            </span>
            <span style={{ fontSize: 12, color: 'var(--text-3)' }}>{events.length} events</span>
          </div>
          {events.length === 0
            ? <div className="empty-state">No logs found for this trace ID.</div>
            : (
              <div className="trace-timeline">
                {events.map(log => (
                  <div key={log.id} className={`trace-event ${sevClass(log.severity)}`}>
                    <div className="trace-header">
                      <span className="timestamp" style={{ fontFamily: 'JetBrains Mono,monospace', fontSize: 11, color: 'var(--text-3)' }}>
                        {fmt(log.timestamp)}
                      </span>
                      <span className={`sev-badge ${sevClass(log.severity)}`}>{log.severity || 'LOW'}</span>
                      {log.service && <span className="service-tag">{log.service}</span>}
                      {log.spanId  && <span className="trace-tag" title={log.spanId}>span:{log.spanId.slice(0, 8)}</span>}
                      <span className="thread-tag">[{log.threadName || 'main'}]</span>
                    </div>
                    <div className="trace-msg">{log.data}</div>
                  </div>
                ))}
              </div>
            )}
        </div>
      )}
      {!traceId && (
        <div className="empty-state">
          Enter a trace ID above to see all logs for a request across services.
        </div>
      )}
    </>
  );
}
