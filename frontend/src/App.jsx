import { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import './App.css';
import LiveFeed      from './components/LiveFeed.jsx';
import StatsPanel    from './components/StatsPanel.jsx';
import TraceExplorer from './components/TraceExplorer.jsx';
import ServiceHealth from './components/ServiceHealth.jsx';
const API  = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const TABS = [
  { id: 'Live Feed', icon: '◉' },
  { id: 'Stats',     icon: '▦' },
  { id: 'Traces',    icon: '⌖' },
  { id: 'Health',    icon: '◈' },
];
export default function App() {
  const [logs,      setLogs]      = useState([]);
  const [tab,       setTab]       = useState('Live Feed');
  const [connected, setConnected] = useState(false);
  const esRef = useRef(null);
  const connectSse = useCallback(() => {
    if (esRef.current) esRef.current.close();
    const es = new EventSource(`${API}/api/logs/stream`);
    esRef.current = es;
    es.onopen = () => setConnected(true);
    es.addEventListener('log', (e) => {
      try {
        const log = JSON.parse(e.data);
        setLogs(prev => {
          if (prev.some(l => l.id === log.id)) return prev;
          return [log, ...prev].slice(0, 5000);
        });
      } catch (_) {}
    });
    es.onerror = () => {
      setConnected(false);
      es.close();
      setTimeout(connectSse, 3000);
    };
  }, []);
  useEffect(() => {
    axios.get(`${API}/api/logs`)
         .then(r => setLogs([...r.data].reverse()))
         .catch(console.error);
    connectSse();
    return () => { if (esRef.current) esRef.current.close(); };
  }, [connectSse]);
  const addLog = async (payload) => {
    try { await axios.post(`${API}/api/logs`, payload); }
    catch (e) { console.error(e); }
  };
  const clearLogs = async () => {
    try { await axios.delete(`${API}/api/logs`); setLogs([]); }
    catch (e) { console.error(e); }
  };
  return (
    <div className="app">
      {}
      <header className="header">
        <div className="header-inner">
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <span className="logo">LOGSTREAM</span>
            <span className={`live-badge ${connected ? '' : 'disconnected'}`}>
              <span className="dot" />
              {connected ? 'LIVE' : 'OFFLINE'}
            </span>
          </div>
          <span className="header-right">{logs.length} / 5000 entries</span>
        </div>
      </header>
      {}
      <nav className="nav">
        <div className="nav-inner">
          {TABS.map(({ id, icon }) => (
            <button
              key={id}
              className={`nav-tab ${tab === id ? 'active' : ''}`}
              onClick={() => setTab(id)}
            >
              <span style={{ fontFamily: 'monospace' }}>{icon}</span> {id}
            </button>
          ))}
        </div>
      </nav>
      {}
      <main className="page">
        <div className="page-inner">
          {tab === 'Live Feed' && <LiveFeed    logs={logs} addLog={addLog} clearLogs={clearLogs} />}
          {tab === 'Stats'     && <StatsPanel  api={API} />}
          {tab === 'Traces'    && <TraceExplorer api={API} logs={logs} />}
          {tab === 'Health'    && <ServiceHealth logs={logs} />}
        </div>
      </main>
    </div>
  );
}
