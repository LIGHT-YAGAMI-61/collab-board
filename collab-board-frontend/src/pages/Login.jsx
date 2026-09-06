import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const response = await api.post('/auth/login', {
        username,
        password,
      });

      localStorage.setItem('token', response.data.token);
      localStorage.setItem('username', response.data.username);

      navigate('/boards');
    } catch (err) {
      setError('Invalid username or password.');
    }
  };

  return (
    <div className="auth-page">
        <div className="auth-card">
      <h2>Login</h2>
      <form onSubmit={handleSubmit}>
        <div className="auth-field">
            <label>Username</label>
                <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                 />
            </div>
        <div className="auth-field">
         <label>Password</label>
            <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            />
         </div>
        {error && <p className="error-text">{error}</p>}
        <button type="submit">Login</button>
        </form>
            <p className="auth-switch">Don't have an account? <Link to="/register">Register</Link></p>
        </div>
    </div>
  );
}

export default Login;