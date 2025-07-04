import React, { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import axios from 'axios';
import ProtectedPage from "./ProtectedPage";
import LoginPage from "./components/LoginPage";
import AdminLayout from "./admin/AdminLayout";
import SamlProviderList from "./admin/SamlProviderList";
import SamlProviderForm from "./admin/SamlProviderForm";
import { Container, Spinner } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

const API_BASE = process.env.REACT_APP_API_BASE || '';

function App() {
  const [jwt, setJwt] = useState(localStorage.getItem("jwt"));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // On mount, check for JWT in URL (after SAML login)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("jwt");
    if (token) {
      localStorage.setItem("jwt", token);
      setJwt(token);
      window.history.replaceState({}, document.title, window.location.pathname);
    }
    setLoading(false);
  }, []);

  // Validate JWT if present
  useEffect(() => {
    if (jwt) {
      setLoading(true);
      axios.get(`${API_BASE}/api/auth/validate?token=${jwt}`)
        .then(response => {
          const data = response.data;
          if (data.valid) {
            // Transform the user data
            const transformedUser = {
              ...data,
              // If email is null but username is an email, use username as email
              email: data.email || (data.username && data.username.includes('@') ? data.username : null),
              // Generate a name from the email/username if not provided
              name: data.name || (() => {
                const email = data.email || data.username;
                if (email && email.includes('@')) {
                  const nameBase = email.split('@')[0].replace(/\./g, ' ');
                  // Capitalize first letter of each word
                  return nameBase.split(' ')
                    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
                    .join(' ');
                }
                return null;
              })()
            };
            setUser(transformedUser);
          } else {
            setUser(null);
            setJwt(null);
            localStorage.removeItem("jwt");
          }
        })
        .catch(() => {
          setUser(null);
          setJwt(null);
          localStorage.removeItem("jwt");
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [jwt]);

  const handleLogout = () => {
    localStorage.removeItem("jwt");
    window.location.href = `${API_BASE}/api/auth/custom-logout?redirect_uri=${encodeURIComponent(window.location.origin)}`;
  };

  // Set up axios interceptor for JWT
  useEffect(() => {
    const interceptor = axios.interceptors.request.use(config => {
      if (jwt) {
        config.headers.Authorization = `Bearer ${jwt}`;
      }
      return config;
    });

    return () => {
      axios.interceptors.request.eject(interceptor);
    };
  }, [jwt]);

  if (loading) {
    return (
      <Container className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </Container>
    );
  }

  // Check if user has admin role
  const isAdmin = user?.roles?.includes('ADMIN');

  return (
    <Router>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={!user ? <LoginPage /> : <Navigate to="/" replace />} />
        
        {/* Protected routes */}
        <Route 
          path="/" 
          element={
            user ? (
              <ProtectedPage user={user} onLogout={handleLogout} />
            ) : (
              <Navigate to="/login" replace />
            )
          } 
        />
        
        {/* Admin routes */}
        <Route path="/admin" element={isAdmin ? <AdminLayout /> : <Navigate to="/" replace />}>
          <Route index element={<div>Admin Dashboard</div>} />
          <Route path="saml" element={<SamlProviderList />} />
          <Route path="saml/add" element={<SamlProviderForm />} />
          <Route path="saml/edit/:id" element={<SamlProviderForm />} />
        </Route>
        
        {/* Fallback route */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
