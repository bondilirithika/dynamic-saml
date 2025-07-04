import React, { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from "react-router-dom";
import ProtectedPage from "./ProtectedPage";
import logo from './logo.svg';
import './App.css';

const API_BASE = process.env.REACT_APP_API_BASE;
const redirectUri = window.location.origin;

// Simplified login flow - just one call
const handleLogin = () => {
  window.location.href = `${API_BASE}/api/auth/custom-login?redirectUri=${encodeURIComponent(redirectUri)}`;
};

function App() {
  const [jwt, setJwt] = useState(localStorage.getItem("jwt"));
  const [user, setUser] = useState(null);

  // On mount, check for JWT in URL (after SAML login)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("jwt");
    if (token) {
      localStorage.setItem("jwt", token);
      setJwt(token);
      window.history.replaceState({}, document.title, "/");
    }
  }, []);

  // Validate JWT if present
  useEffect(() => {
    if (jwt) {
      fetch(`${API_BASE}/api/auth/validate?token=${jwt}`)
        .then(res => res.json())
        .then(data => {
          if (data.valid) setUser(data);
          else {
            setUser(null);
            setJwt(null);
            localStorage.removeItem("jwt");
          }
        });
    }
  }, [jwt]);

  const handleLogout = () => {
    localStorage.removeItem("jwt");
    window.location.href = `${API_BASE}/api/auth/custom-logout?redirect_uri=${encodeURIComponent(window.location.origin)}`;
  };

  // ProtectedRoute logic inline
  const ProtectedRoute = ({ children }) => {
    if (!user) return <Navigate to="/" replace />;
    return children;
  };

  return (
    <Router>
      <div className="App">
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
          <h1>React SAML Client</h1>
          <nav>
            <Link to="/">Home</Link> | <Link to="/protected">Protected</Link>
          </nav>
          <Routes>
            <Route path="/" element={
              !jwt ? (
                <button onClick={handleLogin}>Login with SAML</button>
              ) : user ? (
                <div>
                  <p>Welcome, {user.username} ({user.email})</p>
                  <button onClick={handleLogout}>Logout</button>
                  <pre style={{ background: "#eee", padding: 10 }}>{jwt}</pre>
                </div>
              ) : null
            } />
            <Route path="/protected" element={
              <ProtectedRoute>
                <ProtectedPage />
              </ProtectedRoute>
            } />
          </Routes>
        </header>
      </div>
    </Router>
  );
}

export default App;
