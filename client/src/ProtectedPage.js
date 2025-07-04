import React from "react";
import { Container, Card, Button, Row, Col } from "react-bootstrap";

export default function ProtectedPage({ user, onLogout }) {
  return (
    <Container className="py-5">
      <Row className="justify-content-center">
        <Col md={8} lg={6}>
          <Card>
            <Card.Header as="h4" className="text-center bg-primary text-white">
              🔒 Protected Page
            </Card.Header>
            <Card.Body>
              <Card.Title>
                Welcome, {user?.name || user?.email || "Authenticated User"}!
              </Card.Title>
              <Card.Text>
                You have successfully authenticated via SAML.
              </Card.Text>

              {user && (
                <Card className="mb-4 bg-light">
                  <Card.Body>
                    <h5>User Information</h5>
                    <p>
                      <strong>Email:</strong> {user.email || "Not available"}
                    </p>
                    <p>
                      <strong>Name:</strong> {user.name || "Not available"}
                    </p>
                    <p>
                      <strong>Roles:</strong>{" "}
                      {user.roles ? user.roles.join(", ") : "None"}
                    </p>
                  </Card.Body>
                </Card>
              )}

              <div className="d-grid gap-2">
                <Button variant="danger" onClick={onLogout}>
                  Logout
                </Button>
              </div>
            </Card.Body>
            <Card.Footer className="text-muted text-center">
              Logged in via SAML authentication
            </Card.Footer>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}