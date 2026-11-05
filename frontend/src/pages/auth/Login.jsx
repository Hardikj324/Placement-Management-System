import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

import {
  GraduationCap,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  Info,
  ShieldCheck,
  UserCheck,
  Briefcase,
  Award,
  Sparkles,
  ArrowRight,
} from 'lucide-react';

import ThemeToggle from '../../components/common/ThemeToggle';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const isRegistered = searchParams.get('registered') === 'true';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showForgotInfo, setShowForgotInfo] = useState(false);

  // Redirect users according to their roles
  const routeUserByRole = (role) => {
    switch (role) {
      case 'ROLE_ADMIN':
        navigate('/admin/dashboard');
        break;

      case 'ROLE_OFFICER':
      case 'ROLE_PLACEMENT_OFFICER':
        navigate('/placement/dashboard');
        break;

      case 'ROLE_FACULTY':
        navigate('/faculty/dashboard');
        break;

      case 'ROLE_HOD':
        navigate('/hod/dashboard');
        break;

      case 'ROLE_STUDENT':
      default:
        navigate('/student/dashboard');
        break;
    }
  };

  // Normal login
  const handleSubmit = async (e) => {
    e.preventDefault();

    setError('');
    setShowForgotInfo(false);

    if (!email.trim()) {
      setError('Please enter your email address.');
      return;
    }

    if (!password) {
      setError('Please enter your password.');
      return;
    }

    setLoading(true);

    try {
      const user = await login(email.trim(), password);
      routeUserByRole(user.role);
    } catch (err) {
      setError(
        err.message || 'Login failed. Please check your credentials.'
      );
    } finally {
      setLoading(false);
    }
  };

  // Demo login
  const handleQuickLogin = async (demoEmail, demoPass) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError('');
    setShowForgotInfo(false);
    setLoading(true);

    try {
      const user = await login(demoEmail, demoPass);
      routeUserByRole(user.role);
    } catch (err) {
      setError(err.message || 'Quick login failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-split-wrapper">

      {/* LEFT SECTION: Branding */}
      <div className="auth-split-left">

        <div className="auth-split-left-bg-pattern" />
        <div className="auth-split-left-glow-top" />
        <div className="auth-split-left-glow-bottom" />

        {/* Brand Header */}
        <div className="auth-brand-badge">
          <div className="auth-brand-icon-box">
            <GraduationCap size={24} color="#60a5fa" />
          </div>

          <div className="auth-brand-name">
            JECRC Foundation
          </div>
        </div>

        {/* Hero Content */}
        <div className="auth-brand-hero">

          <h1 className="auth-hero-heading">
            JECRC Placement Portal
          </h1>

          <div className="auth-tagline">
            <Sparkles size={16} />
            <span>Campus Recruitment Made Easy</span>
          </div>

          <p className="auth-supporting-desc">
            Connecting students, recruiters and institutions through
            one unified placement platform.
          </p>

          {/* Features */}
          <div className="auth-feature-bullets">

            <div className="auth-bullet-item">
              <div className="auth-bullet-dot">
                <CheckCircle2 size={13} />
              </div>

              <span className="auth-bullet-text">
                Verified student talent and automated eligibility checks
              </span>
            </div>

            <div className="auth-bullet-item">
              <div className="auth-bullet-dot">
                <CheckCircle2 size={13} />
              </div>

              <span className="auth-bullet-text">
                Streamlined recruitment drives and interview workflows
              </span>
            </div>

            <div className="auth-bullet-item">
              <div className="auth-bullet-dot">
                <CheckCircle2 size={13} />
              </div>

              <span className="auth-bullet-text">
                Comprehensive campus placement metrics and reports
              </span>
            </div>

          </div>
        </div>

        {/* Footer */}
        <div className="auth-split-left-footer">
          <div className="auth-footer-text">
            © 2026 JECRC Foundation • Training & Placement Cell (TPO)
          </div>
        </div>

      </div>

      {/* RIGHT SECTION: Login Form */}
      <div className="auth-split-right">

        {/* Theme Toggle */}
        <div className="auth-theme-toggle-corner">
          <ThemeToggle />
        </div>

        <div className="auth-form-wrapper">

          {/* Form Header */}
          <div className="auth-form-header">
            <h2 className="auth-form-title">
              Sign in
            </h2>

            <p className="auth-form-subtitle">
              Enter your credentials below
            </p>
          </div>

          {/* Registration Success */}
          {isRegistered && (
            <div className="auth-alert auth-alert-success">
              <CheckCircle2
                size={18}
                style={{ flexShrink: 0, marginTop: '1px' }}
              />

              <div>
                Account created successfully! Please sign in with
                your credentials.
              </div>
            </div>
          )}

          {/* Forgot Password Information */}
          {showForgotInfo && (
            <div className="auth-alert auth-alert-info">
              <Info
                size={18}
                style={{ flexShrink: 0, marginTop: '1px' }}
              />

              <div>
                To reset your password, please contact the Placement
                Office at <strong>placement@jecrc.ac.in</strong> or
                reach out to your department coordinator.
              </div>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="auth-alert auth-alert-danger">
              <AlertCircle
                size={18}
                style={{ flexShrink: 0, marginTop: '1px' }}
              />

              <div>{error}</div>
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit} noValidate>

            {/* Email */}
            <div className="auth-field-group">

              <label htmlFor="login-email" className="auth-label">
                Email address
              </label>

              <input
                id="login-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@jecrc.ac.in"
                className="auth-text-input"
                autoComplete="email"
                disabled={loading}
              />

            </div>

            {/* Password */}
            <div className="auth-field-group">

              <div className="auth-label-row">

                <label
                  htmlFor="login-password"
                  className="auth-label"
                >
                  Password
                </label>

                <button
                  type="button"
                  onClick={() => setShowForgotInfo(!showForgotInfo)}
                  className="auth-forgot-btn"
                >
                  Forgot password?
                </button>

              </div>

              <div className="auth-input-container">

                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="auth-text-input"
                  style={{ paddingRight: '44px' }}
                  autoComplete="current-password"
                  disabled={loading}
                />

                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={
                    showPassword ? 'Hide password' : 'Show password'
                  }
                  className="auth-password-toggle"
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeOff size={18} />
                  ) : (
                    <Eye size={18} />
                  )}
                </button>

              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="auth-submit-btn"
            >
              <span>
                {loading ? 'Signing in...' : 'Sign In'}
              </span>

              {!loading && <ArrowRight size={16} />}
            </button>

          </form>

          {/* Registration Link */}
          <p className="auth-footer-prompt">
            Don't have an account?{' '}

            <Link
              to="/register"
              className="auth-register-link"
            >
              Register here
            </Link>
          </p>

          {/* Developer Demo Login */}
          <div className="auth-dev-section">

            <details className="auth-dev-details">

              <summary className="auth-dev-summary">
                <ShieldCheck size={14} />

                <span>
                  Developer Quick Access (Demo Roles)
                </span>
              </summary>

              <div className="auth-dev-grid">

                <button
                  type="button"
                  onClick={() =>
                    handleQuickLogin(
                      'student@placement.edu',
                      'student123'
                    )
                  }
                  className="auth-dev-chip"
                  disabled={loading}
                >
                  <GraduationCap size={14} color="#60a5fa" />
                  <span>Student (Aarav)</span>
                </button>

                <button
                  type="button"
                  onClick={() =>
                    handleQuickLogin(
                      'officer@placement.edu',
                      'officer123'
                    )
                  }
                  className="auth-dev-chip"
                  disabled={loading}
                >
                  <Briefcase size={14} color="#34d399" />
                  <span>Placement Officer</span>
                </button>

                <button
                  type="button"
                  onClick={() =>
                    handleQuickLogin(
                      'faculty@placement.edu',
                      'faculty123'
                    )
                  }
                  className="auth-dev-chip"
                  disabled={loading}
                >
                  <UserCheck size={14} color="#c084fc" />
                  <span>Faculty Coordinator</span>
                </button>

                <button
                  type="button"
                  onClick={() =>
                    handleQuickLogin(
                      'hod@placement.edu',
                      'hod123'
                    )
                  }
                  className="auth-dev-chip"
                  disabled={loading}
                >
                  <Award size={14} color="#fbbf24" />
                  <span>HOD (CSE)</span>
                </button>

                <button
                  type="button"
                  onClick={() =>
                    handleQuickLogin(
                      'admin@placement.edu',
                      'admin123'
                    )
                  }
                  className="auth-dev-chip"
                  style={{ gridColumn: 'span 2' }}
                  disabled={loading}
                >
                  <ShieldCheck size={14} color="#f87171" />
                  <span>System Administrator</span>
                </button>

              </div>
            </details>

          </div>

        </div>
      </div>

    </div>
  );
}