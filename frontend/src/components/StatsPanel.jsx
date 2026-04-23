import { useState, useEffect } from 'react';
import axios from 'axios';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  PieChart, Pie, Cell,
  BarChart, Bar,
} from 'recharts';
const SEV_COLORS = { HIGH: '#f87171', WARN: '#fbbf24', LOW: '#4ade80', UNKNOWN: '#52526a' };
const TOOLTIP_STYLE = {
  background: '#121320',
  border: '1px solid rgba(255,255,255,0.10)',
  borderRadius: 10,
  color: '#eeeef5',
  fontSize: 12,
};
export default function StatsPanel({ api }) {
  const [stats, setStats] = useState(null);
  useEffect(() => {
    const load = () =>
      axios.get(`${api}/api/logs/stats`).then(r => setStats(r.data)).catch(() => {});
    load();
    const id = setInterval(load, 5000);
    return () => clearInterval(id);
  }, [api]);
  if (!stats) return <div className="empty-state">Loading stats…</div>;
  const sevPie = Object.entries(stats.bySeverity || {}).map(([name, value]) => ({ name, value }));
  const svcBar = Object.entries(stats.byService || {})
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([name, value]) => ({ name, value }));
  return (
    <>
      {}
      <div className="kpi-row">
        <div className="kpi">
          <div className="kpi-val">{stats.total}</div>
          <div className="kpi-lbl">Total in memory</div>
        </div>
        <div className="kpi">
          <div className="kpi-val" style={{ color: '#818cf8' }}>{stats.ratePerMinute}</div>
          <div className="kpi-lbl">Logs / min (5 min avg)</div>
        </div>
        <div className="kpi">
          <div className="kpi-val" style={{ color: '#f87171' }}>{stats.bySeverity?.HIGH ?? 0}</div>
          <div className="kpi-lbl">High severity</div>
        </div>
        <div className="kpi">
          <div className="kpi-val" style={{ color: '#fbbf24' }}>{stats.bySeverity?.WARN ?? 0}</div>
          <div className="kpi-lbl">Warnings</div>
        </div>
      </div>
      {}
      <div className="stats-grid">
        {}
        <div className="stat-card span-3">
          <h3>Log Rate — Last 10 Minutes</h3>
          <ResponsiveContainer width="100%" height={190}>
            <LineChart data={stats.rateHistory || []}>
              <defs>
                <linearGradient id="lineGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="minute" tick={{ fill: '#52526a', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={{ fill: '#52526a', fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={TOOLTIP_STYLE} />
              <Line
                type="monotone" dataKey="count" stroke="#6366f1"
                strokeWidth={2.5} dot={{ r: 3, fill: '#6366f1', strokeWidth: 0 }}
                activeDot={{ r: 5, fill: '#818cf8' }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
        {}
        <div className="stat-card">
          <h3>Severity Distribution</h3>
          {sevPie.length === 0
            ? <div className="empty-state" style={{ padding: '30px 0' }}>No data yet</div>
            : (
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie
                    data={sevPie} dataKey="value" nameKey="name"
                    cx="50%" cy="50%" outerRadius={72} innerRadius={36}
                    paddingAngle={3}
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    labelLine={{ stroke: '#52526a', strokeWidth: 1 }}>
                    {sevPie.map(e => (
                      <Cell key={e.name} fill={SEV_COLORS[e.name] || '#52526a'} stroke="transparent" />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={TOOLTIP_STYLE} />
                </PieChart>
              </ResponsiveContainer>
            )}
        </div>
        {}
        <div className="stat-card span-2">
          <h3>Top Services by Volume</h3>
          {svcBar.length === 0
            ? <div className="empty-state" style={{ padding: '30px 0' }}>
                Set a <code style={{ color: '#818cf8' }}>service</code> field when adding logs
              </div>
            : (
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={svcBar} layout="vertical">
                  <defs>
                    <linearGradient id="barGrad" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%"   stopColor="#6366f1" />
                      <stop offset="100%" stopColor="#818cf8" />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
                  <XAxis type="number" allowDecimals={false} tick={{ fill: '#52526a', fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis dataKey="name" type="category" width={130} tick={{ fill: '#9090aa', fontSize: 11 }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={TOOLTIP_STYLE} />
                  <Bar dataKey="value" fill="url(#barGrad)" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
        </div>
      </div>
    </>
  );
}
