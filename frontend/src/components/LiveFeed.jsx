import { useState, useMemo } from 'react';
const fmt = (ts) => {
  if (!ts) return 'N/A';
  const d = new Date(ts);
  return isNaN(d) ? 'N/A' : d.toLocaleTimeString();
};
const sevClass = (s) => (s ? s.toLowerCase() : 'low');
export default function LiveFeed({ logs, addLog, clearLogs }) {
  const [text,     setText]     = useState('');
  const [severity, setSeverity] = useState('LOW');
  const [service,  setService]  = useState('');
  const [traceId,  setTraceId]  = useState('');
  const [search,   setSearch]   = useState('');
  const [sevFilter, setSevFilter] = useState('ALL');
  const submit = () => {
    if (!text.trim()) return;
    addLog({
      data:      text,
      severity,
      service:   service || undefined,
      traceId:   traceId || undefined,
      threadName: 'UI-Thread',
    });
    setText('');
  };
  const filtered = useMemo(() => {
    return logs.filter(log => {
      let matchSearch = true;
      if (search) {
        try {
          matchSearch = new RegExp(search, 'i').test(log.data || '');
        } catch {
          matchSearch = (log.data || '').toLowerCase().includes(search.toLowerCase());
        }
      }
      const matchSev = sevFilter === 'ALL' || (log.severity || 'LOW') === sevFilter;
      return matchSearch && matchSev;
    });
  }, [logs, search, sevFilter]);
  return (
    <>
      {}
      <div className="card controls" style={{ marginBottom: 16 }}>
        <div className="row">
          <select id="sev-select" className="input-field" style={{ maxWidth: 110 }}
            value={severity} onChange={e => setSeverity(e.target.value)}>
            <option value="LOW">Low</option>
            <option value="WARN">Warn</option>
            <option value="HIGH">High</option>
          </select>
          <input id="log-input" className="input-field" placeholder="Log message…"
            value={text}
            onChange={e => setText(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && submit()} />
          <button id="add-btn" className="btn btn-primary" onClick={submit}>
            + Add Log
          </button>
        </div>
        <div className="row">
          <input className="input-field" placeholder="Service name (optional)"
            value={service} onChange={e => setService(e.target.value)} />
          <input className="input-field" placeholder="Trace ID (optional)"
            value={traceId} onChange={e => setTraceId(e.target.value)} />
        </div>
      </div>
      {}
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row">
          <input id="search-input" className="input-field" placeholder="Search / regex…"
            value={search} onChange={e => setSearch(e.target.value)} />
          <select id="sev-filter" className="input-field" style={{ maxWidth: 160 }}
            value={sevFilter} onChange={e => setSevFilter(e.target.value)}>
            <option value="ALL">All Severities</option>
            <option value="HIGH">High</option>
            <option value="WARN">Warn</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      </div>
      {}
      <div className="card">
        <div className="feed-header">
          <span className="feed-title">
            Recent Logs <span className="feed-count">({filtered.length})</span>
          </span>
          <button id="clear-btn" className="btn btn-danger" onClick={clearLogs}>
            Clear All
          </button>
        </div>
        {filtered.length === 0
          ? <div className="empty-state">No logs yet. Add one above or wait for your services.</div>
          : (
            <div className="log-list">
              {filtered.map(log => (
                <div key={log.id} className={`log-card ${sevClass(log.severity)}`}>
                  <div className="log-meta">
                    <span className="timestamp">{fmt(log.timestamp)}</span>
                    <span className={`sev-badge ${sevClass(log.severity)}`}>
                      {log.severity || 'LOW'}
                    </span>
                    {log.service  && <span className="service-tag">{log.service}</span>}
                    <span className="thread-tag">[{log.threadName || 'main'}]</span>
                    {log.traceId  && (
                      <span className="trace-tag" title={log.traceId}>
                        trace:{log.traceId.slice(0, 8)}…
                      </span>
                    )}
                  </div>
                  <div className="log-message">{log.data}</div>
                </div>
              ))}
            </div>
          )}
      </div>
    </>
  );
}
