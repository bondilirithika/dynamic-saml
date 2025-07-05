import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import { Tabs, Tab, Form, Button, Alert, Spinner, Card, Row, Col } from 'react-bootstrap';

const API_BASE = process.env.REACT_APP_API_BASE || '';

const SamlProviderForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('basic');
  const [showSpMetadata, setShowSpMetadata] = useState(false);
  const [spMetadata, setSpMetadata] = useState(null);
  
  const [formData, setFormData] = useState({
    id: '',
    displayName: '',
    enabled: true,
    metadataSource: 'manual',
    metadataUrl: '',
    metadataXml: '',
    idpLoginUrl: '',
    idpLogoutUrl: '',
    idpCertificate: '',
    nameIdFormat: 'urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified',
    limitSelfRegistration: false,
    customIconUrl: '',
    signAuthnRequests: false,
    requireSignedResponses: true,
    requireEncryptedResponses: false,
    spCertificate: '',
    spPrivateKey: '',
    digestAlgorithm: 'SHA-1',
    signatureAlgorithm: 'RSA-SHA1',
    attributeMappings: {
      username: ['mail', 'email'],
      email: ['mail', 'email'],
      firstName: ['givenName'],
      lastName: ['sn']
    },
    requestedAttributes: {
      username: {
        name: 'mail',
        format: 'urn:oasis:names:tc:SAML:2.0:attrname-format:basic'
      },
      email: {
        name: 'mail',
        format: 'urn:oasis:names:tc:SAML:2.0:attrname-format:basic'
      },
      firstName: {
        name: 'givenName',
        format: 'urn:oasis:names:tc:SAML:2.0:attrname-format:basic'
      },
      lastName: {
        name: 'sn',
        format: 'urn:oasis:names:tc:SAML:2.0:attrname-format:basic'
      }
    }
  });

  // Load provider data if editing
  useEffect(() => {
    if (id) {
      setLoading(true);
      axios.get(`${API_BASE}/api/admin/saml/providers/${id}`)
        .then(response => {
          setFormData(response.data);
          setLoading(false);
        })
        .catch(err => {
          setError('Failed to load provider data');
          setLoading(false);
          console.error('Error loading provider:', err);
        });
    }
  }, [id]);

  // Generate ID from display name for new providers
  useEffect(() => {
    if (!id && formData.displayName && !formData.id) {
      const generatedId = formData.displayName
        .toLowerCase()
        .replace(/[^a-z0-9]/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');
      
      setFormData(prev => ({ ...prev, id: generatedId }));
    }
  }, [formData.displayName, formData.id, id]);

  // Handle form input changes
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  // Parse metadata from URL
  const handleParseMetadataUrl = async () => {
    if (!formData.metadataUrl) {
      setError('Please enter a metadata URL');
      return;
    }

    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.post(`${API_BASE}/api/admin/saml/parse-metadata-url`, {
        url: formData.metadataUrl
      });
      
      setFormData(prev => ({
        ...prev,
        metadataSource: 'url',
        idpLoginUrl: response.data.idpLoginUrl || prev.idpLoginUrl,
        idpLogoutUrl: response.data.idpLogoutUrl || prev.idpLogoutUrl,
        idpCertificate: response.data.idpCertificate || prev.idpCertificate,
        nameIdFormat: response.data.nameIdFormat || prev.nameIdFormat
      }));
      
      setError(null);
    } catch (err) {
      setError('Failed to parse metadata URL: ' + (err.response?.data?.message || err.message));
      console.error('Error parsing metadata URL:', err);
    } finally {
      setLoading(false);
    }
  };

  // Parse metadata from XML
  const handleParseMetadataXml = async () => {
    if (!formData.metadataXml) {
      setError('Please enter metadata XML');
      return;
    }

    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.post(`${API_BASE}/api/admin/saml/parse-metadata-xml`, {
        xml: formData.metadataXml
      });
      
      setFormData(prev => ({
        ...prev,
        metadataSource: 'xml',
        idpLoginUrl: response.data.idpLoginUrl || prev.idpLoginUrl,
        idpLogoutUrl: response.data.idpLogoutUrl || prev.idpLogoutUrl,
        idpCertificate: response.data.idpCertificate || prev.idpCertificate,
        nameIdFormat: response.data.nameIdFormat || prev.nameIdFormat
        // Notice: we're not setting spEntityId from the response
      }));
      
      setError(null);
    } catch (err) {
      setError('Failed to parse metadata XML: ' + (err.response?.data?.message || err.message));
      console.error('Error parsing metadata XML:', err);
    } finally {
      setLoading(false);
    }
  };

  // Handle form submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    try {
      const isUpdate = !!id;
      const url = isUpdate 
        ? `${API_BASE}/api/admin/saml/providers/${id}`
        : `${API_BASE}/api/admin/saml/providers`;
      
      const method = isUpdate ? 'put' : 'post';
      
      // For updates, create a copy without modifying spEntityId
      // For new providers, create a copy without spEntityId
      const formDataToSubmit = { ...formData };
      if (!isUpdate && formDataToSubmit.spEntityId) {
        delete formDataToSubmit.spEntityId;
      }
      
      const response = await axios[method](url, formDataToSubmit);
      
      // Show success message and SP details
      setShowSpMetadata(true);
      setSpMetadata({
        id: response.data.id,
        spEntityId: response.data.spEntityId,
        // Construct full ACS URL using the base URL and provider ID
        acsUrl: `${response.data.spEntityId.split('/saml2')[0]}/login/saml2/sso/${response.data.id}`
      });
      
      setLoading(false);
    } catch (err) {
      setError('Failed to save provider: ' + (err.response?.data?.message || err.message));
      setLoading(false);
      console.error('Error saving provider:', err);
    }
  };

  if (loading && !formData.id) {
    return (
      <div className="text-center my-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <div className="saml-provider-form">
      <h2>{id ? 'Edit' : 'Add'} SAML Provider</h2>
      
      {error && <Alert variant="danger">{error}</Alert>}
      
      {showSpMetadata && spMetadata && (
        <Card className="my-4 text-center">
          <Card.Header as="h5" className="bg-success text-white">
            Provider Saved Successfully
          </Card.Header>
          <Card.Body>
            <Card.Title>Service Provider Details for IdP Configuration</Card.Title>
            <Card.Text>
              Use these details to configure your Identity Provider.
            </Card.Text>
            
            <Row className="mb-3">
              <Col md={4} className="text-md-end fw-bold">Entity ID:</Col>
              <Col md={8} className="text-md-start">
                <code>{spMetadata.spEntityId}</code>
              </Col>
            </Row>
            
            <Row className="mb-3">
              <Col md={4} className="text-md-end fw-bold">ACS URL:</Col>
              <Col md={8} className="text-md-start">
                <code>{spMetadata.acsUrl}</code>
              </Col>
            </Row>
            
            <div className="mt-4">
              <Button 
                variant="primary" 
                onClick={() => navigate('/admin/saml')}
                className="me-2"
              >
                Back to Providers List
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setShowSpMetadata(false);
                  setSpMetadata(null);
                }}
              >
                Continue Editing
              </Button>
            </div>
          </Card.Body>
        </Card>
      )}
      
      {!showSpMetadata && (
        <Form onSubmit={handleSubmit}>
          <Tabs
            activeKey={activeTab}
            onSelect={(k) => setActiveTab(k)}
            className="mb-4"
          >
            <Tab eventKey="basic" title="Basic Information">
              <Form.Group className="mb-3">
                <Form.Label>Display Name *</Form.Label>
                <Form.Control
                  type="text"
                  name="displayName"
                  value={formData.displayName || ''}
                  onChange={handleChange}
                  required
                />
                <Form.Text className="text-muted">
                  The name that will appear on the login button
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Provider ID</Form.Label>
                <Form.Control
                  type="text"
                  name="id"
                  value={formData.id || ''}
                  onChange={handleChange}
                  disabled={!!id}
                />
                <Form.Text className="text-muted">
                  Unique identifier for this provider (auto-generated from display name)
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Check
                  type="checkbox"
                  name="enabled"
                  label="Enabled"
                  checked={formData.enabled}
                  onChange={handleChange}
                />
                <Form.Text className="text-muted">
                  When disabled, this provider won't appear on the login page
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Custom Icon URL</Form.Label>
                <Form.Control
                  type="text"
                  name="customIconUrl"
                  value={formData.customIconUrl || ''}
                  onChange={handleChange}
                  placeholder="https://example.com/icon.svg"
                />
                <Form.Text className="text-muted">
                  Optional URL to an icon for this provider (recommended size: 24x24px)
                </Form.Text>
              </Form.Group>
            </Tab>
            
            <Tab eventKey="metadata" title="Metadata Configuration">
              <div className="mb-4 p-3 bg-light rounded">
                <h5>Automatic Configuration</h5>
                <p>
                  Pre-fill configuration using a metadata URL or by pasting metadata XML.
                </p>
                
                <div className="mb-3">
                  <Form.Check
                    type="radio"
                    id="metadata-source-manual"
                    name="metadataSource"
                    value="manual"
                    label="I don't have metadata"
                    checked={formData.metadataSource === 'manual'}
                    onChange={handleChange}
                  />
                  <Form.Text className="ms-4 d-block">
                    Your identity provider does not have a metadata endpoint or XML. You'll configure it manually.
                  </Form.Text>
                </div>
                
                <div className="mb-3">
                  <Form.Check
                    type="radio"
                    id="metadata-source-url"
                    name="metadataSource"
                    value="url"
                    label="Metadata URL"
                    checked={formData.metadataSource === 'url'}
                    onChange={handleChange}
                  />
                  {formData.metadataSource === 'url' && (
                    <div className="ms-4 mt-2">
                      <Form.Control
                        type="text"
                        name="metadataUrl"
                        value={formData.metadataUrl || ''}
                        onChange={handleChange}
                        placeholder="https://idp.example.com/metadata"
                      />
                      <Button 
                        variant="outline-primary" 
                        size="sm" 
                        className="mt-2"
                        onClick={handleParseMetadataUrl}
                        disabled={loading}
                      >
                        {loading ? 'Parsing...' : 'Parse Metadata URL'}
                      </Button>
                    </div>
                  )}
                </div>
                
                <div className="mb-3">
                  <Form.Check
                    type="radio"
                    id="metadata-source-xml"
                    name="metadataSource"
                    value="xml"
                    label="Metadata XML"
                    checked={formData.metadataSource === 'xml'}
                    onChange={handleChange}
                  />
                  {formData.metadataSource === 'xml' && (
                    <div className="ms-4 mt-2">
                      <Form.Control
                        as="textarea"
                        rows={5}
                        name="metadataXml"
                        value={formData.metadataXml || ''}
                        onChange={handleChange}
                        placeholder="<EntityDescriptor xmlns=...>"
                      />
                      <Button 
                        variant="outline-primary" 
                        size="sm" 
                        className="mt-2"
                        onClick={handleParseMetadataXml}
                        disabled={loading}
                      >
                        {loading ? 'Parsing...' : 'Parse Metadata XML'}
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </Tab>
            
            <Tab eventKey="idp" title="Identity Provider">
              <Form.Group className="mb-3">
                <Form.Label>Identity Provider Login URL *</Form.Label>
                <Form.Control
                  type="text"
                  name="idpLoginUrl"
                  value={formData.idpLoginUrl || ''}
                  onChange={handleChange}
                  required
                />
                <Form.Text className="text-muted">
                  The URL of the identity provider login endpoint
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Identity Provider Logout URL</Form.Label>
                <Form.Control
                  type="text"
                  name="idpLogoutUrl"
                  value={formData.idpLogoutUrl || ''}
                  onChange={handleChange}
                />
                <Form.Text className="text-muted">
                  Optional URL for the identity provider logout endpoint
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Identity Provider Certificate *</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={6}
                  name="idpCertificate"
                  value={formData.idpCertificate || ''}
                  onChange={handleChange}
                  placeholder="-----BEGIN CERTIFICATE-----&#10;MIIDdDCCAlygAwIBAgIGAZWx...&#10;-----END CERTIFICATE-----"
                  required
                />
                <Form.Text className="text-muted">
                  X.509 public certificate of the identity provider in PEM format
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Name ID Format</Form.Label>
                <Form.Select
                  name="nameIdFormat"
                  value={formData.nameIdFormat || ''}
                  onChange={handleChange}
                >
                  <option value="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">Unspecified (default)</option>
                  <option value="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">Email Address</option>
                  <option value="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent">Persistent</option>
                  <option value="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">Transient</option>
                </Form.Select>
                <Form.Text className="text-muted">
                  Format of the NameID to request from the identity provider
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Check
                  type="checkbox"
                  name="limitSelfRegistration"
                  label="Limit Self Registration"
                  checked={formData.limitSelfRegistration}
                  onChange={handleChange}
                />
                <Form.Text className="text-muted">
                  If enabled, users can only register using this provider if self-registration is allowed
                </Form.Text>
              </Form.Group>
            </Tab>
            
            <Tab eventKey="signatures" title="Signatures & Encryption">
              <Form.Group className="mb-3">
                <Form.Check
                  type="checkbox"
                  name="signAuthnRequests"
                  label="Sign SAML AuthnRequests"
                  checked={formData.signAuthnRequests}
                  onChange={handleChange}
                />
                <Form.Text className="text-muted">
                  If checked, SAML authentication requests will be signed
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Check
                  type="checkbox"
                  name="requireSignedResponses"
                  label="Require Signed Responses"
                  checked={formData.requireSignedResponses}
                  onChange={handleChange}
                />
                <Form.Text className="text-muted">
                  If checked, responses from the identity provider must be signed
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Check
                  type="checkbox"
                  name="requireEncryptedResponses"
                  label="Require Encrypted Responses"
                  checked={formData.requireEncryptedResponses}
                  onChange={handleChange}
                />
                <Form.Text className="text-muted">
                  If enabled, the identity provider must encrypt the assertion response
                </Form.Text>
              </Form.Group>
              
              {formData.signAuthnRequests && (
                <>
                  <hr className="my-4" />
                  <h5>Signing Configuration</h5>
                  <p className="text-muted">Required when Sign SAML AuthnRequests is enabled</p>
                  
                  <Form.Group className="mb-3">
                    <Form.Label>Certificate for SAML Requests</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={5}
                      name="spCertificate"
                      value={formData.spCertificate || ''}
                      onChange={handleChange}
                      placeholder="-----BEGIN CERTIFICATE-----&#10;MIIDdDCCAlygAwIBAgIGAZWx...&#10;-----END CERTIFICATE-----"
                      required={formData.signAuthnRequests}
                    />
                    <Form.Text className="text-muted">
                      X.509 certificate used for signing SAML requests
                    </Form.Text>
                  </Form.Group>
                  
                  <Form.Group className="mb-3">
                    <Form.Label>Private Key for SAML Requests</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={5}
                      name="spPrivateKey"
                      value={formData.spPrivateKey || ''}
                      onChange={handleChange}
                      placeholder="-----BEGIN PRIVATE KEY-----&#10;MIIEvgIBADANBgkqhkiG9w0BAQ...&#10;-----END PRIVATE KEY-----"
                      required={formData.signAuthnRequests}
                    />
                    <Form.Text className="text-muted">
                      RSA private key for the above certificate
                    </Form.Text>
                  </Form.Group>
                  
                  <Row>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label>Digest Algorithm</Form.Label>
                        <Form.Select
                          name="digestAlgorithm"
                          value={formData.digestAlgorithm || 'SHA-1'}
                          onChange={handleChange}
                        >
                          <option value="SHA-1">SHA-1</option>
                          <option value="SHA-256">SHA-256</option>
                          <option value="SHA-512">SHA-512</option>
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label>Signature Algorithm</Form.Label>
                        <Form.Select
                          name="signatureAlgorithm"
                          value={formData.signatureAlgorithm || 'RSA-SHA1'}
                          onChange={handleChange}
                        >
                          <option value="RSA-SHA1">RSA SHA-1</option>
                          <option value="RSA-SHA256">RSA SHA-256</option>
                          <option value="RSA-SHA512">RSA SHA-512</option>
                        </Form.Select>
                      </Form.Group>
                    </Col>
                  </Row>
                </>
              )}
            </Tab>
            
            <Tab eventKey="attributes" title="Attributes">
              <h5>Attribute Mapping</h5>
              <p className="text-muted">
                Configure how SAML attributes from the identity provider map to user attributes
              </p>
              
              {/* This is simplified - you'd need to implement the full attribute mapping UI */}
              <Form.Group className="mb-3">
                <Form.Label>Username Attribute Mapping</Form.Label>
                <Form.Control
                  type="text"
                  value={formData.attributeMappings?.username?.join(', ') || ''}
                  onChange={(e) => {
                    const values = e.target.value.split(',').map(v => v.trim());
                    setFormData(prev => ({
                      ...prev,
                      attributeMappings: {
                        ...prev.attributeMappings,
                        username: values
                      }
                    }));
                  }}
                  placeholder="mail, email"
                />
                <Form.Text className="text-muted">
                  Comma-separated list of SAML attributes to use for the username
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Email Attribute Mapping</Form.Label>
                <Form.Control
                  type="text"
                  value={formData.attributeMappings?.email?.join(', ') || ''}
                  onChange={(e) => {
                    const values = e.target.value.split(',').map(v => v.trim());
                    setFormData(prev => ({
                      ...prev,
                      attributeMappings: {
                        ...prev.attributeMappings,
                        email: values
                      }
                    }));
                  }}
                  placeholder="mail, email"
                />
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>First Name Attribute Mapping</Form.Label>
                <Form.Control
                  type="text"
                  value={formData.attributeMappings?.firstName?.join(', ') || ''}
                  onChange={(e) => {
                    const values = e.target.value.split(',').map(v => v.trim());
                    setFormData(prev => ({
                      ...prev,
                      attributeMappings: {
                        ...prev.attributeMappings,
                        firstName: values
                      }
                    }));
                  }}
                  placeholder="givenName"
                />
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Last Name Attribute Mapping</Form.Label>
                <Form.Control
                  type="text"
                  value={formData.attributeMappings?.lastName?.join(', ') || ''}
                  onChange={(e) => {
                    const values = e.target.value.split(',').map(v => v.trim());
                    setFormData(prev => ({
                      ...prev,
                      attributeMappings: {
                        ...prev.attributeMappings,
                        lastName: values
                      }
                    }));
                  }}
                  placeholder="sn, surname"
                />
              </Form.Group>
            </Tab>
          </Tabs>
          
          <div className="d-flex justify-content-between mt-4">
            <Button variant="secondary" onClick={() => navigate('/admin/saml')}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? <Spinner size="sm" animation="border" /> : 'Save Provider'}
            </Button>
          </div>
        </Form>
      )}
    </div>
  );
};

export default SamlProviderForm;