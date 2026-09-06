import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';

function JoinBoard() {
  const [boardId, setBoardId] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!boardId.trim()) return;
    setError('');
    setMessage('');

    try {
      await api.post(`/boards/${boardId}/join-request`);
      setMessage('Join request sent. Waiting for admin approval.');
      setBoardId('');
    } catch (err) {
      setError('Failed to send join request. Check the board ID.');
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h2>Join a Board</h2>
        <Link to="/boards" className="back-link">← Back to boards</Link>
      </div>

      <form onSubmit={handleSubmit} className="new-board-form">
        <input
          type="text"
          placeholder="Enter board ID"
          value={boardId}
          onChange={(e) => setBoardId(e.target.value)}
        />
        <button type="submit">Send Join Request</button>
      </form>

      {message && <p className="success-text">{message}</p>}
      {error && <p className="error-text">{error}</p>}
    </div>
  );
}

export default JoinBoard;