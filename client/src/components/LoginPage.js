import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Card, Button, Spinner, Alert } from 'react-bootstrap';
import './LoginPage.css';

const API_BASE = process.env.REACT_APP_API_BASE || '';
const redirectUri = window.location.origin;

const LoginPage = () => {
  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    console.log("Fetching login options from:", `${API_BASE}/api/auth/options`);
    
    // Fetch SAML login options
    axios.get(`${API_BASE}/api/auth/options`)
      .then(response => {
        console.log("Login options response:", response.data);
        // Ensure providers is always an array
        setProviders(Array.isArray(response.data) ? response.data : []);
        setLoading(false);
      })
      .catch(err => {
        console.error("Error loading login options:", err);
        setError("Failed to load login options");
        setLoading(false);
      });
  }, []);

  const handleLogin = (providerId) => {
    window.location.href = `${API_BASE}/api/auth/custom-login?redirectUri=${encodeURIComponent(redirectUri)}&provider=${providerId}`;
  };

  if (loading) {
    return (
      <div className="text-center my-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <div className="login-container">
      <Card className="login-card">
        <Card.Header as="h4" className="text-center">
          Sign In
        </Card.Header>
        
        <Card.Body>
          {error && <Alert variant="danger">{error}</Alert>}
          
          {providers.length === 0 ? (
            <Alert variant="warning">
              No login providers available. Please contact your administrator.
            </Alert>
          ) : (
            <div className="saml-providers">
              {providers.map(provider => (
                <Button
                  key={provider.id}
                  variant="outline-primary"
                  className="w-100 mb-2 saml-login-button"
                  onClick={() => handleLogin(provider.id)}
                  style={{display: 'flex', alignItems: 'center'}}
                >
                  {provider.iconUrl && (
                    <img 
                      src={provider.iconUrl} 
                      alt="" 
                      className="provider-icon me-2" 
                      style={{width: '20px', height: '20px', marginRight: '10px'}}
                    />
                  )}
                  Login with {provider.displayName}
                </Button>
              ))}
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default LoginPage;