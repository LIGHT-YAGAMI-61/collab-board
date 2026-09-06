import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import BoardsList from './pages/BoardsList';
import BoardDetail from './pages/BoardDetail';
import JoinBoard from './pages/JoinBoard';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/boards" element={<BoardsList />} />
        <Route path="/join-board" element={<JoinBoard />} />
        <Route path="/boards/:boardId" element={<BoardDetail />} />
        <Route path="/" element={<Navigate to="/login" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;