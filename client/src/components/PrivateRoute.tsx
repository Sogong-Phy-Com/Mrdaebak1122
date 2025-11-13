import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface PrivateRouteProps {
  children: React.ReactElement;
  requireRole?: string;
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, requireRole }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/login" />;
  }

  if (requireRole) {
    // Admin can access employee routes
    if (requireRole === 'employee' && (user.role === 'employee' || user.role === 'admin')) {
      return children;
    }
    // Check exact role match for other cases
    if (user.role !== requireRole) {
      return <Navigate to="/" />;
    }
  }

  return children;
};

export default PrivateRoute;

