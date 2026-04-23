import { useMemo } from 'react';

const STALE_MS   = 5 * 60 * 1000;
const DEGRADE_MS = 5 * 60 * 1000;

function classify(service, now) {
  if (!service.logs.length) return 'silent';
  const latestTs = Math.max(...service.logs.map(l => new Date(l.timestamp).getTime()));
  if (now - latestTs > STALE_MS) return 'silent';
  const hasHigh = service.logs.some(
    l => l.severity === 'HIGH' && now - new Date(l.timestamp).getTime() < DEGRADE_MS
  );
  return hasHigh ? 'degraded' : 'healthy';
}

const fmt = (ts) => {
  if (!ts) return '—';
  const d = new Date(ts);
  return isNaN(d) ? '—' : d.toLocaleTimeString();
};

const STATUS_LABEL  = { healthy: 'Healthy', degraded: 'Degraded', silent: 'Silent' };
const STATUS_COLORS = { healthy: '#4ade80', degraded: '#fbbf24', silent: '#52526a' };

export default function ServiceHealth({ logs }) {
  const now = Date.now();

  const services = useMemo(() => {
    const map = {};
    logs.forEach(log => {
      const name = log.service || '(untagged)';
      if (!map[name]) map[name] = { name, logs: [] };
      map[name].logs.push(log);
    });
    return Object.values(map).map(s => ({
      ...s,
      status:    classify(s, now),
      total:     s.logs.length,
      highCount: s.logs.filter(l => l.severity === 'HIGH').length,
      warnCount: s.logs.filter(l => l.severity === 'WARN').length,
      lastSeen:  s.logs.reduce((m, l) =>
        new Date(l.timestamp) > new Date(m) ? l.timestamp : m, s.logs[0]?.timestamp),
    })).sort((a, b) => {
      const order = { degraded: 0, silent: 1, healthy: 2 };
      return order[a.status] - order[b.status];
    });
  }, [logs]);

  if (services.length === 0)
    return (
      <div className="empty-state">
        No services detected. Set a <code style={{ color: '#818cf8' }}>service</code> field when submitting logs.
      </div>
    );

  return (
    <>
      <div className="kpi-row" style={{ gridTemplateColumns: 'repeat(3,1fr)', marginBottom: 20 }}>
        {['healthy', 'degraded', 'silent'].map(s => {
          const n = services.filter(svc => svc.status === s).length;
          return (
            <div key={s} className="kpi">
              <div className="kpi-val" style={{ color: STATUS_COLORS[s], fontVariantNumeric: 'tabular-nums' }}>{n}</div>
              <div className="kpi-lbl">{STATUS_LABEL[s]}</div>
            </div>
          );
        })}
      </div>

      <div className="health-grid">
        {services.map(svc => (
          <div key={svc.name} className="health-card">
            <h4>
              <span className={`status-dot ${svc.status}`} />
              {svc.name}
            </h4>
            <div className="health-meta">
              <div className="health-stat">
                <span>Status</span>
                <span style={{ color: STATUS_COLORS[svc.status], fontWeight: 600 }}>
                  {STATUS_LABEL[svc.status]}
                </span>
              </div>
              <div className="health-stat">
                <span>Total logs</span>
                <span>{svc.total}</span>
              </div>
              <div className="health-stat">
                <span>High alerts</span>
                <span style={{ color: svc.highCount ? '#f87171' : undefined }}>
                  {svc.highCount}
                </span>
              </div>
              <div className="health-stat">
                <span>Warnings</span>
                <span style={{ color: svc.warnCount ? '#fbbf24' : undefined }}>
                  {svc.warnCount}
                </span>
              </div>
              <div className="health-stat">
                <span>Last seen</span>
                <span>{fmt(svc.lastSeen)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
