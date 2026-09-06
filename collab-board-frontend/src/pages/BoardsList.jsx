import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';

import { Link } from 'react-router-dom';

function BoardsList() {
  const [boards, setBoards] = useState([]);
  const [newBoardTitle, setNewBoardTitle] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const username = localStorage.getItem('username');

  useEffect(() => {
    fetchBoards();
  }, []);

  const fetchBoards = async () => {
    try {
      const response = await api.get('/boards');
      setBoards(response.data);
    } catch (err) {
      setError('Failed to load boards.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateBoard = async (e) => {
    e.preventDefault();
    if (!newBoardTitle.trim()) return;

    try {
      await api.post('/boards', { title: newBoardTitle, description: '' });
      setNewBoardTitle('');
      fetchBoards();
    } catch (err) {
      setError('Failed to create board.');
    }
  };

  const handleDeleteBoard = async (e, boardId) => {
  e.preventDefault();
  e.stopPropagation();
  if (!window.confirm('Delete this board? This cannot be undone.')) return;

  try {
    await api.delete(`/boards/${boardId}`);
    fetchBoards();
  } catch (err) {
    setError('Failed to delete board.');
  }
};

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    navigate('/login');
  };

  if (loading) return <p>Loading boards...</p>;

  return (
    <div className="page">
            <div className="page-header">
                  <h2>Welcome, {username}</h2>
              <div style={{ display: 'flex', gap: '10px' }}>
                  <Link to="/join-board" className="back-link">Join a board</Link>
                  <button className="ghost" onClick={handleLogout}>Logout</button>
              </div>
            </div>

      <form onSubmit={handleCreateBoard} className="new-board-form" >
        <input
          type="text"
          placeholder="New board title"
          value={newBoardTitle}
          onChange={(e) => setNewBoardTitle(e.target.value)}
        />
        <button type="submit">Create Board</button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div className="board-grid">
        {boards.length === 0 && <p>No boards yet. Create one above.</p>}
        {boards.map((board) => (
          <div key={board.id} className="board-card">
                 <Link to={`/boards/${board.id}`}>
                    <h3>{board.title}</h3>
                    <p>{board.description}</p>
                    <p className="board-id">#{board.id}</p>
                </Link>
                <button className="danger" onClick={(e) => handleDeleteBoard(e, board.id)}>Delete</button>
            </div>
        ))}
      </div>
    </div>
  );
}

export default BoardsList;