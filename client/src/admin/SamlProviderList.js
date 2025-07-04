import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { Table, Button, Badge, Spinner, Alert } from 'react-bootstrap';

const API_BASE = process.env.REACT_APP_API_BASE || '';

const SamlProviderList = () => {
  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchProviders = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE}/api/admin/saml/providers`);
      setProviders(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load SAML providers. Make sure you have admin permissions.');
      console.error('Error fetching providers:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProviders();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this provider?')) {
      return;
    }
    
    try {
      await axios.delete(`${API_BASE}/api/admin/saml/providers/${id}`);
      fetchProviders();
    } catch (err) {
      setError('Failed to delete provider');
      console.error('Error deleting provider:', err);
    }
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
    <div className="saml-providers-container">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>SAML Providers</h2>
        <Link to="/admin/saml/add" className="btn btn-primary">
          Add Provider
        </Link>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {providers.length === 0 ? (
        <Alert variant="info">
          No SAML providers configured. Click "Add Provider" to create one.
        </Alert>
      ) : (
        <Table striped bordered hover responsive>
          <thead>
            <tr>
              <th>Name</th>
              <th>Status</th>
              <th>IdP Login URL</th>
              <th>Configuration Source</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {providers.map(provider => (
              <tr key={provider.id}>
                <td>
                  {provider.customIconUrl && (
                    <img 
                      src={provider.customIconUrl} 
                      alt="" 
                      style={{ height: '20px', marginRight: '8px' }} 
                    />
                  )}
                  {provider.displayName}
                </td>
                <td>
                  {provider.enabled ? (
                    <Badge bg="success">Enabled</Badge>
                  ) : (
                    <Badge bg="danger">Disabled</Badge>
                  )}
                </td>
                <td className="text-truncate" style={{maxWidth: '200px'}}>
                  {provider.idpLoginUrl}
                </td>
                <td>
                  {provider.metadataSource === 'url' ? (
                    <Badge bg="info">Metadata URL</Badge>
                  ) : provider.metadataSource === 'xml' ? (
                    <Badge bg="info">Metadata XML</Badge>
                  ) : (
                    <Badge bg="secondary">Manual</Badge>
                  )}
                </td>
                <td>
                  <div className="d-flex gap-2">
                    <Link 
                      to={`/admin/saml/edit/${provider.id}`} 
                      className="btn btn-sm btn-outline-primary"
                    >
                      Edit
                    </Link>
                    <Button 
                      variant="outline-danger" 
                      size="sm" 
                      onClick={() => handleDelete(provider.id)}
                    >
                      Delete
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
};

export default SamlProviderList;