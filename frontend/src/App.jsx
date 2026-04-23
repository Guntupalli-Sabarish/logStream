import { useState, useEffect, useMemo } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  const [logs, setLogs] = useState([]);
  const [newLog, setNewLog] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterSeverity, setFilterSeverity] = useState('ALL');
  const [newLogSeverity, setNewLogSeverity] = useState('LOW');

  const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

  const fetchLogs = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/logs`);
      // Use spread to avoid mutating the axios response array
      setLogs([...response.data].reverse());
    } catch (error) {
      console.error('Error fetching logs:', error);
    }
  };

  const addSimulatedLog = async () => {
    if (!newLog) return;
    try {
      await axios.post(`${API_BASE_URL}/api/logs`, {
        data: newLog,
        severity: newLogSeverity,
        threadName: 'UI-Thread'
      });
      setNewLog('');
      fetchLogs();
    } catch (error) {
      console.error('Error adding log:', error);
    }
  };

  const clearLogs = async () => {
    try {
      await axios.delete(`${API_BASE_URL}/api/logs`);
      fetchLogs();
    } catch (error) {
      console.error('Error clearing logs:', error);
    }
  };

  useEffect(() => {
    fetchLogs();
    const interval = setInterval(fetchLogs, 2000);
    return () => clearInterval(interval);
  }, []);

  const filteredLogs = useMemo(() => {
    return logs.filter(log => {
      let matchesSearch = false;
      try {
        const regex = new RegExp(searchTerm, 'i');
        matchesSearch = regex.test(log.data);
      } catch (e) {
        matchesSearch = log.data.toLowerCase().includes(searchTerm.toLowerCase());
      }
      const matchesSeverity = filterSeverity === 'ALL' || (log.severity || 'LOW') === filterSeverity;
      return matchesSearch && matchesSeverity;
    });
  }, [logs, searchTerm, filterSeverity]);

  // Safe timestamp formatter — returns "N/A" if timestamp is null/invalid
  const formatTimestamp = (ts) => {
    if (!ts) return 'N/A';
    const d = new Date(ts);
    return isNaN(d.getTime()) ? 'N/A' : d.toLocaleTimeString();
  };

  return (
    <div className="container">
      <header className="header">
        <h1>System Monitor</h1>
        <div className="status-badge">Live</div>
      </header>

      <div className="controls-card">
        <div className="input-group">
          <select
            id="severity-select"
            value={newLogSeverity}
            onChange={(e) => setNewLogSeverity(e.target.value)}
            className="severity-select"
          >
            <option value="LOW">Low</option>
            <option value="WARN">Warn</option>
            <option value="HIGH">High</option>
          </select>
          <input
            id="log-input"
            type="text"
            value={newLog}
            onChange={(e) => setNewLog(e.target.value)}
            placeholder="Simulate a system log..."
            className="log-input"
            onKeyDown={(e) => e.key === 'Enter' && addSimulatedLog()}
          />
          <button id="add-log-btn" onClick={addSimulatedLog} className="add-btn">Add Log</button>
        </div>

        <div className="filters">
          <input
            id="search-input"
            type="text"
            placeholder="Search logs (supports regex)..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
          <select
            id="filter-severity-select"
            value={filterSeverity}
            onChange={(e) => setFilterSeverity(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">All Severities</option>
            <option value="HIGH">High</option>
            <option value="WARN">Warn</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      </div>

      <div className="logs-container">
        <div className="logs-header">
          <h3>Recent Logs ({filteredLogs.length})</h3>
          <button id="clear-logs-btn" onClick={clearLogs} className="clear-btn">Clear Logs</button>
        </div>
        {filteredLogs.length === 0 ? (
          <div className="empty-state">No matching logs found.</div>
        ) : (
          filteredLogs.map((log) => (
            // Use stable server-assigned ID as React key
            <div key={log.id} className="log-card">
              <div className="log-meta">
                <span className="timestamp">{formatTimestamp(log.timestamp)}</span>
                <span className={`severity ${log.severity ? log.severity.toLowerCase() : 'low'}`}>
                  {log.severity || 'LOW'}
                </span>
                <span className="thread">[{log.threadName || 'main'}]</span>
              </div>
              <div className="log-message">{log.data}</div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default App;
